# Capability: mobile-lesson-types

## ADDED Requirements

### Requirement: Three lesson types are supported end-to-end

The mobile client SHALL support the three lesson types defined by `verba-web`: `multiple_choice`, `inline_dropdowns`, `text_input`. For each type the client SHALL:

- Model the task (`stem`/`stem_template`, gaps/options, meta) as a `@Serializable` data class.
- Model the user answer as a `@Serializable` data class with the wire shape the engine expects.
- Render a question Composable that collects the user's answer.
- Render a feedback Composable that shows correctness and explanation when reveals are available.

#### Scenario: Inline-dropdowns task is playable

- **GIVEN** a run whose `lesson_type == inline_dropdowns`
- **WHEN** the run player loads a task
- **THEN** the screen SHALL render the task's `stem_template`, replacing each `{{gap<n>}}` placeholder with an inline dropdown showing the corresponding `gaps[n].options`
- **AND** the "Submit" action SHALL be disabled until every gap has a selected option
- **AND** submitting SHALL POST `{ index, user_answer: { selections: [int, …] } }` to `/api/runs/{id}/answer`

#### Scenario: Text-input task is playable

- **GIVEN** a run whose `lesson_type == text_input`
- **WHEN** the run player loads a task
- **THEN** the screen SHALL render the task's `stem_template`, replacing each `{{gap<n>}}` placeholder with a single-line text field whose placeholder is `gaps[n].hint` (or empty if `hint == null`)
- **AND** the "Submit" action SHALL be disabled until every text field is non-empty (whitespace-only counts as empty)
- **AND** submitting SHALL POST `{ index, user_answer: { inputs: ["…", …] } }` to `/api/runs/{id}/answer`

#### Scenario: Multiple-choice behaviour is unchanged

- **GIVEN** a run whose `lesson_type == multiple_choice`
- **WHEN** the run player loads a task
- **THEN** the screen SHALL render `stem` and each `options[i]` as a selectable card, as it did before this change
- **AND** submitting SHALL POST `{ index, user_answer: { selected_index } }` to `/api/runs/{id}/answer`

### Requirement: Tasks are decoded into a sealed type hierarchy

The `LessonRun.tasks[]` and the response of `POST /api/runs/{id}/next-task` SHALL be decoded into a sealed `Task` Kotlin type with one subclass per lesson type. The `lesson_type` discriminator carried by the parent `LessonRun` SHALL be used to select the per-task serializer.

#### Scenario: View-model handles task type exhaustively

- **WHEN** `RunPlayViewModel` exposes `currentTask: Task?` to the UI
- **THEN** a Kotlin `when (task)` statement on the sealed `Task` SHALL be exhaustive without an `else` branch covering only `MultipleChoiceTask`, `InlineDropdownsTask`, and `TextInputTask`

### Requirement: Reveals are server-controlled, never client-derived

The client SHALL treat `correct_index`, `correct_answers`, `explanation`, `is_correct`, `meta.validated_sentence`, `meta.translation_uk`, and `meta.assembled_sentence` as fields that MAY be absent from the wire response. The UI SHALL render gracefully when any of them are absent and SHALL NOT attempt to derive them from other fields.

#### Scenario: End-mode unanswered task hides reveals

- **GIVEN** a run with `feedback_mode == "end"` and a task that has not yet been answered
- **WHEN** the task is rendered
- **THEN** the question Composable SHALL render the task in a "neutral" visual state (no green/red on options or gaps)
- **AND** the feedback panel SHALL NOT show a correct/incorrect verdict
- **AND** the client SHALL NOT raise an error or display "missing data" — absence of reveals is the expected state in this mode

#### Scenario: End-mode answered task shows submission confirmation

- **GIVEN** a run with `feedback_mode == "end"` and a task that has just been answered
- **WHEN** the API returns the task with reveals stripped
- **THEN** the feedback panel SHALL show a neutral "Answer submitted" message
- **AND** the "Next" / "See result" action SHALL be enabled

