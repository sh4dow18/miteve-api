# Control de Versiones

> Cómo gestionar y exponer la versión de MiTeVe-API.

---

## Semantic Versioning (SemVer)

Formato `X.Y.Z`:

| Componente | Cuándo cambiar |
| ---------- | -------------- |
| `X` Major | Breaking changes en `new-api/...` |
| `Y` Minor | Nuevos endpoints backwards compatible |
| `Z` Patch | Fixes sin cambio de funcionalidad |

Versiones Alpha (`X.Y.Z-Alpha`) si se sube sin QA.

---

## Dónde vive la versión

| Archivo | Campo | Ejemplo |
| ------- | ----- | ------- |
| `build.gradle.kts` | `version = "0.0.4"` | Fuente de verdad |
| `src/main/resources/application.properties` | `api.version=0.0.4` | Espejo para exponer vía endpoint |
| `GET new-api/utils/version` | Respuesta JSON | `{"version":"0.0.4"}` |

Los tres deben mantenerse sincronizados en cada release.

```kotlin
// build.gradle.kts
version = "0.0.4"
```

```properties
# application.properties
api.version=0.0.4
```

```kotlin
// controllers/UtilsController.kt
@GetMapping("version")
@ResponseBody
fun version(@Value("\${api.version}") version: String) = mapOf("version" to version)
```

> Si `GET new-api/utils/version` aún no existe, crearlo como parte del próximo
> release minor siguiendo [Estilo de Código](08-code-style.md).

---

## Procedimiento para cambiar versión

1. Actualizar `version` en `build.gradle.kts`.
2. Actualizar `api.version` en `application.properties`.
3. Verificar `GET new-api/utils/version` responde la misma.
4. `./gradlew build` en verde.

---

## Relación con ramas de release

| Versión | Rama | Tipo |
| ------- | ---- | ---- |
| `0.0.4` | `release/0.0.4` | Estable |
| `0.0.5` | `release/0.0.5` | Hotfix |
| `0.1.0` | `release/0.1.0` | Estable |
| `0.0.4` | `release/0.0.4-Alpha` | Sin QA |
