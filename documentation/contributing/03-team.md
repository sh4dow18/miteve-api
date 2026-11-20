# Equipo de Desarrollo

> Colaboradores, roles y responsabilidades en MiTeVe-API.

---

## Estructura del equipo

**MiTeVe Team** es dueño de MiTeVe-API y responsable de su desarrollo, mantenimiento y
mejora continua. La API alimenta a `miteve-web` y `miteve-apk`.

---

## Colaboradores activos

| Usuario GitHub | Rol |
| -------------- | --- |
| `@sh4dow18` | Owner / Desarrollador Full Stack + DBA responsable (migraciones) |

> Equipo mínimo por ahora: `auto-assign.yml` asigna a `sh4dow18` como reviewer.
> Al crecer el equipo, agregar filas aquí y ampliar el array `team` en el workflow.

---

## Áreas de responsabilidad

Los desarrolladores trabajan en todas las capas
(`controllers` → `services` → `repositories` → `entities` + `mappers`/`dtos`).

### Excepción: cambios en base de datos

Cualquier cambio de esquema (tablas, columnas, constraints, índices) requiere
aprobación del responsable de BD (ver [Database Migrations](14-database-migrations.md)).
Esto incluye:

- Nuevas entidades o columnas
- Cambios en constraints o índices
- Scripts Flyway (`src/main/resources/db/migration/`)

---

## Metodología de trabajo

Kanban en GitHub Projects. Sin ceremonias Scrum.

1. Las tareas las crean el MiTeVe Team o QA.
2. Se trabajan en orden de prioridad.
3. Todo cambio pasa por issue → rama → PR → review → merge a `develop`.

---

## Proceso de onboarding

1. **Acceso**: solicitar acceso al administrador (`sh4dow18`).
2. **Herramientas**: instalar lo verificado en [Requisitos](01-requirements.md).
3. **Clonar y ejecutar**: seguir [Primeros Pasos](02-getting-started.md).
4. **Conocer el flujo**: leer [Flujo de Trabajo](04-workflow.md) y [Estrategia de Ramas](05-branching.md).
5. **Estilo**: familiarizarse con [Estilo de Código](08-code-style.md).
6. **Primer task**: empezar con una tarea asignada por el MiTeVe Team.
