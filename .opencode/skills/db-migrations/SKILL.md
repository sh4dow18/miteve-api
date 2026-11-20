---
name: db-migrations
description: Use when evolving the miteve-api database to move off ddl-auto update toward versioned Flyway or Liquibase migrations safely.
license: MIT
compatibility: opencode
---

# DB Migrations — versioned schema evolution only

This skill covers ONLY how schema changes reach the DB. It does NOT cover entity shape detail, queries, or secrets.

## Rules

- Current risk: `application.properties:2 spring.jpa.hibernate.ddl-auto = update` auto-mutates schema on boot, `show-sql=true` (`:11`) logs SQL. No Flyway/Liquibase dependency in `build.gradle.kts`. No migration history table. Treat every entity change as potentially destructive until versioned migrations land.
- Target: Flyway (or Liquibase) versioned migrations: `V1__baseline.sql`, `V2__...sql` under `src/main/resources/db/migration/`, `ddl-auto=validate` (never `update`/`create-drop` outside scratch), `show-sql=false` in prod.
- Until the target lands: devs NEVER run DDL against shared/prod DBs. Propose entity change in issue + PR and flag `requires DB migration`. Only the DB-responsible applies manual DDL after backup.
- Baseline first: snapshot prod schema (pg_dump schema-only) before enabling Flyway `baseline-on-migrate`; never let Flyway invent history on a dirty DB.
- Each migration: one version, one concern, reversible note (down script or restore step), reviewed SQL with explicit types/constraints/indexes matching `entities/*` (`@Table`, `@Column(length)`, `@JoinColumn(nullable=false)`).
- Backup before any shared-DB change: `pg_dump` full + verify restore path; record dump id in PR. No backup = no migrate.
- Local iteration: use a throwaway local Postgres (`DATABASE_IP/DATABASE` pointing at localhost). `ddl-auto=update` acceptable ONLY on scratch-local; never as the migration story for shared envs.

## Ordering

- `V__` numeric prefix order = execution order. Name `<date>-<entity>-<change>`: `V3__content_add_note.sql`. Keep `entities/` + migration + `repositories/` query in the same PR so code never drifts from schema.

## Naming

- Tables/columns `snake_case` in SQL (`contents`, `content_genre`, `type_id`, `created_date`); entities stay `PascalCase`/`camelCase` with mapping annotations. See `entity-pattern`.

## Verificación

- `./gradlew build` passes with `ddl-auto=validate` against migrated scratch DB; `flyway info` (once added) shows no pending; app boots without Hibernate altering schema (no `alter table` in logs).
- PR contains: SQL file(s), backup evidence, rollback note, `requires DB migration` flag cleared only by DB-responsible.

## Out of scope

- Do NOT define entity field shapes here. See `entity-pattern`.
- Do NOT write JPQL here. See `repository-pattern`.
- Do NOT put secrets in migration files. See `conn-secrets`.
