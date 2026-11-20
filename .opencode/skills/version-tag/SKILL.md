---
name: version-tag
description: Use when bumping versions in miteve-api for syncing build.gradle.kts plus application.properties plus utils version endpoint and GitHub tags.
license: MIT
compatibility: opencode
---

# Version Tag — Single Responsibility: versioning only

This skill covers ONLY version numbers and tags. It does NOT cover git branch naming or build commands.

## Rules

- Bump in sync (currently `0.0.4` in `build.gradle.kts:11 version = "0.0.4"`):
  1. `version` in `build.gradle.kts:11`.
  2. Matching version key in `src/main/resources/application.properties` (add `app.version=<X.Y.Z>` next to `# API` block if missing — today only `access`, `server.port`, `endpoint.*` exist there).
  3. `GET new-api/utils/version` response (see `controllers/UtilsController.kt` — today only `HEAD new-api/utils/health` exists; if no version handler, create `GET version` returning the synced value before tagging).
- Keep all three identical (`X.Y.Z`). Never bump only Gradle and forget properties/endpoint.
- Tags/releases only from GitHub UI on `release/X.Y.Z`, never local `git tag`. Target `main`/`release/*` checkpoints per `git-flow` (2 approvals).
- Verify with `./gradlew build` after bump (see `gradle-build-run`), then smoke `GET new-api/utils/version` equals `build.gradle.kts` value.

## Out of scope

- Do NOT decide branch names here. See `git-flow`.
- Do NOT change endpoints shape here. See `controller-pattern`.
