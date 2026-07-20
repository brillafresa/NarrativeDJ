# Pre-push verification (Harness-First)

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

See [HARNESS_RULES.md](HARNESS_RULES.md) and [docs/harness-inventory.md](docs/harness-inventory.md).
