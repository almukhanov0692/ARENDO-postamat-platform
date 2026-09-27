# Android Modbus gateway profile v1

Status: the four-cell laboratory map is implemented. The supplied BSM-1616RB manual provides a
candidate 16-in/16-out Modbus profile; the physical board revision, output implementation, full
door map, topology and electrical design still require commissioning before production use. See
[`../hardware/bsm-1616rb.md`](../hardware/bsm-1616rb.md).

## Physical path

```text
Android on RK3568 -> USB host -> USB-RS485 converter -> Modbus RTU I/O module
```

Baseline serial parameters are configurable. The current stand uses slave `1`, `9600` baud, `8N1`.

## Laboratory simulator mapping

| Cell | Input | Output | Default zero-based address |
|---|---|---|---:|
| `01` | X1 | Y1 | 0 |
| `02` | X2 | Y2 | 1 |
| `03` | X3 | Y3 | 2 |
| `04` | X4 | Y4 | 3 |

- Inputs are read with function `0x02` (Read Discrete Inputs).
- Outputs are written with function `0x05` (Write Single Coil).
- CRC16, slave ID, function, response length and write echo must be validated.
- Input and output base addresses are configuration values, not backend fields.
- Input polarity is configuration. The four-button stand is calibrated during installation.

For the supplied BSM-1616RB manual, the same zero-based input/output channel numbering is specified
for up to 16 points: X1/Y1 use address 0 through X16/Y16 at address 15. Production use still
requires confirming the physical module/revision, fitted output type, wiring and cell map. Do not
infer that modules beyond BSM-1616RB use the same functions or addresses.

The BSM manual also specifies function `0x01` to read output states and function `0x0F` to write
multiple outputs. The current Android client implements input read `0x02` and single-output write
`0x05`; output-state readback and multi-output write are not yet integrated.

For BSM-1616RB, the manual says `S/S` is the digital-input common: connect it to `VCC` for
active-low inputs, or to `GND` for active-high inputs. Do not leave it floating; verify that the
selected polarity matches the actual door sensor/contact wiring.

Each BSM module has its own 0-15 input/output point range. If multiple modules share an RS-485 bus,
the slave ID distinguishes modules; the point address alone does not identify a locker. Do not
assume logical cell numbering or cabinet wiring until the commissioned map is approved.

## Current stand interpretation

- Output/LED ON represents `door=open` for demonstration.
- Physical button activation represents the return/close action.
- After the configured close transition, the corresponding output is switched OFF and the gateway reports `door_closed`.
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
Nursultan. It can use the first ten logical channels of the BSM profile once the physical wiring map
is confirmed. The current canonical Android screen/polling loop is still limited to four channels,
so the 10-channel configuration requires a software change and separate verification. This
exhibition approval does not replace the TЗ's 14-door first acceptance stand or 28-door target.

## Safety before production use

- Never activate an output from an unverified mapping or while the addressed module is offline.
- After USB/Modbus reconnect or Android restart, read physical inputs first. Do not repeat a prior
  output write or execute a stale backend command.
- Treat Modbus acknowledgement as acceptance of a write, not proof that a lock moved or a door
  opened. Report sensor-confirmed state separately.
- Power, inrush, cable voltage drop, connector limits, protection and failure isolation require
  the H06 electrical calculation and real-hardware acceptance; they are outside this protocol note.

