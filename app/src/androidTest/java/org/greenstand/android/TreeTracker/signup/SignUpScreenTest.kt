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
package org.greenstand.android.TreeTracker.signup

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.greenstand.android.TreeTracker.theme.CustomTheme
import org.greenstand.android.TreeTracker.view.AutomationTags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumentation test for the signup flow (issue #1243, flow 1 "Login/signup").
 *
 * Hermetic: renders the stateless [NameEntryView] / [CredentialEntryView] composables with a fake
 * [SignUpState] and a recording action handler, then asserts the correct [SignupAction] is
 * dispatched. No Koin graph, no network. Selects by [AutomationTags], never by text
 * (best-practices research S2/S7).
 */
@RunWith(AndroidJUnit4::class)
class SignUpScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val actions = mutableListOf<SignupAction>()

    @Test
    fun name_entry_dispatches_first_name_update_and_forward_launches_camera() {
        composeRule.setContent {
            CustomTheme {
                NameEntryView(
                    state = SignUpState(isCredentialView = false),
                    onHandleAction = { actions.add(it) },
                    isFormValid = { true },
                )
            }
        }

        composeRule.onNodeWithTag(AutomationTags.INPUT_FIRST_NAME).assertIsDisplayed().performTextInput("Ada")
        assertEquals("Ada", actions.filterIsInstance<SignupAction.UpdateFirstName>().lastOrNull()?.firstName)

        composeRule.onNodeWithTag(AutomationTags.NAV_FORWARD).assertIsDisplayed().performClick()
        assertTrue(
            "forward from name entry must launch the camera",
            actions.any { it is SignupAction.LaunchCamera },
        )
    }

    @Test
    fun credential_entry_email_dispatches_email_update() {
        composeRule.setContent {
            CustomTheme {
                CredentialEntryView(
                    state = SignUpState(credential = Credential.Email(), showPrivacyDialog = false),
                    onHandleAction = { actions.add(it) },
                )
            }
        }

        composeRule.onNodeWithTag(AutomationTags.INPUT_EMAIL).assertIsDisplayed().performTextInput("ada@example.org")
        assertEquals("ada@example.org", actions.filterIsInstance<SignupAction.UpdateEmail>().lastOrNull()?.email)
    }

    @Test
    fun approving_the_privacy_dialog_dispatches_close() {
        composeRule.setContent {
            CustomTheme {
                CredentialEntryView(
                    state = SignUpState(showPrivacyDialog = true),
                    onHandleAction = { actions.add(it) },
                )
            }
        }

        composeRule.onNodeWithTag(AutomationTags.APPROVE).assertIsDisplayed().performClick()
        assertTrue(
            "approving the privacy dialog must dispatch ClosePrivacyPolicyDialog",
            actions.any { it is SignupAction.ClosePrivacyPolicyDialog },
        )
    }
}
