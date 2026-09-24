# Acceptance matrix

Status values: `not started`, `partial`, `passed`, `blocked`.

| Test | Scope | Owner | Status |
|---|---|---|---|
| T01 | Device identity and backend registration | Backend + device | partial |
| T02 | Valid device authentication | Backend + device | partial |
| T03 | Invalid/disabled token rejection | Backend | not started |
| T04 | Addressed opening of one cell | Device + hardware | partial |
| T05 | Wrong or unknown cell rejection | Device | partial |
| T06 | Expired command rejection | Device + backend | partial |
| T07 | Duplicate command idempotency | Device + backend | not started |
| T08 | Door opened event | Device + hardware | partial |
| T09 | Door closed event | Device + hardware | partial |
| T10 | Door-not-closed timeout | Device + backend | not started |
| T11 | Modbus disconnect and recovery | Device + hardware | partial |
| T12 | Internet disconnect after command delivery | Device + backend | not started |
| T13 | No stale opening after reconnect | Device + backend | not started |
| T14 | Offline event queue and replay | Device + backend | not started |
| T15 | Heartbeat and server offline detection | Backend + device | partial |
| T16 | Network channel/failover behavior | Platform | deferred |
| T17 | Persistent cell block | Device + backend | not started |
| T18 | Authorized unblock with audit reason | Device + backend | not started |
| T19 | Technician login and assignment | Technician app + backend | not started |
| T20 | 24-hour signed service grant | Technician app + backend | not started |
| T21 | Physical service connection and mutual identity | Technician app + device | not started |
| T22 | Diagnostics report | Device + technician app | not started |
| T23 | GPS or approved manual location binding | Platform + backend | deferred |
| T24 | Supported Android device matrix | Android | not started |
| T25 | Signed update and rollback | Platform + backend | deferred |
| T26 | Advertising/audio operation and recovery | Device + backend | deferred |

The four-button/LED stand is a valid simulator for T04, T08 and T09, but it is not evidence of production lock feedback or a certified door sensor.

