# Implementation Tasks

Чек-лист реалізації двомовного UI. Проходити послідовно, тестувати кожну групу окремо.

## 1. Resource files

- [ ] Скопіювати з `verba-web/src/i18n/messages/uk.json` тексти у `app/src/main/res/values/strings.xml` (default = українська).
- [ ] Створити `app/src/main/res/values-en/strings.xml` зі значеннями з `en.json`.
- [ ] Ключі іменувати плоско: `catalog.title` → `catalog_title`, `labels.level.A1` → `label_level_a1` тощо. Точне мапування — у [specs/i18n/spec.md](specs/i18n/spec.md).
- [ ] Для `catalog.lessonCount` створити `<plurals name="lesson_count">` у `values/` (з quantity `zero|one|few|many|other`) і у `values-en/` (`one|other`).
- [ ] У всіх `<string>` обовʼязкові подвійні лапки навколо тексту з апострофами (Android XML escape: `Не вдалося'` або `\'`).
- [ ] Видалити захардкоджені українські рядки з Compose-екранів — використовувати лише `stringResource(R.string.…)` і `pluralStringResource(R.plurals.…)`.

## 2. Locale storage

- [ ] Додати `androidx.datastore:datastore-preferences` у `libs.versions.toml` і `app/build.gradle.kts`.
- [ ] Створити `data/LocalePreferences.kt`:
  - `Flow<String?>` поточної збереженої locale (null = не задано → use system).
  - `suspend fun setLocale(code: String?)`.
  - Key: `"verba.ui_locale"`.
- [ ] Зареєструвати як singleton у `VerbaApp`.

## 3. Locale application

- [ ] У `MainActivity.onCreate` (до `setContent`):
  - Зчитати збережену locale з `LocalePreferences`.
  - Якщо є — викликати `AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(saved))`.
  - Якщо немає — викликати з системною locale (Android сам зробить fallback на `values/`, якщо системна не у списку).
- [ ] Створити helper `LocaleController` (або extension на `Context`):
  - `fun changeLocale(code: String)` — пише у DataStore + викликає `setApplicationLocales`.
  - Android сам тригерить activity recreate з новою конфігурацією.

## 4. Language switcher UI

- [ ] У `FolderTreeScreen` додати `IconButton` у TopBar actions поряд із `Logout`: іконка `Icons.Filled.Translate` (з `material-icons-extended`).
- [ ] При натисканні — `DropdownMenu` з двома пунктами: «Українська» і «English» (тексти беруться через `localeName_uk` / `localeName_en`, що показуються однаково в обох locale).
- [ ] Активна locale підсвічена (Checkmark або інший фарбований state).
- [ ] При виборі — `LocaleController.changeLocale(code)`; activity recreate сам перерендерить UI новою мовою.

## 5. Enum label translation

- [ ] Створити `ui/labels/EnumLabels.kt` з top-level Composable-функціями:
  - `@Composable fun label(level: Level): String` → `stringResource(R.string.label_level_<code>)`.
  - Аналогічно для `EnglishVariant`, `Register`, `ExplanationLanguage`, `DistractorType`, `SkillTarget`, `PresentationFormat`, `QuestionStemStyle`, `FeedbackMode`, `LessonTypeId`.
- [ ] Замінити прямий `.name.lowercase()` на ці helper-функції у:
  - `LessonDetailScreen.EffectiveSettingsCard` і `TypeSettingsCard`.
  - `RunSetupScreen` chip labels (Level, EnglishVariant, Register, ExplanationLanguage, DistractorType).

## 6. API error translation

- [ ] Створити `ui/errors/ApiErrorMessage.kt`:
  - `@Composable fun apiErrorMessage(code: String?, fields: List<String> = emptyList()): String`.
  - Мапить `code` → `R.string.error_<code>` (наприклад `error_missing_required_setting` з `%1$s` placeholder для `{fields}`).
  - Fallback на `R.string.error_unknown_error` для невідомих кодів.
- [ ] Замінити в усіх ViewModel-ах формат `"HTTP ${status} · ${code}"` у `errorMessage` на:
  - На рівні ViewModel — лишити `code` без перетворення (просто зберігати у state).
  - На рівні Screen — викликати `apiErrorMessage(code)` для рендеру.

## 7. Plurals

- [ ] У `FolderTreeScreen.summary(folder)` замінити «X підпапок · Y уроків» на `pluralStringResource(R.plurals.lesson_count, folder.lessonsCount, folder.lessonsCount)`.
- [ ] Перевірити сценарії для `uk`: 0, 1, 2, 5 — мають бути «немає уроків», «1 урок», «2 уроки», «5 уроків».
- [ ] Перевірити для `en`: 0 → «no lessons», 1 → «1 lesson», 2+ → «N lessons».

## 8. Manifest configuration

- [ ] У `AndroidManifest.xml` для активності додати:
  ```xml
  <activity ... android:configChanges="locale|layoutDirection">
  ```
  щоб activity не пересоздавалась без потреби при системній зміні мови — Compose сам перечитає ресурси при `setApplicationLocales`.
- [ ] Створити `app/src/main/res/xml/locales_config.xml` з переліком підтримуваних locale (`uk`, `en`).
- [ ] Додати у `<application>` атрибут `android:localeConfig="@xml/locales_config"` — це дає Android 13+ системний UI per-app language у Settings.

## 9. Coverage check

- [ ] Запустити `grep -P "[А-яҐґЇїІіЄє]" app/src/main/java/com/verba/mobile/ui/ -r` — переконатись, що жодного кириличного хардкоду у Compose-коді не лишилось (лише у `strings.xml`).
- [ ] Виключення допустимі для коментарів і `Log.d` повідомлень — їх локалізувати не треба.

## 10. Verification

Пройти кожен пункт [proposal.md → Acceptance](proposal.md#acceptance) на реальному пристрої:

- [ ] Cold start з системою uk → UI uk.
- [ ] Cold start з системою en → UI en.
- [ ] Перемикання uk → en всередині застосунку → UI оновлюється без перезапуску.
- [ ] Кілька cold start-ів після перемикання — збережений вибір переживає рестарт.
- [ ] Запустити урок з помилковою конфігурацією, побачити локалізовану помилку.
- [ ] У FolderTree з 0/1/2/5 уроками у каталозі побачити коректні plural-форми.

## 11. Cleanup

- [ ] Після ✅ acceptance — перенести `spec/changes/add-i18n/` у `spec/changes/archive/`.
- [ ] Capability `spec/specs/i18n/spec.md` лишається живою специфікацією.
