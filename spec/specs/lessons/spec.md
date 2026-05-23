# lessons Specification

## Purpose

Показати дозволеному користувачу список його уроків та повний текст обраного уроку. Уроки створюються виключно через веб-адмінку і зберігаються у спільній з вебом Firestore колекції `lessons`. Мобільний клієнт читає їх напряму через Firestore Android SDK.

## Requirements

### Requirement: Lesson data model

The client SHALL читати документи колекції `lessons` зі схемою, узгодженою з веб-частиною:

- `ownerUid`: `String` — Firebase UID власника
- `body`: `String` — текст уроку
- `createdBy`: `String` — email адміна, що створив (інформаційне поле, можна не показувати у MVP)
- `createdAt`: `String` — ISO 8601 timestamp

Невідомі поля SHALL ігноруватися без помилок (forward-compatibility).

### Requirement: LessonList screen

The screen `LessonList` SHALL:
1. Виконати запит `lessons.whereEqualTo("ownerUid", currentUser.uid)` з сортуванням `orderBy("createdAt", DESCENDING)`.
2. Показати список елементів, де кожен елемент — превʼю уроку: перші 1-2 рядки тексту (truncate з трикрапкою).
3. Тап по елементу → навігація на `LessonDetail` з ID уроку.

#### Scenario: User with lessons

- **GIVEN** дозволений користувач з 3 уроками у Firestore
- **WHEN** відкриває `LessonList`
- **THEN** бачить 3 елементи, найновіший зверху, кожен з фрагментом тексту

#### Scenario: User without lessons

- **GIVEN** дозволений користувач без уроків
- **WHEN** відкриває `LessonList`
- **THEN** бачить порожній стан з повідомленням «Уроків ще немає. Адмін додасть їх через веб.»

#### Scenario: Loading state

- **GIVEN** запит до Firestore у процесі
- **WHEN** дані ще не прийшли
- **THEN** показується спінер або скелетон-плейсхолдери; список не блимає порожнім станом передчасно

#### Scenario: Read failure

- **GIVEN** Firestore недоступний (немає інтернету або 5xx)
- **WHEN** запит фейлиться
- **THEN** показується повідомлення «Не вдалося завантажити уроки» з кнопкою «Спробувати ще»

### Requirement: LessonDetail screen

The screen `LessonDetail` SHALL показувати:
1. Повний текст уроку (`body`), з нормальним форматуванням абзаців (`\n\n` → нові абзаци, `\n` → перенос рядка).
2. Кнопку «Озвучити» (TTS, див. [[voice]]).
3. Кнопку «Сказати» (STT, див. [[voice]]).
4. Кнопку «Назад» у toolbar для повернення на `LessonList`.

Текст MUST залишатись повністю читабельним для уроків до ~20000 символів (verticalScroll).

#### Scenario: Open lesson

- **GIVEN** користувач на `LessonList`
- **WHEN** тапає на урок
- **THEN** відкривається `LessonDetail` з повним текстом і двома кнопками голосової обгортки

#### Scenario: Lesson not found

- **GIVEN** користувач відкриває `LessonDetail` з ID, що більше не існує (видалений адміном)
- **WHEN** Firestore повертає документ що `!exists()`
- **THEN** показується «Урок недоступний», кнопка «Повернутися до списку»

### Requirement: Direct Firestore as primary path

The mobile client SHALL використовувати Firestore Android SDK напряму для читання уроків. `GET /api/my-lessons` веба NOT SHALL використовуватися у MVP — це резервний шлях для майбутніх сценаріїв, де знадобиться серверна логіка (агрегації, фільтри, дані поза Firestore).

> Контракт `/api/my-lessons` зафіксовано у веб-спеці і MUST залишатися стабільним: див. `../../verba-web/spec/specs/mobile-api/spec.md`.

### Requirement: Read-only surface

The mobile client MUST NOT надавати UI для створення, редагування або видалення уроків. Жоден code path NOT SHALL викликати `add`, `set`, `update`, `delete` на колекції `lessons`. Firestore Rules дублюють цю заборону (`allow write: if isAdmin()`).

### Requirement: Data freshness

The screen `LessonList` SHOULD оновлювати дані при кожному відкритті (не кешувати безкінечно). Простий шлях — використовувати `Source.DEFAULT` (Firestore сам вирішує між сервером і кешем) при cold load, без явного `Source.CACHE` чи `Source.SERVER`. Real-time підписка (`addSnapshotListener`) — допустима, але не вимагається у MVP.
