# Open engineering decisions

These items must be agreed before the corresponding implementation is treated as final.

| ID | Question | Owner(s) | Needed before |
|---|---|---|---|
| Q01 | Which Android build/image runs on the RK3568 controller? | Nursultan / customer | Production installation |
| Q02 | Which USB-RS485 converter VID/PID and serial chipset are approved? | Nursultan | Production hardware list |
| Q03 | Confirm production Modbus function codes, zero/one-based addressing and input polarity. | Nursultan / customer | Hardware acceptance |
| Q04 | Minimum Android version and required phone/tablet device matrix? | Nursultan / customer | Release acceptance T24 |
| Q05 | Exact input/output module models and final address plan beyond the current X1-X4/Y1-Y4 stand? | Nursultan / customer | Hardware purchase |
| Q06 | Wi-Fi/4G failover policy and thresholds? Deferred; gateway currently uses any working Android network. | Nursultan / Sarvar | T16 |
| Q07 | Event-log capacity and retention/full policy? | Nursultan / Sarvar | C05 |
| Q08 | Backend signing/key provisioning and rotation format? | Nursultan / Sarvar | A03-A05 |
| Q09 | Update package format, boot slots and recovery mechanism? Deferred. | Nursultan / Sarvar | U01-U06 |
| Q10 | GPS hardware accepted, or manual server binding approved? Deferred. | Customer / Nursultan | T23 |
| Q11 | Advertising formats, orientation, maximum file size and storage budget? | Customer / Nursultan | R01-R04 |
| Q12 | Exact protocol transport and heartbeat/retry timings for the backend? | Nursultan / Sarvar | Integration acceptance |
| Q13 | Future phone-to-postamat technician transport, USB roles and security handshake? Deferred and not blocking the device gateway. | Nursultan / customer | Technician application |
