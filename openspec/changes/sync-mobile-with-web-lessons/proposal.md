# Change: Sync mobile with the web's catalog and three lesson types

## Why

The mobile client has drifted from the web in three observable ways. A learner who plays the same lesson on both platforms gets a different experience, and any lesson the admin authored in the new types is silently unplayable on mobile.

1. **Only one lesson type works.** `data/model/Lesson.kt` declares `enum class LessonTypeId { MULTIPLE_CHOICE }` and `FolderTreeViewModel` marks every other lesson as `supported = false`. The web ships three types — `multiple_choice`, `inline_dropdowns`, `text_input` — backed by full engines in `verba-web/src/lib/engines/`. Mobile has no model, no schema, and no renderer for the latter two.
2. **The catalog is read out-of-band.** `data/FoldersRepository.kt` hits Firestore directly to list `folders` and `lessons`. The web exposes `GET /api/folders` (with recursive `lesson_count`) and `GET /api/lessons?folder_id=…` for the same data, plus the `Folder.path[]` denormalized ancestor chain that powers efficient subtree queries. Mobile's model has no `path[]`, fetches lessons for every folder up-front, and bypasses the API's centralized access checks.
3. **Run lifecycle is incomplete.** `RunsApi.kt` and `RunPlayViewModel` cover create / next-task / answer / complete / get, but only for `MultipleChoiceTask`. Three pieces are missing:
   - The `POST /api/runs/{id}/next-task` endpoint can return `202 { status: "generating" }` while a peer request is still calling OpenAI; mobile treats this as a generic error.
   - Text-input answers can be appealed via `POST /api/runs/{id}/judge`, which re-asks OpenAI to decide. Mobile has no UI for this, so a learner with a valid synonym gets no recourse.
   - `task_count` on the wire is `number | null` (null = "infinite, learner ends manually") but mobile types it as `Int`, blocking that mode.

Additionally, `UniversalSettings` on mobile lacks the constraint inputs the web added with the grammar-validated sentence pipeline (`english_tenses`, `conditionals`, `sentence_types`, `explanation_enabled`). Lessons that pin those constraints show as "missing settings" on mobile and cannot start a run.

## What changes

- **Add the two missing lesson types.** Introduce sealed `Task` / `UserAnswer` hierarchies discriminated by `lesson_type`. Add `InlineDropdownsTask` + `InlineDropdownsUserAnswer` and `TextInputTask` + `TextInputUserAnswer` data classes, each with the same shape as the web's `src/lib/types/{inline-dropdowns,text-input}.ts`.
- **Switch the catalog to the HTTP API.** `FoldersRepository` is replaced (or backed by) a `CatalogApi` that calls `GET /api/folders` and `GET /api/lessons?folder_id=…`. `Folder` gains `path: List<String>`. The Firestore-direct path is removed; the only direct-Firestore call remaining is the `allowedUsers` check during login (which has no API equivalent today).
- **Render all three task types in the run player.** Refactor `RunPlayScreen` so the body delegates to a per-type Composable: `MultipleChoiceQuestion`, `InlineDropdownsQuestion`, `TextInputQuestion`. The view-model handles a sealed `CurrentTask` and a sealed `PendingAnswer`.
- **Wire the judge appeal flow.** When `feedback_mode == immediate` and a text-input answer comes back `is_correct = false` without a prior `judge_verdict`, show an "Appeal answer" affordance that calls `POST /api/runs/{id}/judge`. Display the verdict and (if upheld) update the task's correctness in-place.
- **Handle in-progress generation.** `RunsApi.nextTask` distinguishes `Generating` (HTTP 202) from a real task. The view-model polls with backoff until a task arrives, an error is returned, or the user cancels.
- **Support open-ended runs.** `CreateRunRequest.task_count` becomes nullable. `RunSetupScreen` exposes an "open-ended" toggle. `RunPlayScreen` shows "Finish now" instead of "Generating…" once an unbounded run reaches the user's chosen stopping point.
- **Expand universal settings.** Add `english_tenses`, `conditionals`, `sentence_types`, `explanation_enabled` to `UniversalSettings` and `EffectiveSettings`. Make them optional inputs on `RunSetupScreen` (since the web makes them optional unless the lesson pins them).
- **Localize the new surface.** Add UK + EN strings for inline-dropdowns, text-input, appeal, and the new enums (`EnglishTense`, `Conditional`, `SentenceType`). Map new API error codes (`already_judged`, `judge_failed`, `all_tasks_generated`, etc.) in `ApiErrorMessage.kt`.

## Impact

**New mobile capabilities** — `mobile-catalog`, `mobile-lesson-types`, `mobile-run-playthrough`. These are the first specs for this project, so each delta is a pure `## ADDED Requirements` document. Once shipped they promote to `openspec/specs/`.

**Modified mobile code (representative)**
- `data/model/Lesson.kt`, `data/model/Folder.kt`, `data/model/Universal.kt`, `data/model/Run.kt` — new fields, sealed task hierarchy, nullable `task_count`.
- `data/model/InlineDropdowns.kt`, `data/model/TextInput.kt` — new files.
- `data/api/CatalogApi.kt` — new file replacing `FoldersRepository` for the lesson-tree fetch.
- `data/api/RunsApi.kt` — `nextTask` returns a sealed `NextTaskOutcome` (`Task | Generating`); new `judge(runId, index)` method.
- `ui/runs/RunPlayScreen.kt` + `RunPlayViewModel.kt` — split into per-type renderers, polling logic, judge appeal.
- `ui/runs/RunSetupScreen.kt` + `RunSetupViewModel.kt` — open-ended toggle, expanded settings inputs.
- `ui/folders/FolderTreeViewModel.kt` — drop the `supported` filter; all three types are playable.
- `ui/errors/ApiErrorMessage.kt` + `ui/labels/EnumLabels.kt` — new codes and enum labels.
- `app/src/main/res/values{,-en}/strings.xml` — new strings.

**No backend changes.** Every endpoint and response shape already exists in `verba-web`; this change only catches mobile up to them. The shared Firestore rules and the `allowedUsers` allow-list are unchanged.

**No data migration.** Mobile holds no persistent state beyond locale preference.

**Out of scope** — admin features (create/edit lessons, manage users, change `allowedUsers`), voice features beyond what is already scaffolded, offline cache, and any iOS work. Those stay in the future-work notes in `mobile-app-spec.md`.
