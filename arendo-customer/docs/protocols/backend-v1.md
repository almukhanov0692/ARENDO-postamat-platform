# Backend device protocol v1

Status: draft compatible with the working MVP.

## Transport and authentication

- WebSocket endpoint: configurable, normally `/v1/device/socket`.
- Production transport: `wss` with a valid certificate.
- Device token: `X-Device-Token` during the HTTP upgrade.
- The backend stores only a hash of the token and may disable or rotate it.
- Tokens and credentials are never written to application logs.

The backend addresses business entities (`postamatId`, `cellCode`). It does not know Modbus addresses, X inputs, Y outputs, baud rate or USB converter details.

## Device to backend

### Hello

Sent once after each connection.

```json
{
  "type": "hello",
  "protocolVersion": 1,
  "postamatId": "map-7",
  "appVersion": "0.1.0",
  "capabilities": ["locks", "door_sensor", "offline_events"],
  "cells": [
    {"code": "01", "door": "closed", "lock": "unknown"}
  ],
  "net": {"kind": "wifi"}
}
```

### Heartbeat

Sent at the interval returned in `welcome`. It contains current cell facts, connectivity and active problems.

```json
{
  "type": "heartbeat",
  "postamatId": "map-7",
  "sentAt": "2026-09-24T12:00:00Z",
  "cells": [{"code": "01", "door": "closed", "lock": "unknown"}],
  "net": {"kind": "wifi"},
  "problems": []
}
```

### Event

Events are facts observed by the device. Each event has a stable ID so the backend can acknowledge duplicates safely.

```json
{
  "type": "event",
  "eventId": "01J8ABC...",
  "kind": "door_closed",
  "postamatId": "map-7",
  "cellCode": "01",
  "occurredAt": "2026-09-24T12:00:04Z",
  "detail": {"source": "sensor"}
}
```

Initial event kinds: `door_opened`, `door_closed`, `door_not_closed`, `device_online`, `device_offline_detected`, `modbus_online`, `modbus_offline`, `cell_blocked`, `cell_unblocked`.

## Backend to device

### Welcome

```json
{
  "type": "welcome",
  "postamatId": "map-7",
  "config": {"heartbeatSec": 15, "pingSec": 20, "pongWaitSec": 45}
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

Version 1 commands:

| Command | Required payload | Device behavior |
|---|---|---|
| `open_cell` | `cellCode` | Validate, activate the mapped output and return an acknowledgement. |
| `cell_report` | none | Return current cells without changing hardware. |
| `confirm_closed` | `cellCode` | Confirm only from the current physical input state. |
| `block_cell` | `cellCode`, `reason` | Persist a service block; opening must be refused. |
| `unblock_cell` | `cellCode`, `reason` | Remove a block if the command is authorized. |

### Acknowledgement

```json
{
  "type": "ack",
  "commandId": "cmd-123",
  "ok": true,
  "result": {"kind": "open_command_accepted", "cellCode": "01"}
}
```

Stable error codes: `expired`, `wrong_postamat`, `invalid_cell`, `blocked`, `modbus_offline`, `door_not_closed`, `unsupported_command`, `internal_error`.

## Safety and reconnect rules

- `id` is globally unique and mandatory for every state-changing command.
- The device rejects an expired command before touching hardware.
- A duplicate command returns the stored result and never repeats the physical action.
- Commands from a closed WebSocket session are not queued in memory for execution after reconnect.
- A backend timeout must not be interpreted as proof that a door did not open; the final state is reconciled from events and `cell_report`.
- Events created offline are retained and resent with the same `eventId` until acknowledged.

## Minimal backend responsibilities

1. Authenticate and register each postamat connection.
2. Route a command by `postamatId` and `cellCode`.
3. Persist command lifecycle: created, delivered, acknowledged, expired or failed.
4. Persist latest device and cell status plus immutable events.
5. Mark a postamat offline after the negotiated heartbeat timeout.
6. Never resend an expired opening command after the device reconnects.

