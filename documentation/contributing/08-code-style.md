# Estilo de Código

> Convenciones de Kotlin + Spring Boot en MiTeVe-API.

Sigue [kotlin-format](../../.opencode/skills/kotlin-format/SKILL.md) para formato
(ktfmt, imports, naming). Aquí van las reglas de arquitectura por capa.

---

## Capas y reglas de dependencia

```text
controllers → services → repositories → entities
                  ↓            ↓
              mappers ← dtos ──┘
```

| Capa | Paquete | Responsabilidad | Ejemplo real |
| ---- | ------- | --------------- | ------------ |
| controllers | `.../controllers/` | REST `new-api/...`, delegar a services, sin SQL ni negocio | `ContentController.kt` |
| services | `.../services/content/` | Lógica de negocio, orquesta repos + mappers | `ContentService` / `AbstractContentService` |
| repositories | `.../repositories/` | Solo Spring Data JPA, queries derivadas/`@Query` | `ContentRepository.kt` |
| entities | `.../entities/` | JPA `@Entity`, relaciones, sin lógica | `Content.kt`, `Season.kt` |
| mappers | `.../mappers/` | MapStruct entity ↔ dto | `ContentMapper.kt` |
| dtos | `.../dtos/` | `*Request` / `*Response` por dominio | `dtos/content/` |
| config | `.../config/` | Seguridad JWT, CORS | `Security.kt`, `SecurityConfig.kt` |
| errors | `.../errors/` | `@ControllerAdvice` + excepciones | `ErrorHandler.kt` |

**Reglas**: el controller nunca toca el repository directo; el service nunca arma
`ResponseEntity`; el repository nunca contiene negocio; el mapper nunca hace I/O.

---

## Controllers

```kotlin
// controllers/ContentController.kt — patrón a seguir
@RestController
@RequestMapping("\${endpoint.contents}") // new-api/contents (application.properties)
@CrossOrigin(origins = ["http://localhost:3000", "https://miteve.vercel.app"])
class ContentController(private val contentService: ContentService) {
    @GetMapping("{id}")
    @ResponseBody
    fun findById(@PathVariable id: String) = contentService.findById(id)
}
```

- Constructor primario con `private val` (inyección por constructor).
- Retornar DTOs/entidades del service; el status de error lo pone `ErrorHandler`.
- `@RequestMapping("\${endpoint.x}")` siempre desde `application.properties`, nunca hardcodear `new-api/...`.

---

## Services

```kotlin
// services/content/ContentService.kt
interface ContentService {
    fun findById(id: String): ContentResponse
}
// services/content/AbstractContentService.kt
@Service
class AbstractContentService(
    private val contentRepository: ContentRepository,
    private val contentMapper: ContentMapper,
) : ContentService { /* ... */ }
```

- Interfaz `XService` + implementación `AbstractXService` con `@Service`.
- Negocio aquí: validaciones, `NoExists`/`BadRequest`/`AlreadyExists`, paginación.
- Transacciones con `@Transactional` donde aplique.

---

## Repositories

```kotlin
// repositories/ContentRepository.kt
interface ContentRepository : JpaRepository<Content, String> {
    fun findByTmdbId(tmdbId: Long): Content?
}
```

- Solo interfaces `JpaRepository`; queries derivadas o `@Query`.
- Sin DDL: nada de `create/drop/alter` en código ni en CI.

---

## Entities

```kotlin
// entities/Season.kt
@Entity
@Table(name = "seasons")
class Season(
    @Id val id: String,
    @ManyToOne(fetch = FetchType.LAZY) val content: Content,
    // ...
)
```

- Una clase por archivo, `PascalCase`, campos `camelCase`.
- Relaciones `LAZY` por defecto; evitar `EAGER` salvo justificado.

---

## Mappers (MapStruct + kapt)

```kotlin
// mappers/ContentMapper.kt
@Mapper(componentModel = "spring")
interface ContentMapper {
    fun toResponse(entity: Content): ContentResponse
}
```

- Toda conversión entity ↔ dto pasa por el mapper. No mapear a mano en services.
- `MapperUtils.kt` solo para helpers compartidos.

---

## Seguridad

`config/SecurityConfig.kt` — dos configs por perfil:

```kotlin
@Profile("init")   // CloseSecurityConfiguration: arranque cerrado
@Profile("!init")  // JwtSecurityConfiguration: JWT stateless + CORS + AppCustomDsl
```

`config/Security.kt` — filtros `JwtAuthenticationFilter` (`POST /new-api/login`) y
`JwtAuthorizationFilter` + `LoggedUser` (ThreadLocal). No duplicar lógica JWT fuera
de estos archivos.

---

## Manejo de errores

Siempre vía `errors/ErrorHandler.kt` (`@ControllerAdvice`):

```kotlin
throw NoExists("Content abc123 no existe") // → 404
throw BadRequest("title es requerido")     // → 400
throw AlreadyExists("...")                 // → 409
throw ServiceUnavailable("lamovie.org caído") // → 503
```

- El service lanza la excepción tipada; el controller no hace `try/catch`.
- Respuesta uniforme `ApiError(status, message)`.

---

## Checklist

- [ ] Compila: `./gradlew build`
- [ ] Capas respetadas (sin atajos controller→repository)
- [ ] MapStruct para conversiones
- [ ] Errores tipados vía `ErrorHandler`
- [ ] Sin credenciales ni secretos en código
- [ ] Nombres claros en inglés, una clase por archivo
