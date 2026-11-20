# AGENTS.md — miteve-api

Kotlin 2.2.21 + Spring Boot 4.0.3 + JPA/Postgres. Prefijo API `new-api/...`
(`access=new-api`). Capas `controllers → services → repositories → entities`
+ `mappers` (MapStruct/kapt) + `dtos`.

## Commands

- Build: `./gradlew build` · correr: `./gradlew bootRun` (env `USER`, `DATABASE_IP`, `DATABASE`, `PASSWORD`, `PORT`, `IP_DOMAIN`).
- Tests: `./gradlew test` (JUnit5; en CI es `continue-on-error` con warning por ahora, sin DDL).
- Verificación rápida antes de commitear: `./gradlew build` en verde + endpoints tocados probados (`GET new-api/contents/...`, `POST new-api/login`).
- Semilla/perfil cerrado: `./gradlew bootRun --args='--spring.profiles.active=init'`.

## Wiring (read before changing)

- Seguridad por perfil en `config/SecurityConfig.kt`: `init` → `CloseSecurityConfiguration` (todo autenticado); `!init` → `JwtSecurityConfiguration` (stateless + CORS + `AppCustomDsl`). Filtros en `config/Security.kt`: `JwtAuthenticationFilter` escucha `POST /new-api/login` y emite `Authorization: Bearer ...`; `JwtAuthorizationFilter` valida por request y fija `LoggedUser` (ThreadLocal). No duplicar JWT fuera de `config/`.
- Ejemplo de controller (`controllers/ContentController.kt`): `@RequestMapping("\${endpoint.contents}")` desde `application.properties`, inyección por constructor `private val contentService`, delega y retorna DTO — sin SQL ni negocio.
- Errores vía `errors/ErrorHandler.kt` (`@ControllerAdvice` → `ApiError`): el service lanza `NoExists` (404) / `BadRequest` (400) / `AlreadyExists` (409) / `ServiceUnavailable` (503, ej. lamovie.org caído); el controller no hace `try/catch`.
- `application.properties`: `spring.jpa.hibernate.ddl-auto = update` (en salida hacia Flyway `validate` + `db/migration/`, ver `14-database-migrations.md`). CI nunca ejecuta DDL.

## Conventions (enforced in review)

- Formato kotlin-format (ktfmt) + reglas `08-code-style.md`: controller delega, service con `XService`/`AbstractXService` + `@Transactional` donde aplique, repository solo `JpaRepository`/`@Query`, entity una clase por archivo `LAZY` por defecto, MapStruct en `mappers/` (nada mapeado a mano), `@RequestMapping("\${endpoint.x}")` nunca hardcodeado.
- Endpoints `new-api/...` paginados (`page`/`size`) con `*Request`/`*Response` en `dtos/`; texto y mensajes de error en español para el equipo, código en inglés.
- Migración de esquema: marcar `requires DB migration` en la task + label `db-migrations`; solo `sh4dow18` aplica en staging/prod. Versión en sync: `build.gradle.kts` + `api.version` + `GET new-api/utils/version`.

## Git guardrails (CI blocks on violation)

- Branch desde `develop`: `Tipo/<issue>-descripcion-corta`, tipos `Feature|Fix|Refactor|Docs` (capitalized). Exentas: `main`, `develop`, `release/*`, `hotfix/*`.
- Commit: `#<issue> <tipo>: <desc>` en inglés minúsculas, tipos `feat|fix|refactor|docs|style|test|chore`. El `#` debe coincidir con la rama; `Docs/*` solo acepta `docs:`.
- PR: título = nombre exacto de la rama; base `develop`; plantilla completa (Historia `Como/Quiero/Para`, Cambios, Cómo probar con `curl` a `new-api/...`, Criterios). Merges: `develop` 1 aprobación, `main`/`release/*` 2. Reviewer auto: `sh4dow18`.

## Do-not-touch rules

- Versión solo en `release/*`/`hotfix/*`: `version` en `build.gradle.kts` + `api.version` en `application.properties` + `GET new-api/utils/version`, en sync. Tags/releases solo desde GitHub UI (`vX.Y.Z`), nunca local.
- Full contributor docs: `documentation/contributing/` (guías 01–22) + `CONTRIBUTING.md`. No tocar `.opencode/skills`.
