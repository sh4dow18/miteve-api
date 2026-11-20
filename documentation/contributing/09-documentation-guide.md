# Guía de Documentación

> Cómo y cuándo documentar cambios en MiTeVe-API.

---

## Tipos de documentación

| Tipo | Ubicación | Propósito |
| ---- | --------- | --------- |
| Guías contributing | `documentation/contributing/` | Procesos de desarrollo |
| Documentación técnica | `documentation/learn-more/` | Detalles técnicos del sistema |
| README | `README.md` | Descripción general |

---

## Cuándo documentar

### Siempre documentar

- Nuevos endpoints `new-api/...` (controller + service + repo + mapper)
- Cambios en `Security.kt` / `SecurityConfig.kt`
- Nuevas entidades o cambios de esquema (con nota de migración)
- Nuevos jobs (scrape, ffmpeg, upload, batch-update)
- Configuración nueva en `application.properties`

### No documentar

- Fixes triviales
- Ajustes de formato

> Ante cada cambio en el código, la documentación debe actualizarse.

---

## Formato de un documento de endpoint

```markdown
# GET new-api/contents/search

> Busca contenido por título con paginación.

## Request

| Param | Tipo | Descripción |
| ----- | ---- | ----------- |
| title | query, requerido | Texto a buscar |
| page | query, default 0 | Página |
| size | query, default 20 | Tamaño |

## Response

- `200`: `Page<MiniContentResponse>`
- `400`: `ApiError` (falta title, vía ErrorHandler)

## Archivos

| Archivo | Rol |
| ------- | --- |
| `controllers/ContentController.kt` | `searchByTitle` |
| `services/content/` | Lógica + validación |
| `repositories/ContentRepository.kt` | Query |
| `mappers/ContentMapper.kt` | Entity → DTO |
```

---

## Imágenes

Evitar imágenes para mantener el repo ligero. Solo cuando sea estrictamente
necesario (diagramas de arquitectura, flujos). Comprimir al máximo.

---

## Agregar documentación nueva

1. Determinar el tipo (`contributing/` vs `learn-more/`).
2. Crear el archivo con el formato indicado.
3. Actualizar el índice en `CONTRIBUTING.md` y `22-learn-more.md` si aplica.
4. Verificar que los links relativos funcionen.
