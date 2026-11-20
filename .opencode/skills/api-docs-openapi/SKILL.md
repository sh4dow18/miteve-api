---
name: api-docs-openapi
description: Use when documenting miteve-api REST endpoints with Springdoc OpenAPI, Swagger UI, and versioned new-api routes.
license: MIT
compatibility: opencode
---

# API Docs OpenAPI — Single Responsibility: contract documentation only

This skill covers ONLY OpenAPI annotations + `springdoc` config. It does NOT cover controller logic, auth internals, or tests.

## Rules

- Dependencia: `springdoc-openapi-starter-webmvc-ui` (Spring Boot 4.0.3). Rutas: `/v3/api-docs` (JSON) + `/swagger-ui.html` + `/swagger-ui/index.html`. No exponer en prod sin gate de `config/SecurityConfig.kt` — Swagger público = fallo de seguridad.
- Anotar en `controllers/*` bajo `${access}=new-api` (`application.properties:13`): `@Tag(name="contents")`, `@Operation(summary=...)`, `@ApiResponse(responseCode="200"/"400"/"401"/"404")`, `@Parameter` en `page/size/sort/search`, `@Schema` en `dtos/*` (ej. `ContentRequest`, `EpisodeResponse`). Ejemplo: `ContentController.findAll` → `@Operation(summary="List contents") @Parameter(name="search")`; `EpisodeController.findById` → `200 + 404` vía `errors/NoExists.kt`; auth login → `200 + 401`.
- Ejemplos reales obligatorios: `new-api/contents?page=0&size=20`, `new-api/episodes/{id}`, `new-api/auth/login` con `@Content(examples=@ExampleObject(value="{...}"))`. Sin ejemplo = doc incompleta.
- Versionado: prefijo `${access}=new-api` es la versión actual; nuevo breaking change → nuevo prefijo en `application.properties:16-31` (`endpoint.*`), nunca romper `new-api/*` sin deprecación `@Deprecated + @Operation(deprecated=true)`.
- Config central: un `@Configuration` (u `application.properties`) con `Info(title="miteve-api", version=...)` + `SecurityScheme Bearer JWT` (JJWT). Sin configs OpenAPI dispersas por controller.

## Ordering

- Dependencia `build.gradle.kts` → config `Info + SecurityScheme` → `@Tag` por controller → `@Operation/@ApiResponse` por método (reads primero, writes después, mismo orden que `controllers/ContentController.kt`) → `@Schema/@ExampleObject` en `dtos/*`.

## Naming

- Tags en minúscula plural (`contents`, `episodes`, `auth`, `utils`). `OperationId` `camelCase` (`listContents`, `getEpisodeById`). Versiones `new-api`, `new-api-v2` (nunca `v1_final2`).

## Verificación

- `./gradlew bootRun` + abrir `/swagger-ui/index.html`: aparecen `contents|episodes|auth`; `/v3/api-docs` devuelve JSON con `paths./new-api/contents.get`.
- `grep -r "@Operation" controllers/ | wc -l` crece con cada endpoint nuevo; `grep -r "ExampleObject" controllers/ dtos/` no vacío.
- `./gradlew test` en verde (un `@WebMvcTest` que pida `/v3/api-docs` no rompe seguridad).

## Out of scope

- Do NOT implement endpoint logic here. See `controller-pattern`.
- Do NOT change auth/JWT internals here. See `auth-server`.
- Do NOT change error payloads here. See `exception-handling`.
- Do NOT add tests aquí salvo smoke de docs. See `api-testing`.
- Do NOT tocar paginación real aquí. See `pagination-filtering`.
