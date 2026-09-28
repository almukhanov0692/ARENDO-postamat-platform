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
    KeepTogether,
    PageBreak,
    PageTemplate,
    Paragraph,
    Preformatted,
    Spacer,
    Table,
    TableStyle,
)


ROOT = Path(__file__).resolve().parents[2]
OUTPUT = ROOT / "output" / "pdf" / "postamat-protocol-map-7.pdf"

FONT_REGULAR = Path(r"C:\Windows\Fonts\arial.ttf")
FONT_BOLD = Path(r"C:\Windows\Fonts\arialbd.ttf")
FONT_MONO = Path(r"C:\Windows\Fonts\consola.ttf")

pdfmetrics.registerFont(TTFont("Arendo", str(FONT_REGULAR)))
pdfmetrics.registerFont(TTFont("Arendo-Bold", str(FONT_BOLD)))
pdfmetrics.registerFont(TTFont("Arendo-Mono", str(FONT_MONO)))

NAVY = colors.HexColor("#0F172A")
BLUE = colors.HexColor("#2563EB")
PALE_BLUE = colors.HexColor("#EFF6FF")
PALE_GRAY = colors.HexColor("#F8FAFC")
MUTED = colors.HexColor("#64748B")
TEXT = colors.HexColor("#172033")
GREEN = colors.HexColor("#166534")
PALE_GREEN = colors.HexColor("#F0FDF4")
RED = colors.HexColor("#991B1B")
PALE_RED = colors.HexColor("#FEF2F2")
LINE = colors.HexColor("#D9E1EC")

styles = getSampleStyleSheet()
styles.add(ParagraphStyle(
    name="CoverTitle", fontName="Arendo-Bold", fontSize=25, leading=30,
    textColor=NAVY, alignment=TA_CENTER, spaceAfter=8,
))
styles.add(ParagraphStyle(
    name="CoverSubtitle", fontName="Arendo", fontSize=13, leading=18,
    textColor=MUTED, alignment=TA_CENTER, spaceAfter=18,
))
styles.add(ParagraphStyle(
    name="H1Arendo", fontName="Arendo-Bold", fontSize=17, leading=22,
    textColor=NAVY, spaceBefore=7, spaceAfter=9,
))
styles.add(ParagraphStyle(
    name="H2Arendo", fontName="Arendo-Bold", fontSize=12.5, leading=16,
    textColor=NAVY, spaceBefore=8, spaceAfter=5,
))
styles.add(ParagraphStyle(
    name="BodyArendo", fontName="Arendo", fontSize=9.8, leading=14,
    textColor=TEXT, spaceAfter=5,
))
styles.add(ParagraphStyle(
    name="SmallArendo", fontName="Arendo", fontSize=8.5, leading=12,
    textColor=MUTED, spaceAfter=4,
))
styles.add(ParagraphStyle(
    name="TableArendo", fontName="Arendo", fontSize=8.4, leading=11,
    textColor=TEXT,
))
styles.add(ParagraphStyle(
    name="TableBoldArendo", fontName="Arendo-Bold", fontSize=8.4, leading=11,
    textColor=NAVY,
))
styles.add(ParagraphStyle(
    name="TableHeaderArendo", fontName="Arendo-Bold", fontSize=8.4, leading=11,
    textColor=colors.white,
))
styles.add(ParagraphStyle(
    name="CalloutArendo", fontName="Arendo-Bold", fontSize=10.4, leading=15,
    textColor=NAVY, spaceAfter=0,
))
styles.add(ParagraphStyle(
    name="CodeArendo", fontName="Arendo-Mono", fontSize=7.2, leading=9.2,
    textColor=colors.HexColor("#E2E8F0"), leftIndent=0, rightIndent=0,
))
styles.add(ParagraphStyle(
    name="CodeLabel", fontName="Arendo-Bold", fontSize=8.2, leading=11,
    textColor=colors.HexColor("#CBD5E1"),
))


def para(text, style="BodyArendo"):
    return Paragraph(text, styles[style])


def bullet(text):
    return Paragraph("<font color='#2563EB'>•</font>&nbsp;" + text, styles["BodyArendo"])


def section(number, title):
    return Paragraph(f"{number}. {title}", styles["H1Arendo"])


