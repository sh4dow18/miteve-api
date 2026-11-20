# Requisitos

> Herramientas, versiones y configuraciones necesarias para trabajar en MiTeVe-API.

---

## Herramientas obligatorias

| Herramienta | Versión mínima | Propósito |
| ----------- | -------------- | --------- |
| JDK | 24 (toolchain `JavaLanguageVersion.of(24)`) | Compilar y ejecutar la API (CI usa `setup-java` 24) |
| Gradle Wrapper | `./gradlew` (Kotlin DSL) | Compilar, testear y correr (`build.gradle.kts`) |
| Git | Última versión | Control de versiones |
| PostgreSQL | 15+ | Base de datos (`org.postgresql:postgresql:42.7.10`) |
| Docker | Última versión | Levantar Postgres local para desarrollo |

---

## Herramientas recomendadas

| Herramienta | Propósito |
| ----------- | --------- |
| **IntelliJ IDEA** | IDE principal (Kotlin + Spring Boot) |
| **DataGrip / DBeaver** | Gestión de PostgreSQL |
| **Postman / Bruno** | Pruebas de endpoints `new-api/...` |
| **OpenCode** | Agente de IA para desarrollo (recomendado) |

---

## Alternativas aceptables

| Herramienta | Uso |
| ----------- | --- |
| VS Code + Kotlin | Alternativa gratuita a IntelliJ |
| pgAdmin | Alternativa a DataGrip para PostgreSQL |
| GitHub Copilot | Alternativa a OpenCode como agente IA |

---

## Stack del proyecto

| Pieza | Versión | Archivo |
| ----- | ------- | ------- |
| Kotlin | 2.2.21 | `build.gradle.kts` |
| Spring Boot | 4.0.3 | `build.gradle.kts` |
| JPA / Hibernate | gestionado por Boot | `src/main/kotlin/.../entities/` |
| MapStruct | 1.6.3 (+ `kapt`) | `src/main/kotlin/.../mappers/` |
| JJWT | 0.11.5 (`jjwt-api/impl/gson`) | `config/Security.kt` |
| Spring Security | gestionado por Boot | `config/SecurityConfig.kt` |

---

## Permisos en el repositorio

| Rol | Permisos |
| --- | -------- |
| **Programador** | Clonar, push, crear PRs, aprobar PRs, issues, gestión de ramas |
| **Administrador** | Todo lo anterior + settings, colaboradores, branch protection |

---

## Acceso a base de datos

PostgreSQL centralizado para desarrollo. No se instala SQL Server. Variables de entorno
(ver [Primeros Pasos](02-getting-started.md)):

```bash
USER=tu_usuario
DATABASE_IP=10.10.20.x
DATABASE=miteve_db
PASSWORD=tu_password
PORT=8080
```

> Nunca commitear credenciales reales. Usar variables de entorno o `application.properties`
> local sin trackear.

---

## Configuración de Git

```bash
git --version
git config user.name "Tu Nombre"
git config user.email "tu.email@miteve.app"
```

---

## Verificación de entorno

- [ ] JDK 24 instalado (`java -version`)
- [ ] `./gradlew --version` responde
- [ ] Docker corriendo (`docker ps`)
- [ ] Variables `USER/DATABASE_IP/DATABASE/PASSWORD/PORT` definidas
- [ ] Postman/Bruno instalado
- [ ] Tienes acceso al repositorio en GitHub
- [ ] Git configurado con tu identidad
