# Configuración de GitHub (.github/)

> Qué hay en `.github/`, para qué sirve cada archivo y cómo está configurado.

---

## Estructura

```text
.github/
├── pull_request_template.md      # Plantilla de PR
├── ISSUE_TEMPLATE/
│   ├── config.yml                 # Bloquea issues vacíos + link a CONTRIBUTING
│   ├── bug_report.yml             # Reportar Error (label Bug)
│   ├── feature_request.yml        # Solicitar Funcionalidad (label Solicitud)
│   └── task.yml                   # Generar Tarea (con checkbox requires DB migration)
└── workflows/
     ├── ci.yml                     # Compila con JDK 24 + Gradle, corre tests
     ├── commitlint.yml             # Valida commits y título del PR
     ├── branch-lint.yml            # Valida nombre de rama
     └── auto-assign.yml            # Asigna reviewer/assignee/labels (team sh4dow18)
```

---

## Plantilla de Pull Request

`.github/pull_request_template.md` — copia exacta del flujo dh-vimax: Historia
(`Como/Quiero/Para`), Descripción, Cambios realizados, Cómo probar, Criterios de
Aceptación, Capturas. Ver [Pull Requests](07-pull-requests.md).

---

## Plantillas de Issues

Nuevo issue → 3 tarjetas. El título va en el campo nativo de GitHub.

### `config.yml`

```yaml
blank_issues_enabled: false
contact_links:
  - name: "📖 Documentación"
    url: "https://github.com/sh4dow18/miteve-api/blob/develop/CONTRIBUTING.md"
    about: "Guía de contribución y documentación del proyecto"
```

### `bug_report.yml` — Reportar Error (`Bug`)

| Campo | Obligatorio | Ejemplo |
| ----- | ----------- | ------- |
| Descripción | Sí | `Al pedir ...` |
| Pasos para reproducir | Sí | `1. GET ...` |
| Comportamiento esperado | Sí | `Debería retornar 200` |
| Comportamiento actual | Sí | Método + path `new-api/...` + status + body |
| Objetivo | Sí (1..n) | Auth, Contents, Seasons, Episodes, Containers, Users, Profiles, Scrape, Ffmpeg, Upload, DB, General |
| Reportado por | Sí | MiTeVe Team, QA, Usuario |
| Capturas / Logs | Sí | Stacktrace, `ApiError` |

### `feature_request.yml` — Solicitar Funcionalidad (`Solicitud`)

| Campo | Obligatorio |
| ----- | ----------- |
| Historia de Usuario (`Como/Quiero/Para`) | Sí |
| Descripción | Sí |
| Criterios de Aceptación | Sí |
| Objetivo | Sí (1..n, mismo catálogo) |
| Reportado por | Sí |
| Mockups (snippet OpenAPI opcional) | No |

### `task.yml` — Generar Tarea (sin label)

| Campo | Obligatorio |
| ----- | ----------- |
| Descripción | Sí |
| Tareas | Sí |
| ¿Requiere migración de BD? (`requires DB migration`) | No (si se marca → label `db-migrations` + [Database Migrations](14-database-migrations.md)) |
| Objetivo | Sí (1..n) |
| Reportado por | Sí |

---

## Workflows

### `ci.yml` — compila y testea

Trigger `push`/`pull_request` en `develop`, `main`. Pasos: checkout → JDK 24
(`temurin`) → Gradle → `chmod +x gradlew` → `./gradlew build` → `./gradlew test`
(`continue-on-error`, warning si no hay tests). **Sin DDL en CI.**

### `commitlint.yml` — commits y título

Igual lógica que dh-vimax: `#<issue> (feat|fix|refactor|docs|style|test|chore): ...`,
correlación `#` rama↔commit, `Docs/*` solo `docs:`, título = rama excepto
checkpoints `develop → main` y `main → release/*`. Se permiten Squash (`Tipo/123-desc (#PR)`)
y `Merge ...` auto-generados.

### `branch-lint.yml` — nombre de rama

Regex `^(Feature|Fix|Refactor|Docs)/[0-9]+-[a-z0-9-]+$`, exentas `main`, `develop`,
`release/*`, `hotfix/*`. Triggers `create`/`push`/`pull_request`.

### `auto-assign.yml` — reviewers/assignee/labels

Al abrir PR (incluye drafts): pide review a `team = ['sh4dow18']` (excepto autor),
asigna el autor como assignee y copia labels del issue `#123` extraído de
`Tipo/123-desc`. No rellenar a mano; el workflow lo pone en segundos. No bloquea si falla.

---

## Resumen rápido

| Archivo | Una frase | Si lo borro... |
| ------- | --------- | -------------- |
| `pull_request_template.md` | Plantilla auto-llenada del PR | PRs vacíos |
| `ISSUE_TEMPLATE/config.yml` | Bloquea blank + link docs | Aparece blank issue |
| `bug_report.yml` | Error (`Bug`) con método/path/status/body | Bugs sin formato |
| `feature_request.yml` | Feature (`Solicitud`) con OpenAPI opcional | Features sin historia |
| `task.yml` | Tarea + checkbox DB migration | Tareas sin formato |
| `workflows/ci.yml` | JDK 24 + build + test | No sabes si compila |
| `workflows/commitlint.yml` | Valida commits y título | Commits malos pasan |
| `workflows/branch-lint.yml` | Valida rama | Ramas malas pasan |
| `workflows/auto-assign.yml` | Asigna reviewer/labels | Asignar a mano |

---

## ¿Quiero cambiar algo? ¿Dónde toco?

| Quiero... | Archivo | Nivel |
| --------- | ------- | ----- |
| Cambiar qué pide el PR | `pull_request_template.md` | Fácil |
| Nuevo tipo de issue | `ISSUE_TEMPLATE/nuevo.yml` | Medio |
| Nuevo robot | `workflows/nuevo.yml` | Avanzado |
| Cambiar quién revisa | `auto-assign.yml` → `team` | Fácil |
| Cambiar regla de commits | `commitlint.yml` → `grep -qE` | Medio |
| Cambiar regla de ramas | `branch-lint.yml` → `grep -qE` | Medio |
| Desactivar un robot | Borrar/renombrar su `.yml` | Fácil |
