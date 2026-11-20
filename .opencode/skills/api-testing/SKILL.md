---
name: api-testing
description: Use when adding or fixing tests in miteve-api with JUnit5, MockMvc, Security test mocks, and Testcontainers Postgres.
license: MIT
compatibility: opencode
---

# API Testing — Single Responsibility: test pyramid only

This skill covers ONLY `src/test/kotlin/**/*.kt` + `build.gradle.kts` test deps. It does NOT cover production code, Docker runtime, or OpenAPI docs.

## Rules

- Stack: JUnit5 (`junit-jupiter`), `spring-boot-starter-test`, `spring-security-test`, Testcontainers Postgres (`org.testcontainers:postgresql`). Postgres driver `42.7.10` is the DB contract — H2 only if explicitly justified, never as silent default.
- Slice per controller: one `@WebMvcTest(XController::class)` per file in `controllers/*` under `${access}=new-api` (`application.properties:13`). Ejemplo: `ContentControllerTest` → `GET new-api/contents?page=0&size=20`; `EpisodeControllerTest` → `GET new-api/episodes/{id}`; `AuthControllerTest` → `POST new-api/auth/login`.
- JWT mock: `@WithMockUser` o `SecurityMockMvcRequestPostProcessors.jwt()` para roles de `config/SecurityConfig.kt`. Nunca deshabilitar seguridad en el test (`@AutoConfigureMockMvc(addFilters=false)` prohibido salvo test explícito de config). Correcto: `mockMvc.get("/new-api/contents") { with(jwt().authorities(SimpleGrantedAuthority("ROLE_USER"))) }`. Incorrecto: bypass con `permitAll` temporal.
- Datos `QA_*`: prefijo `QA_` en todo dato creado por tests (usuarios `QA_user_*`, contenidos `QA_content_*`, comentarios `QA_comment_*`). Limpieza en `@AfterEach` / `@Transactional` para no contaminar seed `init` vs `!init`.
- Capas: `services/*` con Mockito `@MockitoBean` (ej. `ContentService` mockeado en `ContentControllerTest`); `repositories/*` solo vía `@DataJpaTest` + Testcontainers; `mappers/*` (MapStruct kapt) con asserts campo-a-campo `dtos/* ↔ entities/*`; `errors/*` (`errors/ErrorHandler.kt`) con asserts de status `400/404/409/503`.
- Comandos: `./gradlew test` para suite; `./gradlew test --tests "*ContentControllerTest*"` para foco; `./gradlew jacocoTestReport` para coverage. Nuevo endpoint sin test = PR incompleto.

## Ordering

- `package` → JUnit5 + MockMvc + Mockito/Testcontainers imports → `@WebMvcTest/@DataJpaTest` class `XxxTest` → `given` mocks de `services/*` → `when` llamada MockMvc/repositorio → `then` status + body + `verify` interacciones.
- Orden de archivos: controller slice primero, service unit segundo, repository/`entities/*` integración tercero.

## Naming

- Clases `XControllerTest`, `XServiceTest`, `XRepositoryTest`. Métodos `should<Comportamiento>When<Condicion>` (ej. `shouldReturn400WhenSizeIsMinusOne`). Datos `QA_*`. Métodos helper `givenContentQA()`, `givenJwtUser()`.

## Verificación

- `./gradlew test` en verde; `./gradlew jacocoTestReport` con cobertura en nuevos `controllers/*` y `services/*`.
- `grep -r "addFilters=false" src/test` vacío salvo excepción documentada; `grep -r "QA_" src/test` no vacío.
- Smoke manual tras test: `GET new-api/contents?page=0&size=20`, caso absurdo `size=-1` → `400` vía `errors/BadRequest.kt`.

## Out of scope

- Do NOT change business logic to make tests pass. See `service-pattern`.
- Do NOT change queries/índices aquí. See `performance-jpa` / `repository-pattern`.
- Do NOT document endpoints aquí. See `api-docs-openapi`.
- Do NOT levantar infra local aquí. See `docker-local-env`.
- Do NOT redefinir códigos de error aquí. See `exception-handling`.
