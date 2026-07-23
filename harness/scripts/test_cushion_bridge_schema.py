#!/usr/bin/env python3
"""
Validate LLM cushion-bridge plan JSON (pool pick + optional invented bridges).

Purpose: SSOT for runtime A→(C)→B scheduling without a fixed song catalog.
Fixture: harness/tests/mock_cushion_bridge.json
Run: python harness/scripts/test_cushion_bridge_schema.py
"""
from __future__ import annotations

import json
import sys
from pathlib import Path

HARNESS_ROOT = Path(__file__).resolve().parents[1]
FIXTURE = HARNESS_ROOT / "tests" / "mock_cushion_bridge.json"
SIM_MIN, SIM_MAX = 0.0, 1.0
MAX_BRIDGES = 2


def main() -> int:
    data = json.loads(FIXTURE.read_text(encoding="utf-8"))
    failed = 0

    selected = str(data.get("selected_search_query", "")).strip()
    if not selected:
        print("FAIL: selected_search_query required")
        failed += 1

    similarity = data.get("similarity")
    if not isinstance(similarity, (int, float)) or not (SIM_MIN <= float(similarity) <= SIM_MAX):
        print(f"FAIL: similarity must be number in [{SIM_MIN}, {SIM_MAX}]")
        failed += 1

    bridges = data.get("bridge_search_queries")
    if not isinstance(bridges, list):
        print("FAIL: bridge_search_queries must be a list")
        failed += 1
    else:
        if len(bridges) > MAX_BRIDGES:
            print(f"FAIL: at most {MAX_BRIDGES} bridges")
            failed += 1
        for i, q in enumerate(bridges):
            if not str(q).strip():
                print(f"FAIL: bridge_search_queries[{i}] empty")
                failed += 1

    if failed:
        print(f"\n{failed} check(s) FAILED")
        return 1
    print("Cushion bridge schema PASSED")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
