# android-config Specification

## Purpose

Описати Android-специфічні конфігураційні вимоги: маніфест, дозволи, залежності, підпис застосунку, інтеграцію з Firebase. Без коректного `google-services.json` і зареєстрованих SHA-фінгерпринтів Google sign-in не працює — це разове налаштування, від якого залежить уся [[authentication]].

## Requirements

### Requirement: Manifest permissions

The `AndroidManifest.xml` SHALL містити рівно ті дозволи, що потрібні MVP:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
```

- `INTERNET` — для Firebase Auth і Firestore.
- `RECORD_AUDIO` — для STT (див. [[voice]]); запитується у рантаймі перед першим використанням.

Жодних інших dangerous-дозволів (camera, contacts, location, storage) у MVP NOT SHALL бути.

### Requirement: Firebase project linkage

The module `app` SHALL містити файл `google-services.json`, отриманий з Firebase Console для **того самого** проєкту, що використовує веб (`d:\Projects\verba-web`). `projectId` у `google-services.json` MUST збігатися з `FIREBASE_PROJECT_ID` веба.

#### Scenario: Shared project verification

- **GIVEN** свіжо склонований репозиторій і чисте робоче дерево
- **WHEN** розробник перевіряє `projectId` у `app/google-services.json` і `FIREBASE_PROJECT_ID` у веб-проєкті
- **THEN** вони ідентичні

### Requirement: SHA fingerprints registration

The Firebase Console MUST містити **SHA-1 і SHA-256** фінгерпринти підписів застосунку:
- Debug keystore (для розробки локально на машинах команди).
- Release keystore (для playstore/sideload-збірок).

Без цих фінгерпринтів Google sign-in повертає `DEVELOPER_ERROR` / `10:` і вхід не працює.

#### Scenario: Missing debug SHA

- **GIVEN** новий розробник вперше складає debug-білд, а його debug SHA не зареєстрований
- **WHEN** запускає Google sign-in
- **THEN** Credential Manager / Firebase повертає помилку, повʼязану з невідомим підписом; розробник додає свій debug SHA у Firebase Console → перезапуск → працює

### Requirement: Dependencies

The `app/build.gradle.kts` (або еквівалент Groovy) SHALL включати:

- **Firebase BoM** (актуальна стабільна версія на момент налаштування) + модулі:
  - `com.google.firebase:firebase-auth-ktx`
  - `com.google.firebase:firebase-firestore-ktx`
- **Credential Manager + Google ID**:
  - `androidx.credentials:credentials`
  - `androidx.credentials:credentials-play-services-auth`
  - `com.google.android.libraries.identity.googleid:googleid`
- **Jetpack Compose BoM** + `androidx.activity:activity-compose`, `androidx.compose.material3:material3`, `androidx.navigation:navigation-compose`.
- **Lifecycle / coroutines**: `androidx.lifecycle:lifecycle-viewmodel-compose`, `androidx.lifecycle:lifecycle-runtime-ktx`, `kotlinx-coroutines-android`, `kotlinx-coroutines-play-services` (для `await()` на `Task`).

> Жодних версій тут не закріплюємо — на момент початкової імплементації звірити з актуальною документацією та обрати останні стабільні.

The root `build.gradle` SHALL застосовувати `com.google.gms.google-services` plugin, а `app` модуль — `id("com.google.gms.google-services")`.

### Requirement: Minimum SDK and target

- `minSdk` SHALL бути не нижче того, що вимагає Credential Manager + Firebase Auth з Google ID provider — звірити з актуальною документацією AndroidX Credentials. Орієнтовно — Android 9 (API 28) або вище, якщо документація прямо це вимагає.
- `targetSdk` SHALL відповідати поточним вимогам Google Play на момент релізу (не нижче того, що Google Play вимагає для нових застосунків).
- `compileSdk` SHALL бути не нижче `targetSdk`.

> Конкретні числа фіксуються у `build.gradle` під час bootstrap-у; спека вимагає актуальності, а не конкретного API level.

### Requirement: No secrets in APK

The application code, ресурси і buildConfig MUST NOT містити секретні ключі (OpenAI, Firebase Admin private key, service account credentials). `google-services.json` сам по собі не є секретом — він містить публічні ідентифікатори; безпеку забезпечує Firebase Auth + Firestore Rules, а не приховування файлу.

#### Scenario: Secret leak check

- **GIVEN** release-APK
- **WHEN** розгортається і шукається сирий рядок `sk-` (OpenAI keys), `-----BEGIN PRIVATE KEY-----`, або значення Firebase Admin `private_key`
- **THEN** збігів немає

### Requirement: ProGuard / R8 keep rules

If release-білд використовує R8 minification, the rules SHALL зберігати класи моделей, які Firestore десеріалізує через рефлексію (анотовані `@DocumentId` / `@PropertyName`, або плейн data class-и з no-arg конструктором). Без правильних `-keep` Firestore deserialization фейлиться у release і працює у debug — типовий клас помилок.

### Requirement: Network and ProGuard for Firebase

If R8 minification увімкнено, the build SHALL використовувати дефолтні Firebase consumer ProGuard rules (вони підтягуються автоматично з AAR-ів). Жодних додаткових rules для самого Firebase зазвичай не потрібно — лише для власних data-класів моделей (див. вище).

### Requirement: Single activity, Compose navigation

The app SHALL бути single-activity з навігацією через `androidx.navigation:navigation-compose`. Екрани: `Login`, `AccessDenied`, `LessonList`, `LessonDetail`. Жодних XML-фрагментів або activity-per-screen — щоб лишити простір для майбутньої голосової сесії, що переживає навігацію.

### Requirement: Configuration change handling

The voice subsystem ([[voice]]) і Firestore-стани SHALL виживати configuration change (поворот екрана) через `ViewModel` (або еквівалент). Кнопка «Озвучити», що читає урок, NOT SHALL переривати TTS при повороті, якщо її ViewModel зберігає стан. У MVP допустимо не блокувати поворот, але стани не повинні «миготіти».
