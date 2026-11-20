# Flujo de Trabajo

> Ciclo completo de desarrollo en MiTeVe-API.

---

## Diagrama del flujo de desarrollo

```text
┌────────┐    ┌────────┐    ┌────────┐    ┌────────┐    ┌────────┐    ┌────────┐    ┌────────┐    ┌────────┐
│ Issue  │───▶│ Branch │───▶│  Code  │───▶│ Commit │───▶│  Push  │───▶│   PR   │───▶│ Review │───▶│ develop│
│(tarea) │    │(crear) │    │(desarr)│    │(guardar)│   │(subir) │    │(crear) │    │(revisar)│   │(actual)│
└────────┘    └────────┘    └────────┘    └────────┘    └────────┘    └────────┘    └────────┘    └────────┘
```

---

## Diagrama del flujo de release

```text
                          develop
                              │
                              │ (cada 2-3 semanas)
                              ▼
                          PR a main
                              │
                              ▼
                     ┌───────────────────┐
                     │  QA contra staging│
                     │  (API desplegada)│
                     └────────┬──────────┘
                              │
               ┌──────────────┴──────────────┐
               │                             │
               ▼                             ▼
         ┌───────────┐                ┌────────────┐
         │ Aprobado  │                │ No aprobado│
         └─────┬─────┘                └─────┬──────┘
               │                             │
               ▼                             │
      ┌─────────────────┐                    │
      │ release/X.Y.Z   │                    │
      └────────┬────────┘                    │
               │                             │
               ▼                             │
         ┌───────────┐                       │
         │  Release  │                       │
         └───────────┘                       │
                                             │
                ┌────────────────────────────┘
                │
                ▼
       Volver a develop para corregir
       y repetir el ciclo
```

---

## Flujo de hotfix

```text
Si hay error en producción:
        │
        ├─── Release NO es el último ──▶ Crear rama desde release/X.Y.Z
        │
        └─── Release ES el último ─────▶ Crear rama desde main
                                              │
                                              ▼
                                     ┌─────────────────┐
                                     │ hotfix/X.Y.(Z+1)│
                                     └────────┬────────┘
                                              │
                                              ▼
                                     ┌──────────────┐
                                     │ QA aprueba   │
                                     └──────┬───────┘
                                            │
                                            ▼
                                   ┌─────────────────┐
                                   │ release/X.Y.(Z+1)│
                                   └─────────────────┘
```

---

## Pasos detallados

### 1. Issue

Todo trabajo comienza con un issue en GitHub Projects.

- **Bugs**: pasos para reproducir + método/path `new-api/...` + status + body
- **Features**: historia `Como/Quiero/Para` + criterios
- **Tasks**: descripción + subtareas (+ checkbox `requires DB migration` si aplica)

### 2. Branch

Crear rama desde `develop`:

```bash
git checkout develop
git pull origin develop
git checkout -b Feature/123-content-search
```

Ver [Estrategia de Ramas](05-branching.md).

### 3. Code

Desarrollar siguiendo capas `controllers → services → repositories → entities`
(+ `mappers`/`dtos`). Ejemplo: nuevo filtro en `ContentController.kt` →
`ContentService` → `ContentRepository` → `ContentMapper`.

### 4. Commit

```bash
git add .
git commit -m "#123 feat: add content search endpoint"
```

Ver [Convenciones de Commits](06-commits.md).

### 5. Push

```bash
git push origin Feature/123-content-search
```

### 6. Pull Request

Crear PR contra `develop` desde GitHub.

- **Título**: nombre de la rama (ej: `Feature/123-content-search`)
- **Descripción**: completar `.github/pull_request_template.md`
  (Historia, Descripción, Cambios, Cómo probar, Criterios, Capturas)

Ver [Pull Requests](07-pull-requests.md).

### 7. Review

- Checks automáticos en verde (CI, Commitlint, Branch Lint — ver
  [Configuración de GitHub](19-github-config.md))
- Al menos 1 aprobación para `develop`, 2 para `main`/`release/*`
- `auto-assign.yml` asigna reviewer/assignee/labels solo

### 8. Merge

- Usar **Squash and Merge**
- Eliminar la rama después del merge

---

## Ramas principales

| Rama | Propósito |
|------|-----------|
| `main` | Última versión estable aprobada por QA |
| `develop` | Rama base para desarrollo activo |
| `Feature/*` | Nuevas funcionalidades |
| `Fix/*` | Corrección de bugs |
| `Refactor/*` | Refactorización de código |
| `Docs/*` | Cambios en documentación |
| `release/*` | Rama de release (trazabilidad e histórico) |
| `hotfix/*` | Corrección urgente en producción |

---

## Flujo rápido (resumen)

```bash
# 1. Actualizar develop
git checkout develop && git pull

# 2. Crear rama
git checkout -b Feature/123-descripcion

# 3. Desarrollar y commitear
git add . && git commit -m "#123 feat: add new feature"

# 4. Push
git push origin Feature/123-descripcion

# 5. Crear PR en GitHub contra develop

# 6. Después del merge, eliminar rama
git branch -d Feature/123-descripcion
```
