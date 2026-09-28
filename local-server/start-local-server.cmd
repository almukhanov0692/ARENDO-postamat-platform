@echo off
setlocal
cd /d "%~dp0.."
echo ARENDO local server
echo WebSocket: ws://IP-NOTEBOOK:8765/v1/device/socket
echo Admin:     http://127.0.0.1:8766/
echo A device token is required. Python will prompt for it without echoing input.
echo Close this window to stop the server.
py -3 "local-server\postamat_server.py"
pause
