# ADR 0003: Android device gateway on RK3568

- Status: Accepted
- Date: 2026-09-24

## Decision

Run the first postamat controller application as an Android APK on the RK3568 platform. The APK is the boundary between the backend WebSocket contract and the local USB-RS485/Modbus RTU hardware bus.

## Consequences

- Modbus addresses and serial settings stay local to the device.
- The backend uses stable `postamatId` and `cellCode` identifiers only.
- USB permission, serial polling and physical writes run outside the UI thread.
- A later migration to a native/background service must preserve the same backend and cell-state contracts.

