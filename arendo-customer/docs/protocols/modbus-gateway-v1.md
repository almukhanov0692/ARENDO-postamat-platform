# Android Modbus gateway profile v1

Status: the Android app is configured for the ten-output exhibition board reported as
`BCM 04 10 R B`; the supplied manual lists a likely matching `BSM-0410RB` with four inputs and ten
outputs. Confirm the physical label and output revision. The BSM-1616RB remains a separate candidate
for the later 16-in/16-out production profile. Hardware behavior, topology and electrical design
still require commissioning. See [`../hardware/bsm-0410rb-demo.md`](../hardware/bsm-0410rb-demo.md)
for the demo and [`../hardware/bsm-1616rb.md`](../hardware/bsm-1616rb.md) for the future profile.

## Physical path

```text
Android on RK3568 -> USB host -> USB-RS485 converter -> Modbus RTU I/O module
```

Baseline serial parameters are configurable. The current stand uses slave `1`, `9600` baud, `8N1`.

## Current four-cell laboratory simulator mapping

| Cell | Input | Output | Default zero-based address |
|---|---|---|---:|
| `01` | X1 | Y1 | 0 |
| `02` | X2 | Y2 | 1 |
| `03` | X3 | Y3 | 2 |
| `04` | X4 | Y4 | 3 |

- Inputs are read with function `0x02` (Read Discrete Inputs).
- Outputs are written with function `0x05` (Write Single Coil).
- CRC16, response slave ID, function, response length and write echo are validated.
- Input and output base addresses are configuration values, not backend fields.
- Input polarity is configuration. The four-button stand is calibrated during installation.

## Current 10-output / 4-input exhibition mapping

The current demo uses output Y1-Y10 for D01-D10. Following Nursultan's interim proposal, the four
available input points X1-X4 are provisionally assigned to D01-D04. D05-D10 have no physical input
feedback on this board and must be reported to the backend with `door: "unknown"`; an output command
or relay readback is not proof of door position. All addresses below are zero-based.

| Logical cell | Input feedback | Output | Input address | Output address |
|---|---|---|---:|---:|
| `01` | X1 | Y1 | 0 | 0 |
| `02` | X2 | Y2 | 1 | 1 |
| `03` | X3 | Y3 | 2 | 2 |
| `04` | X4 | Y4 | 3 | 3 |
| `05` | not available | Y5 | — | 4 |
| `06` | not available | Y6 | — | 5 |
| `07` | not available | Y7 | — | 6 |
| `08` | not available | Y8 | — | 7 |
| `09` | not available | Y9 | — | 8 |
| `10` | not available | Y10 | — | 9 |

This is a provisional demo wiring map, not a commissioned physical map. The backend receives stable
logical cell IDs only, never X/Y point labels or Modbus addresses.

For the supplied BSM-1616RB manual, the same zero-based input/output channel numbering is specified
for up to 16 points: X1/Y1 use address 0 through X16/Y16 at address 15. Production use still
requires confirming the physical module/revision, fitted output type, wiring and cell map. Do not
infer that modules beyond BSM-1616RB use the same functions or addresses.

The BSM manual also specifies function `0x01` to read output states and function `0x0F` to write
multiple outputs. The Android Modbus client/gateway now supports input read `0x02`, output-state
readback `0x01`, and single-output write `0x05`; multi-output write `0x0F` is not implemented.
Output-state readback is currently a tested gateway API, not yet used by the screen or sent as a
door-state event. It reports the module's output bit only and must never be treated as proof that a
lock moved or a door physically opened; door state comes from the independent input/sensor path.

## Module routing in the Android gateway

The proposed D01-D32 address/channel table is published as [E-003 R0](../hardware/electrical-r0/E-003-cell-map-r0.md);
it is not a commissioned wiring plan. The gateway represents each logical cell with its own Modbus slave ID, input point and output
point. Reads are grouped by slave ID and serialized on the one RS-485 port; an output write is sent
to the mapped slave and local output address. Thus repeated local labels such as X2/Y2 on two boards
are unambiguous: the Modbus slave ID selects the board, and the point address selects its local
channel.

