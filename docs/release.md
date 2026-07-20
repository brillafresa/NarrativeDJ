# Release build & distribution

## Prerequisites

- JDK 17, Android SDK 34 (see [README.md](../README.md))
- Release keystore created locally (not in repo)

## Signing setup

1. Create a keystore (one-time):

```bash
keytool -genkeypair -v -keystore release.keystore -alias narrativedj -keyalg RSA -keysize 2048 -validity 10000
```

2. Copy the example config:

```bash
cp android/signing.properties.example android/signing.properties
```

3. Edit `android/signing.properties` with your keystore path and passwords.

4. Verify scaffold (no secrets committed):

```bash
python harness/scripts/verify_release_config.py
```

## Build release APK

```bash
cd android
./gradlew assembleRelease
```

Output: `android/app/build/outputs/apk/release/app-release.apk`

If `signing.properties` is missing, `assembleRelease` builds an **unsigned** APK (suitable for local testing only).

## Distribution checklist

- [ ] All harness scripts pass (see README)
- [ ] `./gradlew test` green
- [ ] Manual PoC checklist (`harness/docs/webview_poc_checklist.md`) signed off
- [ ] B2B commercial venues use valid B2B license (`CommercialSpaceGuard`)
- [ ] Version bumped in `android/app/build.gradle.kts`
- [ ] `CHANGELOG.md` updated

## CI note

Store `signing.properties` and keystore as CI secrets; inject at build time. Do not commit to git.
