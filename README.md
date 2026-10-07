# ARENDO — проектная библиотека

Папка `ARENDO` — корень рабочей копии и Git-репозитория проекта. Ветка `main` связана с [публичным репозиторием ARENDO](https://github.com/almukhanov0692/ARENDO-postamat-platform). Канонические исходники, требования, руководства, схемы, закупочные материалы и сборки собраны в разделах ниже.

## Навигация по проекту

- [Полный указатель документов](arendo-customer/docs/README.md)
- **Требования:** [ТЗ и требования](arendo-customer/docs/requirements.md), [изображения страниц ТЗ](arendo-customer/docs/requirements/README.md), [архитектура](arendo-customer/docs/architecture.md), [план реализации](arendo-customer/docs/implementation-plan.md), [открытые вопросы](arendo-customer/docs/open-questions.md)
- **Оборудование:** [руководство BSM и заметки по Modbus Poll](arendo-customer/docs/hardware/README_modbus-poll-ru.md), [сохранённые страницы руководства](arendo-customer/docs/hardware/bsm-series-manual/README.md), [предварительные схемы R0](arendo-customer/docs/hardware/electrical-r0/README.md)
- **Связь и интеграция:** [Modbus и backend-протоколы](arendo-customer/docs/protocols/README.md), [модуль Android-шлюза](arendo-customer/device-android/README.md), [локальный сервер-мост](local-server/README.md)
- **Проверки:** [матрица испытаний и приёмки](arendo-customer/docs/testing/README.md), [протокол выездной наладки](arendo-customer/docs/testing/commissioning-record-template-ru.md), [статус реализации](arendo-customer/docs/implementation-status.md), [инженерный аудит](arendo-customer/docs/engineering-audit-2026-10-02-ru.md), [открытые решения](arendo-customer/docs/decisions/)
- **Этапная оплата:** [суммы по ТЗ и доказательства этапов](arendo-customer/docs/commercial/milestone-payment-and-evidence-ru.md), [пакет независимой оценки](arendo-customer/docs/commercial/independent-review-handoff-ru.md)
- **Закупки:** [сводка позиций и статусов](arendo-customer/docs/procurement/procurement-status-inventory-ru.md), [обновление от 30 сентября](arendo-customer/docs/procurement/procurement-update-2026-09-30-ru.md), [первоначальная оценка](arendo-customer/docs/procurement/equipment-estimate-2026-09.md), [пояснение по кабелю и замкам](arendo-customer/docs/procurement/clarification-01-cable-and-lock-quantity-ru.md), [таблица XLSX](arendo-customer/docs/procurement/estimates/ARENDO_purchase_estimate_2026-09-28.xlsx)
- **Отчёты:** [28 сентября](arendo-customer/docs/progress-report-2026-09-28-ru.md), [30 сентября](arendo-customer/docs/progress-report-2026-09-30-ru.md), [2 октября](arendo-customer/docs/progress-report-2026-10-02-ru.md)
- **Релизы:** [APK](output/apk/README.md), [PDF-файлы](output/pdf/README.md), [статус версий](PROJECT_VERSION_STATUS.md)

## Состояние версий

Текущая Android-сборка — **0.2.2-demo от 2 октября 2026 года**. В отчёте указано, что её APK собран, но на стенд не установлен. Последняя описанная установленная версия — **0.2.1 на лабораторном INBOX710**. Отдельного релизного тега для APK в GitHub нет. Обновление документов и привязка рабочей папки к GitHub не меняют версию APK и не означают производственную приёмку.

## Что подтверждено и что требует проверки

Цель программы — карта 28 дверей и 28 выходов. Временная поставочная конфигурация — 10 выходов и обратная связь для 4 дверей. Отдельный этап 2 по ТЗ предусматривает стенд на 14 замков; выполнение этого критерия имеющимися материалами не подтверждено. Наличие исходников, симулятора или APK не подтверждает приёмку реального шкафа и замков.

Страницы Modbus Poll пользователя показывают COM11 и 8E1, а руководство BSM указывает заводские 9600 8N1. Скриншот функции 16 не подтверждает команду управления реле; руководство указывает функции 05 и 0F. Сверьте модель, ревизию и настройки платы перед работой с реальным оборудованием.

В закупочных документах расчёт 28 замков по 2 А даёт 56 А, что выше номинала выбранного источника 12 В/50 А без учёта остальных нагрузок. Заказы и доставка считаются подтверждёнными только при наличии платёжных или транспортных документов.
