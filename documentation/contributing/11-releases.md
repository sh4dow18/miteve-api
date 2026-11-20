# Releases

> Cómo se crean releases en GitHub para trazabilidad e histórico.

---

## Crear un release en GitHub

Siempre desde la interfaz de GitHub, nunca por CLI.

Repositorio: [sh4dow18/miteve-api](https://github.com/sh4dow18/miteve-api/releases)

### Paso 1: Sincronizar versión

1. Cambiar a la rama `release/X.Y.Z` (creada desde `main` tras QA).
2. Verificar `version` en `build.gradle.kts` == `api.version` en
   `application.properties` == `GET new-api/utils/version`.
3. `./gradlew build` en verde.

### Paso 2: Crear el release

1. Ir a `Releases` → **Draft a new release**.
2. Crear tag `vX.Y.Z` (ej: `v0.0.4`).
3. En **Target**, seleccionar la rama `release/X.Y.Z` (nunca `main`).
4. Clic en **Generate release notes** y reorganizar por categorías (plantilla abajo).
5. Agregar **fecha de publicación** y dónde está desplegado.
6. **Publish release**.

---

## Tag

| Release | Tag |
| ------- | --- |
| `release/0.0.4` | `v0.0.4` |
| `release/0.0.5` | `v0.0.5` |
| `release/0.0.4-Alpha` | `v0.0.4-Alpha` |

Tags solo desde la UI de GitHub al crear el release, nunca local.

---

## Descripción del release

Plantilla estándar **obligatoria**:

```markdown
**Publicado**: DD/MM/YYYY

**Desplegado en**:
- Staging: DD/MM/YYYY
- Producción: DD/MM/YYYY

## Cambios

### ✨ Nuevas funcionalidades
- Feature/12 content search by @sh4dow18 in #15

### 🐛 Correcciones
- Fix/34 episode 404 by @sh4dow18 in #36

### ♻️ Refactorización
- Refactor/56 split scrape service by @sh4dow18 in #58

**Full Changelog**: https://github.com/sh4dow18/miteve-api/compare/v0.0.3...v0.0.4
```

Categorías por label del PR: `feat/feature/enhancement` → ✨,
`fix/bug` → 🐛, `refactor` → ♻️, `docs` → 📚, `chore/style/test` → 🧹.

---

## Trazabilidad

```text
GitHub Release v0.0.4
        │
        ├── Tag: v0.0.4
        ├── Rama: release/0.0.4
        ├── PRs incluidos: #15, #36, #58
        ├── Descripción: changelog manual
        └── Versión: build.gradle.kts + api.version + GET new-api/utils/version
```

---

## Release Alpha

Sin aprobación de QA: rama `release/X.Y.Z-Alpha`, tag `vX.Y.Z-Alpha`, indicar en la
descripción que no pasó por QA.

```bash
git checkout -b release/0.0.5-Alpha main
git push origin release/0.0.5-Alpha
```

---

## Notas

- Siempre crear el release **antes** de desplegar a producción.
- El tag debe coincidir con `build.gradle.kts`.
- Base del release: siempre `release/X.Y.Z`, nunca `main`.
- Nunca crear releases/tags desde CLI.
