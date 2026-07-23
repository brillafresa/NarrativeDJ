# NarrativeDJ — Project Scope

> **Read this first.** This document defines what **this repository implements**.
> The full product vision lives in [research.md](research.md); only a subset is in scope here.

## One-line declaration

NarrativeDJ implements **personal BYOK Android MVP** — an in-app YouTube Music WebView client with on-device LLM/TTS DJ commentary and cushion (bridge) song routing. It does **not** implement the entire research roadmap (B2B streaming, admin CRUD, GPS guard, Desktop).

## Document hierarchy

```text
research.md          = Full product vision (original SoT, partially adopted)
project-scope.md     = ★ This repository's goal scope ★ (this file)
development-plan.md  = MVP completion roadmap (Phase A–E)
TODO.md              = Short-term task checklist
CHANGELOG.md         = Completed work history
```

When documents conflict on **what to build**, priority is:

1. Working source code (tests passing)
2. [project-scope.md](project-scope.md)
3. [.cursorrules](../.cursorrules) / [HARNESS_RULES.md](../HARNESS_RULES.md)
4. [development-plan.md](development-plan.md)
5. [research.md](research.md) (vision reference only)

## Completion terminology

| Term | Meaning |
|------|---------|
| **Scaffold Complete** | Classes, UI, harness unit/instrumentation tests exist |
| **Feature Complete** | User scenario works end-to-end on device/emulator |
| **Release Ready** | Harness green + manual QA + [release.md](release.md) checklist |

CHANGELOG v0.6.0 "Phase 1–3 complete" means **Scaffold Complete**, not Feature Complete for all research features.

## In Scope (MVP)

| Area | Target |
|------|--------|
| Architecture A BYOK | User YT Music session + **required Gemini** API key on-device |
| WebView control | SVD, CSP bypass, now-playing parse, search/play JS API |
| Cushion algorithm | Pool pick by similarity + invented bridge `search_query` (LLM); vector catalog = harness parity only |
| Space profiles | Frozen types for admin/B2B tests; **not** in MainActivity UI |
| AI DJ pipeline | Transition ments (Gemini + Android TTS) between tracks |
| Radio UX | ▶ Send → Gemini parse → pool → queue-after-current / search |
| Background playback | Foreground service, wake lock, MediaSession transport + metadata |
| i18n | System locale (KO/EN resources); no in-app language menu |
| Release | Signed APK path, CI harness verification |

## Partial (MVP subset of research)

| research section | Included | Excluded |
|------------------|----------|----------|
| 2.2 Space profiles | Manual templates | GPS auto-detect, schedule auto-switch |
| 2.3 AI DJ pipeline | story → LLM → TTS → ducking | Weather/Time/Trend auto context (STAGE 1) |
| 3.2 Background | Service + notification + transport | Full radio programming timeline |

## Deferred (not in MVP — do not implement)

| Feature | Notes |
|---------|-------|
| B2B partner streaming | HTTP client + mock exist; **frozen** — no ExoPlayer, no live API |
| Admin console CRUD | Read-only HTML viewer; **frozen** (not in production menu) |
| `SchedulePlanner` runtime | Unit tests only; not wired to MainActivity |
| GPS commercial guard | Scaffold only; no location APIs |
| OpenAI BYOK | Removed in v0.9.0 — Gemini-only |
| CI release signing job | Optional post-MVP |

## Out of Scope

| Feature | Notes |
|---------|-------|
| Desktop client | Mentioned in research architecture only |
| Provider server | Architecture A forbids server-side streaming/AI billing |
| Server-side API key storage | BYOK local only |

## research.md adoption matrix

| research.md section | Status | MVP note |
|---------------------|--------|----------|
| 1.x Market analysis | Reference | Context only |
| 2.1 Cushion algorithm | **In Scope** | Playback execution is MVP goal |
| 2.2 Space profiles | **Partial** | Manual selection |
| 2.3 AI DJ pipeline | **Partial** | No auto weather/time/trend |
| 3.1 WebView BYOK | **In Scope** | Core |
| 3.2 Background | **In Scope** | Phase D |
| 3.3 Ducking | **In Scope** | Unified graph for all TTS |
| 4.2 Phase 3 B2B | **Deferred** | Frozen scaffold |
| 4.3 GPS guard | **Deferred** | |
| Admin multi-location | **Deferred** | |
| Desktop | **Out of Scope** | |

## Frozen scaffold (keep, do not extend)

These files exist for Phase 3 scaffold and regression tests. **Do not add features or expand tests** unless scope changes.

| Path | Role |
|------|------|
| `android/.../b2b/` | B2B plugin stub |
| `android/.../admin/` | Admin console read-only |
| `android/app/src/main/assets/admin/` | Admin HTML/JS |
| `harness/tests/mock_b2b_schedule.json` | B2B schema regression |
| `harness/scripts/test_b2b_schedule_schema.py` | Keep in pre-push for regression |

## Worker checklist

Before adding a feature:

1. Is it listed **In Scope** or **Partial** above?
2. If not → record in [TODO.md](../TODO.md) under **Deferred** only; do not implement.
3. If yes → follow [development-plan.md](development-plan.md) phase order and [HARNESS_RULES.md](../HARNESS_RULES.md).

Last updated: 2026-07-21 — scope decision: personal BYOK MVP (option C), B2B/Admin excluded.
