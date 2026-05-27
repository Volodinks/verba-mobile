# Design notes

This change touches the data layer, the network layer, three screens, and the i18n surface. The choices below are the ones likely to be re-litigated in review.

## 1. Polymorphic task modeling

The web treats the lesson type as a discriminator at every boundary (`lesson_type: "multiple_choice" | "inline_dropdowns" | "text_input"`) and engines fan out from there. Mobile already has `LessonTypeId` but only enumerates `MULTIPLE_CHOICE`, and `LessonRun.tasks` is typed `List<JsonObject>` — postponing the parse problem to the UI.

**Decision:** introduce a sealed hierarchy.

```kotlin
@Serializable
@JsonClassDiscriminator("lesson_type")
sealed interface Task {
    val index: Int
    val generatedAt: String
    val answeredAt: String?
    val isCorrect: Boolean?
}

@Serializable @SerialName("multiple_choice")
data class MultipleChoiceTask(...) : Task

@Serializable @SerialName("inline_dropdowns")
data class InlineDropdownsTask(...) : Task

@Serializable @SerialName("text_input")
data class TextInputTask(...) : Task
```

**Problem:** the wire format does NOT include `lesson_type` on each task — the discriminator lives on the parent `LessonRun.lesson_type` and on the `Lesson.type` it was generated from. We have two options:

- **(a) Re-shape on parse.** Decode `LessonRun` via a custom serializer that reads `lesson_type` once and uses it to choose the per-task serializer for every element in `tasks[]`. Same for the bare `NextTaskResponse.task`: thread `lessonType` from the `LessonRun` we bootstrapped.
- **(b) Decode tasks as `JsonElement` and resolve in the view-model.** Simpler, but pushes type-knowledge into the UI layer and loses compile-time exhaustiveness.

**Pick (a).** It localizes the polymorphism to one place (`Task.parse(lessonType, json)`) and lets `when (task)` in renderers be exhaustive. Keep the raw `JsonObject` around in `LessonRun` only for fields we don't yet model (`meta.*` reveal fields the server may add later).

`UserAnswer` mirrors the same pattern — `MultipleChoiceUserAnswer { selected_index }`, `InlineDropdownsUserAnswer { selections: List<Int> }`, `TextInputUserAnswer { inputs: List<String> }`. The submit endpoint takes `index + user_answer` shaped per type; the view-model picks the right serializer based on the current task.

## 2. Catalog data source: API vs Firestore-direct

`FoldersRepository` queries Firestore directly. That was acceptable when the mobile spec said "lessons are just text," but three things have changed:

- The web added `Folder.path: string[]` (denormalized ancestor chain) and uses it for subtree queries. Mobile's old `Folder { parent_id }` model can't reconstruct the breadcrumb without N+1 reads.
- The web's `GET /api/folders` computes recursive `lesson_count` server-side. Doing the same on the client costs an extra full scan of `lessons`.
- The visibility / allow-list logic centralizes on the server (`checkAccess(req)` in `src/lib/server/access.ts`). Firestore rules backstop it, but rule drift is easy to miss.

**Decision:** Move the catalog read to the API. Add `CatalogApi.listFolders()` and `CatalogApi.listLessons(folderId)`. Keep the `FoldersRepository` name but make it a thin wrapper that calls the API; do not preserve the Firestore path. The `allowedUsers` check stays on Firestore because there is no equivalent API endpoint today, and we want the access decision before the first authed API call.

**Trade-off:** the mobile client now requires the verba-web deployment to be reachable for browsing. Acceptable — there is no offline support today and adding the API dependency does not make it worse.

## 3. Folder model: adopt `path[]`

`Folder.path: List<String>` (the ordered ancestor chain including self) is what the breadcrumb renders from. We keep `parent_id` to mirror the wire format, but the UI reads `path` for navigation. Cycle / depth checks remain server-side; mobile just trusts the response.

## 4. Run lifecycle changes

### 4.1. Nullable `task_count`

The web treats `task_count: null` as "open-ended — the learner stops when they want." `CreateRunRequest.task_count: Int?` on mobile, default null only when the user toggles "open-ended" in `RunSetupScreen`. `RunPlayScreen` then:

- shows progress as a count (`N answered`) instead of a percentage bar,
- shows "Finish now" instead of "See result" at any time after the first answer,
- never auto-advances to results.

### 4.2. `next-task` 202 polling

The endpoint can return `202 { status: "generating" }` when another in-flight call to the same `(runId, index)` is already generating. Today mobile sees this as `ApiResult.Error(status=202, code=null)`.

**Decision:** `RunsApi.nextTask` returns a sealed `NextTaskOutcome`:

```kotlin
sealed interface NextTaskOutcome {
    data class Ready(val task: Task) : NextTaskOutcome
    data object Generating : NextTaskOutcome
    data class Failed(val error: UiError) : NextTaskOutcome
}
```

`RunPlayViewModel` polls every 700ms with a 30s ceiling and surface a "still generating…" hint to the user after 3s. Cancellation: leaving the screen cancels the polling coroutine.

