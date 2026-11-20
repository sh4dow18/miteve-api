---
name: repository-pattern
description: Use when adding data access in miteve-api repositories/ for XRepository Spring Data JPA without business logic.
license: MIT
compatibility: opencode
---

# Repository Pattern — Single Responsibility: repository layer only

This skill covers ONLY `src/main/kotlin/sh4dow18/miteve_api/repositories/*Repository.kt` with Spring Data JPA. It does NOT cover services, DDL/migrations, or connection secrets.

## Rules

- New access as `XRepository : JpaRepository<Entity, Id>` in `repositories/`, annotated `@Repository`. No `I` prefix. Reference: `repositories/ContentRepository.kt:11-12 interface ContentRepository : JpaRepository<Content, String>`.
- Prefer derived query methods: `existsByTmdbId(tmdbId: Long)`, `findByTmdbId(tmdbId: Long)`, `findTop10ByComingSoonFalseOrderByCreatedDateDesc()`, `findByTitleContainingIgnoreCase(title, pageable)` (`repositories/ContentRepository.kt:13-24`).
- `@Query` JPQL only when derived names cannot express it (joins, OR, NOT IN). Justify in PR. Reference approved JPQL: `findComingSoon()`, `findSimilarContent(...)`, `findRecommendedContent(...)` with named `@Param` (`repositories/ContentRepository.kt:16-58`).
- No native SQL unless justified and reviewed; never string-concatenated SQL. Pagination via `Pageable`/`Page<T>` parameters, never manual LIMIT strings.
- No business logic: no validation, no status transitions, no permission checks, no cross-service calls, no `HttpClient`/`RestTemplate`, no UI strings.
- No DDL. `spring.jpa.hibernate.ddl-auto = update` is current risk (`application.properties:2`) — migration to Flyway is DB-owner only. Devs propose entity/repo change in issue + PR and flag `requires DB migration`. Never run DDL against shared/prod DBs. See `db-migrations`.
- Return `Optional<T>`/`Page<T>`/`List<T>`; let service map to DTOs via mapper. Do not return DTOs from repositories.

## Ordering

- `package` → Spring Data imports → entity import → `@Repository` → `interface XRepository` → derived finders first, then `@Query` methods grouped by feature.

## Naming

- Interface `XRepository`, file `XRepository.kt`. Methods `findBy*`, `existsBy*`, `countBy*`, `findTopNBy*OrderBy*`. Params `camelCase` with `@Param("name")` matching JPQL `:name`.

## Verificación

- `./gradlew build` passes; `show-sql=true` (`application.properties:11`) used only for local inspection, never committed as debug logging.
- Service still compiles against new method; no service logic leaked into repo (check with `srp-audit`).

## Out of scope

- Do NOT define entity shape here. See `entity-pattern`.
- Do NOT implement business rules here. See `service-pattern`.
- Do NOT map entities to DTOs here. See `mapper-pattern`.
- Do NOT decode secrets or edit `application.properties` here. See `conn-secrets`.
