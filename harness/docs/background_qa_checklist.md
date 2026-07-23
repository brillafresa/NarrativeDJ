# Background playback QA checklist — Phase D

Run after Phase D MediaSession changes. Requires emulator or device with YT Music logged in.

## Limits (WebView YTM)

Audio plays inside the in-app YouTube Music WebView (not a native MediaPlayer). NarrativeDJ keeps a foreground service + wake lock and avoids `WebView.onPause()` on Activity pause, but **some OEM / Chrome versions may still pause media when the app is minimized**. Treat 30‑min background as best-effort; file OEM notes if audio stops.

## Setup

```bash
python harness/scripts/ensure_emulator.py
cd android && ./gradlew installDebug
```

## Checklist

### Notification metadata

- [ ] Start playback in YTM; notification shows track title (not generic "재생 중" only)
- [ ] Artist appears when parsed from now-playing

### Lock screen transport

- [ ] Pause from lock screen / notification stops YT Music
- [ ] Play resumes playback

### Background endurance (30 min)

- [ ] Screen off; audio continues ≥ 30 minutes
- [ ] No ANR or service kill in logcat (`NarrativeDJ`, `MediaPlaybackService`)
- [ ] Wake lock released after service stop

## Sign-off

| Field | Value |
|-------|-------|
| Date | |
| Device / API | |
| Result | PASS / FAIL |
| Notes | |
