# Reference-driven cartoon cemetery — 16 November 2026

The existing `org.cemetery.app` application is retained. The user's first attachment is the visual target; the second attachment is the former scene, not an art reference. No reference bitmap, external model, texture or font is bundled in the APK. All new artwork is original procedural geometry under the application's MIT license. Dependencies and their licenses are unchanged.

## Environment

- A blue gradient, two soft flat-bottom cloud silhouettes and three overlapping shades of triangular evergreen forest replace the gray sky and spherical tree crowns.
- Bright green continuous grass has broad irregular color patches, sparse tufts and white flowers with yellow centers. A thin ground plane extends behind the forest and far beyond the camera's visible foreground; there are no platform walls, borders or pathways.
- Four beveled headstone silhouettes are actual extruded solid meshes with front, back, sides and 0.46-unit depth. A simple front symbol, small earth mound and tufts follow the reference. Restrained normal-based shading makes volume readable during rotation without realistic textures or expensive effects.
- The camera has a near-frontal, approximately 22-degree downward view (height 12, depth 21). The sky stays blue in both app themes, as requested; the existing Material 3 app UI still follows light/dark settings.

## Four rows and normal navigation

The existing schema-3 coordinates are already compatible and are preserved byte-for-byte. The four row centers remain at Z = 2.2, −1.6, −5.4 and −9.2, with bounded deterministic variation. New columns fill all four rows, then alternate right and left from the origin. The persistent monotonic allocator and existing migrations remain intact. No database reset, new migration, task lifecycle change or grave deduplication change is required.

Horizontal one-finger swipes move only camera X. Vertical gestures do not navigate. Pinching smoothly adjusts perspective focal length from 25 to 44 mm, with no ground-plane movement or rotation. Built-in orbit controls are disabled. Signed horizontal chunks load only the nearby environment and miniatures; a selected grave is temporarily pinned if opened from a distant task/list record.

## Object-only fly-out and rotation

A grave's parts are children of one transient rendering group. Selection moves this actual 3D group, so no persistent duplicate or separate task record is created. Its saved plot and all associations remain untouched.

1. The camera's current X, lens, height and orientation are frozen; normal cemetery gestures and additional selection taps are blocked.
2. The rendering group eases from its plot to a foreground position calculated from the camera's view geometry and available space above the task panel. Apparent size increases through perspective and scale. Translation completes in 0.95 seconds.
3. The same group's local Y rotation moves from 0 to exactly 360 degrees over 1.35 seconds, with smooth acceleration/deceleration. Its side faces and plain back are real geometry.
4. Only after the turn completes does the existing localized task information panel open. A thin white geometry outline emphasizes the object. The panel's scrollable content is height-limited so it leaves the selected object visible.
5. Dismissing returns the group over 0.48 seconds to exactly the saved X/Z, zero elevation, unit group scale and original orientation. The camera remains unchanged. Navigation unlocks after restoration.

Early Back cancellation, repeated taps, tab disposal, deleted graves, configuration changes and pause/resume have explicit cleanup paths. Camera framing is saved independently. Reduced-motion system settings skip the large transform/spin and open the same information interface immediately.

## Performance and architecture

`CartoonMeshFactory` owns reusable cube, sphere, thin plane, evergreen triangle, cloud, irregular patch and four headstone primitives. Background sections are combined by material into a small set of renderables. Grave parts and contours use a bounded cache keyed by model variant and component category, shared across records; a stone typically needs four renderables and an outline one. Section batch buffers are destroyed on unload. Materials are cached; flat environment colors use unlit shading. Only the selected group and outline change transforms per animation frame. Ordinary recomposition and panel opening do not rebuild the environment or other grave meshes. Distant sections are detached before their nodes are destroyed. No per-frame database writes, networking, scene-wide rotations, blur or model downloads occur.

## Modified files

- `feature/cemetery/CartoonGeometry.kt` (new): shared procedural meshes and beveled solid extrusion.
- `feature/cemetery/StoneProfiles.kt` (new): four rounded silhouettes.
- `feature/cemetery/GravePresentation.kt` (new): testable selection/return state machine and perspective framing.
- `feature/cemetery/DioramaWorld.kt`: cartoon sections, shared resources and grouped grave transforms.
- `feature/cemetery/CemeteryScene.kt`: renderer integration, palette, fixed camera, gestures, lifecycle and presentation callbacks.
- `feature/cemetery/CameraController.kt`: lower framing, optical zoom and exact presentation locking; compatibility with old saved navigation frames.
- `feature/cemetery/CemeteryScreen.kt`: records control and compact landscape viewport layout.
- `feature/cemetery/GraveSheet.kt`: bounded, scrollable Material 3 task panel with reserved presentation space.
- `core/App.kt`: delayed information panel, selection state and Back/navigation coordination.
- `res/values/strings.xml`, `res/values-ru/strings.xml`: localized interaction accessibility description.
- `materials/garden.mat`, `assets/materials/garden.filamat`: original lightweight cartoon stone shader, compiled with Filament 1.56.0 for OpenGL and Vulkan.
- `test/CameraTest.kt`, `test/PresentationTest.kt` (new), `androidTest/UiSmokeTest.kt`: updated and additional checks; existing domain, database, migration, localization and notification tests retained.
- `AGENTS.md`, `README.md`, this report and verification captures/logs.

