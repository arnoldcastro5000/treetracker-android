# task_15 - dashboard upload-counter testTags for e2e assertions

## Why

The Route 2 end-to-end suite (`apps/e2e` in the monorepo) asserts the upload result on the dashboard:
the ready-to-upload count reaches 0 and the uploaded count reaches 1. The steps located those counters
by content-description ("Trees ready to upload" / "Trees uploaded"), but those descriptions do not
exist anywhere in the app - the counters are plain `Text(state.treesRemainingToSync)` and
`Text(state.treesSynced)` nodes with no automation affordance. So the assertions could never resolve.
UiAutomator also does not receive Compose content-descriptions reliably on the headless emulator; the
app already standardises on `testTagsAsResourceId` (see `AutomationTags.kt`), which the suite reads as
`resource-id`.

## Change

`view/AutomationTags.kt`: add `UPLOADED_COUNT = "uploaded-count"` and
`REMAINING_COUNT = "ready-to-upload-count"`.

`dashboard/DashboardScreen.kt`: apply `Modifier.testTag(AutomationTags.UPLOADED_COUNT)` to the
`treesSynced` counter `Text` and `Modifier.testTag(AutomationTags.REMAINING_COUNT)` to the
`treesRemainingToSync` counter `Text`. With `testTagsAsResourceId` enabled at the Activity root, the
suite reads each counter's rendered number via `byTag(...).getText()`.

## Notes

Test affordance only; no behaviour or layout change. The `apps/e2e` step definitions and `utils/tags.ts`
are updated in the monorepo to read these ids.
