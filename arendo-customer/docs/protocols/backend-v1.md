# Backend device protocol v1

Status: v1 integration contract / MVP. Read the implementation boundary below before treating any
demo value as a physical sensor fact.

The Russian backend task list, questions and joint acceptance flow are in
[`backend-developer-handoff-ru.md`](backend-developer-handoff-ru.md).

## Transport and authentication

- WebSocket endpoint: configurable, normally `/v1/device/socket`.
- Production transport: `wss` with a valid certificate.
- Device token: `X-Device-Token` during the HTTP upgrade.
- The backend stores only a hash of the token and may disable or rotate it.
- Tokens and credentials are never written to application logs.
- The current APK does not verify a command signature. Production command-signing/key provisioning
  must be agreed and implemented before treating token-authenticated MVP commands as production-safe.

The backend addresses business entities (`postamatId`, `cellCode`). It does not know Modbus addresses,
controller terminals, baud rate or USB-converter details. `cellCode` is a string such as `"01"`;
the server must preserve leading zeroes.

## Device to backend

### Hello

Sent once after each connection.

```json
{
  "type": "hello",
  "protocolVersion": 1,
  "postamatId": "map-7",
  "appVersion": "0.2.1",
  "capabilities": ["locks", "buttons", "indicators", "demo_buttons_as_doors", "relay_pulse_2s"],
  "cells": [
    {"code": "01", "door": "unknown", "lock": "unknown"}
  ],
  "net": {"kind": "unknown"}
}
```

### Heartbeat

Sent at the interval returned in `welcome`. It contains current cell facts, connectivity and active problems.

```json
{
  "type": "heartbeat",
  "postamatId": "map-7",
  "sentAt": "2026-09-24T12:00:00Z",
  "cells": [{"code": "01", "door": "unknown", "lock": "unknown"}],
  "net": {"kind": "unknown"},
  "problems": []
}
```

### Event

Events describe a reported transition. `eventId` is unique for the event, but the current APK does
not persist events or replay them after an offline period. Do not assume durable delivery until the
event-queue and acknowledgement extension is implemented.

```json
{
  "type": "event",
  "eventId": "550e8400-e29b-41d4-a716-446655440000",
  "kind": "door_closed",
  "postamatId": "map-7",
  "cellCode": "01",
  "occurredAt": "2026-09-24T12:00:04Z",
  "detail": {"source": "demo_button", "simulated": true}
}
```

Current APK event kinds: `door_opened`, `door_closed`. In the button/LED build, these events carry
`detail.simulated: true` and a `demo_*` source; they are not real door-sensor events. `modbus_offline`
is currently reported in the heartbeat `problems` array, not as an event. The other kinds in older
drafts (`door_not_closed`, `cell_blocked`, `cell_unblocked`, etc.) are not currently emitted.

## Backend to device

### Welcome

```json
{
  "type": "welcome",
  "postamatId": "map-7",
  "config": {"heartbeatSec": 15}
}
```

### Command

```json
{
  "type": "command",
  "id": "cmd-123",
  "kind": "open_cell",
  "postamatId": "map-7",
  "payload": {"cellCode": "01"},
  "expiresAt": "2026-09-24T12:00:10Z"
}
```

`postamatId` is the stable business identity of the postamat. The current `map-7` value is only a
temporary demo ID; it is not a product category, cabinet number or Modbus slave address. Replace it
with the ID assigned by the backend before production.

Commands accepted by the current APK:

| Command | Required payload | Device behavior |
|---|---|---|
| `open_cell` | `cellCode` | Validate postamat, expiry and cell; pulse the mapped lock output for 2 seconds, then switch it OFF and acknowledge command acceptance. This does not prove the door opened. |
| `cell_report` | none | Send a heartbeat snapshot and acknowledge the report request. |

`close_cell`, `confirm_closed`, `block_cell` and `unblock_cell` are **not implemented** by the current APK. Do not
send them until device-side support and the associated authorization/state rules have been agreed.

### Acknowledgement

```json
{
  "type": "ack",
  "commandId": "cmd-123",
  "ok": true,
  "result": {"kind": "open_command_accepted", "cellCode": "01"}
}
```

Current error codes: `invalid_command_id`, `wrong_postamat`, `expired`, `invalid_cell`,
`unsupported_command`, `modbus_offline`, `internal_error`. A repeated state-changing command ID returns
`ok: true` with result kind `duplicate_ignored`; this means “not executed again”, not “door opened/closed”.
`blocked` and `door_not_closed` are future outcomes, not current device behavior.

## Safety and reconnect rules

- `id` is globally unique and mandatory for every state-changing command.
- The device rejects an expired command before touching hardware.
- A duplicate command ID is not executed again. The current ledger remembers a bounded set of IDs;
  the APK does not yet persist a full command result for each ID.