All source paths above are relative to `app/src/main/java/org/cemetery/app/` unless a resource/test/material path is shown.

## Validation

- **50 JVM tests passed**, zero failures/errors/skips: [results](verification/cartoon-unit-tests.txt). Existing domain/localization coverage is retained. Camera tests now enforce exact presentation locking and old saved-frame compatibility. Thirteen presentation tests cover all four side orientations, exactly one turn, stage timing, repeated taps, early cancellation, exact restoration, lifecycle reset, reduced motion and viewport framing.
- **15 Android instrumentation tests passed**, zero failures/skips: [complete regression](verification/cartoon-device-tests.txt). Three migration tests, five repository tests and two notification tests retain coverage of data associations/history, persistence, lifecycle, delivery and denied permission. Five UI tests cover CRUD, four-row real picking, camera pose/lens invariance throughout the spin, delayed information, full-object sheet clearance, exact original transforms after dismissal, cancellation, tab disposal, pause/resume, 2× text, landscape, both languages/themes and reduced motion.
- The focused recording/reduced-motion test was rerun after extending its capture to include the complete return: [result](verification/cartoon-recording-test.txt). It passed.
- The [native transform trace](verification/cartoon-native-spin.csv) records camera X/Y/Z, camera Euler angles, lens in mm, stone elevation, stone front-axis X/Z, stone Z and scale at each native frame. It shows both side directions and the back, then the front again, while camera values remain unchanged. The [actual recording](screenshots/cartoon-selection.mp4) was visually inspected for the fly-out, 3D sides/plain back, stopped front face, subsequent panel and return to the original plot. These are real emulator frames, not generated images.
- The 36-grave fixture scene measured **148 active renderable nodes** after batching: [budget](verification/cartoon-scene-budget.txt). This is a resource count, not a physical-device FPS claim.
- Real captures were compared with Image 1 for blue sky, white clouds, triangular forest, green grass, rounded gray stones, flowers, no pathways and no platform edges. Portrait/landscape and 2× text checks include a native assertion that the complete foreground object stays above the panel.
- Final APK was reinstalled without clearing app data and cold-launched successfully: [launch result](verification/cartoon-final-launch.txt). The ready empty landscape was visually inspected with no dummy graves. Startup logs contain no app fatal exception, native fatal signal, ANR or invalid vertex-attribute error: [log](verification/cartoon-startup-logcat.txt). [Signature verification](verification/cartoon-apk-signature.txt) passed; [packaged permissions](verification/cartoon-apk-permissions.txt) contain no Internet/network permission. [APK checksum](verification/cartoon-apk-sha256.txt).
- Required delivery command: `./gradlew :app:assembleDebug`. [Build output](verification/cartoon-final-build.txt). APK: `app/build/outputs/apk/debug/app-debug.apk`. The build is universal and uses debug signing.

[Reference-style scene](screenshots/cartoon-cemetery.png) · [Foreground stone](screenshots/cartoon-demo-selected.png) · [Full recorded interaction](screenshots/cartoon-selection.mp4) · [Flower](screenshots/cartoon-flower.png) · [Russian dark scene](screenshots/cartoon-cemetery-dark.png) · [Large text](screenshots/cartoon-large-text-selected.png) · [Landscape](screenshots/cartoon-landscape-selected.png) · [Reduced motion](screenshots/cartoon-reduced-motion.png) · [Empty garden](screenshots/cartoon-empty.png)

## Limits

Physical-device GPU performance and exhaustive TalkBack/tablet testing remain unverified. Software-emulator shader startup can take several seconds and show a loading/blank surface before geometry appears; device capture checks wait for visible geometry. Screen recording under software emulation can drop frames. Native animation transforms and the actual visible 3D turn/return were verified; a stable hardware frame-rate target has not been certified. The renderer bounds GPU resources, while the existing repository still supplies lightweight historical metadata for lists. Thousands of historical tasks have not been profiled on physical hardware. The APK is debug-signed and contains universal native renderer libraries. Reduced motion intentionally omits the spin. Existing WorkManager reminder delivery remains approximate.
