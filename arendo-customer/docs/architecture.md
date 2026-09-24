# Architecture

## Context

ARENDO has four trust and execution zones:

```text
Customer mobile/backend clients
              |
              v
ARENDO backend  <------>  Android gateway on RK3568
                                      |
                                      | USB-RS485 / Modbus RTU
                                      v
                             I/O and relay modules

Future service path: technician Android application <-> postamat
```

## Responsibilities

### Backend

- User accounts and technician assignments.
- Signed service grants with a 24-hour lifetime.
- Rentals, payments and business state.
- Device registry, release assignment and advertising policy.
- Receiving device events and audit records.

### Android gateway on RK3568

- Server connection over any available Internet channel (Wi-Fi or 4G policy is deferred).
- Device identity and addressed command execution.
- USB-RS485/Modbus RTU polling of inputs and control of outputs.
- Actual door state and module diagnostics derived from hardware signals.
- Offline event queue and replay-safe delivery.
- Local enforcement of service mode and blocks.
- Media playback, software update and rollback in later milestones.

### I/O mapping baseline

- One cell maps to one discrete input `Xn` and one coil/output `Yn`.
- `Yn` activates the lock/indicator command for that cell.
- `Xn` is interpreted according to the configured polarity and reports the physical door signal.
- The current four-button stand uses `X1..X4` and `Y1..Y4`; production mapping remains configurable up to 32 channels.

### Technician Android application

- Server login and assigned-postamat list.
- Secure storage of the local app key.
- Automatic discovery of the physically attached postamat.
- Proof of grant validity for that specific postamat.
- Service actions, reason capture, diagnostics, audit results and software selection.
- No remote state-changing service commands in version 1.

## Repository split

The device gateway owns Modbus details. The backend and user applications receive only cell-level commands and states. The future technician application is a separate product and must not be embedded into the gateway UI. Shared behavior is defined by versioned protocol documents and test fixtures.

## State model

Door position, rental/tool state, service mode, blocks and faults are separate dimensions. `door=closed` does not imply that a cell is available.

## Reconnect rules

- Commands carry unique IDs, session IDs and expiry.
- Duplicate delivery returns the stored result without repeating the action.
- Commands from an old session are not executed after reconnect or reboot.
- Local blocks are synchronized before an older server snapshot can be applied.
- Events are stored until the server acknowledges them.
