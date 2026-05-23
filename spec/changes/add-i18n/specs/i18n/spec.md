# i18n Specification

## Purpose

Дати мобільному застосунку двомовний інтерфейс (українська + англійська), синхронізований з вебом за смислом ключів і змістом перекладів, але оформлений нативно для Android (XML-ресурси, `LocaleListCompat`, `<plurals>`). Користувач має змогу перемкнути мову всередині застосунку; вибір переживає cold start; перший запуск підхоплює системну locale.

Контентна мова уроку (`explanation_language` у налаштуваннях run-у) — окремий вимір і цією specификацією **не керується**.

## Requirements

### Requirement: Supported locales

The system SHALL підтримувати рівно дві UI-локалі у MVP:

- `uk` — українська (default fallback)
- `en` — англійська

`uk` MUST бути контентом `app/src/main/res/values/strings.xml` (default бакет — те, що використає Android, якщо системна locale не у списку). `en` — `app/src/main/res/values-en/strings.xml`.

Додавання нових локалей — поза скоупом цієї зміни і вимагатиме окремого change-proposal-у.

### Requirement: Locale resolution at startup

On cold start, the app SHALL визначити поточну UI-локаль у такому порядку:

1. Збережений вибір з `DataStore` ключ `"verba.ui_locale"` (якщо це валідне значення з `["uk", "en"]`).
2. System locale пристрою (`Locale.getDefault().language`), якщо вона у списку підтримуваних.
3. Fallback на `uk`.

Резолюція MUST виконуватись у `MainActivity.onCreate` **до** `setContent`, через виклик `AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(code))`. Це уникне flash старої мови при оновленні Compose.

#### Scenario: First launch on English device

- **GIVEN** свіжо встановлений застосунок, у користувача системна мова `en-US`, DataStore порожній
- **WHEN** користувач відкриває застосунок
- **THEN** усі тексти показуються англійською

#### Scenario: First launch on Polish device

- **GIVEN** свіжо встановлений застосунок, системна мова `pl-PL`, DataStore порожній
- **WHEN** користувач відкриває застосунок
- **THEN** усі тексти показуються українською (fallback, бо `pl` не у списку)

#### Scenario: Saved choice overrides system

- **GIVEN** користувач раніше обрав `en` у застосунку, потім змінив системну мову на українську
- **WHEN** наступного cold start
- **THEN** UI лишається англійським (DataStore має пріоритет)

### Requirement: In-app language switcher

The screen `FolderTree` SHALL містити у TopBar кнопку перемикання мови (icon-button з `Icons.Filled.Translate` або еквівалент), що відкриває `DropdownMenu` з двома пунктами:

- «Українська» (label завжди українською, незалежно від поточної locale).
- «English» (label завжди англійською).

Активна locale MUST бути візуально позначена (Checkmark, виділення кольором або інший explicit-індикатор).

При виборі застосунок MUST:

1. Записати нове значення у DataStore (`"verba.ui_locale"`).
2. Викликати `AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(code))`.
3. Активність зміниться сама — Compose-композиція перечитає всі `stringResource(...)` з новою locale.

#### Scenario: Switch from UK to EN on FolderTree

- **GIVEN** користувач на `FolderTree`, TopBar показує «Уроки»
- **WHEN** натискає на іконку Translate, обирає «English»
- **THEN** заголовок одразу змінюється на «Lessons», DataStore містить `"en"`, інші екрани (відкриті після цього) теж англійською

#### Scenario: Persistence across cold start

- **GIVEN** користувач перемкнув на `en`, далі закрив застосунок
- **WHEN** запускає застосунок наступного разу
- **THEN** UI знову англійською без додаткових дій

### Requirement: All UI text via Android resources

Every user-visible string у Compose-коді MUST бути отриманий через `stringResource(R.string.…)` або `pluralStringResource(R.plurals.…)`. Захардкоджені літерали-рядки українською або англійською у Compose-функціях SHALL NOT існувати поза:

- `voice/` (TTS/STT мова — внутрішнє налаштування, не UI).
- Технічними рядками у `Log.d` / `Log.e`.
- TODO-коментарями і dev-діагностикою (з відповідним маркером).

#### Scenario: No hardcoded Cyrillic in UI code

- **WHEN** запустити пошук кириличних символів у `app/src/main/java/com/verba/mobile/ui/`
- **THEN** збігів немає

