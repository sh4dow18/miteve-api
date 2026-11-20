# Database Backup

> Cómo generar un respaldo de PostgreSQL (stub: `pg_dump`, no `.bak`).

---

## Respaldo con pg_dump

```bash
pg_dump -h $DATABASE_IP -U $USER -d $DATABASE -F c -f miteve_$(date +%F).dump
```

Restaurar:

```bash
pg_restore -h $DATABASE_IP -U $USER -d $DATABASE miteve_2026-11-20.dump
```

---

## Reglas

- Antes de cada migración ([Database Migrations](14-database-migrations.md)), tener
  un respaldo reciente por si se requiere rollback.
- Guardar el archivo fuera del repo (nunca commitear dumps).
- Para clientes, coordinar ventana de respaldo con el responsable de BD.
