# BSM-1616RB hardware profile

Status: candidate profile transcribed from the supplied nine-page BSM-series manual export
(`Baidu Cloud - Обмен файлами.pdf`, dated 25 September 2026). Confirm the exact board revision and
output implementation against the physical unit before connecting locks.

## Model and capacity

The model table identifies `BSM-1616RB` as a 16-channel digital-input / 16-channel digital-output
module, with RS-232 and RS-485. The table describes outputs generically as relay/transistor; the
physical board and its terminal block must confirm which output implementation is fitted. The model
row references input wiring `A1` and output wiring `B2`.

If this is the relay `B2` version, the manual depicts normally-open dry contacts with one shared
`COM` per group of four outputs (`COM0` for Y1-Y4, `COM1` for Y5-Y8, etc.). The manual states a
250 V withstand value and up to 2 A for the B2 relay output; it does not establish a suitable
inductive-lock load rating for the specific board. Check the exact board/revision rating, lock
steady and inrush current, suppression, fusing and power design before wiring.

Capacity arithmetic, assuming one input and one output per installed door:

- One 16-in/16-out module has enough I/O points for the 14-door first stand, leaving two unused
  channels of each type.
- Two modules have enough I/O points for 28 physical doors plus four reserved logical channels.
- This arithmetic does not determine cabinet harnesses, electrical load capacity, slave addresses
  or fault isolation. The reserve channels must not be displayed as installed doors.

## Power and digital inputs

- Digital-module supply in the manual: DC 9-48 V.
- Digital switching inputs: DC 9-24 V. Do not infer that the 48 V module supply is safe to apply
  to an input terminal.
- Stated module environment: -40 to +70 °C, 5-85% RH, without freezing or condensation. This does
  not establish the assembled postamat's temperature range; every component and enclosure must
  meet the system requirement.
- `S/S` is the common/reference selector for the digital inputs, not a Modbus signal and not an
  optional floating terminal. According to the manual, connect `S/S` to module `VCC` for
  active-low inputs, or to `GND` for active-high inputs. Select this to match the sensor/contact
  wiring and verify on the actual module before connecting all channels.
- A dry contact alone does not create an input voltage. Use the module's documented input circuit
  and its DC 9-24 V range. Do not connect the lock-power circuit to `X` or `S/S`.

## Modbus RTU profile from the manual

| Item | Manual value / behavior |
|---|---|
| Default serial settings | 9600 baud, 8 data bits, no parity, 1 stop bit (`9600 8N1`) |
| Supported serial baud | 4800, 9600, 19200, 115200 (manual setting table) |
| Default slave address | 1; configurable in the module, stated range 1-255 |
| RS-485 topology | One master to multiple addressed slave modules |
| Digital inputs | Function `0x02`; first input address 0, up to 16 points at addresses 0-15 |
| Output/relay state readback | Function `0x01`; first output address 0, up to 16 points at addresses 0-15 |
| Single output write | Function `0x05`; Y1 address 0 through Y16 address 15; ON value `0xFF00`, OFF value `0x0000` |
| Multiple output write | Function `0x0F` is described by the manual |

The manual shows zero-based point addresses in its examples. It states that the module address can
be set from 1-255; use standard Modbus unicast IDs 1-247 unless the vendor documents an exception.
Each module on a shared RS-485 bus needs a unique ID; multiple modules left at default address `1`
would conflict. Assign IDs and confirm persistence across power cycles during commissioning. Use
the terminal polarity printed on the actual board for RS-485 A/B; do not infer it from wire colors.

## Proposed logical mapping

For an approved single-module 14-door stand, the initial candidate mapping is:

| Logical channel | Door input | Relay output | Modbus address (zero-based) |
|---|---|---|---:|
| D01 | X1 | Y1 | 0 |
| D02 | X2 | Y2 | 1 |
| ... | ... | ... | ... |
| D14 | X14 | Y14 | 13 |

This is a proposed one-to-one map from the BSM manual's channel numbering, not proof that those
contacts are already wired to those doors. The final map must include `postamatId`, cabinet, stable
`cellCode`, slave ID, input/output addresses, input polarity, relay behavior and test evidence. The
backend sees only the logical `cellCode`, never X/Y labels or Modbus addresses.

## Safety and commissioning notes

- An energized relay output indicates the commanded contact state only; it does not prove that a
  lock moved or that a door opened. Use an independent door sensor for the reported door state.
- Determine whether the exact module has mechanical relays or NPN transistor outputs from the
  physical unit. Do not use the generic model name alone to select a load-wiring diagram.
- For relay `B2`, outputs are grouped dry contacts. `Y` is not a powered positive output; the
  external load supply is switched through the group's `COM` and the selected `Y` contact.
- Do not connect or hot-plug cabinet/module wiring under power unless the approved hardware design
  explicitly permits it.
- Record module revision, slave ID, supply, input polarity, mapped channels and a one-at-a-time
  output/input test before enabling backend commands.

## Source pages reviewed

The supplied PDF is a Baidu Cloud export of the BSM-series manual. The model table is on page 2;
input and output wiring diagrams are on page 3; RS-485 topology is on page 4; serial defaults and
configuration are on page 5; Modbus functions `0x02`, `0x01`, `0x05` and `0x0F` are on pages 6-7.
