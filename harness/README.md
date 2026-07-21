# Harness — Validation assets (not shipped as Python runtime)

Python verification harness for NarrativeDJ algorithms and fixture schemas.
**SSOT for mock JSON:** `harness/tests/`

## Quick verify (run before every production change)

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

## Scripts

| Script | Purpose |
|--------|---------|
| `test_cushion_router.py` | 2-bridge cushion route scenarios (Python reference) |
| `test_selector_dictionary.py` | YT Music selector fallback dictionary schema |
| `test_llm_response_schema.py` | DJ audio-control JSON (`mock_llm_response.json`, `mock_dj_transition.json`) |
| `test_user_request_schema.py` | ▶ Send request parser JSON (`mock_user_request.json`) |
| `test_b2b_schedule_schema.py` | Multi-location admin schedule fixture |
| `verify_release_config.py` | Release signing scaffold present, no keystore in repo |
| `sync_fixtures.py` | Copy `harness/tests/*.json` → `android/.../test/resources/` |
| `ensure_emulator.py` | Start **Pixel_8** AVD if needed; wait for boot |
| `run_instrumentation.py` | `ensure_emulator.py` + `./gradlew connectedDebugAndroidTest` |

## Emulator (`harness/config/emulator.json`)

Default local AVD: **Pixel_8**. Equivalent manual command: `emulator -avd Pixel_8`.

If AVDs are stored outside `%USERPROFILE%\.android\avd`, copy `emulator.local.json.example` → `emulator.local.json` and set `avd_home`.

```bash
python harness/scripts/ensure_emulator.py
python harness/scripts/run_instrumentation.py
```

Use `ensure_emulator.py --check-only` to validate SDK/AVD configuration without launching the emulator.

## Fixtures (`harness/tests/`)

| File | Used by |
|------|---------|
| `mock_tracks.json` | Cushion algorithm Python + Kotlin unit tests |
| `mock_llm_response.json` | LLM audio-control parser tests (legacy fixture shape) |
| `mock_dj_transition.json` | Transition ment audio-control fixture |
| `mock_user_request.json` | Radio ▶ Send parser schema |
| `mock_b2b_schedule.json` | Admin schedule planner tests |
| `mock_cushion_playback.json` | Cushion playback order fixture |

See [docs/harness-inventory.md](../docs/harness-inventory.md) for the full cross-language inventory (cushion, WebView/SVD, AI DJ/BYOK, radio UX, i18n, B2B, release).
