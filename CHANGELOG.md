# Changelog

All notable changes to this project are documented here.

## [Unreleased]

## [0.9.2] — 2026-07-23

### Fixed

- **Queue-after-current:** sticky playback occupancy — keep deferring while now-playing metadata is visible even when flaky YTM `isPlaying` reports false (emulator/live interrupt regression)
- Optimistic session hold when a search/play sequence starts so mid-load sends stay in the pool

### Changed

- Version code 11

### Harness verification (this release)

| Check | Result |
|-------|--------|
| Queue policy + sticky occupancy | `RadioPlaybackPolicyTest` (metadata sticky / idle release) |
| Cushion / schemas / release | Python 6 scripts + `./gradlew test` |

### Verified (pre-push)

- Python harness 6/6, `./gradlew testDebugUnitTest` PASS
- Debug APK: [`dist/NarrativeDJ-0.9.2-debug.apk`](dist/NarrativeDJ-0.9.2-debug.apk)

## [0.9.1] — 2026-07-23

### Fixed

- **Gemini key gate:** reject blank/placeholder keys (`test-key-123`, etc.); debug seeder overwrites unusable leftovers from instrumentation
- **SecureKeyStoreTest:** clear encrypted prefs in `@After` so device store is not polluted after connected tests

### Added

- JVM harness: `GeminiApiKeyValidator` + `GeminiApiKeyValidatorTest`
- `SecureKeyStore.hasUsableGeminiApiKey()` for gate / MainActivity / debug seed

### Changed

- Emulator harness: `boot_timeout_sec` 180 → **300** (cold boot with `-no-snapshot-load`)
- Docs / SSOT: HARNESS_RULES emulator command + BYOK usability; harness inventory; session handoff
- Version code 10

### Harness verification (this release)

| Check | Result |
|-------|--------|
| Cushion algorithm | `test_cushion_router.py` + `CushionMusicSchedulerTest` |
| AI DJ schema | `test_llm_response_schema.py` (`mock_llm_response`, `mock_dj_transition`) |
| Radio request schema | `test_user_request_schema.py` + `UserRequestParserTest` |
| BYOK key usability | `GeminiApiKeyValidatorTest` |
| Selector / B2B / release | remaining Python scripts + `./gradlew test` |

### Verified (pre-push)

- Python harness 6/6, `./gradlew test` PASS
- Debug APK: [`dist/NarrativeDJ-0.9.1-debug.apk`](dist/NarrativeDJ-0.9.1-debug.apk)

## [0.9.0] — 2026-07-22

### Added

- **Gemini key gate:** `GeminiKeyGateActivity` is the launcher — app cannot proceed without a Gemini API key
- **Gemini-only BYOK:** `SecureKeyStore` / `GeminiApi` (`gemini-3.5-flash`); OpenAI LLM/TTS clients removed
- **Queue-after-current:** `RadioPlaybackPolicy` defers `searchAndPlay` while a track is playing
- **YTM search resume harness:** `assets/www/fixtures/ytm-search-fixture.html` + instrumentation coverage
- **Login redirect fix:** `YtmWebViewClient` allows Google/YouTube auth URLs; redirects only post-login landings
- JVM: `GeminiApiTest`, `RadioPlaybackPolicyTest`, `YtmWebViewClientTest`

### Changed

- Menu: **API 키 설정** only (language / B2B / Admin removed from UI)
- Locale: follows **system language** (no in-app language switch)
- Runtime radio: direct YTM `search_query` only (demo catalog / cushion bridges not loaded at runtime)
- Request parse: Gemini required — no local-parser production fallback
- DJ ments: Gemini + Android TTS only
- Docs / SSOT synced: `.cursorrules`, `HARNESS_RULES.md`, harness inventory, project scope
- Version code 9

### Harness verification (this release)

| Check | Result |
|-------|--------|
| Cushion algorithm | `test_cushion_router.py` + `CushionMusicSchedulerTest` |
| AI DJ schema | `test_llm_response_schema.py` (`mock_llm_response`, `mock_dj_transition`) |
| Radio request schema | `test_user_request_schema.py` + `UserRequestParserTest` |
| Selector / B2B / release | remaining Python scripts + `./gradlew test` |

### Verified (pre-push)

- Python harness 6/6, `./gradlew test` PASS
- Debug APK published: [`dist/NarrativeDJ-0.9.0-debug.apk`](dist/NarrativeDJ-0.9.0-debug.apk)

## [0.8.1] — 2026-07-21

### Changed

- Pre-push stabilization: removed legacy story-segment production path (`runStorySegment`, `LlmPromptBuilder`, Plan/Play UI strings)
- Extended `test_llm_response_schema.py` to validate `mock_dj_transition.json`
- JVM harness headers on radio + DJ parser tests; polling debug logs removed from `MainActivity`
- Docs synced: harness inventory, development plan Phase F, conflict priority aligned with `.cursorrules`

### Verified (pre-push)

- Python harness 6/6, `./gradlew testDebugUnitTest` PASS

## [0.8.0] — 2026-07-21

### Added

