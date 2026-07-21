# NarrativeDJ — Development Plan (Personal BYOK MVP)

> **Scope:** [project-scope.md](project-scope.md)  
> **Vision reference:** [research.md](research.md) (partial adoption only)

This is the execution roadmap to reach **Release Ready** for the personal BYOK Android MVP.

## Current baseline (2026-07-21)

- Harness: Python 5 scripts + `./gradlew test` + instrumentation 6/6 (Pixel_8) — **green**
- Scaffold: WebView, SVD, cushion planner, DjPipeline, MediaPlaybackService, i18n
- Gaps: cushion playback, live YTM QA sign-off, unified TTS ducking, MediaSession metadata

## Phase overview

| Phase | Focus | Depends on |
|-------|-------|------------|
| **A** | Live YTM validation + search/play JS API | — |
| **B** | Cushion planning → YTM playback | A |
| **C** | DJ radio loop (story → ment → ducking) | B |
| **D** | Background + MediaSession metadata | A |
| **E** | Release ready APK | C, D |

Recommended order: **A → B → C** (D can parallel after A).

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

## Session handoff

Update this section at the end of each work session.

### Session Handoff — 2026-07-21

- **Last completed:** MVP plan v0.7.0 — scope docs, Phase A–E code (search/play, cushion execute, DJ ducking, metadata)
- **Harness status:** Python 5/5 PASS, `./gradlew test` PASS (43 tests), instrumentation 7/7 PASS (search/play fixture added)
- **Blockers:** Live YTM + 30 min background QA require manual sign-off on checklist
- **Next session:** Fill [webview_poc_checklist.md](../harness/docs/webview_poc_checklist.md) and [background_qa_checklist.md](../harness/docs/background_qa_checklist.md); local signed release if keystore ready

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
python harness/scripts/test_b2b_schedule_schema.py
python harness/scripts/verify_release_config.py
cd android && ./gradlew test
python harness/scripts/run_instrumentation.py
```

See [HARNESS_RULES.md](../HARNESS_RULES.md).
