---
name: pagination-filtering
description: Use when adding page/size/sort/search to miteve-api list endpoints with defaults, max-size caps, and 400 on absurd values.
license: MIT
compatibility: opencode
---

# Pagination Filtering — Single Responsibility: list contract only

This skill covers ONLY query params `page/size/sort/search` en los 19 `controllers/*` + `repositories/*` paginados. It does NOT cover OpenAPI docs, índices DB, o lógica de negocio.

## Rules

- Firma estándar en todo `GET` de lista: `@RequestParam(defaultValue="0") page: Int, @RequestParam(defaultValue="20") size: Int, @RequestParam(defaultValue="id,asc") sort: String, @RequestParam(required=false) search: String?`. Referencia: `controllers/ContentController.kt:26-29,48-53` (`page=0 size=20`, genre `size=10`, similar `size=15`). Los 19 controllers (`Content|Episode|Season|Genre|Comment|History|ContinueWatching|...Controller.kt`) usan la misma firma — sin variantes `limit/offset/q`.
- Defaults y tope: `page=0`, `size=20` (respetar `10/15` donde ya existe), `max-size=100`. `size>100` → cap a `100` o `400` según contrato existente; `size=-1`, `page<0`, `sort` malformado → `400` vía `errors/BadRequest.kt`. Correcto: `if (size < 1 || size > 100) throw BadRequest("size must be 1..100")`. Incorrecto: `PageRequest.of(page, Int.MAX_VALUE)` o paginar en memoria con `.drop().take()`.
- `sort` whitelist: `field,direction` contra campos de `entities/*` (ej. `title,asc`, `createdAt,desc`). Dirección solo `asc|desc`; campo desconocido → `400`. Nunca interpolar `sort` crudo en `@Query` — usar `Sort.by(...)` / `Pageable`.
- `search` delega a `repositories/*` (`findByTitleContainingIgnoreCase`, `@Query` con `LIKE`), nunca filtra en `services/*` tras `findAll()`. Respuesta incluye `totalCount` + headers: body `{"items":[],"page":0,"size":20,"totalCount":N}` o `Page<T>` + headers `X-Total-Count: N`. `mappers/*` mapean `Page<Entity>` → `Page<DTO>` sin perder totales.
- Paginación siempre en DB (`Pageable` → JPA `LIMIT/OFFSET`). Ver `performance-jpa` para índices de los campos `sort/search`.

## Ordering

- `controllers/*` params (`page/size/sort/search`) → validación `400` → `services/*` construye `PageRequest` → `repositories/*` query paginada → `mappers/*` a `dtos/*` → respuesta con `totalCount` + `X-Total-Count`.

## Naming

- Params `page/size/sort/search` exactos. Métodos `findAll(page,size,sort,search)`, repo `findByXContainingIgnoreCase(..., pageable)`. Header `X-Total-Count`. Campo body `totalCount`.

## Verificación

- `GET new-api/contents?page=0&size=20` → `200` + `X-Total-Count`; `GET new-api/contents?size=-1` → `400`; `GET new-api/episodes?sort=bogus,asc` → `400`; `GET new-api/contents?search=QA_` filtra.
- `grep -rn "findAll()" controllers/ services/` sin `Pageable` vacío en endpoints listados; `./gradlew test` con `@WebMvcTest` de `400` en absurdos en verde.
- `show-sql` (solo dev) muestra `limit ? offset ?` — sin `select *` completo.

## Out of scope

- Do NOT document el contrato aquí. See `api-docs-openapi`.
- Do NOT crear índices aquí. See `performance-jpa`.
- Do NOT definir el `400` base aquí. See `exception-handling`.
- Do NOT testear fuera de slices de paginación. See `api-testing`.
- Do NOT cambiar thresholds sin actualizar `dtos/*` y docs.
