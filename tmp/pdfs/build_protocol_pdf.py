from pathlib import Path
from xml.sax.saxutils import escape

from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER, TA_LEFT
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import mm
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import (
    BaseDocTemplate,
    Frame,
    PageTemplate,
    Paragraph,
    PageBreak,
    Preformatted,
    Spacer,
    Table,
    TableStyle,
    KeepTogether,
)


ROOT = Path(r"C:\Users\user\Documents\New project 4")
OUT = ROOT / "output" / "pdf" / "ARENDO_Postamat_Backend_Protocol_MVP.pdf"


def register_fonts():
    regular = Path(r"C:\Windows\Fonts\arial.ttf")
    bold = Path(r"C:\Windows\Fonts\arialbd.ttf")
    mono = Path(r"C:\Windows\Fonts\consola.ttf")
    pdfmetrics.registerFont(TTFont("Arial", str(regular)))
    pdfmetrics.registerFont(TTFont("Arial-Bold", str(bold)))
    pdfmetrics.registerFont(TTFont("Consolas", str(mono)))


register_fonts()

PAGE_W, PAGE_H = A4
NAVY = colors.HexColor("#0B1220")
BLUE = colors.HexColor("#2563EB")
PALE_BLUE = colors.HexColor("#EAF2FF")
GREEN = colors.HexColor("#15803D")
PALE_GREEN = colors.HexColor("#ECFDF3")
AMBER = colors.HexColor("#A16207")
PALE_AMBER = colors.HexColor("#FFF8E7")
RED = colors.HexColor("#B42318")
PALE_RED = colors.HexColor("#FFF1F0")
INK = colors.HexColor("#172033")
MUTED = colors.HexColor("#5D687A")
LINE = colors.HexColor("#D7DEE9")
LIGHT = colors.HexColor("#F5F7FB")


styles = getSampleStyleSheet()
styles.add(ParagraphStyle(
    name="CoverTitle", fontName="Arial-Bold", fontSize=25, leading=30,
    textColor=colors.white, alignment=TA_LEFT, spaceAfter=8,
))
styles.add(ParagraphStyle(
    name="CoverSub", fontName="Arial", fontSize=12, leading=18,
    textColor=colors.HexColor("#D9E5FF"), alignment=TA_LEFT,
))
styles.add(ParagraphStyle(
    name="H1x", fontName="Arial-Bold", fontSize=17, leading=22,
    textColor=NAVY, spaceBefore=7, spaceAfter=8,
))
styles.add(ParagraphStyle(
    name="H2x", fontName="Arial-Bold", fontSize=12.5, leading=16,
    textColor=BLUE, spaceBefore=8, spaceAfter=5,
))
styles.add(ParagraphStyle(
    name="Bodyx", fontName="Arial", fontSize=9.3, leading=13.2,
    textColor=INK, spaceAfter=5,
))
styles.add(ParagraphStyle(
    name="Smallx", fontName="Arial", fontSize=8, leading=10.5,
    textColor=MUTED, spaceAfter=3,
))
styles.add(ParagraphStyle(
    name="Tablex", fontName="Arial", fontSize=7.7, leading=10.2,
    textColor=INK,
))
styles.add(ParagraphStyle(
    name="TableHeadx", fontName="Arial-Bold", fontSize=7.8, leading=10.2,
    textColor=colors.white,
))
styles.add(ParagraphStyle(
    name="Codex", fontName="Consolas", fontSize=7.2, leading=9.1,
    textColor=INK, leftIndent=0,
))
styles.add(ParagraphStyle(
    name="Calloutx", fontName="Arial", fontSize=9, leading=12.5,
    textColor=INK, spaceAfter=0,
))


def P(text, style="Bodyx"):
    return Paragraph(text, styles[style])


def code(text):
    return Preformatted(text.strip("\n"), styles["Codex"])


def box(flowables, bg=LIGHT, border=LINE, pad=9):
    t = Table([[flowables]], colWidths=[174 * mm])
    t.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, -1), bg),
        ("BOX", (0, 0), (-1, -1), 0.6, border),
        ("LEFTPADDING", (0, 0), (-1, -1), pad),
        ("RIGHTPADDING", (0, 0), (-1, -1), pad),
        ("TOPPADDING", (0, 0), (-1, -1), pad),
        ("BOTTOMPADDING", (0, 0), (-1, -1), pad),
    ]))
    return t


