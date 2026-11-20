---
name: auth-server
description: Use when changing auth in miteve-api for Security.kt JWT plus SecurityConfig.kt profiles, BCrypt, roles and privileges.
license: MIT
compatibility: opencode
---

# Auth Server — access and scope only

This skill covers ONLY authN/authZ: `config/Security.kt`, `config/SecurityConfig.kt`, `entities/Role.kt`, `entities/Privilege.kt`, login flow. It does NOT cover UI, formatting, or business rules.

## Rules

- Entry: `POST /new-api/login` via `JwtAuthenticationFilter.setFilterProcessesUrl("/new-api/login")` (`config/Security.kt:44`). Only POST; other methods throw `AuthenticationServiceException`. Body `dtos/config/LoginRequest.kt` (`email`, `password`) deserialized with Jackson `ObjectMapper`.
- Token: JJWT `0.11.5` (`api/impl/gson`) `Jwts.builder().signWith(SECRET_KEY)...compact()` with `TOKEN_TYPE=JWT`, `TOKEN_ISSUER=secure-api`, `TOKEN_AUDIENCE=secure-app`, `TOKEN_LIFETIME=864000000`, `TOKEN_PREFIX="Bearer "` (`config/Security.kt:26-35,95-104`). Response header `Authorization: Bearer <jwt>` + JSON principal body.
- Verify: `JwtAuthorizationFilter` extracts `Authorization`, strips prefix, `Jwts.parserBuilder().setSigningKey(SECRET_KEY)...parseClaimsJws(...)`, maps `claims["authorities"]` to `SimpleGrantedAuthority`, sets `SecurityContextHolder` + `LoggedUser.logIn(userId)` (`config/Security.kt:126-163`). On any exception: `clearContext()`, continue chain (do not 500).
- Passwords: `BCryptPasswordEncoder` bean (`config/SecurityConfig.kt:73-77`). Never store/compare plaintext.
- Profiles: `@Profile("init") CloseSecurityConfiguration` = closed mode, `anyRequest().authenticated()` (`SecurityConfig.kt:22-49`); `@Profile("!init") JwtSecurityConfiguration` = JWT stateless + `@EnableMethodSecurity` + CORS + `SessionCreationPolicy.STATELESS` + `AppCustomDsl` filters (`SecurityConfig.kt:50-116`). Do not mix rules between profiles.
- CORS: `corsConfigurationSource()` allows `http://localhost:3000`, `http://localhost:3001`, `http://localhost:${port}`, `${ip.domain}`; methods `GET,POST,PUT,DELETE,PATCH,OPTIONS`; `exposedHeaders=Authorization` (`SecurityConfig.kt:106-115`). Controller `@CrossOrigin` (`ContentController.kt:22`) must stay consistent — keep both in sync. `ip.domain` from `application.properties:77`.
- Roles/privileges: `entities/Role.kt` ↔ `entities/Privilege.kt` `@ManyToMany` (`Privilege.kt:19-20 rolesList`). Authorities are privilege/role strings in JWT `authorities` claim; enforce with method security (`@PreAuthorize`), not scattered `if` in services.
- Secrets: JWT `SECRET_KEY = Keys.secretKeyFor(HS256)` is currently in-memory generated (`Security.kt:34`) — any restart invalidates tokens. Do not commit a hardcoded replacement; rotate via env if changed. See `conn-secrets`.

## Ordering

- `Security.kt`: `SecurityConstants` object → `JwtAuthenticationFilter` → `JwtAuthorizationFilter` → `LoggedUser`. `SecurityConfig.kt`: `CloseSecurityConfiguration` (@Profile init) → `JwtSecurityConfiguration` (@Profile !init) → `AppCustomDsl`.

## Naming

- Filters `JwtAuthenticationFilter`, `JwtAuthorizationFilter`; constants `UPPER_SNAKE`; `LoggedUser.logIn/get` ThreadLocal accessor.

## Verificación

- `./gradlew build` passes; smoke: `POST new-api/login` valid → `Authorization` header present; invalid → 401; `GET new-api/contents` with expired/missing token behaves per active profile.
- No plaintext password, no anonymous bypass beyond intentional `permitAll()` in `!init` filter chain (document any new opening in PR).

## Out of scope

- Do NOT change connection strings here. See `conn-secrets`.
- Do NOT map auth errors to custom codes outside `ErrorHandler`. See `exception-handling`.
- Do NOT put business rules in filters. See `service-pattern`.
