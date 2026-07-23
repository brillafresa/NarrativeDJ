# WebView PoC Checklist — Phase 1-A

Manual verification steps for YouTube Music login & playback PoC.
Run automated harness first, then complete this checklist on a physical device or emulator.

## Emulator setup (local)

```bash
python harness/scripts/ensure_emulator.py
python harness/scripts/run_instrumentation.py
```

Default AVD: **Pixel_8** (`harness/config/emulator.json`). Manual equivalent: `emulator -avd Pixel_8`.

## Prerequisites

```bash
pip install -r harness/requirements.txt
python harness/scripts/test_cushion_bridge_schema.py
cd android && ./gradlew test
python harness/scripts/ensure_emulator.py
cd android && ./gradlew assembleDebug installDebug
```

## Automated harness (run before manual steps)

| Harness | Command | Pass criteria |
|---------|---------|---------------|
| JS controller fixture | `python harness/scripts/run_instrumentation.py` | `YtmControllerFixtureTest` green |
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

### D. Search / play API + messenger send (Phase A + F)

- [ ] ▶ Send queues song/mood to pool (no immediate DJ TTS)
- [ ] On live YTM: search from send plays audible track (manual)

### E. Background (Phase D)

- [ ] 5+ min background with screen off (audio continues)
- [ ] See [background_qa_checklist.md](background_qa_checklist.md) for 30 min sign-off

### F. PoC instrumentation signals

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
