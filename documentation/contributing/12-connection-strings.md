# Connection Strings

> Cómo se conecta MiTeVe-API a PostgreSQL (stub adaptado: sin `vimaxcn`, con env vars).

---

## Variables de entorno

La conexión vive en `src/main/resources/application.properties` y se resuelve con
variables de entorno — nunca commitear valores reales:

```properties
spring.datasource.driver-class-name = org.postgresql.Driver
spring.datasource.username = ${USER}
spring.datasource.url = jdbc:postgresql://${DATABASE_IP}:5432/${DATABASE}
spring.datasource.password = ${PASSWORD}
spring.jpa.database-platform = org.hibernate.dialect.PostgreSQLDialect
```

| Variable | Ejemplo local | Descripción |
| -------- | ------------- | ----------- |
| `USER` | `miteve` | Usuario Postgres |
| `DATABASE_IP` | `localhost` | Host (o IP del servidor) |
| `DATABASE` | `miteve_db` | Nombre de la BD |
| `PASSWORD` | `***` | Password (secreto) |
| `PORT` | `8080` | Puerto de la API |
| `IP_DOMAIN` | `http://localhost:3000` | CORS permitido |

---

## Reglas

- Misma información en todos los entornos; solo cambian los valores por entorno.
- Local: Docker o Postgres instalado. Staging/Prod: valores del servidor.
- Si falla la conexión, verificar `DATABASE_IP` (firewall) y credenciales con el
  responsable de BD antes de tocar código.

Ver [Primeros Pasos](02-getting-started.md) y [Preguntas Frecuentes](21-faq.md).
