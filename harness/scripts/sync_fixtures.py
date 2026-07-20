#!/usr/bin/env python3
"""
Sync harness fixture JSON to Android test resources.

Purpose: Keep harness/tests/ as SSOT; copy fixtures into JVM/instrumentation test classpath.
Run: python harness/scripts/sync_fixtures.py
"""

from __future__ import annotations

import shutil
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
HARNESS_TESTS = REPO_ROOT / "harness" / "tests"
ANDROID_TEST_RES = REPO_ROOT / "android" / "app" / "src" / "test" / "resources"

FIXTURES = (
    "mock_tracks.json",
    "mock_llm_response.json",
    "mock_b2b_schedule.json",
)


def main() -> int:
    ANDROID_TEST_RES.mkdir(parents=True, exist_ok=True)
    for name in FIXTURES:
        src = HARNESS_TESTS / name
        dst = ANDROID_TEST_RES / name
        if not src.exists():
            print(f"FAIL: missing SSOT fixture {src}")
            return 1
        shutil.copy2(src, dst)
        print(f"Synced {name}")
    print("\nFixture sync PASSED")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
