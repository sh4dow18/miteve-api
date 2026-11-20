---
name: backend-separation
description: Use when writing backend code in miteve-api to keep repositories as database-only and services as business-logic-only calling repositories.
license: MIT
compatibility: opencode
---

# Backend Separation — repo vs service vs controller vs mapper boundary only

This skill covers ONLY the separation rule. It does NOT cover formatting, secrets, or REST exposure detail.

## Rules

- Repositories (`repositories/`, `XRepository`): ONLY database via Spring Data JPA. Allowed: derived finders, JPQL `@Query`, `Pageable`. Forbidden: validation, status transitions, permission checks, cross-service calls, `HttpClient`/`RestTemplate`, UI strings, DTO construction.
- Services (`services/<domain>/`, `XService` + `AbstractXService`): ONLY business logic. Must call repositories + mappers, never `EntityManager`/SQL directly. Allowed: validation, orchestration of multiple repos, `java.time` for dates, `errors/*` for failures, mapper calls for DTOs. Example: `AbstractContentService.insert()` orchestrates `contentRepository + genreRepository + containerRepository + contentTypeRepository + mappers` before save.
- Controllers (`controllers/`, `XController`): ONLY HTTP. Allowed: route mapping, `@Valid`, pagination params, call one service, return result. Forbidden: business rules, SQL, repository/mapper access.
- Mappers (`mappers/`, `XMapper`): ONLY mapping entity ↔ DTO. Forbidden: business rules, repository/service calls, HTTP.
- Direction: `sh4dow18.miteve_api.controllers -> services -> repositories -> entities`. `services` never reference `controllers`. `repositories` never reference `services`. Entities/DTOs carry no logic. Reference chain: `ContentController -> ContentService/AbstractContentService -> ContentRepository/... -> Content` + `ContentMapper`.
- Violation examples to fix: SQL/`EntityManager` in service → move to repo method; `if (status != ...)` in repo → move to service; service creating `RestTemplate` for domain logic → move to `services/scrape|ffmpeg|upload` integration service; manual entity→DTO copy in service → move to `XMapper`; repository returning DTO → return entity + map in service.
- Modern reference: `content` module (`ContentController`, `ContentService`, `AbstractContentService`, `ContentRepository`, `ContentMapper`, `Content`). No legacy exceptions — every domain follows the same `XService/AbstractXService` shape.

## Verificación

- New code passes: controller imports only service + DTOs; service imports repos + mappers + errors; repo imports only Spring Data + entity; mapper imports only MapStruct + DTO + entity.
- `./gradlew build` passes; `srp-audit` shows no misplaced function.

## Out of scope

- Do NOT define entity shape here. See `entity-pattern`.
- Do NOT define formatting here. See `kotlin-format`.
- Do NOT expose REST detail here. See `controller-pattern`.
- Do NOT handle errors implementation here. See `exception-handling`.
- Do NOT define mapping annotations here. See `mapper-pattern`.