- **Radio UX:** Messenger-style **▶ Send** — LLM parses song/mood/chat (no immediate TTS)
- **Candidate pool** with dedupe; **20-track play history** skip at selection time
- **RadioScheduler** — auto next track + cushion bridges; empty-pool similar-track fallback
- **DjInterstitialGate** — DJ ment randomly every 1–2 track transitions (substitute apology + chat memory)
- **YTM redirect** — return to `music.youtube.com` after login lands on `youtube.com`
- Harness: `mock_user_request.json`, `test_user_request_schema.py`, radio JVM tests

### Changed

- Removed Plan / Play / DJ buttons; single send control
- Input field dark-theme colors (readable on control panel)
- `LlmClient` adds `generateTransitionMent`; `DjPipeline.runTransitionMent`
- Version code 7

## [0.7.1] — 2026-07-21

### Added

- Harness inventory: cushion playback (Phase B) and AI DJ/BYOK (Phase C) verification tables in [docs/harness-inventory.md](docs/harness-inventory.md)
- JVM harness headers on `CushionPlaybackControllerTest`, `PlaybackMetadataFormatterTest`

### Changed

- Scope banners on [README.md](README.md) and [.cursorrules](.cursorrules); [docs/architecture.md](docs/architecture.md) Roadmap status synced to v0.7.0 code state
- [HARNESS_RULES.md](HARNESS_RULES.md) conflict priority aligned with `.cursorrules` (`project-scope.md`, `development-plan.md`)
- [docs/release.md](docs/release.md) distribution checklist split: automated vs manual sign-off
- Version code 6

### Verified (pre-push)

- Python harness 5/5, `./gradlew test`, instrumentation 7/7 (Pixel_8), `assembleRelease` (unsigned)

## [0.7.0] — 2026-07-21

### Added

- **Project scope docs:** [docs/project-scope.md](docs/project-scope.md), [docs/development-plan.md](docs/development-plan.md) — personal BYOK MVP scope; B2B/Admin deferred
- **Phase A:** `NarrativeDJYtm.searchAndPlay` / `playPause` in `ytm-controller.js`; fixture search/play instrumentation test
- **Phase B:** `CushionPlaybackController`, catalog `search_query`, Execute cushion button, `mock_cushion_playback.json`
- **Phase C:** OpenAI TTS via Web Audio `playSpeechBufferFromBase64`; LLM prompt track context; post-DJ cushion refresh
- **Phase D:** MediaSession/notification now-playing metadata; `background_qa_checklist.md`
- **Tests:** `CushionPlaybackControllerTest`, `PlaybackMetadataFormatterTest`, SSML strip test

### Changed

- `TODO.md` reorganized: Scaffold complete / In scope MVP / Deferred
- Document cross-links: research.md scope notice, README, `.cursorrules`, architecture.md
- Version code 5

### Scope decision

This release implements **personal BYOK Android MVP** only. B2B/Admin scaffolds remain frozen per [project-scope.md](docs/project-scope.md).

### Previous Unreleased (merged)

- **Emulator harness:** `ensure_emulator.py`, `run_instrumentation.py`, `emulator.local.json`
- **SDK migration:** system Android Studio SDK; removed project `.tools/`
- **CI/CD harness:** GitHub Actions `harness-verify.yml`

## [0.6.0] — 2026-07-20

### Added

- **Korean-first i18n:** default UI and DJ prompts in Korean (`values/strings.xml`); English via `values-en/` and menu **언어 / Language**
- **`AppLocaleStore` / `AppLanguage` / `NarrativeDjApp`** — persisted locale, applied at app start
- **Chat-style story input** — multiline `EditText` (3–6 lines) instead of single-line search bar
- **Localized DJ pipeline:** `LlmPromptBuilder` language-aware prompts; Android TTS locale follows setting; `DjAudioControlParser.fallbackForStory(story, language)`

### Changed

- `SpaceProfile` uses `@StringRes labelResId`; `CushionPlan.localizedSummary(context)` for UI strings
- `CommercialSpaceGuard` returns `GuardMessageKey`; messages resolved via string resources
- `hack-timer.js` — Worker fallback to native `setTimeout` when Blob/Worker unavailable (instrumentation fixtures)
- `YtmSvdFixtureTest` — canonical fixture expects production selector priority (`ytmusic-player-bar .title`)
- Version code 4

### Harness verification (this release)

| Area | Command | Result |
|------|---------|--------|
| Cushion algorithm | `python harness/scripts/test_cushion_router.py` | 3/3 PASS |
| Selector dictionary | `python harness/scripts/test_selector_dictionary.py` | PASS |
| LLM JSON schema | `python harness/scripts/test_llm_response_schema.py` | PASS |
| B2B schedule | `python harness/scripts/test_b2b_schedule_schema.py` | PASS |
| Release scaffold | `python harness/scripts/verify_release_config.py` | PASS |
| Kotlin unit tests | `cd android && ./gradlew test` | PASS |
| i18n fallback | `DjAudioControlParserTest` (ko/en) | PASS |
| Optional emulator | `./gradlew connectedDebugAndroidTest` | 6/6 PASS (WebView fixture harness) |

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
- Local YT Music DOM fixture: `android/app/src/main/assets/www/fixtures/ytm-poc-fixture.html`
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
