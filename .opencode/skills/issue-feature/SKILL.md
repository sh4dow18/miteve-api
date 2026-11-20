---
name: issue-feature
description: Use when asked to generate a GitHub issue from a title using the feature template in miteve-api to output copyable markdown with only those fields.
license: MIT
compatibility: opencode
---

# Issue Feature — markdown per feature template only

This skill covers ONLY issue text generation for new features. It does NOT change code, create branches, or file the issue.

## Rules

- Input is only a task title. Infer reasonable content from the title plus repo context; never invent unrelated scope.
- Output copyable markdown with EXACTLY these fields, in this order:
  `## Historia de Usuario`, `## Descripción`, `## Criterios de Aceptación`, `## Alcance API`, `## Reportado por`, `## Mockups`.
- No extra sections, no intro/outro text outside the markdown block.
- `Historia de Usuario` in format: `**Como** [rol]`, `**Quiero** [funcionalidad]`, `**Para** [beneficio]`.
- `Criterios de Aceptación` as checklist `- [ ] ...` derived from the title (endpoint + status + pagination/auth/mapper behavior, e.g. `- [ ] GET new-api/contents/search pagina con size por defecto 20`).
- `Alcance API`: endpoint(s) under `new-api/`, entities (`Content`, `Season`, `Episode`, `Genre`, `History`), DTOs (`ContentRequest/Response`), mapper (`ContentMapper`), security profile/role, and whether scrape/ffmpeg/upload jobs are touched.
- `Reportado por`: leave unchecked unless title says otherwise. `Mockups`: `_N/A_` if none provided (JSON example counts as mockup for API).
- All text in Spanish. API level, no C#/Blazor/EF terms, no file lists beyond endpoint/entity names.
- Never include secrets, connection strings, SSH passwords, JWTs, or IPs.

## Out of scope

- Do NOT commit/push or call GitHub API. Only output markdown.
- Do NOT decide branch names here. See `git-flow`.
