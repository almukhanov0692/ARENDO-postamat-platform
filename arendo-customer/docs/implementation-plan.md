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
- Product target: 28 installed cells across four seven-cell cabinets, with two 16-in/16-out modules
  providing 32 channels of each type. The proposed map is M1/ID 1 → D01-D16, M2/ID 2 → D17-D28;
  M2 channels 13-16 are reserve only and must not be shown as doors. Physical IDs/map remain to be
  commissioned.
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
| 1. Real I/O and cabinet scale | First bring up the 10-channel exhibition configuration, then extend and validate the formal 14-door stand. Implement a configurable logical-cell-to-I/O map, serialized Modbus access, module health and honest `open`/`closed`/`unknown` reporting. Agree the maximum number of simultaneous lock pulses and queue/rejection/expiry behavior before enabling multi-cell output. Validate power, cabling, failure isolation and restart behavior. H01-H07; T01-T05. | Exhibition demo reports only its 10 configured channels. Separately, 14 real cells pass T01; adding the second cabinet does not require reworking existing harnesses; 28-cell design and 32-channel method documented; approved concurrency limit and power/recovery evidence recorded. |
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

As of 2 October 2026, Stage 0 remains in progress. The repository contains a buildable Android gateway tested on INBOX710
(RK3399), a ten-channel
exhibition profile, a WebSocket device client and the temporary laptop bridge. The current
button/LED feedback is explicitly simulated; physical lock and door-sensor acceptance remains open.
The reported procurement list is 1 RK3568 / 4+32 GB controller, two BSM-1616RB boards, 34 locks
(28 + 6 spare), 400 m of 4-core cable, and a 12 V / 50 A / 600 W supply. Exact controller SKU,
antenna/LTE and speaker variants, delivery and final tax/reserve calculation are still open; see the
dated [procurement update](procurement/procurement-update-2026-09-30-ru.md). Electrical drawing E-000 summarizes the exhibition
stand; E-001/E-002 and the production drawing set are tracked in
[`hardware/electrical-r0/README.md`](hardware/electrical-r0/README.md).
The APK implements `hello`, heartbeat, `open_cell` (2-second software-scheduled output OFF in normal operation; independent cutoff is unverified),
`cell_report`, ACK and demo X-input door events. Backend storage, UI, server-side offline detection
and production credentials/signatures still need to be completed against
[`protocols/backend-v1.md`](protocols/backend-v1.md) and the backend handoff.
Durable event replay, technician grant/block flow, update/rollback, GPS, advertising/audio and full
product acceptance have not been demonstrated.

The gateway includes unit-tested routing for multiple logical modules, but the configured APK and
the physical production map remain separate commissioning work. The proposed channel table is
published as E-003 R0 for discussion, not installation. The next gate is to configure/verify unique
module IDs, check the actual feedback semantics on one lock, agree the backend JSON/session/ACK
contract, then continue the formal 14-real-door acceptance path.
The test matrix in
[`testing/acceptance-matrix.md`](testing/acceptance-matrix.md) tracks evidence using the original
T01-T26 identifiers from the specification.

### Update — 2 October 2026

The gateway now reports `unknown` instead of retaining stale feedback after a Modbus read failure or
port close, and it re-baselines inputs after recovery. The software regression passes; the hardware
part of T05 remains open. Before using real locks, separately close the output fail-safe and power/load
review gates recorded in the [full engineering audit](engineering-audit-2026-10-02-ru.md). See also
the [2 October status update](progress-report-2026-10-02-ru.md).
