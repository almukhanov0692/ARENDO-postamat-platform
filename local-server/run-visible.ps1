$ErrorActionPreference = 'Stop'
$project = Split-Path -Parent $PSScriptRoot
Set-Location $project

Write-Host 'ARENDO local backend monitor' -ForegroundColor Cyan
Write-Host 'WebSocket: ws://<IPv4 ноутбука>:8765/v1/device/socket' -ForegroundColor DarkGray
Write-Host 'Для ngrok: ngrok http 8765, затем wss://<домен>/v1/device/socket' -ForegroundColor DarkGray
Write-Host 'Admin UI:  http://127.0.0.1:8766/ (только на ноутбуке)' -ForegroundColor DarkGray
Write-Host 'Токен будет запрошен скрытым вводом; такой же токен введи в Android.' -ForegroundColor DarkGray
Write-Host 'Ожидание Android...' -ForegroundColor Yellow
Write-Host ''

py -3 'local-server\postamat_server.py'
