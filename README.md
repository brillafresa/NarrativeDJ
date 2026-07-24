# NarrativeDJ

> **Scope:** This repository implements **personal BYOK Android MVP** only.
> Full product vision: [docs/research.md](docs/research.md). **Implemented scope:** [docs/project-scope.md](docs/project-scope.md) (read first).

Audio-first AI narrative radio DJ — client-centric BYOK Android app with cushion (bridge) song scheduling.

**Default language:** follows device system locale (Korean + English string resources).

## Download APK (v0.9.6)

Latest sideload build (debug):

- **[NarrativeDJ-0.9.6-debug.apk](https://github.com/brillafresa/NarrativeDJ/raw/main/dist/NarrativeDJ-0.9.6-debug.apk)**
- Details: [dist/README.md](dist/README.md) · signing notes: [docs/release.md](docs/release.md)

Enter your Gemini API key at first launch (not baked into the APK).

```bash
adb install -r dist/NarrativeDJ-0.9.6-debug.apk
```

## Repository structure

```
docs/project-scope.md   Repository goal scope (read first)
docs/research.md        Full product vision (partial adoption)
harness/                Python algorithm verification harness
android/                Kotlin Android app (deployable APK)
```

## Prerequisites

- **Android:** [Android Studio](https://developer.android.com/studio) with Android SDK 34+ and an emulator AVD
- **Python:** 3.11+ for harness (`pip install -r harness/requirements.txt`)

Copy SDK path into Gradle (gitignored):

```powershell
Copy-Item android/local.properties.example android/local.properties
# Edit sdk.dir to your Android Studio SDK, e.g.:
# sdk.dir=C\:\\Users\\YourName\\AppData\\Local\\Android\\Sdk
```

If your AVDs are not under `%USERPROFILE%\.android\avd` (e.g. moved to another drive), copy and edit:

```powershell
Copy-Item harness/config/emulator.local.json.example harness/config/emulator.local.json
```

CLI builds use Android Studio's bundled JDK:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
```

## Python harness (algorithm verification)

```bash
pip install -r harness/requirements.txt
python harness/scripts/sync_fixtures.py
python harness/scripts/test_cushion_bridge_schema.py
python harness/scripts/test_selector_dictionary.py
python harness/scripts/test_llm_response_schema.py
python harness/scripts/test_user_request_schema.py
python harness/scripts/test_b2b_schedule_schema.py
python harness/scripts/verify_release_config.py
```

## Android app

Open `android/` in Android Studio, or:

```bash
cd android
./gradlew assembleDebug
./gradlew test
```

Default AVD: **Pixel_8** (`harness/config/emulator.json`). If AVDs live outside `%USERPROFILE%\.android\avd`, set `avd_home` in `harness/config/emulator.local.json`.

Install debug APK:

```bash
./gradlew installDebug
```

Release APK (unsigned without local `signing.properties`):

```bash
./gradlew assembleRelease
```

See [docs/release.md](docs/release.md) for signing setup.

## Emulator (local debugging)

Default AVD: **Pixel_8** (`harness/config/emulator.json`).

```bash
python harness/scripts/ensure_emulator.py
python harness/scripts/run_instrumentation.py
cd android && ./gradlew installDebug
```

If no device is connected, `ensure_emulator.py` runs `emulator -avd Pixel_8` and waits for boot before instrumentation or manual debugging.

## Release signing

Release keystore is **not** in the repo. Configure `signingConfigs` locally or via CI secrets before `assembleRelease`.

## Documentation

- **[Project scope](docs/project-scope.md)** — what this repo implements (read first)
- [Development plan](docs/development-plan.md) — MVP Phase A–E roadmap
- [Research (full vision)](docs/research.md)
- [Architecture](docs/architecture.md)
- [Features](docs/features.md)
- [Harness rules](HARNESS_RULES.md)
- [Harness inventory](docs/harness-inventory.md)
- [Release build](docs/release.md)