def table(headers, rows, widths):
    data = [[P(h, "TableHeadx") for h in headers]]
    for row in rows:
        data.append([P(str(value), "Tablex") for value in row])
    t = Table(data, colWidths=widths, repeatRows=1, hAlign="LEFT")
    t.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, 0), NAVY),
        ("GRID", (0, 0), (-1, -1), 0.35, LINE),
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("LEFTPADDING", (0, 0), (-1, -1), 6),
        ("RIGHTPADDING", (0, 0), (-1, -1), 6),
        ("TOPPADDING", (0, 0), (-1, -1), 5),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 5),
        ("ROWBACKGROUNDS", (0, 1), (-1, -1), [colors.white, LIGHT]),
    ]))
    return t


def bullet(text):
    return P("<font color='#2563EB'>•</font> " + text, "Bodyx")


def header_footer(canvas, doc):
    canvas.saveState()
    if doc.page > 1:
        canvas.setStrokeColor(LINE)
        canvas.setLineWidth(0.5)
        canvas.line(18 * mm, PAGE_H - 14 * mm, PAGE_W - 18 * mm, PAGE_H - 14 * mm)
        canvas.setFont("Arial-Bold", 8)
        canvas.setFillColor(NAVY)
        canvas.drawString(18 * mm, PAGE_H - 10.5 * mm, "ARENDO | Протокол постамата")
        canvas.setFont("Arial", 8)
        canvas.setFillColor(MUTED)
        canvas.drawRightString(PAGE_W - 18 * mm, PAGE_H - 10.5 * mm, "MVP v1.0 | 19.09.2026")
    canvas.setStrokeColor(LINE)
    canvas.setLineWidth(0.5)
    canvas.line(18 * mm, 14 * mm, PAGE_W - 18 * mm, 14 * mm)
    canvas.setFont("Arial", 7.5)
    canvas.setFillColor(MUTED)
    canvas.drawString(18 * mm, 9 * mm, "Для согласования backend и Android-контроллера")
    canvas.drawRightString(PAGE_W - 18 * mm, 9 * mm, f"Стр. {doc.page}")
    canvas.restoreState()


doc = BaseDocTemplate(
    str(OUT), pagesize=A4, leftMargin=18 * mm, rightMargin=18 * mm,
    topMargin=20 * mm, bottomMargin=19 * mm,
    title="ARENDO Postamat Backend Protocol MVP",
    author="ARENDO / Android controller",
)
frame = Frame(doc.leftMargin, doc.bottomMargin, doc.width, doc.height, id="normal")
doc.addPageTemplates([PageTemplate(id="main", frames=frame, onPage=header_footer)])

story = []

# Cover
cover = Table([[
    [
        P("ARENDO", "CoverTitle"),
        P("Протокол интеграции постамата", "CoverTitle"),
        P("Backend <-> WebSocket <-> Android", "CoverSub"),
        Spacer(1, 7 * mm),
        P("Готовый документ для backend-разработчика", "CoverSub"),
        P("MVP: команда открытия, состояние двери, подтверждение закрытия и контроль связи", "CoverSub"),
    ]
]], colWidths=[174 * mm], rowHeights=[64 * mm])
cover.setStyle(TableStyle([
    ("BACKGROUND", (0, 0), (-1, -1), NAVY),
    ("BOX", (0, 0), (-1, -1), 0, NAVY),
    ("LEFTPADDING", (0, 0), (-1, -1), 14 * mm),
    ("RIGHTPADDING", (0, 0), (-1, -1), 14 * mm),
    ("TOPPADDING", (0, 0), (-1, -1), 12 * mm),
    ("BOTTOMPADDING", (0, 0), (-1, -1), 8 * mm),
]))
story += [Spacer(1, 18 * mm), cover, Spacer(1, 8 * mm)]
story.append(P("Назначение документа", "H1x"))
story.append(P("Документ фиксирует фактический контракт между сервером и контроллером постамата. Его задача - чтобы backend отправлял команды в одном формате, Android однозначно понимал ячейку, а сервер принимал только подтвержденные физические состояния.", "Bodyx"))
story.append(box([
    P("Коротко о готовности", "H2x"),
    bullet("Android-приложение подключается к WebSocket и отправляет hello/heartbeat."),
    bullet("Команды open_cell, confirm_closed и cell_report принимаются."),
    bullet("Ячейки D1-D4 передаются через единый идентификатор cellCode."),
    bullet("События door_opened и door_closed видны в журнале и отправляются на backend."),
    bullet("Локальный сервер ноутбука используется для стендовой проверки; production backend должен заменить его своим endpoint."),
], bg=PALE_GREEN, border=colors.HexColor("#A7D8B7")))
story.append(Spacer(1, 4 * mm))
story.append(P("Что backend должен сделать по этому документу", "H2x"))
story.append(P("Реализовать WebSocket endpoint, регистрацию устройства, маршрутизацию команд по postamatId и cellCode, хранение последнего состояния, heartbeat timeout, идемпотентность команд и корректную бизнес-логику аренды. Дизайн мобильного приложения в этот контракт не входит.", "Bodyx"))
story.append(PageBreak())

