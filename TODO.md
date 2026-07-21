# TODO — NarrativeDJ

**Scope:** [docs/project-scope.md](docs/project-scope.md)  
**Roadmap:** [docs/development-plan.md](docs/development-plan.md)  
**Current release:** `0.8.1` — Radio messenger UX (manual YTM QA pending)

## Scaffold complete (not E2E)

- [x] Phase 1 scaffold: WebView PoC, SVD, CSP, BYOK, space profiles
- [x] Phase 2 scaffold: Background service, LLM/TTS, ducking, cushion planner
- [x] Phase 3 scaffold: B2B/Admin UI (frozen — see Deferred)
- [x] Harness boundary + emulator harness (`ensure_emulator.py`, Pixel_8)
- [x] Korean-first i18n + chat-style story input
- [x] Scope docs: `project-scope.md`, `development-plan.md`

## In scope — MVP feature completion

### Phase F — Radio messenger UX (v0.8.0)

- [x] Single ▶ send control (Plan/Play/DJ removed)
- [x] LLM/local request parser → candidate pool + listener memory
- [x] RadioScheduler + auto play + cushion + 20-track history
- [x] DJ interstitial (random 1–2 songs) with substitute/chat context
- [x] YTM login redirect to music.youtube.com
- [ ] Live YTM manual QA with new send flow

### Phase A — Live YTM + WebView control

- [x] `ytm-controller.js` search/play API
- [x] Instrumentation test for search/play on fixture
- [ ] Live YTM manual QA sign-off ([webview_poc_checklist.md](harness/docs/webview_poc_checklist.md))

### Phase B — Cushion playback

- [x] `CushionPlaybackController` + catalog `search_query`
- [x] Auto cushion via `RadioScheduler` + `RadioSessionController` (Plan/Execute UI removed in v0.8.0)
- [x] Harness: `mock_cushion_playback.json` + unit test

### Phase C — DJ radio loop

- [x] OpenAI TTS via Web Audio `playSpeechBuffer`
- [x] Transition ments between tracks (`runTransitionMent`, not send-triggered)
- [x] Substitute apology + chat memory in DJ fallback

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
python harness/scripts/test_user_request_schema.py
python harness/scripts/test_b2b_schedule_schema.py
python harness/scripts/verify_release_config.py
cd android && ./gradlew test
python harness/scripts/ensure_emulator.py
python harness/scripts/run_instrumentation.py
```

See [HARNESS_RULES.md](HARNESS_RULES.md) and [docs/harness-inventory.md](docs/harness-inventory.md).