- Commands from a closed WebSocket session are not queued in memory for execution after reconnect.
- A backend timeout must not be interpreted as proof that a door did not open; the final state is reconciled from events and `cell_report`.
- Offline event persistence/replay and event acknowledgements are not implemented in this APK yet.
  They are required before claiming lossless event delivery.

## Minimal backend responsibilities

1. Authenticate and register each postamat connection.
2. Route a command by `postamatId` and `cellCode`.
3. Persist command lifecycle: created, delivered, acknowledged, expired or failed.
4. Persist latest device and cell status plus immutable events.
5. Mark a postamat offline after the negotiated heartbeat timeout.
6. Never resend an expired opening command after the device reconnects.

## Door status displayed by the backend

The backend displays the reported state for each logical `cellCode`, for example `D1 open` or
`D2 closed`. The `door` field has only these meanings:

| Protocol value | UI meaning |
|---|---|
| `open` | Door-open feedback was reported for this cell. |
| `closed` | Door-closed feedback was reported for this cell. |
| `unknown` | No reliable door feedback is available. |

An `open_cell` acknowledgement means that the device accepted the command and started the output
pulse; it is not proof that the door physically opened. The output turns OFF automatically after
two seconds, but that does not mean a person physically closed the door. The backend
must wait for a `door_opened` / `door_closed` event or a subsequent reported door state from feedback.
Demo/simulated feedback must be marked as such and must not be presented as a verified physical
sensor reading. The backend contract never includes USB, RS-485, Modbus addresses or controller
terminal labels; those are local device-gateway configuration.

### Feedback timeout after opening

Target behavior for the selected stand: after an `open_cell` action, wait up to 30 seconds for the
configured X-input feedback to report the expected transition. If no feedback arrives, keep the door
state `unknown`, record a no-feedback/timeout result in the command journal, and do not show the
opening as confirmed. Agree the exact result code and which component owns the timer during backend
integration. This 30-second timeout is a requirement for the next software update; it is not yet
implemented in the current APK/protocol flow. A late feedback event may still update the observed
door state, with its own timestamp; it must not rewrite the earlier timeout as if confirmation had
arrived on time.

## Status availability and interpretation

| Status | Source of truth | Current availability |
|---|---|---|
| Postamat `online` / `offline` | Backend derives it from the live WebSocket and heartbeat timeout. A disconnected device cannot send its own offline event. | `hello` and heartbeat are implemented; backend timeout/persistence/UI are backend work. |
| Controller communication | Latest heartbeat `problems`; `modbus_offline` means the gateway currently cannot read the I/O module. An empty list means no problem was reported in that heartbeat. | Implemented in heartbeat; no separate recovery event. |
| Door `open` / `closed` / `unknown` | `cells[].door` snapshot plus `door_opened` / `door_closed` events. | Presently simulated by LED/button; only a real sensor can make it a physical door fact. |
| Lock `locked` / `unlocked` | Requires a dedicated reliable lock-state signal. | Not available; APK reports `lock: "unknown"`. Do not infer it from an output/relay write. |
| Network type (`wifi` / `cellular`) | Device network monitor, if added. | APK currently sends `net.kind: "unknown"`; backend can report WebSocket connectivity only. |
| Cell block / technician note | Agreed backend/service workflow and an implemented device command/event. | Not in current device protocol implementation. |
| GPS, payment, rental and QR state | GPS acquisition/reporting is Nursultan's device-side scope; backend owns business/payment/rental state and will agree coordinate ingestion/binding when the device payload is ready. | GPS is not in current device messages; payment/rental/QR remain backend/business state. |

The app currently sends a heartbeat every 15 seconds by default (the server may set `heartbeatSec`
between 5 and 300). The APK consumes `heartbeatSec`; the `pingSec` and `pongWaitSec` fields in older
examples are not currently configurable by the APK. Device heartbeats currently use `net.kind:
"unknown"`.

## Backend presentation rules

- Keep device connectivity, controller connectivity, door state, lock state and business/block state
  as separate fields. Do not collapse them into one green “online” indicator.
- Show a door as `unknown` when feedback is missing, stale, or the gateway reports a problem; do not
  keep presenting an old `open`/`closed` value as current.
- Show command acceptance separately from sensor-confirmed door movement. The UI may show “Команда
  принята” after ACK, then update to `D1 open` only after the corresponding state/event arrives.
- Preserve and display the `simulated` flag in the event journal during the demo. Never count a
  simulated event as production sensor evidence.
- Derive postamat `offline` after the agreed heartbeat timeout; do not wait for a device-originated
  `device_offline` event.

