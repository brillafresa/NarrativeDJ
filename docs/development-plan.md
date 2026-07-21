# NarrativeDJ — Development Plan (Personal BYOK MVP)

> **Scope:** [project-scope.md](project-scope.md)  
> **Vision reference:** [research.md](research.md) (partial adoption only)

This is the execution roadmap to reach **Release Ready** for the personal BYOK Android MVP.

## Current baseline (2026-07-21)

- Harness: Python 6 scripts + `./gradlew test` — **green** (instrumentation optional)
- MVP code: Radio messenger UX (v0.8.0), YTM search/play, auto cushion scheduler, transition DJ ments
- Release: v0.8.1; unsigned `assembleRelease` OK
- **Remaining for Release Ready:** live YTM manual QA + 30 min background sign-off + signed APK

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

**Goal:** Canon route (몽중인 → California Dreamin' → Hotel California → Sweet Child O' Mine) plays via YTM.

**Deliverables:**
- `CushionPlaybackController` — sequential `bridgeIds` + target via JS
- `CatalogTrack.searchQuery` in catalog JSON (fallback: title)
- UI: Plan (preview) + Execute (play route) buttons

**Harness:**
- `harness/tests/mock_cushion_playback.json` — bridge order + queries
- `CushionPlaybackControllerTest` (JVM)
- Instrumentation: fixture search/play

**Exit criteria:** Execute plays bridge sequence on fixture; planner tests green.

---

## Phase C — DJ radio loop

**Goal:** Story → LLM → TTS with unified Web Audio ducking; profile + now-playing in prompt.

**Deliverables:**
- OpenAI TTS via `NarrativeDJ.playSpeechBuffer()` (not MediaPlayer-only ducking)
- `LlmPromptBuilder.build(..., currentTrack, targetTrack)` context
- SSML: strip tags for Android TTS; pass through for OpenAI
- After DJ segment: refresh cushion suggestion

**Harness:**
- Extend `mock_llm_response.json` / schema tests if needed
- `DjAudioControlParserTest` SSML strip cases

**Exit criteria:** BYOK keys → story → audible ment + duck in/out + cushion hint in status.

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

## Phase F — Radio messenger UX (v0.8.0)

**Goal:** Messenger-style ▶ Send → parse (no immediate TTS) → candidate pool → auto scheduler + transition DJ.

**Deliverables:**
- `RadioSessionController`, `CandidatePool`, `PlayHistory`, `ListenerMemory`, `RadioScheduler`
- `RequestParserService` + `UserRequestParser` (BYOK LLM or local fallback)
- `DjInterstitialGate` — random ment every 1–2 track transitions
- Single ▶ send UI; YTM login redirect to `music.youtube.com`

**Harness:**
- `mock_user_request.json`, `test_user_request_schema.py`
- `mock_dj_transition.json` (validated by `test_llm_response_schema.py`)
- JVM: `CandidatePoolTest`, `PlayHistoryTest`, `RadioSchedulerTest`, `UserRequestParserTest`, `DjInterstitialGateTest`

**Exit criteria:** Harness green; device QA — send song/mood/chat, verify pool + auto play + transition DJ.

---

## Session handoff

Update this section at the end of each work session.

### Session Handoff — 2026-07-21 (v0.8.1 stabilization)

- **Last completed:** Harness cleanup, dead code removal (legacy story-segment path), docs sync, v0.8.1
- **Harness status:** Python 6/6 PASS, `./gradlew testDebugUnitTest` PASS
- **Blockers:** Live YTM QA with new send flow; signed APK
- **Next session:** Device QA — send song/mood/chat, verify pool + transition DJ

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
python harness/scripts/test_cushion_router.py
python harness/scripts/test_selector_dictionary.py
python harness/scripts/test_llm_response_schema.py
python harness/scripts/test_user_request_schema.py
python harness/scripts/test_b2b_schedule_schema.py
python harness/scripts/verify_release_config.py
cd android && ./gradlew test
python harness/scripts/run_instrumentation.py
```

See [HARNESS_RULES.md](../HARNESS_RULES.md).
