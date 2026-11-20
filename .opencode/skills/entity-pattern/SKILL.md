---
name: entity-pattern
description: Use when creating or editing JPA entities in miteve-api entities/ for @Entity/@Table/@Column shape with correct nullability.
license: MIT
compatibility: opencode
---

# Entity Pattern — Single Responsibility: JPA entity shape only

This skill covers ONLY `src/main/kotlin/sh4dow18/miteve_api/entities/*.kt`. It does NOT cover repositories, services, mappers, or DDL execution.

## Rules

- Location: `src/main/kotlin/sh4dow18/miteve_api/entities/`. One class per file. File name matches class: `Content.kt` holds `Content`, `Privilege.kt` holds `Privilege`.
- Class `PascalCase` friendly English: `Content`, `Season`, `Episode`, `ContinueWatching`, `BugReport`, `SuggestedContentReport`. Never DB-snake as class name.
- Annotations: `@Entity` + `@Table(name = "contents")` on class. `@Column(...)` only when DB column differs or length needed: `@Column(length = 1000) var description: String?` (`entities/Content.kt:25`). `@Id` for PK; `@GeneratedValue(IDENTITY)` for numeric PKs (`entities/Privilege.kt:14-15`).
- Relationships explicit: `@ManyToMany` + `@JoinTable` for `Content.genresList` (`entities/Content.kt:38-44`), `@ManyToOne` + `@JoinColumn(name = "type_id", nullable = false)` (`entities/Content.kt:47-49`), `@OneToMany(mappedBy = ...)` for `seasonsList`, `continueWatchingList`, `commentsList`.
- Nullability must match DB: nullable columns use `String?`, `Int?`, `ZonedDateTime?` (e.g. `tagline: String?`, `endTime: Int?`, `containerElement: ContainerElement?`). Required fields non-nullable (`title: String`, `rating: Float`).
- Collections naming: `genresList: Set<Genre>`, `seasonsList: List<Season>` — `List` suffix per `kotlin-format`.
- `equals`/`hashCode` by id only when entity is `data class` with relations (see `entities/Content.kt:57-66`, `entities/Privilege.kt:22-31`). Do not include relation fields in equality.
- Timestamps as `java.time.ZonedDateTime` (`createdDate: ZonedDateTime`). No `java.util.Date` in entities.
- DB schema change: no local DDL. `application.properties:2 spring.jpa.hibernate.ddl-auto = update` is risk — propose change in issue + PR and flag `requires DB migration`. Never run DDL against shared/prod DBs. See `db-migrations`.

## Ordering

- `package` → `jakarta.persistence` imports → `java.time` imports → `// X Entity` comment → `@Entity/@Table` → `data class`/`class` with PK first, scalars, then relations.

## Naming

- Properties `camelCase` friendly: `tmdbId`, `trailerDuration`, `comingSoon`, `createdDate`. DB snake lives only in `@Table`/`@JoinColumn(name = ...)`, never as property name.

## Verificación

- `./gradlew build` passes (kapt + `allOpen` for `@Entity` via `build.gradle.kts:49-53`).
- No business logic, no repository call, no mapper call inside entity file.

## Out of scope

- Do NOT write queries here. See `repository-pattern`.
- Do NOT put business logic here. See `service-pattern`.
- Do NOT map to DTOs here. See `mapper-pattern`.
- Do NOT run migrations here. See `db-migrations`.
