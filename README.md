# ARENDO postamat platform

Public project repository for the ARENDO postamat device software, backend integration contract,
test bridge, delivery status and equipment estimate.

Current device-gateway demo build: **0.2.2-demo** (2 October 2026). It is not production-accepted.
See [changes](CHANGELOG.md) and the [2 October status report](arendo-customer/docs/progress-report-2026-10-02-ru.md).

## Project materials

The canonical Android gateway is under `arendo-customer/device-android/`. The root `app/` module is
an older prototype retained for history; do not use it for current builds or changes.

- [Customer/backend project source](arendo-customer/README.md)
- [Implementation status and remaining work](arendo-customer/docs/implementation-status.md)
- [Backend WebSocket protocol](arendo-customer/docs/protocols/backend-v1.md)
- [Current demo WebSocket exchange, with JSON examples](arendo-customer/docs/protocols/demo-websocket-handoff-ru.md)
- [Ready-to-send backend developer handoff and questions](arendo-customer/docs/protocols/backend-developer-handoff-ru.md)
- [Equipment and indicative prices](arendo-customer/docs/procurement/equipment-estimate-2026-09.md)
- [30 September project status and decisions](arendo-customer/docs/progress-report-2026-09-30-ru.md)
- [2 October project status and T05 software correction](arendo-customer/docs/progress-report-2026-10-02-ru.md)
- [Full engineering audit and next gates (2 October)](arendo-customer/docs/engineering-audit-2026-10-02-ru.md)
- [Proposed D01-D32 Modbus routing sheet (not an installation drawing)](arendo-customer/docs/hardware/electrical-r0/E-003-cell-map-r0.md)
- [Backend draft compatibility audit](arendo-customer/docs/protocols/backend-integration-audit-2026-09-30-ru.md)
- [Preliminary electrical drawings R0 — not for installation](arendo-customer/docs/hardware/electrical-r0/README.md)
- [Full procurement clarification and supplier links](arendo-customer/docs/procurement/clarification-01-cable-and-lock-quantity-ru.md)
- [Temporary laptop WebSocket bridge](local-server/README.md)

The current four-button/LED stand is a demonstration simulator, not a production locker. The
repository distinguishes tested behavior from proposed hardware configuration and remaining
acceptance work.
