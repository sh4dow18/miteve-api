---
name: audit-trail
description: Use when recording who-did-what-when in miteve-api via History, ContinueWatching, and BugReportStatus with JWT actor and minimal overhead.
license: MIT
compatibility: opencode
---

# Audit Trail — Single Responsibility: who-did-what-when only

This skill covers ONLY `entities/History.kt`, `entities/ContinueWatching.kt`, `entities/BugReportStatus.kt` + sus `services/*`, `repositories/*`, `mappers/*`. It does NOT cover logs técnicos, métricas, ni auth internals.

## Rules

- Qué se audita: `History` (reproducciones/acciones por perfil), `ContinueWatching` (progreso `profileId + contentId/episodeId + position`), `BugReportStatus` (transiciones `OPEN→IN_PROGRESS→RESOLVED`). Cada fila guarda `actor` (userId/profileId desde JWT), `action`, `entityId`, `createdAt`. Sin actor = auditoría inútil — rechazar con `401/400` vía `errors/*` antes de persistir.
- Actor desde JWT: `SecurityContextHolder.getAuthentication()` en `services/history/*`, `services/continue_watching/*`, `services/bug_report/*`; nunca `userId` confiable desde body/query. Correcto: `val actor = auth.name; historyService.record(actor, ...)`. Incorrecto: `record(@RequestParam userId)` sin contrastar con JWT (spoofing).
- `ContinueWatching` es upsert por (`profileId`, `episodeId`): `findByProfileIdAndEpisodeId` → update `position` o insert. `History` es append-only (sin `update/delete` salvo GDPR). `BugReportStatus` solo avanza por transiciones válidas (`BugReportMapper.kt` + guard en `BugReportService`); transición ilegal → `400` `errors/BadRequest.kt`.
- Sin sobrecarga: escritura async (`@Async` o evento) o batch cuando sea alta frecuencia (latidos de progreso cada N s, no por frame); índices en (`profileId`, `createdAt`) y (`profileId`, `episodeId`); paginación obligatoria en `controllers/HistoryController.kt` y `ContinueWatchingController.kt` (`page=0 size=20`, ver `pagination-filtering`).
- Minimización: guardar ids + timestamps + `action` enum, nunca cuerpos `dtos/*` enteros ni PII (email, IP cruda). Retención documentada (ej. `History 12m`, progreso hasta borrado de perfil).

## Ordering

- `controllers/*` extrae JWT → `services/*` (`History|ContinueWatching|BugReportService`) valida actor + transición → `mappers/*` a `entities/*` → `repositories/*` persiste (upsert o append) → respuesta `201/200` sin eco de PII.

## Naming

- Entidades `History`, `ContinueWatching`, `BugReportStatus`. Métodos `record()`, `updateProgress()`, `transitionTo()`. Campos `actorId/profileId`, `action`, `entityId`, `createdAt`. Acciones `PLAY|PAUSE|COMPLETE|REPORT|STATUS_CHANGE`.

## Verificación

- Como `QA_user` con JWT: `POST new-api/history` crea fila con `actorId=QA_user`; `PUT new-api/continue-watching` dos veces = 1 fila actualizada; transición ilegal de bug → `400`.
- `SELECT actor_id,action,created_at FROM history ORDER BY created_at DESC LIMIT 5` muestra actor real, sin emails; `./gradlew test` de upsert + guard de transición en verde.
- Sin JWT → `401` (vía `config/SecurityConfig.kt`); con `userId` ajeno en body → `400/403`, nunca escribe.

## Out of scope

- Do NOT loguear técnico aquí. See `observability-logs`.
- Do NOT tocar reglas de auth/JWT aquí. See `auth-server`.
- Do NOT paginar fuera del estándar. See `pagination-filtering`.
- Do NOT optimizar queries fuera de índices de auditoría. See `performance-jpa`.
- Do NOT exponer PII en `dtos/*` de auditoría. See `mapper-pattern`.
