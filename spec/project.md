# Verba Mobile — Project Context

## Призначення

Нативний Android-додаток Verba: базовий прототип для перегляду текстових уроків англійської, які створює адмін через веб (`d:\Projects\verba-web`). Спільний Firebase-бекенд (Auth + Firestore). Мобільний клієнт працює **тільки на читання**. Додатково — демо-обгортка голосу (TTS + STT) як фундамент під майбутні голосові фічі.

## Стек

| Шар | Технологія |
|-----|-----------|
| Мова / UI | Kotlin + Jetpack Compose |
| Auth | Firebase Authentication (Google) + Credential Manager |
| DB | Cloud Firestore (Android SDK) |
| TTS | Android `TextToSpeech` (on-device) |
| STT | Android `SpeechRecognizer` (on-device де доступно) |
| Build | Gradle (Kotlin DSL), `google-services` plugin |

## Принципи

- **Один Firebase-проєкт на дві платформи.** Той самий `projectId`, що й веб → один Google-акаунт дає один `uid`. Це інваріант, від якого залежить логіка доступу.
- **Read-only клієнт.** Жодних мутацій з мобільного у MVP. Створення уроків і керування `allowedUsers` — лише через адмін-веб.
- **Прямий Firestore замість API веба.** Для базового прототипу мобільний читає `lessons` напряму через Android Firestore SDK. Security Rules — той самий незалежний периметр захисту, що й для веба.
- **Жодних секретних ключів у застосунку.** OpenAI ключ ніколи не зашивається у APK. Будь-який майбутній виклик OpenAI — через серверний ендпоінт веба.
- **Голос — центральна майбутня фіча.** Вибір нативного Android (а не React Native) обґрунтований саме прямим доступом до системних STT/TTS. У MVP — мінімальне демо, але архітектура має це закладати.

## Інтеграції

- **Спільний Firebase** з вебом (`d:\Projects\verba-web`): колекції `allowedUsers` і `lessons`, провайдер Google, ті самі Firestore Rules.
- **Альтернативний шлях даних:** `GET /api/my-lessons` веба (Authorization: Bearer Firebase ID-token). Не використовується у MVP, але доступний для майбутніх сценаріїв, де потрібна серверна логіка. Контракт — у [веб-спеці](../../verba-web/spec/specs/mobile-api/spec.md).

## Поза обсягом

- Створення / редагування уроків з мобільного.
- iOS (тільки Android).
- Офлайн-кеш, push-нотифікації, складна синхронізація.
- Повноцінний голосовий діалог з ШІ (закладається на майбутнє: STT → OpenAI через серверний ендпоінт веба → TTS).
- Категоризація уроків (рівні, теми, прогрес, медіа).
- Інші провайдери auth.

## Орієнтація у спеці

- [specs/authentication/spec.md](specs/authentication/spec.md) — Google sign-in через Credential Manager
- [specs/access-control/spec.md](specs/access-control/spec.md) — allowedUsers, AccessDenied, Firestore Rules
- [specs/lessons/spec.md](specs/lessons/spec.md) — LessonList, LessonDetail, читання з Firestore
- [specs/voice/spec.md](specs/voice/spec.md) — TTS і STT демо
- [specs/android-config/spec.md](specs/android-config/spec.md) — Manifest, дозволи, SHA-фінгерпринти, build-конфіг
- [changes/add-initial-prototype/proposal.md](changes/add-initial-prototype/proposal.md) — план MVP
- [changes/add-initial-prototype/tasks.md](changes/add-initial-prototype/tasks.md) — імплементаційний чек-лист
- Оригінальна неформальна спека: [../mobile-app-spec.md](../mobile-app-spec.md)