# 1 architecture
story.append(P("1. Архитектура и зоны ответственности", "H1x"))
story.append(P("Основной канал - WebSocket. MQTT в текущей архитектуре не используется. Детали аппаратного подключения устройства в этот документ не входят.", "Bodyx"))
story.append(box([code(r'''
Пользователь / мобильное приложение клиента
                 |
                 | HTTP API: бронь, QR, оплата
                 v
Backend (API + БД + очередь команд)
                 |
                 | WebSocket /v1/device/socket
                 v
Android-приложение постамата
                 |
                 v
Состояния ячеек и команды открытия/закрытия
''')], bg=LIGHT, border=LINE))
story.append(P("Каналы", "H2x"))
story.append(table(
    ["Канал", "Назначение", "Контракт"],
    [
        ["Backend -> Android", "Команды открытия, закрытия, отчет и сервис", "WebSocket JSON"],
        ["Android -> Backend", "hello, heartbeat, ack и физические события", "WebSocket JSON"],
        ["Реклама", "Получение плейлиста и файлов", "HTTP; WebSocket только сообщает playlist_changed"],
    ], [34 * mm, 72 * mm, 68 * mm]))
story.append(P("Идентификаторы", "H2x"))
story.append(table(
    ["Поле", "Правило"],
    [
        ["postamatId", "Уникальный ID постамата, например map-7. Не менять между подключениями."],
        ["cellCode", "Строка с двумя цифрами: 01, 02, 03, 04. В интерфейсе можно показывать D1, D2, D3, D4."],
        ["command id", "Уникальный ID команды. По нему backend и устройство обеспечивают идемпотентность."],
        ["door", "Только physical-состояние: open, closed или unknown."],
        ["rentalStatus", "Бизнес-состояние ячейки. Его считает backend на основе операций аренды и физических событий."],
    ], [38 * mm, 136 * mm]))
story.append(PageBreak())

# 2 transport/auth
story.append(P("2. Подключение WebSocket и авторизация", "H1x"))
story.append(P("Устройство устанавливает одно постоянное соединение с endpoint сервера. Токен передается в HTTP-заголовке WebSocket handshake:", "Bodyx"))
story.append(box([code(r'''GET /v1/device/socket HTTP/1.1
Host: backend.example.com
Upgrade: websocket
Connection: Upgrade
X-Device-Token: <DEVICE_TOKEN>
''')], bg=LIGHT, border=LINE))
story.append(table(
    ["Режим", "Пример URL", "Правило"],
    [
        ["Production", "wss://backend.example.com/v1/device/socket", "Только TLS с проверкой сертификата."],
        ["Локальный стенд", "ws://<laptop-ip>:8765/v1/device/socket", "Только локальная сеть; не использовать в production."],
        ["Тестовый сервер", "wss://<server>/v1/device/socket", "Требует зарегистрированный и активный device token."],
    ], [29 * mm, 77 * mm, 68 * mm]))
