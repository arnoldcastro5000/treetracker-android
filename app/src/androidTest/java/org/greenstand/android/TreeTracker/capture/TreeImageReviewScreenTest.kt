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
package org.greenstand.android.TreeTracker.capture

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.greenstand.android.TreeTracker.support.HermeticScreen
import org.greenstand.android.TreeTracker.view.AutomationTags
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumentation test for the tree-capture review flow (issue #1243, flow 2 "Tree capture").
 *
 * Hermetic: it renders the stateless [TreeImageReview] composable with a fake
 * [TreeImageReviewState] and a recording action handler, then asserts the review controls
 * dispatch the correct [TreeImageReviewAction]. The actual camera capture is the `.local`
 * bypass and is exercised end to end by the Appium route2 E2E; the in-process test covers
 * the review-and-proceed UI, which is the screen a captured tree image lands on.
 */
@RunWith(AndroidJUnit4::class)
class TreeImageReviewScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val actions = mutableListOf<TreeImageReviewAction>()

    @Test
    fun approving_the_review_checks_forward_navigation() {
        composeRule.setContent {
            HermeticScreen {
                TreeImageReview(
                    state = TreeImageReviewState(treeImagePath = "/fake/tree.jpg"),
                    onHandleAction = { actions.add(it) },
                )
            }
        }

        composeRule.onNodeWithTag(AutomationTags.APPROVE).assertIsDisplayed().performClick()

        assertTrue(
            "approving the tree-image review must dispatch CheckIfCanNavigateForward",
            actions.any { it is TreeImageReviewAction.CheckIfCanNavigateForward },
        )
    }

    @Test
    fun declining_the_review_navigates_back() {
        composeRule.setContent {
            HermeticScreen {
                TreeImageReview(
                    state = TreeImageReviewState(treeImagePath = "/fake/tree.jpg"),
                    onHandleAction = { actions.add(it) },
                )
            }
        }

        composeRule.onNodeWithTag(AutomationTags.DECLINE).assertIsDisplayed().performClick()

        assertTrue(
            "declining the tree-image review must dispatch NavigateBack",
            actions.any { it is TreeImageReviewAction.NavigateBack },
        )
    }
}
