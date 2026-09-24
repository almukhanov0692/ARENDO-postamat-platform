# Android Modbus gateway profile v1

Status: current four-cell baseline; production module passport remains authoritative.

## Physical path

```text
Android on RK3568 -> USB host -> USB-RS485 converter -> Modbus RTU I/O module
```

Baseline serial parameters are configurable. The current stand uses slave `1`, `9600` baud, `8N1`.

## Cell mapping

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

## Current stand interpretation

- Output/LED ON represents `door=open` for demonstration.
- Physical button activation represents the return/close action.
- After the configured close transition, the corresponding output is switched OFF and the gateway reports `door_closed`.
- Production door sensors replace this simulation without changing the backend contract.

## Polling and failures

- Only one Modbus transaction may be active on a serial port.
- Input polling runs on a worker thread, never on the Android main thread.
- State events are emitted only on transitions, not on every poll.
- Repeated failures are rate-limited in logs and change module status to offline.
- No `open_cell` command is accepted while the module is offline.
- Reconnection must not automatically repeat the last output write.

## Expansion

The mapping model supports up to 32 cells. Adding cells changes configuration only; the backend continues to use stable string `cellCode` values.

