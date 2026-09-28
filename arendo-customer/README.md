# ARENDO postamat platform

Customer and backend-developer project area for the ARENDO postamat platform. The GitHub repository
is private; grant access only to project collaborators who need the source.

The repository is based on the technical specification v1.1 dated 21 September 2026. It intentionally separates the Android software running inside a postamat from the future Android technician application.

## Repository status

MVP foundation validated on the laboratory four-cell stand. The project is not production-ready and
must not be used to operate public postamats yet. See
[`docs/implementation-status.md`](docs/implementation-status.md) for the current delivery status and
the remaining work, and [`docs/implementation-plan.md`](docs/implementation-plan.md) for the staged
implementation sequence and exit gates.

## Components

- `device-android/` - Android gateway running inside the RK3568 postamat.
- `controller/` - hardware integration notes and future native controller services.
- `technician-android/` - reserved for the separate technician application defined in the specification.
- `docs/protocols/` - versioned backend and USB contracts.
- `docs/testing/` - acceptance traceability for the original tests T01-T26 in the specification.
- `docs/decisions/` - architecture decisions and unresolved choices.
- `docs/procurement/` - approved equipment list and indicative cost estimate.

## Product boundary

The backend owns accounts, permissions, rentals, payments, business statuses, advertising assignments and release policy.

The postamat Android gateway executes authorized commands, reads device feedback and reports observed facts. Its local USB/RS-485 controller wiring is not part of the backend contract.

The technician application is a local service tool. In version 1, state-changing technician actions require a physical USB connection to the target postamat. Remote technician control is out of scope.

## Validated MVP slice

1. Buildable Android postamat gateway installed on the RK3568 test unit.
2. USB/RS-485 discovery and permission handling.
3. Logical-cell I/O routing in the device gateway, with unit-tested module addressing.
4. Door-state simulation: LED/output ON is treated as `open`; the corresponding button/input
   transition is treated as `closed`.
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

The current source supports API 21 and newer. The connected RK3568 test unit reports API 25.
The final supported-device matrix remains an acceptance item.

## Security

Do not commit production tokens, signing keys, passwords, service grants, keystores or private customer data. See `SECURITY.md`.

## Source specification

The signed/commercial PDF remains outside the repository. The approved equipment and indicative
prices are summarized in [`docs/procurement/equipment-estimate-2026-09.md`](docs/procurement/equipment-estimate-2026-09.md);
the document intentionally does not publish order quantities or reserve split.
