# Run the Compose UI tests

This guide shows you how to run the Compose UI tests for the Tree Tracker Android app. It is a task
recipe. It does not explain the test design. For the wider test picture, read [Testing](testing.md).

## What this covers

The Compose UI tests are instrumented tests. They run on an Android emulator or a physical device
through the AndroidX test instrumentation, launched by the Gradle task `connectedLocalAndroidTest`
against the `local` build variant.

Each test renders one screen composable with fake state through `createComposeRule`, then asserts on
the on-screen nodes. The tests are hermetic: they use no Koin dependency-injection graph, no camera,
no network, and no backend. A small test harness (`ComposeHarness.kt`) provides the app theme and the
`NavHostController` that the shared top bars need, so each screen renders in isolation.

The suite lives in `app/src/androidTest/java/org/greenstand/android/TreeTracker/` and covers four
flows: sign-up (`SignUpScreenTest`), tree-capture review (`TreeImageReviewScreenTest`), dashboard sync
and dashboard counts (`DashboardScreenTest`). One full end-to-end journey test
(`CriticalUserJourneyTest`) stays `@Ignore` for now, so the suite reports it as skipped.

## Before you start

You need:

- The Java Development Kit, version 17.
- The Android SDK, with the `ANDROID_HOME` environment variable set to its path.
- A running Android emulator, or a physical device connected through `adb`. Use an emulator with API
  level 30 or higher. The continuous integration (CI) pipeline uses API level 30, so this matches CI.
- The credentials file `treetracker.keys.properties` at the repository root. Gradle reads this file
  during configuration, so the build fails without it. The Compose UI tests never reach AWS, so
  placeholder values are enough for a local run. For a full build, request the real file from the
  `#android_chat` Slack channel (see [Prerequisites](../getting-started/prerequisites.md)).

Confirm that `adb` sees your device before you continue:

```bash
adb devices
```

The command must list one device with the state `device`.

## Run the tests locally

1. Start an emulator (or connect a device) and wait until it is ready:

   ```bash
   adb wait-for-device
   ```

2. From the repository root, run the whole suite against the `local` build variant:

   ```bash
   JAVA_HOME=<jdk-17-home> ANDROID_HOME=<your-sdk-path> \
     ./gradlew connectedLocalAndroidTest
   ```

   The task builds the `local` variant, installs it and its test package on the device, then runs
   every test. Use `connectedLocalAndroidTest`, not `connectedAndroidTest`, because only the `local`
   variant carries the test manifest that the Compose test rule needs to launch its host activity.

3. To run one test class, pass the fully qualified class name to the instrumentation runner:

   ```bash
   ./gradlew connectedLocalAndroidTest \
     -Pandroid.testInstrumentationRunnerArguments.class=org.greenstand.android.TreeTracker.signup.SignUpScreenTest
   ```

## Run the tests in CI

The Tree Tracker monorepo runs these tests in CI, not this repository. The workflow file
`.github/workflows/android-instrumentation.yml` lives in the monorepo. It checks out this submodule,
boots an emulator, and runs `connectedLocalAndroidTest`.

The workflow has two triggers:

- A push to the CI branch runs one pass.
- A manual dispatch runs one pass, or an opt-in 50-times flake-hunt when you set the `extensive`
  input:

  ```bash
  gh workflow run android-instrumentation.yml -f extensive=true
  gh run list --workflow android-instrumentation.yml
  gh run watch <run-id>
  ```

## Check the result

For a local run, a pass ends with `BUILD SUCCESSFUL`. Read the detailed result in these files:

- The HTML report at `app/build/reports/androidTests/connected/`.
- The JUnit XML at `app/build/outputs/androidTest-results/`.

For a CI run, read the run summary page. It shows a pass/fail table and a per-class breakdown. The
50-times flake-hunt run adds a per-iteration ledger and a "50/50 pass, 0 flakes" line. The full HTML
report, the JUnit XML, and the device logcat ship in the `instrumentation-report` artifact.

## If it fails

- The message `Unable to find explicit activity class` means the build has no test manifest. Run
  `connectedLocalAndroidTest` (the `local` variant), not another connected-test task.
- An empty `adb devices` list means no device is ready. Start an emulator or connect a device, then
  run `adb wait-for-device`.
- A Gradle configuration error about a missing property means `treetracker.keys.properties` is
  absent. Place the file at the repository root.
- For a composition crash in CI, read the logcat crash report inside the `instrumentation-report`
  artifact, not only the Gradle console output. The Gradle output shows the failure count; the
  logcat file shows the stack trace.

## See also

- [Testing](testing.md): the unit tests and the wider test structure.
- [App Distribution Testing](app-distribution-testing.md): manual and release testing.
