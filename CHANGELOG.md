# Changelog

All notable changes to this project are documented here.

## [0.5.0] — 2026-07-20

### Changed (pre-push cleanup)

- Production–harness boundary: removed `mock_*` from `src/main/`; demo catalog → `catalog/demo_tracks.json`, admin schedule → `default_schedule.json`, WebView fixtures → `www/fixtures/`
- Added `harness/scripts/sync_fixtures.py`, [harness/README.md](harness/README.md), [docs/harness-inventory.md](docs/harness-inventory.md)
- Expanded [HARNESS_RULES.md](HARNESS_RULES.md) with pre-push verification checklist
- Removed debug `Log.d` / `console.log`; renamed LLM `mockForStory` → `fallbackForStory`
- Version code 3

## [0.4.0-phase3] — 2026-07-20

### Added

- **B2B plugin:** `MusicProvider` (BYOK WebView ↔ B2B stream), `B2bPartnerApiClient`, `B2bLicenseStore`, `CommercialSpaceGuard`
- **Admin console:** `AdminConsoleActivity`, `ScheduleRepository`, `SchedulePlanner`, local HTML console
- **Release:** `android/signing.properties.example`, `docs/release.md`, conditional release signing in Gradle
- **Harness:** `test_b2b_schedule_schema.py`, `verify_release_config.py`, `B2bPluginTest`, `SchedulePlannerTest`

### Changed

- `MainActivity` — B2B settings menu, admin console entry, commercial-space guard status
- Version code 2, release APK builds via `./gradlew assembleRelease`

## [0.3.0-phase2] — 2026-07-20

### Added

- **Background:** `MediaPlaybackService` with `MediaSessionCompat`, partial Wake Lock, foreground notification
- **HackTimer:** `hack-timer.js` Web Worker timer (instrumentation harness)
- **BYOK LLM/TTS:** `GeminiLlmClient`, `OpenAiLlmClient`, `OpenAiTtsClient`, `LlmResponseExtractor`
- **Cushion integration:** `CushionRoutePlanner`, `TrackCatalogLoader`, profile-filtered route planning
- **Harness:** `test_llm_response_schema.py`, `LlmResponseExtractorTest`, `CushionRoutePlannerTest`, `HackTimerFixtureTest`

### Changed

- `DjPipeline` uses real LLM + OpenAI TTS (Android TTS fallback)
- `audio-ducking.js` — exponential ramps, `duckForSpeech`, bridge events
- `MainActivity` — playback service, cushion Plan button, live route suggestions

## [0.2.0-phase1] — 2026-07-20

### Added

- **1-B SVD:** `selector_dictionary.json`, `ytm-svd.js`, `YtmSvdReportParser`, fixture alt DOM + tests
- **1-C CSP:** `YtmCspBypass` with HTML transform + `shouldInterceptRequest` wiring
- **1-D BYOK/Ducking:** `SecureKeyStore` (EncryptedSharedPreferences), `DjPipeline`, full `audio-ducking.js` GainNode graph
- **1-E Profiles:** `SpaceProfile` templates, spinner UI, `SpaceProfileMatcher`
- Python harness: `harness/scripts/test_selector_dictionary.py`

### Changed

- `ytm-controller.js` delegates selector resolution to SVD
- `MainActivity` — profile spinner, story→DJ pipeline, BYOK settings menu

## [0.1.1-harness] — 2026-07-20

### Added

- WebView PoC harness: `harness/docs/webview_poc_checklist.md` manual verification checklist
- Local YT Music DOM fixture: `android/app/src/main/assets/www/ytm-poc-fixture.html`
- `ytm-controller.js` — now-playing parser with selector fallbacks (PoC)
- `YtmNowPlayingParserTest` (JUnit) and `YtmControllerFixtureTest` (instrumentation skeleton)
- `YtmAssetInjector`, `YtmJsBridge`, status bar in `MainActivity`

### Changed

- `YtmWebViewClient` injects bridge/ducking/controller scripts on page load
- `bridge.js` routes to Android `JavascriptInterface` when present

## [0.1.0-harness] — 2026-07-20

### Added

- `docs/research.md` — Source of Truth (Gemini research report)
- Harness engineering guidelines (`HARNESS_RULES.md`, `.cursorrules`)
- Python harness: `CushionMusicScheduler`, mock tracks, `test_cushion_router.py`
- Android Kotlin scaffold: WebView shell, MediaSession stub, scheduler port
- Documentation: `architecture.md`, `features.md`, `README.md`
- Phase 1 milestone tracking in `TODO.md`
