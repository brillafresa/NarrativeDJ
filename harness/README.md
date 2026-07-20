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
python harness/scripts/test_b2b_schedule_schema.py
python harness/scripts/verify_release_config.py
cd android && ./gradlew test
```

## Scripts

| Script | Purpose |
|--------|---------|
| `test_cushion_router.py` | 2-bridge cushion route scenarios (Python reference) |
| `test_selector_dictionary.py` | YT Music selector fallback dictionary schema |
| `test_llm_response_schema.py` | DJ audio-control JSON from LLM |
| `test_b2b_schedule_schema.py` | Multi-location admin schedule fixture |
| `verify_release_config.py` | Release signing scaffold present, no keystore in repo |
| `sync_fixtures.py` | Copy `harness/tests/*.json` → `android/.../test/resources/` |

## Fixtures (`harness/tests/`)

| File | Used by |
|------|---------|
| `mock_tracks.json` | Cushion algorithm Python + Kotlin unit tests |
| `mock_llm_response.json` | LLM response parser tests |
| `mock_b2b_schedule.json` | Admin schedule planner tests |

See [docs/harness-inventory.md](../docs/harness-inventory.md) for the full cross-language inventory (cushion, WebView/SVD, AI DJ/BYOK, i18n, B2B, release).
