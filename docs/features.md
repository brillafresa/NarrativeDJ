# Features

Summary of [research.md](research.md) Section 2. See research.md on conflict.

## Cushion algorithm

### Prose formula (2.1)

Feature vector: $\mathbf{v} = [B, E, V, D, A, \mathbf{g}]$

Distance (prose):

$$D = \sqrt{w_B \Delta B^2 + w_E \Delta E^2 + w_V \Delta V^2} + w_g (1 - \cos\_sim(\mathbf{g}_i, \mathbf{g}_j))$$

Weights: $w_B=0.25$, $w_E=0.25$, $w_V=0.20$, $w_g=0.30$

### Reference implementation (Python/Kotlin — use this)

Vector: `[BPM, Energy, Valence, embedding...]` — **D and A not used** in reference code.

```text
bpm_dist    = |BPM1 - BPM2| / 200
energy_dist = |E1 - E2|
valence_dist= |V1 - V2|
cosine_dist = 1 - cos_sim(emb1, emb2)
D = 0.25*bpm + 0.25*energy + 0.20*valence + 0.30*cosine
```

- `alpha_threshold = 0.55`, `max_bridges = 2`
- `D(current, target) <= threshold` → `[]` (direct)
- Each bridge hop: `d < threshold` (strict)
- No path → `None` (Drop)

### Canonical example (2.1)

몽중인 → California Dreamin' → Hotel California → Sweet Child O' Mine

> Phase 1 roadmap (4.2) mentions a 1-bridge path only; harness validates **2.1 two-bridge** path.

## Space profile mode (2.2)

| Template | BPM | Energy | Mood |
|----------|-----|--------|------|
| Cozy brunch café | 80–110 | 0.35–0.50 | Calm, warm |
| Analog LP bar | 70–100 | 0.25–0.45 | Nostalgic |
| Quiet bookstore | 60–90 | 0.10–0.30 | Focused |

## AI DJ pipeline (2.3)

1. Context parsing (story, weather, time, trend)
2. Radio script (persona, cushion bridge narration, SSML)
3. Audio control JSON (ducking volume, ramp, script, ssml)

## Radio-style programming

Flexible timeline: not rigid 1-song-1-commentary; supports multi-song blocks between DJ lines.