### 4.3. Text-input judge appeal

For `feedback_mode == immediate`, after the user submits a wrong text-input answer:

- If `task.judge_verdict == null` and `task.is_correct == false`, show a small "Appeal answer" button under the feedback block.
- Tapping it calls `POST /api/runs/{id}/judge` with `{ index }`. The button shows a spinner; the view-model awaits the verdict.
- On `is_actually_correct == true`, replace the red "Incorrect" feedback with a neutral "Accepted on appeal" panel showing `reason`, mark the task green, and let the user proceed.
- On `is_actually_correct == false`, replace the appeal button with the verdict text (`reason`) inline; no second appeal.
- For `feedback_mode == end`, no appeal during play — the user can only see verdicts in the final results (and we do not auto-appeal there to avoid silently re-judging the whole run).

The web's `judge` endpoint already enforces "only text-input, only when not previously correct, only once." Mobile mirrors the UI preconditions but does not re-implement them — server errors map to `already_judged` / `task_not_found` and surface as a localized snackbar.

## 5. Visibility — trust the server

`fix-feedback-mode-info-leak` in `verba-web/openspec/changes/` consolidated reveal-stripping into one server function. Mobile must NOT derive correctness from absent fields and MUST NOT infer the correct answer from `meta.*`. Specifically:

- The view-model treats `correct_index == null` (multiple-choice) or `correct_answers == null` (text-input) as "the server is withholding this" and renders the task as un-feedback-able. The `is_correct` flag may also be `null` until the run completes.
- Only at `LessonRunStatus == COMPLETED` (i.e. after `POST /api/runs/{id}/complete` returns) do we render mistakes with full reveals.

## 6. Universal-settings expansion

Add `english_tenses: List<EnglishTense>?`, `conditionals: List<Conditional>?`, `sentence_types: List<SentenceType>?`, `explanation_enabled: Boolean?` to both `UniversalSettings` and `EffectiveSettings` (where they remain optional — the web requires only `level`, `english_variant`, `register`, `explanation_language`, `distractor_types`).

`RunSetupScreen` shows these as collapsible "Advanced" sections, pre-populated from the lesson's `effective_settings`. The user can override but does not have to.

## 7. UI: split `RunPlayScreen` by task type

`RunPlayScreen` currently embeds the multiple-choice UI inline. New structure:

```
RunPlayScreen
  ProgressHeader(currentIndex, taskCount?)
  Box {
    when (val task = state.currentTask) {
      is MultipleChoiceTask  -> MultipleChoiceQuestion(task, state, on…)
      is InlineDropdownsTask -> InlineDropdownsQuestion(task, state, on…)
      is TextInputTask       -> TextInputQuestion(task, state, on…)
      null                   -> GeneratingPlaceholder()
    }
  }
  FeedbackPanel(state.feedback)
  ActionRow(state)
```

Per-type Composables own their answer-collection state via a shared `PendingAnswer` sealed type lifted into the view-model. This keeps the view-model engine-agnostic at submit time: it serializes whichever `PendingAnswer` subtype is set.

## 8. i18n & enum labels

New strings in both `values/strings.xml` (uk, default) and `values-en/strings.xml`:

- inline-dropdowns UI: `run_inline_choose_for_gap`, `run_inline_select_each_gap`, …
- text-input UI: `run_text_placeholder_hint`, `run_text_appeal`, `run_text_appeal_pending`, `run_text_appeal_accepted`, `run_text_appeal_rejected`, …
- open-ended runs: `start_open_ended`, `run_finish_now`, …
- new error codes: `error_already_judged`, `error_judge_failed`, `error_all_tasks_generated`, `error_invalid_index_gap`, …
- new enums labels: `label_tense_*`, `label_conditional_*`, `label_sentence_type_*`.

Plural strings (`lesson_count`, `subfolder_count`) need no change — the catalog data shape stays the same.

## 9. Backwards compatibility / migration

No persisted client state changes, no schema migration. The only risk is decoding: an older app version receiving a `task` with a `lesson_type` it does not know will fail to deserialize. Since this change adds the missing types in one shot and the wire format already includes the discriminator on the parent, we don't need a fallback type. After this change ships, adding a fourth type would require either (a) a "graceful unknown" sealed branch, or (b) forcing an app upgrade — we will revisit then.

## 10. Risks & open questions

- **Polling cost on flaky networks.** A 700ms poll for 30s is 40+ requests for a single slow generation. We currently have no rate-limit headroom data; if this proves problematic, switch to a 1s base + 1.5× exponential backoff capped at 5s.
- **Judge appeal UX in end-mode results.** This change does not expose an appeal button in the final results screen for end-mode runs. If teachers ask for it, a follow-up change can add it (the endpoint already supports it; we just chose not to surface it without a clear design for the post-hoc flow).
- **Open-ended run UX.** We add the toggle but the screen text "How many tasks?" is awkward when the answer is "as many as you want." Final copy decided in implementation; nothing semantic to spec.
