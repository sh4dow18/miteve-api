---
name: mapper-pattern
description: Use when mapping entities to DTOs in miteve-api mappers/ with MapStruct componentModel spring and MapperUtils.
license: MIT
compatibility: opencode
---

# Mapper Pattern — Single Responsibility: mapper layer only

This skill covers ONLY `src/main/kotlin/sh4dow18/miteve_api/mappers/*Mapper.kt` + `MapperUtils.kt` with MapStruct via kapt. It does NOT cover services, repositories, or HTTP.

## Rules

- One mapper per domain: `ContentMapper`, `SeasonMapper`, `EpisodeMapper`, `UserMapper`, `CommentMapper`, etc. File `XMapper.kt` holds only `XMapper`. Shared constants in `mappers/MapperUtils.kt` (`EMPTY_LIST`, `EMPTY_SET`).
- Annotation: `@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = [...])` on `interface XMapper`. Reference: `mappers/ContentMapper.kt:15-16` with `uses = [ContainerMapper::class]`.
- Entity ↔ DTO only. Allowed: `@Mapping(target=..., expression=...)` for computed fields, `source = "containerElement.container"` nesting, list helpers (`contentsListToContentResponsesList`, `contentsListToMiniContentResponsesList`). Reference: `mappers/ContentMapper.kt:18-56`.
- Null-safe: never `!!` in expressions; guard with ternaries (`content.getContainerElement() != null ? ... : null` in `ContentMapper.kt:33`). Use `EMPTY_LIST`/`EMPTY_SET` for empty collections instead of null.
- No business logic, no repository calls, no service calls, no HTTP. Mapper receives fully-loaded entities/params (`id`, `contentRequest`, `genresList`, `type` in `contentRequestToContent`).
- kapt processor required: `build.gradle.kts:40 kapt("org.mapstruct:mapstruct-processor:1.6.3")`. If mappings do not generate, check kapt + `allOpen` before touching logic.
- DTOs live in `dtos/<domain>/` (`ContentRequest`, `ContentResponse`, `MiniContentResponse`, `ShortContentResponse`). Mapper never invents DTO shapes — service owns the contract.

## Ordering

- `package` → MapStruct imports → DTO imports → entity imports → `// X Mapper` → `@Mapper interface` → request-to-entity first, entity-to-response next, list helpers last (same order as `ContentMapper.kt`).

## Naming

- Mapper `XMapper`, methods `<source>To<Target>`: `contentRequestToContent`, `contentToContentResponse`, `contentsListToMiniContentResponsesList`. Params `contentsList`, `genresList` with `List` suffix.

## Verificación

- `./gradlew build` passes and generated impls appear under `build/generated/source/kapt/`; unmapped-target warnings reviewed (policy is IGNORE — confirm intentional).
- No `!!`, no repo/service import in `mappers/`.

## Out of scope

- Do NOT implement business rules here. See `service-pattern`.
- Do NOT write queries here. See `repository-pattern`.
- Do NOT expose REST here. See `controller-pattern`.
- Do NOT define DTO validation here beyond mapping (validation lives in controller/service with `@Valid`).