### Requirement: Translated enum labels

For each enum that appears у UI, the dictionary SHALL містити перекладений лейбл, узгоджений з вебом (`verba-web/src/i18n/messages/{uk,en}.json` → `labels.…`):

- `Level` (`A1`..`C2`) — наприклад `A1` → «A1 (Початковий)» / «A1 (Beginner)».
- `EnglishVariant` (`british` / `american`).
- `Register` (`formal` / `neutral` / `informal`).
- `ExplanationLanguage` (`english` / `ukrainian`).
- `DistractorType` (4 значення).
- `SkillTarget` (5 значень).
- `PresentationFormat` (`isolated_sentences` / `connected_text`).
- `QuestionStemStyle` (`gap_fill` / `direct_question`).
- `FeedbackMode` (`immediate` / `end`).
- `LessonTypeId` (`multiple_choice` і майбутні).

Кожен лейбл MUST бути доступний через єдину Composable-функцію `label(value: <Enum>): String`, що повертає `stringResource(R.string.label_…)` за відповідним ключем. UI-екрани (`RunSetupScreen`, `LessonDetailScreen`, …) NOT SHALL викликати `.name` чи `.name.lowercase()` для відображення.

#### Scenario: Distractor label in English mode

- **GIVEN** locale `en`
- **WHEN** UI рендерить chip для `DistractorType.SEMANTIC_CLOSE`
- **THEN** chip-текст — «Semantically close», не `semantic_close` і не «Близькі за змістом»

#### Scenario: Level label in Ukrainian mode

- **GIVEN** locale `uk`
- **WHEN** UI показує `Level.B1` у LessonDetail
- **THEN** значення — «B1 (Середній)»

### Requirement: Localized API error messages

The client SHALL мати у словнику запис `error_<code>` для кожного коду помилки, що повертається API (узгоджено з [verba-web](../../../../verba-web/src/i18n/messages/) → `errors.…`):

- Auth: `missing_token`, `invalid_token`, `no_email`, `not_allowed`, `admin_required`.
- Validation: `invalid_input`, `invalid_email`, `invalid_role`, `invalid_type_settings`, `invalid_lesson_type_settings`, `missing_required_setting`, `task_count_out_of_range`, `options_count_5_requires_c1_c2`, `prompt_required`, `prompt_too_long`.
- Resource: `folder_not_found`, `lesson_not_found`, `run_not_found`, `task_not_found`, `email_not_found`.
- State: `cycle_blocked`, `folder_not_empty`, `email_already_allowed`, `cannot_remove_self`, `run_already_finished`, `all_tasks_generated`, `already_answered`, `invalid_index_gap`, `invalid_index_param`, `invalid_selected_index`, `user_never_signed_in`.
- External: `rate_limited`, `generation_failed`, `timeout`, `upstream_failed`, `openai_not_configured`.
- Fallback: `unknown_error` для невідомих або нових кодів.

Повідомлення MAY містити placeholder `%1$s` для інтерполяції (наприклад у `missing_required_setting` — список полів).

Замість відображення raw-коду «HTTP 400 · missing_required_setting», UI MUST показувати перекладене повідомлення «Не задано обов'язкові поля: level, register» (uk) / «Missing required settings: level, register» (en).

#### Scenario: User sees translated error

- **GIVEN** locale `en`, спроба `POST /api/runs` без `level` у effective settings
- **WHEN** сервер повертає `400 { error: "missing_required_setting", details: { fields: ["level"] } }`
- **THEN** UI показує «Missing required settings: level», не raw-код

#### Scenario: Unknown error code falls back

- **GIVEN** locale `uk`, сервер раптом повернув `error: "quantum_overflow"` (новий код, ще не у словнику)
- **WHEN** клієнт викликає переклад
- **THEN** показується «Сталася помилка» (з `error_unknown_error`)

### Requirement: Pluralization

Counts (наприклад «5 уроків у каталозі») MUST використовувати Android `<plurals>` ресурс з квантифікаторами CLDR відповідної locale:

- `uk` — `zero`, `one`, `few`, `many`, `other`.
- `en` — `one`, `other`.

Виклик у Compose: `pluralStringResource(R.plurals.lesson_count, count, count)`.

#### Scenario: Ukrainian plurals on FolderTree

