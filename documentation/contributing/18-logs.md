# Logs

> Cómo se registran y consultan los logs en MiTeVe-API.

---

## Destinos

| Destino | Cuándo | Dónde se ve |
| ------- | ------ | ----------- |
| **Consola** | Local (`./gradlew bootRun`) | Terminal + `spring.jpa.show-sql=true` |
| **Plataforma** | Staging/producción | Logs del contenedor/servicio (stdout) |

---

## Consola local

Spring Boot loguea requests, errores de `ErrorHandler` y SQL (modo `show-sql`).
Ejemplo de error uniforme (`errors/ApiError.kt` vía `ErrorHandler.kt`):

```json
{
  "status": "NOT_FOUND",
  "message": "Content abc123 no existe"
}
```

---

## Buenas prácticas

- No loguear secretos (`PASSWORD`, tokens JWT, `UPLOAD_SSH_PASSWORD`).
- Los services lanzan excepciones tipadas (`NoExists`, `BadRequest`,
  `AlreadyExists`, `ServiceUnavailable`); `ErrorHandler` las convierte a `ApiError`
  con el status correcto — no hacer `try/catch` con `println` en controllers.
- En jobs largos (scrape/ffmpeg/upload), loguear inicio/fin con ids para trazabilidad.

---

## Consultar en producción

1. Revisar stdout del servicio (según plataforma de deploy).
2. Filtrar por endpoint (`new-api/contents/...`) y por `message` del `ApiError`.
3. Para bugs, adjuntar el `status` + `body` en el issue (ver `bug_report.yml`).
