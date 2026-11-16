# Procrastination Cemetery

A native, offline Android productivity app. Unfinished commitments become persistent gravestones; completing them brings flowers. There are no accounts, ads, analytics, scores, or cloud services.

## Stack and requirements

Kotlin 2.1.10, AppCompat 1.7.0 for Android app locales, Compose Foundation with a custom design system (BOM 2025.02.00), Navigation 2.8.9, SceneView 2.3.0 / Google Filament 1.56.0, Hilt 2.55, Room 2.6.1, DataStore 1.1.3, Coroutines / Flow, WorkManager 2.10.0, AGP 8.7.3 and Gradle 8.9. Android 8.0 / API 26 or newer, OpenGL ES 3.0. Build with JDK 17, Android platform 35, and Build Tools 35.0.0. Dependency versions are intentionally pinned.

## Repository workflow

Source repository: [mivuuu/ProcrastinationGraveyard](https://github.com/mivuuu/ProcrastinationGraveyard). Read `AGENTS.md` before development. After each requested change, run relevant tests, build and verify the debug APK, review the diff/status for unintended files or secrets, commit the intended changes, push the current branch to `origin`, and verify the remote commit. Never force-push. Use GitHub CLI or a secure Git credential helper; do not commit local SDK paths, build products, credentials or signing keys. The debug APK stays local and is excluded from Git.

## Open and build

Open this repository root in Android Studio, select JDK 17 for Gradle, install SDK 35 and Build Tools 35.0.0, and sync. Android Studio can create `local.properties` automatically; otherwise set `sdk.dir` to your Android SDK. Select the `app` run configuration and an API 26+ device.

```sh
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
./gradlew :app:connectedDebugAndroidTest  # emulator or USB device required
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.

Run instrumented tests on a disposable emulator: they seed/delete fixture commitments and reset preferences. On this Linux host, use the emulator’s ANGLE software driver (`emulator -avd YOUR_AVD -gpu swangle`); the legacy SwiftShader GLES translator returned blank frames. API 35 with ANGLE is the verified configuration.

The APK is a universal debug build containing arm64-v8a, armeabi-v7a, x86, and x86_64 native renderer libraries as supplied upstream. It is larger than an architecture-specific release. Building initially downloads dependencies; using the installed app requires no network connection. The application manifest explicitly removes inherited Internet and network-state permissions.

## Implementation

The interface follows a custom cream/forest light theme and emerald dark theme with lime accents. CompositionLocal design tokens, explicit local Nunito variable-font weights, original Canvas icons and a smiling sprout illustration replace visible Material components. Tasks, forms, settings, confirmation dialogs, deadline pickers, grave panels and the floating bottom navigation share Foundation controls. Theme changes update immediately without recreating the Activity. System/language/theme preferences remain in the existing DataStore. The native 3D renderer and all Room/domain/notification code are unchanged by this UI redesign.

- Task creation, editing, localized deadline picking, completion, rescheduling, deletion with confirmation, active/overdue/completed lists and historical records.
- Room persistence with documented, tested schema migrations; no destructive fallback. Original deadline and missed-deadline records survive edits. Unique `(taskId, missedDeadline)` indexing prevents duplicate graves.
- Timestamp reconciliation on load, on resume, during visible use, before mutations, and through periodic WorkManager work. WorkManager execution and reminders are approximate, never exact alarms.
- Four naturally offset depth rows, deterministic center-out horizontal allocation, four beveled solid stone silhouettes, a near-frontal fixed-angle camera, ray-cast selection, an object-only fly-out with one 360-degree turn, thin white contour geometry, and custom Foundation grave sheets. A one-time schema 2→3 migration relocates historical graves while retaining their identities, task associations, history, epitaphs, variants and completion state.
- Horizontal chunk-scoped 3D objects: five to seven nearby sections on a typical phone, up to eleven on very wide viewports at the widest zoom; distant render entities are destroyed. Only changed sections or miniatures are rebuilt. Ten shared procedural primitives feed a bounded cache of grave/outline meshes; static sections are batched by material. The material palette and grave batches are reused instead of duplicated for each record. Historical metadata remains independent of the renderer. The reference-driven environment has a blue gradient sky, white cartoon clouds, three layers of triangular evergreens, bright seamless grass, broad irregular patches, tufts and daisies. There is no pathway or exposed terrain platform.
- Original unlit/normal-shaded cartoon Filament materials include both OpenGL and Vulkan shaders. The app uses Filament’s standard OpenGL backend. A narrow SceneView buffer-builder correction disables invalid normalization of floating-point tangents. Material sources are in `materials/`; rebuild them with Filament 1.56.0 `matc` via `MATC=/path/to/matc ./scripts/compile_materials.sh`. The compiled assets are checked in.
- Sprouts shrink as their deadline approaches; historical graves become flowers when their associated task is completed.
- English and Russian UI, system fallback to English, localized plurals/dates, language/theme settings, reminder lead times (15 minutes / 1 hour / 1 day), and epitaph style preferences.
- Offline resource library: 24 original epitaphs per style per language (72 English and 72 Russian); persistent selection identifiers and recent-use avoidance. Changing language changes the displayed resource, never task titles.
- One-finger horizontal swipes and two-finger optical zoom with fixed camera orientation, height and depth. Vertical swipes do not navigate. Selection freezes the camera exactly. The real stone group flies forward, turns once in 1.35 seconds, and returns to its unchanged persistent plot on dismissal. The information panel opens after the turn and stays below the object. Reduced motion skips the spin. No directional toolbar. Accessibility labels, a grave-record list, bundled Nunito typography, and scrollable forms remain available.

## Source layout

```text
app/src/main/java/org/cemetery/app/
  CemeteryApplication.kt, MainActivity.kt
  core/                  # Hilt bindings, ViewModel, app navigation, theme, locale/formatting
  domain/                # task/grave/history models, lifecycle rules, plots, epitaph selection
  data/                  # Room entities/DAO/repository, schema migrations, DataStore
  ui/                    # Foundation controls, custom navigation, pickers, Canvas artwork
  feature/tasks/         # lists and deadline editor
  feature/cemetery/      # Filament scene, camera controller, sheet and accessible record list
  feature/settings/      # preferences and offline notices
  feature/notifications/ # reminder scheduling and background reconciliation
app/src/main/res/values/ and values-ru/  # all interface copy and epitaph libraries
app/src/test/             # domain, camera, placement and resource tests
app/src/androidTest/      # database, migration and device UI tests
app/schemas/              # versioned Room schema JSON
AGENTS.md                # mandatory tests and APK build after every development request
```

## Data and deadline semantics

Deadlines are instants stored as epoch milliseconds. The editor interprets local date/time in the device's current `ZoneId`; Java Time resolves DST gaps/overlaps consistently. A task is overdue strictly after its deadline. Completing exactly at the deadline counts as on time. Approaching begins at the last quarter of the task interval, capped at one day. Rescheduling first records any missed current deadline, keeps its stone, then changes the current deadline. A later missed deadline gets its own historical stone. Deletion removes the task, history, and graves together, but never rolls back plot allocation. There is no undo or export in this MVP.

## Licenses

Application source, original copy, icon, and procedural artwork: [MIT](LICENSE). Third-party packages retain their own licenses. [THIRD_PARTY_LICENSES.md](THIRD_PARTY_LICENSES.md) records Maven POM declarations; full principal license texts and attribution are bundled in Settings → Open-source licenses and `app/src/main/assets/THIRD_PARTY_NOTICES.txt`. Nunito is bundled under SIL OFL 1.1 with its full notice and verified Latin/Cyrillic glyph coverage. No external models, photographs or generated bitmap assets are required.

## Verification status and limitations

The custom UI redesign passed 56 JVM tests and all 18 Android instrumentation tests. After visual polish, all eight UI/native interaction tests passed again; two targeted portrait/large-text/landscape panel-inset tests also passed. Actual light/dark English/Russian captures, forms, settings and a 320 dp / 2× text keyboard capture are in `docs/screenshots/custom-ui/`. The native renderer/material hashes remain unchanged.

The earlier 3D redesign passed 50 JVM tests and 15 Android instrumentation tests. The final cartoon scene was installed and visually inspected on Android 15 / API 35 with ANGLE/OpenGL, including 2× text, landscape, both themes, reduced motion, all four rows and the object-only selection animation. Real `cartoon-*` captures and a native screen recording are in `docs/screenshots/`. Native frame traces verify that the camera stays fixed while the stone turns. Earlier `diorama-*` captures document the previous layout.

See [docs/UI_REDESIGN.md](docs/UI_REDESIGN.md) for the custom UI implementation and current validation. See [docs/CARTOON_REDESIGN.md](docs/CARTOON_REDESIGN.md) for the current scene, object animation, tests, build results and limitations. [docs/DIORAMA_REDESIGN.md](docs/DIORAMA_REDESIGN.md) and [docs/VERIFICATION.md](docs/VERIFICATION.md) preserve verification of the earlier layouts. Compilation alone is not runtime verification. Physical-device GPU performance, OEM background scheduling, exhaustive accessibility review, and every screen-size combination require broader device QA. Future versions may add export/backup, richer history browsing, configurable approach thresholds, larger epitaph libraries, and release signing/ABI splits. The app deliberately has no account or synchronization feature.
