# Capability: mobile-catalog

## ADDED Requirements

### Requirement: Catalog is fetched from the HTTP API, not Firestore-direct

The mobile client SHALL obtain the folder tree and the lessons inside a folder by calling the `verba-web` HTTP API. Direct Firestore reads of the `folders` or `lessons` collections from the client are not permitted.

The only direct-Firestore read allowed at catalog time is the `allowedUsers/{email}` lookup performed once after sign-in, because that check has no API equivalent.

#### Scenario: Folder list comes from `GET /api/folders`

- **WHEN** the user opens the catalog screen for the first time after sign-in
- **THEN** the client SHALL issue exactly one `GET /api/folders` request with the Firebase ID token as `Authorization: Bearer …`
- **AND** the client SHALL NOT issue any direct Firestore query against the `folders` collection

#### Scenario: Lessons of a folder come from `GET /api/lessons?folder_id=…`

- **WHEN** the user navigates into a folder
- **THEN** the client SHALL issue `GET /api/lessons?folder_id={id}` with the Firebase ID token
- **AND** the client SHALL render the response's `lessons[]` array in alphabetical order by `title` (the server already sorts; the client SHALL NOT re-sort the response)
- **AND** the client SHALL NOT pre-fetch lessons of sibling or descendant folders

### Requirement: Folder model carries the denormalized ancestor chain

The `Folder` model SHALL include a `path: List<String>` field containing the ordered chain of folder ids from the root down to and including the folder itself. The breadcrumb on `FolderTreeScreen` SHALL render directly from `path` — the client SHALL NOT walk `parent_id` chains across separate folders to assemble it.

#### Scenario: Breadcrumb for a deep folder

- **GIVEN** a folder `id = "f3"` with `path = ["f1", "f2", "f3"]`
- **WHEN** the user opens that folder
- **THEN** the breadcrumb SHALL render the names of folders `f1 → f2 → f3` in that order
- **AND** the client SHALL NOT issue extra Firestore or API requests to resolve `f1` and `f2`'s names — they SHALL be looked up in the already-loaded folders list

### Requirement: Recursive lesson counts are read from the server

The `Folder.lesson_count` returned by `GET /api/folders` is a recursive count (lessons in the folder plus all its descendants). The client SHALL display this value as-is and SHALL NOT recompute it from the `lessons` collection.

#### Scenario: Folder card shows server-supplied lesson count

- **GIVEN** the server returns `Folder { id: "f1", lesson_count: 42 }`
- **WHEN** the folder card for `f1` is rendered on `FolderTreeScreen`
- **THEN** the card SHALL show "42 lessons" (localized via the existing `lesson_count` plural)

### Requirement: All three lesson types are surfaced as playable

Every lesson returned by the API SHALL be rendered as a playable item in the catalog, regardless of `type`. The client SHALL NOT mark inline-dropdowns or text-input lessons as unsupported or hide them.

#### Scenario: Lesson list mixes the three types

- **GIVEN** a folder contains one lesson of each type (`multiple_choice`, `inline_dropdowns`, `text_input`)
- **WHEN** the user opens the folder
- **THEN** the lesson list SHALL show all three items
- **AND** each item SHALL render an icon that distinguishes its type
- **AND** tapping any item SHALL navigate to `LessonDetailScreen`

### Requirement: Lesson icons distinguish type

Each lesson item on `FolderTreeScreen` SHALL render an icon whose shape uniquely identifies its `type`. The mapping between `LessonTypeId` and icon is part of the design and SHALL be consistent across the catalog and `LessonDetailScreen`.

#### Scenario: A text-input lesson visually differs from a multiple-choice lesson

- **WHEN** the catalog renders a `text_input` lesson next to a `multiple_choice` lesson
- **THEN** their leading icons SHALL be different
- **AND** the choice of icon SHALL match the one used on `LessonDetailScreen` for the same lesson

### Requirement: Catalog read failures degrade gracefully

When `GET /api/folders` or `GET /api/lessons` fails, the `FolderTreeScreen` SHALL show a localized error from `ApiErrorMessage` and a retry affordance. The screen SHALL NOT fall back to a Firestore read.

#### Scenario: API returns 503 on folder fetch

- **WHEN** `GET /api/folders` returns HTTP 503
- **THEN** the catalog screen SHALL show the localized message for `error_upstream_failed` (or the generic fallback if no specific code matches)
- **AND** a "Retry" button SHALL re-issue the request
- **AND** the screen SHALL NOT issue a Firestore query against `folders`
