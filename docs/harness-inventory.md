# Harness inventory

Cross-language validation assets for NarrativeDJ.  
**Conflict priority:** see [HARNESS_RULES.md](../HARNESS_RULES.md) and [.cursorrules](../.cursorrules).

## Production vs harness boundary

| Layer | Path | Role |
|-------|------|------|
| **Harness SSOT** | `harness/tests/*.json` | Canonical mock/fixture data |
| **Python verify** | `harness/scripts/*.py` | Algorithm & schema checks (not shipped) |
| **JVM unit tests** | `android/app/src/test/` | Kotlin ports, parsers, planners |
| **Instrumentation** | `android/app/src/androidTest/` | WebView/SVD/HackTimer fixture tests |
| **Production assets** | `android/app/src/main/assets/` | Runtime JS, demo catalog, admin default schedule |

Production **must not** reference paths named `mock_*`. Demo data uses neutral names:

- `assets/catalog/demo_tracks.json` — MVP cushion catalog (content synced from `harness/tests/mock_tracks.json`)
- `assets/admin/default_schedule.json` — bundled admin demo schedule
- `assets/www/fixtures/*.html` — **instrumentation-only** DOM fixtures (not loaded in normal YT Music flow)

## Cushion algorithm harness

| Asset | Verify command |
|-------|----------------|
| Python reference | `python harness/scripts/test_cushion_router.py` |
| Mock tracks SSOT | `harness/tests/mock_tracks.json` |
| Kotlin port | `./gradlew test` → `CushionMusicSchedulerTest`, `CushionRoutePlannerTest` |

**Canonical scenario:** 몽중인 → California Dreamin' → Hotel California → Sweet Child O' Mine (2 bridges).

## WebView / SVD harness

| Asset | Verify command |
|-------|----------------|
| Selector dictionary | `python harness/scripts/test_selector_dictionary.py` |
| DOM fixtures | `assets/www/fixtures/ytm-poc-fixture*.html` |
| Instrumentation | `./gradlew connectedAndroidTest` → `YtmControllerFixtureTest`, `YtmSvdFixtureTest`, `HackTimerFixtureTest` |
| JVM parsers | `YtmNowPlayingParserTest`, `YtmSvdReportParserTest`, `YtmCspBypassTest` |
| Manual checklist | [harness/docs/webview_poc_checklist.md](../harness/docs/webview_poc_checklist.md) |

## AI DJ / BYOK harness

| Asset | Verify command |
|-------|----------------|
| LLM JSON fixture | `harness/tests/mock_llm_response.json` |
| Schema script | `python harness/scripts/test_llm_response_schema.py` |
| Response extractor | `LlmResponseExtractorTest` |
| Audio control parser | `DjAudioControlParserTest` |
| Encrypted keys | `SecureKeyStoreTest` (instrumentation) |

Production LLM/TTS uses on-device HTTP (Gemini/OpenAI) with keys in `SecureKeyStore` / `B2bLicenseStore`. No API keys in repo.

## B2B / admin harness

| Asset | Verify command |
|-------|----------------|
| Schedule fixture | `harness/tests/mock_b2b_schedule.json` |
| Schema script | `python harness/scripts/test_b2b_schedule_schema.py` |
| Planner tests | `SchedulePlannerTest`, `B2bPluginTest` |
| Commercial guard | `CommercialSpaceGuardTest` (in `B2bPluginTest`) |

## Release harness

| Asset | Verify command |
|-------|----------------|
| Signing example | `android/signing.properties.example` |
| Release doc | `docs/release.md` |
| Verify script | `python harness/scripts/verify_release_config.py` |

## JS injection pipeline (production)

Loaded at runtime into WebView (`YtmAssetInjector` order):

1. `bridge.js` — NativeAudioBridge
2. `hack-timer.js` — background timer defense
3. `audio-ducking.js` — Web Audio GainNode ducking
4. `ytm-svd.js` — selector self-validation
5. `ytm-controller.js` — now-playing control

Selector dictionary: `assets/www/selector_dictionary.json` (also validated by Python script).
