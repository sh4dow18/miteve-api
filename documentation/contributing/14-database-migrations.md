# Database Migrations

> Cómo se gestionan las migraciones del esquema en MiTeVe-API (Flyway).

---

## Responsable único

| Rol | Responsable | Descripción |
| --- | ----------- | ----------- |
| **DBA / Owner** | **sh4dow18** | Única persona autorizada para aplicar migraciones en staging/producción |

> **Regla crítica**: ningún desarrollador aplica DDL en staging/producción sin
> autorización expresa de `sh4dow18`.

---

## Estado actual y meta

Hoy `application.properties` usa:

```properties
spring.jpa.hibernate.ddl-auto = update
```

**Meta**: salir de `ddl-auto=update` y pasar a **Flyway versionado** +
`ddl-auto=validate`:

```properties
spring.jpa.hibernate.ddl-auto = validate
spring.flyway.enabled = true
spring.flyway.locations = classpath:db/migration
```

Migraciones en `src/main/resources/db/migration/` con formato
`V<versión>__<descripcion>.sql` (ej: `V1__create_contents.sql`).

Hasta completar la migración a Flyway, todo cambio de esquema pasa por el
responsable (revisión del diff de entidades JPA vs BD).

---

## Qué es una migración

Cualquier cambio de **esquema**, no de datos:

| Tipo | Ejemplo |
| ---- | ------- |
| Tablas | Crear/eliminar `contents`, `seasons` |
| Columnas | Agregar `tmdb_id`, cambiar tipo |
| Constraints | FK `season.content_id → contents.id` |
| Índices | Índice por `title`, `tmdb_id` |

Cambios de datos (`INSERT/UPDATE/DELETE`) no son migración de esquema.

---

## Flujo

```text
PR con cambio en entities/ ──▶ marca "requires DB migration"
        │ (task.yml checkbox + label db-migrations)
        ▼
sh4dow18 revisa el diff
        │
        ▼
Aplica migración (Flyway / script revisado) en staging → prod
        │
        ▼
Verifica GET new-api/utils/health + endpoints afectados
```

### Si necesitas un cambio de esquema

1. Describirlo en la issue (tabla, columna, tipo, motivo).
2. Implementar la entidad y probarlo localmente.
3. Marcar el checkbox `requires DB migration` en la task (ruteo a `db-migrations`).
4. Mencionar explícitamente en el PR que requiere migración.
5. Esperar autorización de `sh4dow18`; no aplicar por cuenta propia.

---

## Prohibido sin autorización

- `ALTER/CREATE/DROP TABLE` en staging/producción.
- Cambiar `ddl-auto` en CI o en ramas de feature para "probar".
- Compartir scripts de esquema con terceros.

## Permitido

- Proponer el cambio en issue/PR (entidades, repositories).
- Probar localmente contra Docker/Postgres dev.
- Documentar el cambio en el PR.

---

## Relación con otros procesos

| Proceso | Documento | Relación |
| ------- | --------- | -------- |
| Releases | [Releases](11-releases.md) | Cada release con esquema dispara migración |
| Deployments | [Deployments](15-deployments.md) | La migración se aplica con el deploy |
| Backup | [Database Backup](13-database-backup.md) | Respaldo reciente antes de migrar |

---

## Preguntas frecuentes

### ¿Puedo migrar yo si el responsable no está?

No. Esperar autorización expresa. Si hay urgencia, escalar por el canal del equipo.

### ¿Dónde veo si un release requiere migración?

En la descripción del PR (label `db-migrations`) y en las notas del release. Si el PR
toca `entities/` con tablas/columnas, asumir que sí.
