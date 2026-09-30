# Open engineering decisions

These items must be agreed before the corresponding implementation is treated as final.

| ID | Question | Owner(s) | Needed before |
|---|---|---|---|
| Q01 | Which Android build/image runs on the RK3568 controller? | Nursultan / customer | Production installation |
| Q02 | Which USB-RS485 converter VID/PID and serial chipset are approved? | Nursultan | Production hardware list |
| Q03 | BSM-1616RB manual gives FC02 input read, FC01 output-state read, FC05 single-output write, FC0F multi-output write and zero-based points 0-15. Bench-confirm the fitted output type/rating, input polarity, lock pulse/load behavior and sensor fault handling. | Nursultan / customer | Stage 1 production map and hardware acceptance |
| Q04 | Minimum Android version and required phone/tablet device matrix? | Nursultan / customer | Release acceptance T24 |
| Q05 | Proposed software map: one shared A/B RS-485 bus; BSM M1 / slave ID 1 serves D01-D16, M2 / slave ID 2 serves D17-D28; M2 channels 13-16 remain spare. On actual boards, write and verify unique IDs, persistence after power loss, fitted output type, cell/harness assignment, load design and fault isolation. | Nursultan / customer | Stage 1 hardware freeze and commissioning |
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
| Q17 | Reconcile the final KZT calculation: one dated CNY exchange rate, freight quote, local consumables, IP 5% basis, import/customs/VAT treatment, and separate 5% actual-cost reserve. Prevent double counting and replace the 272,800 KZT draft with an audited amount. | Nursultan | Before requesting final procurement transfer |
