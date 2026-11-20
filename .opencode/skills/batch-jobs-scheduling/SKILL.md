---
name: batch-jobs-scheduling
description: Use when adding scheduled batch, scrape, or ffmpeg jobs in miteve-api with idempotency, retry/backoff, and persisted job state.
license: MIT
compatibility: opencode
---

# Batch Jobs Scheduling — Single Responsibility: background jobs only

This skill covers ONLY `services/scrape/*`, `services/ffmpeg/FfmpegJobService.kt`, `batch_update*` y `@Scheduled`. It does NOT cover upload SSH, request HTTP síncrono, ni config de secrets.

## Rules

- Entradas: `services/scrape/ScrapeJobService.kt` (orquesta), `services/scrape/LamovieScrapeService.kt` + `AbstractLamovieScrapeService.kt` (fuente primaria), `services/ffmpeg/FfmpegJobService.kt` + `AbstractFfmpegService.kt` (transcode DASH), scripts `batch_update*` (lotes manuales). Nuevo job → nuevo método en `*JobService`, nunca lógica suelta en `controllers/ScrapingController.kt` o `FfmpegController.kt` (estos solo disparan/monitorear).
- Fallback anime obligatorio: `lamovie → jkanime → ... → nyaa` en ese orden, cada fuente con timeout propio (ver `config-pattern` `ffmpeg.timeout=120min`). Correcto: `for (source in listOf(lamovie, jkanime, nyaa)) try { scrape(source) } catch (e: Exception) { log.warn(...); continue }`. Incorrecto: un solo `try` gigante o abortar al primer fallo sin `WARN` por fuente.
- `@Scheduled` con `@EnableScheduling`: `fixedDelay` o `cron` en `application.properties` (nunca hardcodeado), `@SchedulerLock` o flag DB si hay réplicas. Job largo de ffmpeg usa `fixedDelay` > `timeout 120min` para no solaparse.
- Idempotencia: clave natural (`tmdbId + season + episode` o `sourceUrl` hash) con `existsBy` en `repositories/*` antes de insertar; re-ejecución del mismo lote = `0 inserts`. Jobs persistidos: entidad `JobExecution` (`jobName`, `startedAt`, `finishedAt`, `status`, `attempts`, `lastError`) vía `repositories/*` — estado en memoria (`var running`) prohibido.
- Retry + backoff: `maxAttempts=3`, `backoff=2s→8s→32s` (`Spring Retry @Retryable` o bucle propio). `errors/ServiceUnavailable.kt` para fuente caída (reintentable), `errors/BadRequest.kt`/`AlreadyExists.kt` no reintentan. Tras agotar: `status=FAILED` + `ERROR` log con `requestId` (ver `observability-logs`).

## Ordering

- `batch_update*` o `@Scheduled` trigger → `ScrapeJobService` (scrape + fallback) → `FfmpegJobService` (transcode `work-dir`, `timeout 120min`) → `UploadService`/`ArchiveController` handoff → persistir `JobExecution` → log `module=scrape|ffmpeg action=finished status=`.

## Naming

- Clases `*JobService`, métodos `runDailyScrape()`, `processPendingFfmpeg()`. Cron keys `jobs.scrape.cron`, `jobs.ffmpeg.fixed-delay`. Estados `PENDING|RUNNING|DONE|FAILED`. Tablas/entidades `JobExecution` en `entities/*`.

## Verificación

- `./gradlew test` con test de idempotencia (doble run → 1 fila) y de fallback (lamovie mock caído → jkanime usado) en verde.
- `SELECT job_name,status,attempts FROM job_execution` muestra historial; re-run manual de `batch_update` no duplica (`AlreadyExists` o skip logueado).
- Logs `scrape→ffmpeg` con mismo `requestId`; `GET new-api/utils/health` sigue `UP` durante job largo.

## Out of scope

- Do NOT hacer upload SSH aquí. See `file-upload-archive`.
- Do NOT definir timeouts/work-dir aquí. See `config-pattern`.
- Do NOT exponer jobs como CRUD aquí salvo trigger. See `controller-pattern`.
- Do NOT loguear secretos/PII del job. See `observability-logs`.
- Do NOT optimizar queries del job aquí. See `performance-jpa`.
