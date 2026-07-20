#!/usr/bin/env python3
"""
Validate YT Music selector fallback dictionary schema.

Purpose: Ensure selector_dictionary.json has required fields before WebView SVD runs.
Run: python harness/scripts/test_selector_dictionary.py
"""

from __future__ import annotations

import json
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
DICT_PATH = REPO_ROOT / "android" / "app" / "src" / "main" / "assets" / "www" / "selector_dictionary.json"
REQUIRED_FIELDS = ("title", "artist", "playButton")


def main() -> int:
    data = json.loads(DICT_PATH.read_text(encoding="utf-8"))
    failed = 0
    for field in REQUIRED_FIELDS:
        selectors = data.get(field)
        if not isinstance(selectors, list) or not selectors:
            print(f"FAIL: {field} must be a non-empty list")
            failed += 1
            continue
        if not all(isinstance(s, str) and s.strip() for s in selectors):
            print(f"FAIL: {field} contains invalid selector entries")
            failed += 1
            continue
        print(f"PASS: {field} ({len(selectors)} selectors)")

    if failed:
        print(f"\n{failed} check(s) FAILED")
        return 1
    print("\nSelector dictionary schema PASSED")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
