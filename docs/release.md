# Release build & distribution

## Download latest APK

Prebuilt **debug** APK for personal sideload / emulator (not Play Store):

- **v0.9.2:** [NarrativeDJ-0.9.2-debug.apk](https://github.com/brillafresa/NarrativeDJ/raw/main/dist/NarrativeDJ-0.9.2-debug.apk)
- Catalog: [dist/README.md](../dist/README.md)

```bash
adb install -r dist/NarrativeDJ-0.9.2-debug.apk
```

**Agent / maintainer rule:** on every **commit + push**, rebuild `assembleDebug`, copy to `dist/NarrativeDJ-<versionName>-debug.apk`, and refresh links in `README.md`, this file, and `dist/README.md` (see `.cursor/rules/commit-push-apk.mdc`).

Signed production APKs are not published in-repo; configure a local keystore below.

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

## Distribution checklist (Personal BYOK MVP v0.9.2)

**Automated (pre-push):**

- [x] All harness scripts pass (see [HARNESS_RULES.md](../HARNESS_RULES.md))
- [x] `./gradlew test` green
- [ ] Instrumentation on Pixel_8 (`ensure_emulator.py` + `run_instrumentation.py`) — optional before push; required for Release Ready
- [x] Version bumped in `android/app/build.gradle.kts` (0.9.2 / code 11)
- [x] `CHANGELOG.md` updated

**Release build:**

- [x] `./gradlew assembleDebug` → published under `dist/NarrativeDJ-0.9.2-debug.apk`
- [x] `./gradlew assembleRelease` succeeds (unsigned without `signing.properties`)
- [ ] Signed release APK (`android/signing.properties` + local keystore)

**Manual sign-off (required for Release Ready):**

- [ ] Live YTM PoC checklist ([webview_poc_checklist.md](../harness/docs/webview_poc_checklist.md)) — include queue-after-current
- [ ] 30 min background QA ([background_qa_checklist.md](../harness/docs/background_qa_checklist.md))
- [ ] Gemini key gate: fresh install without usable key cannot enter MainActivity; placeholders rejected

**Deferred (out of MVP scope):** B2B commercial venues / Admin UI — see [project-scope.md](project-scope.md).

## CI note

Store `signing.properties` and keystore as CI secrets; inject at build time. Do not commit to git.
