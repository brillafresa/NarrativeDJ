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
python harness/scripts/test_b2b_schedule_schema.py
python harness/scripts/verify_release_config.py
cd android && ./gradlew test
```

Optional (device/emulator): `cd android && ./gradlew connectedDebugAndroidTest`

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
3. **`docs/research.md`**
4. **`docs/architecture.md` / `docs/features.md`**

## Changelog

Record meaningful harness or scaffolding changes in `CHANGELOG.md`.
