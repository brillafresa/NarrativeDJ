#!/usr/bin/env python3
"""Fail if Gemini API keys can be baked into the Android APK via BuildConfig.

Purpose: keep BYOK keys out of published debug/release APKs.
Run: python harness/scripts/test_no_baked_api_key.py
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
BUILD_GRADLE = REPO_ROOT / "android" / "app" / "build.gradle.kts"
SEEDER = (
    REPO_ROOT
    / "android"
    / "app"
    / "src"
    / "main"
    / "kotlin"
    / "com"
    / "narrativedj"
    / "app"
    / "byok"
    / "DebugByokSeeder.kt"
)
EXAMPLE = REPO_ROOT / "android" / "local.properties.example"
DIST_DIR = REPO_ROOT / "dist"

# Google AI Studio keys commonly start with this prefix when present as literals.
AIZA_RE = re.compile(rb"AIza[0-9A-Za-z_\-]{20,}")
# ≤0.9.5 dist APKs may still contain a revoked baked key — skip scan (no history scrub).
# ≥0.9.6 APKs published via normal commit+push must stay key-free.
DIST_APK_RE = re.compile(r"NarrativeDJ-(\d+)\.(\d+)\.(\d+)-debug\.apk\Z")
DIST_SCAN_MIN = (0, 9, 6)


def _apk_version(path: Path) -> tuple[int, int, int] | None:
    match = DIST_APK_RE.fullmatch(path.name)
    if not match:
        return None
    return int(match.group(1)), int(match.group(2)), int(match.group(3))


def main() -> int:
    failed = 0

    gradle_text = BUILD_GRADLE.read_text(encoding="utf-8")
    if "GEMINI_API_KEY" in gradle_text:
        print("FAIL: android/app/build.gradle.kts must not define GEMINI_API_KEY BuildConfig")
        failed += 1
    else:
        print("PASS: no GEMINI_API_KEY BuildConfig in build.gradle.kts")

    if "gemini.api.key" in gradle_text:
        print("FAIL: build.gradle.kts must not read gemini.api.key into the APK")
        failed += 1
    else:
        print("PASS: build.gradle.kts does not read gemini.api.key")

    if SEEDER.exists():
        print(f"FAIL: remove dead seed path {SEEDER.relative_to(REPO_ROOT)}")
        failed += 1
    else:
        print("PASS: DebugByokSeeder.kt absent")

    kotlin_hits: list[str] = []
    main_kt = REPO_ROOT / "android" / "app" / "src" / "main"
    for path in main_kt.rglob("*.kt"):
        text = path.read_text(encoding="utf-8")
        if "BuildConfig.GEMINI_API_KEY" in text or "DebugByokSeeder" in text:
            kotlin_hits.append(str(path.relative_to(REPO_ROOT)))
    if kotlin_hits:
        print("FAIL: production still references baked-key seed:")
        for hit in kotlin_hits:
            print(f"  - {hit}")
        failed += 1
    else:
        print("PASS: no production BuildConfig.GEMINI_API_KEY / DebugByokSeeder refs")

    if EXAMPLE.exists():
        example = EXAMPLE.read_text(encoding="utf-8")
        if "auto-seed" in example.lower() or "BuildConfig" in example:
            print("FAIL: local.properties.example must not document APK auto-seed")
            failed += 1
        else:
            print("PASS: local.properties.example does not document APK auto-seed")

    if DIST_DIR.is_dir():
        scanned = 0
        for apk in sorted(DIST_DIR.glob("*.apk")):
            version = _apk_version(apk)
            if version is None or version < DIST_SCAN_MIN:
                print(f"SKIP: {apk.relative_to(REPO_ROOT)} (≤0.9.5; scan starts at 0.9.6)")
                continue
            scanned += 1
            data = apk.read_bytes()
            if AIZA_RE.search(data):
                print(
                    f"FAIL: {apk.relative_to(REPO_ROOT)} appears to contain a Google API key literal"
                )
                failed += 1
            else:
                print(f"PASS: no AIza* literal in {apk.relative_to(REPO_ROOT)}")
        if scanned == 0:
            print("PASS: no ≥0.9.6 dist APKs present yet (source guards still apply)")

    if failed:
        print(f"\n{failed} check(s) FAILED")
        return 1
    print("\nNo-baked-API-key checks PASSED")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