### Requirement: User answer submission is shape-correct per type

The `AnswerRequest.user_answer` field sent to `POST /api/runs/{id}/answer` SHALL be a JSON object whose shape matches the engine's `UserAnswerSchema` for the current `lesson_type`. The client SHALL NOT send extra fields, SHALL NOT omit required fields, and SHALL NOT change the wire shape across types (e.g. SHALL NOT send `selected_index` for a text-input task).

#### Scenario: Wrong-shape answer is impossible to construct

- **GIVEN** a `TextInputTask`
- **WHEN** the user submits
- **THEN** the request body SHALL be of shape `{ index: <int>, user_answer: { inputs: [<string>, …] } }`
- **AND** the request SHALL NOT carry `selected_index` or `selections`

### Requirement: Text-input judge appeal is a first-class flow

For runs with `feedback_mode == "immediate"`, when a text-input task comes back with `is_correct == false` and `judge_verdict == null`, the UI SHALL offer an "Appeal answer" affordance that calls `POST /api/runs/{id}/judge`.

The client SHALL NOT offer the appeal affordance for tasks where:

- `lesson_type != text_input`, OR
- `is_correct == true`, OR
- `judge_verdict != null` (the task has already been judged), OR
- `feedback_mode == "end"` (during play — verdicts during play would defeat the delayed-feedback contract).

#### Scenario: Wrong text-input answer can be appealed once

- **GIVEN** a text-input run with `feedback_mode == immediate`
- **AND** the user submitted "colour" for a task whose `correct_answers == ["color"]`
- **WHEN** the task returns with `is_correct: false` and no `judge_verdict`
- **THEN** the feedback panel SHALL render an "Appeal answer" button
- **WHEN** the user taps it
- **THEN** the client SHALL POST `{ index: <i> }` to `/api/runs/{id}/judge`
- **AND** while waiting SHALL show a spinner on the button
- **AND** the button SHALL NOT be re-tappable while the request is pending

#### Scenario: Appeal accepted promotes the task to correct

- **WHEN** `POST /api/runs/{id}/judge` returns `{ is_actually_correct: true, reason: "British spelling 'colour' is acceptable.", task: … }`
- **THEN** the feedback panel SHALL switch to a positive "Accepted on appeal" state showing `reason`
- **AND** the local task model SHALL set `is_correct = true` and store the returned `judge_verdict`
- **AND** the "Next" action SHALL be enabled

#### Scenario: Appeal rejected shows verdict and removes the button

- **WHEN** `POST /api/runs/{id}/judge` returns `{ is_actually_correct: false, reason: "…", task: … }`
- **THEN** the feedback panel SHALL show the `reason` text inline beneath the original "Incorrect" verdict
- **AND** the "Appeal answer" button SHALL be removed (the verdict is final)

#### Scenario: Already-judged task does not show the button

- **GIVEN** a text-input task whose `judge_verdict` is non-null when the screen first renders
- **WHEN** the feedback panel is shown
- **THEN** the "Appeal answer" button SHALL NOT be rendered, regardless of `is_correct`

### Requirement: Universal-settings inputs cover the validated-sentence pipeline

The client SHALL model `english_tenses: List<EnglishTense>?`, `conditionals: List<Conditional>?`, `sentence_types: List<SentenceType>?`, and `explanation_enabled: Boolean?` on both `UniversalSettings` and `EffectiveSettings`. `RunSetupScreen` SHALL expose them as optional inputs.

#### Scenario: Setup screen pre-populates advanced inputs from effective settings

- **GIVEN** a lesson whose `effective_settings` includes `english_tenses: ["past_simple", "present_perfect"]`
- **WHEN** the user opens `RunSetupScreen` for that lesson
- **THEN** the advanced section SHALL pre-select chips for `past_simple` and `present_perfect`
- **AND** the user MAY add or remove tenses before starting the run
- **AND** the resulting `CreateRunRequest.player_override.english_tenses` SHALL reflect the final selection
