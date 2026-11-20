---
name: observability-logs
description: Use when adding structured logs, MDC request IDs, levels, and utils metrics in miteve-api without leaking PII or secrets.
license: MIT
compatibility: opencode
---

# Observability Logs — Single Responsibility: logging and metrics only

This skill covers ONLY log statements + MDC filter + métricas en `controllers/UtilsController.kt`. It does NOT cover business logic, audit legal, or error HTTP codes.

## Rules

- Formato estructurado: `logger.info("module={} action={} id={} status={} ms={}", module, action, id, status, ms)`. Módulos fijos: `scrape`, `ffmpeg`, `upload`, `archive`. Pipeline completo traza `scrape→ffmpeg→upload→archive` con el mismo `requestId`: ej. `ScrapeJobService` loguea `module=scrape action=start`, `FfmpegJobService` `module=ffmpeg action=transcode`, `UploadService` `module=upload action=ssh-put`, `ArchiveController` `module=archive action=verify-dash`.
- MDC `requestId`: filtro `OncePerRequestFilter` que genera/propaga `X-Request-Id` (`MDC.put("requestId", ...)`, `clear()` en `finally`). `controllers/*` nunca generan UUIDs ad-hoc; `services/scrape/*`, `services/ffmpeg/*`, `services/upload/*` solo leen `MDC.get("requestId")`.
- Niveles: `DEBUG` decisiones internas (fallback anime `lamovie→jkanime→...→nyaa`, SQL solo dev con `show-sql`); `INFO` hitos (job started/finished, upload OK); `WARN` retry/backoff recuperable; `ERROR` con stacktrace solo fallo definitivo + `errors/ApiError.kt` code. Nunca `INFO` por fila ni `ERROR` para `404` esperado (`errors/NoExists.kt` → `WARN` máximo).
- Sin PII/secretos: jamás `password`, `TMDB key`, `JWT`, `SSH user/host` con credenciales, `email` completo, ni `dtos/*` con datos sensibles. Correcto: `logger.info("upload ok remotePath={}", sshProps.remotePath)`. Incorrecto: `logger.info("upload {}", sshProps)` o loguear `User` entity entera.
- Métricas utils: contadores/tiempos en `controllers/UtilsController.kt` (`GET new-api/utils/metrics` o `/health` extendido): `jobs_scrape_total`, `jobs_ffmpeg_duration_ms`, `uploads_bytes`. Sin sistema externo obligatorio; Micrometer si ya está, si no contadores simples testeables.

## Ordering

- Filtro MDC → `logback-spring.xml` pattern con `%X{requestId}` → logs `INFO` en bordes (`controllers/*`, `services/*` públicos) → `DEBUG` interno → métricas en `utils/health|metrics`.

## Naming

- Keys MDC: `requestId` (nunca `reqId/request_id`). Campos: `module, action, id, status, ms`. Métricas `snake_case` (`ffmpeg_jobs_failed_total`).

## Verificación

- `curl -H "X-Request-Id: QA-123" localhost:8080/new-api/utils/health` y el log contiene `requestId=QA-123`.
- `grep -rni "password\|apiKey\|authorization\|Bearer " src/main/kotlin --include="*.kt" | grep -i "log"` vacío; `./gradlew test` con test de filtro MDC en verde.
- Flujo `scrape→ffmpeg→upload→archive` emite 4 líneas con el mismo `requestId` (grep por id en logs).

## Out of scope

- Do NOT implementar auditoría quién-hizo-qué aquí. See `audit-trail`.
- Do NOT definir códigos HTTP aquí. See `exception-handling`.
- Do NOT configurar env/secretos aquí. See `config-pattern` / `conn-secrets`.
- Do NOT schedulear jobs aquí. See `batch-jobs-scheduling`.
- Do NOT exponer métricas con PII. See `audit-trail` para reglas de minimización.
