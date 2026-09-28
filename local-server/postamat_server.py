#!/usr/bin/env python3
"""Temporary local postamat backend for Android/WebSocket integration tests.

No third-party packages are required. It exposes:
  WebSocket: ws://<laptop-ip>:8765/v1/device/socket
  HTTP admin: http://<laptop-ip>:8766/

The server requires a device token. It reads ARENDO_DEVICE_TOKEN from the
environment or prompts for one without echoing it. The admin UI binds to
loopback by default and must not be exposed through a public tunnel.
"""

from __future__ import annotations

import argparse
import base64
import hashlib
import json
import logging
import getpass
import hmac
import os
import secrets
import socket
import socketserver
import threading
import uuid
from collections import deque
from datetime import datetime, timedelta, timezone
from http import HTTPStatus
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import parse_qs, urlparse


WS_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11"
CELL_CODES = tuple(f"{number:02d}" for number in range(1, 11))


def now_iso() -> str:
    return datetime.now(timezone.utc).isoformat(timespec="seconds").replace("+00:00", "Z")


def command_expiry() -> str:
    return (datetime.now(timezone.utc) + timedelta(minutes=2)).isoformat(
        timespec="seconds"
    ).replace("+00:00", "Z")


class PostamatState:
    def __init__(self, expected_token: str):
        if not expected_token:
            raise ValueError("A device token is required")
        self.lock = threading.RLock()
        self.expected_token = expected_token
        self.client: WebSocketConnection | None = None
        self.postamat_id = "map-7"
        self.postamat_name = "Локальный постамат"
        self.app_version = "—"
        self.capabilities: list[str] = []
        self.last_seen = None
        self.last_hello = None
        self.last_heartbeat = None
        self.cells = {
            code: {"code": code, "door": "unknown", "lock": "unknown"}
            for code in CELL_CODES
        }
        self.events = deque(maxlen=200)

    def log(self, message: str, payload: dict | None = None) -> None:
        item = {"at": now_iso(), "message": message}
        if payload is not None:
            item["payload"] = payload
        with self.lock:
            self.events.appendleft(item)
        logging.info(message)

    def attach(self, client: "WebSocketConnection", token: str) -> None:
        with self.lock:
            old = self.client
            self.client = client
        if old is not None and old is not client:
            old.close()
        self.log("WebSocket подключён: токен принят")

    def detach(self, client: "WebSocketConnection") -> None:
        with self.lock:
            if self.client is client:
                self.client = None
        self.log("WebSocket отключён")

    def send(self, message: dict) -> bool:
        with self.lock:
            client = self.client
        if client is None:
            self.log("Команда не отправлена: Android не подключён")
            return False
        try:
            client.send_json(message)
            self.log(f"→ Android: {message.get('type')} {message.get('kind', '')}".strip(), message)
            return True
        except OSError as error:
            self.log(f"Ошибка отправки в Android: {error}")
            return False

    def handle_message(self, client: "WebSocketConnection", message: dict) -> None:
        message_type = message.get("type", "")
        with self.lock:
            self.last_seen = now_iso()

        if message_type == "hello":
            with self.lock:
                self.postamat_id = message.get("postamatId") or "map-7"
                self.app_version = message.get("appVersion") or "—"
                capabilities = message.get("capabilities")
                self.capabilities = [item for item in capabilities if isinstance(item, str)] \
                    if isinstance(capabilities, list) else []
                self.last_hello = now_iso()
                self._merge_cells(message.get("cells"))
                welcome_cells = [dict(self.cells[code]) for code in CELL_CODES]
            self.log("← Android: hello", message)
            self.send(
                {
                    "type": "welcome",
                    "postamatId": self.postamat_id,
                    "postamatName": self.postamat_name,
                    "config": {"heartbeatSec": 15, "pingSec": 20, "pongWaitSec": 45},
                    "cells": [
                        dict(cell, rentalStatus="available") for cell in welcome_cells
                    ],
                    "qrCode": "LOCAL-ARENDO-MAP-7",
                }
            )
            return

        if message_type == "heartbeat":
            with self.lock:
                self.last_heartbeat = now_iso()
                self._merge_cells(message.get("cells"))
            self.log("← Android: heartbeat", message)
            return

        if message_type == "event":
            kind = message.get("kind", "unknown")
            code = self._normalise_cell(message.get("cellCode"))
            with self.lock:
                if code and kind == "door_opened":
                    self.cells[code]["door"] = "open"
                elif code and kind == "door_closed":
                    self.cells[code]["door"] = "closed"
            self.log(f"← Android: event {kind} {code or ''}".strip(), message)
            return

        if message_type == "ack":
            self.log(
                f"← Android: ack {message.get('commandId', '')} ok={message.get('ok')}",
                message,
            )
            return

        self.log(f"← Android: {message_type or 'unknown'}", message)

    def _normalise_cell(self, value) -> str | None:
        digits = "".join(ch for ch in str(value or "") if ch.isdigit())
        if not digits:
            return None
        try:
            number = int(digits)
        except ValueError:
            return None
        return f"{number:02d}" if 1 <= number <= len(CELL_CODES) else None

    def _merge_cells(self, cells) -> None:
        if not isinstance(cells, list):
            return
        for item in cells:
            if not isinstance(item, dict):
                continue
            code = self._normalise_cell(item.get("code", item.get("cellCode")))
            if code is None:
                continue
            if item.get("door") in {"open", "closed", "unknown"}:
                self.cells[code]["door"] = item["door"]
            if item.get("lock"):
                self.cells[code]["lock"] = item["lock"]

    def command(self, kind: str, cell_code: str | None = None) -> bool:
        code = self._normalise_cell(cell_code)
        command = {
            "type": "command",
            "id": f"local-{uuid.uuid4().hex[:12]}",
            "postamatId": self.postamat_id,
            "kind": kind,
            "payload": {"cellCode": code} if code else {},
            "expiresAt": command_expiry(),
            "signature": "local-development",
        }
        return self.send(command)

    def snapshot(self) -> dict:
        with self.lock:
            return {
                "connected": self.client is not None,
                "postamatId": self.postamat_id,
                "postamatName": self.postamat_name,
                "appVersion": self.app_version,
                "capabilities": list(self.capabilities),
                "safeRelayCommands": self.client is not None
                    and "relay_pulse_2s" in self.capabilities,
                "lastSeen": self.last_seen,
                "lastHello": self.last_hello,
                "lastHeartbeat": self.last_heartbeat,
                "cells": [dict(self.cells[code]) for code in CELL_CODES],
                "events": list(self.events),
            }


