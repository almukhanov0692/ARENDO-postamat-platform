# Implementation plan — ARENDO technical specification v1.1

This is the working sequence for implementing the technical specification dated 21 September
2026. The specification remains authoritative; this plan makes dependencies, evidence and release
gates explicit. A prototype or simulator result is not a production acceptance result.

## Delivery boundary

- First physical acceptance stand: one master section and two seven-cell modules — 14 real locks
  with door sensors.
- Interim exhibition build: 10 relay channels, approved by the customer according to Nursultan.
  This is a demonstration configuration only and does not replace the 14-door acceptance stand or
  the 28-door product target.
- Product target: up to 28 installed cells across four seven-cell cabinets, with capacity for 32
  logical channels. Four reserve channels are not installed cells and must not be shown as doors.
- Device platform: RK3568-class Android controller, USB host, USB-RS485 and Modbus RTU. Exact I/O
  module SKUs, topology and production register map still require approval.
- The four-button/LED setup is a simulator only. It cannot prove real lock actuation or door-sensor
  accuracy.
- The backend owns rentals, payments, user permissions and business availability. The device
  executes authorized commands and reports observed facts.
- Server-facing payloads use stable logical identifiers (`postamatId`, `cellCode`, command/event
  IDs). USB, Modbus slave IDs, X/Y terminals and serial settings stay inside the device gateway.

## Stages and exit gates

| Stage | Work and specification trace | Exit gate |
|---|---|---|
| 0. Freeze the baseline | Reconcile the source requirements, current code, hardware list and server contract. Resolve the I/O module models/count, topology, Modbus addressing/polarity, sensor behavior, device identity and command security. | Approved hardware bill of materials and wiring/map appendix; versioned server contract and named owners for unresolved decisions. No guessed production addresses. |
| 1. Real I/O and cabinet scale | First bring up the 10-channel exhibition configuration, then extend and validate the formal 14-door stand. Implement a configurable logical-cell-to-I/O map, serialized Modbus access, module health and honest `open`/`closed`/`unknown` reporting. Validate power, cabling, failure isolation and restart behavior. H01-H07; T01-T05. | Exhibition demo reports only its 10 configured channels. Separately, 14 real cells pass T01; adding the second cabinet does not require reworking existing harnesses; 28-cell design and 32-channel method documented; power/recovery evidence recorded. |
| 2. Device ↔ backend contract | Integrate the WebSocket client into the canonical Android gateway. Implement device authentication, command lifecycle, expiry/session checks, idempotency, heartbeat/offline status, durable event queue, acknowledgements, conflict serialization and agreed Wi-Fi/4G behavior. C01-C09; protocol appendix; T13-T16. | Backend and device pass end-to-end command → physical output → sensor state → acknowledged event, plus duplicate, expiry, offline, reconnect and stale-command cases. |
| 3. Technician USB and service mode | Build the separate technician Android app; server login/assignment and renewable device-specific 24-hour grant; mutual USB identity; inspection mode; return flow; local blocks; service reason/audit; safe USB disconnect/restart handling. A01-A07, M01-M09; T06-T12 and T15. | Authorized and unauthorized USB cases pass; entering/leaving inspection, return during inspection, offline service, persistent blocks and command recovery pass on supported Android devices. |
| 4. Device services | Nursultan implements device-side GPS/location acquisition and reporting; Sarvar agrees the server receive/binding API when the payload is ready. Also implement signed update selection/install/rollback/recovery, bundled voice prompts with audio priority, and cached advertising playlist/fallback. U01-U06, V01-V02, R01-R04, location requirement; T17-T23. | Each service passes its offline, interrupted-power/network and recovery cases on target hardware; unsupported data is reported as unavailable, not fabricated. |
| 5. Release and handover | Reproducible source builds, first full cabinet assembly, documentation, test records, customer-controlled access/identity provisioning and training. D01-D06; T24-T26. | A second specialist can build, install, configure and acceptance-test the system using the handed-over repository, credentials process and documents. |

## Work that can proceed in parallel

- Stage 0 hardware/map decisions and backend message-contract decisions can be reviewed together.
- Backend server implementation can proceed against the logical protocol without waiting for Modbus
  details; it must not encode X/Y terminals or serial settings.
- Android USB/service UI can be prototyped separately, but production authorization and service
  actions remain gated on the approved grant and USB handshake.
- Media, GPS and update implementation must not delay the first real-door feedback slice, but remain
  required before final acceptance unless the customer approves a scope change.

## Current position

Stage 0 is in progress. The repository contains a buildable Android gateway tested on INBOX710
(RK3399), a ten-channel
exhibition profile, a WebSocket device client and the temporary laptop bridge. The current
button/LED feedback is explicitly simulated; physical lock and door-sensor acceptance remains open.
The procurement estimate is documented. Electrical drawing E-000 now summarizes the exhibition
stand; E-001/E-002 and the remaining production drawing set are tracked in
[`hardware/electrical-r0/README.md`](hardware/electrical-r0/README.md).
The APK implements `hello`, heartbeat, `open_cell` (2-second output pulse with automatic OFF),
`cell_report`, ACK and demo X-input door events. Backend storage, UI, server-side offline detection
and production credentials/signatures still need to be completed against
[`protocols/backend-v1.md`](protocols/backend-v1.md) and the backend handoff.
Durable event replay, technician grant/block flow, update/rollback, GPS, advertising/audio and full
product acceptance have not been demonstrated.

The gateway includes unit-tested routing for multiple logical modules, but the configured APK and
the physical production map remain separate commissioning work. No routing fixture is an approved
cabinet wiring plan. The next gate is joint agreement on the backend status/command contract and
physical verification of the exhibition I/O, followed by the formal 14-real-door acceptance path.
The test matrix in
[`testing/acceptance-matrix.md`](testing/acceptance-matrix.md) tracks evidence using the original
T01-T26 identifiers from the specification.
