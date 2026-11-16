# Cemetery diorama redesign — 16 November 2026

This changes the existing native application (`org.cemetery.app`). It keeps task management, notifications, local epitaphs, navigation, settings, data ownership and dependency versions intact. No external assets or network services were added.

## Scene and controls

The former path and directional/home camera toolbar are removed. Continuous grass, subtle ground patches, small flowers and low boundary bushes surround four recognizable grave rows. Two layers of reusable low-poly trees form a horizontally continuous forest behind the rear row. Four stone variants use stepped bases, softened caps, small inscription details, deterministic size and stone-tone variation. Historical resurrection still replaces a stone with a flower.

The perspective camera is 17 world units above ground, at a fixed three-quarter orientation. One finger moves only the horizontal coordinate. A predominantly vertical gesture is ignored until that gesture ends. Two fingers change lens focal length between 22 and 52 mm (default 32 mm); camera height, depth and orientation stay fixed. Drag and multi-touch gestures cannot become grave taps. There is no orbit controller or movement toolbar. The existing bottom navigation and accessible grave-record list remain.

Picking uses SceneView's real 3D collision results. Environment geometry is excluded from picking. Selection adds a thin unlit white contour, smoothly aligns the miniature horizontally and adjusts the projection shift to keep it visible above the existing Material 3 task sheet. Closing the sheet restores the previous horizontal coordinate, lens and projection shift. Saved framing survives recreation.

## Placement, persistence and rendering

Rows are centered at Z = 2.2, −1.6, −5.4 and −9.2. Each newly allocated column fills all four rows before allocation moves to the next column: center, right, left, farther right, farther left. Column spacing is 3.6 units; deterministic horizontal and depth jitter is bounded to preserve minimum spacing of 2.8 units. Coordinates and variant are saved, never recomputed on normal restart. Deleting a task does not reuse its allocated position.

Sections span 14.4 units along X, with signed indices for both sides. Five nearby sections are active on a typical phone; wide viewports can request up to nine. Distant render entities are detached and destroyed. Section decorations, individual grave miniatures, sprouts and the selection contour are reconciled separately. Updating a title or opening a sheet does not rebuild the cemetery. Two shared meshes and a cached material palette serve the whole active scene. Database metadata remains independent of loaded GPU objects.

Room schema 3 includes a deterministic one-time layout migration from schema 2. It orders existing graves by creation time and ID, then updates only X, Z and section. It preserves grave IDs, task associations, model variants, timestamps, epitaph selections/styles, resurrection, tasks and history. The allocator advances past all converted plots. Schema 1 upgrades through both registered migrations; there is no destructive fallback or app-data reset.

## Changed files

- `app/src/main/java/org/cemetery/app/feature/cemetery/`: rewritten `CemeteryScene.kt`, `CemeteryScreen.kt`, `CameraController.kt`; new `CemeteryGestures.kt` and `DioramaWorld.kt`. Existing task detail sheet retained.
- `app/src/main/java/org/cemetery/app/domain/Models.kt`: four-row deterministic placement and horizontal section lookup.
- `app/src/main/java/org/cemetery/app/data/Database.kt`, `Migrations.kt`; `core/Injection.kt`: schema 3 and migration registration. Exported schema: `app/schemas/org.cemetery.app.data.CemeteryDatabase/3.json`.
- `app/src/main/res/values/strings.xml`, `values-ru/strings.xml`: gesture and accessibility instructions.
- `app/src/test/java/org/cemetery/app/CameraTest.kt`, `DomainTest.kt`; `app/src/androidTest/java/org/cemetery/app/MigrationTest.kt`, `UiSmokeTest.kt`: gesture, placement, migration and native-scene checks.
- `AGENTS.md`, `README.md` and this report: persistent build requirement and current documentation.

## Validation

- **36 JVM tests passed**, zero failures/errors/skips: [results](verification/diorama-unit-tests.txt). Coverage includes four bands, deterministic coordinates through ordinal 5,000, collision prevention across 1,001 plots, signed section lookup, horizontal expansion, gesture arbitration, zoom limits, smooth focus restoration, saved framing, lifecycle boundaries, epitaph selection, localization parity and time-zone examples.
- **12 Android instrumentation tests passed**, zero failures/skips: [results](verification/diorama-device-tests.txt). Three migration tests cover both upgrade paths and preserved data; five repository tests cover task lifecycle/persistence/deduplication/allocation; two UI tests cover CRUD, native camera pose, horizontal/vertical/pinch arbitration, direct taps in all four rows, exact selected task, white contour nodes, restoring previous framing, flower resurrection, both tabs, Russian locale and persisted dark theme; two reminder tests cover delivery/disabling and permission denial.
- Tests ran on a separate disposable Android 15 / API 35 x86_64 emulator using ANGLE/OpenGL (`-gpu swangle`). Existing app data outside that QA emulator was not cleared or modified. Fixtures were removed by the test suite; production code creates no dummy graves.
- Both light and dark scenes were checked for visible rendered geometry after initialization. Actual captures were visually inspected for four readable bands, forest, no pathway or movement toolbar, clearly visible white selection above the sheet, and flower replacement. Shader startup on the software emulator can briefly show a blank surface; capture checks wait for geometry.
- Final APK was reinstalled without clearing data and cold-launched successfully. The empty garden was visually inspected after fixture cleanup; no dummy stones remained. Startup log inspection found no app fatal exception, native fatal signal or ANR: [log](verification/diorama-startup-logcat.txt). APK Signature Scheme v2 verification passed.
- Required final command: `./gradlew :app:assembleDebug`. See [build output](verification/diorama-final-build.txt). Delivery APK: `app/build/outputs/apk/debug/app-debug.apk`.

[Light scene](screenshots/diorama-cemetery.png) · [Zoomed selection](screenshots/diorama-focused-zoom.png) · [Front row](screenshots/diorama-row-0.png) · [Second row](screenshots/diorama-row-1.png) · [Third row](screenshots/diorama-row-2.png) · [Rear row](screenshots/diorama-row-3.png) · [Flower](screenshots/diorama-flower.png) · [Russian dark scene](screenshots/diorama-cemetery-dark.png) · [Russian settings](screenshots/diorama-settings-ru-dark.png) · [Empty garden](screenshots/diorama-empty.png)

## API verification

The integration was checked against the pinned SceneView 2.3.0 sources and Filament 1.56.0, including the [lens-shift implementation](https://github.com/google/filament/blob/v1.56.0/filament/src/details/Camera.h), [public projection getters](https://github.com/google/filament/blob/v1.56.0/filament/src/Camera.cpp) and [SceneView camera conversions](https://github.com/SceneView/sceneview-android/blob/v2.3.0/sceneview/src/main/java/io/github/sceneview/utils/Camera.kt). Filament doubles the public shift value internally; its public projection getters omit that additional shift. Automated native-camera assertions therefore verify lens/pose and horizontal centering, while actual rendered captures verify vertical sheet clearance and the visible outline.

## Limits

Physical-device GPU performance, exhaustive tablet/landscape composition and TalkBack gesture coverage remain unverified. The active scene is bounded and placement tests cover thousands of coordinates; thousands of historical tasks have not been performance-profiled on a physical device. Existing WorkManager reminders remain approximate. The APK uses debug signing and universal native libraries. Original MVP screenshots and verification remain historical evidence of the previous scene, rather than the redesigned layout.
