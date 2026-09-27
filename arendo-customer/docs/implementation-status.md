# ARENDO implementation status

Status date: 27 September 2026
Repository: `almukhanov0692/ARENDO-postamat-platform`

This document is the handoff summary for the customer and backend team. It separates the parts that
were demonstrated on the laboratory stand from the work that is still required before production.

The staged implementation sequence and exit gates are in
[`implementation-plan.md`](implementation-plan.md). Acceptance IDs in the matrix follow the source
specification; they are not replaced with locally invented test numbering.

## Demonstrated and ready for review

### Repository and source foundation

- The official customer-facing repository is `ARENDO-postamat-platform`.
- The source is separated into the Android device gateway, controller notes, versioned protocols,
  decisions and acceptance tests.
- The Android project builds reproducibly from `arendo-customer/device-android/`.
- A debug APK was installed on the RK3568 Android test unit running API 25.

### Hardware stand

- Android gateway connects to the I/O module through USB host and a USB-RS485 converter.
- Baseline serial settings are slave `1`, `9600 8N1`.
- The supplied BSM-1616RB manual matches the baseline and documents 16 inputs and 16 outputs;
  one module has enough channel count for the first 14-door stand. The manual's model row describes
  outputs generically as relay/transistor, so the physical output implementation and revision must
  be confirmed before connecting lock loads. Its B2 relay diagram uses grouped dry contacts, not
  powered Y outputs.
- Four-cell mapping is fixed for the current stand:

  | Cell | Physical input | Physical output |
  |---|---|---|
  | 01 | X1 | Y1 |
  | 02 | X2 | Y2 |
  | 03 | X3 | Y3 |
  | 04 | X4 | Y4 |

- Modbus RTU function `0x02` reads the inputs and function `0x05` controls the outputs.
- CRC validation, response-function validation and write-echo validation are implemented.
- Only one serial transaction is allowed at a time; polling runs outside the Android UI thread.

### Four-button demonstration behavior

- `Y1..Y4`/LED ON is the demonstration meaning of `D1..D4 open`.
- A transition on the corresponding `X1..X4` button is the demonstration meaning of
  `D1..D4 closed`.
- The output is switched off after the close transition.
- The stand is a simulator. It does not prove a production lock sensor or a real door sensor.

### Backend integration prototype

The working local prototype was tested separately from the clean customer-facing source tree:

- WebSocket connection with a device token header.
- `hello`/`welcome` registration exchange.
- Heartbeat and cell-state feedback.
- Addressed cell command and acknowledgement.
- Reconnect after connection loss.
- Windows local WebSocket monitor for sending test commands and viewing device status.

The canonical message contract is documented in [`protocols/backend-v1.md`](protocols/backend-v1.md).
No production token, certificate exception, private key or local machine address is part of this
repository.

## Not production-ready yet

These items must not be presented as completed:

1. Merge the tested WebSocket client into the canonical `kz.arendo.device` module with a production
   configuration store.
2. Add durable offline event storage, replay with stable `eventId`, command idempotency and a safe
   reconnect rule that cannot execute an old `open_cell` command.
3. Finish backend authentication, token rotation/revocation and canonical command signature rules.
4. Replace the button/LED simulation with the approved lock and physical door sensor, then validate
   `open`, `closed`, `door_not_closed`, `unknown` and hardware-fault states.
5. Complete server-side command lifecycle and offline detection: created, delivered, acknowledged,
   expired, failed and device offline.
6. Run the acceptance cases for invalid/expired/duplicate commands, Internet loss, Modbus loss and
   offline event replay.
7. Confirm the supported Android device matrix, USB-RS485 VID/PID list and final Modbus register map.
8. Implement the technician/service application and its signed, postamat-specific service grant.
   Bluetooth is a future secured service transport; it is not a replacement for the device WebSocket.
9. Decide and implement GPS/manual location binding, advertising/audio, signed updates and rollback.

## Backend developer handoff

The backend must work only with these business identifiers:

- `postamatId` — the postamat identity;
- `cellCode` — the logical cell identity (`01`, `02`, `03`, `04` for the current stand);
- command `id` — globally unique and required for every state-changing command.

The backend must not depend on `X1`, `Y1`, baud rate, slave address or USB-converter details. Those are
local gateway configuration.

Minimum server behavior:

- authenticate the device and accept one `hello` per connection;
- send `welcome` with heartbeat/ping timing;
- route `open_cell` only to the matching `postamatId` and `cellCode`;
- persist and deduplicate command results;
- display `D1 open`, `D1 closed` (and the same for D2-D4) from device events/heartbeats;
- mark a device offline after the negotiated heartbeat timeout;
- never replay an expired opening command after reconnect;
- acknowledge and store events without double-counting them.

See the protocol and test documents before changing field names:

- [`protocols/backend-v1.md`](protocols/backend-v1.md)
- [`protocols/modbus-gateway-v1.md`](protocols/modbus-gateway-v1.md)
- [`testing/acceptance-matrix.md`](testing/acceptance-matrix.md)

## Evidence and scope note

The four-button stand is valid evidence for a simulated logical flow `command -> output -> input ->
status` only. It is not evidence for T01's 14 real doors or production lock security, door-sensor
accuracy, payment processing, QR authorization, GPS accuracy, Bluetooth authorization or update
rollback. Those require their own hardware, backend and acceptance tests.
