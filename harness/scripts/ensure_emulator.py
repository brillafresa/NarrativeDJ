#!/usr/bin/env python3
"""
Ensure the configured Android emulator AVD is running and boot-complete.

Default AVD: Pixel_8 (harness/config/emulator.json)

Run:
  python harness/scripts/ensure_emulator.py
  python harness/scripts/ensure_emulator.py --check-only
"""

from __future__ import annotations

import argparse
import subprocess
import sys
from pathlib import Path

SCRIPT_DIR = Path(__file__).resolve().parent
if str(SCRIPT_DIR) not in sys.path:
    sys.path.insert(0, str(SCRIPT_DIR))

from android_env import (  # noqa: E402
    adb_path,
    connected_devices,
    emulator_path,
    find_android_sdk,
    harness_env,
    list_avds,
    load_emulator_config,
    resolve_avd_home,
    wait_for_boot,
)


def start_emulator(avd_name: str, startup_args: list[str]) -> None:
    emulator_bin = emulator_path()
    args = [emulator_bin, "-avd", avd_name, *startup_args]
    print(f"Starting emulator: {' '.join(str(part) for part in args)}")
    creationflags = 0
    if sys.platform == "win32":
        creationflags = subprocess.CREATE_NEW_PROCESS_GROUP
    subprocess.Popen(
        [str(part) for part in args],
        stdout=subprocess.DEVNULL,
        stderr=subprocess.DEVNULL,
        env=harness_env(),
        creationflags=creationflags,
    )


def main() -> int:
    parser = argparse.ArgumentParser(description="Ensure the harness emulator AVD is ready.")
    parser.add_argument(
        "--check-only",
        action="store_true",
        help="Verify SDK/AVD configuration without starting the emulator.",
    )
    args = parser.parse_args()

    config = load_emulator_config()
    avd_name = config["avd_name"]
    boot_timeout_sec = int(config.get("boot_timeout_sec", 180))
    startup_args = list(config.get("startup_args", []))

    sdk = find_android_sdk(require_emulator=True)
    adb = adb_path(sdk)
    avd_home = resolve_avd_home()
    print(f"Using Android SDK: {sdk}")
    print(f"Using AVD home: {avd_home}")
    print(f"Target AVD: {avd_name}")

    avds = list_avds(emulator_path(sdk))
    if avd_name not in avds:
        print(f"FAIL: AVD '{avd_name}' not found.")
        if avds:
            print("Available AVDs:")
            for name in avds:
                print(f"  - {name}")
        else:
            print("No AVDs found. Create one in Android Studio Device Manager.")
        return 1

    devices = connected_devices(adb)
    if devices:
        print(f"Device already connected: {', '.join(devices)}")
        if wait_for_boot(adb, timeout_sec=30):
            print("\nEmulator harness READY")
            return 0
        print("WARN: device connected but boot not complete; waiting...")
        if wait_for_boot(adb, timeout_sec=boot_timeout_sec):
            print("\nEmulator harness READY")
            return 0
        print(f"FAIL: boot did not complete within {boot_timeout_sec}s")
        return 1

    if args.check_only:
        print("PASS: SDK and AVD configuration look valid (emulator not running).")
        return 0

    start_emulator(avd_name, startup_args)
    if wait_for_boot(adb, timeout_sec=boot_timeout_sec):
        print("\nEmulator harness READY")
        return 0

    print(f"FAIL: emulator started but boot did not complete within {boot_timeout_sec}s")
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
