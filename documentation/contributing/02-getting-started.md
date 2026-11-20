# Primeros Pasos

> Cómo clonar, configurar y ejecutar MiTeVe-API.

---

## Clonar el repositorio

```bash
git clone https://github.com/sh4dow18/miteve-api.git
cd miteve-api
```

Cambiar a la rama de desarrollo:

```bash
git checkout develop
```

---

## Configurar variables de entorno

La API lee la conexión desde `src/main/resources/application.properties`:

```properties
spring.datasource.username = ${USER}
spring.datasource.url = jdbc:postgresql://${DATABASE_IP}:5432/${DATABASE}
spring.datasource.password = ${PASSWORD}
server.port=${PORT}
```

Define en tu shell (no commitear):

```bash
export USER=tu_usuario
export DATABASE_IP=localhost
export DATABASE=miteve_db
export PASSWORD=tu_password
export PORT=8080
export IP_DOMAIN=http://localhost:3000
```

En Windows (PowerShell):

```powershell
$env:USER="tu_usuario"
$env:DATABASE_IP="localhost"
$env:DATABASE="miteve_db"
$env:PASSWORD="tu_password"
$env:PORT="8080"
```

---

## Levantar Postgres con Docker (opcional)

```bash
docker run --name miteve-postgres \
  -e POSTGRES_USER=tu_usuario \
  -e POSTGRES_PASSWORD=tu_password \
  -e POSTGRES_DB=miteve_db \
  -p 5432:5432 -d postgres:15
```

---

## Ejecutar la aplicación

```bash
./gradlew bootRun
```

La API queda en `http://localhost:8080` (o el `$PORT` definido). Prefijo de endpoints:
`new-api/...` (ver `access=new-api` en `application.properties`).

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

Probar:

```bash
curl http://localhost:8080/new-api/contents/abc123
```

---

## Estructura del proyecto

```text
src/main/kotlin/sh4dow18/miteve_api/
├── controllers/      (19 REST: Archive, BugReport, Comment, Container, Content,
│                      ContinueWatching, Episode, Ffmpeg, Genre, History, Privilege,
│                      Profile, Role, Scraping, Season, SuggestedContentReport,
│                      Upload, User, Utils)
├── services/         (lógica: content/, episode/, season/, scrape/, ffmpeg/, ...)
├── repositories/     (Spring Data JPA: ContentRepository, EpisodeRepository, ...)
├── entities/         (JPA: Content, Season, Episode, User, Profile, ...)
├── mappers/          (MapStruct: ContentMapper, EpisodeMapper, ...)
├── dtos/             (requests/responses por dominio)
├── config/           (Security.kt, SecurityConfig.kt)
└── errors/           (ErrorHandler, ApiError, NoExists, BadRequest, ...)
src/main/resources/
└── application.properties
```

---

## Perfiles de seguridad

`config/SecurityConfig.kt` define dos perfiles:

| Perfil | Clase | Uso |
| ------ | ----- | --- |
| `init` | `CloseSecurityConfiguration` | Arranque/semilla: todo autenticado |
| `!init` | `JwtSecurityConfiguration` | Normal: JWT stateless + `JwtAuthenticationFilter` (`POST /new-api/login`) |

```bash
./gradlew bootRun --args='--spring.profiles.active=init'   # semilla
./gradlew bootRun                                            # normal (JWT)
```

---

## Verificar que funciona

1. `./gradlew build` compila sin errores.
2. `curl http://localhost:8080/new-api/contents?page=0&size=5` responde `200`.
3. `POST /new-api/login` con email/password devuelve header `Authorization: Bearer ...`.

---

## Problemas comunes al iniciar

| Problema | Solución |
| -------- | -------- |
| `Connection to localhost:5432 refused` | Revisar `DATABASE_IP` y que Postgres/Docker esté arriba |
| `USER/DATABASE_IP must not be null` | Exportar variables de entorno antes de `bootRun` |
| Puerto en uso | Cambiar `$PORT` o liberar el puerto |
| Beans de seguridad duplicados | Activar un solo perfil: `init` o default, no ambos |
