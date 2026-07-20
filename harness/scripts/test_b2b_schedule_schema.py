#!/usr/bin/env python3
"""Validate multi-location B2B schedule fixture schema."""

from __future__ import annotations

import json
import sys
from pathlib import Path

HARNESS_ROOT = Path(__file__).resolve().parents[1]
FIXTURE = HARNESS_ROOT / "tests" / "mock_b2b_schedule.json"
VALID_PROFILES = {"cozy_brunch_cafe", "analog_lp_bar", "quiet_bookstore"}


def main() -> int:
    data = json.loads(FIXTURE.read_text(encoding="utf-8"))
    failed = 0

    if data.get("version") != 1:
        print("FAIL: version must be 1")
        failed += 1

    locations = data.get("locations")
    if not isinstance(locations, list) or not locations:
        print("FAIL: locations must be a non-empty list")
        failed += 1
        return 1

    for idx, loc in enumerate(locations):
        for key in ("location_id", "name", "profile_id", "active_hours", "timezone"):
            if key not in loc:
                print(f"FAIL: locations[{idx}] missing {key!r}")
                failed += 1
        if loc.get("profile_id") not in VALID_PROFILES:
            print(f"FAIL: locations[{idx}] unknown profile_id {loc.get('profile_id')!r}")
            failed += 1

    if failed:
        print(f"\n{failed} check(s) FAILED")
        return 1
    print(f"B2B schedule schema PASSED ({len(locations)} locations)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
