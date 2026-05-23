# Add Initial Prototype

## Why

Зараз у репозиторії `verba-mobile` порожньо — лише `mobile-app-spec.md` і ця openspec-документація. Веб-частина (`d:\Projects\verba-web`) уже має повну спеку MVP і реалізує спільний Firebase-бекенд (Auth + Firestore + `lessons` + `allowedUsers`). Мобільний клієнт — друга платформа того самого продукту: дозволені користувачі мають змогу читати свої уроки з телефона і отримати фундамент під майбутні голосові фічі.

Це greenfield-Android-проєкт, тому всі capabilities — ADDED.

## What Changes

Реалізувати з нуля наступні capabilities (див. `spec/specs/`):

- **authentication** — Google sign-in через Firebase Auth + Credential Manager; спільний `uid` з вебом; session persistence; sign-out.
- **access-control** — клієнтська перевірка `allowedUsers`; `AccessDenied` екран; довіра до Firestore Rules як незалежного периметру.
- **lessons** — `LessonList` (читання `lessons` напряму з Firestore за `ownerUid`); `LessonDetail` з повним текстом; read-only поверхня.
- **voice** — демо TTS (озвучення тексту уроку через `TextToSpeech`) і демо STT (розпізнавання короткої фрази через `SpeechRecognizer`); рантайм-дозвіл `RECORD_AUDIO`; ізоляція голосової підсистеми під майбутнє розширення.
- **android-config** — `AndroidManifest.xml` з мінімальними дозволами; `google-services.json` спільного Firebase-проєкту; SHA-1/256 фінгерпринти; залежності (Firebase BoM, Credentials, Compose); single-activity Compose navigation.

Налаштувати інфраструктуру: Kotlin + Jetpack Compose, Gradle (Kotlin DSL), `google-services` plugin, Firebase SDK, Credential Manager, ViewModel + coroutines.

## Impact

- **Створюється з нуля:** усі 5 capabilities — ADDED.
- **Залежність від веб-частини:** спільний Firebase-проєкт MUST бути налаштований і `firestore.rules` опубліковано (це робиться з веб-репозиторію). Перший адмін заведений у Firestore. Хоча б один урок з `ownerUid` мобільного користувача створений через `/admin/lessons` веба — інакше нема що показувати.
- **Жодних змін у веб-частині не вимагається** для MVP. `GET /api/my-lessons` уже існує у веб-спеці як стабільний контракт, але мобільний MVP його не використовує (читає Firestore напряму).
- **Поза обсягом цієї зміни:** офлайн-кеш, push-нотифікації, голосовий діалог з LLM, iOS, мутації з мобільного, перегляд прогресу — окремі майбутні зміни.

## Acceptance

- [ ] Вхід через Google працює зі спільним Firebase-проєктом.
- [ ] Один Google-акаунт дає той самий `uid`, що й на вебі (перевірено вручну: залогінитись на вебі та мобільному, звірити `uid` у Firebase Console).
- [ ] Недозволений Google-користувач бачить `AccessDenied` з кнопкою «Вийти».
- [ ] Дозволений користувач бачить `LessonList` зі своїми уроками, найновіший зверху.
- [ ] Користувач не бачить чужих уроків, навіть якщо вручну спробувати запитати їх (Firestore Rules повертають PERMISSION_DENIED — перевірено логом).
- [ ] `LessonDetail` показує повний текст уроку з нормальним форматуванням абзаців.
- [ ] Кнопка «Озвучити» зачитує текст уроку англійською; кнопка «Зупинити» зупиняє; ресурси звільняються при виході з екрана.
- [ ] Кнопка «Сказати» розпізнає коротку англійську фразу і показує її; запит `RECORD_AUDIO` обробляється коректно при першому використанні.
- [ ] Жодного API-ключа стороннього сервісу у застосунку (перевірено пошуком `sk-`, `BEGIN PRIVATE KEY` у розпакованому release-APK).
- [ ] Release-білд з R8 не ламає Firestore-десеріалізацію уроків.
- [ ] Сесія переживає cold start (повторний запуск не вимагає вводу credentials).

## References

- Цільові специфікації: [spec/specs/](../../specs/)
- Імплементаційний чек-лист: [tasks.md](tasks.md)
- Контекст проєкту: [spec/project.md](../../project.md)
- Оригінальна неформальна спека: [../../../mobile-app-spec.md](../../../mobile-app-spec.md)
- Веб-спеки (для довідки про спільні моделі і Rules):
  - `../../../../verba-web/spec/specs/access-control/spec.md`
  - `../../../../verba-web/spec/specs/lessons/spec.md`
  - `../../../../verba-web/spec/specs/mobile-api/spec.md`
