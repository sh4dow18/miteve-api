---
name: srp-audit
description: Use when reviewing a .kt file in miteve-api to verify Single Responsibility and relocate misplaced functions to their correct service, repository, mapper, or controller.
license: MIT
compatibility: opencode
---

# SRP Audit — single-file .kt review and relocation

This skill covers ONLY auditing one target `.kt` file for SRP and moving misplaced logic to its correct owner. It does NOT cover formatting, secrets, or REST exposure detail.

Trigger phrases: "revisa si cumple SRP/SPR", "acoplarlo", "esta funcion pertenece a otro archivo", "viola responsabilidad unica".

## 1. Define the owned responsibility

- Derive expected responsibility from file name + class + folder + layer:
  - `services/<domain>/AbstractXService.kt` owns business rules for `{X}` only (e.g. `AbstractContentService` owns content orchestration).
  - `services/<domain>/XService.kt` owns the contract for `{X}` only.
  - `repositories/XRepository.kt` owns persistence for `{X}` only (e.g. `ContentRepository` owns content queries).
  - `mappers/XMapper.kt` owns entity↔DTO mapping for `{X}` only (e.g. `ContentMapper`).
  - `controllers/XController.kt` owns HTTP for `{X}` only (e.g. `ContentController`).
  - `errors/ErrorHandler.kt` owns exception→HTTP mapping only.
- One class/interface per file. File name must match class. If file holds two responsibilities, it already fails SRP.

## 2. Inventory every function

- List all `public`/`private` funs in the target file with signature + 1-line summary of what entity it queries/mutates.
- For each record: primary entity touched, repositories/`@Query`s used, params, whether caller needs the owning entity at all.
- Do not trust method name alone. Read body: JPQL/`JpaRepository` calls, service calls, mapper calls, entity types returned.

## 3. Classify each function

- `native`: operates on owned entity, or orchestrates owned entity with others where owned entity is required input/filter/result.
- `misplaced`: operates on a foreign entity with no need for owned entity. Canonical example: `findGenres()`/`renumberUsers()` living in `AbstractContentService` without any content filter — belongs in `AbstractGenreService`/`AbstractUserService`.
- `mixed`: does both. Split it: keep owned-entity part, extract foreign-entity part to its owner, then compose via service-to-service call if orchestration is genuinely needed.
- Other violation signals in miteve-api:
  - JPQL/`EntityManager`/raw SQL for entity B inside `{A}Repository` where A is never used.
  - Business validation / status transition / permission check inside `repositories/` → belongs in `services/`.
  - `EntityManager` / raw SQL / `RestTemplate` inside domain `services/` → belongs in `repositories/` method or `services/scrape|ffmpeg|upload` integration service.
  - HTTP/servlet types inside `services/` / `repositories/` → belongs in `controllers/` or dedicated integration service.
  - Manual entity→DTO field copy inside service → belongs in `mappers/XMapper`.
  - Repository returning DTO → return entity, map in service.
  - Controller calling repository/mapper directly → call service instead.

## 4. Decide relocation target

- Default target is the owner of the foreign entity: `{Foreign}Service` + `Abstract{Foreign}Service` + `{Foreign}Repository` + `{Foreign}Mapper` in its own domain folder.
- Respect direction `controllers -> services -> repositories -> entities`. `services` never reference `controllers`. `repositories` never reference `services`. DTOs in `dtos/`, entities in `entities/`.
- If target owner does not exist, create `XService` + `AbstractXService` in `services/<domain>/` and/or `XRepository` in `repositories/`, following the `content` module shape (`ContentService`, `AbstractContentService`, `ContentRepository`, `ContentMapper`, `ContentController`).
- DB schema migrations: DB-responsible only. Propose entity/repo change in issue + PR and flag `requires DB migration`. Never run DDL against shared/prod DBs. See `db-migrations`.

## 5. Relocate without duplication

- Move, do not copy: cut misplaced method to target file, delete from source, update package/usings.
- Keep callers working: inject correct service (`GenreService` not `ContentService` for genres) via constructor DI. Controllers call services, never repositories.
- If source still needs the data, call the correct owner (service-to-service via interface), never duplicate the query.
- Delete dead code. No commented-out blocks. No logic change beyond the move plus minimal wiring.
- Errors via `errors/*` (`NoExists`, `AlreadyExists`, `BadRequest`, `ServiceUnavailable`), not raw try/catch + logger. Dates via `java.time`, not `java.util.Date` in new code.

## 6. Verify

- Re-run inventory: source file must contain only `native` methods. Target file must still be single-responsibility.
- Search callers (`grep` for old method name / class) and fix every reference.
- Check for errors, fix, run `./gradlew build` until zero errors. See `gradle-build-run`.

## Out of scope

- Do NOT define repo-vs-service boundary beyond relocation. See `backend-separation`.
- Do NOT define new service scaffolding detail here. See `service-pattern`.
- Do NOT define new repository detail here. See `repository-pattern`.
- Do NOT define mapper annotations here. See `mapper-pattern`.
- Do NOT define Kotlin style here. See `kotlin-format`.
- Do NOT define error mapping here. See `exception-handling`.
- Do NOT expose REST here. See `controller-pattern`.
