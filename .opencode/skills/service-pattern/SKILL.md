---
name: service-pattern
description: Use when adding business logic in miteve-api services/ for XService interface plus AbstractXService implementation with DI.
license: MIT
compatibility: opencode
---

# Service Pattern — Single Responsibility: service layer only

This skill covers ONLY `src/main/kotlin/sh4dow18/miteve_api/services/*/{XService,AbstractXService}.kt`. It does NOT cover repositories, HTTP, or SQL.

## Rules

- New logic as `XService` interface + `AbstractXService : XService` impl in `services/<domain>/`. Reference: `services/content/ContentService.kt:14-31` declares `findAll/findById/insert/update/...`; `services/content/AbstractContentService.kt:29-56` implements with constructor DI.
- DI via primary constructor (`val contentRepository: ContentRepository, val contentMapper: ContentMapper, ...`). `@Service` on impl only, `@Transactional` on mutating methods (`insert`, `insertEpisodes`, `update` in `AbstractContentService.kt:164-222`). No field injection beyond existing `@Autowired val` constructor style; no `new` of repositories.
- Services call repositories + mappers, never `DbContext`/`EntityManager`/raw SQL/`JdbcTemplate` directly. Allowed: validation, orchestration of multiple repos, slug/position normalization (`toSlug`, `renumberContainerPositions`), `java.time` for dates, `errors/*` for failures (`AlreadyExists`, `NoExists`, `BadRequest`).
- Throw domain errors, do not swallow: `orElseThrow { NoExists(id, "Content") }`, `throw AlreadyExists(title, "Content")`, `throw BadRequest("Seasons List cannot be zero")`. Central mapping lives in `ErrorHandler.kt`. See `exception-handling`.
- No HTTP access: no `RestTemplate`/`WebClient`/servlet types in domain services. External I/O belongs to `services/scrape/*`, `services/ffmpeg/*`, `services/upload/*` behind interfaces — still no controller types.
- Dependency direction: `controllers -> services -> repositories -> entities`. Services never reference `controllers`. DTOs (`dtos/*`) only at signature boundary; entities never leak past service return without mapper.
- Keep `src/test` gap in mind: `src/test/.../MiteveApiApplicationTests.kt` is near-empty — do not claim coverage that does not exist.

## Ordering

- Interface: `package` → DTO imports → `// X Service Interface` comment → `interface XService` with reads first, writes last. Impl: `package` → Spring imports → DTO/entity/error/mapper/repo imports → `// Spring Abstract X Service` → `@Service class AbstractXService(...) : XService` → private helpers (`toSlug`, `renumberContainerPositions`) then overrides in interface order.

## Naming

- Interface `XService`, impl `AbstractXService`, files match. Methods `findAll/findById/findByX/insert/update/delete`. Private helpers `camelCase` verbs (`renumberContainerPositions`).

## Verificación

- `./gradlew build` passes; every new public method reachable from a thin controller or job; no `EntityManager`/SQL string in `services/` (`grep -r EntityManager services/` empty for new code).
- Cross-check with `srp-audit` (no repo-owned query logic) and `backend-separation`.

## Out of scope

- Do NOT write JPQL/SQL here. See `repository-pattern`.
- Do NOT expose REST here. See `controller-pattern`.
- Do NOT map fields manually here. See `mapper-pattern`.
- Do NOT define error HTTP mapping here. See `exception-handling`.
