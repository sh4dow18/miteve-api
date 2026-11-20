# Conocer Más

> Fin de la información general. Aquí sigue la documentación técnica avanzada.

Si llegaste hasta aquí ya revisaste [CONTRIBUTING.md](../../CONTRIBUTING.md) y las
guías de [contributing](.). Ahora sigue la documentación técnica avanzada.

---

## Documentación técnica por dominio

| Dominio | Endpoints (`new-api/...`) | Archivos clave |
| ------- | ------------------------- | -------------- |
| Auth | `login`, `users`, `roles`, `privileges` | `config/Security.kt`, `config/SecurityConfig.kt`, `services/config/AppUserDetailsService.kt` |
| Contents | `contents` | `controllers/ContentController.kt`, `services/content/`, `mappers/ContentMapper.kt` |
| Seasons | `seasons` | `controllers/SeasonController.kt`, `services/season/` |
| Episodes | `episodes` | `controllers/EpisodeController.kt`, `services/episode/`, `services/batch_update/` |
| Containers | `containers` | `controllers/ContainerController.kt`, `services/container/` |
| Users / Profiles | `users`, `profiles`, `continue-watching`, `history` | `controllers/UserController.kt`, `controllers/ProfileController.kt` |
| Scrape | `scrape` | `controllers/ScrapingController.kt`, `services/scrape/` |
| Ffmpeg | `ffmpeg`, `archive` | `controllers/FfmpegController.kt`, `services/ffmpeg/` |
| Upload | `upload` | `controllers/UploadController.kt`, `services/upload/` |
| Reportes | `bug-reports`, `suggested-content-reports`, `comments`, `genres` | Controllers + services homónimos |
| Utils | `utils` | `controllers/UtilsController.kt` (`health`, `version`) |

---

## Estructura de carpetas (futura `learn-more/`)

```text
documentation/learn-more/
├── auth/            # JWT, perfiles init/!init, LoggedUser
├── contents/        # Contents + Seasons + Episodes
├── scrape-ffmpeg/   # Scrape, Ffmpeg, Upload, batch-update
└── ops/             # Deployments, migraciones Flyway, backups
```

*Pendiente de documentación detallada por dominio. Al crearla, enlazarla desde
esta tabla.*

---

## Agregar nueva documentación

1. Crear el archivo en `documentation/learn-more/...`.
2. Actualizar la tabla de arriba.
3. Actualizar el enlace en `CONTRIBUTING.md` si aplica.