class WebSocketConnection:
    def __init__(self, sock: socket.socket, state: PostamatState):
        self.sock = sock
        self.state = state
        self.send_lock = threading.Lock()
        self.closed = False

    def send_json(self, message: dict) -> None:
        self.send_text(json.dumps(message, ensure_ascii=False, separators=(",", ":")))

    def send_text(self, text: str) -> None:
        payload = text.encode("utf-8")
        if len(payload) < 126:
            header = bytes([0x81, len(payload)])
        elif len(payload) < 65536:
            header = bytes([0x81, 126]) + len(payload).to_bytes(2, "big")
        else:
            header = bytes([0x81, 127]) + len(payload).to_bytes(8, "big")
        with self.send_lock:
            self.sock.sendall(header + payload)

    def send_control(self, opcode: int, payload: bytes = b"") -> None:
        with self.send_lock:
            self.sock.sendall(bytes([0x80 | opcode, len(payload)]) + payload)

    def close(self) -> None:
        self.closed = True
        try:
            self.send_control(0x8)
        except OSError:
            pass
        try:
            self.sock.close()
        except OSError:
            pass

    def recv_exact(self, count: int) -> bytes:
        result = bytearray()
        while len(result) < count:
            chunk = self.sock.recv(count - len(result))
            if not chunk:
                raise ConnectionError("соединение закрыто")
            result.extend(chunk)
        return bytes(result)

    def recv_text(self) -> str | None:
        first = self.recv_exact(2)
        opcode = first[0] & 0x0F
        masked = bool(first[1] & 0x80)
        length = first[1] & 0x7F
        if length == 126:
            length = int.from_bytes(self.recv_exact(2), "big")
        elif length == 127:
            length = int.from_bytes(self.recv_exact(8), "big")
        mask = self.recv_exact(4) if masked else b""
        payload = bytearray(self.recv_exact(length))
        if masked:
            for index in range(length):
                payload[index] ^= mask[index % 4]
        if opcode == 0x8:
            return None
        if opcode == 0x9:
            self.send_control(0xA, bytes(payload))
            return ""
        if opcode != 0x1:
            return ""
        return bytes(payload).decode("utf-8")


