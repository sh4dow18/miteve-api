---
name: controller-pattern
description: Use when adding REST endpoints in miteve-api controllers/ under ${access}=new-api as thin layers over services.
license: MIT
compatibility: opencode
---

# Controller Pattern — Single Responsibility: REST boundary only

This skill covers ONLY `src/main/kotlin/sh4dow18/miteve_api/controllers/*.kt`. It does NOT cover business logic, data access, or mapping.

## Rules

- Location: `controllers/`. One class per file, `@RestController` + `@RequestMapping("\${endpoint.<name>}")` where `endpoint.*` comes from `application.properties:16-31` under `access=new-api` (`application.properties:13`). Reference: `controllers/ContentController.kt:20-23 @RequestMapping("\${endpoint.contents}") class ContentController(private val contentService: ContentService)`.
- Keep thin: map request → call exactly one service method → return result. No business rules, no SQL, no direct repository/mapper access. Correct: `fun findById(@PathVariable id: String) = contentService.findById(id)`. Wrong: repository or `ContentMapper` call inside controller.
- Routing: `@GetMapping`, `@PostMapping(consumes/produces = APPLICATION_JSON_VALUE)`, `@PutMapping`, `@RequestParam(defaultValue=...)` for pagination (`page/size` in `ContentController.kt:26-29,48-53`), `@PathVariable` for ids. `ResponseBody` where existing style uses it; prefer `ResponseEntity` only when status/headers matter.
- Validation: `@Valid @RequestBody` DTOs on writes; fail fast before service. Pagination defaults mirror existing (`page=0 size=20`, genre `size=10`, similar `size=15`).
- CORS: class-level `@CrossOrigin(origins = ["http://localhost:3000", "https://miteve.vercel.app"])` as in `ContentController.kt:22` and `UtilsController.kt`. Global CORS source also exists in `config/SecurityConfig.kt:106-115` — do not fight it, keep both in sync.
- No security bypass here; method security lives in `config/SecurityConfig.kt` + `@EnableMethodSecurity`. See `auth-server`.

## Ordering

- `package` → Spring web imports → DTO/service imports → `// X Rest Controller` → `@RestController/@RequestMapping/@CrossOrigin class XController` → `@GetMapping` reads first, `@PostMapping/@PutMapping` writes last (same order as `ContentController.kt`).

## Naming

- Class `XController`, file `XController.kt`. Methods `findAll/findById/findByX/searchByX/insert/update/delete`. Params `camelCase` (`profileId`, `genreId`, `tmdbId`).

## Verificación

- `./gradlew build` passes; manual smoke: `GET ${access}/contents?page=0&size=20`, `GET ${access}/contents/{id}`, `POST ${access}/contents` with valid `ContentRequest`.
- `grep -r "Repository\|Mapper" controllers/<New>Controller.kt` empty for new code (thin check).

## Out of scope

- Do NOT implement business logic here. See `service-pattern`.
- Do NOT write queries here. See `repository-pattern`.
- Do NOT map entities manually here. See `mapper-pattern`.
- Do NOT handle auth internals here. See `auth-server`.
- Do NOT define error HTTP codes here. See `exception-handling`.
