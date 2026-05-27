# Working with OpenSpec in this repo

This directory holds **proposed and active specifications** for the Verba Mobile codebase, written in the OpenSpec convention. Specs describe *behaviour*, not implementation.

The sibling `verba-web` project owns the lesson generation pipeline, admin UI, and HTTP API. This project consumes that API on Android, so most mobile capabilities are scoped to "what the Android client must do given a stable backend contract." When the backend contract changes, the corresponding spec lives in `verba-web/openspec/`; mirror it here only when the mobile UX changes.

## Folder map

```
openspec/
  project.md                       Top-level project conventions (stack, layout, rules).
  AGENTS.md                        This file.
  specs/                           Active capability specs (source of truth for shipped behaviour).
    <capability>/
      spec.md
  changes/                         Proposed changes, one folder per change-id.
    <change-id>/
      proposal.md                  Why + what + impact, short.
      tasks.md                     Ordered, checkable implementation steps.
      design.md                    Optional. Required for non-trivial changes.
      specs/                       Deltas against active specs (or new capability specs).
        <capability>/
          spec.md
```

## Writing capability specs

Each requirement is one `### Requirement: <name>` followed by a normative sentence using **SHALL / MUST / MUST NOT** (RFC-2119 style), then **at least one scenario** introduced by `#### Scenario: <name>` and written as `WHEN … / THEN …` bullets.

## Writing delta specs (in `changes/<id>/specs/<capability>/spec.md`)

Group additions under `## ADDED Requirements`, edits under `## MODIFIED Requirements`, removals under `## REMOVED Requirements`. Reuse the same `### Requirement:` / `#### Scenario:` structure inside each group. A delta file MAY add a brand-new capability — in that case all of its content goes under `## ADDED Requirements`.

## After a change ships

Promote the deltas into `openspec/specs/<capability>/spec.md` (folding ADDED/MODIFIED into the active spec, deleting REMOVED requirements) and delete the `changes/<change-id>/` folder.
