---
name: kotlin-format
description: Use when remediating any .kt file in miteve-api (e.g. ContentService.kt) to full readability and maintainability standards.
license: MIT
compatibility: opencode
---

# Kotlin Format — .kt remediation workflow

Self-contained checklist for `.kt` cleanup. Do NOT change business logic, only apply the standards below. Goal: any dev can read and maintain it.
For isolated tasks prefer atomic skills: `service-pattern`, `repository-pattern`, `entity-pattern`, `mapper-pattern`, `controller-pattern` for design, `exception-handling` for errors, `gradle-build-run` for builds.

## 1. File order and sections

- Order top to bottom: `package`, imports (java/kotlinx, then third-party, then `sh4dow18.miteve_api`), constants/objects, primary constructor + properties, public functions, private functions, `companion object` last.
- No blank lines between related property declarations in entities; one blank line between functions.
- Section headers as comments only where the file demands it: `// Content Service Interface ...`, `// Spring Abstract Content Service`. Do NOT comment variable-by-variable.
- Example header: `src/main/kotlin/sh4dow18/miteve_api/services/content/ContentService.kt:12` interface comment, `AbstractContentService.kt:27` impl comment.

## 2. Naming and declarations

- All code and comments in English.
- Variables/funcs `camelCase`: `contentRequest`, `findByTmdbId`, `toSlug`. Classes/interfaces `PascalCase`: `ContentService`, `AbstractContentService`, `ContentMapper`.
- No `var` when `val` suffices. Prefer `val pageable = PageRequest.of(...)` (see `AbstractContentService.kt:68`).
- Never use `!!`. Narrow with `?.let`, Elvis `?: throw`, or `orElseThrow { NoExists(...) }`. Correct: `contentRepository.findById(id).orElseThrow { NoExists(id, "Content") }`. Wrong: `findById(id).get()!!`.
- Explicit types on public API: `fun findById(id: String): ContentResponse`. Local inference OK when obvious.
- Lists end with `List` suffix: `genresList`, `seasonsList`, `contentsList`. Sets end with `Set` only via `toSet()` at boundary. Reference: `entities/Content.kt:44 genresList: Set<Genre>`, `mappers/ContentMapper.kt:44 contentsList: List<Content>`.
- One class/interface per file. File name = class name. Example: `services/content/ContentService.kt` holds only `ContentService`; `services/content/AbstractContentService.kt` holds only `AbstractContentService`.
- Use primary constructor with DI: `class AbstractContentService(@Autowired val contentRepository: ContentRepository, ... ) : ContentService` — constructor injection, no field `@Autowired lateinit`.
- Extract inline lambdas/handlers to named private funs. Example: `toSlug(input: String)` in `AbstractContentService.kt:57` is named, not an inline `Normalizer` lambda inside `insert()`.

## 3. Braces, comments, cleanup

- Conditionals and loops always with braces, never oneline or braceless.
- Comments with `//`. Comment non-trivial blocks (slug normalization, container renumbering) line by line. See `AbstractContentService.kt:181-186` position-normalization comments.
- Correct indentation (4 spaces, Kotlin default). No wildcard-star import abuse beyond existing `repositories.*` — prefer explicit imports in new code.
- Dates via `java.time` (`ZonedDateTime.now()`, `DateTimeFormatter`) + injectable clock where testable; never `java.util.Date` in new code except JWT `Date(...)` interop in `config/Security.kt:100`. Exception: explicit interop must comment why.
- Delete unused code. No commented-out blocks.

## 4. Verify

- After applying: check for errors, fix, run `./gradlew build`, repeat until zero errors. See `gradle-build-run`.
- `src/test` is near-empty — do not invent test commands beyond `./gradlew test`.

## Out of scope

- Do NOT define entity shape here. See `entity-pattern`.
- Do NOT define repository queries here. See `repository-pattern`.
- Do NOT define service orchestration here. See `service-pattern`.
- Do NOT define mapper annotations here. See `mapper-pattern`.
- Do NOT define REST shape here. See `controller-pattern`.
- Do NOT define error mapping here. See `exception-handling`.
