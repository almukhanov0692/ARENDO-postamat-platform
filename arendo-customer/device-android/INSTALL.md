# Установка на Android RK3568

Подключить устройство по USB и включить USB debugging.

```powershell
adb devices -l
.\gradlew.bat assembleDebug
adb -s DEVICE_SERIAL install -r -d app\build\outputs\apk\debug\app-debug.apk
adb -s DEVICE_SERIAL shell am start -n kz.arendo.device/.MainActivity
```

Для текущего устройства команда выглядит так:

```powershell
adb -s BF7102351058413 install -r -d app\build\outputs\apk\debug\app-debug.apk
adb -s BF7102351058413 shell am start -n kz.arendo.device/.MainActivity
```

Проверка пакета:

```powershell
adb -s DEVICE_SERIAL shell pm path kz.arendo.device
```

## Arduino

На настоящую Arduino Android APK не устанавливается. Для Arduino потребуется отдельная прошивка (`.ino`/PlatformIO) и команда загрузки, например:

```powershell
arduino-cli compile --fqbn arduino:avr:uno path\to\arendo-rs485
arduino-cli upload -p COM_PORT --fqbn arduino:avr:uno path\to\arendo-rs485
```

Точные `FQBN`, COM-порт и библиотека RS-485 зависят от модели Arduino и конвертера. Их нельзя безопасно угадать.

