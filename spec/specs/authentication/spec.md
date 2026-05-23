# authentication Specification

## Purpose

Дозволити користувачам входити у мобільний застосунок через Google-акаунт, отримувати Firebase-сесію зі стабільним `uid`, спільним з вебом, і використовувати цей `uid` для читання своїх даних з Firestore. Сесія MUST зберігатися між запусками застосунку.

## Requirements

### Requirement: Google sign-in via Credential Manager

The system SHALL надавати екран `Login` з кнопкою «Увійти через Google», що ініціює Google sign-in через `androidx.credentials.CredentialManager` з `GetGoogleIdOption`, після чого передає отриманий `idToken` у Firebase Auth (`GoogleAuthProvider.getCredential(idToken, null)` → `FirebaseAuth.signInWithCredential`).

> Старі підходи (`GoogleSignInClient` з Play Services Auth) NOT SHALL використовуватись у новому коді — вони deprecated на користь Credential Manager.

#### Scenario: Successful sign-in

- **GIVEN** користувач відкриває додаток без активної сесії
- **WHEN** натискає «Увійти через Google» і обирає Google-акаунт у системному UI
- **THEN** Firebase повертає `FirebaseUser` з ненульовим `uid`, додаток переходить до перевірки доступу ([[access-control]])

#### Scenario: User cancels picker

- **GIVEN** користувач відкрив системний account picker
- **WHEN** закриває його без вибору акаунта
- **THEN** залишається на `Login`, показується inline-повідомлення «Вхід скасовано»

#### Scenario: No Google account on device

- **GIVEN** на пристрої немає жодного Google-акаунта
- **WHEN** користувач натискає «Увійти через Google»
- **THEN** показується повідомлення «Додайте Google-акаунт у налаштуваннях пристрою», `Login` лишається відкритим

#### Scenario: Network error during sign-in

- **GIVEN** Firebase Auth недоступний (немає інтернету або 5xx)
- **WHEN** клієнт ловить помилку sign-in
- **THEN** показується «Не вдалося увійти. Спробуйте пізніше» з кнопкою повтору

### Requirement: Shared identity with web

One Google account MUST давати один Firebase `uid` на вебі й мобільному. Це досягається використанням **спільного** Firebase-проєкту (того самого `projectId` у `google-services.json`, що й веб-конфіг).

#### Scenario: Cross-platform uid consistency

- **GIVEN** користувач уже логінився на вебі з Google-акаунтом X і має у вебі `uid = "ABC"`
- **WHEN** входить тим самим акаунтом X у мобільному додатку
- **THEN** `FirebaseAuth.getInstance().currentUser.uid == "ABC"`

### Requirement: Session persistence

The system SHALL зберігати Firebase сесію між запусками застосунку (Firebase Android SDK робить це за замовчуванням через локальне сховище). Користувач NOT SHALL вводити Google-credentials повторно після холодного старту, якщо токен не прострочений.

#### Scenario: Cold start with persisted session

- **GIVEN** користувач уже логінився раніше і токен ще валідний
- **WHEN** запускає додаток заново
- **THEN** додаток показує спінер до завершення ініціалізації Firebase, далі переходить одразу до перевірки доступу і `LessonList`, минаючи `Login`

### Requirement: Auth state guard

Усі екрани крім `Login` SHALL чекати на завершення першої емісії `FirebaseAuth.AuthStateListener` (або еквівалентного Flow) перед рендером основного контенту. Поки стан невідомий — показати спінер; якщо `currentUser == null` — навігація до `Login`.

#### Scenario: Direct deep link to lesson without session

- **GIVEN** користувач без сесії
- **WHEN** додаток відкривається на екрані, що очікує `LessonDetail` (deep link або відновлений стан)
- **THEN** короткий спінер, далі навігація на `Login`

### Requirement: Sign-out

The system SHALL надавати дію «Вийти», яка:
1. Виконує `FirebaseAuth.getInstance().signOut()`.
2. Очищає Credential Manager state (`CredentialManager.clearCredentialState`), щоб наступний вхід знову показав picker.
3. Навігує на `Login` і чистить back-stack.

Кнопка «Вийти» MUST бути доступна щонайменше на екрані `AccessDenied` (див. [[access-control]]) і на головному екрані `LessonList`.

### Requirement: ID token availability for API calls

If майбутні екрани викликатимуть веб-API (наприклад, `GET /api/my-lessons`), the client SHALL отримувати свіжий ID-token через `FirebaseUser.getIdToken(forceRefresh = false)` і додавати його у заголовок `Authorization: Bearer <token>`. У MVP це не використовується (дані читаються прямо з Firestore), але інтерфейс отримання токена MUST бути готовий.

> Контракт серверного ендпоінту описаний у веб-спеці: `../../verba-web/spec/specs/mobile-api/spec.md`.
