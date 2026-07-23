# Harness inventory

Cross-language validation assets for NarrativeDJ.  
**Conflict priority:** see [HARNESS_RULES.md](../HARNESS_RULES.md) and [.cursorrules](../.cursorrules).  
**Current app version:** `0.9.4`

## Production vs harness boundary

| Layer | Path | Role |
|-------|------|------|
| **Harness SSOT** | `harness/tests/*.json` | Canonical mock/fixture data |
| **Python verify** | `harness/scripts/*.py` | Algorithm & schema checks (not shipped) |
| **JVM unit tests** | `android/app/src/test/` | Kotlin ports, parsers, planners |
| **Instrumentation** | `android/app/src/androidTest/` | WebView/SVD/HackTimer fixture tests |
| **Production assets** | `android/app/src/main/assets/` | Runtime JS; synced demo catalog (**not loaded by MainActivity**); admin schedule (frozen) |

Production **must not** reference paths named `mock_*`. Demo data uses neutral names:

- `assets/catalog/demo_tracks.json` — synced from `harness/tests/mock_tracks.json`; **harness/vector parity only** (not loaded for radio scheduling)
- `assets/admin/default_schedule.json` — frozen Admin scaffold
- `assets/www/fixtures/*.html` — **instrumentation-only** DOM fixtures (not loaded in normal YT Music flow)

## Cushion — two layers (do not confuse)

### A) Vector reference (harness / research parity)

Fixed demo catalog + distance math (BPM/energy/valence/embedding). Used to prove DROP / DIRECT / 1–2 bridge routes.

| Asset | Verify command |
|-------|----------------|
| Python router | `python harness/scripts/test_cushion_router.py` |
| Kotlin scheduler | `./gradlew test` → `CushionMusicSchedulerTest` |
| Route planner | `CushionRoutePlannerTest` |
| Catalog id helper (test-only) | `CatalogMatcherTest` |
| Playback order fixture | `harness/tests/mock_cushion_playback.json` + `CushionPlaybackControllerTest` |

### B) Runtime radio cushion (production, v0.9.4+)

No fixed catalog. Gemini picks the most-similar **candidate-pool** track (B) vs now-playing (A). If similarity is below 0.55, invents 1–2 YTM `search_query` bridges (C), then plays A→C→B via `NarrativeDJYtm.searchAndPlay`.

| Asset | Verify command |
|-------|----------------|
| LLM plan fixture | `harness/tests/mock_cushion_bridge.json` |
| Schema script | `python harness/scripts/test_cushion_bridge_schema.py` |
| Parser + threshold | `CushionBridgePlanParserTest` |
| Apply plan → queries | `RadioSchedulerTest` (`decisionFromPlan`) |
| Pool resolve | `CushionBridgePlannerServiceTest` |

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
| Encrypted keys | `SecureKeyStoreTest` (instrumentation) — Gemini only; clears prefs in `@After` |
| Key usability | `GeminiApiKeyValidator` + `GeminiApiKeyValidatorTest` |
| Metadata | `PlaybackMetadataFormatterTest` |

**Production pipeline (v0.9.4):** track transition → `DjInterstitialGate` → `DjPipeline.runTransitionMent` → **Gemini** (`gemini-3.5-flash`) → **Android TTS** (rate 0.85) → `audio-ducking.js` duck in/out.

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

Not reachable from production UI in v0.9.4 (menu entries removed; AdminActivity not launched).

## Release harness

| Asset | Verify command |
|-------|----------------|
| Signing example | `android/signing.properties.example` |
| Release doc | `docs/release.md` |
| Verify script | `python harness/scripts/verify_release_config.py` |

## JS injection pipeline (production)

Loaded at runtime into WebView (`YtmAssetInjector` order):

1. `bridge.js` — NativeAudioBridge
2. `hack-timer.js` — Web Worker timer (native `setTimeout` fallback on fixture pages)
3. `audio-ducking.js` — Web Audio GainNode ducking
4. `ytm-svd.js` — selector self-validation
5. `ytm-controller.js` — search/play + now-playing control

Selector dictionary: `assets/www/selector_dictionary.json` (also validated by Python script).

## BYOK readiness (production, not harness)

| Component | Storage | Runtime |
|-----------|---------|---------|
| Gemini API key | `SecureKeyStore` (EncryptedSharedPreferences) | Launcher key gate + Menu → **API 키 설정** |
| Debug seed | `BuildConfig.GEMINI_API_KEY` from `local.properties` | `DebugByokSeeder` (DEBUG only) |
| YT Music session | In-app WebView user login | No server proxy |
| TTS | Android TTS | Locale from system language; speech rate 0.85 |
| OpenAI keys | — | **Removed** (v0.9.0) |