def table(data, widths, header=True, row_heights=None):
    converted = []
    for row_index, row in enumerate(data):
        converted.append([
            Paragraph(str(cell), styles["TableHeaderArendo" if header and row_index == 0
                     else "TableArendo"])
            for cell in row
        ])
    t = Table(converted, colWidths=widths, rowHeights=row_heights, repeatRows=1 if header else 0)
    commands = [
        ("GRID", (0, 0), (-1, -1), 0.45, LINE),
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("LEFTPADDING", (0, 0), (-1, -1), 7),
        ("RIGHTPADDING", (0, 0), (-1, -1), 7),
        ("TOPPADDING", (0, 0), (-1, -1), 6),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 6),
    ]
    if header:
        commands.extend([
            ("BACKGROUND", (0, 0), (-1, 0), NAVY),
            ("TEXTCOLOR", (0, 0), (-1, 0), colors.white),
        ])
        for index in range(1, len(data)):
            if index % 2 == 0:
                commands.append(("BACKGROUND", (0, index), (-1, index), PALE_GRAY))
    t.setStyle(TableStyle(commands))
    return t


def callout(text, background=PALE_BLUE, border=BLUE):
    t = Table([[para(text, "CalloutArendo")]], colWidths=[170 * mm])
    t.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, -1), background),
        ("BOX", (0, 0), (-1, -1), 0.8, border),
        ("LEFTPADDING", (0, 0), (-1, -1), 11),
        ("RIGHTPADDING", (0, 0), (-1, -1), 11),
        ("TOPPADDING", (0, 0), (-1, -1), 9),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 9),
    ]))
    return t


def code_block(label, text):
    content = escape(text).replace("\n", "<br/>")
    code = Table([[Paragraph(label, styles["CodeLabel"])], [Paragraph(content, styles["CodeArendo"])]] , colWidths=[170 * mm])
    code.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, -1), NAVY),
        ("BOX", (0, 0), (-1, -1), 0.5, colors.HexColor("#334155")),
        ("LEFTPADDING", (0, 0), (-1, -1), 10),
        ("RIGHTPADDING", (0, 0), (-1, -1), 10),
        ("TOPPADDING", (0, 0), (-1, 0), 7),
        ("BOTTOMPADDING", (0, 0), (-1, 0), 3),
        ("TOPPADDING", (0, 1), (-1, 1), 5),
        ("BOTTOMPADDING", (0, 1), (-1, 1), 8),
    ]))
    return code


def on_page(canvas, doc):
    canvas.saveState()
    width, height = A4
    canvas.setFillColor(NAVY)
    canvas.rect(0, height - 13 * mm, width, 13 * mm, fill=1, stroke=0)
    canvas.setFillColor(colors.white)
    canvas.setFont("Arendo-Bold", 8.5)
    canvas.drawString(18 * mm, height - 8.5 * mm, "ARENDO  |  ПРОТОКОЛ ПОСТАМАТА")
    canvas.setFillColor(MUTED)
    canvas.setFont("Arendo", 8)
    canvas.drawString(18 * mm, 10 * mm, "MVP map-7  |  Android + Modbus RS-485 + WebSocket")
    canvas.drawRightString(width - 18 * mm, 10 * mm, f"Страница {doc.page}")
    canvas.restoreState()


class ProtocolDoc(BaseDocTemplate):
    def __init__(self, filename):
        super().__init__(filename, pagesize=A4, leftMargin=20 * mm, rightMargin=20 * mm,
                         topMargin=21 * mm, bottomMargin=18 * mm,
                         title="Протокол интеграции постамата map-7",
                         author="ARENDO")
        frame = Frame(self.leftMargin, self.bottomMargin, self.width, self.height,
                      id="main", leftPadding=0, rightPadding=0, topPadding=0, bottomPadding=0)
        self.addPageTemplates([PageTemplate(id="protocol", frames=[frame], onPage=on_page)])


story = []

story.append(Spacer(1, 22 * mm))
story.append(Paragraph("Протокол интеграции постамата", styles["CoverTitle"]))
story.append(Paragraph("MVP: Android-приложение, Modbus RS-485 и WebSocket backend", styles["CoverSubtitle"]))
story.append(callout("Цель MVP: бронирование -> команда открытия -> постамат -> подтверждение открытия/закрытия -> приложение."))
story.append(Spacer(1, 10 * mm))
story.append(table([
    ["Параметр", "Значение"],
    ["Постамат", "map-7"],
    ["Физические ячейки стенда", "D1, D2, D3, D4"],
    ["Транспорт", "WebSocket /v1/device/socket"],
    ["Локальный контроллер", "Modbus RTU через USB-RS-485"],
    ["Статус документа", "Готово для интеграции backend"],
], [53 * mm, 117 * mm]))
story.append(Spacer(1, 11 * mm))
story.append(para("Важно: Android не подключается к MongoDB напрямую. Рабочая схема: Android и Modbus общаются с WebSocket backend, а backend работает с MongoDB/Redis.", "SmallArendo"))
story.append(Spacer(1, 5 * mm))
story.append(para("Версия документа: 1.0  |  19.09.2026", "SmallArendo"))
story.append(PageBreak())

