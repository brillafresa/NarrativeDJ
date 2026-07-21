#!/usr/bin/env python3
"""
Ensure the harness emulator is running, then run connectedDebugAndroidTest.

Run:
  python harness/scripts/run_instrumentation.py
  python harness/scripts/run_instrumentation.py --task connectedAndroidTest
"""

from __future__ import annotations

import argparse
import subprocess
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
ANDROID_DIR = REPO_ROOT / "android"
SCRIPT_DIR = Path(__file__).resolve().parent

if str(SCRIPT_DIR) not in sys.path:
    sys.path.insert(0, str(SCRIPT_DIR))


def gradle_command(task: str) -> list[str]:
    wrapper_name = "gradlew.bat" if sys.platform == "win32" else "gradlew"
    return [str(ANDROID_DIR / wrapper_name), task, "--no-daemon"]


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Start Pixel_8 emulator if needed, then run Gradle instrumentation tests.",
    )
    parser.add_argument(
        "--task",
        default="connectedDebugAndroidTest",
        help="Gradle connected test task (default: connectedDebugAndroidTest)",
    )
    parser.add_argument(
        "--skip-emulator",
        action="store_true",
        help="Skip ensure_emulator.py (device/emulator already running).",
    )
    args = parser.parse_args()

    if not args.skip_emulator:
        ensure = subprocess.run(
            [sys.executable, str(SCRIPT_DIR / "ensure_emulator.py")],
            check=False,
        )
        if ensure.returncode != 0:
            return ensure.returncode

    print(f"\nRunning Gradle task: {args.task}")
    result = subprocess.run(gradle_command(args.task), cwd=ANDROID_DIR, check=False)
    if result.returncode == 0:
        print("\nInstrumentation harness PASSED")
    else:
        print(f"\nInstrumentation harness FAILED (exit {result.returncode})")
    return result.returncode


if __name__ == "__main__":
    raise SystemExit(main())
