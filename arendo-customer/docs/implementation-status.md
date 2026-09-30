# ARENDO implementation status

Status date: 30 September 2026
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
- Debug APK `0.2.1` was installed on the INBOX710 (RK3399) Android laboratory unit running API 25.
  The proposed RK3568 production unit requires separate commissioning.

### Hardware stand

- The Android gateway communicates with the controller locally over USB/RS-485. This physical
  interface is configured and commissioned on the device side; the backend does not address
  controller terminals or Modbus points.
- The exhibition software profile has ten logical output channels and four feedback channels. A
  door without reliable feedback must be reported as `unknown`; a relay/output command alone is not
  evidence that a physical door opened.
- Modbus frame/response validation and serialized background I/O are implemented. Detailed
  controller mapping remains a device-side commissioning item, separate from the backend contract.

### Electrical drawing update — 28 September 2026

- Added a clear overview drawing for the current ten-output/four-input stand: Android USB → USB-RS485,
  `A→A` / `B→B`, relay-switched positive to each lock, shared lock return, and feedback inputs `X1-X4`.
- Recorded the BSM input polarity rule from its manual: with `S/S` tied to `GND`, an input is active
  on a positive signal; with `S/S` tied to module `VCC`, it is active on a negative signal. The lock's
  two feedback wires still need checking on the actual part before their exact connection is assigned.
- The drawing register now names the remaining terminal map, power/protection, cabinet/cable, and AC/PE
  drawings. Procurement quantities and indicative prices are already documented separately.
- This improves the hardware handoff but does not change any physical acceptance result: no real lock,
  28-cell cabinet, or production wiring has passed commissioning yet.

### Ten-output / four-input demonstration behavior

- An `open_cell` command pulses one output for 2 seconds, then automatically switches it OFF. Output
  state is not treated as door position. Available X-input feedback determines `open`/`closed` for
  D01-D04; D05-D10 remain `unknown` without feedback.
- On the current wiring, active X is interpreted as `closed`, inactive X as `open`. Applying +12 V to
  X1-X4 was observed to update both the APK and local monitor. This proves the demo input path, not
  the polarity or semantics of the ordered lock's feedback pair. An open circuit can also result
  from a broken wire, so it must not be treated as a verified physical opening.
- The stand is a simulator. It does not prove a production lock sensor or a real door sensor.
- The customer has approved 10 relay channels for the interim exhibition build, according to
  Nursultan. The canonical Android app is configured for ten outputs and four input-feedback points;
  real-lock behavior and final feedback assignment still need verification. This demo does not meet
  T01's 14-real-door requirement or replace the 28-door product target.
- The 28-cell product configuration is not yet commissioned on physical equipment. The current
  simulator and software tests do not prove the final controller layout or door feedback.

### Backend integration prototype

The current Android source includes the device-side WebSocket client and the local laptop bridge:

- The app sends the device token in `X-Device-Token`; the token is not logged or saved with URL settings.
- `hello`/`welcome`, heartbeat, `open_cell`, `cell_report`, acknowledgements and demo X-input door events are implemented in the Android source.
- `open_cell` energizes one mapped output for 2 seconds, then switches it OFF. The ACK is not a physical door-state confirmation. There is no remote close command; a person closes the door physically.
- Door state is derived from the available feedback inputs; the current button-as-door inputs remain simulated. D05-D10 have no feedback input and stay `unknown`.
- The temporary laptop dashboard has per-cell opening, sequential open-all and status-report actions. It does not expose a close command.
- `map-7` is the current temporary demo `postamatId`, not a category, cabinet number, or Modbus address; backend must provide the production identity.
- Updated safe-pulse APK is version `0.2.1`; the local dashboard blocks relay commands from older APKs that do not advertise `relay_pulse_2s`.
- Reconnect uses bounded backoff. Processed command IDs are persisted locally to prevent repeating an open operation.
- The temporary bridge requires a matching token. Its command/dashboard API binds to laptop loopback; only the WebSocket port is intended for LAN/ngrok testing.
- Android unit tests and debug APK build pass. The connected APK registered through WebSocket and
  returned heartbeat/ACK for `cell_report`; applying +12 V to X1-X4 was observed to update the APK
  and local monitor. This is not acceptance of the ordered lock's feedback contact or the full
  physical I/O-to-backend chain.

The canonical message contract is documented in [`protocols/backend-v1.md`](protocols/backend-v1.md).
The laptop bridge remains an in-memory development tool, not Sarvar's production server.
The backend developer's latest draft has not yet been proven compatible with this APK. In particular,
the nested-versus-flat command envelope, `sessionId` echo, exact ACK/event fields and the distinction
between relay-pulse acceptance and sensor-confirmed door state need agreement. See the
[`30 September compatibility audit`](protocols/backend-integration-audit-2026-09-30-ru.md).

### Procurement and 28-cell routing — 30 September 2026

- Reported procurement configuration: RK3568 controller, 4 GB RAM / 32 GB storage; two
  BSM-1616RB 16-in/16-out modules; 34 locks (28 + 6 spare); 400 m 4-core cable; and 12 V / 50 A /
  600 W supply. The exact controller variant, LTE module/antenna and speaker SKU remain to be
  confirmed against the order.
- One shared RS-485 bus is planned. Proposed software mapping: module 1 / ID 1 → D01-D16;
  module 2 / ID 2 → D17-D28; the remaining four points on module 2 are spares. This map is covered
  by software routing tests but has not been set on or commissioned with physical boards.
- The two-second relay pulse is implemented. The requested 30-second wait for actual feedback is
  not yet implemented. Feedback contact meaning and polarity must be measured on the selected lock;
  the current X-input demo cannot establish physical door position.
- The 28 September KZT estimate is historical. A 30 September draft total contained inconsistent
  line-item conversions and is not treated as a final payable amount. The current auditable CNY
  product subtotal and unresolved tax/logistics basis are recorded in the
  [procurement update](procurement/procurement-update-2026-09-30-ru.md).

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
7. Confirm the supported Android device matrix, USB-RS485 VID/PID list, exact RK3568 controller
   variant and commissioned Modbus register/cell map.
8. Implement the technician/service application and its signed, postamat-specific service grant.
   Bluetooth is a future secured service transport; it is not a replacement for the device WebSocket.
9. GPS hardware/data acquisition and device-side location reporting are Nursultan's scope. The
   backend should agree the receive/binding API when the device payload is ready. Advertising/audio,
   signed updates and rollback remain unimplemented.
10. Implement the 30-second wait for lock feedback after an opening command; absent feedback must
    remain `unknown` and be logged as a timeout, not reported as a successful opening.

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
- route `open_cell` only to the matching `postamatId` and `cellCode`; treat ACK separately from sensor-confirmed door state;
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
