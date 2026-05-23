# Implementation Tasks

Чек-лист реалізації мобільного MVP. Слідуйте групами по черзі — кожна група спирається на попередню. Перш ніж починати, перевірте що веб-частина задеплоєна і `firestore.rules` опубліковано, інакше доступ і дані не працюватимуть.

## 1. Bootstrap

- [ ] Створити новий Android-проєкт в Android Studio: **Empty Activity (Compose)**, мова Kotlin, мінімальний SDK звірити з актуальними вимогами Credential Manager + Firebase (див. [specs/android-config](../../specs/android-config/spec.md)).
- [ ] Переконатися, що Gradle використовує Kotlin DSL (`build.gradle.kts`).
- [ ] Додати `.gitignore` для Android (Android Studio templates / GitHub's Android `.gitignore`). Перевірити, що `google-services.json` НЕ в `.gitignore` (це звичайна практика — він не секрет, але дехто все одно тримає його поза репо; команда має вирішити узгоджено з веб-репо).
- [ ] Базова структура пакетів: `com.verba.mobile.{ui, data, voice, auth}` або еквівалентна.

## 2. Firebase project setup

> Майже все робиться з боку Firebase Console — той самий проєкт, що використовує веб.

- [ ] Відкрити Firebase Console → той самий проєкт, що `verba-web`.
- [ ] Додати Android-додаток: package name (наприклад, `com.verba.mobile`), отримати `google-services.json`.
- [ ] Покласти `google-services.json` у `app/`.
- [ ] Згенерувати debug SHA-1 і SHA-256 (`./gradlew signingReport`) і додати у Firebase Console → Project Settings → Android app → Add fingerprint.
- [ ] Якщо є release keystore — згенерувати і додати release SHA-1 і SHA-256.
- [ ] Перевірити що Authentication → Google sign-in увімкнено (це робилось для веба, але звірити).

## 3. Dependencies and Gradle

Деталі вимог — у [specs/android-config](../../specs/android-config/spec.md).

- [ ] У root `build.gradle.kts`: classpath `com.google.gms:google-services` (актуальна версія).
- [ ] У `app/build.gradle.kts`: plugin `com.google.gms.google-services`.
- [ ] Додати залежності (актуальні стабільні версії на момент налаштування):
  - Firebase BoM + `firebase-auth-ktx`, `firebase-firestore-ktx`.
  - `androidx.credentials:credentials`, `androidx.credentials:credentials-play-services-auth`, `com.google.android.libraries.identity.googleid:googleid`.
  - Compose BoM + `activity-compose`, `material3`, `navigation-compose`.
  - Lifecycle ViewModel Compose, coroutines (`kotlinx-coroutines-android`, `kotlinx-coroutines-play-services`).
- [ ] Перевірити `targetSdk` ≥ актуальної вимоги Google Play; `compileSdk` ≥ `targetSdk`.
- [ ] Синк Gradle і збірка debug — переконатись, що нічого не падає на старті.

## 4. Manifest and permissions

- [ ] `AndroidManifest.xml`:
  - `<uses-permission android:name="android.permission.INTERNET" />`
  - `<uses-permission android:name="android.permission.RECORD_AUDIO" />`
- [ ] Жодних додаткових dangerous-дозволів у MVP.
- [ ] Application label, icon, theme — стандартні Compose-дефолти на старті.

## 5. Authentication (див. [specs/authentication](../../specs/authentication/spec.md))

- [ ] `auth/AuthRepository.kt` — обгортка над `FirebaseAuth.getInstance()` + `CredentialManager`:
  - `signInWithGoogle(activityContext): Result<FirebaseUser>`.
  - `signOut(context)`: `auth.signOut()` + `CredentialManager.clearCredentialState`.
  - `currentUserFlow: Flow<FirebaseUser?>` (через `AuthStateListener`).
- [ ] `ui/LoginScreen.kt` Compose: кнопка «Увійти через Google», стани loading/error/cancelled.
- [ ] Інтегрувати `GetGoogleIdOption` з правильним Web client ID з `google-services.json` (`default_web_client_id` у `R.string`).
- [ ] `MainActivity` — single activity з `NavHost`; стартовий маршрут визначається залежно від `currentUserFlow`.
- [ ] Загальна Compose-обгортка-гард: поки `currentUser == null` ∧ ще не визначено — спінер; визначилось null → `Login`; визначилось user → перевірка доступу (наступний крок).

## 6. Access control (див. [specs/access-control](../../specs/access-control/spec.md))

- [ ] `data/AllowedUsersRepository.kt`: `isAllowed(email: String): Result<Boolean>` через Firestore `firestore.collection("allowedUsers").document(email.lowercase()).get()`.
- [ ] `ui/AccessDeniedScreen.kt`: заголовок, email користувача, кнопка «Вийти».
- [ ] Логіка маршрутизації після успішного логіну: `isAllowed()` → `LessonList` або `AccessDenied`. На `PERMISSION_DENIED` або відсутність документа — `AccessDenied`. На іншу помилку — retryable error UI.
- [ ] Перевірити вручну: створити запис у `allowedUsers/{your-email}` через веб-адмінку → мобільний пропускає; видалити → мобільний показує AccessDenied.

## 7. Lessons (див. [specs/lessons](../../specs/lessons/spec.md))

- [ ] `data/LessonsRepository.kt`:
  - `data class Lesson(val id: String, val ownerUid: String, val body: String, val createdAt: String? = null, val createdBy: String? = null)`.
  - `getMyLessons(uid): Result<List<Lesson>>` — query `whereEqualTo("ownerUid", uid).orderBy("createdAt", DESCENDING)`.
  - `getLessonById(id): Result<Lesson?>`.
- [ ] `ui/LessonListScreen.kt`: ViewModel + LazyColumn зі станами loading / empty / error / list. Тап → навігація `LessonDetail`.
- [ ] `ui/LessonDetailScreen.kt`: повний `body` з абзацами, кнопка «Назад», два слоти під голосові кнопки (наступна capability).
- [ ] R8/ProGuard keep-rules для `Lesson` (якщо використовується автодесеріалізація Firestore через `toObject<Lesson>()`).
- [ ] Перевірити вручну: створити кілька уроків через веб для свого `uid`, відкрити мобільний — список і деталі мають співпадати.

## 8. Voice — TTS (див. [specs/voice](../../specs/voice/spec.md))

- [ ] `voice/TtsEngine.kt`: ізольований клас, що тримає `TextToSpeech` instance:
  - `init()` з `Locale.ENGLISH`, callback готовності.
  - `speak(text)`, `stop()`, `shutdown()`.
  - Стан як `StateFlow<TtsState>` (Idle / Speaking / Unavailable / Initializing).
- [ ] У `LessonDetailScreen`: кнопка «Озвучити» ↔ «Зупинити» залежно від стану.
- [ ] `DisposableEffect` / `ViewModel.onCleared`: `stop() + shutdown()` при виході з екрана.
- [ ] Перевірити сценарії: коротке і довге читання, зупинка, відсутній English voice pack.

## 9. Voice — STT (див. [specs/voice](../../specs/voice/spec.md))

- [ ] `voice/SpeechListener.kt`: ізольований клас навколо `SpeechRecognizer`:
  - `isAvailable(context): Boolean`.
  - `recognize(): Flow<RecognitionEvent>` де `RecognitionEvent = Started | PartialResult(text) | Result(text) | Error(code) | Stopped`.
  - `destroy()`.
- [ ] Рантайм-запит дозволу `RECORD_AUDIO`:
  - Перед першим викликом — `rememberLauncherForActivityResult(RequestPermission)`.
  - Гілки: granted → запустити recognition; denied → snackbar + кнопка у налаштування; permanently denied → одразу кнопка у налаштування.
- [ ] У `LessonDetailScreen`: кнопка «Сказати» ↔ «Слухаю...», TextView з результатом нижче.
- [ ] Перевірити сценарії: успішне розпізнавання, тиша (`ERROR_SPEECH_TIMEOUT` / `ERROR_NO_MATCH`), без інтернету (`ERROR_NETWORK`), STT недоступний (стара версія/AOSP без сервісу).

## 10. End-to-end smoke test

- [ ] Запустити свіжий debug-білд на реальному пристрої (емулятор не має нормального мікрофона для STT).
- [ ] Пройти всі сценарії з [proposal.md → Acceptance](proposal.md#acceptance).
- [ ] Зробити release-білд з R8 minification — переконатись, що десеріалізація уроків не зламалась.
- [ ] Розпакувати release APK і перевірити пошуком, що жодних API-ключів у файлах не зашито.

## 11. Cleanup and archive

Перевірити кожен пункт [proposal.md → Acceptance](proposal.md#acceptance). Після ✅ — перенести `spec/changes/add-initial-prototype/` у `spec/changes/archive/`. Capabilities у `spec/specs/` залишаються як живі специфікації — їх змінюємо через нові `changes/`.