- **GIVEN** locale `uk`, каталог містить N уроків
- **WHEN** UI рендерить summary
- **THEN** для N=0 → «немає уроків», N=1 → «1 урок», N=2 → «2 уроки», N=5 → «5 уроків», N=21 → «21 урок»

#### Scenario: English plurals

- **GIVEN** locale `en`
- **WHEN** UI рендерить summary
- **THEN** для N=0 → «no lessons», N=1 → «1 lesson», N=2 → «2 lessons»

### Requirement: Key mapping with web dictionary

The Android resource keys MUST бути узгоджені з веб-ключами (`verba-web/src/i18n/messages/uk.json`) за наступним правилом перетворення:

- Вкладені JSON-ключі сплющуються через `_`: `catalog.title` → `R.string.catalog_title`.
- Camel-case у JSON стає snake_case в Android: `catalog.lessonCount` → `R.plurals.lesson_count`.
- Enum-лейбли мають префікс `label_<enum>_<value>`: `labels.level.A1` → `R.string.label_level_a1` (lowercase).
- Помилки: `errors.<code>` → `R.string.error_<code>`.

Тексти MUST дзеркалити веб байт-в-байт там, де це не суперечить Android-формату (іконки, escape-sequences `\'` для апострофа, `\\n` для переносу).

Тільки ті ключі, що дійсно використовуються мобільним UI, переносяться. Адмінські ключі (`admin.users.*`, `admin.folders.*`, `admin.lessonForm.*`) — поза скоупом мобільного MVP і NOT SHALL переноситись.

#### Scenario: Web-mobile parity for catalog title

- **GIVEN** веб має `catalog.title = "Каталог"` / `"Catalog"`
- **WHEN** мобільний рендерить TopBar `FolderTree`
- **THEN** заголовок — «Каталог» (uk) або «Catalog» (en), той самий текст що на вебі

### Requirement: System per-app language settings (Android 13+)

The app SHALL оголосити підтримувані локалі через `app/src/main/res/xml/locales_config.xml` і прив'язати атрибутом `android:localeConfig="@xml/locales_config"` у `<application>` маніфесту.

Це дає Android 13+ показувати застосунок у системних Settings → Languages → per-app, дозволяючи користувачу обрати мову з системного UI. Вибір через системні Settings MUST бути еквівалентний вибору з in-app перемикача — обидва шляхи призводять до того ж результату через `setApplicationLocales`.

#### Scenario: Per-app language from system settings

- **GIVEN** Android 13+, користувач відкриває Settings → Languages → Verba → English
- **WHEN** повертається в застосунок
- **THEN** UI англійською; цей вибір записаний як `applicationLocales` на рівні Android

> Узгодження з in-app перемикачем: при відкритті FolderTree, локаль для UI беремо саме з `AppCompatDelegate.getApplicationLocales()` (якщо встановлена) → це покриває обидва шляхи введення.

### Requirement: Voice subsystem unaffected

The voice capability ([[voice]]) — `TextToSpeech` і `SpeechRecognizer` — MUST лишатись на `Locale.ENGLISH`, незалежно від UI locale. Перемикання UI мови NOT SHALL впливати на голос (це фіча продукту: озвучення і розпізнавання — англійською, бо це мова уроку).

#### Scenario: Voice still English when UI is Ukrainian

- **GIVEN** locale `uk` встановлена, користувач відкриває `LessonDetail` (майбутній text-based тип, де є TTS)
- **WHEN** натискає «Озвучити»
- **THEN** TTS читає текст англійським голосом (як було)

### Requirement: Content language independence

The OpenAI `explanation_language` setting (per session, частина universal settings) MUST лишатись незалежним від UI locale. Користувач з UI `en` MAY стартувати run з `explanation_language: ukrainian` і навпаки.

#### Scenario: English UI, Ukrainian explanations

- **GIVEN** UI locale `en`, у RunSetup користувач обирає `Explanation language: Ukrainian`
- **WHEN** запускається run, OpenAI генерує task з explanation
- **THEN** UI всі лейбли і кнопки англійською, але `task.explanation` — українською

### Requirement: Visual indication of active locale

The language switcher MUST візуально показувати, яка locale зараз активна (виділена опція у DropdownMenu, або галочка біля назви, або кольорова рамка). Користувач MUST мати змогу визначити поточний стан без додаткових дій.
