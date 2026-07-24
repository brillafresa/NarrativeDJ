# NarrativeDJ — Development Plan (Personal BYOK MVP)

> **Scope:** [project-scope.md](project-scope.md)  
> **Vision reference:** [research.md](research.md) (partial adoption only)

This is the execution roadmap to reach **Release Ready** for the personal BYOK Android MVP.

## Current baseline (2026-07-24)

- Harness: Python scripts (cushion_bridge + selector + llm + user_request + b2b + no_baked_api_key + release) + `./gradlew test`
- MVP code: Gemini-only BYOK (runtime gate; no APK bake); radio phases Idle/Live/PausedUser/StalePaused; LLM cushion; queue-after-current
- Release: **v0.9.6**; unsigned `assembleRelease` OK
- **Remaining for Release Ready:** live YTM manual QA on device + 30 min background sign-off + signed APK

## Phase overview

| Phase | Focus | Depends on |
|-------|-------|------------|
| **A** | Live YTM validation + search/play JS API | — |
| **B** | Cushion planning → YTM playback | A |
| **C** | DJ transition ments + ducking | B |
| **D** | Background + MediaSession metadata | A |
| **E** | Release ready APK | C, D |
| **F** | Radio messenger UX (▶ Send, pool, auto loop) | B, C |

Recommended order: **A → B → C → F** (D can parallel after A).

---

## Phase A — Live YTM + WebView control

**Goal:** Real `music.youtube.com` now-playing and transport; JS search/play API for cushion.

**Deliverables:**
- `ytm-controller.js`: `searchAndPlay(query)`, `playPause()`
- [harness/docs/webview_poc_checklist.md](../harness/docs/webview_poc_checklist.md): search/play + 5 min background sections
- SVD degraded path documented in [harness-inventory.md](harness-inventory.md)

**Harness:**
- `YtmControllerFixtureTest` — add search/play test on fixture
- Pre-push checklist before JS changes

**Exit criteria:** YTM login → play → status title/artist; fixture search/play test green; checklist sign-off table filled.

---

## Phase B — Cushion playback

**Goal (historical):** Canon route via YTM search/play.

**Current production (v0.9.4+):** No fixed song catalog. Gemini picks the most-similar **candidate-pool** track and invents bridge `search_query` values when similarity is low. Playback via `CushionPlaybackController.playSequence`.

**Harness:**
- `harness/tests/mock_cushion_bridge.json` + `test_cushion_bridge_schema.py`
- `CushionBridgePlanParserTest`, `RadioSchedulerTest`, `CushionPlaybackControllerTest`
- Instrumentation: fixture search/play

**Exit criteria:** Bridge schema + schedule-apply tests green; fixture search/play green.

---

## Phase C — DJ radio loop

**Goal:** Story → LLM → TTS with unified Web Audio ducking; profile + now-playing in prompt.

**Deliverables:**
- Gemini transition ments via `DjPipeline.runTransitionMent`
- Android TTS + `audio-ducking.js` GainNode ducking
- SSML: strip tags for Android TTS
- Prompt context: previous/next track + substitute/chat memory

**Harness:**
- `mock_llm_response.json` / `mock_dj_transition.json` + `test_llm_response_schema.py`
- `DjAudioControlParserTest` SSML strip + fallback scripts

**Exit criteria:** Gemini key → track transition → audible ment + duck in/out.

**Deferred post-MVP:** Weather/Time/Trend auto context (research 2.3 STAGE 1).

---

## Phase D — Background + MediaSession

**Goal:** research 3.2 MVP — notification metadata, stable transport, 30 min QA.

**Deliverables:**
- `PlaybackSessionState` now-playing fields → notification + MediaSession metadata
- `MainActivity.onDestroy` clears transport handler; service stop policy
- [harness/docs/background_qa_checklist.md](../harness/docs/background_qa_checklist.md)

**Harness:**
- `PlaybackMetadataTest` (JVM)
- Manual 30 min checklist

**Exit criteria:** Lock screen pause/resume; notification shows title/artist when playing.

---

## Phase E — Release ready

**Goal:** Deployable personal BYOK MVP APK.

