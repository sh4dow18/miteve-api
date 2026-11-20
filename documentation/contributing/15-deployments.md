# Deployments

> Cómo se despliega MiTeVe-API en staging y producción.

---

## Entornos

| Entorno | Uso | Base de datos |
| ------- | --- | ------------- |
| Local | Desarrollo (`./gradlew bootRun`) | Docker Postgres |
| Staging | QA (`develop` desplegado) | Postgres staging |
| Producción | `main` / `release/X.Y.Z` | Postgres producción |

---

## Build del artefacto

```bash
./gradlew build
# artefacto: build/libs/miteve-api-<version>.jar
```

La versión sale de `build.gradle.kts` (`version = "0.0.4"`) — ver
[Control de Versiones](10-versioning.md).

---

## Deploy a staging

1. Merge a `develop` (PR aprobado).
2. Desplegar el jar/branch `develop` en el servidor staging.
3. Verificar:
   - `HEAD new-api/utils/health` → `200`
   - `GET new-api/utils/version` → versión esperada
   - `POST new-api/login` emite `Authorization: Bearer ...`
4. QA prueba los criterios del release.

## Deploy a producción

1. QA aprueba en staging.
2. PR `develop` → `main` (2 aprobaciones) → rama `release/X.Y.Z`.
3. Release en GitHub con tag `vX.Y.Z` (ver [Releases](11-releases.md)).
4. Si hay cambio de esquema: aplicar migración con `sh4dow18`
   (ver [Database Migrations](14-database-migrations.md)) **antes** del deploy.
5. Desplegar el jar del release.
6. Verificar health + version + login.

---

## Variables requeridas en el servidor

`USER`, `DATABASE_IP`, `DATABASE`, `PASSWORD`, `PORT`, `IP_DOMAIN`,
`TMDB_API_KEY`, y las de scrape/ffmpeg/upload (`LAMOVIE_*`, `JKANIME_*`,
`UPLOAD_SSH_*`). Nunca commitearlas; van en el entorno del servidor.

---

## Flujo de deploy

```text
develop ──▶ staging ──▶ QA aprueba ──▶ release/X.Y.Z ──▶ producción
                │                           (+ migración BD si aplica)
                └──▶ QA rechaza ──▶ volver a develop
```

---

## Notas

- Todo deploy a producción pasa por QA primero (salvo Alpha declarado).
- Los deploys a producción los coordina `sh4dow18`.
- Ante error de conexión a BD, revisar firewall/IP antes de reintentar.
