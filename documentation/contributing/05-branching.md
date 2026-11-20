# Estrategia de Ramas

> Naming convention y flujo de branching en MiTeVe-API.

---

## Rama base

La rama principal de desarrollo es **`develop`**. Todas las ramas nacen desde `develop`
y se fusionan de vuelta a ella.

```text
develop ─────●─────●─────●─────●─────●─────●─────●──────
               \         /       \         /
                ●───────●         ●───────●
             Feature/123        Fix/456
```

---

## Naming convention

### Formato general

```text
Tipo/<issue>-descripción-corta
```

### Tipos de ramas

| Tipo | Propósito | Ejemplo |
| ---- | --------- | ------- |
| `Feature/` | Nuevas funcionalidades | `Feature/12-content-search` |
| `Fix/` | Corrección de bugs | `Fix/34-episode-404-on-similar` |
| `Refactor/` | Refactorización | `Refactor/56-split-scrape-service` |
| `Docs/` | Solo documentación | `Docs/78-update-contributing` |

> **Nota Docs:** En ramas `Docs/123-...` solo se permiten commits `#123 docs: ...`.

### Reglas de naming

1. **Siempre incluir el número de issue**.
2. **Descripción corta**: máximo 4-5 palabras.
3. **Separador**: guion `-` entre palabras.
4. **Sin caracteres especiales**: solo letras, números y guiones.
5. **Minúsculas**: la descripción va en minúsculas.

### Ejemplos reales del proyecto

```text
Feature/12-content-search
Fix/34-episode-404-on-similar
Refactor/56-split-scrape-service
Docs/78-update-contributing
Fix/90-jwt-expiry-on-upload
```

---

## Crear una rama

```bash
git checkout develop
git pull origin develop
git checkout -b Feature/123-nueva-funcionalidad
git branch # debe mostrar * Feature/123-nueva-funcionalidad
```

---

## Sincronizar con develop

```bash
git checkout develop
git pull origin develop
git checkout Feature/123-nueva-funcionalidad
git merge develop
```

O con rebase:

```bash
git checkout Feature/123-nueva-funcionalidad
git rebase develop
```

---

## Después del merge

```bash
git checkout develop
git pull origin develop
git branch -d Feature/123-nueva-funcionalidad
git push origin --delete Feature/123-nueva-funcionalidad
```

---

## Validación automática

Misma regex que dh-vimax: `^(Feature|Fix|Refactor|Docs)/[0-9]+-[a-z0-9-]+$`:

```bash
^(Feature|Fix|Refactor|Docs)/[0-9]+-[a-z0-9-]+$
# Válido:   Feature/123-content-search
# Inválido: feature/123-search (tipo minúscula), Feature/search (falta 123)
```

| Dónde | Archivo | Cuándo valida |
| ----- | ------- | ------------- |
| Remoto (GitHub) | `.github/workflows/branch-lint.yml` | En cada `create`/`push`/`pull_request` |

Exentas: `main`, `develop`, `release/*`, `hotfix/*`.

El commit se bloquea si el `#` del commit no coincide con el `#` de la rama.
Ver [Commits](06-commits.md).

Renombrar: `git branch -m Vieja Nueva && git push -u origin Nueva && git push origin --delete Vieja`.

## Ramas protegidas

| Rama | Protección |
| ---- | ---------- |
| `develop` | Requiere PR y al menos 1 aprobación |
| `main` | Solo merges desde `develop`, PR + 2 aprobaciones |
| `release/*` | Solo hotfix, PR + 2 aprobaciones |

---

## Errores comunes

| Error | Solución |
| ----- | -------- |
| Crear rama desde main | `git checkout -b Feature/123 develop` |
| Olvidar el número de issue | `git branch -m Feature/desc Feature/123-desc` |
| Conflicto con develop | Merge o rebase de develop primero |
| Push sin upstream | `git push -u origin Feature/123-desc` |
