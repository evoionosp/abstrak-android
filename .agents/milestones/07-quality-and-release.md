# Milestone 7: Quality and Release Readiness

## Goal

Prepare Abstrak for broader release with stronger reliability, testing, observability, and maintainability.

## Main Work

- Expand unit and integration test coverage.
- Add repository/cache/sync tests.
- Add playback and download reliability tests.
- Add ViewModel tests that assert `StateFlow` and `SharedFlow` behavior.
- Standardize coroutine testing with `runTest`, test dispatchers, and Flow testing tools.
- Define the Room migration strategy, starting from the schema that ships as version 1.
- Improve error reporting and structured logging.
- Review startup, sync, and recovery flows.
- Review performance and memory impact for large libraries.
- Tighten release build configuration.
- Declare foreground service types in Play Console (App content → Foreground service permissions): `dataSync` for downloads and `mediaPlayback` for playback, each with a justification and a short demo video.

## Important Notes

- Quality should improve continuously before this milestone.
- This milestone exists to force explicit stabilization work rather than always chasing the next feature.

## Started In Repo

- 249 unit tests across all modules (domain use cases, repository sync/cache behavior, ViewModel state, logout ordering/fail-open, progress hydration), run with `runTest`, test dispatchers, Turbine, and MockK/fakes.
- Kover coverage wired with per-module floor ratchet in `settings.gradle.kts` (all floors start at 0); CI reports per-module numbers and largest gaps.
- CI gates: ktlint, Android Lint, unit tests + coverage verify, debug/release assemble; emulator tests are manual-dispatch only until a device suite exists.
- Pre-release schema policy: Room stays at version 1 with a destructive fallback and no migrations (`AGENTS.md`); proper migrations from the released schema are still required before release (see Exit Criteria).

## Exit Criteria

- Core user flows are covered by tests.
- Coroutine-driven state and event flows are tested in critical areas.
- Schema migration path is established, with the released schema as its baseline.
- Release behavior is reviewed for security, reliability, and performance.

## Risks

- Delaying stabilization work until the codebase has already grown too much.