class WebSocketRequestHandler(socketserver.BaseRequestHandler):
    def handle(self) -> None:
        state: PostamatState = self.server.state  # type: ignore[attr-defined]
        try:
            request = read_http_request(self.request)
            if request is None:
                return
            method, path, headers = request
            parsed = urlparse(path)
            if headers.get("upgrade", "").lower() != "websocket":
                send_http_json(self.request, HTTPStatus.NOT_FOUND, {"error": "websocket endpoint"})
                return
            if parsed.path != "/v1/device/socket":
                send_http_json(self.request, HTTPStatus.NOT_FOUND, {"error": "not found"})
                return
            token = headers.get("x-device-token", "")
            if not hmac.compare_digest(token, state.expected_token):
                state.log("WebSocket отклонён: неверный токен")
                send_http_json(
                    self.request,
                    HTTPStatus.UNAUTHORIZED,
                    {"code": "UNAUTHORIZED", "message": "Локальный токен не принят"},
                )
                return
            key = headers.get("sec-websocket-key", "")
            if not key:
                send_http_json(self.request, HTTPStatus.BAD_REQUEST, {"error": "missing websocket key"})
                return
            accept = base64.b64encode(
                hashlib.sha1((key + WS_GUID).encode("ascii")).digest()
            ).decode("ascii")
            response = (
                "HTTP/1.1 101 Switching Protocols\r\n"
                "Upgrade: websocket\r\n"
                "Connection: Upgrade\r\n"
                f"Sec-WebSocket-Accept: {accept}\r\n\r\n"
            )
            self.request.sendall(response.encode("ascii"))
            client = WebSocketConnection(self.request, state)
            state.attach(client, token)
            while not client.closed:
                raw = client.recv_text()
                if raw is None:
                    break
                if not raw:
                    continue
                try:
                    message = json.loads(raw)
                except json.JSONDecodeError:
                    state.log("← Android: некорректный JSON")
                    continue
                if isinstance(message, dict):
                    state.handle_message(client, message)
        except (ConnectionError, OSError) as error:
            logging.debug("WebSocket завершён: %s", error)
        finally:
            if "client" in locals():
                state.detach(client)
            try:
                self.request.close()
            except OSError:
                pass


class WebSocketServer(socketserver.ThreadingTCPServer):
    allow_reuse_address = True
    daemon_threads = True

    def __init__(self, address, handler, state):
        super().__init__(address, handler)
        self.state = state


