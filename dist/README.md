# Prebuilt APK downloads

Installable debug builds for personal sideload / emulator testing.  
**Not** Play Store releases. Signed production APKs require local `signing.properties` (see [docs/release.md](../docs/release.md)).

## Latest (v0.9.2)

| File | Notes |
|------|--------|
| [NarrativeDJ-0.9.2-debug.apk](./NarrativeDJ-0.9.2-debug.apk) | Debug build from `assembleDebug` (v0.9.2 / versionCode 11) — sticky queue-after-current |

**Direct download (GitHub):**  
https://github.com/brillafresa/NarrativeDJ/raw/main/dist/NarrativeDJ-0.9.2-debug.apk

### Install

```bash
adb install -r dist/NarrativeDJ-0.9.2-debug.apk
```

Or download the file on the phone and open it (allow install from unknown sources).

### Rebuild locally

```bash
cd android
./gradlew assembleDebug
# output: app/build/outputs/apk/debug/app-debug.apk
# copy → dist/NarrativeDJ-<versionName>-debug.apk and update README / docs/release.md links
```

### Previous

| File | Notes |
|------|--------|
| [NarrativeDJ-0.9.1-debug.apk](./NarrativeDJ-0.9.1-debug.apk) | Gemini key gate usability (v0.9.1 / versionCode 10) |
