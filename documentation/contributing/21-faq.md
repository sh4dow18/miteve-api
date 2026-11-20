# Preguntas Frecuentes

> Problemas comunes y soluciones en MiTeVe-API.

---

## Build

### `./gradlew build` falla: toolchain JDK 24 no encontrado

```bash
java -version        # debe ser 24
./gradlew --version  # verifica toolchain
```

Instalar JDK 24 (Temurin) y reintentar.

### `permission denied: ./gradlew` (Linux/CI)

```bash
chmod +x gradlew
```

CI ya lo hace solo.

### Error de `kapt` / MapStruct

Verificar que `mapstruct` y `mapstruct-processor` (kapt) tienen la misma versión
(1.6.3) y recompilar limpio:

```bash
./gradlew clean build
```

---

## Base de datos

### `Connection to localhost:5432 refused`

1. Verificar `DATABASE_IP` y que Postgres/Docker esté arriba.
2. `docker ps` / revisar firewall si es IP remota.

### `USER/DATABASE_IP must not be null` o placeholders sin resolver

Exportar variables antes de correr (ver [Primeros Pasos](02-getting-started.md)).

### Cambio de esquema no se aplica

Cualquier DDL lo coordina `sh4dow18`. Ver [Database Migrations](14-database-migrations.md).
No tocar `ddl-auto` en local para "forzarlo" sin avisar.

---

## Seguridad / Auth

### `POST new-api/login` retorna 403/401

- Verificar perfil activo (`init` cierra todo; `!init` usa JWT).
- Verificar email/password y que el usuario exista con privilegios.
- Revisar `Authorization: Bearer ...` en requests siguientes.

### CORS bloquea al frontend

`SecurityConfig.kt#corsConfigurationSource` permite `localhost:3000/3001`,
`localhost:${port}` e `IP_DOMAIN`. Agregar el origen ahí, no con `@CrossOrigin`
sueltos (los controllers ya traen los de web por defecto).

---

## Git y GitHub

### Push rechazado a `main` / `release/*`

Son ramas protegidas: crear PR (1 aprobación `develop`, 2 en `main`/`release/*`).

### Commit rechazado por formato

Usar `#<issue> <tipo>: <desc>` en inglés, tipos
`feat|fix|refactor|docs|style|test|chore`. El `#` debe coincidir con la rama.
Ver [Commits](06-commits.md).

### Título del PR inválido

El título debe ser exactamente la rama (excepto checkpoints `develop → main`,
`main → release/*`). Ver [Pull Requests](07-pull-requests.md).

---

## Tests

### CI en amarillo: tests vacíos

`ci.yml` permite `test` en warning por ahora. Al agregar tests JUnit5, correr:

```bash
./gradlew test
```

---

## Versiones

### `GET new-api/utils/version` no coincide con `build.gradle.kts`

Actualizar `api.version` en `application.properties` al mismo valor y redesplegar.
Ver [Control de Versiones](10-versioning.md).

---

## Dónde buscar ayuda

| Recurso | URL |
| ------- | --- |
| Issues | https://github.com/sh4dow18/miteve-api/issues |
| Documentación | [Conocer Más](22-learn-more.md) |
| Spring Boot | https://docs.spring.io/spring-boot |
| Kotlin | https://kotlinlang.org/docs |
