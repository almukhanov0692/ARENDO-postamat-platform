# Open engineering decisions

These items require technical evidence or coordination before the related implementation is accepted. Owner-confirmed scope is recorded separately below and is not an open question.

## Already confirmed — 8 October 2026

- Software target: 28 logical doors and 28 opening outputs.
- Temporary setup: 10 available outputs and feedback for 4 doors; the software map remains 28 and unconnected outputs are unavailable.
- Full hardware plan: two 16-output Modbus I/O modules; exact model/revision and physical mapping are checked when available.
- Commercial option selected: Variant B, 1 350 000 KZT including agreed equipment for the first launch; the 300 000 KZT equipment amount is a preliminary reserve, and the owner-provided itemized list, quantities and prices are already captured in the procurement update and estimate; confirm supplier variants, delivery and included accessories before ordering.
- For BSM-series modules, the documented factory serial station ID is 1. If they share one RS-485 bus, configure unique IDs; do not assume both can remain at 1.
- See [decision 0004](decisions/0004-28-cell-target-and-temporary-10-4-profile.md). Do not re-ask the owner to choose between 10 and 28 as the software target.

| ID | Question | Owner(s) | Needed before |
|---|---|---|---|
| Q01 | Which Android build/image runs on the RK3568 controller? | Nursultan / customer | Production installation |
| Q02 | Which USB-RS485 converter VID/PID and serial chipset are approved? | Nursultan | Production hardware list |
| Q03 | The BSM-series manual gives FC02 input read, FC01 output-state read, FC05 single-output write, FC0F multi-output write, zero-based points, and factory station ID 1. Confirm exact board model/revision, register profile, output rating/type, input polarity, lock load/pulse behavior and sensor faults on the actual modules. | Nursultan / customer | Before real-lock commissioning |
| Q04 | Minimum Android version and required phone/tablet device matrix? | Nursultan / customer | Release acceptance T24 |
| Q05 | The owner confirmed the 28-door scope, temporary 10-output/4-feedback setup and two future 16-output modules. Remaining commissioning work: record the actual bus topology, assign unique slave IDs on a shared bus, verify persistence after power loss, and approve the real cell/channel/harness map. | Nursultan / customer | When the actual modules and wiring are available |
| Q06 | Wi-Fi/4G failover policy and thresholds? Deferred; gateway currently uses any working Android network. | Nursultan / Sarvar | T16 |
| Q07 | Event-log capacity and retention/full policy? | Nursultan / Sarvar | C05 |
| Q08 | Backend signing/key provisioning and rotation format? | Nursultan / Sarvar | A03-A05 |
| Q09 | Update package format, boot slots and recovery mechanism? Deferred. | Nursultan / Sarvar | U01-U06 |
| Q10 | Nursultan to implement device-side GPS/location reporting. Agree the server receive/binding payload with Sarvar when the device interface is ready; indoor accuracy and any manual override remain to be accepted. | Nursultan (device); Sarvar (server API) | T23 |
| Q11 | Advertising formats, orientation, maximum file size and storage budget? | Customer / Nursultan | R01-R04 |
| Q12 | Exact protocol transport and heartbeat/retry timings for the backend? | Nursultan / Sarvar | Integration acceptance |
| Q13 | Future phone-to-postamat technician transport, USB roles and security handshake? Deferred and not blocking the device gateway. | Nursultan / customer | Technician application |
| Q14 | Confirm the exact RK3568 controller SKU/revision and whether Modbus is reached through the selected USB-RS485 converter or an accessible onboard RS-485 port; confirm Android serial permissions/device node. | Nursultan / supplier | Hardware purchase and controller commissioning |
| Q15 | Confirm whether the controller order includes an LTE modem and Wi-Fi/BT/LTE antennas. The reported antenna connector is not evidence that a modem or antenna is included. | Nursultan / supplier | Final BOM and network acceptance |
| Q16 | Confirm exact audio product: powered speaker with 3.5 mm input and USB power, or passive 8-ohm speaker that requires an amplified output. Do not substitute one for the other based on a similar listing photo. | Nursultan / supplier | Final BOM and audio test |
| Q17 | Reconcile the itemized equipment calculation to the selected 1 350 000 KZT Variant B total before ordering: one dated CNY exchange rate, freight quote, local consumables, IP 5% basis, import/customs/VAT treatment, and separate 5% actual-cost reserve. Prevent double counting and replace the 272,800 KZT draft with an audited amount; any discrepancy must be agreed before purchase, with no automatic surcharge. | Nursultan | Before placing equipment orders |
| Q18 | What is the maximum number of lock outputs that may be energized at once, and should simultaneous opens to different cells be queued or rejected? A provisional 12 V / 50 A supply cannot support 28 × 2 A lock loads simultaneously, before controller and peripheral loads. Define expiry/ACK behavior for queued or busy commands and the approved current limit. | Nursultan / Sarvar / customer | Stage 1 power design, command contract and before enabling multi-cell hardware acceptance |
| Q19 | What independently guarantees that an energized output turns OFF if Android stops, USB/RS-485 disconnects, or the Modbus OFF write fails during the two-second pulse? Confirm the exact module's output state on communication loss and specify a hardware cutoff or other proven safe behavior. | Nursultan / customer | Before connecting a real lock; T12 and T04 fault testing |
| Q20 | A device credential is embedded in the legacy Android source and exists in public Git history. Has Sarvar revoked/rotated it, and have the owners agreed whether to remove the historical blob by rewriting Git history? Do not put the replacement secret in Git. | Sarvar / Nursultan / repository owner | Immediately, before further use of the credential or public build |
