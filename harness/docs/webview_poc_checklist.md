# WebView PoC Checklist — Phase 1-A

Manual verification steps for YouTube Music login & playback PoC.
Run automated harness first, then complete this checklist on a physical device or emulator.

## Prerequisites

```bash
pip install -r harness/requirements.txt
python harness/scripts/test_cushion_router.py
cd android && ./gradlew test
cd android && ./gradlew assembleDebug installDebug
```

## Automated harness (run before manual steps)

| Harness | Command | Pass criteria |
|---------|---------|---------------|
| JS controller fixture | `./gradlew connectedAndroidTest` | `YtmControllerFixtureTest` green |
| Now-playing JSON parser | `./gradlew test` | `YtmNowPlayingParserTest` green |

## Manual PoC checklist

### A. App launch & WebView load

- [ ] App opens without crash
- [ ] WebView loads `https://music.youtube.com`
- [ ] Status bar shows page-load state (not stuck on "Loading…")

### B. YouTube Music login (user session)

- [ ] Google sign-in UI appears when not logged in
- [ ] User can complete sign-in in WebView (no external browser required)
- [ ] After sign-in, YT Music home/library is visible

### C. Playback

- [ ] User can start playback from YT Music UI
- [ ] Audio is audible from device speaker/headphones
- [ ] Status bar updates with track title and artist (may take up to 5 s poll interval)

### D. PoC instrumentation signals

- [ ] Logcat tag `NarrativeDJ` shows `Music page loaded`
- [ ] Logcat shows `Now playing:` with non-empty title when a track plays
- [ ] `NativeAudioBridge` stub logs appear after page load (CSP bypass still Phase 1-C)

## Failure triage

| Symptom | Likely cause | Next step |
|---------|--------------|-----------|
| Blank WebView | Network / WebView settings | Check INTERNET permission, retry on Wi-Fi |
| Login loop | Cookie / DOM issue | Clear WebView data, retry sign-in |
| No now-playing in status | YT Music DOM change | Extend selector list in `ytm-controller.js` (Phase 1-B SVD) |
| No audio | User-gesture policy | Tap play in YT Music UI first |

## Sign-off

| Field | Value |
|-------|-------|
| Date | |
| Device / API level | |
| Tester | |
| Result | PASS / FAIL |
| Notes | |
