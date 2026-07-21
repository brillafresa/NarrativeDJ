"""Resolve Android SDK paths and emulator/adb binaries for local harness scripts."""

from __future__ import annotations

import json
import os
import re
import subprocess
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
ANDROID_DIR = REPO_ROOT / "android"
LOCAL_PROPERTIES = ANDROID_DIR / "local.properties"
EMULATOR_CONFIG = REPO_ROOT / "harness" / "config" / "emulator.json"
EMULATOR_LOCAL_CONFIG = REPO_ROOT / "harness" / "config" / "emulator.local.json"


def load_emulator_config() -> dict:
    if not EMULATOR_CONFIG.exists():
        raise FileNotFoundError(f"missing emulator config: {EMULATOR_CONFIG}")
    with EMULATOR_CONFIG.open(encoding="utf-8") as handle:
        config = json.load(handle)
    if EMULATOR_LOCAL_CONFIG.exists():
        with EMULATOR_LOCAL_CONFIG.open(encoding="utf-8") as handle:
            config.update(json.load(handle))
    return config


def _sdk_from_local_properties() -> Path | None:
    if not LOCAL_PROPERTIES.exists():
        return None
    for line in LOCAL_PROPERTIES.read_text(encoding="utf-8").splitlines():
        match = re.match(r"^\s*sdk\.dir=(.+)\s*$", line)
        if match:
            raw = match.group(1).strip()
            return Path(raw.replace("\\:", ":")).expanduser()
    return None


def _avd_home_from_config() -> Path | None:
    config = load_emulator_config()
    avd_home = config.get("avd_home")
    if not avd_home:
        return None
    return Path(avd_home).expanduser()


def avd_home_candidates() -> list[Path]:
    seen: set[Path] = set()
    ordered: list[Path] = []

    def add(path: Path | None) -> None:
        if path is None:
            return
        resolved = path.expanduser().resolve()
        if resolved in seen:
            return
        seen.add(resolved)
        ordered.append(resolved)

    if os.environ.get("ANDROID_AVD_HOME"):
        add(Path(os.environ["ANDROID_AVD_HOME"]))
    add(_avd_home_from_config())
    add(Path.home() / ".android" / "avd")
    return ordered


def resolve_avd_home() -> Path:
    for avd_home in avd_home_candidates():
        if avd_home.exists():
            return avd_home
    return avd_home_candidates()[-1]


def sdk_candidates() -> list[Path]:
    seen: set[Path] = set()
    ordered: list[Path] = []

    def add(path: Path | None) -> None:
        if path is None:
            return
        resolved = path.expanduser().resolve()
        if resolved in seen:
            return
        seen.add(resolved)
        ordered.append(resolved)

    for env_name in ("ANDROID_HOME", "ANDROID_SDK_ROOT"):
        add(Path(os.environ[env_name]) if os.environ.get(env_name) else None)

    config = load_emulator_config()
    if config.get("sdk_dir"):
        add(Path(config["sdk_dir"]))

    add(Path.home() / "AppData" / "Local" / "Android" / "Sdk")
    add(Path.home() / "Library" / "Android" / "sdk")
    add(Path.home() / "Android" / "Sdk")
    add(_sdk_from_local_properties())
    return ordered


def harness_env() -> dict[str, str]:
    env = os.environ.copy()
    sdk = find_android_sdk()
    env["ANDROID_HOME"] = str(sdk)
    env["ANDROID_SDK_ROOT"] = str(sdk)
    env["ANDROID_AVD_HOME"] = str(resolve_avd_home())
    return env


def _tool_name(base: str) -> str:
    return f"{base}.exe" if sys.platform == "win32" else base


def find_android_sdk(*, require_emulator: bool = False) -> Path:
    adb_name = _tool_name("adb")
    emulator_name = _tool_name("emulator")

    for sdk in sdk_candidates():
        adb = sdk / "platform-tools" / adb_name
        if not adb.exists():
            continue
        if require_emulator:
            emulator = sdk / "emulator" / emulator_name
            if not emulator.exists():
                continue
        return sdk

    requirement = "platform-tools/adb"
    if require_emulator:
        requirement += " and emulator/emulator"
    raise FileNotFoundError(
        f"Android SDK with {requirement} not found. "
        "Install Android Studio SDK or set sdk.dir in android/local.properties."
    )


def adb_path(sdk: Path | None = None) -> Path:
    sdk = sdk or find_android_sdk()
    path = sdk / "platform-tools" / _tool_name("adb")
    if not path.exists():
        raise FileNotFoundError(f"missing adb: {path}")
    return path


def emulator_path(sdk: Path | None = None) -> Path:
    sdk = sdk or find_android_sdk(require_emulator=True)
    path = sdk / "emulator" / _tool_name("emulator")
    if not path.exists():
        raise FileNotFoundError(
            f"missing emulator binary: {path}. Install Android Emulator via SDK Manager."
        )
    return path


def run_command(
    args: list[str | Path],
    *,
    check: bool = True,
    env: dict[str, str] | None = None,
) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        [str(arg) for arg in args],
        check=check,
        text=True,
        capture_output=True,
        env=env or harness_env(),
    )


def list_avds(emulator_bin: Path | None = None) -> list[str]:
    emulator_bin = emulator_bin or emulator_path()
    result = run_command([emulator_bin, "-list-avds"])
    return [line.strip() for line in result.stdout.splitlines() if line.strip()]


def connected_devices(adb_bin: Path | None = None) -> list[str]:
    adb_bin = adb_bin or adb_path()
    result = run_command([adb_bin, "devices"])
    devices: list[str] = []
    for line in result.stdout.splitlines()[1:]:
        if not line.strip():
            continue
        parts = line.split()
        if len(parts) >= 2 and parts[1] == "device":
            devices.append(parts[0])
    return devices


def is_boot_complete(adb_bin: Path) -> bool:
    result = run_command(
        [adb_bin, "shell", "getprop", "sys.boot_completed"],
        check=False,
    )
    return result.returncode == 0 and result.stdout.strip() == "1"


def wait_for_boot(adb_bin: Path, timeout_sec: int) -> bool:
    import time

    deadline = time.monotonic() + timeout_sec
    run_command([adb_bin, "wait-for-device"], check=False)
    while time.monotonic() < deadline:
        if is_boot_complete(adb_bin):
            return True
        time.sleep(2)
    return False
