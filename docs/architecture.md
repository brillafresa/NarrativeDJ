# Architecture

> **Implemented scope:** [project-scope.md](project-scope.md) — personal BYOK Android MVP only.  
> Summary of [research.md](research.md) Sections 3–4. See project-scope on conflict.

## Architecture A — BYOK client

| Layer | Technology |
|-------|------------|
| Platform | Android APK (Phase 1), Desktop later |
| UI language | Korean default; English via `AppLocaleStore` + `values-en/` |
| Music | In-app WebView → YouTube Music PWA |
| Control | JS injection (play/pause, now-playing parse) |
| AI | User Gemini/OpenAI API key on-device |
| Audio mix | Web Audio API GainNode ducking |
| Keys | EncryptedSharedPreferences (`SecureKeyStore`) |

No provider server for streaming or AI inference billing.

## WebView control (3.1)

```mermaid
flowchart LR
    App[Android_Kotlin] --> WebView[YT_Music_WebView]
    WebView --> Inject[JS_Injection]
    Inject --> Scheduler[CushionMusicScheduler]
    Scheduler --> Inject
    App --> CSP[shouldInterceptRequest_CSP_bypass]
```

- **SVD:** Selector self-validation on app start; fallback dictionary
- **CSP bypass:** Intercept `music.youtube.com` HTML; inject permissive CSP meta + `NativeAudioBridge`
- **Anti-bot:** Real WebView user session, not headless automation

## Background playback (3.2)

- `PARTIAL_WAKE_LOCK`
- `FOREGROUND_SERVICE_MEDIA_PLAYBACK` + `MediaSessionService`
- Web Worker timer (`HackTimer.js` pattern)

## Audio ducking pipeline (3.3)

```mermaid
flowchart TB
    YT[YT_Music_MediaElement] --> MG[Music_GainNode]
    MG --> DEST[AudioContext_destination]
    TTS[TTS_BufferSource] --> SG[Speech_GainNode]
    SG --> DEST
    CTRL[Ducking_Controller] --> MG
```

Flow: user story → LLM (BYOK) → SSML + fade params → TTS (BYOK) → duck in → play → duck out

## Monorepo layout

```text
harness/tests/       Mock JSON SSOT + Python verification scripts
harness/src/           Python reference algorithms (not shipped)
android/app/src/main/  Production Kotlin + runtime assets (no mock_* names)
android/app/src/test/  JVM unit tests (fixtures synced via sync_fixtures.py)
android/app/src/androidTest/  WebView instrumentation harness
```

Production demo data uses neutral asset names (`catalog/demo_tracks.json`, `admin/default_schedule.json`). See [harness-inventory.md](harness-inventory.md).

```text
docs/research.md     Source of Truth
harness/             Python algorithm verification
android/             Production Kotlin app → APK
```

## Deployment (Android)

- `applicationId`: `com.narrativedj.app`
- `minSdk` 26, `targetSdk` 34
- Debug: `./gradlew assembleDebug`
- Release: local keystore required (not in repo)

## Roadmap

**Active (MVP v0.7.1):** [development-plan.md](development-plan.md) Phase A–E.

| Phase | Focus | Status (v0.7.1) |
|-------|--------|-----------------|
| A | Live YTM + search/play JS | Code complete — live YTM manual QA pending |
| B | Cushion playback execution | Complete |
| C | DJ radio loop | Complete |
| D | Background + MediaSession metadata | Code complete — 30 min background QA pending |
| E | Release ready APK | Unsigned release built — signed APK + checklist sign-off pending |

**Scaffold complete (research phases 1–3.1):** WebView, SVD, BYOK, profiles, i18n, B2B/Admin UI shells.

**Deferred (out of MVP scope):** B2B streaming, admin CRUD, GPS guard, Desktop — see [project-scope.md](project-scope.md).

## Legal / risk (4.1–4.3)

- Client proxy: no server-side music copy
- Commercial space: manual venue toggle + B2B scaffold (**frozen**); GPS deferred per [project-scope.md](project-scope.md)
- YT Music ToS: side-load / personal use MVP; B2B partner APIs in Phase 3
