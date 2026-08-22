# task_14 - `.local` camera capture bypass for headless CI e2e

## Why

The Route 2 end-to-end suite (`apps/e2e` in the monorepo) drives this app in a HEADLESS
`reactivecircus/android-emulator-runner` emulator. CameraX `ImageCapture.takePicture(...)` is a known
no-op on the emulator: its `OnImageSavedCallback` never fires, so the selfie/tree capture shutter does
nothing and the flow never advances. Config-only fixes cannot help (the selfie uses the FRONT lens,
which `virtualscene` cannot serve), and real camera image-injection is a proprietary cloud feature.

## Change

`camera/Camera.kt` `captureListener`: when `BuildConfig.BUILD_TYPE == "local"`, feed the bundled
`assets/testtreeimage.jpg` (via `ImageUtils.createTestImageFile`) through the SAME
`resizeImage`/`orientImage` post-processing and `onImageCaptured(file)` callback the real path uses,
then return, skipping `imageCapture.takePicture(...)`.

Test-only: gated on the `local` build variant (already test-only, `applicationIdSuffix .local`,
static-cred LocalStack S3). `release` / `prerelease` / `dev` / `debug` are unchanged - they still use
the live camera. Applies to both the onboarding selfie and the tree capture (one shared
`captureListener`).

## Verification

Built + run by the monorepo CI workflow `android-e2e-route2.yml` (stage 2/3) against a co-located
LocalStack + k3s backend. The real `.local` APK now completes capture -> upload -> `field_data.raw_capture`.
