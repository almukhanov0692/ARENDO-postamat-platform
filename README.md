# ARENDO postamat platform

Public project repository for the ARENDO postamat device software, backend integration contract,
test bridge, delivery status and equipment estimate.

Current device-gateway release: **0.2.1** (28 September 2026). See [changes](CHANGELOG.md).

## Project materials

- [Customer/backend project source](arendo-customer/README.md)
- [Implementation status and remaining work](arendo-customer/docs/implementation-status.md)
- [Backend WebSocket protocol](arendo-customer/docs/protocols/backend-v1.md)
- [Current demo WebSocket exchange, with JSON examples](arendo-customer/docs/protocols/demo-websocket-handoff-ru.md)
- [Ready-to-send backend developer handoff and questions](arendo-customer/docs/protocols/backend-developer-handoff-ru.md)
- [Equipment and indicative prices](arendo-customer/docs/procurement/equipment-estimate-2026-09.md)
- [Full procurement clarification and supplier links](arendo-customer/docs/procurement/clarification-01-cable-and-lock-quantity-ru.md)
- [Temporary laptop WebSocket bridge](local-server/README.md)

The current four-button/LED stand is a demonstration simulator, not a production locker. The
repository distinguishes tested behavior from proposed hardware configuration and remaining
acceptance work.
