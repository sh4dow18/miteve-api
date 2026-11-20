---
name: file-upload-archive
description: Use when handling large multipart uploads, SSH transfer, DASH archive, checksum verify, resume, and tmp cleanup in miteve-api.
license: MIT
compatibility: opencode
---

# File Upload Archive — Single Responsibility: file ingest only

This skill covers ONLY `services/upload/UploadService.kt`, `controllers/UploadController.kt`, `controllers/ArchiveController.kt` + `verify-dash`. It does NOT cover scrape scheduling, transcode params, ni auth.

## Rules

- Actores: `services/upload/UploadService.kt` + `AbstractUploadService.kt` (SSH `host/user/remote-path` desde `config-pattern` `upload.ssh.*`), `controllers/UploadController.kt` (multipart), `controllers/ArchiveController.kt` (`verify-dash` checksum). Nunca SSH hardcodeado ni `Runtime.exec("scp ...")` con string concatenado — usar cliente SSH con props tipadas.
- Multipart grande: `spring.servlet.multipart.max-file-size/max-request-size` dimensionados (ej. `2GB`), streaming a disco (`transferTo` / `InputStream.copyTo`), nunca `bytes` completos en memoria. Límite de tipo por extensión/mime (`mp4|mkv|mpd|m4s`) + tope de tamaño → `400` vía `errors/BadRequest.kt`.
- Resume: uploads por chunks (`chunkNumber/totalChunks/uploadId`) o `Content-Range`; reintento reanuda desde último chunk persistido, no desde cero. `verify-dash` recalcula checksum (SHA-256) del `.mpd` + segmentos y compara con el manifest; mismatch → `409/422` vía `errors/*`, nunca marcar `DONE` a ciegas.
- Antivirus básico: escaneo de firma/magia (magic bytes) + bloqueo de ejecutables (`exe|sh|bat|jar`); si hay ClamAV, `clamdscan` con timeout; cuarentena en `work-dir/quarantine`. Todo rechazo → `400` + log `WARN module=upload action=rejected`.
- Limpieza tmp: `try/finally` borra staging en éxito y en fallo; job `@Scheduled` purga `work-dir` con `mtime > 24h` (ver `batch-jobs-scheduling` para el patrón). `df`-check antes de aceptar: sin espacio → `503` vía `errors/ServiceUnavailable.kt`.

## Ordering

- `UploadController` valida tipo/tamaño → staging en `ffmpeg.work-dir` → antivirus → `UploadService` SSH put (`host/user/remote-path`) → `ArchiveController.verify-dash` checksum → `DONE` + limpieza tmp.

## Naming

- Endpoints `POST new-api/upload`, `POST new-api/upload/{id}/chunks`, `POST new-api/archive/verify-dash`. Vars `uploadId`, `chunkNumber`, `totalChunks`, `checksumSha256`. Dirs `staging/`, `quarantine/`.

## Verificación

- `curl -F file=@QA_big.mp4 new-api/upload` → `200/201`; reintento de chunk duplica sin corromper; `verify-dash` con archivo alterado → `409`.
- `ls $WORK_DIR/staging` vacío tras éxito y tras fallo; `grep -rn "Runtime.exec.*scp" services/upload/` vacío.
- `./gradlew test` con `@WebMvcTest(UploadController)` + `UploadService` mockeado en verde; test de `QA_*.exe` rechazado con `400`.

## Out of scope

- Do NOT schedulear/transcodear aquí. See `batch-jobs-scheduling`.
- Do NOT definir `work-dir/timeout/ssh` aquí. See `config-pattern`.
- Do NOT exponer métricas de bytes fuera de utils. See `observability-logs`.
- Do NOT cambiar códigos de error base. See `exception-handling`.
- Do NOT paginar listados de archivos aquí. See `pagination-filtering`.
