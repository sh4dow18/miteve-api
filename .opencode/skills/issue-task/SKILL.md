---
name: issue-task
description: Use when asked to generate a GitHub issue from a title using the task template in miteve-api to output copyable markdown with only those fields.
license: MIT
compatibility: opencode
---

# Issue Task — markdown per task template only

This skill covers ONLY issue text generation for general tasks. It does NOT change code, create branches, or file the issue.

## Rules

- Input is only a task title. Infer reasonable content from the title plus repo context; never invent unrelated scope.
- Output copyable markdown with EXACTLY these fields, in this order:
  `## Descripción`, `## Tareas`, `## Alcance API`, `## Reportado por`.
- No extra sections, no intro/outro text outside the markdown block.
- `Descripción`: exactly one single paragraph, no line breaks or lists.
- `Tareas` as checklist `- [ ] ...` derived from the title (concrete API steps: entity → repository → mapper → service → controller → `./gradlew build`).
- `Alcance API`: endpoints under `new-api/`, entities, `XService/AbstractXService`, `XRepository`, `XMapper`, `ErrorHandler` code if relevant, profile (`init`/`!init`), and `requires DB migration` flag when schema is touched.
- `Reportado por`: leave unchecked unless title says otherwise.
- All text in Spanish. API level, no C#/Blazor/EF terms.
- Never include secrets, connection strings, SSH passwords, JWTs, or IPs.

## Out of scope

- Do NOT commit/push or call GitHub API. Only output markdown.
- Do NOT decide branch names here. See `git-flow`.
