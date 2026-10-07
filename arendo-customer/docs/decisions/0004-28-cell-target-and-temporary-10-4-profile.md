# Decision 0004 — 28-cell target and temporary 10-output / 4-feedback profile

**Recorded:** 7 October 2026
**Status:** Confirmed by the project owner for implementation planning.

## Agreed configuration

- The software target is **28 logical doors and 28 opening outputs**. The 28-cell map is built now; it is not reduced to match the temporary relay count.
- The current temporary project-delivery setup provides **10 relay outputs** and feedback inputs for **4 doors**. The application may operate the connected outputs; it must mark missing outputs unavailable and must not claim an opening or door state without the corresponding hardware/feedback.
- The planned full system uses **two 16-output Modbus I/O modules**. Twenty-eight outputs are assigned to doors; remaining channels, if present, are not additional doors unless the project owner changes the scope.
- Physical availability will be recorded when the owner reports what is connected. No assumption is made that the final board models or configurations match the temporary board.
- Keep logical `cellCode` values separate from Modbus module IDs and local channels. The 10-output and 16-output hardware profiles must be configurable so the 28-cell software map does not depend on one board revision.

## Modbus address note

The BSM-series manufacturer manual states that the default RS-232/RS-485 communication settings include station ID 1, and that communication settings are changed with Modbus_BSI and take effect after power cycling. Thus, for two BSM modules on a shared RS-485 bus, assume both may initially answer at ID 1 and configure distinct IDs before normal polling. The Modbus serial-line specification requires each slave on one bus to have a unique address. Confirm the exact module labels, revisions and saved IDs on the actual units before commissioning.

- [BSM-series manufacturer manual](https://www.rvauto.cn/static/upload/file/20211231/1640933271306767.pdf)
- [Modbus Serial Line Protocol and Implementation Guide](https://www.modbus.org/docs/Modbus_over_serial_line_V1.pdf)

This decision records the target and temporary hardware profile; it does not claim that missing hardware has been connected or accepted.
