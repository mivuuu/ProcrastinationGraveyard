# Custom application UI redesign

The existing Android project now uses an independent Compose Foundation design system inspired by the supplied light/dark Tasks references. No visible application code imports Compose Material or Material 3. The direct Material 3 and Material icon dependencies were replaced with explicit Foundation dependencies. Transitive compatibility libraries may remain under SceneView.

## Design and implementation

- `core/Theme.kt`: CompositionLocal colors, typography, shapes, spacing and motion preferences. Cream/forest light and deep emerald dark gradients share lime primary controls, rounded surfaces, sage secondary text and yellow decorative accents. Contrast tests cover primary/secondary text, errors and primary buttons.
- `ui/Components.kt`: BasicText, custom panels, pill buttons, icon buttons, filter tabs, BasicTextField, switch, checkbox, divider, loading indicator, Dialog-based custom dialogs and bottom sheets. Press/focus states, screen-reader roles, selected/checked/error semantics, scrollable forms, keyboard insets and minimum touch sizes are included. Reduced-motion settings disable press/toggle transitions.
- `ui/AppChrome.kt`: large header, original gear button, floating Tasks/Cemetery navigation. A compact landscape/large-type header and horizontal landscape navigation conserve usable space. Tab back stacks and task-filter state remain saved.
- `ui/Icons.kt`: original Canvas symbols, including check-in-circle Tasks and a rounded gravestone for Cemetery.
- `ui/Illustration.kt`: original resolution-independent smiling lime sprout with two leaves, closed happy eyes, curved smile, little hands, green bushes, curved grassy hill and yellow rays. The hill/bush palette changes with the theme. No bitmap mockup, emoji or stock art is used.
- `ui/DateTimePickers.kt`: custom localized calendar and time entry; locale-specific week starts and month/day names, leap-year handling, device 12/24-hour preference, AM/PM labels and input validation. Existing instant/ZoneId deadline semantics are retained.
- Locally bundled unmodified Nunito variable font, weights 500–900 selected explicitly through the installed Compose font-variation API. Actual Unicode cmap glyphs are checked for English and Russian. The SIL OFL 1.1 notice and copyright are bundled and displayed offline in Settings.

The Tasks empty screen uses the supplied hierarchy: large title, bold two-line promise heading, yellow rays, green filter pills, large sprout, centered empty-state copy, lime “Plant a promise” button and floating navigation. Filters share the available width on typical phones and scroll at narrow widths or large text settings. Populated lists use custom rounded cards with localized state/deadline, completion controls, grave access and confirmed deletion.

Task editing, validation, date/time pickers, Settings preferences, license notices, errors, deletion confirmation, accessible grave records and the grave information panel all use the same custom components. Settings retains language, theme, reminders, lead time, epitaph style and About. Theme changes update CompositionLocals immediately without recreating the Activity.

## Preserved implementation

The application ID, Room schemas/migrations, repository, domain rules, ViewModel, DataStore implementation, background reconciliation, reminders, epitaph library and offline behavior were not rewritten. Both English and Russian remain resource based; new calendar/time accessibility/validation labels have matching translations. Task titles are untouched.

The SceneView/Filament renderer, procedural assets, materials, camera controller, gestures, placement and object animation are byte-for-byte unchanged. `verification/ui-renderer-before.sha256` records the pre-change files and is checked by a JVM test. Only `CemeteryScreen.kt` and `GraveSheet.kt` changed in that feature: surrounding Compose UI and panel presentation. The 360-degree stone spin, fixed camera, four depth rows, chunk loading, foreground placement, selection lifecycle and resurrection behavior remain native 3D.

## Modified files

- `AGENTS.md`, `README.md`, `THIRD_PARTY_LICENSES.md`, this report and verification artifacts.
- `app/build.gradle.kts` (Foundation dependencies).
- `core/App.kt`, `core/Theme.kt`.
- `feature/tasks/TaskScreen.kt`, `feature/tasks/TaskEditor.kt`.
- `feature/settings/SettingsScreen.kt`.
- `feature/cemetery/CemeteryScreen.kt`, `feature/cemetery/GraveSheet.kt`.
- New `ui/AppChrome.kt`, `Components.kt`, `Icons.kt`, `Illustration.kt`, `DateTimePickers.kt`.
- `res/values/styles.xml`, `res/values/strings.xml`, `res/values-ru/strings.xml`.
- New `res/font/nunito.ttf`, `assets/fonts/Nunito-OFL.txt`.
- New JVM `CustomDesignTest.kt`, Android `CustomUiTest.kt`; existing `UiSmokeTest.kt` updated for the new localized action label. Existing tests are retained.

## Validation

The full validation run passed **56 JVM tests and 18 Android instrumentation tests** on the disposable Android 15 / API 35 x86_64 emulator with ANGLE/OpenGL. After final UI polish, all **eight UI/native scene UI tests passed again**, followed by **two targeted panel-inset/foreground tests** across portrait, 2× text, landscape, zoom and all four rows. The panel resolves navigation insets in its host composition before entering the separate Dialog window; assertions check its bounds against the system navigation area. Android’s extra dialog dim is disabled so the custom scrim is applied once. Build logs live in `verification/`; screenshots live in `screenshots/custom-ui/`. Earlier test attempts exposed stale UiAutomator node references after window changes; click helpers now reacquire only stale nodes without changing or removing assertions. An initial variable-font loading issue was fixed with explicit weight-axis settings. Russian filters were refined after screenshot inspection to keep all three labels on one line at standard phone sizes. The required `./gradlew :app:assembleDebug` completed successfully; the nonempty debug APK is `app/build/outputs/apk/debug/app-debug.apk` (exact delivery path: `/home/setare/Procrastination Graveyard/app/build/outputs/apk/debug/app-debug.apk`). See `verification/ui-final-build.txt` and `verification/ui-apk.txt` for the final command output, size and checksum.

The JVM suite covers domain lifecycle/timestamp rules, deterministic four-row placement/collision prevention, camera/selection transitions and restoration, local epitaph selection, en/ru resource completeness, localized calendar layouts, palette contrast, actual font glyph coverage, absence of Material UI imports and renderer/material hash preservation.

The device suite exercises Room persistence and migrations, task CRUD, overdue/reschedule/resurrection/history associations, reminders and denied permission, both languages/themes, theme changes without Activity recreation, saved filter/navigation state, custom date/time dialogs, deletion confirmation/cancellation, a 320 dp layout at 2× text with the software keyboard visible, and landscape. Native tests trace the actual Filament stone through front/right/back/left/front, freeze all camera transform/lens components during selection, tap all four rows, check pinch/horizontal/vertical gesture behavior, verify foreground/sheet bounds and exact restoration, cancel transitions, switch tabs, background/resume, and reduced motion.

Actual emulator Tasks screens: [light Russian](screenshots/custom-ui/light-ru.png), [dark Russian](screenshots/custom-ui/dark-ru.png), [light English](screenshots/custom-ui/light-en.png), [dark English](screenshots/custom-ui/dark-en.png). [Narrow screen with keyboard](screenshots/custom-ui/narrow-keyboard.png). These are application captures, not mockups.

## Limits

The references are adapted for real Android insets, device widths, Android font scaling and local Nunito typography. At large font sizes/narrow widths, filters and content scroll to keep actions accessible. Keyboard, Android permission dialogs and system navigation are OS-provided surfaces. The full accessibility experience with TalkBack, physical device GPU/IME differences, tablets and all OEM configurations require broader manual QA. This change adds no new business features or network services.
