# Technical requirements traceability

This file summarizes the implementation requirements from ARENDO technical specification v1.1. Commercial terms are intentionally excluded.

## Scope

- Up to 32 channels; normal configuration up to four cabinets with seven cells each.
- Indoor installation at or above +5 C.
- Wi-Fi primary connectivity and 4G fallback.
- No backup power; closed doors remain locked without power.
- Business logic remains on the backend.
- Physical USB service from an Android phone or tablet.

## Controller requirements

| IDs | Required outcome |
|---|---|
| C01-C03 | Authenticate addressed commands; distinguish receipt, acceptance, action and sensor confirmation; reject duplicates and expired/old-session commands. |
| C04-C05 | Report open/closed/unknown separately from faults; persist events offline and deliver without loss or double counting. |
| C06-C07 | Wi-Fi/4G failover, versions, connectivity, module availability and timestamped diagnostics. |
| C08-C09 | Persist blocks and service results; serialize conflicting operations for one cell. |

## Technician Android requirements

| IDs | Required outcome |
|---|---|
| A01 | Phone and tablet support; documented USB requirements and tested-device list. |
| A02-A03 | Server-created accounts, assigned postamats and renewable 24-hour service grants. |
| A04-A05 | Automatic mutual USB identification, grant verification for the exact postamat, no global shared secret and replay protection. |
| A06-A07 | Safe expiry, missing permission, unsupported cable/device, disconnect, reconnect, background and restart behavior. |

## Service mode

| IDs | Required outcome |
|---|---|
| M01-M04 | Authorized USB automatically starts `inspection`; new issues are blocked while returns remain available. Offline local enforcement synchronizes later. |
| M05-M06 | USB disconnect triggers diagnostics and releases only healthy closed cells; explicit blocks persist. |
| M07-M09 | Service opening requires a reason and never starts billing. Human closes the door. Immediate/deferred blocks and cancellation are supported. |

## Update and media

- U01-U06: selected or assigned versions, signed/integrity-checked package, safe install, two confirmations, automatic rollback and USB recovery.
- V01-V02: bundled recorded phrases and deterministic audio priority over advertising.
- R01-R04: local playlist, offline playback, safe file replacement and built-in ARENDO fallback media.

## Delivery

- Reproducible source builds and install packages.
- Versioned dependencies and configuration.
- Protocols, assembly/operation/recovery instructions and test reports.
- No dependency on a personal AI account or unavailable private service.

