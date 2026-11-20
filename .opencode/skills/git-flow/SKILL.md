---
name: git-flow
description: Use when creating branches, commits, or PRs in miteve-api for branch naming, commit format, and PR targets.
license: MIT
compatibility: opencode
---

# Git Flow — Single Responsibility: git conventions only

This skill covers ONLY branch/commit/PR naming. It does NOT cover version bumping or code style.

## Rules

- Branch from `develop`: `Tipo/<issue>-desc-corta`, types `Feature|Fix|Refactor|Docs` capitalized (e.g. `Feature/42-content-search`, `Fix/57-container-order`). Exempt: `main`, `develop`, `release/*`, `hotfix/*`.
- Commit: `#<issue> tipo(scope): desc` in English lowercase, types `feat|fix|refactor|docs|style|test|chore`, scope mandatory (`content`, `auth`, `scrape`, `ffmpeg`, `upload`, `mapper`, `security`). Examples: `#42 feat(content): add title search paging`, `#57 fix(container): correct position renumber` (never `contenairs`-style typos — spell-check scope). Branch `#` must equal commit `#`. `Docs/*` branches accept only `docs:` commits.
- PR title must equal branch name exactly (except `develop->main`, `main->release/*` checkpoints). Target `develop`. Body via `pr-description` skill (Historia/Descripcion/Cambios/Como probar/Criterios). Base diff on `git diff --name-only origin/develop...HEAD`.
- Merges: `develop` needs 1 approval, `main`/`release/*` needs 2.
- Never commit `build/`, `.gradle/`, `.kotlin/`, local `application-*.properties` edits with real secrets, `.env`, `downloads/`, ffmpeg work-dir artifacts. Verify hooks if configured with `git config --get core.hooksPath`.

## Verificación

- `git status` clean of secrets/build outputs; `git log --oneline -10` shows `#<issue> tipo(scope):` on every row; PR base = `develop`.

## Out of scope

- Do NOT bump versions here. See `version-tag`.
- Do NOT invent issue numbers — read them from GitHub (`issue-flow`).
