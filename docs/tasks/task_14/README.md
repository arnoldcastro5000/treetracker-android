# task_14 - `.local` camera capture bypass for headless CI e2e

## Why

The Route 2 end-to-end suite (`apps/e2e` in the monorepo) drives this app in a HEADLESS
`reactivecircus/android-emulator-runner` emulator. CameraX `ImageCapture.takePicture(...)` is a known
no-op on the emulator: its `OnImageSavedCallback` never fires, so the selfie/tree capture shutter does
nothing and the flow never advances. Config-only fixes cannot help (the selfie uses the FRONT lens,
which `virtualscene` cannot serve), and real camera image-injection is a proprietary cloud feature.

## Change

`camera/Camera.kt` Camera `factory`: when `BuildConfig.BUILD_TYPE == "local"`, wire
`cameraControl.captureListener` at the TOP of the factory - independent of the CameraX provider/bind -
to feed the bundled `assets/testtreeimage.jpg` (via `ImageUtils.createTestImageFile`) through the same
`resizeImage`/`orientImage` post-processing and `onImageCaptured(file)`, then `return@also` to skip
camera setup entirely. Wiring it here (not inside the provider callback) matters: on the emulator the
camera often never binds, so a callback-assigned listener stays null and the always-enabled shutter is
a no-op. The capture button (`CaptureButton`, always enabled) then fires the bypass regardless of
camera hardware.

Test-only: gated on the `local` build variant (already test-only, `applicationIdSuffix .local`,
static-cred LocalStack S3). `release` / `prerelease` / `dev` / `debug` are unchanged - they still use
the live camera. Applies to both the onboarding selfie and the tree capture (one shared
`captureListener`).

## Verification

Built + run by the monorepo CI workflow `android-e2e-route2.yml` (stage 2/3) against a co-located
LocalStack + k3s backend. The real `.local` APK now completes capture -> upload -> `field_data.raw_capture`.