story.append(section("1", "Соответствие ячеек и Modbus"))
story.append(para("В протоколе backend номер ячейки передается как двухзначный code. В интерфейсе приложения тот же номер показывается в коротком виде D1, D2, D3, D4."))
story.append(table([
    ["Интерфейс", "Вход кнопки", "Выход LED", "cellCode в JSON", "Смысл"],
    ["D1", "X1", "Y1", "01", "open / closed"],
    ["D2", "X2", "Y2", "02", "open / closed"],
    ["D3", "X3", "Y3", "03", "open / closed"],
    ["D4", "X4", "Y4", "04", "open / closed"],
], [25 * mm, 28 * mm, 28 * mm, 36 * mm, 53 * mm]))
story.append(Spacer(1, 7 * mm))
story.append(section("2", "Что уже работает на нашей стороне"))
for item in [
    "Android-приложение подключается к Modbus RTU через USB-RS-485.",
    "Команда backend open_cell включает нужный Y-выход.",
    "LED включен - ячейка считается открытой: D1 open.",
    "Нажатие X1-X4 выключает соответствующий LED - ячейка считается закрытой: D1 closed.",
    "WebSocket подключение к тестовому backend работает.",
    "Сообщения hello, welcome и heartbeat проходят.",
    "Подтверждения ack и события door_opened / door_closed отправляются.",
    "В Android есть отдельный журнал событий, сохранение и очистка журнала.",
]:
    story.append(bullet(item))
story.append(callout("Ограничение MVP: настоящего замка и датчика двери пока нет. LED и кнопка имитируют физические состояния. Поэтому lock = unknown, а занятость ячейки устройство не определяет.", PALE_RED, RED))
story.append(PageBreak())

story.append(section("3", "Обмен сообщениями"))
story.append(para("Все сообщения - JSON с полем type. Примеры ниже приведены в формате JSONC для пояснений. Комментарии после // не отправляются по сети."))
story.append(para("Android -> backend: hello после подключения", "H2Arendo"))
story.append(code_block("HELLO", '''{
  "type": "hello",
  "postamatId": "map-7",
  "appVersion": "0.1.0",
  "keyVersion": 0,
  "capabilities": ["locks", "buttons", "indicators", "demo_buttons_as_doors"],
  "cells": [
    {"code": "01", "door": "closed", "lock": "unknown"},
    {"code": "02", "door": "closed", "lock": "unknown"},
    {"code": "03", "door": "closed", "lock": "unknown"},
    {"code": "04", "door": "closed", "lock": "unknown"}
  ],
  "net": {"kind": "wifi"}
}'''))
story.append(Spacer(1, 5 * mm))
story.append(para("Backend -> Android: welcome после hello", "H2Arendo"))
story.append(code_block("WELCOME", '''{
  "type": "welcome",
  "postamatId": "map-7",
  "postamatName": "Постамат map-7",
  "cells": [
    {"code": "01", "door": "closed", "lock": "unknown"},
    {"code": "02", "door": "closed", "lock": "unknown"},
    {"code": "03", "door": "closed", "lock": "unknown"},
    {"code": "04", "door": "closed", "lock": "unknown"}
  ],
  "config": {"heartbeatSec": 15, "pingSec": 20, "pongWaitSec": 45}
}'''))
story.append(Spacer(1, 5 * mm))
story.append(para("Для физического стенда backend должен использовать только cellCode 01-04. Ячейки 05-08 пока не подключены.", "SmallArendo"))
story.append(PageBreak())

