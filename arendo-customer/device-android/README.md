# ARENDO Device Android

Android gateway tested on the INBOX710 (RK3399) laboratory unit; RK3568 is the proposed production controller.
The 0.2.2-demo package includes the 2 October feedback-state recovery correction; it has not been
accepted on real locks or the 14-door stand.

Download: [ARENDO-Postamat-0.2.2-demo.apk](../../output/apk/ARENDO-Postamat-0.2.2-demo.apk). The
package was built from the debug variant and has not been installed on a device.

## Implemented in the foundation build

- USB host discovery and permission request;
- automatic selection of a supported USB serial adapter, preferring PL2303 when present;
- Modbus RTU `9600 8N1`, slave `1` baseline, matching the supplied BSM-series manual defaults;
- `0x02` input reads and `0x05` single-output writes for the exhibition profile;
- unit-tested gateway API for `0x01` output-state readback, kept separate from physical door status;
- validated logical-cell map that routes I/O by module slave ID and local point address;
- CRC, response slave-ID/function, length and write-echo validation;
- transition-only X-input door feedback for D01-D04; 2-second software-scheduled output pulse.
  Independent cutoff on app/USB/Modbus failure is not verified; do not treat the software timer as
  hardware protection for a real lock.
- WebSocket client with device-token header, `hello`/`welcome`, heartbeat, reconnect,
  addressed `open_cell`, acknowledgements and demo door events;
- persistent command-ID deduplication ledger to prevent replaying an already-seen open command;
- simple local diagnostic screen;
- unit-tested Modbus frame generation.

The current exhibition board was identified by Nursultan as `BCM 04 10 R B`; the supplied BSM manual
lists a likely matching model `BSM-0410RB` with four digital inputs and ten outputs. Confirm the
physical label and output revision before wiring locks. The demo maps D01-D10 to Y1-Y10 and
provisionally maps X1-X4 feedback to D01-D04; D05-D10 must report door state as unknown. This is a
demo profile, not the future 16-in/16-out production-module profile. Function `0x01` output-state
readback is available through the tested gateway API, but is
not yet displayed or sent to the backend; it does not confirm physical door position. Multi-output
write (`0x0F`) and production configuration are not yet integrated. The mapping layer and tests can
route cells through multiple slave IDs; the active app profile is still one module at slave `1`. A
sequential two-module/28-cell map exists only as a software test fixture, not as a commissioned
hardware map. See
`../docs/hardware/bsm-1616rb.md` and `../docs/protocols/modbus-gateway-v1.md`.

The customer has approved an interim exhibition setup with 10 relay channels, according to Nursultan.
The app displays ten outputs and polls the four currently available input-feedback channels. The
physical output paths and the provisional D01-D04 feedback assignment still need one-by-one
verification.
Do not treat the 10-channel exhibit as the TЗ's 14-real-door acceptance test or 28-door target.

The current debug APK was installed on INBOX710 and exchanged `hello`, heartbeat and `cell_report`
ACK with the laptop bridge. Demonstration X-input changes reached the APK and monitor; a complete
real-lock and door-sensor acceptance run is still required. The local laptop bridge is in `../../local-server/`.
For Wi-Fi on one LAN, use `ws://<laptop-ip>:8765/v1/device/socket` in the debug build. For ngrok,
use `wss://<assigned-domain>/v1/device/socket`. The bridge requires a token and keeps its admin UI
on the laptop only. Do not expose port 8766.

Production configuration storage, a durable offline event queue, backend-issued command signing,
token rotation/revocation and production server authorization remain future work. The event IDs are
currently generated in memory and are not persisted for offline replay. See
`../docs/protocols/backend-v1.md`.

The foundation build supports Android API 21 and newer; the connected INBOX710 (RK3399) laboratory
unit reports API 25. The proposed RK3568 production controller requires separate acceptance.

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
| Outputs | Y1-Y10 from zero-based address `0` |

These values will move to validated persistent configuration before production use.
