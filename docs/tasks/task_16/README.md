# task_16 - E2E experiment: real CameraX tree capture switch (virtualscene poster probe)

## Why

The Route 2 CI e2e uses a `.local` camera bypass because CameraX capture is unreliable on the headless
emulator (API 30 + swiftshader). An independent review (see the monorepo
`apps/e2e/docs/emulator-camera-limitation.md`) found that a newer image + `-camera-back virtualscene`
plus a `-virtualscene-poster` can place a real tree image in the scene, so the BACK-lens tree capture
might work with the real CameraX path. This task adds a switch to probe that in an isolated CI run.

## Change (`camera/Camera.kt`)

- New `isRealTreeCameraRequested()`: reads the system prop `debug.e2e.realtree` via `getprop`.
- The `.local` capture bypass is now skipped for the TREE (back lens, `isSelfie == false`) when that
  prop == "1", so real CameraX runs. The SELFIE (front lens) always keeps the bypass - virtualscene is
  back-lens only.
- Added `bindToLifecycle OK/FAILED` logs (tag `CameraXApp`) and a "REAL camera path" marker, so the CI
  logcat shows whether the camera binds and whether `takePicture` fires (`Photo capture succeeded` /
  `Photo capture failed`).

## Notes

Experiment-only, `.local`-gated, off by default (prop unset -> normal bypass). No behaviour change to
the green pipeline. Enabled only on the dedicated experiment branch via
`adb shell setprop debug.e2e.realtree 1` + `-camera-back virtualscene -virtualscene-poster ...`.
