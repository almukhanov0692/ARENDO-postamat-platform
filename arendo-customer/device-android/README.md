# ARENDO Device Android

Android gateway installed on the RK3568 postamat controller.

## Implemented in the foundation build

- USB host discovery and permission request;
- automatic selection of a supported USB serial adapter, preferring PL2303 when present;
- Modbus RTU `9600 8N1`, slave `1` baseline, matching the supplied BSM-1616RB manual defaults;
- `0x02` read of X1-X10 and `0x05` write of Y1-Y10 for the exhibition profile;
- CRC, response function, length and write-echo validation;
- transition-only close handling for the ten-channel exhibition profile;
- simple local diagnostic screen;
- unit-tested Modbus frame generation.

The ten-point exhibition client profile is compatible with BSM-1616RB manual functions `0x02` and
`0x05`. The module's documented 16-point capacity, output-state readback (`0x01`), multi-output
write (`0x0F`) and production configuration are not yet integrated. See
`../docs/hardware/bsm-1616rb.md` and `../docs/protocols/modbus-gateway-v1.md`.

The customer has approved an interim exhibition setup with 10 relay channels, according to Nursultan.
The confirmed exhibition mapping is D01-X1/Y1 through D10-X10/Y10. The app now displays and polls
ten channels and the debug build compiles; the ten physical paths still need one-by-one verification.
Do not treat the 10-channel exhibit as the TЗ's 14-real-door acceptance test or 28-door target.

The backend WebSocket client, persistent offline queue and production configuration store are
intentionally the next milestone. Their contract is documented in `../docs/protocols/backend-v1.md`.

The foundation build supports Android API 21 and newer; the connected RK3568 test unit reports API 25.

## Build

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.

## Current stand defaults

| Setting | Value |
|---|---|
| Slave | `1` |
| Serial | `9600 8N1` |
| Inputs | X1-X10 from zero-based address `0` |
| Outputs | Y1-Y10 from zero-based address `0` |

These values will move to validated persistent configuration before production use.
