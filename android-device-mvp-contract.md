# Android PLC — MVP device contract

This is the current demo contract implemented by the Android APK.

## Hardware mapping

| Cell | Button input | LED/relay output |
|---|---:|---:|
| `1` | `X1` | `Y1` |
| `2` | `X2` | `Y2` |
| `3` | `X3` | `Y3` |
| `4` | `X4` | `Y4` |

For this MVP, the LED represents an open door and a button press represents a closed door.
There is no real lock feedback or door sensor yet.

## WebSocket connection

The APK connects to:

```text
<configured URL>/v1/device/socket
```

The device token is sent in the `X-Device-Token` HTTP header. The APK reconnects after a
connection loss with an increasing delay up to 30 seconds and jitter.

The URL, `postamat_id`, token, Modbus slave address, baud rate, and X/Y start addresses are
configurable in the administrator screen.

## Device → server

### `hello`

```json
{
  "type": "hello",
  "postamatId": "demo-postamat-1",
  "appVersion": "0.1.0",
  "keyVersion": 0,
  "capabilities": ["locks", "buttons", "indicators", "demo_buttons_as_doors"],
  "cells": [
    {"code": "1", "door": "closed", "lock": "unknown"}
  ],
  "net": {"kind": "wifi"}
}
```

### `heartbeat`

Sent every 15 seconds while the WebSocket is active. It contains the same `cells`, `net`, and
an empty `problems` array.

### Physical demo events

When `open_cell` is successfully executed:

```json
{
  "type": "event",
  "kind": "door_opened",
  "cellCode": "1",
  "detail": {"source": "demo_relay", "simulated": true}
}
```

When the corresponding physical button is pressed, the APK switches the relay off and sends:

```json
{
  "type": "event",
  "kind": "door_closed",
  "cellCode": "1",
  "detail": {"source": "button", "simulated": true}
}
```

## Server → device

### `open_cell`

```json
{
  "type": "command",
  "id": "command-123",
  "kind": "open_cell",
  "payload": {"cellCode": "1"},
  "expiresAt": "2026-09-19T12:00:00Z"
}
```

The APK validates the cell and expiry, writes the corresponding Modbus relay `Y`, then sends:

```json
{
  "type": "ack",
  "commandId": "command-123",
  "ok": true,
  "result": {"kind": "door_open_ack", "cellCode": "1"}
}
```

An expired, invalid, or Modbus-disconnected command is rejected with `ok: false` and an error
such as `expired`, `invalid_cell`, or `modbus_offline`.

### `confirm_closed`

The command is acknowledged as `door_close_ack` when the demo relay is already off. Otherwise
the APK returns `door_not_closed`.

### `cell_report`

The APK returns an `ack` containing the current `cells` array.

Other protocol commands (`reboot`, `update_app`, `rotate_token`, and `sync_keys`) currently
return `unsupported_command` because they are outside this week’s hardware MVP.

## Current limitations

- `door` is simulated from the relay state; it is not a physical door sensor.
- `lock` is reported as `unknown`; the current relay board has no lock feedback.
- Offline QR opening is not implemented in this APK.
- HMAC command-signature verification needs the backend’s canonical-signature rules and device
  secret format before it can be enabled safely.
- The exact Modbus register map remains configurable because the board manual/register table has
  not yet been provided.
