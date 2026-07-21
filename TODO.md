# TODO — NarrativeDJ

**Scope:** [docs/project-scope.md](docs/project-scope.md)  
**Roadmap:** [docs/development-plan.md](docs/development-plan.md)  
**Current release:** `0.7.1` — Personal BYOK MVP (manual QA pending)

## Scaffold complete (not E2E)

- [x] Phase 1 scaffold: WebView PoC, SVD, CSP, BYOK, space profiles
- [x] Phase 2 scaffold: Background service, LLM/TTS, ducking, cushion planner
- [x] Phase 3 scaffold: B2B/Admin UI (frozen — see Deferred)
- [x] Harness boundary + emulator harness (`ensure_emulator.py`, Pixel_8)
- [x] Korean-first i18n + chat-style story input
- [x] Scope docs: `project-scope.md`, `development-plan.md`

## In scope — MVP feature completion

See [development-plan.md](docs/development-plan.md) for exit criteria.

### Phase A — Live YTM + WebView control

- [x] `ytm-controller.js` search/play API
- [x] Instrumentation test for search/play on fixture
- [ ] Live YTM manual QA sign-off ([webview_poc_checklist.md](harness/docs/webview_poc_checklist.md))

### Phase B — Cushion playback

- [x] `CushionPlaybackController` + catalog `search_query`
- [x] Plan + Execute UI buttons
- [x] Harness: `mock_cushion_playback.json` + unit test

### Phase C — DJ radio loop

- [x] OpenAI TTS via Web Audio `playSpeechBuffer`
- [x] LLM prompt includes profile + current/target track context
- [x] Post-DJ cushion suggestion refresh

### Phase D — Background + MediaSession

- [x] Notification + MediaSession now-playing metadata
- [x] Service lifecycle cleanup
- [ ] Manual 30 min background QA ([background_qa_checklist.md](harness/docs/background_qa_checklist.md))

### Phase E — Release ready

- [x] Version 0.7.0 bump
- [x] Unsigned release APK (`assembleRelease` — local test build)
- [ ] Signed release APK (local keystore — see [release.md](docs/release.md))
- [ ] Full release checklist manual sign-off ([release.md](docs/release.md))

## Deferred / out of scope

Do not implement unless [project-scope.md](docs/project-scope.md) is revised.

- [ ] Live B2B partner API + stream playback (frozen scaffold)
- [ ] GPS-based commercial venue detection
- [ ] Admin console write/edit + Korean admin HTML
- [ ] `SchedulePlanner` runtime auto profile switch
- [ ] CI/CD release pipeline with signing secrets
- [ ] Full CI emulator matrix API 26–34
- [ ] Desktop client
- [ ] Weather/Time/Trend auto context (research 2.3 STAGE 1)

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
python harness/scripts/ensure_emulator.py
python harness/scripts/run_instrumentation.py
```

See [HARNESS_RULES.md](HARNESS_RULES.md) and [docs/harness-inventory.md](docs/harness-inventory.md).
