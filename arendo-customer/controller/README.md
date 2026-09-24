# Postamat controller

The current controller runtime is an Android gateway on RK3568 and is implemented in `../device-android/`. It connects to the I/O module through a USB-RS485 converter using Modbus RTU.

This directory is reserved for hardware notes and possible native/background services introduced after the Android image, production module models and update/rollback mechanism are confirmed.

The controller will own:

- Android gateway lifecycle, backend connectivity and offline event delivery;
- addressed cell commands and actual door state;
- service mode and persistent blocks;
- future technician access and command authorization;
- advertising, audio, updates, rollback and recovery;
- Internet connectivity and optional location reporting.

See `../docs/open-questions.md` before choosing the runtime stack.
