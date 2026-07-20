# TODO — NarrativeDJ

**Current release:** `0.6.0` — Phase 1–3 MVP + Korean i18n + harness boundary clean

## Completed

- [x] Phase 1: WebView PoC, SVD, CSP, BYOK, space profiles
- [x] Phase 2: Background playback, LLM/TTS, ducking, cushion integration
- [x] Phase 3: B2B plugin, admin console, release scaffold
- [x] Harness boundary cleanup (`sync_fixtures.py`, inventory docs)
- [x] Korean-first UI + language setting (English optional)
- [x] Multiline chat-style story input
- [x] Emulator manual QA + instrumentation harness fixes (HackTimer fallback, SVD selector priority)

## Future enhancements

- [ ] Live B2B partner API integration (replace mock stream fallback)
- [ ] GPS-based commercial venue detection
- [ ] Admin console write/edit schedules + Korean admin HTML
- [ ] CI/CD release pipeline with signing secrets
- [ ] Full `connectedAndroidTest` green on CI emulator matrix

## Pre-push verification (Harness-First)

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

Optional (device/emulator):

```bash
cd android && ./gradlew connectedDebugAndroidTest
```

See [HARNESS_RULES.md](HARNESS_RULES.md) and [docs/harness-inventory.md](docs/harness-inventory.md).
