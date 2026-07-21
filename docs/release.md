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

## Distribution checklist (Personal BYOK MVP v0.7.0)

**Automated (pre-push):**

- [x] All harness scripts pass (see [HARNESS_RULES.md](../HARNESS_RULES.md))
- [x] `./gradlew test` green
- [x] Instrumentation on Pixel_8 (`ensure_emulator.py` + `run_instrumentation.py`)
- [x] Version bumped in `android/app/build.gradle.kts` (0.7.0 / code 5)
- [x] `CHANGELOG.md` updated

**Release build:**

- [x] `./gradlew assembleRelease` succeeds (unsigned without `signing.properties`)
- [ ] Signed release APK (`android/signing.properties` + local keystore)

**Manual sign-off (required for Release Ready):**

- [ ] Live YTM PoC checklist ([webview_poc_checklist.md](../harness/docs/webview_poc_checklist.md))
- [ ] 30 min background QA ([background_qa_checklist.md](../harness/docs/background_qa_checklist.md))

**Deferred (out of MVP scope):** B2B commercial venues / `CommercialSpaceGuard` runtime — see [project-scope.md](project-scope.md).

## CI note

Store `signing.properties` and keystore as CI secrets; inject at build time. Do not commit to git.
