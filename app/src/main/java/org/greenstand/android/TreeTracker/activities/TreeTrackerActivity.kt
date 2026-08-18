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
package org.greenstand.android.TreeTracker.activities

import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import org.greenstand.android.TreeTracker.models.TreeTrackerViewModelFactory
import org.greenstand.android.TreeTracker.root.Root
import org.greenstand.android.TreeTracker.theme.CustomTheme
import org.greenstand.android.TreeTracker.utilities.GpsUtils
import org.greenstand.android.TreeTracker.view.NoGPSDeviceDialog
import org.koin.android.ext.android.inject

class TreeTrackerActivity : AppCompatActivity() {
    private val viewModelFactory: TreeTrackerViewModelFactory by inject()
    private val gpsUtils: GpsUtils by inject()

    @OptIn(ExperimentalComposeApi::class, ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle =
                SystemBarStyle.dark(
                    scrim = android.graphics.Color.argb(128, 0, 0, 0), // 50% black scrim
                ),
            navigationBarStyle =
                SystemBarStyle.dark(
                    scrim = android.graphics.Color.argb(128, 0, 0, 0),
                ),
        )

        setContent {
            CustomTheme {
                // Surface every Modifier.testTag in the tree to UiAutomator2 as a
                // resource-id, so the e2e suite selects controls by stable id
                // instead of screen coordinates.
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .semantics { testTagsAsResourceId = true },
                ) {
                    if (gpsUtils.hasGPSDevice()) {
                        Root(viewModelFactory)
                    } else {
                        NoGPSDeviceDialog(onPositiveClick = { finishAndRemoveTask() })
                    }
                }
            }
        }
    }
}