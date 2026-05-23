# voice Specification

## Purpose

Закласти фундамент під майбутні голосові фічі (діалог з ШІ англійською). У MVP — демо-обгортка двох системних можливостей Android: озвучення тексту уроку (TTS) і розпізнавання короткої фрази користувача (STT). Голосовий «мозок» (LLM) — поза обсягом цього MVP; коли зʼявиться, його виклик MUST йти через серверний ендпоінт веба, а не з мобільного напряму.

## Requirements

### Requirement: TTS — read lesson aloud

The screen `LessonDetail` SHALL надавати кнопку «Озвучити», яка зачитує поточний текст уроку через Android `TextToSpeech`.

Поведінка:
1. На першому натисканні (або при відкритті екрана) ініціалізувати `TextToSpeech` з `Locale.ENGLISH` (US — допустимий дефолт).
2. У `OnInitListener` перевірити результат: якщо `SUCCESS` і мова доступна (`isLanguageAvailable(Locale.ENGLISH) >= LANG_AVAILABLE`) — кнопка активна; інакше — disabled з підписом «English TTS недоступний».
3. По натисканню — `speak(body, QUEUE_FLUSH, params, utteranceId)`.
4. Поки TTS активний, кнопка перетворюється на «Зупинити»; натискання — `stop()`.
5. При виході з екрана (`onDispose` / `onDestroy`) — `stop()` + `shutdown()`.

#### Scenario: Speak short lesson

- **GIVEN** користувач на `LessonDetail` з коротким англійським текстом
- **WHEN** натискає «Озвучити»
- **THEN** чути синтезоване читання тексту, кнопка показує стан «Зупинити»

#### Scenario: Stop midway

- **GIVEN** TTS активно читає урок
- **WHEN** користувач натискає «Зупинити»
- **THEN** мовлення припиняється негайно, кнопка повертається до стану «Озвучити»

#### Scenario: English voice unavailable

- **GIVEN** на пристрої немає встановленого English TTS pack
- **WHEN** ініціалізація `TextToSpeech` завершується з `LANG_MISSING_DATA` / `LANG_NOT_SUPPORTED`
- **THEN** кнопка «Озвучити» disabled з підписом «English TTS недоступний», тап показує snackbar з порадою встановити голосові дані у системних налаштуваннях

#### Scenario: Leave screen while speaking

- **GIVEN** TTS активно читає
- **WHEN** користувач натискає «Назад» і йде на `LessonList`
- **THEN** TTS зупиняється; ресурси звільнено (`shutdown`)

### Requirement: STT — recognize short phrase

The screen `LessonDetail` SHALL надавати кнопку «Сказати», яка запускає одноразове розпізнавання мовлення через Android `SpeechRecognizer` англійською мовою.

Поведінка:
1. Перед першим використанням — рантайм-запит дозволу `android.permission.RECORD_AUDIO`.
2. Якщо дозвіл надано — створити `SpeechRecognizer.createSpeechRecognizer(context)` з `Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)`:
   - `EXTRA_LANGUAGE_MODEL = LANGUAGE_MODEL_FREE_FORM`.
   - `EXTRA_LANGUAGE = "en-US"`.
   - `EXTRA_PREFER_OFFLINE = true` (де доступно — реальна офлайн-здатність залежить від OEM і пристрою; це лише підказка).
3. Поки слухає — кнопка показує стан «Слухаю...», UI блокує повторні натискання.
4. По завершенні (`onResults`) — показати розпізнаний текст у TextView/Text composable нижче кнопки.
5. При помилках — показати лаконічне повідомлення (див. сценарії нижче).
6. При виході з екрана — `destroy()` recognizer-а.

#### Scenario: Successful recognition

- **GIVEN** дозвіл `RECORD_AUDIO` надано, мікрофон працює
- **WHEN** користувач натискає «Сказати» і вимовляє «hello world»
- **THEN** через секунди в інтерфейсі зʼявляється текст «hello world»

#### Scenario: Permission denied (first time)

- **GIVEN** перший запуск STT, користувач відмовив у дозволі `RECORD_AUDIO`
- **WHEN** натискає «Сказати»
- **THEN** показується snackbar «Потрібен доступ до мікрофона», з кнопкою «Налаштування» що відкриває app settings

#### Scenario: Permission denied permanently

- **GIVEN** користувач натиснув «Не питати знову» при попередній відмові
- **WHEN** натискає «Сказати»
- **THEN** показується пояснення з кнопкою, що веде у системні налаштування застосунку (бо звичайний `requestPermissions` більше не покаже діалог)

#### Scenario: STT not available on device

- **GIVEN** на пристрої немає `SpeechRecognizer` (`SpeechRecognizer.isRecognitionAvailable(context) == false`)
- **WHEN** екран `LessonDetail` рендериться
- **THEN** кнопка «Сказати» disabled з підписом «Розпізнавання голосу недоступне»

#### Scenario: No speech detected

- **GIVEN** користувач натиснув «Сказати», але мовчав
- **WHEN** `onError(ERROR_SPEECH_TIMEOUT)` / `ERROR_NO_MATCH`
- **THEN** показується «Нічого не розпізнано, спробуйте ще»; кнопка знов активна

#### Scenario: Network unavailable for online STT

- **GIVEN** пристрій без офлайн-моделі English STT і без інтернету
- **WHEN** запит на розпізнавання фейлиться (`ERROR_NETWORK`)
- **THEN** показується «Потрібен інтернет для розпізнавання», кнопка знов активна

### Requirement: No secrets in voice path

The voice subsystem MUST NOT містити жодного API-ключа стороннього сервісу (OpenAI, Azure Speech тощо). У MVP голос — це on-device TTS + on-device/системний STT. Будь-яке майбутнє розширення до хмарного голосу/LLM MUST йти через серверний ендпоінт веба з Firebase ID-token-ом, а не з ключем у застосунку.

### Requirement: Voice subsystem isolation

The TTS і STT логіка SHALL бути ізольована у власних класах/об'єктах (наприклад, `VoiceController` / `TtsEngine` / `SpeechListener`) і не змішуватися з UI-композаблами та доменом уроків. Це готує заміну реалізації (наприклад, заміна системного TTS на хмарний голос) без зміни решти коду.

### Requirement: Future voice dialog architecture (informative)

> Не є нормативним вимаганням MVP, але задає напрямок:
>
> Голосовий діалог = on-device STT → серверний ендпоінт веба (проксі до OpenAI) → on-device TTS. «Мозок» діалогу завжди у хмарі; ключ OpenAI завжди на сервері веба. Мобільний відправляє розпізнаний текст + Firebase ID-token у `Authorization` заголовку, отримує відповідь, озвучує. Жоден реальний LLM-провайдер на пристрій не виноситься.
