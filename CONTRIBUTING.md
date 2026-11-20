# Contributing a MiTeVe-API

> Guía de referencia para el equipo de desarrollo de MiTeVe-API.

---

## Qué es MiTeVe-API

**MiTeVe-API** es el backend de la plataforma **MiTeVe** (Kotlin 2.2.21 + Spring Boot
4.0.3 + JPA/Postgres). Expone **19 controllers** bajo el prefijo `new-api/...`
(`access=new-api` en `application.properties`) y alimenta a `miteve-web` y `miteve-apk`.

| Controller | Path (`new-api/...`) | Dominio |
| ---------- | -------------------- | ------- |
| `ArchiveController` | `archive` | Archivo |
| `BugReportController` | `bug-reports` | Reportes |
| `CommentController` | `comments` | Comentarios |
| `ContainerController` | `containers` | Containers |
| `ContentController` | `contents` | Contents |
| `ContinueWatchingController` | `continue-watching` | Seguir viendo |
| `EpisodeController` | `episodes` | Episodes |
| `FfmpegController` | `ffmpeg` | Ffmpeg |
| `GenreController` | `genres` | Géneros |
| `HistoryController` | `history` | Historial |
| `PrivilegeController` | `privileges` | Auth |
| `ProfileController` | `profiles` | Profiles |
| `RoleController` | `roles` | Auth |
| `ScrapingController` | `scrape` | Scrape |
| `SeasonController` | `seasons` | Seasons |
| `SuggestedContentReportController` | `suggested-content-reports` | Reportes |
| `UploadController` | `upload` | Upload |
| `UserController` | `users` | Users |
| `UtilsController` | `utils` | Health / versión |

Ejemplo real (`controllers/ContentController.kt`):

```kotlin
@RestController
@RequestMapping("\${endpoint.contents}") // new-api/contents
class ContentController(private val contentService: ContentService) {
    @GetMapping("{id}")
    @ResponseBody
    fun findById(@PathVariable id: String) = contentService.findById(id)
}
```

---

## Arquitectura del proyecto

Capas con dependencia en una sola dirección:

```text
controllers → services → repositories → entities
                  ↓            ↓
              mappers ← dtos ──┘
```

| Capa | Paquete | Responsabilidad |
| ---- | ------- | --------------- |
| **controllers** | `.../controllers/` | Endpoints REST `new-api/...`, delegan al service |
| **services** | `.../services/<dominio>/` | Lógica de negocio (`XService` + `AbstractXService`) |
| **repositories** | `.../repositories/` | Spring Data JPA, solo queries |
| **entities** | `.../entities/` | Modelos JPA |
| **mappers** | `.../mappers/` | MapStruct entity ↔ dto (+ `MapperUtils`) |
| **dtos** | `.../dtos/` | `*Request` / `*Response` por dominio |
| **config** | `.../config/` | JWT (`Security.kt`, `SecurityConfig.kt`) |
| **errors** | `.../errors/` | `ErrorHandler` (`@ControllerAdvice`) + `ApiError` |

Seguridad por perfiles (`config/SecurityConfig.kt`): `init`
(`CloseSecurityConfiguration`, arranque cerrado) y `!init`
(`JwtSecurityConfiguration`, JWT stateless con `JwtAuthenticationFilter` en
`POST /new-api/login`). Errores uniformes vía `errors/ErrorHandler.kt`
(`NoExists` → 404, `BadRequest` → 400, `AlreadyExists` → 409,
`ServiceUnavailable` → 503).

---

## Propósito de esta guía

- Estandarizar los procesos de desarrollo
- Facilitar el onboarding de nuevos miembros
- Mantener calidad y consistencia del código
- Documentar convenciones y flujos de trabajo

---

## A quién va dirigido

- **MiTeVe Team**: referencia rápida de procesos y convenciones
- **Nuevas incorporaciones**: onboarding (proyecto, equipo, flujos)
- **QA / colaboradores**: cómo reportar y validar

---

## Cómo usar esta guía

### Para nuevos miembros

1. [Requisitos](documentation/contributing/01-requirements.md) — JDK 24 + Gradle + Postgres + Docker
2. [Primeros Pasos](documentation/contributing/02-getting-started.md) — `./gradlew bootRun`, env `USER/DATABASE_IP/...`
3. [Equipo](documentation/contributing/03-team.md)
4. [Flujo de Trabajo](documentation/contributing/04-workflow.md)

### Para miembros actuales

- [Ramas](documentation/contributing/05-branching.md) · [Commits](documentation/contributing/06-commits.md) · [PRs](documentation/contributing/07-pull-requests.md)
- [IA](documentation/contributing/20-ai.md) · [FAQ](documentation/contributing/21-faq.md)

---

## Tabla de Contenidos

| # | Sección | Descripción |
| --- | ------- | ----------- |
| 1 | [Requisitos](documentation/contributing/01-requirements.md) | JDK 24 + Gradle + Postgres + Docker |
| 2 | [Primeros Pasos](documentation/contributing/02-getting-started.md) | Clonar, env `USER/DATABASE_IP/...`, `./gradlew bootRun` |
| 3 | [Equipo](documentation/contributing/03-team.md) | MiTeVe Team, roles y onboarding |
| 4 | [Flujo de Trabajo](documentation/contributing/04-workflow.md) | Issue → branch → code → commit → push → PR → review → merge |
| 5 | [Ramas](documentation/contributing/05-branching.md) | `Tipo/<issue>-desc`, main/develop/release |
| 6 | [Commits](documentation/contributing/06-commits.md) | `#<issue> tipo: desc` |
| 7 | [Pull Requests](documentation/contributing/07-pull-requests.md) | Crear, revisar, squash, aprobaciones 1/2 |
| 8 | [Estilo de Código](documentation/contributing/08-code-style.md) | Capas controller/service/repository/entity + mapper/controller |
| 9 | [Guía de Documentación](documentation/contributing/09-documentation-guide.md) | Cuándo y cómo documentar endpoints |
| 10 | [Control de Versiones](documentation/contributing/10-versioning.md) | `build.gradle.kts` + `application.properties` + `GET new-api/utils/version` |
| 11 | [Releases](documentation/contributing/11-releases.md) | Tipos, ciclo de vida, hotfix |
| 12 | [Connection Strings](documentation/contributing/12-connection-strings.md) | Env vars del datasource (stub) |
| 13 | [Database Backup](documentation/contributing/13-database-backup.md) | `pg_dump` (stub) |
| 14 | [Database Migrations](documentation/contributing/14-database-migrations.md) | Flyway, responsable `sh4dow18`, salir de `ddl-auto=update` |
| 15 | [Deployments](documentation/contributing/15-deployments.md) | Staging, producción, verificación |
| 16 | [Certificados](documentation/contributing/16-certificates.md) | Stub → JWT (sin `.pfx`) |
| 17 | [Packages](documentation/contributing/17-packages.md) | Dependencias Gradle |
| 18 | [Logs](documentation/contributing/18-logs.md) | Consola, plataforma, `ApiError` |
| 19 | [Configuración de GitHub](documentation/contributing/19-github-config.md) | `.github/`, workflows, plantillas |
| 20 | [Uso de IA](documentation/contributing/20-ai.md) | Prompts para endpoints, capas, seguridad |
| 21 | [Preguntas Frecuentes](documentation/contributing/21-faq.md) | Build, BD, auth, Git, tests |
| 22 | [Conocer Más](documentation/contributing/22-learn-more.md) | Índice técnico por dominio |