class AdminHandler(BaseHTTPRequestHandler):
    server_version = "ARENDO-Local/0.1"

    def log_message(self, format, *args):
        logging.info("HTTP %s", format % args)

    @property
    def state(self) -> PostamatState:
        return self.server.state  # type: ignore[attr-defined]

    def do_GET(self):
        parsed = urlparse(self.path)
        if parsed.path == "/" or parsed.path == "/index.html":
            body = dashboard_html().encode("utf-8")
            self.send_response(HTTPStatus.OK)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)
            return
        if parsed.path in {"/health", "/api/state"}:
            send_http_json(self, HTTPStatus.OK, self.state.snapshot())
            return
        if parsed.path == "/api/command/open":
            query = parse_qs(parsed.query)
            code = (query.get("cell") or [""])[0]
            self._send_command("open_cell", code)
            return
        if parsed.path == "/api/command/report":
            self._send_command("cell_report", None)
            return
        send_http_json(self, HTTPStatus.NOT_FOUND, {"error": "not found"})

    def do_POST(self):
        parsed = urlparse(self.path)
        body = read_json_body(self)
        if parsed.path in {"/api/command", "/api/webhook"}:
            if not isinstance(body, dict):
                send_http_json(self, HTTPStatus.BAD_REQUEST, {"error": "JSON object expected"})
                return
            if parsed.path == "/api/webhook":
                self.state.log("← webhook ноутбука", body)
                send_http_json(self, HTTPStatus.ACCEPTED, {"ok": True})
                return
            kind = body.get("kind", "")
            code = body.get("cellCode", body.get("cell", ""))
            if kind not in {"open_cell", "cell_report"}:
                send_http_json(self, HTTPStatus.BAD_REQUEST, {"error": "unsupported command"})
                return
            self._send_command(kind, code)
            return
        send_http_json(self, HTTPStatus.NOT_FOUND, {"error": "not found"})

    def _send_command(self, kind: str, code: str | None):
        if kind == "open_cell":
            normalized = self.state._normalise_cell(code)
            if normalized is None:
                send_http_json(
                    self,
                    HTTPStatus.BAD_REQUEST,
                    {"sent": False, "error": "invalid_cell", "cellCode": code},
                )
                return
            code = normalized
            with self.state.lock:
                compatible = self.state.client is not None and "relay_pulse_2s" in self.state.capabilities
            if not compatible:
                self.state.log("Команда реле заблокирована: устройство не объявило safe relay pulse v1")
                send_http_json(
                    self,
                    HTTPStatus.CONFLICT,
                    {"sent": False, "error": "device_update_required", "cellCode": code},
                )
                return
        sent = self.state.command(kind, code)
        send_http_json(
            self,
            HTTPStatus.OK if sent else HTTPStatus.SERVICE_UNAVAILABLE,
            {"sent": sent, "kind": kind, "cellCode": code},
        )


class AdminServer(ThreadingHTTPServer):
    daemon_threads = True

    def __init__(self, address, state):
        super().__init__(address, AdminHandler)
        self.state = state


def read_http_request(sock: socket.socket):
    data = bytearray()
    while b"\r\n\r\n" not in data and len(data) < 65536:
        chunk = sock.recv(4096)
        if not chunk:
            return None
        data.extend(chunk)
    header = bytes(data).split(b"\r\n\r\n", 1)[0].decode("iso-8859-1")
    lines = header.split("\r\n")
    method, path, _ = lines[0].split(" ", 2)
    headers = {}
    for line in lines[1:]:
        if ":" in line:
            key, value = line.split(":", 1)
            headers[key.strip().lower()] = value.strip()
    return method, path, headers


def read_json_body(handler: BaseHTTPRequestHandler):
    try:
        length = int(handler.headers.get("Content-Length", "0"))
        raw = handler.rfile.read(length) if length else b"{}"
        return json.loads(raw.decode("utf-8"))
    except (ValueError, json.JSONDecodeError, UnicodeDecodeError):
        return None


