# Redmi Fold Demo Port Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a fullscreen Android application that lets the user choose a gallery image, then drives the approved fold-warp optical effect from the phone's motion sensors while the right side is lifted.

**Architecture:** The entire physical display represents the moving panel and its left edge represents the virtual Y-axis hinge. A `TYPE_GAME_ROTATION_VECTOR` controller captures the post-picker pose as zero, extracts relative local-Y rotation, and maps its magnitude to normalized fold progress. An OpenGL ES renderer ports the approved homography, 13-tap X blur, RGB dispersion, black missing corners, and inward corner mask; static exported progress, camera, frame coordinates, and debug guides are intentionally ignored.

**Tech Stack:** Kotlin 2.2, Android Gradle Plugin 9.0, Gradle 9.1, Android SDK 36, OpenGL ES 3.0, Android `SensorManager`, platform document picker, JUnit 4.

---

### Task 1: Scaffold the standalone Android project

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle.properties`
- Create: `app/build.gradle.kts`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/res/values/styles.xml`

- [x] Create a single `app` module with package `com.redmi.folddemo`, `compileSdk/targetSdk 36`, `minSdk 29`, portrait orientation, fullscreen no-action-bar theme, and OpenGL ES 3 requirement.
- [x] Generate a Gradle 9.1 wrapper from the locally verified HelloAndroid wrapper.
- [x] Run the project wrapper through build, test, and lint tasks with exit code 0.

### Task 2: Parse only approved visual calibration values

**Files:**
- Create: `app/src/main/assets/demo_fold_config.json`
- Create: `app/src/main/java/com/redmi/folddemo/FoldConfig.kt`
- Test: `app/src/test/java/com/redmi/folddemo/FoldConfigTest.kt`

- [x] Add a failing test asserting T1=3.25, T2=2.7, blur radius=40, shared blur/dispersion exponents=3.25/2.05, dispersion=16, and mask values 1/5/1.75/1.3.
- [x] Implement JSON parsing for `texture.warp` and `optics` only; keep a 65-degree sensor range as an app constant rather than reading exported instantaneous geometry.
- [x] Run the unit test and expect it to pass.

### Task 3: Implement sensor-to-fold mapping

**Files:**
- Create: `app/src/main/java/com/redmi/folddemo/FoldMath.kt`
- Create: `app/src/main/java/com/redmi/folddemo/FoldSensorController.kt`
- Test: `app/src/test/java/com/redmi/folddemo/FoldMathTest.kt`

- [x] Add failing tests for dead-zone removal, clamping, direction-independent right-edge lift magnitude, and homography coefficients at progress 0 and 1.
- [x] Implement pure angle/progress and homography functions.
- [x] Implement baseline-relative quaternion twist extraction around device-local Y using `TYPE_GAME_ROTATION_VECTOR`, with exponential smoothing and gyroscope-feature validation.
- [x] Run unit tests and expect them to pass.

### Task 4: Port the approved optical renderer

**Files:**
- Create: `app/src/main/java/com/redmi/folddemo/FoldRenderer.kt`
- Create: `app/src/main/java/com/redmi/folddemo/FoldSurfaceView.kt`

- [x] Render a fullscreen quad with abstract domain 0..9 by 0..16 so the selected image fills the real display at progress 0.
- [x] Port the single continuous inverse homography and paint out-of-domain missing corners black.
- [x] Port the 13-tap Gaussian X blur, per-channel X dispersion, and post-optics inward black corner mask using the exported values.
- [x] Upload decoded gallery bitmaps as clamped linear-filtered textures and update progress thread-safely from the sensor callback.

### Task 5: Add picker-first, UI-free application flow

**Files:**
- Create: `app/src/main/java/com/redmi/folddemo/MainActivity.kt`
- Create: `README.md`

- [x] Launch `ACTION_OPEN_DOCUMENT` for `image/*` on each fresh app launch while the renderer stays black.
- [x] Decode the selected image off the UI thread, enter immersive mode, then establish the current phone pose as sensor zero and start rendering.
- [x] Stop sensors when the Activity pauses; canceling selection closes the Activity without adding on-screen controls.
- [x] Document the physical gesture, calibration behavior, visual parameters used, build command, and known need for user-assisted hand-motion validation.

### Task 6: Build and perform unattended device checks

**Files:**
- Modify only if a build or runtime defect is found in the files above.

- [x] Run `gradlew.bat testDebugUnitTest assembleDebug` and expect all tests plus APK assembly to pass.
- [x] Inspect the APK with `apkanalyzer`/`aapt2` and confirm package, API levels, Activity, and gyroscope/OpenGL requirements.
- [x] Install with `adb install -r` only while the single authorized Redmi remains connected.
- [x] Start the Activity and verify the system image picker is foreground.
- [x] Capture filtered Logcat, confirm the renderer initializes, and complete user-assisted image and motion validation.

Final device result: after the user unlocked the phone and approved USB installation, the APK installed successfully; the picker launched, OpenGL ES 3.2 initialized on Adreno 830, and the user accepted the single-side fold behavior on hardware.
