# Pull Requests

> Cómo crear, revisar y mergear Pull Requests en MiTeVe-API.

---

## Crear un PR

Desde la interfaz de GitHub:

1. Push de tu rama.
2. `Pull requests` > `New pull request`.
3. Base `develop`, comparar con tu rama.
4. Título = nombre exacto de la rama.
5. Completar la plantilla y crear.

---

## Título del PR

Debe ser **el nombre exacto de la rama**:

```text
Feature/123-content-search
Fix/34-episode-404-on-similar
```

> **Excepción checkpoint**: `develop` → `main` y `main` → `release/*` no siguen
> esta regla (ver `commitlint.yml`, job `pr-title`).

---

## Descripción del PR

Viene pre-llenada por `.github/pull_request_template.md`:

```markdown
# Pull Request

## Historia de Usuario

**Como** <!-- rol -->
**Quiero** <!-- funcionalidad -->
**Para** <!-- beneficio -->

## Descripción

## Cambios realizados

## Cómo probar

## Criterios de Aceptación

## Capturas (si aplica)
```

### Ejemplo

```markdown
# Pull Request

## Historia de Usuario

**Como** usuario con perfil
**Quiero** buscar contenido por título
**Para** encontrar rápido qué ver

## Descripción

Se agregó `GET new-api/contents/search?title=...` con paginación.

## Cambios realizados

- `ContentController.searchByTitle` + `ContentService.searchByTitle`
- Query por título en `ContentRepository`
- Mapeo con `ContentMapper` a `MiniContentResponse`

## Cómo probar

1. `./gradlew bootRun`
2. `GET /new-api/contents/search?title=naruto&page=0&size=5`
3. Verificar `200` y lista paginada

## Criterios de Aceptación

- [ ] Busca parcial, insensible a mayúsculas
- [ ] Respeta `page`/`size`
- [ ] Sin título retorna `400` vía `ErrorHandler`
```

---

## Proceso de review

### 1. Checks automáticos

| Check | Workflow | Qué hace |
| ----- | -------- | -------- |
| CI | `ci.yml` | JDK 24 + `./gradlew build` + `./gradlew test` (warning si vacío) |
| Commitlint | `commitlint.yml` | Commits `#<issue> tipo:` + título = rama |
| Branch Lint | `branch-lint.yml` | Rama `Tipo/123-desc` |
| Auto Assign | `auto-assign.yml` | Reviewer `sh4dow18` + assignee + labels del issue |

### 2. Revisión de código

- `develop`: mínimo **1 aprobación**. `main`/`release/*`: **2 aprobaciones**.
- El reviewer verifica: capas correctas (`controllers → services → repositories`),
  MapStruct en `mappers/`, errores vía `errors/ErrorHandler`, sin DDL en código.

### 3. Feedback

```bash
git add .
git commit -m "#123 fix: address review feedback"
git push origin Feature/123-content-search
```

### 4. Aprobación

Resolver todo el feedback antes del merge.

---

## Observaciones del PR

Formato estándar obligatorio (igual que dh-vimax): un solo comentario general.

```markdown
# Observaciones (DD de Mes del YYYY HH:MMam/pm)
Durante la revisión de los cambios realizados en esta Pull Request se identificaron los siguientes puntos que se deben atender.

## [Ubicación / Módulo / Funcionalidad]

### [Categoría: Experiencia de Usuario (UX) / Diseño / Arquitectura / Funcionalidad / Reglas de Negocio / Traducciones / etc.]

- [Descripción. Qué se encontró, qué problema genera y qué se espera.]

## [Otra Ubicación]

### [Categoría]

- [Descripción.]
```

Categorías típicas en API: Arquitectura (lógica en capa incorrecta),
Funcionalidad, Reglas de Negocio, Código (duplicación, validaciones, `ErrorHandler`).

Reglas: una observación por bullet, siempre los 3 elementos, ser específico
(endpoint, paso para reproducir, criterio que falla). Sin observaciones → aprobar.

---

## Mergear un PR

**Squash and Merge** siempre:

1. Verificar aprobaciones (1 en `develop`, 2 en `main`/`release/*`).
2. `Squash and merge` → confirmar mensaje.
3. Eliminar la rama.

```bash
git checkout develop
git pull origin develop
git branch -d Feature/123-content-search
```

---

## Tamaño del PR

| Tamaño | Líneas | Ejemplo |
| ------ | ------ | ------- |
| Pequeño | 1-100 | Fix de un endpoint |
| Mediano | 100-500 | Feature completa (controller+service+repo+mapper) |
| Grande | 500+ | Refactor de un dominio |

Recomendación: PRs pequeños y enfocados; si crece, dividir.

---

## Errores comunes

| Error | Solución |
| ----- | -------- |
| PR contra rama incorrecta | Cerrar y crear contra `develop` |
| Commits sucios | Squash and Merge |
| PR sin issue | Crear el issue primero |
| PR abandonado | Cerrar y reabrir cuando esté listo |
| Conflictos | Resolver antes del merge |
