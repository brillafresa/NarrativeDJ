"""Cushion (bridge) song scheduler — reference implementation from docs/research.md §2.1."""

from __future__ import annotations

import json
from pathlib import Path
from typing import Any

import numpy as np


class CushionMusicScheduler:
    def __init__(
        self,
        track_vector_db: dict[str, np.ndarray],
        max_bridges: int = 2,
        alpha_threshold: float = 0.55,
    ):
        self.db = track_vector_db
        self.max_bridges = max_bridges
        self.threshold = alpha_threshold

    def get_distance(self, v1: np.ndarray, v2: np.ndarray) -> float:
        bpm_dist = float(np.abs(v1[0] - v2[0]) / 200.0)
        energy_dist = float(np.abs(v1[1] - v2[1]))
        valence_dist = float(np.abs(v1[2] - v2[2]))

        emb1, emb2 = v1[3:], v2[3:]
        norm1 = float(np.linalg.norm(emb1))
        norm2 = float(np.linalg.norm(emb2))
        if norm1 == 0.0 or norm2 == 0.0:
            cosine_dist = 1.0
        else:
            cos_sim = float(np.dot(emb1, emb2) / (norm1 * norm2))
            cosine_dist = 1.0 - cos_sim

        return (
            0.25 * bpm_dist
            + 0.25 * energy_dist
            + 0.20 * valence_dist
            + 0.30 * cosine_dist
        )

    def calculate_cushion_route(
        self, current_id: str, target_id: str
    ) -> list[str] | None:
        v_curr = self.db[current_id]
        v_target = self.db[target_id]

        direct_dist = self.get_distance(v_curr, v_target)
        if direct_dist <= self.threshold:
            return []

        best_single_bridge: list[str] | None = None
        min_deviation = float("inf")
        for s_id, v_cand in self.db.items():
            if s_id in (current_id, target_id):
                continue
            d1 = self.get_distance(v_curr, v_cand)
            d2 = self.get_distance(v_cand, v_target)
            if d1 < self.threshold and d2 < self.threshold:
                if (d1 + d2) < min_deviation:
                    min_deviation = d1 + d2
                    best_single_bridge = [s_id]
        if best_single_bridge:
            return best_single_bridge

        best_double_bridge: list[str] | None = None
        min_double_deviation = float("inf")
        for s1_id, v_cand1 in self.db.items():
            if s1_id in (current_id, target_id):
                continue
            d1 = self.get_distance(v_curr, v_cand1)
            if d1 >= self.threshold:
                continue
            for s2_id, v_cand2 in self.db.items():
                if s2_id in (current_id, target_id, s1_id):
                    continue
                d2 = self.get_distance(v_cand1, v_cand2)
                d3 = self.get_distance(v_cand2, v_target)
                if d2 < self.threshold and d3 < self.threshold:
                    total = d1 + d2 + d3
                    if total < min_double_deviation:
                        min_double_deviation = total
                        best_double_bridge = [s1_id, s2_id]
        if best_double_bridge:
            return best_double_bridge

        return None


def track_to_vector(track: dict[str, Any]) -> np.ndarray:
    """Build [BPM, Energy, Valence, embedding...] from mock track metadata."""
    embedding = np.array(track["embedding"], dtype=float)
    return np.concatenate(
        ([track["bpm"], track["energy"], track["valence"]], embedding)
    )


def build_vector_db(tracks: list[dict[str, Any]]) -> dict[str, np.ndarray]:
    return {track["id"]: track_to_vector(track) for track in tracks}


def load_tracks(path: Path) -> list[dict[str, Any]]:
    with path.open(encoding="utf-8") as f:
        data = json.load(f)
    return data["tracks"]
