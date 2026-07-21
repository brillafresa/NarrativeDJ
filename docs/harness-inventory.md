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

## Emulator harness (local debugging)

| Asset | Verify command |
|-------|----------------|
| AVD config | `harness/config/emulator.json` — default **Pixel_8** |
| Ensure running | `python harness/scripts/ensure_emulator.py` |
| Instrumentation runner | `python harness/scripts/run_instrumentation.py` |
| Manual start (equivalent) | `emulator -avd Pixel_8` |

**Agent rule:** before WebView fixture tests, `installDebug`, or on-device debugging, run `ensure_emulator.py`. If `adb devices` shows no ready device, the script starts the configured AVD and waits for `sys.boot_completed=1`.

SDK resolution order: `ANDROID_HOME` / `ANDROID_SDK_ROOT` → `harness/config/emulator.local.json` `sdk_dir` → OS default SDK (`AppData/Local/Android/Sdk`) → `android/local.properties` `sdk.dir`.

AVD resolution order: `ANDROID_AVD_HOME` → `emulator.local.json` `avd_home` → `%USERPROFILE%\.android\avd`. If Studio moved AVDs to another drive (e.g. `E:\AndroidAVD\avd`), set `avd_home` in `emulator.local.json`.

## WebView / SVD harness

| Asset | Verify command |
|-------|----------------|
| Selector dictionary | `python harness/scripts/test_selector_dictionary.py` |
| DOM fixtures | `assets/www/fixtures/ytm-poc-fixture*.html` |
| Instrumentation | `python harness/scripts/run_instrumentation.py` → `YtmControllerFixtureTest`, `YtmSvdFixtureTest`, `HackTimerFixtureTest` |
| JVM parsers | `YtmNowPlayingParserTest`, `YtmSvdReportParserTest`, `YtmCspBypassTest` |
| Manual checklist | [harness/docs/webview_poc_checklist.md](../harness/docs/webview_poc_checklist.md) |

**SVD note:** canonical fixture validates **production selector priority** (`ytmusic-player-bar .title`); alt fixture validates fallback selectors.

## AI DJ / BYOK harness

| Asset | Verify command |
|-------|----------------|
| LLM JSON fixture | `harness/tests/mock_llm_response.json` |
| Schema script | `python harness/scripts/test_llm_response_schema.py` |
| Response extractor | `LlmResponseExtractorTest` |
| Audio control parser | `DjAudioControlParserTest` (includes ko/en `fallbackForStory`) |
| Encrypted keys | `SecureKeyStoreTest` (instrumentation) |
| LLM prompts | `LlmPromptBuilder.build(story, profile, AppLanguage)` — Korean default |

Production LLM/TTS uses on-device HTTP (Gemini/OpenAI) with keys in `SecureKeyStore` / `B2bLicenseStore`. No API keys in repo.

**DJ ment flow:** story input → `DjPipeline.runStorySegment` → LLM JSON (`script`, ducking params) → OpenAI TTS or Android TTS with locale → Web Audio ducking via `audio-ducking.js`.

## i18n harness

| Asset | Verify command |
|-------|----------------|
| Default Korean strings | `android/app/src/main/res/values/strings.xml` |
| English overlay | `android/app/src/main/res/values-en/strings.xml` |
| Locale store | `AppLocaleStore` / `NarrativeDjApp` |
| Fallback scripts | `DjAudioControlParserTest` — `AppLanguage.KOREAN` / `ENGLISH` |
| Manual | Menu → **언어** → switch English → UI + TTS locale refresh |

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
2. `hack-timer.js` — Web Worker timer (native `setTimeout` fallback on fixture pages)
3. `audio-ducking.js` — Web Audio GainNode ducking
4. `ytm-svd.js` — selector self-validation
5. `ytm-controller.js` — now-playing control

Selector dictionary: `assets/www/selector_dictionary.json` (also validated by Python script).

## BYOK readiness (production, not harness)

| Component | Storage | Runtime |
|-----------|---------|---------|
| Gemini / OpenAI keys | `SecureKeyStore` (EncryptedSharedPreferences) | Menu → API 키 설정 |
| B2B license | `B2bLicenseStore` | Menu → B2B / 음원 제공 |
| YT Music session | In-app WebView user login | No server proxy |
| TTS | OpenAI TTS (if OpenAI key) else Android TTS | Locale from `AppLocaleStore` |
