import fs from "node:fs/promises";
import { SpreadsheetFile, Workbook } from "@oai/artifact-tool";

const outputDir = "C:/Users/user/Documents/New project 4/outputs/01a0b93c-c8db-7903-ba1c-90f647926160";
const filePath = `${outputDir}/ARENDO_purchase_estimate_2026-09-28.xlsx`;
const previewPath = "C:/Users/user/Documents/New project 4/tmp/purchase-estimate-builder/preview.png";

await fs.mkdir(outputDir, { recursive: true });

const workbook = Workbook.create();
const sheet = workbook.worksheets.add("Смета");
sheet.showGridLines = false;

sheet.getRange("A1:F1").merge();
sheet.getRange("A1").values = [["ARENDO | Смета закупки"]];
sheet.getRange("A2:F2").merge();
sheet.getRange("A2").values = [["Предварительный расчёт в тенге по курсу НБ РК на 28.09.2026; доставка и 5% на ИП включены."]];

sheet.getRange("A4:F4").values = [["№", "Позиция", "Количество", "Цена за единицу", "Валюта", "Сумма"]];
sheet.getRange("A5:E10").values = [
  [1, "PLK на RK3568, 4 ГБ / 32 ГБ", 1, 575, "CNY"],
  [2, "Modbus-модуль BSM1616RB, 16 входов / 16 выходов", 2, 97, "CNY"],
  [3, "Блок питания 12 В, 50 А (600 Вт)", 1, 135, "CNY"],
  [4, "Кабель 4×1,0 мм², катушка 200 м", 2, 146, "CNY"],
  [5, "Электрический замок 12 В / 2 А с обратным сигналом", 34, 30, "CNY"],
  [6, "Расходники и коннекторы", 1, 30000, "KZT"],
];
sheet.getRange("F5:F10").formulas = [
  ["=C5*D5"], ["=C6*D6"], ["=C7*D7"], ["=C8*D8"], ["=C9*D9"], ["=C10*D10"],
];

sheet.getRange("A12:F12").merge();
sheet.getRange("A12").values = [["Итоговый бюджет"]];
sheet.getRange("A13:B19").values = [
  ["Официальный курс CNY/KZT на 28.09.2026", ""],
  ["Оборудование и замки в тенге", ""],
  ["Расходники и коннекторы", ""],
  ["Доставка (резерв)", ""],
  ["База для расчёта ИП", ""],
  ["ИП, 5% от базы", ""],
  ["ИТОГО К ОПЛАТЕ / БЮДЖЕТ", ""],
];
for (let row = 13; row <= 19; row++) sheet.mergeCells(`A${row}:B${row}`);
sheet.getRange("C13").values = [[65.83]];
sheet.getRange("D13:F13").merge();
sheet.getRange("D13").values = [["KZT за 1 CNY · источник: НБ РК"]];
sheet.getRange("C14:C19").formulas = [
  ["=SUM(F5:F9)*C13"],
  ["=F10"],
  ["=30000"],
  ["=SUM(C14:C16)"],
  ["=C17*5%"],
  ["=C17+C18"],
];
sheet.getRange("D14:F19").merge(true);
sheet.getRange("D14:D19").values = [
  ["Оборудование: 2 216 CNY × курс НБ РК"],
  ["Сумма, сообщённая заказчиком"],
  ["Резерв доставки: 30 000 KZT"],
  ["Оборудование + расходники + доставка"],
  ["5% на ИП, по указанию заказчика"],
  ["Закупка + расходники + доставка + 5% на ИП"],
];

sheet.getRange("A20:F24").clear({ applyTo: "all" });

sheet.getRange("A1:F1").format = {
  fill: "#12213A",
  font: { name: "Arial", size: 17, bold: true, color: "#FFFFFF" },
  verticalAlignment: "center",
};
sheet.getRange("A2:F2").format = {
  font: { name: "Arial", size: 10, italic: true, color: "#526174" },
  wrapText: true,
  verticalAlignment: "center",
};
sheet.getRange("A4:F4").format = {
  fill: "#17365D",
  font: { name: "Arial", size: 10, bold: true, color: "#FFFFFF" },
  horizontalAlignment: "center",
  verticalAlignment: "center",
  wrapText: true,
};
sheet.getRange("A5:F10").format = {
  font: { name: "Arial", size: 10, color: "#1F2937" },
  verticalAlignment: "center",
  wrapText: true,
};
sheet.getRange("A5:A10").format.horizontalAlignment = "center";
sheet.getRange("C5:F10").format.horizontalAlignment = "right";
sheet.getRange("D5:D10").format.numberFormat = "#,##0";
sheet.getRange("F5:F10").format.numberFormat = "#,##0";
sheet.getRange("A5:F10").format.borders = {
  insideHorizontal: { style: "thin", color: "#D9E1EA" },
  bottom: { style: "thin", color: "#D9E1EA" },
};
sheet.getRange("A12:F12").format = {
  fill: "#DCE6F1",
  font: { name: "Arial", size: 11, bold: true, color: "#17365D" },
};
sheet.getRange("A13:F19").format = {
  font: { name: "Arial", size: 10, color: "#1F2937" },
  verticalAlignment: "center",
  wrapText: true,
};
sheet.getRange("C13:C19").format.numberFormat = '#,##0.00 "KZT"';
sheet.getRange("C13:C19").format.horizontalAlignment = "right";
sheet.getRange("A13:F19").format.borders = {
  insideHorizontal: { style: "thin", color: "#E2E8F0" },
};
sheet.getRange("A19:F19").format.font = {
  name: "Arial", size: 10, bold: true, color: "#12213A",
};
sheet.getRange("A19:F19").format.fill = "#DDF2E1";

sheet.getRange("A1:F19").format.verticalAlignment = "center";
sheet.getRange("A1").format.rowHeight = 30;
sheet.getRange("A2").format.rowHeight = 32;
sheet.getRange("A4:F4").format.rowHeight = 34;
sheet.getRange("A5:F10").format.rowHeight = 32;
sheet.getRange("A12").format.rowHeight = 24;
sheet.getRange("A13:F19").format.rowHeight = 29;
sheet.getRange("A:A").format.columnWidth = 6;
sheet.getRange("B:B").format.columnWidth = 55;
sheet.getRange("C:C").format.columnWidth = 16;
sheet.getRange("D:D").format.columnWidth = 20;
sheet.getRange("E:E").format.columnWidth = 13;
sheet.getRange("F:F").format.columnWidth = 19;
sheet.freezePanes.freezeRows(4);

workbook.recalculate();

const check = await workbook.inspect({
  kind: "table",
  range: "Смета!A1:F19",
  include: "values,formulas",
  tableMaxRows: 20,
  tableMaxCols: 7,
  maxChars: 10000,
});
console.log(check.ndjson);

const errors = await workbook.inspect({
  kind: "match",
  searchTerm: "#REF!|#DIV/0!|#VALUE!|#NAME\\?|#N/A|#NUM!|#NULL!|#SPILL!|#CALC!",
  options: { useRegex: true, maxResults: 50 },
  summary: "formula error scan",
});
console.log(errors.ndjson);

const preview = await workbook.render({ sheetName: "Смета", range: "A1:F19", scale: 1, format: "png" });
await fs.writeFile(previewPath, new Uint8Array(await preview.arrayBuffer()));

const output = await SpreadsheetFile.exportXlsx(workbook);
await output.save(filePath);
console.log(`Saved ${filePath}`);
