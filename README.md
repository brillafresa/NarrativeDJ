# NarrativeDJ

Audio-first AI narrative radio DJ — client-centric BYOK Android app with cushion (bridge) song scheduling.

## Repository structure

```
docs/research.md    Source of Truth (full research report)
harness/            Python algorithm verification harness
android/            Kotlin Android app (deployable APK)
```

## Prerequisites

- **Android:** Android Studio Ladybug+ or JDK 17+, Android SDK 34
- **Python:** 3.11+ for harness (`pip install -r harness/requirements.txt`)

## Python harness (algorithm verification)

```bash
pip install -r harness/requirements.txt
python harness/scripts/sync_fixtures.py
python harness/scripts/test_cushion_router.py
python harness/scripts/test_selector_dictionary.py
python harness/scripts/test_llm_response_schema.py
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

**Local toolchain (no system JDK/Android Studio required):** portable JDK 17 and Android SDK can live under `.tools/`. On Windows, set `JAVA_HOME` to the junction path without `+` in the folder name:

```powershell
$env:JAVA_HOME = "E:\Users\likedy\Projects\NarrativeDJ\.tools\jdk-17-home"
$env:ANDROID_HOME = "E:\Users\likedy\Projects\NarrativeDJ\.tools\android-sdk"
```

`android/local.properties` should point `sdk.dir` at the same SDK path.

Install debug APK:

```bash
./gradlew installDebug
```

Release APK (unsigned without local `signing.properties`):

```bash
./gradlew assembleRelease
```

See [docs/release.md](docs/release.md) for signing setup.

## Release signing

Release keystore is **not** in the repo. Configure `signingConfigs` locally or via CI secrets before `assembleRelease`.

## Documentation

- [Research (SoT)](docs/research.md)
- [Architecture](docs/architecture.md)
- [Features](docs/features.md)
- [Harness rules](HARNESS_RULES.md)
- [Harness inventory](docs/harness-inventory.md)
- [Release build](docs/release.md)
