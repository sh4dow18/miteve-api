---
name: performance-jpa
description: Use when fixing N+1, adding FK/search indexes, keeping pagination in DB, and gating new indexes on EXPLAIN in miteve-api.
license: MIT
compatibility: opencode
---

# Performance JPA — Single Responsibility: query efficiency only

This skill covers ONLY fetch strategy + índices + paginación DB en `repositories/*` y `entities/*`. It does NOT cover lógica de negocio, docs, ni tests de contrato.

## Rules

- N+1 primero: toda colección/EAGER sospechosa (`Content→episodes|seasons|genres`, `Episode→season`, `Comment→profile`) se corrige con `JOIN FETCH` o `@EntityGraph` en `repositories/*`. Correcto: `@EntityGraph(attributePaths=["episodes","genres"]) fun findWithDetailsById(id: String): Content?`. Incorrecto: `findAll().map { it.episodes.size }` en `services/*` (N+1 + memoria).
- Índices FK/búsqueda: FK (`content_id`, `season_id`, `profile_id`, `episode_id`) + columnas `search/sort` (`title`, `createdAt`) vía Flyway/Liquibase (`db-migrations`), nunca `@Table(indexes=...)` silencioso sin migración. Ejemplo: `CREATE INDEX idx_episode_content_id ON episode(content_id); CREATE INDEX idx_content_title ON content(title);`.
- Paginación en DB, no en memoria: todo listado usa `Pageable` en `repositories/*` (`findByTitleContainingIgnoreCase(..., pageable)`); prohibido `findAll()` + `.drop().take()` o `.filter{}` en `services/*`. Referencia: `ContentController.kt:26-29` (`page/size`) debe llegar como `PageRequest` hasta JPA (`limit ? offset ?` en `show-sql` dev).
- EXPLAIN antes de índice nuevo: `EXPLAIN ANALYZE SELECT ...` con el query real (copiado de `show-sql`) antes y después; solo crear índice si `Seq Scan → Index Scan` y el costo baja. Guardar el plan en la descripción del PR (ver `pr-description`).
- `show-sql` solo dev: `spring.jpa.show-sql=true` / `format_sql` jamás en `prod`; en prod solo métricas de `utils` (ver `observability-logs`). Sin `SELECT *` innecesario: proyecciones/DTOs (`dtos/*` + `mappers/*`) para listados, entidad completa solo en detalle.

## Ordering

- Reproducir con `show-sql` dev → `EXPLAIN ANALYZE` → `JOIN FETCH/@EntityGraph` o índice vía migración → re-medir → `Pageable` hasta `controllers/*` → PR con planes antes/después.

## Naming

- Métodos repo `findWithDetailsById`, `findByTitleContainingIgnoreCase`. Índices `idx_<table>_<column>` (`idx_episode_content_id`). Migraciones `V<nn>__add_idx_<table>_<column>.sql`.

## Verificación

- `show-sql` dev: detalle `GET new-api/contents/{id}` = 1-2 queries (no N+1); lista `GET new-api/contents?page=0&size=20` con `limit 20 offset 0`.
- `EXPLAIN` usa el índice nuevo (`Index Scan using idx_...`); `./gradlew test` (`@DataJpaTest` conteo de queries o asserts de página) en verde.
- `grep -rn "findAll()" services/ | grep -v "Pageable\|Page"` vacío en rutas listadas; `grep -rn "show-sql=true" src/main/resources/application*.properties` solo en perfil dev.

## Out of scope

- Do NOT escribir la migración base aquí. See `db-migrations`.
- Do NOT cambiar el contrato `page/size/sort` aquí. See `pagination-filtering`.
- Do NOT mapear DTOs aquí salvo proyección. See `mapper-pattern` / `entity-pattern`.
- Do NOT loguear queries en prod. See `observability-logs`.
- Do NOT redefinir entidades aquí. See `entity-pattern`.
