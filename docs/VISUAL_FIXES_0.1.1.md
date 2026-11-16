# Visual and navigation fixes — 0.1.1

Version `0.1.0` / code `1` advances to **`0.1.1` / code `2`**. The application ID, signing configuration, Room schema, stored task/grave positions, history and epitaph data are unchanged.

## Flower selection

The previous contour builder used a four-corner rectangle for flowers and drew it at two depths. The two rectangular loops were visible around the actual petals and stem. Flowers now use one selection group containing a small, two-tone round pool of light made from flattened reusable sphere meshes. No rectangular contour is generated for a flower. Gravestones retain their existing profile-shaped front/back contours.

Old selection groups are hidden immediately before retirement, removed from the active scene, then destroyed after detachment. The halo follows the same temporary presentation transform as the flower and disappears on restoration. Repeated selection also invalidates the cached presentation pose: with reduced motion, consecutive selections have the same travel/yaw values, which otherwise could skip the second fly-out transform.

## Cemetery surface ownership

SceneView 2.3.0 renders through an Android `SurfaceView`, a separate compositor surface. The former NavHost used default crossfades and passed the Activity lifecycle to SceneView. Outgoing destinations could therefore remain rendered while Tasks entered, and the renderer continued receiving frames while its destination was not active.

The NavHost now switches destinations without crossfading the native surface. SceneView uses the destination lifecycle, becomes invisible when paused, and is hidden/disconnected on disposal. Its normal release path detaches the swap chain and stops frame callbacks. The viewport is clipped to its layout bounds, with normal below-window surface ordering. No opaque cover or artificial timing delay is used to mask another destination.

The expensive Filament engine is remembered above the NavHost and survives tab changes. Destination-specific views, materials, meshes and scene graphs still release when their screen leaves. Camera state remains in the existing saveable controller. Tests check the same engine instance through 20 full tab round trips, plus rapid switching; retired view attachment, lifecycle, surface readiness and frame callbacks are checked after leaving. Tasks captures are checked for stale blue-sky pixels and for any attached SceneView.

## Close footer and safe areas

Close was the final item in the scrolling details column. It could end up at the screen edge or behind system navigation. The custom sheet also exposed a Compose 1.7.8 measurement issue: `usePlatformDefaultWidth=false` replaces the incoming height constraint with `configuration.screenHeightDp`, which can include bars on Android 15. Merely padding with host insets or enabling fitted bars did not constrain that measured content correctly.

The sheet now uses an explicitly fitted `MATCH_PARENT` window while retaining the platform measurement path, rather than that configuration-based height override. Remaining safe drawing insets are consumed in Compose, and sheet height comes from actual window constraints. Close has a fixed footer, a minimum 48 dp touch target and normal 16 dp design spacing underneath. Details and task actions scroll independently above it. Short windows with large text use a larger bounded sheet fraction; foreground object framing reserves matching space without moving the camera.

The palette, typography, custom controls, rounded panel and drag dismissal remain intact. There is no hardcoded system-navigation offset.

## Camera-relative clouds

The earlier sky was manually positioned in world coordinates from cached camera/viewport changes and used an assumed lens/sensor size. The sky/cloud-only root is now a child of the actual camera. Its local depth is constant, and its horizontal/vertical scale comes from the camera's real projection matrix. Horizontal camera translation is inherited directly; pinch zoom updates the background scale to retain its screen-space composition. Trees, grass, graves and presentation objects remain world-relative. Cloud meshes and materials are reused, without per-frame geometry recreation.

Native projection tests compare both cloud anchor positions during far-left/far-right swipes, both zoom directions and repeated destination changes. Selection has no independent cloud transform.

## Versioning and preserved features

Gradle holds version `0.1.1`, code `2`. Both About resources format `BuildConfig.VERSION_NAME`, so future patch versions do not require editing translated literals. `AGENTS.md` now permanently requires a patch/code increment before the final tested APK build, commit and verified push.

The custom non-Material design, English/Russian resources, light/dark palettes, task CRUD, lifecycle reconciliation, rescheduling, notifications, local epitaphs, four-row layout, horizontal gestures, optical zoom and one-turn object animation are preserved. No database migration or data reset was introduced.

## Files changed

- `AGENTS.md`, `README.md`, `app/build.gradle.kts`.
- `core/App.kt`: engine scope and destination transitions.
- `feature/cemetery/CemeteryScene.kt`: destination lifecycle, surface visibility, sky framing and repeated-selection cache.
- `feature/cemetery/CemeteryScreen.kt`: engine injection and viewport clipping.
- `feature/cemetery/DioramaWorld.kt`: flower halo, effect retirement and camera-relative sky.
- `feature/cemetery/GraveSheet.kt`: fixed Close footer and clipped scrollable body.
- `ui/Components.kt`: fitted, constrained sheet window.
- New `ui/GravePanelLayout.kt`: shared responsive sheet/presentation reservation.
- `feature/settings/SettingsScreen.kt`, English/Russian strings: actual About version.
- `CustomDesignTest.kt`: retain the immutable material/mesh/camera/gesture/animation hash checks while allowing the two intentionally revised renderer files; original baseline data remains intact.
- `UiSmokeTest.kt`: visible Close bounds/spacing assertions alongside existing real scene/camera checks.
- New `VisualFixTest.kt`: flower/halo cleanup, repeated and reduced-motion selection, completion while selected, footer scrolling, cloud projection, far swipes/zoom, 20 tab round trips, rapid switching, engine/view lifecycle, task pixel captures and installed About version.

## Verification

**56 JVM tests and all 20 Android instrumentation tests passed** in the full run. Commands/results are recorded in `verification/0.1.1/`. Actual emulator images are in `screenshots/0.1.1/`, including [light flower selection](screenshots/0.1.1/flower-light.png), [dark flower selection](screenshots/0.1.1/flower-dark.png), [2× text](screenshots/0.1.1/large-text-selected.png), [landscape](screenshots/0.1.1/landscape-selected.png), [Tasks after switching](screenshots/0.1.1/tasks-no-surface-0.png), [far-left clouds](screenshots/0.1.1/clouds-far-left.png) and [far-right clouds](screenshots/0.1.1/clouds-far-right.png).

```sh
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:connectedDebugAndroidTest
./gradlew :app:assembleDebug
```

Debug APK: `/home/setare/Procrastination Graveyard/app/build/outputs/apk/debug/app-debug.apk` (kept out of Git).

The earlier failed footer checks were retained and used to diagnose/correct the window measurement problem; assertions were not weakened. Existing task, migration, notification, localization, camera, geometry and animation tests remain present.

## Limits

Visual/device checks use the disposable API 35 x86_64 emulator with ANGLE/OpenGL. Broader physical-device GPU, API 26–34, OEM window/inset behavior and TalkBack QA remain unverified. The intended halo is a lightweight stylized pool of light, with no expensive blur/postprocessing. This patch preserves the existing capped, drag-dismissed sheet rather than adding a new navigation or rendering design.
