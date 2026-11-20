---
name: issue-bug
description: Use when asked to generate a GitHub issue from a title using the bug template in miteve-api to output copyable markdown with only those fields.
license: MIT
compatibility: opencode
---

# Issue Bug — markdown per bug template only

This skill covers ONLY issue text generation for bugs. It does NOT change code, create branches, or file the issue.

## Rules

- Input is only a task title. Infer reasonable content from the title plus repo context; never invent unrelated scope.
- Output copyable markdown with EXACTLY these fields, in this order:
  `## Descripción`, `## Pasos para reproducir`, `## Comportamiento esperado`, `## Comportamiento actual`, `## Endpoint / Entidad`, `## Perfil y auth`, `## Reportado por`, `## Evidencia`.
- No extra sections, no intro/outro text outside the markdown block.
- `Pasos para reproducir` as numbered list starting with `1. bootRun con perfil ...`, including method + path under `new-api/` (e.g. `GET new-api/contents/{id}`), `page/size` if paged, and request body shape (`ContentRequest`, `SeasonRequest`) when relevant.
- `Endpoint / Entidad`: exact endpoint (`${endpoint.contents}` → `new-api/contents`), entity (`Content`, `Season`, `Episode`, `Container`), mapper (`ContentMapper`) if mapping suspected; `Perfil y auth`: `init` vs `!init`, JWT valid/expired/missing, role/privilege involved.
- `Reportado por`: leave unchecked unless title says otherwise. `Evidencia`: `_Agregar request/response + logs_` if none provided.
- All text in Spanish. API level (endpoints, entities, MapStruct, scrape/ffmpeg/upload job), no Blazor/EF terms, no method internals beyond names needed to repro.
- Never include secrets, connection strings, SSH passwords, JWTs, or IPs.

## Out of scope

- Do NOT commit/push or call GitHub API. Only output markdown.
- Do NOT decide branch names here. See `git-flow`.
