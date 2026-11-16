# Verified test cases — 0.1.2

59 JVM tests passed. All 21 Android cases passed across the full run and targeted rerun.

The first full run passed 18 cases and exposed three outdated synchronization assumptions in UI tests. The updated tests retain the original assertions and await actual dialog state / window frame submission. All three passed in the targeted rerun; its Gradle command completed successfully. No application behavior was changed between these device runs (only a code comment and test synchronization). Raw reports for both runs are retained.

| Device test | Passing run |
| --- | --- |
| `CustomUiTest.customEditorValidationKeyboardAndPersistence` | targeted rerun |
| `CustomUiTest.narrowScreenLargeTextAndKeyboardRemainUsable` | full run |
| `CustomUiTest.referenceThemesAndLiveThemeState` | full run |
| `DeniedReminderTest.deniedPermissionLeavesTasksUsable` | full run |
| `MigrationTest.horizontalLayoutMigrationPreservesAllUserRecords` | full run |
| `MigrationTest.migrationKeepsRecordsAndAdvancesAllocation` | full run |
| `MigrationTest.upgradeFromFirstVersionKeepsAssociations` | full run |
| `ReminderTest.reminderDeliveredAndDisabledPreferenceIsSafe` | full run |
| `RepositoryTest.createEditCompleteDelete` | full run |
| `RepositoryTest.deletionNeverReusesHistoricalPlot` | full run |
| `RepositoryTest.overdueDedupRescheduleResurrect` | full run |
| `RepositoryTest.persistedDatabaseSurvivesReopen` | full run |
| `RepositoryTest.repeatedMissCreatesOneGravePerDeadline` | full run |
| `UiSmokeTest.createCompleteAndDeleteThroughUi` | targeted rerun |
| `UiSmokeTest.earlyCancellationAndTabChangesRestoreWorld` | full run |
| `UiSmokeTest.largeTextAndLandscapeKeepPresentationVisible` | full run |
| `UiSmokeTest.realScreensAndPersistedSettings` | full run |
| `UiSmokeTest.recordedSpinAndReducedMotionKeepDataSafe` | full run |
| `VisualFixTest.firstFrameIsRendered` | full run |
| `VisualFixTest.flowersUseOneRoundHaloAndCloseIsFixed` | full run |
| `VisualFixTest.repeatedTabsKeepOneEngineAndCameraRelativeClouds` | targeted rerun |
