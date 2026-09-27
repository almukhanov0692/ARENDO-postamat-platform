# Open engineering decisions

These items must be agreed before the corresponding implementation is treated as final.

| ID | Question | Owner(s) | Needed before |
|---|---|---|---|
| Q01 | Which Android build/image runs on the RK3568 controller? | Nursultan / customer | Production installation |
| Q02 | Which USB-RS485 converter VID/PID and serial chipset are approved? | Nursultan | Production hardware list |
| Q03 | BSM-1616RB manual gives FC02 input read, FC01 output-state read, FC05 single-output write, FC0F multi-output write and zero-based points 0-15. Bench-confirm the fitted output type/rating, input polarity, lock pulse/load behavior and sensor fault handling. | Nursultan / customer | Stage 1 production map and hardware acceptance |
| Q04 | Minimum Android version and required phone/tablet device matrix? | Nursultan / customer | Release acceptance T24 |
| Q05 | BSM-1616RB is the supplied candidate. Confirm physical revision/output implementation and quantity (channel-count minimum: one module for 14 doors; two for 28 doors + 4 reserves if each door uses one DI and one DO), cabinet wiring, unique slave IDs and address retention after power cycle. | Nursultan / customer | Stage 1 hardware freeze and purchase |
| Q06 | Wi-Fi/4G failover policy and thresholds? Deferred; gateway currently uses any working Android network. | Nursultan / Sarvar | T16 |
| Q07 | Event-log capacity and retention/full policy? | Nursultan / Sarvar | C05 |
| Q08 | Backend signing/key provisioning and rotation format? | Nursultan / Sarvar | A03-A05 |
| Q09 | Update package format, boot slots and recovery mechanism? Deferred. | Nursultan / Sarvar | U01-U06 |
| Q10 | GPS hardware accepted, or manual server binding approved? Deferred. | Customer / Nursultan | T23 |
| Q11 | Advertising formats, orientation, maximum file size and storage budget? | Customer / Nursultan | R01-R04 |
| Q12 | Exact protocol transport and heartbeat/retry timings for the backend? | Nursultan / Sarvar | Integration acceptance |
| Q13 | Future phone-to-postamat technician transport, USB roles and security handshake? Deferred and not blocking the device gateway. | Nursultan / customer | Technician application |
