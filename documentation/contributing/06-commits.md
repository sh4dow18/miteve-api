# Convenciones de Commits

> Formato, tipos y buenas prácticas para mensajes de commit.

---

## Formato del mensaje

```text
#<issue> <tipo>: <descripción>
```

### Ejemplos

```text
#12 feat: add content search endpoint
#34 fix: resolve episode 404 on similar
#56 refactor: split scrape service
#78 docs: update contributing guidelines
```

---

## Tipos de commit

| Tipo | Propósito | Ejemplo |
| ---- | --------- | ------- |
| `feat` | Nueva funcionalidad | `#12 feat: add content search endpoint` |
| `fix` | Corrección de bug | `#34 fix: resolve episode 404 on similar` |
| `refactor` | Refactorización sin cambio de comportamiento | `#56 refactor: split scrape service` |
| `docs` | Cambios en documentación | `#78 docs: update contributing guidelines` |
| `style` | Formato (no afecta código) | `#79 style: apply ktfmt` |
| `test` | Agregar o modificar tests | `#80 test: add content service test` |
| `chore` | Mantenimiento | `#81 chore: bump spring boot` |

---

## Reglas

### 1. Siempre incluir el número de issue

**Mal:** `feat: add search` — **Bien:** `#12 feat: add search`

### 2. Un commit = un cambio lógico

**Mal:** `#12 feat: add search and fix upload bug`
**Bien:** `#12 feat: add search` + `#13 fix: resolve upload bug`

### 3. Mensaje descriptivo

**Mal:** `#34 fix: change code` — **Bien:** `#34 fix: resolve episode 404 on similar`

### 4. En inglés

Los mensajes van en inglés para consistencia.

### 5. Sin punto final

**Mal:** `#12 feat: add search.` — **Bien:** `#12 feat: add search`

### 6. Minúsculas después del tipo (recomendado)

```text
#12 feat: add content search endpoint
```

---

## Validación automática

| Dónde | Archivo | Qué valida |
| ----- | ------- | ---------- |
| Remoto (GitHub) | `.github/workflows/commitlint.yml` | Formato + correlación rama↔commit; permite `Fix/123-... (#PR)` y `Merge ...` de Squash |

En `Fix/123-tarea` solo se permiten commits `#123 ...`. En `Docs/123-...` solo `docs: ...`.

---

## Qué NO commitear

| Archivo / Carpeta | Razón |
| ----------------- | ----- |
| `application.properties` local con credenciales | Secretos |
| `.env` | Secretos |
| `build/` | Artefactos Gradle |
| `.gradle/` | Caché local |
| `.idea/` | Preferencias IDE |
| `*.log` | Logs locales |
| `downloads/` | Archivos scrape/ffmpeg locales |

---

## Verificar antes de commitear

- [ ] `./gradlew build` compila sin errores
- [ ] Sin warnings nuevos
- [ ] Formato Conventional Commits con `#issue`
- [ ] Sin archivos sensibles
- [ ] Commit atómico (un solo cambio lógico)
