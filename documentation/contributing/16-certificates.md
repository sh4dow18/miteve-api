# Certificados

> Stub adaptado: MiTeVe-API no usa certificados cliente/servidor propios; la
> autenticación es JWT (ver abajo). Sin gestión de `.pfx`/IIS aquí.

---

## Autenticación JWT (equivalente funcional)

En lugar de certificados, la API usa JWT emitido en `POST /new-api/login`
(`config/Security.kt`):

- `JwtAuthenticationFilter` valida email/password y firma el token HS256.
- `JwtAuthorizationFilter` valida `Authorization: Bearer ...` por request.
- `LoggedUser` (ThreadLocal) expone el user id del token en el hilo actual.
- CORS permitido: `http://localhost:3000`, `https://miteve.vercel.app`
  (`SecurityConfig.kt#corsConfigurationSource`).

TLS termina en el reverse proxy / plataforma de despliegue, no en la app.

---

## Si algún día se requiere mTLS

1. Crear issue con objetivo `Auth` describiendo el caso.
2. Documentar aquí el flujo (emisión, rotación, revocación).
3. No commitear llaves: solo referencias a secretos del entorno.
