---
name: docker-local-env
description: Use when running miteve-api locally with docker-compose Postgres, env vars, seed profiles, and health checks.
license: MIT
compatibility: opencode
---

# Docker Local Env — Single Responsibility: reproducible local runtime only

This skill covers ONLY `docker-compose.yml` + `.env` + perfiles Spring + health check. It does NOT cover migrations, business logic, or production deploy.

## Rules

- Compose: servicio `postgres` imagen `postgres:16` (driver `42.7.10` en `build.gradle.kts`), volumen `pgdata`, `healthcheck: pg_isready`. Servicio `api` con `depends_on: postgres: condition: service_healthy`. Puertos fijos documentados, nunca `latest` sin pin.
- Env obligatorias: `USER`, `DATABASE_IP`, `DATABASE`, `PASSWORD`, `PORT` (nombres exactos que usa `application.properties`). Ejemplo `.env.example`: `USER=miteve\nDATABASE_IP=localhost\nDATABASE=miteve\nPASSWORD=changeme\nPORT=5432`. Correcto: `spring.datasource.url=jdbc:postgresql://${DATABASE_IP}:${PORT}/${DATABASE}`. Incorrecto: hardcodear `localhost:5432/miteve` o commitear `.env` real.
- Perfiles seed: `init` = carga seed inicial; `!init` = arranca sin seed (datos existentes). Ejemplo: `SPRING_PROFILES_ACTIVE=init ./gradlew bootRun` primera vez; `SPRING_PROFILES_ACTIVE=default` después. Nunca correr seed `init` sobre DB con datos reales sin backup.
- Health: `controllers/UtilsController.kt` → `GET new-api/utils/health` debe responder `200 {"status":"UP"}` con DB reachable. Compose `api.healthcheck: curl -f http://localhost:8080/new-api/utils/health`. Si cae, revisar `docker compose logs postgres` antes de tocar código.
- Sin secretos en git: `.env` en `.gitignore`; solo `.env.example` con placeholders. Conexiones SSH/TMDB/JWT van por env — ver `config-pattern` y `conn-secrets`.

## Ordering

- `.env.example` → `docker-compose.yml` (postgres → api) → `application.properties` env bindings → `docker compose up -d postgres` → `SPRING_PROFILES_ACTIVE=init bootRun` → `GET new-api/utils/health`.

## Naming

- Servicios `postgres`, `api`. Volúmenes `pgdata`. Perfiles `init`, `default`. Archivo `docker-compose.yml` en raíz, `.env.example` junto a él.

## Verificación

- `docker compose ps` → `postgres healthy`; `curl localhost:8080/new-api/utils/health` → `200 UP`.
- `docker compose logs postgres | grep "ready to accept"` presente; `./gradlew test` pasa contra la misma versión Postgres.
- `git status --porcelain | grep "^\?\? .env$"` vacío (ningún `.env` real trackeado); `git check-ignore .env` lo confirma.

## Out of scope

- Do NOT write migrations here. See `db-migrations`.
- Do NOT define app config keys here beyond env binding. See `config-pattern`.
- Do NOT store real secrets here. See `conn-secrets`.
- Do NOT add endpoint logic to health. See `controller-pattern`.
- Do NOT tune queries aquí. See `performance-jpa`.
