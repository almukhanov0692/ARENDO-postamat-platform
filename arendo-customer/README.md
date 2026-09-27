# ARENDO postamat platform

Clean customer-facing source repository for the ARENDO postamat platform.

The repository is based on the technical specification v1.1 dated 21 September 2026. It intentionally separates the Android software running inside a postamat from the future Android technician application.

## Repository status

MVP foundation validated on the laboratory four-cell stand. The project is not production-ready and
must not be used to operate public postamats yet. See
[`docs/implementation-status.md`](docs/implementation-status.md) for the current delivery status and
the remaining work.

## Components

- `device-android/` - Android gateway running inside the RK3568 postamat.
- `controller/` - hardware integration notes and future native controller services.
- `technician-android/` - reserved for the separate technician application defined in the specification.
- `docs/protocols/` - versioned backend and USB contracts.
- `docs/testing/` - acceptance traceability for tests T01-T26.
- `docs/decisions/` - architecture decisions and unresolved choices.

## Product boundary

The backend owns accounts, permissions, rentals, payments, business statuses, advertising assignments and release policy.

The postamat Android gateway executes authorized commands, reads actual device state over USB-RS485/Modbus RTU, stores events while offline and reports facts.

The technician application is a local service tool. In version 1, state-changing technician actions require a physical USB connection to the target postamat. Remote technician control is out of scope.

## Validated MVP slice

1. Buildable Android postamat gateway installed on the RK3568 test unit.
2. USB-RS485 discovery and permission handling.
3. Modbus RTU mapping for the four-cell demonstration stand: `X1..X4` and `Y1..Y4`.
4. Door-state simulation: LED/relay ON is treated as `open`; the corresponding button/input
   transition is treated as `closed`.
5. Versioned backend WebSocket contract and acceptance-test traceability.

The working WebSocket/local-server prototype was validated separately during the MVP demonstration.
The production-safe transport, durable offline queue and signed command handling still have to be
merged into this canonical customer-facing Android module.

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

The signed/commercial PDF is kept outside this public-ready repository because it contains names, pricing and contractual conditions. `docs/requirements.md` contains the technical traceability needed by developers.
