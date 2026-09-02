/*
 * Copyright 2023 Treetracker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.greenstand.android.TreeTracker.view

/**
 * Stable automation identifiers applied via `Modifier.testTag`.
 *
 * `testTagsAsResourceId = true` is enabled at each Activity root (see
 * `TreeTrackerActivity` and `ImageCaptureActivity`), so every tag below surfaces
 * to UiAutomator2 / Appium as a selectable `resource-id`. These replace the
 * coordinate-driven taps in the `apps/e2e` suite.
 *
 * Keep these strings in sync with `apps/e2e/utils/tags.ts`.
 */
object AutomationTags {
    // Navigation
    const val NAV_FORWARD = "nav-forward"
    const val NAV_BACK = "nav-back"

    // Primary actions
    const val APPROVE = "approve"
    const val DECLINE = "decline"
    const val CAPTURE_SELFIE = "capture-selfie"
    const val CAPTURE_TREE = "capture-tree"

    // Text inputs
    const val INPUT_FIRST_NAME = "input-first-name"
    const val INPUT_LAST_NAME = "input-last-name"
    const val INPUT_PHONE = "input-phone"
    const val INPUT_EMAIL = "input-email"

    // Screen-scoped tags (where a role tag would collide on one screen)
    const val TUTORIAL_DISMISS = "tutorial-dismiss"

    // Misc controls
    const val LANGUAGE_MENU = "language-menu"
    const val INFO = "info"
    const val ADD = "add"
    const val USER_IMAGE = "user-image"

    // Dashboard counters (read by the e2e suite to assert upload progress)
    const val UPLOADED_COUNT = "uploaded-count"
    const val REMAINING_COUNT = "ready-to-upload-count"

    // Dashboard actions
    const val DASHBOARD_SYNC = "dashboard-sync"

    /** Per-language selection row, e.g. "language-option-english". */
    fun languageOption(name: String): String = "language-option-" + name.lowercase().replace(" ", "-")
}
