# Verba Mobile — Project Conventions

## Purpose

Verba Mobile is a native Android client (Kotlin + Jetpack Compose) for the Verba English-learning platform. It is a **read-mostly** companion to `verba-web`: it does not generate tasks, edit lessons, or manage users — it only signs the learner in, browses the catalog created by admins on the web, and plays through lesson runs.

Authoritative state lives in the shared Firebase project (Auth + Firestore) and the `verba-web` HTTP API. Mobile owns no business logic that the web does not also own.

## Tech stack

- Kotlin 2.x + Jetpack Compose (Material 3) — single-activity app, navigation via `androidx.navigation:navigation-compose`.
- Firebase Auth (Google sign-in via Credential Manager) and Firestore (Android SDK) for identity and the `allowedUsers` allow-list check.
- Ktor `client-okhttp` for the HTTP API, with `client-content-negotiation` + `kotlinx.serialization` for JSON. Bearer auth uses the Firebase ID token, refreshed per request via `FirebaseUser.getIdToken(forceRefresh)`.
- `androidx.datastore:preferences` for small user preferences (current UI locale).
- On-device `TextToSpeech` and `SpeechRecognizer` are wired but not yet integrated into the run flow — reserved for the future voice features described in `mobile-app-spec.md`.

## Code layout (relevant to the catalog + runs flow)

- `app/src/main/java/com/verba/mobile/data/model/` — `@Serializable` data classes mirroring the web's `src/lib/types/` shapes. Keep these structurally identical to the wire format; the web is the source of truth.
- `app/src/main/java/com/verba/mobile/data/api/` — Ktor endpoint wrappers. One file per resource group (`LessonsApi`, `RunsApi`, …). Every method returns `ApiResult<T>` (Success / Error / Network).
- `app/src/main/java/com/verba/mobile/data/` — Firestore-backed repositories (auth allow-list, anything that the API does not yet expose).
- `app/src/main/java/com/verba/mobile/ui/<feature>/` — Compose screen + a single `AndroidViewModel` per screen, exposing a sealed `UiState` via `StateFlow`.
- `app/src/main/java/com/verba/mobile/ui/labels/EnumLabels.kt` — `@Composable label(): String` for every API enum the UI displays.
- `app/src/main/res/values{,-en}/strings.xml` — Ukrainian (default) and English UI strings. Every user-visible string MUST come from `strings.xml`; never hard-code Cyrillic or Latin display text in Kotlin.

## Conventions

- Models are wire-compatible with the web. When the web adds a field, add it here as nullable (`val newField: SomeType? = null`) and let `ignoreUnknownKeys = true` absorb the rest. When the web changes a field's shape, this project changes in lockstep — never silently downgrade.
- The client trusts server-side visibility: when the run's `feedback_mode == "end"` and the task is unanswered, the wire response will not contain `correct_index` / `correct_answers` / `explanation`. UI code MUST handle their absence gracefully and MUST NOT try to derive them.
- Polymorphic task rendering goes through the `lesson_type` discriminator. Adding a new task type means: (a) a sealed-subclass model in `data/model/`, (b) a renderer Composable in `ui/runs/`, (c) localized labels, and (d) a settings preview branch — not branching on type inside one giant Composable.
- HTTP errors are mapped through `ui/errors/ApiErrorMessage.kt` so the UI shows localized text. New error codes added to the API get a row here in the same change.
- UI text is bilingual (uk / en). When introducing new strings, add entries to **both** `values/strings.xml` and `values-en/strings.xml`; missing translations fall back to the default and look broken.
- Firestore-direct reads are a temporary shortcut. Prefer the HTTP API for any data that is exposed there, so the same access-control and visibility logic applies as on the web.

## OpenSpec workflow

Proposed changes live under `openspec/changes/<change-id>/`. Each change carries a `proposal.md` (why + summary), `tasks.md` (implementation checklist), an optional `design.md` for non-trivial work, and capability deltas under `specs/<capability>/spec.md` using the `## ADDED Requirements` / `## MODIFIED Requirements` / `## REMOVED Requirements` markers. Active capability specs (after a change ships) move to `openspec/specs/<capability>/spec.md`.
