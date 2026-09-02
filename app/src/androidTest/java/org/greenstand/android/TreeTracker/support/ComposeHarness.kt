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
package org.greenstand.android.TreeTracker.support

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.navigation.compose.rememberNavController
import org.greenstand.android.TreeTracker.root.LocalNavHostController
import org.greenstand.android.TreeTracker.theme.CustomTheme

/**
 * Renders a stateless screen composable with the minimum app-wide CompositionLocals it
 * transitively needs, so the per-screen tests stay hermetic (no Koin graph):
 *
 *  - [LocalNavHostController]: the shared top bars ([org.greenstand.android.TreeTracker.view.TopBarTitle]
 *    and [org.greenstand.android.TreeTracker.view.LanguageButton]) read it eagerly during composition,
 *    and its default value throws. A throwaway [rememberNavController] satisfies it with no navigation.
 *  - [LocalInspectionMode] = true: makes `LanguageButton` skip building its Koin-backed
 *    `LanguagePickerViewModel` (the exact guard Compose Previews rely on), so the test needs no
 *    `LocalViewModelFactory`.
 *
 * Wrap the composable under test in [CustomTheme] as the real app does.
 */
@Composable
fun HermeticScreen(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalNavHostController provides rememberNavController(),
        LocalInspectionMode provides true,
    ) {
        CustomTheme {
            content()
        }
    }
}
