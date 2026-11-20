---
name: config-pattern
description: Use when adding app configuration in miteve-api with @ConfigurationProperties, env binding, and no secrets in git.
license: MIT
compatibility: opencode
---

# Config Pattern — Single Responsibility: typed configuration only

This skill covers ONLY `config/*Properties.kt` + `application.properties` bindings. It does NOT cover secrets storage, controller logic, or Docker runtime.

## Rules

- Siempre `@ConfigurationProperties(prefix=...)` + `@ConstructorBinding` (Kotlin data class), nunca `@Value` disperso. Correcto: `@ConfigurationProperties("tmdb") data class TmdbProperties(val apiKey: String, val baseUrl: String)`. Incorrecto: `@Value("\${tmdb.api-key}")` en 5 `services/scrape/*`.
- Prefijos obligatorios: `tmdb.*` (key, base-url), `ffmpeg.*` (work-dir, timeout=`120min` como `Duration`), `upload.ssh.*` (host, user, remote-path, port=22), `access=new-api` (`application.properties:13`) + `endpoint.*` (`application.properties:16-31`). Ejemplo: `ffmpeg.work-dir=/tmp/miteve-ffmpeg`, `ffmpeg.timeout=120m`; `upload.ssh.host=${SSH_HOST}`, `upload.ssh.remote-path=/var/miteve/dash`.
- Binding env: `application.properties` solo referencia env (`tmdb.api-key=${TMDB_API_KEY}`, `upload.ssh.host=${SSH_HOST}`), con default local inofensivo donde aplique. Validar con `@Validated + @NotBlank/@Min` (ej. `ffmpeg.timeout` no nulo, `ssh.port` 1-65535). Fallar al arrancar es mejor que fallar en `services/ffmpeg/FfmpegJobService.kt` a mitad de job.
- `${access}=new-api` es el único prefijo de rutas: `controllers/*` usan `@RequestMapping("\${endpoint.contents}")` y nunca hardcodean `"new-api/contents"`. Nuevo módulo → nueva key `endpoint.<name>` en `application.properties`, nunca string literal.
- Sin secretos en git: ninguna key real, ningún `*.pem`, ningún `.env` en el repo. Ver `conn-secrets` + `docker-local-env`. `grep -r "apiKey\s*=\s*['\"][^$]" config/ application.properties` debe dar vacío.

## Ordering

- `config/XxxProperties.kt` (`@ConfigurationProperties`) → `application.properties` binding con `${ENV}` → `@EnableConfigurationProperties` en `@Configuration` → inyección por constructor en `services/*` (ej. `UploadService(sshProps: SshProperties)`) → documentar key nueva en `.env.example`.

## Naming

- Clases `TmdbProperties`, `FfmpegProperties`, `SshProperties` en `config/`. Keys `kebab-case` (`work-dir`, `remote-path`, `api-key`). Prefijos `tmdb|ffmpeg|upload.ssh|endpoint|access`.

## Verificación

- `./gradlew bootRun` arranca con `.env.example` + placeholders válidos; sin env requerida falla con `BindingValidationException` claro.
- `grep -rn "@Value" src/main/kotlin` vacío o solo legacy documentado; `grep -rn "new-api/" controllers/` vacío (todo vía `${endpoint.*}`).
- `./gradlew test` con `ApplicationContextRunner` o slice que aserte binding (`ffmpeg.timeout == Duration.ofMinutes(120)`).

## Out of scope

- Do NOT store real secrets here. See `conn-secrets`.
- Do NOT define compose/profiles here. See `docker-local-env`.
- Do NOT log secret values here. See `observability-logs`.
- Do NOT implement scrape/ffmpeg/upload logic here. See `scrape-upload-ffmpeg` / `batch-jobs-scheduling` / `file-upload-archive`.
- Do NOT change route behavior here. See `controller-pattern`.
