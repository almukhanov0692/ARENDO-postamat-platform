# ADR 0002: USB transport to the I/O module

- Status: Accepted for the current stand
- Date: 2026-09-24

## Decision

The Android gateway on RK3568 acts as USB host. A USB-RS485 converter connects it to the I/O module, and the application communicates with that module using Modbus RTU.

This decision concerns the internal hardware bus only. It does not select the future physical transport between a technician phone and the postamat.

## Required validation

- Approve the production converter chipset and VID/PID.
- Confirm the production module register map and signal polarity.
- Test Type-C hubs, USB permission flow, detach/reconnect and application restart.
- Confirm power and connector durability.

The backend protocol remains independent of Modbus and USB details.
