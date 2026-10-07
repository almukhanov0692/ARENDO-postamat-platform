# Changes

## ARENDO project library update — 8 October 2026

- Set the `ARENDO` folder as the canonical local Git working copy on the existing `main` history.
- Recorded the owner-confirmed product target of 28 logical doors, the temporary 10-output / 4-feedback profile, and the selected Variant B price of 1,350,000 KZT with the agreed first-launch equipment.
- Added the canonical document index, Modbus Poll screenshots, saved manual and specification pages, procurement inventory, commissioning record template, and milestone evidence handoff.
- No Android application code or APK was changed; the demo version remains `0.2.2-demo`.

## Engineering audit and documentation correction - 2 October 2026

- Added a cross-discipline audit against ARENDO TZ v1.1 and the current source/test evidence.
- Recorded the published legacy device credential for rotation, plus two pre-lock blockers:
  independent output pulse cutoff and verified load/power limits. The secret value is not reproduced.
- Corrected procurement notes so the 30-second feedback timeout is described as not yet implemented
  and direct lock switching is not presented as electrically approved.
- No production protocol or physical acceptance status changed; the 14-door stand remains unverified.

## Android gateway 0.2.2-demo - 2 October 2026

- Feedback-backed cells become `unknown` when Modbus polling fails or the USB port closes; the first
  sample after recovery establishes a new baseline instead of inventing a transition during the
  outage.
- Results from a closed or replaced Modbus gateway are ignored.
- `DoorFeedbackTrackerTest` passes. This is a demo software build; real module/line failure testing,
  the 14-door bench and production acceptance remain open.

The APK is `output/apk/ARENDO-Postamat-0.2.2-demo.apk`.

## Project documentation update - 30 September 2026

- Recorded the planned 28-cell procurement configuration: two BSM-1616RB modules on one shared
  RS-485 bus, with distinct proposed Modbus IDs `1` and `2`; the physical IDs and wiring remain to
  be commissioned.
- Added the logical D01-D32 routing sheet: D01-D28 are the product target and D29-D32 are spare I/O
  capacity, not installed doors.
- Updated the procurement notes with the RK3568 4/32 GB controller selection, 34 locks (28 + 6
  spare), 400 m cable, 12 V / 50 A supply, and separate antenna/audio caveats.
- Added a backend-draft compatibility audit against the current APK. This is documentation and test
  coverage only; it does not change the Android wire protocol or claim production acceptance.
- Added endpoint routing assertions for the two-module software fixture. No APK release number is
  changed by this documentation/test-only update.

## Android gateway 0.2.1 - 28 September 2026

- The INBOX710 laboratory APK and local laptop bridge were tested together over Wi-Fi/WebSocket.
  The bridge received `hello`, `heartbeat` and an acknowledged `cell_report` from the installed APK.
- The `open_cell` action uses a 2-second output pulse with automatic OFF. The local monitor can open
  D1-D10 individually or send ten sequential `open_cell` commands; it can request a fresh status
  snapshot. "Open all" is a demo UI sequence, not a new backend protocol command.
- Removed remote `close_cell` from the Android device contract and local monitor/API. A person closes
  the door; the feedback input, not a relay-OFF command, reports `door_closed`.
- D1-D4 use demo X-input transitions for `open`/`closed`; D5-D10 have no wired feedback and report
  `unknown`. Current events are explicitly marked `simulated:true`.
- Added a short [Russian backend handoff](arendo-customer/docs/protocols/demo-websocket-handoff-ru.md)
  with current WebSocket messages and implementation boundaries. Updated the protocol and status
  documents to distinguish command ACK from door feedback.
- A local-test connection shortcut is available only when a debug build receives a URL and a
  temporary token from build environment variables. No device token is committed to this repository.

This is a development stand, not acceptance of the specification's 14-real-door bench or completed
28-door product. Lock feedback polarity, physical sensor meaning, production authentication,
durable events, USB technician access, updates and recovery still require implementation and tests.
