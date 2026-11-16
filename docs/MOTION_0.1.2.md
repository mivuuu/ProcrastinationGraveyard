# Motion and native 3D integration — 0.1.2

Version: `0.1.1` / code `2` → `0.1.2` / code `3`. Application ID, signing, Room schemas, preferences and task data formats are unchanged.

## Renderer ownership and readiness

Only the Filament engine survived tab switches previously. Material loading, primitive meshes, lighting, the camera node, scene/view/renderer and the entire procedural world were recreated on each entry. That repeated CPU/GPU setup made returning to the cemetery unnecessarily expensive. Four SceneView `onFrame` callbacks were also treated as readiness, although the callback occurs before `beginFrame` and does not establish that a buffer was displayed.

`CemeteryRenderStore` now lazily owns one session for the app composition. Its materials, meshes, world/chunk cache, camera, scene, Filament view, renderer and loaders survive ordinary navigation. The camera controller remains saveable; only nearby sections remain resident. No persistent grave coordinates or records change.

The visible Android viewport is now a `TextureView`, using Filament 1.56.0's supported `UiHelper.attachTo(TextureView)` API. An unattached SceneView controller continues to own scene nodes, collision picking, camera projection, the original render loop, swapchain callbacks and the destination lifecycle. Its SurfaceView is never added to the window. Touch coordinates are forwarded unchanged. The controller's display comes from the hosted texture. This avoids a separate visible SurfaceView compositor layer and its hole-punching/z-order limitations under Compose animation. See the [installed-version UiHelper source](https://raw.githubusercontent.com/google/filament/v1.56.0/android/filament-android/src/main/java/com/google/android/filament/android/UiHelper.java) and [Android TextureView documentation](https://developer.android.com/reference/android/view/TextureView).

A one-shot `onSurfaceTextureUpdated` acknowledgment opens the touch-readiness gate after a real buffer is consumed. Generation checks reject obsolete callbacks after pauses, tab changes and disposal. The texture is non-opaque before its first buffer and sits over the existing sky color. A small static native readiness panel fades away; no full-window mask, fake cemetery image, polling, frame-count guess or artificial delay is used. The texture itself is not alpha-animated. Native-only Tasks/Settings reveals are separate from navigation, and NavHost crossfades remain disabled to prevent simultaneous cemetery destinations.

Leaving Cemetery disconnects touch/frame callbacks, removes nodes and collision listeners, destroys the controller and detaches the texture/swapchain. SceneView's lifecycle stops Choreographer work while paused. Shared world and GPU resources remain without a hidden render loop and release with the app composition. There is no renderer allocation in ordinary per-frame work.

## Camera

The fixed downward pitch changes from `atan2(12,30)` = **21.801°** to `atan2(12,42)` = **15.945°**, a **5.856°** reduction. Camera height remains 12 world units; default depth moves from 21 to 23 for front-row framing. Horizontal position, optical zoom bounds and save-state format are preserved. A new test checks the requested pitch reduction and all four row centers at maximum zoom.

The cloud/sky projection, forest, grass, grave/flower meshes, placements, materials and original object-only fly-out/360° turn are preserved. Selection still locks the exact camera position and lens; it never changes camera orientation or starts an orbit.

## Shared motion

`CemeteryMotion` centralizes smooth cubic easing and durations: press 120 ms, selection 220 ms, navigation 280 ms, native content 260 ms, panels 300 ms and cards 240 ms. A ContentObserver follows Android's animator-duration scale live. Reduced motion gives the custom transitions zero duration and disables the decorative idle loop.

- Navigation uses one sliding rounded background under equal-width tabs, with label/icon tint and press feedback; its layout never changes during selection.
- Filters animate background and label colors with stable dimensions.
- Buttons/icon buttons, switches and check controls share light press feedback; switches/checks animate state changes while preserving their full hit targets and semantics.
- Stable-key task cards animate insertions, removal and placement in the list. Completion feedback starts immediately; persistence is not delayed. Scroll state remains saveable.
- Only the sprout's leaves sway gently (±0.6°, 3.6-second half-cycle); the hill, face and screen remain stationary.
- Dialogs and sheets retain their actual windows for entrance/exit, animate native panel opacity/translation, and use one scrim. Dismissal latches the first action, disables closing controls, and handles back/outside/drag consistently. Save/delete data mutations remain immediate; only subsequent UI dismissal is deferred.
- The sheet keeps its fitted window, safe drawing insets, fixed Close footer and scrollable details. Drag cancellation settles smoothly. Grave information still appears only after the existing 3D rotation completes.

## Changed files

- `AGENTS.md`, `README.md`, `app/build.gradle.kts`, this report and verification evidence.
- `core/App.kt`, `core/Theme.kt`, new `core/Motion.kt`.
- `feature/cemetery/CameraController.kt`, `CemeteryScene.kt`, `CemeteryScreen.kt`, `GraveSheet.kt`.
- New `feature/cemetery/CemeteryRenderSession.kt`, `CemeteryTextureView.kt`, `CemeteryViewport.kt`, `SurfaceFrameGate.kt`.
- `feature/tasks/TaskScreen.kt`, `TaskEditor.kt`; `feature/settings/SettingsScreen.kt`.
- `ui/AppChrome.kt`, `Components.kt`, `DateTimePickers.kt`, `Illustration.kt`; new `ContentMotion.kt`, `OverlayMotion.kt`.
- `CameraTest.kt`, `CustomDesignTest.kt`, new `OverlayMotionTest.kt`; `VisualFixTest.kt`, `UiSmokeTest.kt`, `CustomUiTest.kt`.

## Validation

**59 JVM tests passed; all 21 Android cases passed across the full run (18) and targeted rerun (3).** The first run exposed test synchronization assumptions, documented with both raw reports and the [case-by-case matrix](verification/0.1.2/TEST_MATRIX.md). The targeted rerun and debug assembly completed successfully. Existing tests remain present; viewport tests now inspect the real TextureView and its SceneView-owned scene rather than looking for an attached SurfaceView. New assertions check first-frame sky/forest/grass pixels, absence of black/blank ready viewports, retained GPU resource identity across 20 tab round trips, one-shot readiness, camera preservation, clouds and rapid switching. UI evidence is exported before Gradle uninstalls the test application. The editor cancellation regression now waits for the confirmation window to finish animating out before checking accessibility and persisted task identity; the assertion is retained rather than replaced with a fixed sleep. Date/time flows wait for their unique picker controls and the returning editor before advancing. Tasks pixel checks await a real HWUI window-frame commit because accessibility can change before the new destination buffer is submitted; the zero-blue-pixel assertion and detached-renderer checks remain strict.

Actual emulator evidence includes [the four-row cemetery](screenshots/0.1.2/cemetery.png), [selected stone](screenshots/0.1.2/demo-selected.png), [Russian light](screenshots/0.1.2/custom-light-ru.png) and [dark](screenshots/0.1.2/custom-dark-ru.png) themes, [large text](screenshots/0.1.2/large-text-selected.png), [landscape](screenshots/0.1.2/landscape-selected.png), and the [recorded object turn](screenshots/0.1.2/cartoon-selection.mp4) / [contact sheet](screenshots/0.1.2/rotation-contact.png). Screenshots and video frames were visually inspected; the device checks also verify the exact fixed camera and restored transforms.

Final `./gradlew :app:assembleDebug` completed **BUILD SUCCESSFUL**. The verified APK is **42,229,030 bytes**, version **0.1.2 / code 3**, at `/home/setare/Procrastination Graveyard/app/build/outputs/apk/debug/app-debug.apk`. Its SHA-256 is `691e0023f9c2cfe0092f6be634b2cafe858c4ad8ed62952276f8eb328bafac01`. APK signature and permissions were checked; Internet/network-state permissions remain absent. Installation and launch succeeded. Java/native startup crash logs are empty.

A separate [navigation/form recording](screenshots/0.1.2/navigation-motion.mp4) and [contact sheet](screenshots/0.1.2/navigation-contact.png) were captured from the final installed APK and visually inspected. They show cold cemetery entry over the sky backdrop, return to Tasks, filter changes, and the real task editor opening/dismissing. There are no black viewport frames or cross-screen 3D overlays in that inspected recording. This is emulator evidence, not a physical-device frame-rate guarantee.

## Limits

Runtime QA uses a disposable Android 15 / API 35 x86_64 emulator with ANGLE/OpenGL. Physical-device GPU/OEM behavior, older Android versions and sustained mid-range-device frame-time/power measurements require broader testing. TextureView involves an additional normal window-composition step; retaining GPU resources avoids repeated setup but does not remove the initial shader/geometry initialization cost. The existing list-to-empty layout remains responsive; list-item motion uses Compose's stable-key lazy-item animation rather than delaying task data changes.
