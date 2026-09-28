import base64
import socket
import threading
import unittest

from postamat_server import PostamatState, WebSocketRequestHandler, WebSocketServer


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


if __name__ == "__main__":
    unittest.main()
