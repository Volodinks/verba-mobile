# Capability: mobile-run-playthrough

## ADDED Requirements

### Requirement: Run lifecycle mirrors the web API

A mobile run SHALL follow the same lifecycle as on the web:

1. Create via `POST /api/runs` (status `pending`).
2. Fetch each task via `POST /api/runs/{id}/next-task`.
3. Answer via `POST /api/runs/{id}/answer`.
4. Optionally appeal a text-input answer via `POST /api/runs/{id}/judge`.
5. Finalize via `POST /api/runs/{id}/complete`, transitioning the run to `completed`.

The mobile client SHALL NOT mutate run state through any other channel (no direct Firestore writes; no client-side status transitions).

#### Scenario: Run transitions only through documented endpoints

- **WHEN** the project is grepped for writes to the `lessonRuns` Firestore collection from `app/src/main/java/com/verba/mobile/`
- **THEN** there SHALL be no such writes
- **AND** every state change on a `LessonRun` displayed in the UI SHALL be the result of one of the five endpoints above returning a new `LessonRun` snapshot

### Requirement: `task_count` is nullable to support open-ended runs

`CreateRunRequest.task_count` SHALL be modeled as `Int?`. A `null` value SHALL mean "open-ended: the learner ends the run manually." `RunSetupScreen` SHALL provide a way for the user to choose between a fixed count and open-ended.

#### Scenario: Open-ended toggle starts a `null`-count run

- **GIVEN** the user is on `RunSetupScreen`
- **WHEN** they enable the "Open-ended" switch
- **THEN** the task-count input SHALL be hidden
- **AND** "Start" SHALL be enabled when the other required settings are filled
- **AND** the `POST /api/runs` body SHALL carry `task_count: null`

#### Scenario: Open-ended run shows "Finish now" after the first answer

- **GIVEN** a run with `task_count == null`
- **WHEN** the user has submitted an answer to at least one task
- **THEN** `RunPlayScreen` SHALL show a "Finish now" action
- **AND** tapping it SHALL POST `/api/runs/{id}/complete` and navigate to `RunResultScreen`
- **AND** the run SHALL NOT auto-finalize on the client without an explicit user action

### Requirement: The client handles the "still generating" wire signal

`POST /api/runs/{id}/next-task` MAY return HTTP `202` with a body of `{ status: "generating" }` when another in-flight request is generating the same task. The mobile client SHALL treat this as a "wait and retry" signal — not as an error — and SHALL poll until either a task arrives, a non-202 error is returned, or the user cancels by leaving the screen.

The polling SHALL use a base interval of 700 ms with a ceiling of 30 s total wait. While polling, the UI SHALL surface a "Still generating…" hint after 3 s so the user understands the delay.

#### Scenario: Single 202 resolves on the next poll

- **GIVEN** the run player calls `next-task` and gets HTTP `202 { status: "generating" }`
- **WHEN** 700 ms elapses
- **THEN** the client SHALL re-issue `next-task` for the same `index`
- **WHEN** the second response returns a `task`
- **THEN** the client SHALL render the task and stop polling

#### Scenario: Sustained 202 surfaces a user-facing hint

- **GIVEN** the run player has been receiving `202 { status: "generating" }` continuously for more than 3 s
- **THEN** the screen SHALL show a localized "Still generating…" indicator
- **AND** the indicator SHALL be removed as soon as a task is returned or an error occurs

#### Scenario: Total wait ceiling fails the load

- **GIVEN** the run player has been polling `next-task` for 30 s without receiving a task
- **THEN** the client SHALL stop polling
- **AND** show the localized message for `error_timeout`
- **AND** offer a "Retry" affordance that restarts the polling cycle

#### Scenario: Leaving the screen cancels polling

- **WHEN** the user navigates away from `RunPlayScreen` while polling is in progress
- **THEN** the polling coroutine SHALL be cancelled
- **AND** no further `next-task` requests SHALL be issued for the abandoned run

### Requirement: Answer submission is idempotent per task index

The client SHALL prevent the user from submitting the same `index` twice. Once an answer has been submitted for a given task, the UI SHALL disable the submit action until the user advances to the next task.

#### Scenario: Re-tapping submit is a no-op

- **GIVEN** the user has just tapped "Submit" on task `index = 2`
- **WHEN** they tap "Submit" again before the response returns
- **THEN** the client SHALL NOT issue a second `POST /api/runs/{id}/answer` request
- **AND** the visual state of the submit control SHALL indicate it is disabled

### Requirement: Statistics are rendered as the server computes them

`RunResultScreen` SHALL render the `Statistics` payload returned by `POST /api/runs/{id}/complete` without recomputing accuracy, totals, or breakdowns on the client. Per-skill and per-distractor breakdowns SHALL use the server-supplied keys, looked up via `labelForRawSkill` / `labelForRawDistractor` for display.

#### Scenario: Accuracy on the result screen matches the server

- **WHEN** `complete` returns `Statistics { total_tasks: 10, correct: 7, accuracy: 0.7, … }`
- **THEN** the summary card SHALL display "70%" (the server's `accuracy * 100` rounded to a whole number)
- **AND** the client SHALL NOT recompute accuracy from `correct / total_tasks`

#### Scenario: Unknown skill key falls back gracefully

- **GIVEN** `Statistics.breakdown_by_skill` contains a key the client does not yet have a label for
- **WHEN** the breakdown card renders
- **THEN** the row SHALL show the raw key as a fallback label (lowercased, underscores replaced with spaces) rather than crashing or omitting the row

### Requirement: Run errors map to localized API error messages

Every error response from the run endpoints SHALL be surfaced via `ApiErrorMessage.kt`. The client SHALL map at minimum: `not_allowed`, `run_not_found`, `task_not_found`, `run_already_finished`, `already_answered`, `already_judged`, `all_tasks_generated`, `invalid_index_param`, `invalid_index_gap`, `task_count_out_of_range`, `missing_required_setting`, `options_count_5_requires_c1_c2`, `rate_limited`, `generation_failed`, `judge_failed`, `timeout`, `upstream_failed`, `internal`.

#### Scenario: `already_answered` does not surface as a generic error

- **WHEN** `POST /api/runs/{id}/answer` returns `{ error: "already_answered" }` with HTTP 409
- **THEN** the UI SHALL show the specific localized "Question already answered" message (not the generic fallback)
- **AND** the run player SHALL move on to the next task instead of remaining stuck

#### Scenario: `rate_limited` shows a helpful retry message

- **WHEN** any run endpoint returns `{ error: "rate_limited" }`
- **THEN** the UI SHALL show the localized "Too many requests" message
- **AND** the failing action SHALL be re-attemptable (the request SHALL NOT be permanently blocked client-side)
