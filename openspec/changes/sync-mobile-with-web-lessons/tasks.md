# Implementation tasks

Ordered by dependency. Each section can ship as a separate commit.

## 1. Data model: universal settings & shared enums

- [ ] 1.1 Add enums `EnglishTense`, `Conditional`, `SentenceType` in `data/model/Universal.kt` matching `verba-web/src/lib/types/universal.ts` value sets (`SerialName` for each kotlinx-serialization enum constant).
- [ ] 1.2 Extend `UniversalSettings` with nullable `english_tenses`, `conditionals`, `sentence_types`, `explanation_enabled`.
- [ ] 1.3 Extend `EffectiveSettings` with the same fields (still nullable — the web's `REQUIRED_SETTINGS_KEYS` does not include them).
- [ ] 1.4 Update `RunSetupViewModel.RunSetupUiState` so it can carry the new fields; pre-populate from `effective_settings` on load.
- [ ] 1.5 Update `ui/runs/RunSetupScreen.kt` to surface the new sections under a collapsible "Advanced" group; values are optional.

## 2. Data model: polymorphic Task hierarchy

- [ ] 2.1 In `data/model/Task.kt` (new file) define `sealed interface Task` with the four common fields (`index`, `meta`, `generatedAt`, `answeredAt`, `isCorrect`, `userAnswerRaw: JsonObject?`).
- [ ] 2.2 Move `MultipleChoiceTask` into `Task` hierarchy as a sealed subclass; preserve its existing wire shape.
- [ ] 2.3 Add `data/model/InlineDropdowns.kt`:
  - `InlineDropdownsSettings { skill_target, min_gaps, max_gaps, options_per_gap, presentation_format }`
  - `InlineDropdownsGap { options: List<String>, correct_index: Int?, explanation: String? }` (last two null when reveals are hidden)
  - `InlineDropdownsTaskMeta { skill_target, distractor_type, level, gap_count, … }`
  - `InlineDropdownsUserAnswer { selections: List<Int> }`
  - `InlineDropdownsTask : Task` with `stem_template`, `gaps`, `meta`, `user_answer?`.
- [ ] 2.4 Add `data/model/TextInput.kt`:
  - `TextInputSettings { skill_target, min_gaps, max_gaps, presentation_format }`
  - `TextInputGap { correct_answers: List<String>?, explanation: String?, hint: String? }` (first two null when hidden)
  - `TextInputTaskMeta { skill_target, level, gap_count, … }`
  - `TextInputUserAnswer { inputs: List<String> }`
  - `JudgeVerdict { is_actually_correct: Boolean, reason: String, judged_at: String }`
  - `TextInputTask : Task` with `stem_template`, `gaps`, `meta`, `user_answer?`, `judge_verdict?`.
- [ ] 2.5 Extend `LessonTypeId` with `INLINE_DROPDOWNS` and `TEXT_INPUT` (`@SerialName("inline_dropdowns")`, `@SerialName("text_input")`).
- [ ] 2.6 Write `Task.parse(lessonType: LessonTypeId, json: JsonElement): Task` that selects the right per-type serializer.
- [ ] 2.7 In `data/model/Run.kt`, change `LessonRun.tasks: List<JsonObject>` to lazily parse via `Task.parse(this.lesson_type, …)`; expose `fun parsedTasks(): List<Task>`.

## 3. API layer

- [ ] 3.1 Create `data/api/CatalogApi.kt` with:
  - `suspend fun listFolders(): ApiResult<List<Folder>>` calling `GET /api/folders`.
  - `suspend fun listLessons(folderId: String): ApiResult<List<Lesson>>` calling `GET /api/lessons?folder_id={id}`.
- [ ] 3.2 Update `data/model/Folder.kt`: add `path: List<String> = emptyList()`. Remove the legacy `universal_settings` field (web no longer stores settings on folders).
- [ ] 3.3 Update `data/model/Lesson.kt`: add `universal_settings: UniversalSettings? = null` (the server-resolved one). Keep `universal_override` for now but treat it as deprecated.
- [ ] 3.4 In `data/api/RunsApi.kt`:
  - Change `nextTask` to return `NextTaskOutcome` (`Ready(task)` / `Generating` / `Failed(error)`). When HTTP status is 202 or body has `{ status: "generating" }`, return `Generating`.
  - Make `CreateRunRequest.task_count: Int?`.
  - Add `suspend fun judge(runId: String, index: Int): ApiResult<JudgeResponse>` calling `POST /api/runs/{id}/judge`; the response surfaces `is_actually_correct`, `reason`, and the updated `task`.
  - Make `AnswerRequest.user_answer` accept any of the three user-answer shapes (use `JsonObject` at the wire boundary, build it from a sealed `PendingAnswer`).
- [ ] 3.5 In `VerbaApp.kt`, lazily expose `catalogApi`.

## 4. Replace `FoldersRepository` with API-backed catalog

- [ ] 4.1 Rewrite `data/FoldersRepository.kt` so `listFolders()` and `listLessons(folderId)` delegate to `CatalogApi`. Drop the `FirebaseFirestore` dependency from this class.
- [ ] 4.2 In `ui/folders/FolderTreeViewModel.kt`:
  - Use `Folder.path` to render breadcrumbs (no client-side ancestor walk).
  - Lazily fetch lessons for the current folder only (no whole-catalog scan on init).
  - Remove the `supported = (type == MULTIPLE_CHOICE)` gate; mark every lesson as playable.
- [ ] 4.3 In `ui/folders/FolderTreeScreen.kt`, surface lesson type icons distinct per type (use `Icons.Outlined.RadioButtonChecked` for multiple-choice, `Icons.Outlined.ArrowDropDownCircle` for inline-dropdowns, `Icons.Outlined.Edit` for text-input — or pick from `material-icons-extended` as the designer prefers).

## 5. Run setup: open-ended + advanced settings

- [ ] 5.1 In `ui/runs/RunSetupScreen.kt` add an "Open-ended" switch above the task-count field. When on, the task-count input is hidden and the resulting `CreateRunRequest.task_count` is `null`.
- [ ] 5.2 Add UI for advanced inputs (`english_tenses`, `conditionals`, `sentence_types`, `explanation_enabled`) in a collapsible section. Re-use `FilterChip` for the multi-select enums; `Switch` for the boolean.
- [ ] 5.3 In `ui/runs/RunSetupViewModel.kt`, wire `canStart` to accept `task_count == null` (open-ended is always valid for count, but level/variant/register/explanation_language/distractor_types still required).

## 6. Run play: polymorphic rendering + `Generating` polling

- [ ] 6.1 Split `ui/runs/RunPlayScreen.kt`:
  - Top-level `RunPlayScreen` holds `ProgressHeader`, the question container, the feedback panel, and the action row.
  - Per-type Composables: `MultipleChoiceQuestion`, `InlineDropdownsQuestion`, `TextInputQuestion` (new files under `ui/runs/questions/`).
- [ ] 6.2 In `ui/runs/RunPlayViewModel.kt`:
  - Replace `currentTask: MultipleChoiceTask?` with `currentTask: Task?` (sealed).
  - Introduce a sealed `PendingAnswer` (`MultipleChoice(selectedIndex)`, `InlineDropdowns(selections)`, `TextInput(inputs)`); the question Composables push into it.
  - `submit()` builds the wire `user_answer` JSON per type and calls `runsApi.answer(...)`.
  - `loadCurrent()` handles `NextTaskOutcome.Generating` by scheduling a re-attempt after 700ms (with the 30s ceiling described in `design.md`).
- [ ] 6.3 Render task-type-specific UI:
  - **Multiple choice** — keep existing card list; refactor it into the new `MultipleChoiceQuestion` composable without behaviour change.
  - **Inline dropdowns** — render `stem_template` by splitting on `{{gap<n>}}` placeholders. For each gap, render an inline `ExposedDropdownMenuBox` with the gap's `options`. Selections feed `InlineDropdownsUserAnswer.selections`.
  - **Text input** — render `stem_template` similarly. For each gap, render an `OutlinedTextField` with `placeholder = gap.hint ?: ""`. Inputs feed `TextInputUserAnswer.inputs`.
- [ ] 6.4 Feedback panel:
  - Show "Correct" / "Incorrect" + `explanation` when reveals are present.
  - When reveals are absent (end-mode unanswered or pre-reveal), show only "Answer submitted — see result at the end."

## 7. Text-input judge appeal

- [ ] 7.1 In `RunPlayViewModel`, add `appeal(index: Int)` which calls `runsApi.judge(runId, index)`. Update the in-memory task with the returned `judge_verdict` and (if accepted) flip `isCorrect` to true.
- [ ] 7.2 In `TextInputQuestion`, when `feedbackMode == IMMEDIATE` and `task.isCorrect == false` and `task.judgeVerdict == null`, render an `OutlinedButton(text = stringResource(R.string.run_text_appeal))` below the feedback. Disable during the request; show a spinner.
- [ ] 7.3 Render the verdict result:
  - Accepted → green panel with `reason`, mark task as correct, continue.
  - Rejected → muted panel with `reason`, hide the button.
- [ ] 7.4 In `RunResultScreen`, when rendering `mistakes`, mark any mistake whose task carries `judge_verdict.is_actually_correct == true` so it does not appear as a mistake (the server's `Statistics.mistakes` should already exclude these — verify and add a defensive client filter if not).

## 8. i18n & error mapping

- [ ] 8.1 In `app/src/main/res/values/strings.xml` (uk) and `values-en/strings.xml` (en), add strings for:
  - Inline-dropdowns gameplay: `run_inline_choose_for_gap`, `run_inline_gap_label`, `run_inline_submit_disabled_until_all`.
  - Text-input gameplay: `run_text_placeholder_hint`, `run_text_submit_disabled_until_all`, `run_text_appeal`, `run_text_appeal_pending`, `run_text_appeal_accepted`, `run_text_appeal_rejected`.
  - Open-ended run: `start_open_ended`, `start_open_ended_description`, `run_finish_now`, `run_count_unbounded`.
  - Lesson-type labels: `label_lesson_type_inline_dropdowns`, `label_lesson_type_text_input`.
  - Enum labels: `label_tense_present_simple` … (full set from `EnglishTense`), `label_conditional_zero/first/second/third/mixed/none`, `label_sentence_type_affirmative/negative/yes_no_question/wh_question/imperative/exclamative`.
- [ ] 8.2 In `ui/labels/EnumLabels.kt`, add `@Composable label()` overloads for the new enums and a `labelForRawTense/Conditional/SentenceType(key: String?): String?` for stats / settings rendering.
- [ ] 8.3 In `ui/errors/ApiErrorMessage.kt`, map new codes: `already_judged`, `judge_failed`, `all_tasks_generated`, `invalid_index_gap`, `task_not_found`, `task_already_being_generated`. Add `error_*` strings in both `strings.xml` files.

## 9. Manual verification

- [ ] 9.1 Sign in on Android as a learner already in `allowedUsers`. Browse a folder tree authored on the web; confirm breadcrumbs are correct, lesson counts match the web admin view, and all three lesson types appear with the right icons.
- [ ] 9.2 Start a multiple-choice run with `feedback_mode = immediate`; confirm answers flow as before (no regressions).
- [ ] 9.3 Start an inline-dropdowns run; play through; confirm each gap dropdown lists the right options and submit only enables when all gaps are filled.
- [ ] 9.4 Start a text-input run with `feedback_mode = immediate`; type a wrong-but-plausible synonym; tap "Appeal"; confirm the verdict is rendered and (if accepted) the task is marked correct.
- [ ] 9.5 Start an open-ended run; answer 3 questions; tap "Finish now"; confirm results show 3 tasks with correct statistics.
- [ ] 9.6 Start a run while another device is generating the same task (simulate by hitting `next-task` twice in quick succession via two installs of the app on the same account); confirm the `Generating` state is observed and resolves without error.
- [ ] 9.7 Toggle UI language between UK and EN on `FolderTreeScreen`; restart-free locale switch already works — confirm all new strings are translated.

## 10. Update specs after ship

- [ ] 10.1 Promote `changes/sync-mobile-with-web-lessons/specs/mobile-catalog/spec.md` to `openspec/specs/mobile-catalog/spec.md` (folding `## ADDED Requirements` into a clean active spec).
- [ ] 10.2 Same for `mobile-lesson-types` and `mobile-run-playthrough`.
- [ ] 10.3 Delete `openspec/changes/sync-mobile-with-web-lessons/`.
