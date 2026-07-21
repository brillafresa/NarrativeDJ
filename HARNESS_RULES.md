# Harness Engineering Rules

## Core Principle: Harness-First

Every feature implementation and bug fix **must** verify or build a harness before modifying production code.

1. **Before changing production code**, confirm that mock data, test scripts, or a test framework exists to validate the change.
2. If no harness exists, **build the harness first**, then implement the production change.
3. After changing fixtures in `harness/tests/`, run `python harness/scripts/sync_fixtures.py` before `./gradlew test`.

## Pre-push verification (required)

```bash
pip install -r harness/requirements.txt
python harness/scripts/sync_fixtures.py
python harness/scripts/test_cushion_router.py
python harness/scripts/test_selector_dictionary.py
python harness/scripts/test_llm_response_schema.py
python harness/scripts/test_user_request_schema.py
python harness/scripts/test_b2b_schedule_schema.py
python harness/scripts/verify_release_config.py
cd android && ./gradlew test
```

Optional (device/emulator):

```bash
python harness/scripts/ensure_emulator.py
python harness/scripts/run_instrumentation.py
```

Local default AVD: **Pixel_8** (`harness/config/emulator.json`). If no device is connected, `ensure_emulator.py` starts `emulator -avd Pixel_8` and waits for boot. Agents and developers should use this harness before `installDebug` or WebView instrumentation debugging.

## i18n

- Default UI language: **Korean** (`values/strings.xml`)
- English: `values-en/strings.xml`, switch via app menu **언어 / Language**
- DJ LLM prompts and Android TTS follow the selected locale

Full inventory: [docs/harness-inventory.md](docs/harness-inventory.md)

## Repository layout

| Path | Role |
|------|------|
| `harness/tests/` | **SSOT** for mock JSON fixtures |
| `harness/scripts/` | Python verification (not app runtime) |
| `harness/config/emulator.json` | Local emulator AVD name and boot settings |
| `harness/config/emulator.local.json` | Machine-specific SDK/AVD paths (gitignored) |
| `docs/project-scope.md` | **Repository goal scope** (read before research.md tasks) |
| `docs/development-plan.md` | MVP Phase A–F execution plan |
| `harness/src/` | Python reference algorithms |
| `android/app/src/main/` | Production Kotlin + runtime assets |
| `android/app/src/test/` | JVM unit tests (synced fixtures) |
| `android/app/src/androidTest/` | Instrumentation / WebView fixtures |

## Production–harness boundary

- Do **not** add `mock_*` filenames under `android/app/src/main/`.
- Demo catalog: `assets/catalog/demo_tracks.json` (from SSOT `mock_tracks.json`).
- Admin demo schedule: `assets/admin/default_schedule.json`.
- WebView DOM fixtures: `assets/www/fixtures/` (instrumentation only).
- BYOK keys and B2B licenses: encrypted local storage only; never commit secrets.

## Algorithm parity

Cushion scheduler changes must pass:

- `harness/scripts/test_cushion_router.py` (Python)
- `android/.../CushionMusicSchedulerTest.kt` (Kotlin)

## Source of truth priority

When context conflicts:

1. **Working source code** (tests passing)
2. **`.cursorrules`** and **this file**
3. **`docs/project-scope.md`** (repository goal scope)
4. **`docs/development-plan.md`**
5. **`docs/research.md`** (full vision — partial adoption only)
6. **`docs/architecture.md` / `docs/features.md`**

## Changelog

Record meaningful harness or scaffolding changes in `CHANGELOG.md`.