story.append(P("Токен", "H2x"))
story.append(P("В production backend хранит только SHA-256 токена и признак active/disabled. Сам токен не печатается в логах и не возвращается в hello/welcome. Неизвестный или отключенный токен должен получить HTTP 401 до установления WebSocket-сессии.", "Bodyx"))
story.append(box([P("Важно для текущего MVP: локальный сервер ноутбука принимает local-dev для стенда. Это не является production-аутентификацией.", "Calloutx")], bg=PALE_AMBER, border=colors.HexColor("#E7C875")))
story.append(P("После успешного handshake", "H2x"))
story.append(P("Android сразу отправляет hello. Backend отвечает welcome. Только после welcome устройство считается согласованным с конкретным postamatId и получает дальнейшие команды.", "Bodyx"))
story.append(P("Настройки heartbeat", "H2x"))
story.append(table(
    ["Параметр", "Значение", "Кто контролирует"],
    [
        ["heartbeatSec", "15 секунд", "Android отправляет heartbeat"],
        ["pingSec", "20 секунд", "Backend отправляет WebSocket ping"],
        ["pongWaitSec", "45 секунд", "Backend закрывает зависшее соединение"],
    ], [42 * mm, 38 * mm, 94 * mm]))
story.append(PageBreak())

# 3 messages
story.append(P("3. Сообщения WebSocket", "H1x"))
story.append(P("Все сообщения - UTF-8 JSON с обязательным полем type. События и heartbeat могут прийти повторно. Backend должен принимать повтор безопасно: состояние перезаписывается, команды дедуплицируются по id.", "Bodyx"))
story.append(P("3.1 Android -> Backend: hello", "H2x"))
story.append(code(r'''{
  "type": "hello",
  "postamatId": "map-7",
  "appVersion": "0.2.1",
  "keyVersion": 0,
  "capabilities": ["locks", "door_sensors", "service_open"],
  "cells": [
    {"code": "01", "door": "closed", "lock": "unknown"},
    {"code": "02", "door": "closed", "lock": "unknown"},
    {"code": "03", "door": "closed", "lock": "unknown"},
    {"code": "04", "door": "closed", "lock": "unknown"}
  ],
  "net": {"kind": "wifi"}
}'''))
story.append(P("Backend проверяет postamatId, регистрирует online-сессию, сохраняет appVersion/capabilities и отправляет welcome.", "Smallx"))
story.append(P("3.2 Backend -> Android: welcome", "H2x"))
story.append(code(r'''{
  "type": "welcome",
  "postamatId": "map-7",
  "postamatName": "Постамат map-7",
  "config": {"heartbeatSec": 15, "pingSec": 20, "pongWaitSec": 45},
  "cells": [
    {"code": "01", "door": "closed", "lock": "unknown", "rentalStatus": "available"},
    {"code": "02", "door": "closed", "lock": "unknown", "rentalStatus": "available"}
  ]
}'''))
story.append(P("Массив cells в welcome может содержать все ячейки постамата. rentalStatus - бизнес-расширение backend; физическое door-состояние приходит от устройства.", "Smallx"))
story.append(PageBreak())

# 4 telemetry
story.append(P("4. Состояния ячеек и события", "H1x"))
story.append(P("Backend получает только внешнее состояние ячейки: D1 open/closed, D2 open/closed, D3 open/closed и D4 open/closed. Способ, которым Android получает это состояние внутри устройства, не является частью backend-контракта.", "Bodyx"))
story.append(P("4.1 heartbeat", "H2x"))
story.append(code(r'''{
  "type": "heartbeat",
  "cells": [
    {"code": "01", "door": "open", "lock": "unknown"},
    {"code": "02", "door": "closed", "lock": "unknown"}
  ],
  "net": {"kind": "wifi"},
  "problems": []
}'''))
story.append(P("Heartbeat - периодический снимок. Он не должен сам запускать оплату или аренду. Backend использует его для online/offline и диагностики.", "Smallx"))
story.append(P("4.2 event", "H2x"))
story.append(code(r'''{
  "type": "event",
  "kind": "door_opened",
  "cellCode": "01",
  "detail": {"source": "lock_relay"}
}

{
  "type": "event",
  "kind": "door_closed",
  "cellCode": "01",
  "detail": {"source": "door_sensor"}
}'''))
story.append(table(
    ["kind", "Когда отправляется", "Действие backend"],
    [
        ["door_opened", "Android подтвердил открытие ячейки", "Зафиксировать открытие; аренду начинать только по успешному ack команды."],
        ["door_closed", "Android подтвердил закрытие ячейки", "Обновить physical state; закрытие аренды подтверждать только в нужном бизнес-контексте."],
        ["door_jammed", "Есть команда, но механизм не сработал", "Создать incident и не считать операцию успешной."],
        ["net_degraded", "Проблема сети", "Показать offline/degraded, не терять последнюю команду и состояние."],
        ["power_lost", "Питание контроллера потеряно", "Зафиксировать incident; после восстановления ждать hello/heartbeat."],
        ["tamper", "Тревога вскрытия, если появится датчик", "Создать incident высокой важности."],
    ], [31 * mm, 66 * mm, 77 * mm]))
