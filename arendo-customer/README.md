# ARENDO postamat platform

Clean customer-facing source repository for the ARENDO postamat platform.

The repository is based on the technical specification v1.1 dated 21 September 2026. It intentionally separates the Android software running inside a postamat from the future Android technician application.

## Repository status

Early foundation. The project is not production-ready and must not be used to operate public postamats yet.

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

## First delivery slice

1. Buildable Android postamat gateway.
2. USB-RS485 discovery and permission handling.
3. Modbus RTU mapping from cells to inputs and outputs.
4. Versioned WebSocket device protocol.
5. Acceptance-test traceability and public review workflow.

## Android build

Requirements:

- JDK 17
- Android SDK 35

From `device-android/`:

```powershell
.\gradlew.bat assembleDebug
```

The minimum Android version is provisionally API 26. It remains an explicit decision to confirm against the target device list.

## Security

Do not commit production tokens, signing keys, passwords, service grants, keystores or private customer data. See `SECURITY.md`.

## Source specification

The signed/commercial PDF is kept outside this public-ready repository because it contains names, pricing and contractual conditions. `docs/requirements.md` contains the technical traceability needed by developers.