def send_http_json(target, status: HTTPStatus, payload: dict):
    body = json.dumps(payload, ensure_ascii=False, indent=2).encode("utf-8")
    if isinstance(target, BaseHTTPRequestHandler):
        target.send_response(status)
        target.send_header("Content-Type", "application/json; charset=utf-8")
        target.send_header("Access-Control-Allow-Origin", "*")
        target.send_header("Content-Length", str(len(body)))
        target.end_headers()
        target.wfile.write(body)
        return
    target.sendall(
        (
            f"HTTP/1.1 {status.value} {status.phrase}\r\n"
            "Content-Type: application/json; charset=utf-8\r\n"
            f"Content-Length: {len(body)}\r\n"
            "Connection: close\r\n\r\n"
        ).encode("ascii")
        + body
    )


def dashboard_html() -> str:
    return """<!doctype html>
<html lang="ru"><meta charset="utf-8"><title>ARENDO local bridge</title>
<style>
body{font:16px system-ui;background:#0b1118;color:#e8edf4;max-width:1000px;margin:28px auto;padding:0 18px}
main{background:#111a25;border:1px solid #2a3a4e;border-radius:16px;padding:22px}
button{margin:4px;padding:10px 16px;border:1px solid #2d78e8;border-radius:9px;background:#17263a;color:#e8edf4;cursor:pointer}
button:disabled{opacity:.55;cursor:wait}.muted{color:#a8b4c4}
.bulk,.cells{display:flex;flex-wrap:wrap;gap:8px;margin:12px 0}.cell{background:#0c1420;border:1px solid #2a3a4e;border-radius:12px;padding:10px;min-width:170px}
pre{white-space:pre-wrap;background:#080d13;padding:14px;border-radius:10px;max-height:480px;overflow:auto}.ok{color:#39d39a}.bad{color:#ff6677}
</style>
<main><h1>ARENDO · локальный стенд</h1><p id="status">Загрузка…</p>
<p id="identity" class="muted"></p>
<p class="muted">Открытие — импульс реле на 2 секунды, затем выход автоматически выключается. Дверца закрывается физически; статус open/closed определяется только обратным сигналом X, иначе остаётся unknown.</p>
<h2>Управление ячейками</h2><div id="bulk" class="bulk"></div><p id="actionStatus" class="muted"></p><div id="buttons" class="cells"></div>
<button id="report">Запросить статусы</button><h2>Состояние</h2><pre id="state"></pre><h2>События</h2><pre id="events"></pre></main>
<script>
async function api(url, options){const r=await fetch(url, options);const data=await r.json();if(!r.ok||data.sent===false)throw new Error(data.error||'Команда не отправлена');return data}
let controlsReady=false, actionsBusy=false;
function setBusy(busy){actionsBusy=busy;document.querySelectorAll('#bulk button,#buttons button').forEach(button=>button.disabled=busy||!controlsReady);document.querySelector('#report').disabled=busy}
async function refresh(){const s=await api('/api/state');document.querySelector('#status').innerHTML=s.connected?'<span class="ok">Android подключён</span>':'<span class="bad">Android не подключён</span>';const id=s.postamatId||'не задан';const hint=id==='map-7'?' · временный тестовый ID, не категория и не адрес Modbus':'';document.querySelector('#identity').textContent='Постамат: '+(s.postamatName||'без названия')+' · ID: '+id+hint+' · APK '+(s.appVersion||'—');document.querySelector('#state').textContent=JSON.stringify({postamatId:s.postamatId,appVersion:s.appVersion,capabilities:s.capabilities,lastSeen:s.lastSeen,cells:s.cells},null,2);document.querySelector('#events').textContent=s.events.map(x=>x.at+' · '+x.message).join('\\n');controlsReady=s.safeRelayCommands===true;setBusy(actionsBusy);if(!controlsReady)document.querySelector('#actionStatus').textContent=s.connected?'Открытие заблокировано: установите APK с поддержкой двухсекундного импульса.':'Команды реле недоступны: Android не подключён.'}
const cells=Array.from({length:10},(_,i)=>String(i+1).padStart(2,'0'));
const bulk=document.querySelector('#bulk');
bulk.innerHTML='<button id="openAll">Открыть все D1–D10 · по очереди, 2 с</button>';
const buttons=document.querySelector('#buttons');
buttons.innerHTML=cells.map(c=>'<section class="cell"><strong>D'+Number(c)+'</strong><div><button data-cell="'+c+'">Открыть D'+Number(c)+'</button></div></section>').join('');
function pause(ms){return new Promise(resolve=>setTimeout(resolve,ms))}
async function openAll(){if(!window.confirm('Открыть все 10 ячеек? Каждое реле получит импульс 2 секунды по очереди; проверьте, что это безопасно.'))return;setBusy(true);try{for(let i=0;i<cells.length;i++){document.querySelector('#actionStatus').textContent='Открытие: D'+Number(cells[i])+' ('+(i+1)+'/10)';await api('/api/command/open?cell='+cells[i]);if(i<cells.length-1)await pause(2200)}document.querySelector('#actionStatus').textContent='Команды открытия отправлены по очереди. Ждём обратную связь X.'}catch(error){document.querySelector('#actionStatus').textContent='Ошибка: '+error.message}finally{setBusy(false);await refresh()}}
bulk.querySelector('#openAll').addEventListener('click',openAll);
buttons.addEventListener('click',async event=>{const button=event.target.closest('button[data-cell]');if(!button)return;const cell=button.dataset.cell;if(!window.confirm('Подать на D'+Number(cell)+' импульс реле на 2 секунды?'))return;button.disabled=true;try{await api('/api/command/open?cell='+cell);document.querySelector('#actionStatus').textContent='Команда открытия отправлена для D'+Number(cell)}catch(error){document.querySelector('#actionStatus').textContent='Ошибка: '+error.message}finally{button.disabled=false;await refresh()}});
document.querySelector('#report').addEventListener('click',async()=>{try{await api('/api/command/report');document.querySelector('#actionStatus').textContent='Запрос статусов отправлен'}catch(error){document.querySelector('#actionStatus').textContent='Ошибка: '+error.message}await refresh()});
refresh();setInterval(refresh,2000);
</script></html>"""


