# Verba Mobile

Нативний Android-додаток Verba: перегляд уроків англійської, які адмін створює через веб (`d:\Projects\verba-web`). Базовий прототип. Read-only клієнт зі спільним Firebase-бекендом.

Деталі специфікації — у [spec/](spec/) (openspec.dev формат).

## Стек

- Kotlin + Jetpack Compose, Material 3
- Firebase Auth (Google) + Cloud Firestore
- Credential Manager + Google ID для входу
- Android `TextToSpeech` і `SpeechRecognizer` для голосового демо

## Передумови

- **Android Studio** Ladybug або новіше (тягне власний JDK 17).
- **Android SDK** API 35 (compile/target), API 28+ (min).
- Налаштований і працюючий веб-проєкт у `d:\Projects\verba-web`:
  - Спільний Firebase-проєкт з увімкненим **Google** sign-in.
  - `firestore.rules` опубліковано.
  - Хоча б один документ у `allowedUsers/{your-email}` (інакше побачите тільки `AccessDenied`).
  - Хоча б один урок у `lessons` з `ownerUid` вашого Firebase UID (інакше `LessonList` буде порожнім).

## Перший запуск

1. **Клонувати і відкрити в Android Studio:**
   - File → Open → вибрати папку `verba-mobile`.
   - Android Studio запропонує імпорт проєкту і завантаження потрібних SDK-компонентів.

2. **Згенерувати Gradle wrapper jar:**
   `gradle-wrapper.jar` не комітиться сюди (бінарний). Виконати одноразово:
   ```
   gradle wrapper --gradle-version 8.10.2
   ```
   (потрібен Gradle 8+ у PATH або скористайтесь панеллю Gradle Android Studio: Tasks → wrapper).
   Альтернатива: Android Studio сам запропонує згенерувати wrapper при першому відкритті.

3. **Додати Android-додаток до Firebase:**
   - Firebase Console → той самий проєкт, що `verba-web` → Add app → Android.
   - Package name: `com.verba.mobile`.
   - Завантажити `google-services.json`, покласти у `app/google-services.json`.

4. **Зареєструвати SHA-фінгерпринти:**
   ```
   ./gradlew signingReport
   ```
   У Firebase Console → Project Settings → Android app → **Add fingerprint**:
   - Debug SHA-1 і SHA-256 (з `signingReport` під `Variant: debug`).
   - Release SHA-1 і SHA-256 (якщо є release keystore).
   Без цього Google sign-in поверне `DEVELOPER_ERROR`.

5. **Перевірити, що Google sign-in увімкнено у Firebase:**
   Authentication → Sign-in method → Google → Enabled. Це вже зроблено для веба, але звірити. Без цього у `google-services.json` не буде Web OAuth client, і ресурс `default_web_client_id` не згенерується — застосунок не скомпілюється.

6. **Збірка і запуск:**
   - У Android Studio: Run → app → обрати реальний пристрій (емулятор не має нормального мікрофона для STT).
   - Або через CLI: `./gradlew :app:installDebug`.

## Перевірка прийнятності

Див. [spec/changes/add-initial-prototype/proposal.md → Acceptance](spec/changes/add-initial-prototype/proposal.md#acceptance):

- Вхід через Google → `LessonList` (якщо email у `allowedUsers`) або `AccessDenied`.
- `LessonList` показує ваші уроки з вебу, найновіший зверху; тап → `LessonDetail`.
- `LessonDetail`: «Озвучити» зачитує текст англійською; «Сказати» розпізнає короткий голос.
- Кросплатформена ідентичність: той самий Google-акаунт = той самий `uid` на вебі і мобільному.

## Поширені проблеми

| Симптом | Імовірна причина |
|---------|------------------|
| `Resource ... default_web_client_id not found` при компіляції | Немає Web OAuth client у `google-services.json`. Увімкнути Google sign-in у Firebase Authentication і перезавантажити `google-services.json`. |
| Google вхід падає з `DEVELOPER_ERROR` / `code: 10` | SHA-фінгерпринт не зареєстрований у Firebase Console. |
| Усі залогінені бачать `AccessDenied` | Документа `allowedUsers/{email}` немає або `firestore.rules` блокує. Перевірити правила і колекцію у Firestore. |
| `LessonList` порожній | У користувача немає уроків з його `ownerUid`. Створити через `/admin/lessons` веба. |
| STT падає одразу з помилкою | На пристрої немає `SpeechRecognizer` (старий AOSP/емулятор) або відмовлено в `RECORD_AUDIO`. |

## Структура

```
app/src/main/java/com/verba/mobile/
├── MainActivity.kt                 — single-activity host, NavHost
├── VerbaApp.kt                     — Application з singleton-репозиторіями
├── auth/AuthRepository.kt          — Credential Manager + Firebase Auth
├── data/
│   ├── Lesson.kt
│   ├── AllowedUsersRepository.kt
│   └── LessonsRepository.kt
├── voice/
│   ├── TtsEngine.kt                — обгортка TextToSpeech
│   └── SpeechListener.kt           — обгортка SpeechRecognizer
└── ui/
    ├── app/AppViewModel.kt         — топ-стейт (auth + access check)
    ├── login/                      — LoginScreen + ViewModel
    ├── accessdenied/               — AccessDeniedScreen
    ├── lessons/                    — LessonList + LessonDetail + ViewModels
    ├── navigation/Routes.kt
    └── theme/                      — Compose Material3 theme
```
