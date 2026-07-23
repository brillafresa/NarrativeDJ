# Harness inventory

Cross-language validation assets for NarrativeDJ.  
**Conflict priority:** see [HARNESS_RULES.md](../HARNESS_RULES.md) and [.cursorrules](../.cursorrules).  
**Current app version:** `0.9.5` (2026-07-24)

## Production vs harness boundary

| Layer | Path | Role |
|-------|------|------|
| **Harness SSOT** | `harness/tests/*.json` | Canonical mock/fixture data |
| **Python verify** | `harness/scripts/*.py` | Schema checks (not shipped) |
| **JVM unit tests** | `android/app/src/test/` | Kotlin parsers, planners, radio |
| **Instrumentation** | `android/app/src/androidTest/` | WebView/SVD/HackTimer fixture tests |
| **Production assets** | `android/app/src/main/assets/` | Runtime JS; admin schedule (frozen); WWW fixtures |

Production **must not** reference paths named `mock_*`. Neutral production demo data:

- `assets/admin/default_schedule.json` — frozen Admin scaffold
- `assets/www/fixtures/*.html` — **instrumentation-only** DOM fixtures (not loaded in normal YT Music flow)

There is **no fixed song catalog** in production or harness. Runtime cushion uses Gemini pool pick + invented bridge `search_query` values only.

> **Superseded (do not restore):** `test_cushion_router.py`, `CushionMusicScheduler*`, `mock_tracks.json`, `demo_tracks.json` — removed in v0.9.5. Use `test_cushion_bridge_schema.py` instead.

## Runtime radio cushion (production)

Gemini picks the most-similar **candidate-pool** track (B) vs now-playing (A). If similarity is below 0.55, invents 1–2 YTM `search_query` bridges (C), then plays A→C→B via `NarrativeDJYtm.searchAndPlay`.

| Asset | Verify command |
|-------|----------------|
| LLM plan fixture | `harness/tests/mock_cushion_bridge.json` |
| Schema script | `python harness/scripts/test_cushion_bridge_schema.py` |
| Parser + threshold | `CushionBridgePlanParserTest` |
| Apply plan → queries | `RadioSchedulerTest` (`decisionFromPlan`) |
| Pool resolve | `CushionBridgePlannerServiceTest` |
| Play sequence | `CushionPlaybackControllerTest` |

**Status UX:** `쿠션 브릿지 N곡 → …` when bridges used; else `재생 예약: …`.

## Emulator harness (local debugging)

| Asset | Verify command |
|-------|----------------|
| AVD config | `harness/config/emulator.json` — default **Pixel_8**; `startup_args: [-no-snapshot-load]`; `boot_timeout_sec: 300` |
| Ensure running | `python harness/scripts/ensure_emulator.py` |
| Instrumentation runner | `python harness/scripts/run_instrumentation.py` |

**Agent rule:** before WebView fixture tests, `installDebug`, or on-device debugging, run `ensure_emulator.py`.

## WebView / SVD harness

| Asset | Verify command |
|-------|----------------|
| Selector dictionary | `python harness/scripts/test_selector_dictionary.py` |
| DOM fixtures | `assets/www/fixtures/ytm-poc-fixture*.html`, `ytm-search-fixture.html` |
| Leave-page auto-confirm | `YtmWebChromeClientTest` + `YtmSearchNavigation` during app-driven search |
| Instrumentation | `YtmControllerFixtureTest`, `YtmSvdFixtureTest`, `HackTimerFixtureTest` |
| JVM parsers | `YtmNowPlayingParserTest`, `YtmSvdReportParserTest`, `YtmCspBypassTest`, `YtmWebViewClientTest` |
| Manual checklist | [harness/docs/webview_poc_checklist.md](../harness/docs/webview_poc_checklist.md) |

## AI DJ / BYOK harness (Phase C + F)

| Asset | Verify command |
|-------|----------------|
| LLM audio-control fixtures | `mock_llm_response.json`, `mock_dj_transition.json` |
| Schema script | `python harness/scripts/test_llm_response_schema.py` |
| Response extractor + parser | `LlmResponseExtractorTest`, `DjAudioControlParserTest` |
| TTS rate | `DjSpeechTimingTest` (`DEFAULT_SPEECH_RATE = 0.85`) |
| Gemini HTTP helper | `GeminiApiTest` |
| Model allow-list | `GeminiModelCatalogTest` — default Flash-Lite + resolve |
| Session 503 fallback | `GeminiModelSessionTest` |
| Encrypted keys | `SecureKeyStoreTest` (instrumentation) — Gemini only; clears prefs in `@After` |
| Key usability | `GeminiApiKeyValidator` + `GeminiApiKeyValidatorTest` |
| Metadata | `PlaybackMetadataFormatterTest` |

**Production pipeline (v0.9.5):** track transition → `DjInterstitialGate` → `DjPipeline.runTransitionMent` → **Gemini** (default `gemini-3.5-flash-lite`, menu-selectable; 503 → session sticky fallback) → **Android TTS** (rate 0.85) → `audio-ducking.js` duck in/out.

## Radio messenger UX harness (Phase F)

| Asset | Verify command |
|-------|----------------|
| User request fixture | `harness/tests/mock_user_request.json` |
| Request schema | `python harness/scripts/test_user_request_schema.py` |
| JSON + local edge parser | `UserRequestParserTest` (`parseLocal` = **harness-only** edge cases) |
| Candidate pool | `CandidatePoolTest` |
| Play history | `PlayHistoryTest` |
| Scheduler | `RadioSchedulerTest` — pool pick + apply LLM cushion plan |
| Queue policy | `RadioPlaybackPolicyTest` — sticky occupancy while metadata visible |
| Waiting queue UI | `WaitingQueueFormatterTest` |
| DJ interstitial gate | `DjInterstitialGateTest` |

**Flow:** ▶ Send → Gemini parse → candidate pool → if playing, **queue**; else plan cushion (LLM) → `searchAndPlay` sequence → optional DJ ment on track transition.

## i18n harness

| Asset | Verify command |
|-------|----------------|
| Default Korean strings | `android/app/src/main/res/values/strings.xml` |
| English overlay | `android/app/src/main/res/values-en/strings.xml` |
| Locale resolution | `AppLocaleStore.getLanguage` from **system** locale |
| Manual | Change device language → relaunch app (no in-app language menu) |

## B2B / admin harness (frozen scaffold)

| Asset | Verify command |
|-------|----------------|
| Schedule fixture | `harness/tests/mock_b2b_schedule.json` |
| Schema script | `python harness/scripts/test_b2b_schedule_schema.py` |
| Planner tests | `SchedulePlannerTest`, `B2bPluginTest` |

## Dead-code policy

Remove unused production code, fixtures, and harness scripts when a design changes (do not leave “reference-only” orphans that compete with the live path). Prefer deleting over documenting why something is unused.
