# Task 16: Android instrumentation tests for critical UI flows (issue #1243)

## Goal

Add on-device Compose instrumentation tests for the critical UI flows, addressing upstream
issue #1243. Fork-local (branch `e2e/selfie-asset`); not an upstream PR.

Tracked by the monorepo wayfinder tracker:
`MONO/.scratch/volunteer-e2e-ci/issues/40-build-instrumentation-test-sources.md` (build recipe) and
`research/1243-instrumentation-test-best-practices.md` (best practices).

## Approach

Written FRESH (not copied from upstream draft PR #1322, which covers 2 of 4 flows, selects by text
instead of `AutomationTags`, and is mostly per-screen smoke). The robust design uses the app's
public, stateless inner composables (`Dashboard`, `NameEntryView`, `CredentialEntryView`) rendered
with fake state and a recording action handler, so the tests are hermetic: no Koin graph, no
WorkManager, no network, no emulator camera.

## Changes

Main source (minimal, testability only):
- `view/AutomationTags.kt`: add `DASHBOARD_SYNC = "dashboard-sync"`.
- `dashboard/DashboardScreen.kt`: apply `.testTag(AutomationTags.DASHBOARD_SYNC)` to the sync button.

Gradle:
- `gradle/libs.versions.toml`: add the `androidx-test-rules` alias.
- `app/build.gradle`: `testBuildType "local"` (so androidTest runs on the `.local` camera-bypass
  variant); add `androidTest` deps: `compose-ui-test-junit4`, `test-ext-junit`, `test-rules`; add
  `localImplementation` of `ui-test-manifest` (the `local` variant does NOT inherit
  `debugImplementation`, and `createComposeRule()` needs that ComponentActivity to launch).

Monorepo (kept in sync with the `AutomationTags` invariant):
- `apps/e2e/utils/tags.ts`: add `DASHBOARD_SYNC: "dashboard-sync"` to mirror the new Kotlin tag.

Note on test naming: androidTest methods use snake_case (not the backtick `WHEN/THEN` form the unit
tests use). Backtick method names with spaces are only safe on instrumentation runtimes at API 30+;
snake_case is the more robust choice for on-device tests given `minSdk 23`.

Tests (`app/src/androidTest/.../`):
- `dashboard/DashboardScreenTest.kt` — flow 4 (dashboard counts) + flow 3 (sync trigger). Runs.
- `signup/SignUpScreenTest.kt` — flow 1 (signup name + credential + privacy dialog). Runs.
- `flows/CriticalUserJourneyTest.kt` — full signup -> capture -> dashboard -> sync via the real
  Activity. `@Ignore` until a Koin test graph lands (see its KDoc); committed as the executable
  spec. The same path is already covered cross-process by the Appium route2 E2E.

## Verification status

This host has NO Android SDK / emulator, so the sources were NOT compiled or run here. They were
cross-checked against the real composable signatures, `SignupAction` types, and `SignUpState` /
`DashboardState` constructors. First green run is the CI workflow (monorepo ticket 41) or a real
machine. `ktlint` / `detekt` could not run (need gradle + SDK).

## Follow-on

- Monorepo ticket 41: the dedicated `android-instrumentation.yml` CI workflow + the 50x
  extensive-test harness that runs `connectedLocalAndroidTest`.
- Enable `CriticalUserJourneyTest` once the test Application + custom runner (fake network/GPS) land.
- Keep `AutomationTags` in sync with `apps/e2e/utils/tags.ts` (the new `dashboard-sync` tag).
