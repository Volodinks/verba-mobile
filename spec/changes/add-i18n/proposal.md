# Add UI Localization (Ukrainian + English)

## Why

Поточний мобільний UI зашитий жорстко українською у `strings.xml`. Веб-частина вже отримала повноцінну i18n у форматі JSON-словників (`uk` / `en`) і перемикач у хедері — мобільний має ту саму UX-функцію, щоб користувач міг перемикати інтерфейс і мав узгоджений досвід між платформами.

Контентна мова (`explanation_language` для генерації пояснень OpenAI) керується окремо per-session і не торкається цієї зміни — це виключно про UI.

## What Changes

### ADDED capabilities

- **`i18n`** — підтримка двох UI-локалей (`uk` + `en`), in-app перемикач у TopBar, збереження вибору між запусками, дефолт за системною locale пристрою, перекладені enum-лейбли і повідомлення про помилки API.

### MODIFIED capabilities

Жодних поведінкових змін. Текстові рядки — деталь реалізації існуючих екранів (`folders`, `lessons`, `runs`, `authentication`, `access-control`), не зафіксована у відповідних specs. Сам набір екранів і потоків лишається тим самим.

### Out of scope

- Інші локалі (польська, російська, …) — додаватимуться окремими змінами.
- Локалізація даних адміна (`folder.name`, `lesson.title`, `description`) — це контент, зберігається мовою як адмін написав. Можна додати окремою зміною (`title_translations: { uk, en }`).
- Локалізація голосової підсистеми ([[voice]]) — `TextToSpeech` і `SpeechRecognizer` лишаються `Locale.ENGLISH` (це частина продукту, а не UI).
- RTL-розкладка — обидві поточні мови LTR.
- Локалізація форматування часу/чисел поза стандартним Android `NumberFormat` / `DateTimeFormatter` з поточною locale.

## Impact

- **UI:** усі видимі тексти переходять з захардкоджених на `stringResource(R.string.…)`. У TopBar головного екрана зʼявляється icon-button для перемикання мови (або action у меню).
- **APK size:** додається другий набір ресурсів (`values-en/strings.xml`) — приблизно +5KB після стиснення.
- **Сумісність із веб:** ключі та значення дзеркалять `verba-web/src/i18n/messages/{uk,en}.json` (з поправкою на формат — Android XML замість ICU JSON, `<plurals>` замість `{count, plural, ...}`).
- **Дефолт:** для існуючих юзерів без збереженого вибору — system locale пристрою; якщо вона не у списку підтримуваних — `uk` (як і на вебі).
- **Без змін на сервері / Firestore** — чисто клієнтська зміна. Сервер вже повертає error-коди (`missing_required_setting`, `cannot_remove_self`, …); мобільний транслює їх у локалізовані повідомлення з власного словника.

## Acceptance

- [ ] Усі екрани (`Login`, `AccessDenied`, `FolderTree`, `LessonDetail`, `RunSetup`, `RunPlay`, `RunResult`) показують тексти або українською, або англійською — без mix-а.
- [ ] У TopBar `FolderTree` доступне перемикання мови; обраний варіант підсвічений.
- [ ] Перемикання застосовує нову мову до всього UI без перезапуску застосунку (configuration change через `AppCompatDelegate.setApplicationLocales` або еквівалент).
- [ ] Вибір зберігається між cold start-ами.
- [ ] Перший запуск з російською системною мовою → fallback на `uk`.
- [ ] Перший запуск з англійською системною мовою → UI англійською.
- [ ] Перший запуск з українською системною мовою → UI українською.
- [ ] Lable-и enum-ів (рівень, варіант, регістр, дистрактор, skill, presentation, stem-style, feedback mode) перекладаються разом з UI у RunSetup/LessonDetail/RunPlay/RunResult.
- [ ] Помилки API (`HTTP {status} · {code}`) перекладаються через словник: користувач бачить «Сесію вже завершено», а не `run_already_finished`.
- [ ] Множинні форми працюють правильно для української («1 урок», «2 уроки», «5 уроків»).
- [ ] OpenAI-пояснення у завданнях лишаються мовою, обраною у `explanation_language` per session — UI locale на це не впливає.

## References

- Цільова специфікація: [specs/i18n/spec.md](specs/i18n/spec.md)
- Імплементаційний чек-лист: [tasks.md](tasks.md)
- Джерело перекладів: [verba-web/src/i18n/messages/](../../../../verba-web/src/i18n/messages/) — `uk.json` і `en.json`
- Веб-аналог зміни (для узгодженості ключів і логіки): [verba-web/spec/changes/add-i18n/](../../../../verba-web/spec/changes/add-i18n/)
- Контекст: [project.md](../../project.md)