**Deliverables:**
- [release.md](release.md) checklist complete
- Version bump in `android/app/build.gradle.kts`
- [CHANGELOG.md](../CHANGELOG.md) entry
- CI harness workflow green

**Exit criteria:** `assembleRelease` (signed if keystore configured) + full harness green + PoC checklist signed off.

---

## Phase F — Radio messenger UX (v0.8.0 → v0.9.0)

**Goal:** Messenger-style ▶ Send → Gemini parse → candidate pool → queue-after-current / search → transition DJ.

**Deliverables:**
- `RadioSessionController`, `CandidatePool`, `PlayHistory`, `ListenerMemory`, `RadioScheduler`
- `RequestParserService` (Gemini-only; no production local fallback)
- `RadioPlaybackPolicy` — defer search while a track is playing
- `DjInterstitialGate` — random ment every 1–2 track transitions
- Gemini key gate launcher; menu = API key only; system locale
- Single ▶ send UI; YTM auth-safe redirect policy

**Harness:**
- `mock_user_request.json`, `test_user_request_schema.py`
- `mock_dj_transition.json` (validated by `test_llm_response_schema.py`)
- JVM: pool/history/scheduler/parser/gate + `RadioPlaybackPolicyTest` + `YtmWebViewClientTest`

**Exit criteria:** Harness green; device QA — send song/mood while playing does not interrupt; after track ends, queued item plays.

---

## Session handoff

Update this section at the end of each work session.

### Session Handoff — 2026-07-24 (v0.9.6 pre-push cleanup)

- **Last completed:** Docs/CI SSOT sync; radio occupancy + no-bake key ready to ship; `test_cushion_router` removed from CI; dist APK 0.9.6 prepared for push
- **Harness status:** Full pre-push Python set + `RadioPlaybackPolicyTest` + `./gradlew test`
- **Blockers:** Live YTM device QA; signed APK
- **Next session:** Device QA checklist (cold resume / pause queue / idle send); user pushes this tree

### Session Handoff — 2026-07-24 (radio pause / cold resume occupancy)

- **Last completed:** `RadioPlaybackPhase` Idle/Live/PausedUser/StalePaused; sticky only after confirmed playing; one-shot mid-track `playPause(true)` resume; pause/queue status strings; JVM + Python harness green
- **Harness status:** `RadioPlaybackPolicyTest` + full `./gradlew test` + pre-push Python set PASS
- **Blockers:** Emulator WAN may still block live YTM QA; signed APK
- **Next session:** Device QA — (1) cold mid-stop resume (2) send while live → queue (3) send while paused → no auto-next (4) idle send → play now. Commit+push publishes `dist/` as usual.

### Session Handoff — 2026-07-24 (action-plan execution + no-bake key)

- **Last completed:** Pre-push Python + JVM green; Pixel_8 emulator up; `installDebug` + instrumentation 8/8 PASS; `AgentByokInjectTest` injects agent key via instrumentation args (not BuildConfig); MainActivity opens past key gate; overflow shows **Gemini 모델** menu
- **Harness status:** Python (incl. `test_no_baked_api_key.py`) + `./gradlew test` + `run_instrumentation.py` PASS
- **Blockers:** Emulator `eth0`/WAN still unreachable (`ping 8.8.8.8` 100% loss) → live YTM / cushion / 503 fallback QA blocked (env, not app). Signed APK still open.
- **Next session:** Fix emulator network (or use physical device) → YTM login → model picker + cushion bridges + queue-after-current. On commit+push, publish current debug APK under `dist/` as usual (no need to scrub old APK history).

### Session Handoff — 2026-07-24 (v0.9.5 model picker + dead catalog purge)

- **Last completed:** Default `gemini-3.5-flash-lite`; overflow model picker; 503 → session sticky fallback; deleted unused vector catalog stack (`test_cushion_router` / `CushionMusicScheduler` / `demo_tracks`); docs SSOT aligned to LLM cushion only
- **Harness status:** `test_cushion_bridge_schema.py` + model session/catalog JVM tests + full pre-push Python set + `./gradlew testDebugUnitTest`
- **Blockers:** Device live QA under capacity 503; signed APK
- **Next session:** Device — confirm model menu + 503 fallback status string; multi-song cushion bridges; push includes `dist/NarrativeDJ-0.9.5-debug.apk`

