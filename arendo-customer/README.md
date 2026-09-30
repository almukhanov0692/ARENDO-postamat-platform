# ARENDO postamat platform

Customer and backend-developer project area for the ARENDO postamat platform. The repository is
prepared for public review; do not commit access tokens, passwords, personal data or private signing
keys.

The repository is based on the technical specification v1.1 dated 21 September 2026. It intentionally separates the Android software running inside a postamat from the future Android technician application. The latest project snapshot is [30 September 2026](docs/progress-report-2026-09-30-ru.md).

## Repository status

MVP foundation validated on the laboratory four-cell stand. The project is not production-ready and
must not be used to operate public postamats yet. See
[`docs/implementation-status.md`](docs/implementation-status.md) for the current delivery status and
the remaining work, and [`docs/implementation-plan.md`](docs/implementation-plan.md) for the staged
implementation sequence and exit gates. The dated progress report is in
[`docs/progress-report-2026-09-30-ru.md`](docs/progress-report-2026-09-30-ru.md) (current); the
previous snapshot remains at [`docs/progress-report-2026-09-28-ru.md`](docs/progress-report-2026-09-28-ru.md).

## Components

- `device-android/` - Android gateway running inside the RK3568 postamat.
- `controller/` - hardware integration notes and future native controller services.
- `technician-android/` - reserved for the separate technician application defined in the specification.
- `docs/protocols/` - versioned backend and USB contracts.
- `docs/testing/` - acceptance traceability for the original tests T01-T26 in the specification.
- `docs/decisions/` - architecture decisions and unresolved choices.
- `docs/procurement/` - reported equipment list, dated estimate and open cost reconciliation.
- [`docs/procurement/procurement-update-2026-09-30-ru.md`](docs/procurement/procurement-update-2026-09-30-ru.md) - current quantities, accessory notes and audited price basis.
- [`docs/hardware/electrical-r0/E-003-cell-map-r0.md`](docs/hardware/electrical-r0/E-003-cell-map-r0.md) - proposed D01-D32 channel mapping; not an installation drawing.
- [`docs/protocols/backend-integration-audit-2026-09-30-ru.md`](docs/protocols/backend-integration-audit-2026-09-30-ru.md) - compatibility gaps between backend draft and current APK.
- [`docs/hardware/electrical-r0/`](docs/hardware/electrical-r0/README.md) - preliminary USB–RS485,
  Modbus, lock-power and feedback drawings; not yet an installation release.
- [`docs/procurement/clarification-01-cable-and-lock-quantity-ru.md`](docs/procurement/clarification-01-cable-and-lock-quantity-ru.md) - buyer response, equipment quantities, supplier cards and technical caveats.

Backend integration handoff and open questions: [`docs/protocols/backend-developer-handoff-ru.md`](docs/protocols/backend-developer-handoff-ru.md).

## Product boundary

The backend owns accounts, permissions, rentals, payments, business statuses, advertising assignments and release policy.

The postamat Android gateway executes authorized commands, reads device feedback and reports observed facts. Its local USB/RS-485 controller wiring is not part of the backend contract.

The technician application is a local service tool. In version 1, state-changing technician actions require a physical USB connection to the target postamat. Remote technician control is out of scope.

## Validated MVP slice

1. Buildable Android postamat gateway installed on the INBOX710 (RK3399) laboratory unit.
2. USB/RS-485 discovery and permission handling.
3. Logical-cell I/O routing in the device gateway, with unit-tested module addressing.
4. Demonstration door feedback for D01-D04 comes from X-input transitions, not relay output state.
   D05-D10 have no connected feedback and report `unknown`.
5. Versioned backend WebSocket contract and acceptance-test traceability.

The Android source includes the WebSocket device client and a temporary laptop bridge for integration
tests. The protocol flow is a development prototype; durable offline event replay, production
credentials and signed command handling remain unfinished. The button/LED simulator is not a physical
lock-and-door-sensor acceptance test and does not prove the final 28-cell configuration.

## Android build

Requirements:

- JDK 17
- Android SDK 35

From `device-android/`:

```powershell
.\gradlew.bat assembleDebug
```

The current source supports API 21 and newer. The connected INBOX710 laboratory unit reports API 25;
the proposed RK3568 production controller has not been commissioned here.
The final supported-device matrix remains an acceptance item.

## Security

Do not commit production tokens, signing keys, passwords, service grants, keystores or private customer data. See `SECURITY.md`.

## Source specification

The signed/commercial PDF remains outside the repository. Equipment quantities, reserve split,
supplier links and indicative prices are recorded in [`docs/procurement/equipment-estimate-2026-09.md`](docs/procurement/equipment-estimate-2026-09.md) and the linked procurement clarification.
