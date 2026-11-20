---
name: issue-flow
description: Use when triaging miteve-api issues to pick bug vs feature vs task templates and route to branch, scope, and DB-migration flag.
license: MIT
compatibility: opencode
---

# Issue Flow — triage and routing only

This skill covers ONLY deciding which issue template applies and what metadata the issue must carry. It does NOT write code or file the issue.

## Rules

- Input is a title (+ optional body). Classify first, then delegate text to exactly one writer skill:
  - Crash/wrong data/regression on existing `new-api/` behavior → `issue-bug`.
  - New endpoint/field/job/capability → `issue-feature`.
  - Chore/refactor/version/migration/cleanup/QA without user-facing change → `issue-task`.
- Every triaged issue must state: base branch `develop`, expected branch `Tipo/<issue>-desc` per `git-flow`, scope (`content`, `auth`, `scrape`, `ffmpeg`, `upload`, `mapper`, `security`), touched endpoints under `new-api/`, entities + `XRepository` + `XMapper` + `XService`, profile (`init`/`!init`), and `requires DB migration: yes/no`.
- If the title implies schema change (new field/table/relation/constraint), set `requires DB migration: yes` and route DDL to `db-migrations` (DB-responsible only). If it implies secrets/hosts, route to `conn-secrets` and strip values from the issue.
- If auth/permissions implied (login, JWT, roles, privileges, CORS, `ip.domain`), tag scope `auth|security` and reference `auth-server`; require profile + role + token-state in repro.
- If scrape/ffmpeg/upload implied, tag scope accordingly and reference `scrape-upload-ffmpeg`; require provider/job idempotency note.
- Output is the routing decision (template + scope + branch suggestion + flags), then invoke the matching `issue-*` skill for the copyable body. No code, no GitHub API calls.
- Never include secrets, tokens, passwords, or IPs in the triage output.

## Verificación

- Template choice justified in one line; `requires DB migration` explicitly answered; endpoints use `new-api/` prefix, never bare paths.

## Out of scope

- Do NOT write the full body here — delegate to `issue-bug`, `issue-feature`, or `issue-task`.
- Do NOT decide PR body here. See `pr-description`.
- Do NOT bump versions here. See `version-tag`.
