# TODO — NarrativeDJ

**Scope:** [docs/project-scope.md](docs/project-scope.md)  
**Roadmap:** [docs/development-plan.md](docs/development-plan.md)  
**Current release:** `0.9.4` — LLM pool cushion (no runtime catalog)

## Scaffold complete (not E2E)

- [x] Phase 1 scaffold: WebView PoC, SVD, CSP, BYOK, space profiles
- [x] Phase 2 scaffold: Background service, LLM/TTS, ducking, cushion planner
- [x] Phase 3 scaffold: B2B/Admin UI (frozen — see Deferred)
- [x] Harness boundary + emulator harness (`ensure_emulator.py`, Pixel_8)
- [x] Korean-first i18n (system locale; no in-app language menu)
- [x] Scope docs: `project-scope.md`, `development-plan.md`

## In scope — MVP feature completion

### Phase F — Radio messenger UX (v0.8.0 → v0.9.4)

- [x] Single ▶ send control (Plan/Play/DJ removed)
- [x] Gemini request parser → candidate pool + listener memory (no local production fallback)
- [x] RadioScheduler + auto play; LLM picks most-similar pool track; invents bridge search queries when similarity low (no fixed catalog)
- [x] Queue-after-current policy (`RadioPlaybackPolicy`) — sticky occupancy while metadata visible (do not trust flaky `isPlaying` alone)
- [x] DJ interstitial (random 1–2 songs) with substitute/chat context
- [x] YTM login redirect narrowed (auth URLs allowed)
- [x] Gemini key gate (launcher) — usable key required (`GeminiApiKeyValidator`; placeholders rejected)
- [x] Debug live-QA seed from `local.properties` via `DebugByokSeeder` (gitignored; overwrites unusable leftovers)
- [ ] Live YTM manual QA with send + queue-after-current + cushion status (device)

### Phase A — Live YTM + WebView control

- [x] `ytm-controller.js` search/play API (URL search + Songs preference)
- [x] Instrumentation test for search/play on fixture (+ `ytm-search-fixture.html`)
- [ ] Live YTM manual QA sign-off ([webview_poc_checklist.md](harness/docs/webview_poc_checklist.md))

### Phase B — Cushion playback

- [x] `CushionPlaybackController` + catalog `search_query` (harness/JVM)
- [x] Algorithm parity: `test_cushion_router.py` ↔ `CushionMusicSchedulerTest`
- [x] Harness: `mock_cushion_playback.json` + unit test
- [x] Runtime cushion via Gemini pool similarity + invented bridges; vector catalog = harness only

### Phase C — DJ radio loop

- [x] Transition ments via Gemini (`GeminiApi` / `gemini-3.5-flash`) + Android TTS
- [x] Ducking via `audio-ducking.js` GainNode
- [x] Substitute apology + chat memory in DJ fallback scripts (harness-validated)

### Phase D — Background + MediaSession

- [x] Notification + MediaSession now-playing metadata
- [x] Service lifecycle cleanup
- [ ] Manual 30 min background QA ([background_qa_checklist.md](harness/docs/background_qa_checklist.md))

### Phase E — Release ready

- [x] Version 0.9.4 bump
- [x] Unsigned release APK path (`assembleRelease` — local test build)
- [ ] Signed release APK (local keystore — see [release.md](docs/release.md))
- [ ] Full release checklist manual sign-off ([release.md](docs/release.md))

## Deferred / out of scope

Do not implement unless [project-scope.md](docs/project-scope.md) is revised.

- [ ] Live B2B partner API + stream playback (frozen scaffold)
- [ ] GPS-based commercial venue detection
- [ ] Admin console write/edit + Korean admin HTML
- [ ] `SchedulePlanner` runtime auto profile switch
- [ ] Runtime space-profile spinner / demo-catalog routing
- [ ] OpenAI LLM/TTS BYOK (removed — Gemini-only)
- [ ] CI/CD release pipeline with signing secrets
- [ ] Full CI emulator matrix API 26–34
- [ ] Desktop client
- [ ] Weather/Time/Trend auto context (research 2.3 STAGE 1)

## Known gaps (manual QA)

- Queue-after-current: automated policy unit-tested; live YTM end-of-track handoff still needs manual sign-off
- Mood/search play: prefer Songs shelf; playlist mis-clicks reduced — re-verify on device
- Emulator cold boot (`-no-snapshot-load`): wait for network before YTM load (`ERR_NAME_NOT_RESOLVED` = env, not app)

## Pre-push verification (Harness-First)

```bash
pip install -r harness/requirements.txt
python harness/scripts/sync_fixtures.py
python harness/scripts/test_cushion_router.py
python harness/scripts/test_cushion_bridge_schema.py
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