### Session Handoff — 2026-07-23 (v0.9.4 LLM pool cushion)

- **Last completed:** Removed runtime catalog cushion; Gemini picks most-similar pool track + invents bridge search queries when similarity below threshold; harness schema/parser; version 0.9.4
- **Harness status:** Python scripts incl. `test_cushion_bridge_schema.py` + `./gradlew testDebugUnitTest`
- **Blockers:** Device QA; Gemini call cost for cushion planning; signed APK
- **Next session:** Device — multi-song pool while playing A; confirm status shows cushion bridges when dissimilar; direct when similar

### Session Handoff — 2026-07-23 (v0.9.3 live QA fixes)

- **Last completed:** Leave-page auto-confirm; runtime cushion via demo catalog; TTS 0.85; waiting-queue marquee; title/hint UX; WebView background harden; version 0.9.3
- **Harness status:** Python 6/6 + `./gradlew testDebugUnitTest` (RadioScheduler cushion cases, WebChromeClient, CatalogMatcher, DjSpeechTiming)
- **Blockers:** Device re-QA; OEM WebView background limits; signed APK
- **Next session:** Device QA — LLM cushion (dissimilar pool → bridges in status); queue-after-current; background minimize; TTS pace

### Session Handoff — 2026-07-23 (v0.9.2 sticky queue-after-current)

- **Last completed:** Sticky occupancy for queue-after-current (`RadioPlaybackPolicy.nextOccupancy` + optimistic hold); harness regression for metadata-sticky defer; version 0.9.2
- **Harness status:** Python 6/6 + `./gradlew test` (incl. `RadioPlaybackPolicyTest` sticky cases)
- **Blockers:** Device live YTM QA (emulator audio stalls); signed APK
- **Next session:** Install `0.9.2` debug APK on device → play track → send while playing → confirm status “현재 곡 다음에 재생 대기” and no interrupt

### Session Handoff — 2026-07-23 (v0.9.1 usable Gemini key gate)

- **Last completed:** `GeminiApiKeyValidator` + gate/seeder hygiene; instrumentation `@After` clears prefs; emulator `boot_timeout_sec=300`; docs/SSOT sync; version 0.9.1
- **Harness status:** Python 6/6 + `./gradlew test` (incl. `GeminiApiKeyValidatorTest`, cushion parity)
- **Blockers:** Live YTM QA for queue handoff (wait for emulator network after cold boot); signed APK
- **Next session:** `ensure_emulator.py` → wait for network → debug install (auto-seed from `local.properties`) → queue-after-current manual QA

### Session Handoff — 2026-07-22 (v0.9.0 Gemini-only + queue policy)

- **Last completed:** Gemini-only BYOK + key gate; menu cleanup; system locale; YTM search/play harden; queue-after-current; docs/SSOT sync; version 0.9.0
- **Harness status:** Python 6/6 + `./gradlew test` (see CHANGELOG Verified)
- **Blockers:** Live YTM QA for queue handoff; signed APK
- **Next session:** Device QA — play track → send new request → confirm no interrupt → next plays after current; re-check mood search Songs preference

```markdown
<!-- Template for future sessions -->
## Session Handoff — YYYY-MM-DD
- Last completed: Phase X / task Y
- Harness status: (commands + PASS/FAIL)
- Blockers: ...
- Next session start: Phase X step 1 — ensure_emulator.py + harness baseline
```

---

## Harness commands (every phase)

```bash
pip install -r harness/requirements.txt
python harness/scripts/sync_fixtures.py
python harness/scripts/test_cushion_bridge_schema.py
python harness/scripts/test_selector_dictionary.py
python harness/scripts/test_llm_response_schema.py
python harness/scripts/test_user_request_schema.py
python harness/scripts/test_b2b_schedule_schema.py
python harness/scripts/test_no_baked_api_key.py
python harness/scripts/verify_release_config.py
cd android && ./gradlew test
python harness/scripts/run_instrumentation.py
```

See [HARNESS_RULES.md](../HARNESS_RULES.md).