story.append(section("4", "Открытие ячейки"))
story.append(para("Команда приходит от backend после успешной проверки бронирования и оплаты."))
story.append(code_block("BACKEND -> ANDROID: COMMAND OPEN_CELL", '''{
  "type": "command",
  "id": "command-123",
  "kind": "open_cell",
  "payload": {"cellCode": "01"},
  "expiresAt": 1800000000
}'''))
story.append(Spacer(1, 4 * mm))
story.append(para("Android проверяет срок действия, включает Y1 и отправляет подтверждение:", "BodyArendo"))
story.append(code_block("ANDROID -> BACKEND: ACK", '''{
  "type": "ack",
  "commandId": "command-123",
  "ok": true,
  "result": {"kind": "door_open_ack", "cellCode": "01"}
}'''))
story.append(Spacer(1, 4 * mm))
story.append(code_block("ANDROID -> BACKEND: EVENT", '''{
  "type": "event",
  "kind": "door_opened",
  "cellCode": "01",
  "detail": {"source": "demo_relay", "simulated": true}
}'''))
story.append(Spacer(1, 5 * mm))
story.append(callout("Только успешный ack с result.kind = door_open_ack является основанием начать аренду и оплату.", PALE_GREEN, colors.HexColor("#22C55E")))
story.append(Spacer(1, 5 * mm))
story.append(para("Если ячейка неизвестна, команда просрочена или Modbus недоступен, Android отправляет ack с ok = false. Backend не должен начинать аренду.", "BodyArendo"))
story.append(PageBreak())

story.append(section("5", "Закрытие ячейки"))
story.append(para("Пользователь нажимает физическую кнопку. Android выключает соответствующий LED и сообщает backend, что дверь закрыта."))
story.append(code_block("ANDROID -> BACKEND: DOOR CLOSED", '''{
  "type": "event",
  "kind": "door_closed",
  "cellCode": "01",
  "detail": {"source": "button", "simulated": true}
}'''))
story.append(Spacer(1, 4 * mm))
story.append(para("Backend может дополнительно запросить подтверждение:", "BodyArendo"))
story.append(code_block("BACKEND -> ANDROID: CONFIRM CLOSED", '''{
  "type": "command",
  "id": "command-124",
  "kind": "confirm_closed",
  "payload": {"cellCode": "01"}
}'''))
story.append(Spacer(1, 4 * mm))
story.append(code_block("ANDROID -> BACKEND: CLOSE ACK", '''{
  "type": "ack",
  "commandId": "command-124",
  "ok": true,
  "result": {"kind": "door_close_ack", "cellCode": "01"}
}'''))
story.append(Spacer(1, 5 * mm))
story.append(section("6", "Heartbeat и потеря связи"))
story.append(para("Android отправляет heartbeat каждые 15 секунд. Backend обновляет last_seen_at и статус online."))
story.append(code_block("ANDROID -> BACKEND: HEARTBEAT", '''{
  "type": "heartbeat",
  "cells": [
    {"code": "01", "door": "open", "lock": "unknown"},
    {"code": "02", "door": "closed", "lock": "unknown"},
    {"code": "03", "door": "closed", "lock": "unknown"},
    {"code": "04", "door": "closed", "lock": "unknown"}
  ],
  "net": {"kind": "wifi"},
  "problems": []
}'''))
story.append(Spacer(1, 4 * mm))
for item in [
    "Нет heartbeat около 45 секунд - постамат offline.",
    "Команда с истекшим expiresAt не должна выполняться после восстановления связи.",
    "Повторная команда с тем же id не должна открывать дверь повторно.",
    "Серверный ping должен получать обычный WebSocket pong.",
]:
    story.append(bullet(item))
story.append(PageBreak())

story.append(section("7", "Что должен завершить backend"))
story.append(table([
    ["Задача", "Ожидаемый результат"],
    ["Бронирование и оплата", "После проверки оплаты отправить open_cell для нужного cellCode."],
    ["Начало аренды", "Начинать только после door_open_ack с ok=true."],
    ["Закрытие аренды", "Завершать после door_closed и подтверждения закрытия."],
    ["Статусы для приложения", "Показывать D1 open, D1 closed и аналогично для D2-D4."],
    ["Online/offline", "Контролировать heartbeat и last_seen_at."],
    ["Безопасность команд", "Проверять expiresAt, commandId и повторную доставку."],
    ["Ошибки", "Обработать no ack, modbus_offline, door_not_closed и потерю связи."],
    ["Ячейки стенда", "Для map-7 использовать только 01-04."],
], [52 * mm, 118 * mm]))
story.append(Spacer(1, 8 * mm))
story.append(callout("Итог: Android, Modbus, тестовый WebSocket и обратная связь уже работают. Backend нужно связать с бронированием, оплатой, арендой, статусами пользователя и offline-сценариями.", PALE_GREEN, colors.HexColor("#22C55E")))
story.append(Spacer(1, 9 * mm))
story.append(para("Короткие состояния для интерфейса: D1 open / D1 closed / D2 open / D2 closed / D3 open / D3 closed / D4 open / D4 closed.", "SmallArendo"))

doc = ProtocolDoc(str(OUTPUT))
doc.build(story)
print(OUTPUT)
