#!/usr/bin/env python3
"""
Cushion (bridge) route verification harness.

Purpose: Validate Python reference CushionMusicScheduler against canonical scenarios.
Run: python harness/scripts/test_cushion_router.py
Fixture SSOT: harness/tests/mock_tracks.json
"""

from __future__ import annotations

import sys
from pathlib import Path

HARNESS_ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(HARNESS_ROOT / "src"))

from cushion_scheduler import (  # noqa: E402
    CushionMusicScheduler,
    build_vector_db,
    load_tracks,
)

MOCK_PATH = HARNESS_ROOT / "tests" / "mock_tracks.json"


def title_for(track_id: str, tracks_by_id: dict) -> str:
    return tracks_by_id[track_id]["title"]


def print_route(
    label: str,
    current_id: str,
    target_id: str,
    bridges: list[str] | None,
    scheduler: CushionMusicScheduler,
    tracks_by_id: dict,
) -> bool:
    print(f"\n{'=' * 60}")
    print(label)
    print(f"{'=' * 60}")

    v_curr = scheduler.db[current_id]
    v_target = scheduler.db[target_id]
    direct = scheduler.get_distance(v_curr, v_target)
    print(f"Direct distance: {direct:.3f} (threshold {scheduler.threshold})")

    if bridges is None:
        print("Result: DROP (no valid 1-2 bridge path)")
        return True

    if not bridges:
        print("Result: DIRECT (no bridge needed)")
        print(f"  {title_for(current_id, tracks_by_id)} → {title_for(target_id, tracks_by_id)}")
        return True

    path_ids = [current_id, *bridges, target_id]
    print("Route:")
    for i, track_id in enumerate(path_ids):
        print(f"  {title_for(track_id, tracks_by_id)}")
        if i < len(path_ids) - 1:
            d = scheduler.get_distance(scheduler.db[track_id], scheduler.db[path_ids[i + 1]])
            ok = "OK" if d < scheduler.threshold else "FAIL"
            print(f"    ↓ D={d:.3f} {ok}")

    print(f"Bridges: {bridges}")
    return True


def main() -> int:
    tracks = load_tracks(MOCK_PATH)
    tracks_by_id = {t["id"]: t for t in tracks}
    db = build_vector_db(tracks)
    scheduler = CushionMusicScheduler(db)

    scenarios = [
        {
            "label": "Scenario A: 몽중인 → Sweet Child O' Mine (2-bridge canonical)",
            "current": "mongjungin",
            "target": "sweet_child",
            "expected": ["california_dreamin", "hotel_california"],
        },
        {
            "label": "Scenario B: California Dreamin' → Hotel California (direct)",
            "current": "california_dreamin",
            "target": "hotel_california",
            "expected": [],
        },
        {
            "label": "Scenario C: 몽중인 → Death Metal Extreme (DROP)",
            "current": "mongjungin",
            "target": "death_metal_extreme",
            "expected": None,
        },
    ]

    failed = 0
    for scenario in scenarios:
        current = scenario["current"]
        target = scenario["target"]
        expected = scenario["expected"]
        result = scheduler.calculate_cushion_route(current, target)
        print_route(scenario["label"], current, target, result, scheduler, tracks_by_id)

        if result != expected:
            print(f"FAIL: expected {expected!r}, got {result!r}")
            failed += 1
        else:
            print("PASS")

    print(f"\n{'=' * 60}")
    if failed:
        print(f"{failed} scenario(s) FAILED")
        return 1
    print("All scenarios PASSED")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
