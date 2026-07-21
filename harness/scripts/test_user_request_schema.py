#!/usr/bin/env python3
"""
Validate user request parse JSON fixture for radio messenger UX.

Purpose: SSOT schema for ▶ Send LLM/local parser output.
Fixture: harness/tests/mock_user_request.json
Run: python harness/scripts/test_user_request_schema.py
"""
import json
import sys
from pathlib import Path

HARNESS_ROOT = Path(__file__).resolve().parents[1]
FIXTURE = HARNESS_ROOT / "tests" / "mock_user_request.json"
VALID_INTENTS = {"explicit_tracks", "mood_request", "chat_only", "mixed"}


def main() -> int:
    data = json.loads(FIXTURE.read_text(encoding="utf-8"))
    failed = 0
    intent = data.get("intent")
    if intent not in VALID_INTENTS:
        print(f"FAIL: invalid intent {intent!r}")
        failed += 1
    tracks = data.get("tracks")
    if not isinstance(tracks, list):
        print("FAIL: tracks must be a list")
        failed += 1
    else:
        for i, track in enumerate(tracks):
            if not isinstance(track, dict):
                print(f"FAIL: tracks[{i}] must be object")
                failed += 1
                continue
            if not str(track.get("search_query", "")).strip():
                print(f"FAIL: tracks[{i}].search_query required")
                failed += 1
    if failed:
        print(f"\n{failed} check(s) FAILED")
        return 1
    print("User request schema PASSED")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
