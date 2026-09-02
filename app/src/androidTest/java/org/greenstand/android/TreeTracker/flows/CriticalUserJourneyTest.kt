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
package org.greenstand.android.TreeTracker.flows

import android.Manifest
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import org.greenstand.android.TreeTracker.activities.TreeTrackerActivity
import org.greenstand.android.TreeTracker.view.AutomationTags
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Full critical-path journey (issue #1243, flows 1-4 end to end): signup -> selfie -> tree capture
 * -> dashboard -> sync, driven through the REAL single Activity and its Koin-wired NavHost.
 *
 * IGNORED for now, on purpose. Unlike the per-screen tests ([org.greenstand.android.TreeTracker.dashboard.DashboardScreenTest],
 * [org.greenstand.android.TreeTracker.signup.SignUpScreenTest]), this journey boots the whole app,
 * so before it can run green it needs, ON A REAL DEVICE / EMULATOR:
 *   1. The `.local` build variant (this module sets `testBuildType "local"`), so the committed
 *      camera bypass in `Camera.kt` (the `.local` build branch) feeds the bundled test assets
 *      instead of the emulator camera.
 *   2. A hermetic Koin graph: a test Application + custom `testInstrumentationRunner` that starts
 *      Koin with fake network / a fake `LocationDataCapturer` (the Splash screen gates on a GPS fix),
 *      or a `loadKoinModules(override = true)` overlay. See ticket 37 + best-practices research S4.
 *   3. A clean install state per run so a prior signup/user does not leak.
 * Enable by removing @Ignore once the harness in item 2 lands. This test is committed now as the
 * executable specification of the journey; the same path is already covered cross-process by the
 * Appium route2 E2E. Tracked by MONO/.scratch/volunteer-e2e-ci/issues/40 + 41.
 */
@RunWith(AndroidJUnit4::class)
class CriticalUserJourneyTest {

    @get:Rule(order = 0)
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.CAMERA,
        Manifest.permission.POST_NOTIFICATIONS,
    )

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<TreeTrackerActivity>()

    /** Waits for a tagged node to appear without sleeping (best-practices research S2). */
    private fun awaitTag(tag: String) {
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    @Ignore("Enable on-device: needs the .local camera bypass + a Koin test graph (fake network/GPS). See KDoc.")
    fun signup_capture_and_sync_end_to_end() {
        // 1. Splash resolves the start destination; the language screen appears.
        awaitTag(AutomationTags.NAV_FORWARD)

        // 2. Language selection -> forward.
        composeRule.onNodeWithTag(AutomationTags.languageOption("English")).performClick()
        composeRule.onNodeWithTag(AutomationTags.NAV_FORWARD).performClick()

        // 3. Credential entry: accept the privacy dialog, then advance (default credential = phone).
        awaitTag(AutomationTags.APPROVE)
        composeRule.onNodeWithTag(AutomationTags.APPROVE).performClick()
        composeRule.onNodeWithTag(AutomationTags.INPUT_PHONE).performClick()
        composeRule.onNodeWithTag(AutomationTags.NAV_FORWARD).performClick()

        // 4. Name entry -> forward launches the camera.
        awaitTag(AutomationTags.INPUT_FIRST_NAME)
        composeRule.onNodeWithTag(AutomationTags.NAV_FORWARD).performClick()

        // 5. Selfie capture (.local bypass feeds testselfieimage.png) -> approve the review.
        awaitTag(AutomationTags.CAPTURE_SELFIE)
        composeRule.onNodeWithTag(AutomationTags.CAPTURE_SELFIE).performClick()
        awaitTag(AutomationTags.APPROVE)
        composeRule.onNodeWithTag(AutomationTags.APPROVE).performClick()

        // 6. Dashboard reached. Tree capture (.local bypass feeds testtreeimage.jpg) -> approve.
        awaitTag(AutomationTags.CAPTURE_TREE)
        composeRule.onNodeWithTag(AutomationTags.CAPTURE_TREE).performClick()
        awaitTag(AutomationTags.APPROVE)
        composeRule.onNodeWithTag(AutomationTags.APPROVE).performClick()

        // 7. Back on the dashboard: trigger sync and assert the remaining counter is present.
        awaitTag(AutomationTags.DASHBOARD_SYNC)
        composeRule.onNodeWithTag(AutomationTags.DASHBOARD_SYNC).performClick()
        awaitTag(AutomationTags.REMAINING_COUNT)
    }
}
