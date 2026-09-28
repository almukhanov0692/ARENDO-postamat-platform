import base64
import json
import socket
import threading
import urllib.error
import urllib.request
import unittest

from postamat_server import (
    PostamatState,
    AdminHandler,
    AdminServer,
    WebSocketRequestHandler,
    WebSocketServer,
    dashboard_html,
)


class WebSocketTokenTests(unittest.TestCase):
    def setUp(self):
        self.state = PostamatState("demo-device-token")
        self.server = WebSocketServer(
            ("127.0.0.1", 0), WebSocketRequestHandler, self.state
        )
        self.thread = threading.Thread(target=self.server.serve_forever, daemon=True)
        self.thread.start()
        self.port = self.server.server_address[1]

    def tearDown(self):
        self.server.shutdown()
        self.server.server_close()
        self.thread.join(timeout=2)

    def handshake_status(self, token=None):
        sock = socket.create_connection(("127.0.0.1", self.port), timeout=2)
        sock.settimeout(2)
        headers = [
            "GET /v1/device/socket HTTP/1.1",
            "Host: localhost",
            "Upgrade: websocket",
            "Connection: Upgrade",
            f"Sec-WebSocket-Key: {base64.b64encode(b'arendo-test-key').decode('ascii')}",
            "Sec-WebSocket-Version: 13",
        ]
        if token is not None:
            headers.append(f"X-Device-Token: {token}")
        request = "\r\n".join(headers) + "\r\n\r\n"
        try:
            sock.sendall(request.encode("ascii"))
            response = bytearray()
            while b"\r\n\r\n" not in response:
                response.extend(sock.recv(1024))
            return bytes(response).split(b"\r\n", 1)[0].decode("ascii")
        finally:
            sock.close()

    def test_missing_token_is_rejected(self):
        self.assertIn("401", self.handshake_status())

    def test_wrong_token_is_rejected(self):
        self.assertIn("401", self.handshake_status("wrong-token"))

    def test_expected_token_allows_websocket_upgrade(self):
        self.assertIn("101", self.handshake_status("demo-device-token"))

    def test_token_is_required_when_creating_server_state(self):
        with self.assertRaises(ValueError):
            PostamatState("")


class CellCodeTests(unittest.TestCase):
    def setUp(self):
        self.state = PostamatState("demo-device-token")

    def test_normalizes_supported_cells(self):
        self.assertEqual("01", self.state._normalise_cell("D1"))
        self.assertEqual("10", self.state._normalise_cell("10"))

    def test_rejects_out_of_range_cells(self):
        self.assertIsNone(self.state._normalise_cell("D11"))
        self.assertIsNone(self.state._normalise_cell("door"))


class CommandEnvelopeTests(unittest.TestCase):
    def test_command_targets_the_connected_postamat(self):
        class RecordingClient:
            def __init__(self):
                self.messages = []

            def send_json(self, message):
                self.messages.append(message)

        state = PostamatState("demo-device-token")
        state.postamat_id = "map-7"
        client = RecordingClient()
        state.client = client

        self.assertTrue(state.command("cell_report"))
        self.assertEqual("map-7", client.messages[0]["postamatId"])

        self.assertTrue(state.command("open_cell", "01"))
        self.assertEqual("open_cell", client.messages[1]["kind"])
        self.assertEqual({"cellCode": "01"}, client.messages[1]["payload"])


class DashboardTests(unittest.TestCase):
    def test_event_separator_is_a_valid_javascript_escape(self):
        self.assertIn(r".join('\n')", dashboard_html())

    def test_dashboard_has_bulk_and_per_cell_relay_controls(self):
        html = dashboard_html()
        self.assertIn("Открыть все D1–D10", html)
        self.assertIn("Открыть D", html)
        self.assertIn("Запросить статусы", html)
        self.assertNotIn("Закрыть все", html)
        self.assertNotIn("Закрыть D", html)
        self.assertIn("'/api/command/open?cell='+cell", html)

    def test_dashboard_explains_demo_postamat_id(self):
        self.assertIn("map-7", dashboard_html())
        self.assertIn("не категория и не адрес Modbus", dashboard_html())


class AdminCommandApiTests(unittest.TestCase):
    def setUp(self):
        class RecordingClient:
            def __init__(self):
                self.messages = []

            def send_json(self, message):
                self.messages.append(message)

        self.state = PostamatState("demo-device-token")
        self.client = RecordingClient()
        self.state.client = self.client
        self.state.capabilities = ["relay_pulse_2s"]
        self.server = AdminServer(("127.0.0.1", 0), self.state)
        self.thread = threading.Thread(target=self.server.serve_forever, daemon=True)
        self.thread.start()
        self.base_url = f"http://127.0.0.1:{self.server.server_address[1]}"

    def tearDown(self):
        self.server.shutdown()
        self.server.server_close()
        self.thread.join(timeout=2)

    def test_open_endpoint_routes_cell_command(self):
        with urllib.request.urlopen(self.base_url + "/api/command/open?cell=1") as response:
            result = json.loads(response.read().decode("utf-8"))
        self.assertTrue(result["sent"])
        self.assertEqual("open_cell", self.client.messages[0]["kind"])
        self.assertEqual({"cellCode": "01"}, self.client.messages[0]["payload"])

    def test_open_endpoint_rejects_unknown_cell(self):
        with self.assertRaises(urllib.error.HTTPError) as error:
            urllib.request.urlopen(self.base_url + "/api/command/open?cell=11")
        self.assertEqual(400, error.exception.code)
        self.assertEqual([], self.client.messages)

    def test_close_endpoint_is_removed(self):
        with self.assertRaises(urllib.error.HTTPError) as error:
            urllib.request.urlopen(self.base_url + "/api/command/close?cell=1")
        self.assertEqual(404, error.exception.code)

    def test_close_command_is_not_accepted_by_local_api(self):
        request = urllib.request.Request(
            self.base_url + "/api/command",
            data=json.dumps({"kind": "close_cell", "cellCode": "01"}).encode("utf-8"),
            headers={"Content-Type": "application/json"},
        )
        with self.assertRaises(urllib.error.HTTPError) as error:
            urllib.request.urlopen(request)
        self.assertEqual(400, error.exception.code)
        self.assertEqual([], self.client.messages)

    def test_relay_commands_are_blocked_for_old_apk(self):
        self.state.capabilities = []
        with self.assertRaises(urllib.error.HTTPError) as error:
            urllib.request.urlopen(self.base_url + "/api/command/open?cell=1")
        self.assertEqual(409, error.exception.code)
        self.assertEqual([], self.client.messages)


if __name__ == "__main__":
    unittest.main()
