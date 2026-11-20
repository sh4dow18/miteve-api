---
name: conn-secrets
description: Use when configuring connections and secrets in miteve-api application.properties plus env without committing real values.
license: MIT
compatibility: opencode
---

# Conn Secrets — Single Responsibility: connection config only

This skill covers ONLY `src/main/resources/application.properties` + env wiring. It does NOT cover repository queries or DDL.

## Rules

- Source of truth: `src/main/resources/application.properties`. All hosts/secrets come from env with local defaults — never commit real credentials.
- Database: `spring.datasource.username=${USER}`, `url=jdbc:postgresql://${DATABASE_IP}:5432/${DATABASE}`, `password=${PASSWORD}` (`application.properties:6-9`). Port `server.port=${PORT}` (`:14`). Set `USER/DATABASE_IP/DATABASE/PASSWORD/PORT` in env; no defaults with real values.
- TMDB + scrape: `tmdb.api-key=${TMDB_API_KEY:...default...}` (`:33`), `lamovie.base-url/fast-api`, `lamovie.la.*`, provider bases (`jkanime/latanime/monoschinos/tioanime/animefenix/animeflv/consumet/nyaa/jikan`) and `*.enabled` flags (`:34-62`). Toggle via env (`JKANIME_ENABLED`, etc.), do not hardcode URLs in services.
- FFmpeg: `ffmpeg.work-dir=${lamovie.download-dir}` (`:66`), `ffmpeg.timeout-minutes=120` (`:67`), async `spring.mvc.async.request-timeout=3600000` (`:74`). Work dir from `LAMOVIE_DOWNLOAD_DIR` env.
- Upload SSH: `upload.ssh.host=${UPLOAD_SSH_HOST:...}`, `user=${UPLOAD_SSH_USER:...}`, `remote-path=${UPLOAD_SSH_REMOTE_PATH:...}`, `port=${UPLOAD_SSH_PORT:22}`, `password=${UPLOAD_SSH_PASSWORD:${MITEVE_SERVER_PASSWORD:${SSH_PASSWORD:}}}` (`:68-73`), `timeout-minutes=30`. All server-side here — there is no PUBLIC client bundle; do not prefix with `PUBLIC_` or expose in logs/errors.
- Domain/CORS: `ip.domain=${IP_DOMAIN}` (`:77`) feeds `SecurityConfig.corsConfigurationSource()` + controllers. Keep in sync.
- JWT: `Security.kt:34 SECRET_KEY = Keys.secretKeyFor(HS256)` is runtime-generated (restart invalidates tokens). Do not commit a static key; rotate via env-backed config if persistence is ever required (flag in issue + PR).
- Never commit real `.env`, real `application-*.properties` overrides, or pasted tokens/SSH passwords in issues/PRs/logs.

## Ordering

- `application.properties` order is contract: JPA → datasource → `access/server.port` → `endpoint.*` → tmdb/lamovie → anime fallbacks → ffmpeg/archive/upload → timeouts → `ip.domain`. Keep new keys in their section.

## Naming

- Env `UPPER_SNAKE`; property keys `lowercase.dot`: `upload.ssh.host`, `ffmpeg.timeout-minutes`, `anime.latino.fallback.providers`. Placeholders `${VAR:default}` with safe non-secret defaults only.

## Verificación

- `./gradlew bootRun` resolves every `${...}` without `Could not resolve placeholder`; `git status` shows no secret file staged; `git diff` on `application.properties` contains only `${VAR}` references, no literal secrets.

## Out of scope

- Do NOT write queries here. See `repository-pattern`.
- Do NOT run DDL here. See `db-migrations`.
- Do NOT change auth logic here. See `auth-server`.
