---
name: scrape-upload-ffmpeg
description: Use when working on miteve-api scrape fallback chain, ffmpeg jobs, or SSH upload pipeline as idempotent background jobs.
license: MIT
compatibility: opencode
---

# Scrape Upload Ffmpeg — bulk ingest and media pipeline only

This skill covers ONLY `services/scrape/*` (ScrapeJobService, LamovieScrapeService/AbstractLamovieScrapeService, fallback/AnimeFallbackService + 8 providers), `services/ffmpeg/*` (FfmpegService/FfmpegJobService, `FfmpegController`), `services/upload/*` (UploadService, `UploadController`), `services/batch_update/*`, endpoints `endpoint.scrape/ffmpeg/archive/upload` (`application.properties:31,63-65`). It does NOT cover generic repository design or UI.

## Rules

- Scrape chain order is contract (`application.properties:39-62`): `lamovie.org` → `lamovie.la` → `jkanime → latanime → monoschinos → tioanime → animefenix → animeflv → consumet → nyaa`, metadata via `jikan.api`, gated by `anime.latino.fallback.enabled` + `*.enabled` flags + `anime.latino.fallback.providers` list. Never reorder/hardcode in code — read flags/providers from config. Services: `services/scrape/ScrapeJobService.kt`, `LamovieScrapeService.kt`, `AbstractLamovieScrapeService.kt`, `fallback/AnimeFallbackService.kt`.
- Idempotent jobs: scrape/ffmpeg/upload/batch-update must be safe to retry (keyed by `tmdbId`/slug `content.id`, `seasonId=contentId-seasonNumber`, `episodeId=seasonId-episodeNumber` as in `AbstractContentService.insertEpisodes`). Duplicate trigger → same result, no duplicate rows/files. Verify with `verify-dash`-equivalent: re-run job and confirm counts unchanged.
- FFmpeg: `ffmpeg.work-dir=${lamovie.download-dir}` (`:66`), `ffmpeg.timeout-minutes=120` (`:67`), async timeout `3600000` (`:74-75`). Long jobs run async/off-request; controllers (`FfmpegController`) only enqueue + return job handle, never block. Never run ffmpeg inline in a CRUD service.
- Upload SSH: `UploadService` reads `upload.ssh.*` (`:68-73` host/user/remote-path/port/password/timeout `30`). Stream from work-dir to `remote-path`; never log password; retry with backoff; verify remote size/checksum before marking done.
- Archive endpoint (`endpoint.archive`, `ArchiveController`) is cold-storage move, not delete — keep DB row, relocate file, record new path.
- Errors via `errors/ServiceUnavailable` for provider down/paused (maps to 503 in `ErrorHandler.kt:22-24`); validation via `BadRequest`; missing via `NoExists`. Dates `java.time`, no raw `Date`.
- Controllers thin (`ScrapingController`, `FfmpegController`, `UploadController`, `ArchiveController`): enqueue/query, delegate to job services. Orchestration across scrape→ffmpeg→upload lives in job services, not controllers.

## Ordering

- Pipeline: scrape (fetch metadata/files) → ffmpeg (transcode/package in work-dir) → upload (SSH to remote-path) → archive (relocate) → content/episode rows visible via `ContentController`. Batch episode inserts via `services/batch_update/*` (`BatchUpdateEpisodesRequest/Response`).

## Naming

- Services `ScrapeJobService`, `LamovieScrapeService`, `AnimeFallbackService`, `FfmpegService`, `FfmpegJobService`, `UploadService`; controllers `ScrapingController`, `FfmpegController`, `UploadController`, `ArchiveController`. Jobs `camelCase` verbs (`scrapeContent`, `transcodeEpisode`, `uploadFile`).

## Verificación

- `./gradlew build` passes; job re-run is idempotent (second run = no new rows/files); ffmpeg job respects 120min timeout; upload with bad host → `ServiceUnavailable`/retry, never secret in log; `git diff` shows no hardcoded provider URL outside `application.properties`.

## Out of scope

- Do NOT write generic repository queries here. See `repository-pattern`.
- Do NOT put business validation in controllers. See `controller-pattern` and `service-pattern`.
- Do NOT handle error HTTP mapping here. See `exception-handling`.
- Do NOT commit hosts/passwords. See `conn-secrets`.
