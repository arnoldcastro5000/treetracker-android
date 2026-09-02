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
package org.greenstand.android.TreeTracker.dashboard

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.greenstand.android.TreeTracker.theme.CustomTheme
import org.greenstand.android.TreeTracker.view.AutomationTags
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumentation test for the Dashboard flow (issue #1243, flow 4 "Dashboard" + flow 3 "Sync").
 *
 * Hermetic: it renders the stateless [Dashboard] composable with a fake [DashboardState] and a
 * recording sync callback. No Koin graph, no WorkManager, no network, no emulator camera. This is
 * the in-app unit of the sync flow: the actual upload to LocalStack/S3 is covered cross-process by
 * the Appium route2 E2E, not by an in-process instrumentation test (best-practices research S3/S4).
 */
@RunWith(AndroidJUnit4::class)
class DashboardScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun dashboard_displays_synced_and_remaining_counts() {
        composeRule.setContent {
            CustomTheme {
                Dashboard(state = DashboardState(treesSynced = 5, treesRemainingToSync = 3, totalTreesToSync = 8))
            }
        }

        composeRule.onNodeWithTag(AutomationTags.UPLOADED_COUNT).assertIsDisplayed().assertTextEquals("5")
        composeRule.onNodeWithTag(AutomationTags.REMAINING_COUNT).assertIsDisplayed().assertTextEquals("3")
    }

    @Test
    fun tapping_sync_triggers_the_sync_callback() {
        var syncClicked = false
        composeRule.setContent {
            CustomTheme {
                Dashboard(
                    state = DashboardState(treesSynced = 0, treesRemainingToSync = 2, totalTreesToSync = 2),
                    onSyncClicked = { syncClicked = true },
                )
            }
        }

        composeRule.onNodeWithTag(AutomationTags.DASHBOARD_SYNC).assertIsDisplayed().performClick()

        assertTrue("tapping the dashboard sync control must trigger onSyncClicked", syncClicked)
    }
}
