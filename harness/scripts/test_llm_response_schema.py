#!/usr/bin/env python3
"""Validate mock LLM audio-control JSON fixtures."""

from __future__ import annotations

import json
import sys
from pathlib import Path

HARNESS_ROOT = Path(__file__).resolve().parents[1]
FIXTURE = HARNESS_ROOT / "tests" / "mock_llm_response.json"
REQUIRED = ("ducking_volume", "ramp_duration", "script")


def main() -> int:
    data = json.loads(FIXTURE.read_text(encoding="utf-8"))
    failed = 0
    for key in REQUIRED:
        if key not in data:
            print(f"FAIL: missing key {key!r}")
            failed += 1
    if not isinstance(data.get("script"), str) or not data["script"].strip():
        print("FAIL: script must be non-empty string")
        failed += 1
    for float_key in ("ducking_volume", "ramp_duration", "ramp_out_duration"):
        if float_key in data and not isinstance(data[float_key], (int, float)):
            print(f"FAIL: {float_key} must be numeric")
            failed += 1

    if failed:
        print(f"\n{failed} check(s) FAILED")
        return 1
    print("Mock LLM response schema PASSED")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
