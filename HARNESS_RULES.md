# Harness Engineering Rules

## Core Principle: Harness-First

Every feature implementation and bug fix **must** verify or build a harness before modifying production code.

1. **Before changing production code**, confirm that mock data, test scripts, or a test framework exists to validate the change.
2. If no harness exists, **build the harness first**, then implement the production change.
3. After changing fixtures in `harness/tests/`, run `python harness/scripts/sync_fixtures.py` before `./gradlew test`.

## Dead-code cleanup (mandatory)

When a design changes, **delete** unused production code, fixtures, tests, and harness scripts in the same change. Do not keep “reference-only” orphans that compete with the live path — they confuse agents and humans later.

## Pre-push verification (required)

```bash
pip install -r harness/requirements.txt
python harness/scripts/sync_fixtures.py
python harness/scripts/test_cushion_bridge_schema.py
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

Local default AVD: **Pixel_8** (`harness/config/emulator.json`).

Canonical start command (via `ensure_emulator.py`):

```text
emulator -avd Pixel_8 -no-snapshot-load
```

`-no-snapshot-load` forces a cold boot (slower; wait up to `boot_timeout_sec`, default **300**). Agents and developers should use this harness before `installDebug` or WebView instrumentation debugging. After boot, confirm network (`eth0` up) before live YTM QA — `ERR_NAME_NOT_RESOLVED` is an emulator DNS/network failure, not an app bug.

## i18n

- UI strings: Korean (`values/strings.xml`) + English (`values-en/strings.xml`)
- **No in-app language menu** — follows the **device system locale** (`AppLocaleStore.getLanguage`)
- DJ LLM prompts and Android TTS follow the resolved system language

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
| `harness/src/` | Optional Python helpers (not shipped) |
| `android/app/src/main/` | Production Kotlin + runtime assets |
| `android/app/src/test/` | JVM unit tests (synced fixtures) |
| `android/app/src/androidTest/` | Instrumentation / WebView fixtures |

## Production–harness boundary

- Do **not** add `mock_*` filenames under `android/app/src/main/`.
- **No fixed song catalog** — runtime cushion is Gemini pool similarity + invented bridge `search_query` only.
- Admin demo schedule: `assets/admin/default_schedule.json` (frozen B2B/Admin scaffold).
- WebView DOM fixtures: `assets/www/fixtures/` (instrumentation only).
- BYOK: **Gemini API key only** in `SecureKeyStore`; never commit secrets.
- Debug live QA: seed from `local.properties` `gemini.api.key` via `DebugByokSeeder` (DEBUG only; gitignored).
- Gate requires a **usable** key (`GeminiApiKeyValidator`) — blank / `test-key-123` / placeholders are rejected. Instrumentation must clear prefs in `@After`.

## Algorithm parity

Runtime LLM cushion (pool pick + invented bridges) changes must pass:

- `harness/scripts/test_cushion_bridge_schema.py`
- `CushionBridgePlanParserTest` / `RadioSchedulerTest` (decisionFromPlan)

AI DJ transition schema changes must pass:

- `harness/scripts/test_llm_response_schema.py` (`mock_llm_response.json`, `mock_dj_transition.json`)
- `DjAudioControlParserTest` / `LlmResponseExtractorTest` (JVM)

Radio request schema changes must pass:

- `harness/scripts/test_user_request_schema.py`
- `UserRequestParserTest` (JVM; includes `parseLocal` edge cases — **not** a production fallback)

BYOK key usability changes must pass:

- `GeminiApiKeyValidatorTest` (JVM)
- Gate / `DebugByokSeeder` / `SecureKeyStore.hasUsableGeminiApiKey`

Inventory: [docs/harness-inventory.md](docs/harness-inventory.md).

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