The active Android exhibition profile remains D01-D10 on slave ID `1`, outputs Y1-Y10, and available
feedback inputs X1-X4 on D01-D04. A
unit-tested routing fixture models a possible full map as D01-D16 on slave `1` and D17-D28 on slave
`2`; under that uncommissioned proposal D18 routes to slave `2`, local X2/Y2 (zero-based Modbus point
address `1`). This fixture is for software routing tests only. It does not approve cabinet wiring,
assign addresses to physical modules or change the installed APK profile. The two physical modules
must be configured with distinct IDs and the cell-to-module map must be confirmed before selecting
or deploying a production profile.

For BSM-1616RB, the manual says `S/S` is the digital-input common: connect it to `VCC` for
active-low inputs, or to `GND` for active-high inputs. Do not leave it floating; verify that the
selected polarity matches the actual door sensor/contact wiring.

Each BSM module has its own 0-15 input/output point range. If multiple modules share an RS-485 bus,
the slave ID distinguishes modules; the point address alone does not identify a locker. Do not
assume logical cell numbering or cabinet wiring until the commissioned map is approved.

## Current stand interpretation

- `open_cell` switches the mapped output ON and schedules an OFF write after 2 seconds during normal
  APK/Modbus operation. This is a software timer, not a verified independent cutoff on app/USB/Modbus
  failure. Output/LED state does not determine `door` state.
- For D01-D04, active X-input is currently interpreted as `closed` and inactive X as `open`;
  transitions produce demo door events marked `simulated:true`. D05-D10 remain `unknown`.
- Door closure is a physical action, not a remote output-OFF command. An open or broken feedback
  wire may look inactive, so this provisional polarity cannot prove physical opening.
- Production door sensors replace this simulation without changing the logical backend contract.
  Their contact type, normal state, wire-break behavior and diagnostic coverage must be confirmed
  from the selected sensor/module datasheets and physical tests. Until then, unknown/fault must not
  be converted into `open` or `closed`.

## Polling and failures

- Only one Modbus transaction may be active on a serial port.
- Input polling runs on a worker thread, never on the Android main thread.
- State events are emitted only on transitions, not on every poll.
- Repeated failures are rate-limited in logs and change module status to offline.
- No `open_cell` command is accepted while the module is offline.
- Reconnection must not automatically repeat the last output write.

## Production map to be supplied and approved

Before implementing production polling, record one row per physical cell and each module:

| Field | Required meaning |
|---|---|
| `postamatId` | Stable device identity; not a Modbus address. |
| `cabinetId` / cabinet order | Physical cabinet in the approved daisy-chain topology. |
| `cellCode` | Stable logical cell ID exposed to the backend (target installed cells 01-28). |
| `slaveId` | Approved Modbus address of the actual I/O module. |
| input function/address/polarity | Exact door-sensor read method and interpreted normal state. |
| output function/address/pulse | Exact relay/lock-control method, polarity, pulse duration and safe default. |
| fault/timeout behavior | Which module, line and sensor failures can be detected and reported. |

The BSM-1616RB channel count can cover 14 doors with one module and 28 doors plus four reserved
logical channels with two modules, assuming one digital input and one output per door. This is only
channel-count arithmetic; module addresses, cabinet wiring, output load ratings and fault isolation
must be separately approved and tested. Reserve logical capacity 29-32 is not a claim that
physical doors exist. Configuration validation must reject duplicate cell IDs,
duplicate/conflicting module addresses and mappings outside the approved hardware profile.

The customer has approved an interim exhibition build with 10 relay channels, according to
Nursultan. The logical X/Y map is confirmed as shown above. The canonical Android screen and poller
are configured for ten channels and the debug APK compiles; physical verification of each channel
is still pending. This exhibition approval does not replace the TЗ's 14-door first acceptance stand
or 28-door target.

## Safety before production use

- Never activate an output from an unverified mapping or while the addressed module is offline.
- After USB/Modbus reconnect or Android restart, read physical inputs first. Do not repeat a prior
  output write or execute a stale backend command.
- Treat Modbus acknowledgement as acceptance of a write, not proof that a lock moved or a door
  opened. Report sensor-confirmed state separately.
- Power, inrush, cable voltage drop, connector limits, protection and failure isolation require
  the H06 electrical calculation and real-hardware acceptance; they are outside this protocol note.

