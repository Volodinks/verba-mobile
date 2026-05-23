# access-control Specification

## Purpose

Обмежити мобільний доступ лише попередньо дозволеними email-адресами (тими, що є у спільній з вебом колекції `allowedUsers`), і забезпечити подвійний периметр захисту: клієнтська перевірка для UX + Firestore Security Rules як незалежний серверний рівень.

## Requirements

### Requirement: Client-side allowed check

After successful Google sign-in (див. [[authentication]]), the client SHALL виконати читання документа `allowedUsers/{email}`, де `email = FirebaseUser.email!!.lowercase()`, через Firestore Android SDK.

Результат визначає навігацію:
- Документ існує → перейти до `LessonList`.
- Документ не існує (`DocumentSnapshot.exists() == false`) → показати екран `AccessDenied`.
- `PERMISSION_DENIED` від Firestore → також `AccessDenied` (Rules відмовили — той самий змістовний результат для UX).
- Інша помилка (мережа, недоступність Firestore) → показати retryable error state.

#### Scenario: Allowed user

- **GIVEN** користувач увійшов через Google, email `bob@example.com` у `allowedUsers`
- **WHEN** клієнт читає `allowedUsers/bob@example.com`
- **THEN** документ існує, додаток переходить до `LessonList`

#### Scenario: Disallowed user

- **GIVEN** користувач увійшов через Google, email НЕ у `allowedUsers`
- **WHEN** клієнт читає `allowedUsers/{email}`
- **THEN** документ відсутній (або PERMISSION_DENIED від Rules), показується `AccessDenied`

### Requirement: AccessDenied screen

If користувач увійшов через Google, але доступ не надано, the system SHALL показати екран з:
- Заголовком «Доступ не надано».
- Поясненням «Ваш email не доданий до списку дозволених. Зверніться до адміністратора.».
- Email, з яким зайшли (для допомоги адміну).
- Кнопкою «Вийти», що виконує sign-out (див. [[authentication]]).

Жодних інших екранів, даних або навігаційних шляхів NOT SHALL бути доступні з `AccessDenied`.

### Requirement: Email normalization

The client MUST використовувати `email.lowercase()` (Locale-independent: `kotlin.text.lowercase()`) для ключа документа `allowedUsers/{email}`. Це узгоджено з веб-частиною і Firestore Rules, де ключ — email у нижньому регістрі.

### Requirement: Firestore Security Rules as independent perimeter

The Firestore rules MUST незалежно від мобільного коду блокувати читання будь-яким користувачем, чий email не у `allowedUsers`, а також читання чужих уроків. Це той самий файл `firestore.rules`, що публікується з веб-репозиторію — мобільний клієнт нічого не публікує, а лише довіряє опублікованим правилам.

Канонічний вміст rules (узгоджено з вебом):

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    function emailKey() { return request.auth.token.email.lower(); }

    function isAllowed() {
      return request.auth != null
        && exists(/databases/$(database)/documents/allowedUsers/$(emailKey()));
    }

    function isAdmin() {
      return isAllowed()
        && get(/databases/$(database)/documents/allowedUsers/$(emailKey())).data.role == 'admin';
    }

    match /allowedUsers/{email} {
      allow read: if isAllowed();
      allow write: if isAdmin();
    }

    match /lessons/{lessonId} {
      allow read: if isAllowed() && resource.data.ownerUid == request.auth.uid;
      allow write: if isAdmin();
    }
  }
}
```

#### Scenario: Disallowed user bypasses client check

- **GIVEN** зловмисник, що модифікував APK і пропустив клієнтську перевірку
- **WHEN** додаток робить запит на `lessons` без запису у `allowedUsers`
- **THEN** Firestore Rules повертають PERMISSION_DENIED — жодних даних не повернеться

### Requirement: No role-based UI on mobile (MVP)

The mobile MVP NOT SHALL розрізняти UI для `role == "admin"` і `role == "user"`. Будь-які адмінські операції доступні лише через веб. Поле `role` зчитується тільки якщо знадобиться у майбутньому; у MVP його присутність ігнорується після факту «доступ є».
