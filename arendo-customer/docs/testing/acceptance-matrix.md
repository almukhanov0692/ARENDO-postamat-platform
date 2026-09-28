# Acceptance matrix — ARENDO technical specification v1.1

Test IDs and acceptance outcomes below follow the source specification in scope. Status means
evidence in the canonical project/physical stand, not a statement that work is underway.

Status values: `not started`, `partial`, `passed`, `blocked`, `deferred`. `partial` means only a narrower
prototype or subset has evidence; it does not satisfy the full test.

| Test | Acceptance scope | Owner(s) | Status | Evidence / remaining gap |
|---|---|---|---|---|
| T01 | Two seven-cell modules; address each of 14 real locks and confirm door sensors, with no other lock actuating. | Nursultan | not started | Customer approved a 10-output exhibition build. The active app maps D01-D10 to Y1-Y10, but only D01-D04 have X1-X4 feedback; D05-D10 have no feedback. The ordered locks and 14-real-door stand are not yet accepted; exhibition approval does not replace T01. |
| T02 | Add the second cabinet in the chain without reworking existing harnesses; addresses do not conflict and inventory updates. | Nursultan | not started | Android routing code now distinguishes cells by module slave ID and local channel; this has unit-test coverage only. Physical topology, unique module IDs, commissioned map and inventory update remain unverified. |
| T03 | Calculations and a verification plan for 28 physical cells and 32 logical channels. | Nursultan | partial | Requirement documented; approved electrical calculations and validated production map remain. |
| T04 | Power loss keeps closed doors locked; restart reads sensors and saved blocks without spontaneous opening. | Nursultan | not started | Real lock power-loss/restart acceptance not evidenced. |
| T05 | Module/line failure is isolated; stale/unknown data is not shown as current; remaining modules behave as designed. | Nursultan | not started | Fault boundaries depend on selected hardware/topology. |
| T06 | Valid service access controls only its assigned postamat; foreign device and expired/forged grant are rejected. | Nursultan + Sarvar | not started | Technician grant and mutual USB authentication are not implemented. |
| T07 | Grant lasts 24 hours after successful refresh; expires without refresh and cannot be extended by reboot/time changes. | Nursultan + Sarvar | not started | Grant protocol and trusted-time behavior remain to implement. |
| T08 | USB authorization enters `inspection`; issues are blocked, existing rentals/reservations remain. | Nursultan + Sarvar | not started | Service-mode workflow not implemented. |
| T09 | Server return works during inspection; client/service operations do not conflict; technician can resume and finish service. | Nursultan + Sarvar | not started | End-to-end service/return coordination not implemented. |
| T10 | USB disconnect checks doors/equipment; only healthy closed cells leave global inspection restriction; other blocks remain. | Nursultan | not started | Safe disconnect reconciliation not implemented. |
| T11 | Approved USB service actions work offline; events and blocks persist and synchronize correctly later. | Nursultan | not started | Durable service/event storage and offline synchronization not implemented. |
| T12 | USB permission failure, cable loss, backgrounding and app restart do not repeat an opening or lose its result. | Nursultan | partial | USB permission and basic reconnect path exist; full fault/restart acceptance remains. |
| T13 | Command receipt and sensor-confirmed opening are distinct; missing sensor feedback is not reported as success. | Nursultan + Sarvar | partial | Simulator command/input flow exists; real sensor confirmation, the 30-second no-feedback timeout and canonical backend flow remain. |
| T14 | Duplicate command does not actuate twice; stale command after restart/reconnect is rejected. | Nursultan + Sarvar | partial | APK checks expiry and persists a bounded set of processed command IDs; production end-to-end results, backend deduplication and restart/reconnect acceptance remain. |
| T15 | Immediate/deferred blocks, cancellation and unblock remain consistent through USB loss/restart; returns remain possible. | Nursultan + Sarvar | not started | Persistent conflict-safe block workflow not implemented. |
| T16 | Wi-Fi/4G switch by agreed policy without duplicate actions or event loss. | Nursultan + Sarvar | deferred | Failover policy and thresholds must be agreed; end-to-end test pending. |
| T17 | Cached authorized videos loop offline and expire correctly; ARENDO fallback plays after authorization ends. | Nursultan + Sarvar | not started | Media player/playlist not implemented. |
| T18 | Interrupted/corrupt download cannot replace working playlist; new authorization applies after reconnection. | Nursultan + Sarvar | not started | Verified media staging/replacement not implemented. |
| T19 | Bundled prompts play at required events; video/download load does not disrupt lock and sensor control. | Nursultan | not started | Audio event list and implementation not finalized. |
| T20 | Technician can select a server-approved update over USB; device can install the server-assigned version. | Nursultan + Sarvar | not started | Update package/distribution path not implemented. |
| T21 | Failed download/install/start/power interruption preserves a working version; automatic rollback and USB recovery demonstrated. | Nursultan | not started | A/B or equivalent recovery design not selected. |
| T22 | Previous version remains until both device and server confirm the new version; server loss is not success. | Nursultan + Sarvar | not started | Two-party update confirmation protocol not implemented. |
| T23 | GPS reports coordinates and server-confirmed binding inside target building; or separately approved manual alternative works. | Nursultan (device GPS) + Sarvar (server binding API) | deferred | Device-side GPS path and indoor acceptance not implemented; server API is agreed when Nursultan provides the payload; manual alternative requires approval. |
| T24 | Handover instructions reproduce installable builds on the declared Android device matrix. | Nursultan | partial | APK 0.2.1 builds and was installed on INBOX710 (RK3399, API 25). RK3568 and the supported-device matrix require separate tests; independent reproduction remains. |
| T25 | Nursultan participates in first full build/start; real doors, backend, USB, sound and display are checked. | Nursultan + Sarvar | not started | Full postamat build and integrated acceptance not evidenced. |
| T26 | Another specialist can assemble, configure and test from delivered docs, access and materials. | Nursultan | partial | Source/docs exist; complete manufacturing/service package and independent handover test remain. |

## Demonstration evidence boundary

The ten-output/four-input laboratory stand demonstrates a simulated command/input/status path.
Its APK 0.2.1 and temporary laptop bridge exchanged `hello`, heartbeat and `cell_report` ACK, and
X-input changes were observed on the APK and local monitor. This is not a pass for T01, T04, T05
or T13's real-lock and real-sensor requirements. It also does not establish production-safe command
security, offline event replay or acceptance against the real backend.

Record the hardware models/revisions, wiring/map revision, Android build, application version,
server version, exact steps, result, timestamps and logs for every acceptance run. Any failed or
unrun mandatory test remains open until retest or explicit approved scope change.

