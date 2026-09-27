# Android Modbus gateway profile v1

Status: laboratory four-cell profile only. This is not the approved production hardware map.
The production I/O module manuals, module count, addressing, topology and electrical design remain
to be approved before deployment.

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

These addresses describe only the currently demonstrated module. Do not copy them to a different
module or infer that production cells 05-32 use consecutive addresses or the same Modbus function
codes.

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

Reserve logical capacity 29-32 is not a claim that physical doors exist. Configuration validation
must reject duplicate cell IDs, duplicate/conflicting module addresses and mappings outside the
approved hardware profile. Whether production can be changed by configuration alone depends on the
selected modules; this has not yet been demonstrated.

## Safety before production use

- Never activate an output from an unverified mapping or while the addressed module is offline.
- After USB/Modbus reconnect or Android restart, read physical inputs first. Do not repeat a prior
  output write or execute a stale backend command.
- Treat Modbus acknowledgement as acceptance of a write, not proof that a lock moved or a door
  opened. Report sensor-confirmed state separately.
- Power, inrush, cable voltage drop, connector limits, protection and failure isolation require
  the H06 electrical calculation and real-hardware acceptance; they are outside this protocol note.

