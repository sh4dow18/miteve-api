---
name: apply-qa
description: Use when doing strict exhaustive QA in miteve-api on the current branch with API-level tests, no code changes, and project-format report with user-gated actions.
license: MIT
compatibility: opencode
---

# Apply QA — Single Responsibility: probar la rama actual vía API y reportar

This skill covers ONLY QA exhaustiva vía API sobre la rama actual activa para cualquier módulo. It does NOT edit code, run migrations, or switch branches.

## Rules

- Este skill siempre aplica a la rama actual activa. Nunca pruebe otra rama a menos que el usuario lo especifique explícitamente con nombre de rama.
- Antes de cualquier prueba, identificar la PR asociada a la rama (`list_pull_requests` + `pull_request_read`) y leer sus observaciones existentes (`get_comments` + `get_reviews` + `get_review_comments`). Si la PR ya tiene observaciones, el alcance de la pasada es SOLO el recheck de esas observaciones (más regresión mínima del flujo tocado); si no tiene observaciones, el alcance es todo lo del PR. En ambos casos, todo en una sola pasada exhaustiva: planificar la ruta completa al inicio (build → alcance PR → credenciales → recorrido → limpieza → reporte), no pedir confirmaciones intermedias salvo bloqueo real (credenciales, riesgo irreversible no autorizado).
- Prohibido dar reportes intermedios: aunque encuentre errores durante la pasada, no los reporte ni los anuncie en el chat hasta el final; anótelos en silencio y continúe con el recorrido. Solo al cerrar la pasada única entregue el reporte completo.
- Para probar use nivel API, no UI: `curl`/RestAssured contra `http://localhost:${PORT}/new-api/...` + Postgres MCP (`DATABASE_URL`) para verificar estado real. Playwright MCP solo si la prueba exige navegador (el API es server-only; no hay bundle PUBLIC). Flujos: `POST new-api/login` → guardar `Authorization: Bearer`, luego CRUD por módulo (`contents`, `seasons`, `episodes`, `genres`, `containers`, `users`, `profiles`, `continue-watching`, `history`, `comments`, `bug-reports`, `suggested-content-reports`, `roles`, `privileges`, `scrape`, `ffmpeg`, `archive`, `upload`, `utils/health`).
- Estabilidad: tras `./gradlew bootRun`, esperar readiness (`HEAD new-api/utils/health`) antes de probar; aislar cada caso con datos `QA_*` (crear → probar → borrar si hay delete; si no hay delete, documentar rastro). Sobre fixtures compartidos sin vía de restauración, no borrar: documentar como límite.
- No hay usuario admin por defecto. Siempre pida al usuario los usuarios y contraseñas a usar en las pruebas antes de empezar, una sola vez al inicio de la pasada. No inventar credenciales ni saltar re-auth. Perfiles: probar `init` (cerrado) y `!init` (JWT stateless) según alcance; token expirado/ausente/rol indebido deben comportarse sin 500.
- Por cada módulo pruebe flujo completo en una sola pasada: listar + paginación (`page/size`), filtros, validación vacía y límites, crear, leer, actualizar, borrar donde aplique, más acciones propias (recomendar, buscar por título/género, similar, insertar episodios, scrape→ffmpeg→upload→archive) con limpieza. No omitir módulos por parecer solo lectura (400/404/409/503 se verifican igual).
- En cada revisión, actuar además como usuario malicioso: IDs inexistentes/negativos, duplicados (`AlreadyExists` → 409), cuerpos inválidos (`BadRequest` → 400), SQL/XSS en `title`/`description`/`note`, textos larguísimos (límites `@Column(length=1000/500)`), paginación absurda (`size=-1`, `size=99999`), tokens manipulados. Todo rechazo con código correcto vía `ErrorHandler.kt`, nunca 500 sin manejar, nunca secreto en respuesta.
- Registre request/response exactos (método, path `new-api/...`, status, body) + IDs (`content.id`/slug, `tmdbId`, `seasonId`, `episodeId`, `profileId`) + conteos Postgres antes/después. Guarde evidencias en `.playwright-mcp/` solo si hubo navegador.
- En la revisión exigir build sin errores nuevos: `./gradlew build` en verde antes del recorrido API. Para servidores de larga vida use `timeout` grande o el shell los mata a los 120s.
- Shell Windows (PowerShell): no existe `wc` (usar `Measure-Object`), no hay heredoc `<<` (escribir `.py` con `write` y ejecutarlo con `python`, no `python3`).
- Documentar al final lo verificado OK en positivo (paginación, slug, renumerado contenedores, recomendaciones) y los límites no verificables (sin hardware real de transcode masivo; sin spam de proveedores; sin dañar fixtures compartidos) fuera del comentario PR, en el chat.
- Nunca edite el código. Es solo revisión de QA exhaustiva. Sin DDL: `ddl-auto=update` no se toca en QA; cualquier drift se reporta y se rutea a `db-migrations`.

## Reporte y cierre

- Reporte solo errores con: módulo/ruta, pasos, comportamiento encontrado, problema que genera, esperado. Incluya método + path + status + body + IDs exactos.
- Al cerrar la pasada única, entregar en el chat: build (errores/warnings), alcance aplicado (recheck de observaciones o revisión completa), lo verificado módulo por módulo o punto por punto, hallazgos numerados y límites no verificables. Después, entregar el texto exacto y completo del comentario PR en bloque copiable, sin pedir datos que ya se probaron.
- Si hay hallazgos, no los anuncie ni solicite cambios en medio de la pasada; anótelos y continúe. Solo repórtelos al final en el cierre, y espere a que el usuario confirme o quiera agregar algo más que decidió probar manualmente.
- Observaciones PR solo en formato proyecto: un solo comentario general `# Observaciones (DD de Mes del YYYY HH:MMam/pm)`, luego `## Ubicación / Módulo / Funcionalidad` y `### Categoría`, un bullet por observación con los 3 elementos. Nada de texto libre.
- Si hay observaciones, `REQUEST_CHANGES`, nunca apruebe.
- Nunca apruebe directo. Aunque no haya observaciones, siempre pida autorización al usuario para aprobar.
- Publicar el comentario y el review solo con OK explícito del usuario; nunca auto-publicar al terminar la pasada.
- Si el usuario aprueba al final, entonces acepte la PR, haga squash and merge de la PR y borre la rama al haber completado el merge con éxito.

## Out of scope

- Do NOT edit code, migrations, or ports. See `gradle-build-run` (build) and `db-migrations` (DDL owner only).
- Do NOT inventar credenciales ni saltar re-auth.
- Do NOT cambiar de rama. Solo rama actual salvo especificación explícita.