def main() -> None:
    parser = argparse.ArgumentParser(description="Temporary ARENDO local WebSocket backend")
    parser.add_argument("--ws-port", type=int, default=8765)
    parser.add_argument("--http-port", type=int, default=8766)
    parser.add_argument("--ws-host", default="0.0.0.0", help="WebSocket bind address")
    parser.add_argument("--admin-host", default="127.0.0.1", help="admin UI bind address; keep private")
    parser.add_argument("--token-env", default="ARENDO_DEVICE_TOKEN", help="environment variable containing the expected device token")
    args = parser.parse_args()

    token = os.environ.get(args.token_env, "").strip()
    if not token:
        try:
            token = getpass.getpass("Тестовый токен устройства (ввод скрыт): ").strip()
        except (EOFError, KeyboardInterrupt):
            parser.error("Для запуска требуется токен устройства")
    if not token:
        parser.error("Для запуска требуется непустой токен устройства")

    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
    state = PostamatState(token)
    ws_server = WebSocketServer((args.ws_host, args.ws_port), WebSocketRequestHandler, state)
    admin_server = AdminServer((args.admin_host, args.http_port), state)
    threading.Thread(target=ws_server.serve_forever, name="websocket", daemon=True).start()
    threading.Thread(target=admin_server.serve_forever, name="admin", daemon=True).start()
    logging.info("WebSocket: ws://%s:%s/v1/device/socket", args.ws_host, args.ws_port)
    logging.info("Admin:      http://%s:%s/ (локально на ноутбуке)", args.admin_host, args.http_port)
    logging.info("Ожидается Android; токен обязателен и не выводится в журнал")
    try:
        threading.Event().wait()
    except KeyboardInterrupt:
        logging.info("Остановка")
        ws_server.shutdown()
        admin_server.shutdown()


if __name__ == "__main__":
    main()
