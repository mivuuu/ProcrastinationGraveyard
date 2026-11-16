# Verification — 15 November 2026

The implementation was compiled incrementally, installed, launched, and exercised on Android 15 / API 35 (x86_64). The final verified graphics configuration is Android Emulator 37.2.13 with `-gpu swangle` (ANGLE / OpenGL ES 3.1), using Filament's standard OpenGL backend. Screenshots are real device captures, not mockups.

## Automated results

- **27 JVM tests passed**, zero failures or errors. [Results](verification/unit-tests.txt).
- **10 Android instrumentation tests passed**, zero failures or skips. [Results](verification/device-tests.txt).
- `./gradlew :app:assembleDebug` is the required delivery build; its output is saved in [final-build.txt](verification/final-build.txt).
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.

Coverage includes creation/validation, editing, completion boundaries, overdue timestamps, repeated missed deadlines, rescheduling history, grave deduplication, non-overlapping deterministic plots, forward expansion, permanent allocation after deletion, recently used epitaph avoidance, EN/RU resource parity and plural forms, DST/time-zone examples, camera bounds/smoothing/focus restoration, Room reopen persistence, and migration from schema 1 to 2.

The device suite additionally verifies actual task creation, date/time picker confirmation, completion and confirmed deletion; native camera translation with unchanged orientation; swipe-versus-tap separation; a direct 3D grave tap; the details sheet; late-completion resurrection; language/theme preference persistence; Russian settings; notification delivery, disabling, and denied permission.

## Visual and device checks

- Actual Filament terrain, path, trees and three procedural stone variants are visible. A fixture set spans three chunks.
- A selected stone has a visible white geometry contour. Camera orientation remains fixed during movement and focus.
- Completing the selected overdue fixture produces a visible flower at its saved plot. The panel keeps its historical deadline and epitaph.
- Russian dark settings and the dark cemetery were visually inspected after frame initialization.
- Task layouts were inspected at approximately 375 dp width, 2× font scale, portrait and landscape, with system animation scales disabled. Tabs remain scrollable, and the primary action and navigation remain reachable.
- App manifest has no Internet or network-state permission. Task data, preferences, epitaphs and render assets are local; backup is disabled. The remaining runtime permission is notifications. WorkManager uses its normal boot/wake-lock integration.

[Light cemetery](screenshots/cemetery.png) · [Selected grave](screenshots/selected.png) · [Flower](screenshots/flower.png) · [Russian dark settings](screenshots/settings-ru-dark.png) · [Dark cemetery](screenshots/cemetery-dark.png) · [Large text](screenshots/large-text-small-phone.png) · [Landscape](screenshots/landscape-large-text.png)

## Known limits

- Physical-device GPU coverage, release performance profiling, OEM battery policies, actual device reboot scheduling, exhaustive TalkBack navigation, and tablet coverage remain unverified. Emulator results must not be interpreted as those checks.
- This host's legacy SwiftShader GLES translator displayed blank frames. Native Vulkan experiments also exposed emulator semaphore crashes. ANGLE/OpenGL completed the final suite; use `-gpu swangle` here. Software-emulator shader initialization can take several seconds.
- WorkManager reminder/reconciliation times are approximate and depend on Android scheduling. There are no exact alarms.
- The renderer bounds active chunks and shares mesh/material resources; the JVM placement tests cover thousands of coordinates. Thousands of simultaneous historical records have not been performance-profiled on hardware.
- The APK uses debug signing and universal native libraries. Release signing, ABI splits, backup/export, richer history views and additional environment/epitaph content are future work.

All requested MVP features are implemented. The limitations above concern broader device certification, performance and optional future features.