story.append(box([P("Если Android не может достоверно определить lock, передается lock=unknown. Backend не должен заменять unknown на ok самостоятельно.", "Calloutx")], bg=PALE_AMBER, border=colors.HexColor("#E7C875")))
story.append(PageBreak())

# 5 commands
story.append(P("5. Команды backend -> Android", "H1x"))
story.append(P("Каждая команда имеет id, kind, payload, expiresAt и signature. Для MVP signature может быть проверена в production-слое позже, но backend должен уже формировать это поле.", "Bodyx"))
story.append(code(r'''{
  "type": "command",
  "id": "cmd-20260919-00001",
  "kind": "open_cell",
  "payload": {"cellCode": "01"},
  "expiresAt": "2026-09-19T12:05:00Z",
  "signature": "<HMAC-SHA256>"
}'''))
story.append(table(
    ["kind", "payload", "MVP-результат"],
    [
        ["open_cell", "cellCode: 01..04", "Открыть указанную ячейку; вернуть ack result.kind=door_open_ack и отправить door_opened."],
        ["confirm_closed", "cellCode: 01..04", "Проверить, что дверь закрыта; вернуть ack result.kind=door_close_ack или door_not_closed."],
        ["cell_report", "нет", "Вернуть ack с полным cells-снимком вне очереди."],
        ["reboot", "reason", "Зарезервировано; не выполнять при открытой двери."],
        ["update_app", "version, url, checksum", "Зарезервировано для обновления приложения."],
        ["rotate_token", "token", "Зарезервировано для безопасной смены токена."],
        ["sync_keys", "keys, keyVersion", "Зарезервировано для офлайн QR/Ed25519."],
    ], [34 * mm, 55 * mm, 85 * mm]))
story.append(P("Проверки перед выполнением команды", "H2x"))
for text in [
    "Проверить, что cellCode существует и относится к этому postamatId.",
    "Проверить expiresAt. Просроченная команда не должна открывать дверь.",
    "Не повторять физическое действие при повторном id команды.",
    "При потере связи команда, которую Android не получил, не должна открыться позже после восстановления.",
    "Для open_cell backend считает операцию успешной только после ack с door_open_ack.",
]:
    story.append(bullet(text))
story.append(PageBreak())

# 6 ack errors
story.append(P("6. Подтверждения и ошибки", "H1x"))
story.append(P("6.1 Успешное открытие", "H2x"))
story.append(code(r'''{
  "type": "ack",
  "commandId": "cmd-20260919-00001",
  "ok": true,
  "result": {"kind": "door_open_ack", "cellCode": "01"}
}'''))
story.append(P("После ack Android отправляет физическое событие door_opened. Backend должен связать ack и событие по commandId/cellCode или по текущей операции.", "Bodyx"))
story.append(P("6.2 Успешное закрытие", "H2x"))
story.append(code(r'''{
  "type": "ack",
  "commandId": "cmd-20260919-00002",
  "ok": true,
  "result": {"kind": "door_close_ack", "cellCode": "01"}
}'''))
story.append(P("6.3 Типовые ошибки", "H2x"))
story.append(table(
    ["error", "Причина", "Что делает backend"],
    [
        ["invalid_cell", "Неизвестная ячейка", "Не менять состояние аренды; вернуть ошибку API."],
        ["expired", "Команда просрочена", "Закрыть попытку; не открывать дверь повторно."],
        ["device_unavailable", "Android не смог выполнить физическую команду", "Создать incident locker_no_ack, не списывать оплату."],
        ["door_not_closed", "Android не подтвердил закрытие", "Не завершать возврат; показать дверь как open/unknown."],
        ["unsupported_command", "Команда ещё не реализована на Android", "Сохранить ошибку и не повторять бесконечно."],
    ], [38 * mm, 55 * mm, 81 * mm]))
