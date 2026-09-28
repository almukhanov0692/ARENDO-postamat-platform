# ARENDO local WebSocket bridge

Временный мост для демонстрационной цепочки:

`Android → WebSocket → ноутбук → open_cell → Modbus → ack/event/heartbeat`

Состояния хранятся только в памяти и теряются после остановки процесса. Это тестовый стенд, не production-backend.

## Запуск

Требуется Python 3.10 или новее. Из корня репозитория:

```powershell
py -3 .\local-server\postamat_server.py
```

Или открой отдельное окно PowerShell:

```powershell
Set-ExecutionPolicy -Scope Process Bypass
.\local-server\run-visible.ps1
```

При запуске сервер попросит тестовый токен. Ввод скрыт. Введи тот же токен в настройках Backend на Android. Токен не печатается в журнал.

## Подключение по локальной сети

- WebSocket: `ws://IP_НОУТБУКА:8765/v1/device/socket`
- Панель управления: `http://127.0.0.1:8766/` — открывается только на ноутбуке.

Для Wi-Fi укажи в приложении IPv4 ноутбука, например `ws://192.168.1.20:8765/v1/device/socket`. Android и ноутбук должны быть в одной сети; Windows Firewall должен разрешить входящие подключения к порту 8765.

## Подключение через ngrok

На ноутбуке, где работает мост, запусти:

```powershell
ngrok http 8765
```

В Android укажи выданный ngrok-домен с WebSocket-путём, например:
`wss://example.ngrok.app/v1/device/socket`.

Публикуется только порт WebSocket `8765`. Не публикуй порт панели `8766`. Токен обязателен, но ngrok делает тестовый сервер доступным из интернета, поэтому используй отдельный временный токен и останови туннель после демонстрации.

## Что проверяет стенд

- `hello` / `welcome` и heartbeat;
- адресную команду `open_cell` и подтверждение `ack`;
- входы X1-X4 как демонстрационную обратную связь D01-D04;
- D05-D10 как выходы без датчика двери: статус остаётся `unknown`;
- `cell_report` и события в панели/консоли.

Команды панели из PowerShell на ноутбуке:

```powershell
Invoke-RestMethod 'http://127.0.0.1:8766/api/command/open?cell=01'
Invoke-RestMethod 'http://127.0.0.1:8766/api/command/report'
Invoke-RestMethod 'http://127.0.0.1:8766/api/state'
```

Локальный мост не заменяет сервер Сарвара. Здесь нет базы данных, управления пользователями, производственной авторизации, подписи команд и durable-журнала.

## Тесты

```powershell
py -3 -m unittest discover -s local-server -p 'test_*.py'
```
