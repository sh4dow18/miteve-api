---
name: exception-handling
description: Use when handling errors in miteve-api to use errors/* custom exceptions plus global ErrorHandler @ControllerAdvice.
license: MIT
compatibility: opencode
---

# Exception Handling — Single Responsibility: error handling only

This skill covers ONLY `src/main/kotlin/sh4dow18/miteve_api/errors/*` + `ErrorHandler.kt`. It does NOT cover formatting or business rules.

## Rules

- Throw custom domain exceptions from services, never raw `try/catch + logger` scattered: `AlreadyExists(value, entity)`, `NoExists(id, entity)`, `BadRequest(msg)`, `ServiceUnavailable(msg)`. Reference throws: `AbstractContentService.kt:168 throw AlreadyExists(...)`, `:73 orElseThrow { NoExists(id, "Content") }`, `:199 throw BadRequest("Seasons List cannot be zero")`.
- Central mapping in `errors/ErrorHandler.kt:12 @ControllerAdvice class ErrorHandler : ResponseEntityExceptionHandler()`: `AlreadyExists → 409 CONFLICT`, `BadRequest → 400`, `NoExists → 404 NOT_FOUND`, `ServiceUnavailable → 503`, fallback `Exception/RuntimeException → 500` (`ErrorHandler.kt:14-27`).
- Shape via `errors/ApiError.kt` (`status`, `message`); build in `getError(exception, status)` with `logger.debug("Exception Found: {}", exception)` (`ErrorHandler.kt:29-36`). Keep logging central — do not add per-service loggers for expected domain errors.
- Services decide *which* domain error; `ErrorHandler` decides HTTP code. Controllers do not catch. No `try/catch` in controllers to remap codes.
- Upstream failures (lamovie.org down/paused) use `ServiceUnavailable`, not generic 500. Reference comment `ErrorHandler.kt:22-24`.
- All text in English for messages; user-facing Spanish (if any) belongs to client, not API strings.

## Ordering

- `errors/` one class per file: `ApiError.kt`, `BadRequest.kt`, `AlreadyExists.kt`, `NoExists.kt`, `ServiceUnavailable.kt`, `ErrorHandler.kt`. Handler lists specific `@ExceptionHandler` first, generic `Exception` last.

## Naming

- Exceptions `PascalCase` nouns: `AlreadyExists`, `NoExists`, `BadRequest`, `ServiceUnavailable`. Handler methods `camelCase`: `alreadyExists`, `badRequest`, `noExists`, `serviceUnavailable`, `default`, `getError`.

## Verificación

- `./gradlew build` passes; smoke: missing content `GET new-api/contents/nope` → 404; duplicate `POST new-api/contents` → 409; empty seasons list → 400.
- No raw `try/catch (Exception e) { logger... }` in new service/controller code (`grep -r "catch (" services/ controllers/` reviewed).

## Out of scope

- Do NOT define formatting here. See `kotlin-format`.
- Do NOT define business validation rules here (only mapping). See `service-pattern`.
- Do NOT define auth failures here. See `auth-server`.