story.append(box([P("Принцип денег: одна телеметрия door_opened или door_closed сама по себе не двигает деньги. Финансовое состояние меняется только по подтвержденному сценарию backend.", "Calloutx")], bg=PALE_RED, border=colors.HexColor("#E9A5A0")))
story.append(PageBreak())

# 7 backend actions
story.append(P("7. Что должен реализовать backend-разработчик", "H1x"))
story.append(table(
    ["Приоритет", "Действие", "Критерий готовности"],
    [
        ["P0", "Зарегистрировать postamatId map-7 и активный device token", "WebSocket handshake не возвращает 401 для тестового устройства."],
        ["P0", "Реализовать /v1/device/socket и auth через X-Device-Token", "Android получает welcome после hello."],
        ["P0", "Направить команду open_cell на нужный postamatId/cellCode", "Команда D1 не может открыть D2."],
        ["P0", "Обработать ack door_open_ack и door_close_ack", "Аренда/возврат меняются только после правильного ack."],
        ["P0", "Сохранять cells и события", "В админке видны D1 open/closed и время последнего heartbeat."],
        ["P0", "Heartbeat timeout", "Нет heartbeat более 45 секунд -> offline/incident."],
        ["P0", "Идемпотентность по command id", "Повтор сообщения не выполняет физическое действие второй раз."],
        ["P1", "Команда cell_report", "Backend может запросить полный снимок ячеек."],
        ["P1", "Корреляция операций аренды", "Одна бронь связана с postamatId, cellCode, commandId."],
        ["P1", "Audit log без токенов", "Токен никогда не попадает в логи и UI."],
        ["P2", "rotate_token, update_app, sync_keys", "Добавить после стабилизации основного MVP."],
    ], [18 * mm, 78 * mm, 78 * mm]))
story.append(P("Серверный API аренды", "H2x"))
story.append(P("Когда клиент завершил оплату и бронь подтверждена, backend выбирает нужный postamatId и cellCode, отправляет open_cell, переводит операцию в pending_open и ждет ack. Только после door_open_ack операция переводится в opened/active. Если ack не пришел или пришел с ошибкой - аренда не стартует, деньги не списываются, создается locker_no_ack.", "Bodyx"))
story.append(PageBreak())

# 8 sequence and failure
story.append(P("8. Основной сценарий MVP", "H1x"))
story.append(code(r'''
1. Бронь и успешная оплата на backend.
2. Backend определяет postamatId=map-7 и cellCode=01.
3. Backend -> Android: command open_cell, cellCode=01.
4. Android выполняет открытие и отвечает ack door_open_ack.
5. Android отправляет event door_opened, cellCode=01.
6. Backend переводит аренду в active/opened.
7. Пользователь закрывает дверь; Android фиксирует закрытие.
8. Android отправляет event door_closed, cellCode=01.
9. Backend отправляет confirm_closed или принимает подтверждение по текущей операции.
10. Android отвечает door_close_ack либо door_not_closed.
11. Backend завершает возврат только при подтвержденном закрытии.
'''))
story.append(P("Сценарий потери связи", "H2x"))
story.append(table(
    ["Ситуация", "Ожидаемое поведение"],
    [
        ["Команда не доставлена", "После восстановления Android не выполняет старую просроченную команду."],
        ["Команда доставлена, ack потерян", "Backend не создает вторую команду без проверки command id и текущего состояния."],
        ["Связь восстановилась", "Android отправляет hello и heartbeat; backend повторно синхронизирует состояние."],
        ["Молчание > 45 сек", "postamat offline; активная аренда получает incident."],
        ["Дверь реально открыта, ack потерян", "Событие door_opened после восстановления помогает диагностике; бизнес-решение принимает backend."],
    ], [48 * mm, 126 * mm]))
