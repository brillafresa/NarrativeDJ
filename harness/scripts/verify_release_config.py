#!/usr/bin/env python3
"""Verify release signing scaffold is present and secrets are not committed."""

from __future__ import annotations

import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
ANDROID_DIR = REPO_ROOT / "android"
EXAMPLE = ANDROID_DIR / "signing.properties.example"
RELEASE_DOC = REPO_ROOT / "docs" / "release.md"


def main() -> int:
    failed = 0
    if not EXAMPLE.exists():
        print(f"FAIL: missing {EXAMPLE.relative_to(REPO_ROOT)}")
        failed += 1
    else:
        print(f"PASS: {EXAMPLE.relative_to(REPO_ROOT)} exists")

    if not RELEASE_DOC.exists():
        print(f"FAIL: missing {RELEASE_DOC.relative_to(REPO_ROOT)}")
        failed += 1
    else:
        print(f"PASS: {RELEASE_DOC.relative_to(REPO_ROOT)} exists")

    committed_signing = ANDROID_DIR / "signing.properties"
    if committed_signing.exists():
        print("WARN: android/signing.properties exists locally (must stay gitignored)")

    for keystore in REPO_ROOT.rglob("*.keystore"):
        if keystore.name == "debug.keystore":
            continue
        print(f"FAIL: keystore must not be in repo: {keystore.relative_to(REPO_ROOT)}")
        failed += 1

    if failed:
        print(f"\n{failed} check(s) FAILED")
        return 1
    print("\nRelease config scaffold PASSED")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
