---
name: gradle-build-run
description: Use when building or running miteve-api locally with Gradle wrapper, Java 24 toolchain, kapt, and Postgres env.
license: MIT
compatibility: opencode
---

# Gradle Build Run — Single Responsibility: local build and run only

This skill covers ONLY restore/build/run/test. It does NOT cover migrations, deployment, or secrets content.

## Rules

- Toolchain: Java 24 via `build.gradle.kts:14-18 toolchain { languageVersion = JavaLanguageVersion.of(24) }`. Kotlin `2.2.21` JVM + `plugin.spring` + `plugin.jpa` + `kapt`, Spring Boot `4.0.3` (`build.gradle.kts:1-8`). Do not downgrade.
- Build: `./gradlew build` (Windows: `.\gradlew.bat build`). This runs kapt (MapStruct `1.6.3` processor, `build.gradle.kts:40`) + tests. Repeat until zero errors.
- Run: `./gradlew bootRun` (or `.\gradlew.bat bootRun`). Port from `server.port=${PORT}` (`application.properties:14`); base path `${access}=new-api` (`application.properties:13`), e.g. `http://localhost:${PORT}/new-api/contents`.
- Test: `./gradlew test`. `src/test` is near-empty (`MiteveApiApplicationTests.kt` only) + `webmvc-test`/`security-test` deps exist — do not invent missing suites; verify with build + test only.
- Profiles: `init` = closed security (`CloseSecurityConfiguration`); `!init` = JWT stateless. Pass via `SPRING_PROFILES_ACTIVE` env or `--args='--spring.profiles.active=...'`. Document which profile the run used in PR/issue repro.
- Postgres local required: `spring.datasource.url=jdbc:postgresql://${DATABASE_IP}:5432/${DATABASE}`, `username=${USER}`, `password=${PASSWORD}` (`application.properties:4-9`), driver `postgresql:42.7.10`. No embedded H2 — do not add one to "fix" a missing DB.
- kapt note: after mapper/DTO/entity changes, prefer clean `./gradlew clean build` if generated impls look stale (`build/generated/source/kapt/`).

## Verificación

- `./gradlew build` → BUILD SUCCESSFUL; `./gradlew bootRun` → app up; `HEAD new-api/utils/health` (see `controllers/UtilsController.kt:12-13`) returns success.
- Keep `show-sql=true` (`application.properties:11`) local-only observation; do not silence globally to hide a failure.

## Out of scope

- Do NOT run DDL/migrations. See `db-migrations` (Flyway owner only).
- Do NOT change ports without syncing `application.properties:14`, `SecurityConfig` CORS, and controller docs.
- Do NOT commit secrets here. See `conn-secrets`.
