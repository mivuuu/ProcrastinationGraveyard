# Project instructions

Build a native, offline Android app using Kotlin, Compose with the custom Cemetery Foundation design system, SceneView/Filament, Hilt, Room, DataStore, Coroutines/Flow, WorkManager, and Gradle Kotlin DSL. Keep domain, persistence, UI, and rendering logic separate. No analytics, accounts, network permissions, or cloud task storage. MIT application source; document dependency and asset licenses.

Read this file at the beginning of every development task. All user-facing text belongs in matching English and Russian resources. Preserve user data, use safe Room migrations, and confirm permanent deletion. Never replace the real 3D cemetery with a placeholder.

After EVERY user request that modifies the app:
1. Save all changes and run relevant automated tests.
2. Run `./gradlew :app:assembleDebug` (Windows: `gradlew.bat :app:assembleDebug`).
3. Inspect output, fix errors, and repeat until successful or a genuine environment blocker is established.
4. Verify `app/build/outputs/apk/debug/app-debug.apk` exists and is nonempty.
5. Report changes, test/build results, exact APK path, and unverified or incomplete criteria. Never claim successful build or runtime validation without evidence.

Build after each implementation phase; do not defer compilation to the end. Inspect actual installed third-party APIs. When a device/emulator is available, install and launch and inspect startup logs. Keep README implementation status and limitations accurate.

Every future development prompt must end with the complete debug APK build, including small UI changes, translations, fixes, and refactoring. Preserve the four-row horizontal diorama layout and fixed-angle swipe/pinch camera. Grave selection must animate the 3D object through one 360-degree local-Y turn; never move or rotate the camera during selection. Preserve the bright blue sky, triangular forest and seamless cartoon grass art direction. Database layout migrations must retain grave IDs, task associations, history and epitaphs.

All visible application UI uses the custom CompositionLocal theme, locally bundled Nunito font, Foundation controls and original Canvas icons/illustrations. Do not introduce visible Material components. Preserve the cream/forest light palette, emerald dark palette and lime accents. Keep matching English and Russian resources, accessible semantics, font scaling, touch targets and system insets. UI-only requests must not change the native 3D rendering implementation or business logic.

## MANDATORY WORKFLOW AFTER EVERY FUTURE USER PROMPT

Repository: `https://github.com/mivuuu/ProcrastinationGraveyard` (`origin`).

- Complete the requested changes.
- Run relevant tests.
- Build the debug APK using `./gradlew :app:assembleDebug`.
- Verify that `app/build/outputs/apk/debug/app-debug.apk` exists and is nonempty.
- Review `git status` and `git diff`, including the staged diff and newly added files.
- Commit all intended project changes with a clear, descriptive commit message.
- Push the commit to `origin` on the current working branch.
- Verify that the push succeeded by checking the remote branch against the local commit.
- Report the test/build status, exact APK path, commit hash, branch name and push status.

Never commit passwords, API tokens, signing keys or other secrets. Authenticate through GitHub CLI or a secure Git credential helper; never put credentials in source files or remote URLs. Review intended files for secrets before staging and committing. Keep generated APKs/build outputs and local configuration excluded by `.gitignore`.

Never use `git push --force` or any other force-push option. Preserve local and remote history. If the remote branch advances, fetch and integrate safely, resolve conflicts without discarding user changes, and repeat relevant verification before pushing.

If authentication or permissions prevent pushing, explain the blocker and report the actual commit/build state instead of claiming success.

Every completed development prompt must end with the debug APK build, Git commit and GitHub push, including small UI changes, translations, fixes and refactoring.