story.append(box([P("Критично: нельзя отправлять старую open_cell после восстановления связи без проверки expiresAt и idempotency. Иначе дверь может открыться неожиданно.", "Calloutx")], bg=PALE_RED, border=colors.HexColor("#E9A5A0")))
story.append(PageBreak())

# 9 tests
story.append(P("9. Чек-лист совместного тестирования", "H1x"))
story.append(table(
    ["Тест", "Действие", "Ожидаемый результат"],
    [
        ["T1 Handshake", "Подключить Android с активным токеном", "hello -> welcome, устройство online"],
        ["T2 Wrong token", "Подключить с неверным токеном", "HTTP 401, WebSocket не создается"],
        ["T3 Open D1", "Отправить open_cell/01", "ack door_open_ack, D1 open"],
        ["T4 Close D1", "Закрыть D1 штатным способом", "event door_closed, D1 closed"],
        ["T5 Open D4", "Отправить open_cell/04", "Меняется только D4"],
        ["T6 Invalid D5", "Отправить cellCode 05", "ack invalid_cell, физического действия нет"],
        ["T7 Device unavailable", "Сымитировать недоступность устройства", "ошибка выполнения, аренда не стартует"],
        ["T8 Duplicate", "Повторить тот же command id", "Второго физического действия нет"],
        ["T9 Expired", "Отправить просроченную команду", "ack expired, дверь не открывается"],
        ["T10 Network loss", "Отключить Wi-Fi на 60 сек", "offline/incident, после возврата hello и heartbeat"],
        ["T11 Close failure", "Оставить дверь открытой", "door_not_closed, возврат не завершен"],
        ["T12 Report", "Отправить cell_report", "Полный снимок D1-D4 возвращен в ack"],
    ], [28 * mm, 66 * mm, 80 * mm]))
story.append(P("Для демонстрации завтра достаточно T1, T3, T4, T5, T7 и T10. Остальные тесты обязательны перед production.", "Bodyx"))
story.append(PageBreak())

# 10 current status and notes
story.append(P("10. Текущий статус и границы MVP", "H1x"))
story.append(P("Готово с нашей стороны", "H2x"))
for text in [
    "Android-приложение с WebSocket-клиентом постамата.",
    "Единая модель ячеек D1-D4 и статусов open/closed.",
    "WebSocket-клиент с hello, welcome, heartbeat, command, ack и event.",
    "Статусы двери open/closed и отдельный журнал обратной связи.",
    "Локальный WebSocket-сервер ноутбука и панель проверки команд.",
    "Проверка работы Android и backend через Wi-Fi.",
]:
    story.append(bullet(text))
story.append(P("Не является готовым production", "H2x"))
for text in [
    "Локальный сервер ноутбука - только стенд, он не заменяет основной backend.",
    "В текущем стенде lock=unknown, пока нет отдельного датчика замка.",
    "Офлайн QR/open, Ed25519, rotate_token, update_app и mTLS требуют отдельной реализации.",
    "Тестовый TLS без проверки сертификата нельзя оставлять в production.",
    "Supabase и MQTT в текущем MVP не участвуют; основной канал - WebSocket.",
]:
    story.append(bullet(text))
story.append(box([
    P("Финальное правило для согласования", "H2x"),
    P("Backend управляет бизнес-сценарием, оплатой, бронью и идентификаторами. Android выполняет команды и сообщает только то, что реально видит: D1 open/closed, D2 open/closed и так далее. Поэтому сервер не должен рисовать или считать состояние, которого нет в hello, heartbeat или event.", "Calloutx"),
], bg=PALE_BLUE, border=colors.HexColor("#AFC7F4")))
story.append(Spacer(1, 8 * mm))
story.append(P("Контактная точка для backend", "H2x"))
story.append(P("При изменении endpoint, postamatId, токена, числа ячеек или формата команд backend-разработчик должен сначала согласовать изменение контракта. В противном случае Android будет подключен, но команда может не дойти до нужной ячейки.", "Bodyx"))

doc.build(story)
print(OUT)
