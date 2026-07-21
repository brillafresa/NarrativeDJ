#!/usr/bin/env python3
"""
Validate DJ audio-control JSON fixtures (LLM response shape).

Purpose: Shared schema for story-segment and transition-ment LLM outputs.
Fixtures: harness/tests/mock_llm_response.json, mock_dj_transition.json
Run: python harness/scripts/test_llm_response_schema.py
"""

from __future__ import annotations

import json
import sys
from pathlib import Path

HARNESS_ROOT = Path(__file__).resolve().parents[1]
FIXTURES = (
    HARNESS_ROOT / "tests" / "mock_llm_response.json",
    HARNESS_ROOT / "tests" / "mock_dj_transition.json",
)
REQUIRED = ("ducking_volume", "ramp_duration", "script")


def validate_fixture(path: Path) -> int:
    data = json.loads(path.read_text(encoding="utf-8"))
    failed = 0
    for key in REQUIRED:
        if key not in data:
            print(f"FAIL [{path.name}]: missing key {key!r}")
            failed += 1
    if not isinstance(data.get("script"), str) or not data["script"].strip():
        print(f"FAIL [{path.name}]: script must be non-empty string")
        failed += 1
    for float_key in ("ducking_volume", "ramp_duration", "ramp_out_duration"):
        if float_key in data and not isinstance(data[float_key], (int, float)):
            print(f"FAIL [{path.name}]: {float_key} must be numeric")
            failed += 1
    if failed == 0:
        print(f"PASS: {path.name}")
    return failed


def main() -> int:
    total_failed = 0
    for fixture in FIXTURES:
        if not fixture.exists():
            print(f"FAIL: missing fixture {fixture}")
            total_failed += 1
            continue
        total_failed += validate_fixture(fixture)
    if total_failed:
        print(f"\n{total_failed} check(s) FAILED")
        return 1
    print("\nLLM audio-control schema PASSED")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
