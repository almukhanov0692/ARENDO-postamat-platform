# ARENDO implementation status

Status date: 28 September 2026
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

- The Android gateway communicates with the controller locally over USB/RS-485. This physical
  interface is configured and commissioned on the device side; the backend does not address
  controller terminals or Modbus points.
- The exhibition software profile has ten logical output channels and four feedback channels. A
  door without reliable feedback must be reported as `unknown`; a relay/output command alone is not
  evidence that a physical door opened.
- Modbus frame/response validation and serialized background I/O are implemented. Detailed
  controller mapping remains a device-side commissioning item, separate from the backend contract.

### Four-button demonstration behavior

- LED/output ON is the simulator's `open` indication; pressing the corresponding button changes the
  simulated state to `closed` and switches the output off.
- The stand is a simulator. It does not prove a production lock sensor or a real door sensor.
- The customer has approved 10 relay channels for the interim exhibition build, according to
  Nursultan. The canonical Android app is configured for ten outputs and four input-feedback points;
  physical output behavior and the final feedback assignment still need verification. This demo
  does not meet T01's
  14-real-door requirement or replace the 28-door product target.
- The 28-cell product configuration is not yet commissioned on physical equipment. The current
  simulator and software tests do not prove the final controller layout or door feedback.

### Backend integration prototype

The current Android source includes the device-side WebSocket client and the local laptop bridge:

- The app sends the device token in `X-Device-Token`; the token is not logged or saved with URL settings.
- `hello`/`welcome`, heartbeat, `open_cell`, `cell_report`, acknowledgements and demo door events are implemented.
- Reconnect uses bounded backoff. Processed command IDs are persisted locally to prevent repeating an open operation.
- The temporary bridge requires a matching token. Its command/dashboard API binds to laptop loopback; only the WebSocket port is intended for LAN/ngrok testing.
- Android unit tests and debug APK build pass. The demonstration log showed WebSocket registration
  and heartbeats; Modbus read timeouts remain in the log, so complete physical I/O-to-backend
  acceptance is still outstanding.

The canonical message contract is documented in [`protocols/backend-v1.md`](protocols/backend-v1.md).
The laptop bridge remains an in-memory development tool, not Sarvar's production server.

## Not production-ready yet

These items must not be presented as completed:

1. Add production configuration storage and server-issued credentials.
2. Add durable offline event storage and replay with stable `eventId`; agree final idempotency and
   command-signature rules with the backend.
3. Finish production authentication, token rotation/revocation and canonical command verification.
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
- display `D1 open` / `D1 closed` (and the same format for each configured cell) from device
  events/heartbeats; show `unknown` when feedback is unavailable;
- mark a device offline after the negotiated heartbeat timeout;
- never replay an expired opening command after reconnect;
- acknowledge and store events without double-counting them.

See the protocol and test documents before changing field names:

- [`protocols/backend-v1.md`](protocols/backend-v1.md)
- [`protocols/backend-developer-handoff-ru.md`](protocols/backend-developer-handoff-ru.md)
- [`testing/acceptance-matrix.md`](testing/acceptance-matrix.md)

## Evidence and scope note

The four-button stand is valid evidence for a simulated logical flow `command -> output -> input ->
status` only. It is not evidence for T01's 14 real doors or production lock security, door-sensor
accuracy, payment processing, QR authorization, GPS accuracy, Bluetooth authorization or update
rollback. Those require their own hardware, backend and acceptance tests.
