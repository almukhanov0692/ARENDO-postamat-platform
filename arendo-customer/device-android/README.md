# ARENDO Device Android

Android gateway installed on the RK3568 postamat controller.

## Implemented in the foundation build

- USB host discovery and permission request;
- automatic selection of a supported USB serial adapter, preferring PL2303 when present;
- Modbus RTU `9600 8N1`, slave `1` baseline, matching the supplied BSM-1616RB manual defaults;
- `0x02` read of X1-X4 and `0x05` write of Y1-Y4;
- CRC, response function, length and write-echo validation;
- transition-only close handling for the four-button demonstration stand;
- simple local diagnostic screen;
- unit-tested Modbus frame generation.

The current four-point client profile is compatible with BSM-1616RB manual functions `0x02` and
`0x05`. The module's documented 16-point capacity, output-state readback (`0x01`), multi-output
write (`0x0F`) and production cell configuration are not yet integrated. See
`../docs/hardware/bsm-1616rb.md` and `../docs/protocols/modbus-gateway-v1.md`.

The exhibition setup is planned for 10 relay channels; the current app still displays and polls four
simulator channels. Do not treat the 10-channel exhibit as the TЗ's 14-real-door acceptance test.

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
| Inputs | X1-X4 from zero-based address `0` |
| Outputs | Y1-Y4 from zero-based address `0` |

These values will move to validated persistent configuration before production use.
