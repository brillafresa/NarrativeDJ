# Harness Engineering Rules

## Core Principle: Harness-First

Every feature implementation and bug fix **must** verify or build a harness before modifying production code.

1. **Before changing production code**, confirm that mock data, test scripts, or a test framework exists to validate the change.
2. If no harness exists, **build the harness first**, then implement the production change.
3. Algorithm changes must pass both:
   - `harness/scripts/test_cushion_router.py` (Python)
   - `android/app/src/test/.../CushionMusicSchedulerTest.kt` (Kotlin)

## Repository Layout

| Path | Role |
|------|------|
| `docs/research.md` | Source of Truth (full research report) |
| `harness/` | Python algorithm verification (not app runtime) |
| `android/` | Deployable Kotlin Android app |

## Source of Truth Priority

When context conflicts:

1. **Working source code** (what actually runs and passes tests)
2. **`.cursorrules`**
3. **`docs/research.md`**
4. **`docs/architecture.md` / `docs/features.md`** (summaries only)

## Change Log

Record meaningful harness or scaffolding changes in `CHANGELOG.md`.
