# Current exhibition I/O board profile

## Identity and evidence

Nursultan reports the physical board marking as `BCM 04 10 R B`. The supplied BSM-series manual
lists a likely matching model, `BSM-0410RB`; verify the exact letters and revision from the board
label before treating this profile as final.

The manual's model table describes this model as four digital inputs and ten relay/transistor
outputs, with RS-232 and RS-485. It lists wiring diagrams A1/B2. The B2 relay diagram shows dry
contacts; an output terminal is not a powered positive supply. The manual's generic model row does
not prove which output hardware is fitted on the physical board.

## Communication

- Default: Modbus RTU, slave `1`, `9600 8N1`.
- Digital input read: function `0x02`; X1-X4 use zero-based points 0-3.
- Output state read: function `0x01`; diagnostic only, not proof of door movement.
- Single output write: function `0x05`; Y1-Y10 use zero-based points 0-9.
- The manual's parameter procedure describes all DIP switches OFF for communication-parameter
  mode and all ON for normal operation, with a power cycle. Do not apply this to an unidentified
  two-position switch; verify the exact board and switch bank first.

## Interim demo map

| Logical cell | Feedback input | Relay output | Door status source |
|---|---|---|---|
| D01-D04 | X1-X4 respectively | Y1-Y4 respectively | Demo input/button path; current app interprets active X as `closed` and inactive X as `open`; verify polarity and wiring on the board |
| D05-D10 | Not available on this board | Y5-Y10 respectively | `unknown`; output state must not be reported as door state |

The D01-D04 assignment and active/inactive interpretation are a provisional software profile based
on Nursultan's request to start with the first four input points and the button-as-door demo. Confirm
the actual wiring/contact polarity before installation with real door sensors. The 10-output demo is
not the 14-real-door acceptance stand and is not the final 28-cell hardware configuration.

## Electrical caution

The manual gives the digital input range as DC 9-24 V. `S/S` is the digital-input common/reference;
the manual describes different active polarity depending on its reference connection. Confirm the
input circuit and contact polarity on the actual board before wiring all channels. For B2 relay
outputs the manual states a 2 A maximum and depicts shared COM groups; verify the exact revision,
load inrush, suppression, supply, fuse and wiring before connecting a lock. Do not infer lock-load
capacity from the model name alone.

## Separate future profile

The later 16-in/16-out board profile is documented separately in
[`bsm-1616rb.md`](bsm-1616rb.md). Its address and wiring assumptions must not be copied onto this
four-input/ten-output exhibition board without verification.
