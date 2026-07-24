# Prebuilt APK downloads

Installable debug builds for personal sideload / emulator testing.  
**Not** Play Store releases. Signed production APKs require local `signing.properties` (see [docs/release.md](../docs/release.md)).

**Security:** From v0.9.6, APKs must not contain Gemini API keys (runtime key gate). Verified by `python harness/scripts/test_no_baked_api_key.py`.

## Latest (v0.9.6)

| File | Notes |
|------|--------|
| [NarrativeDJ-0.9.6-debug.apk](./NarrativeDJ-0.9.6-debug.apk) | No baked API keys; radio Idle/Live/PausedUser/StalePaused; LLM cushion SSOT (v0.9.6 / versionCode 15) |

**Direct download (GitHub):**  
https://github.com/brillafresa/NarrativeDJ/raw/main/dist/NarrativeDJ-0.9.6-debug.apk

### Install

```bash
adb install -r dist/NarrativeDJ-0.9.6-debug.apk
```

Or download the file on the phone and open it (allow install from unknown sources).

### Rebuild locally

```bash
cd android
./gradlew assembleDebug
# output: app/build/outputs/apk/debug/app-debug.apk
# copy → dist/NarrativeDJ-<versionName>-debug.apk and update README / docs/release.md links
python ../harness/scripts/test_no_baked_api_key.py
```

### Previous

| File | Notes |
|------|--------|
| [NarrativeDJ-0.9.5-debug.apk](./NarrativeDJ-0.9.5-debug.apk) | Flash-Lite default, model picker, 503 session fallback (may contain revoked baked key) |
| [NarrativeDJ-0.9.4-debug.apk](./NarrativeDJ-0.9.4-debug.apk) | LLM pool cushion |
| [NarrativeDJ-0.9.3-debug.apk](./NarrativeDJ-0.9.3-debug.apk) | Catalog cushion attempt + live QA UX |
| [NarrativeDJ-0.9.2-debug.apk](./NarrativeDJ-0.9.2-debug.apk) | Sticky queue-after-current |
| [NarrativeDJ-0.9.1-debug.apk](./NarrativeDJ-0.9.1-debug.apk) | Gemini key gate usability |
