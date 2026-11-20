# Packages

> Dependencias Gradle de MiTeVe-API (`build.gradle.kts`).

---

## Dependencias principales

| Paquete | Versión | Uso |
| ------- | ------- | --- |
| `spring-boot-starter-data-jpa` | Boot 4.0.3 | JPA + Hibernate (`entities/`, `repositories/`) |
| `spring-boot-starter-webmvc` | Boot 4.0.3 | REST (`controllers/`) |
| `spring-boot-starter-security` | Boot 4.0.3 | Seguridad (`config/`) |
| `spring-security-test` (test) | Boot 4.0.3 | Tests de seguridad |
| `postgresql` | 42.7.10 | Driver Postgres |
| `mapstruct` + `mapstruct-processor` (kapt) | 1.6.3 | `mappers/` entity ↔ dto |
| `jjwt-api/impl/gson` | 0.11.5 | JWT (`Security.kt`) |
| `kotlin-reflect`, `jackson-module-kotlin` | Boot/Kotlin | Soporte Kotlin |
| `kotlin-test-junit5`, `junit-platform-launcher` (test) | — | `./gradlew test` |

---

## Agregar / actualizar

```bash
# editar build.gradle.kts -> dependencies { implementation("...") }
./gradlew build
```

Luego actualizar la tabla de arriba y probar `./gradlew bootRun`.

---

## Reglas

- No bajar versiones de Boot/Kotlin sin issue que lo justifique.
- `kapt("org.mapstruct:mapstruct-processor:...")` debe ir a la par de `mapstruct`.
- Probar `build` + `test` tras cada cambio.
