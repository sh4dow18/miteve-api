---
name: pr-description
description: Use when asked for a pull request description in miteve-api to output copyable markdown per project template targeting develop.
license: MIT
compatibility: opencode
---

# PR Description — markdown per template, process level only

This skill covers ONLY PR text generation. It does NOT change code, run builds, or decide branch names.

## Rules

- Base diff on current branch vs `develop` (`git status`, `git diff`, `git log --oneline -10`). Never invent changes.
- Output copyable markdown with EXACTLY these sections, in this order:
  `# Pull Request`, `## Historia de Usuario (Como/Quiero/Para)`, `## Descripcion`, `## Cambios realizados`, `## Como probar`, `## Criterios de Aceptacion`, `## Capturas (si aplica)`.
- Process + API level: what the caller can now do and how the flow works (endpoints under `new-api/`, entities, pagination, auth profile, scrape/ffmpeg/upload job outcome). Reference endpoint paths + DTO names + HTTP codes (`200/400/404/409/503`) where they help testing; no method internals, no SQL dumps, no full file lists.
- `Como probar` must be executable: profile (`init`/`!init`), env vars needed (names only, never values), `./gradlew build`, `bootRun`, curl/RestAssured calls (`GET new-api/contents?page=0&size=20`, `POST new-api/login`), expected codes, and `requires DB migration` + backup note when schema changed.
- Target is `develop` unless `develop->main` or `main->release/*` checkpoint. Title rule lives in `git-flow` (`Tipo/<issue>-desc`); this skill only writes the body.
- If branch is `Docs/*`, body accepts only `docs:`-scoped changes.
- Never include secrets, tokens, SSH passwords, connection strings, or IPs.

## Out of scope

- Do NOT commit/push here. Only output markdown.
- Do NOT bump versions here. See `version-tag`.
