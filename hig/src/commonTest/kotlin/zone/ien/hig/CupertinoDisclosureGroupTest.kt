/*
 * Copyright (c) 2023-2024. Compose Cupertino project and open source contributors.
 * Copyright (c) 2025. Scott Lanoue.
 * Copyright (c) 2026. IENGROUND of IENLAB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package zone.ien.hig

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.LayoutDirection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class, ExperimentalCupertinoApi::class)
class CupertinoDisclosureGroupTest {
    @Test
    fun chevronPointsForwardWhenCollapsedAndDownWhenExpanded() {
        assertEquals(0f, disclosureChevronRotation(expanded = false, layoutDirection = LayoutDirection.Ltr))
        assertEquals(180f, disclosureChevronRotation(expanded = false, layoutDirection = LayoutDirection.Rtl))
        assertEquals(90f, disclosureChevronRotation(expanded = true, layoutDirection = LayoutDirection.Ltr))
        assertEquals(90f, disclosureChevronRotation(expanded = true, layoutDirection = LayoutDirection.Rtl))
    }

    @Test
    fun tappingHeaderTogglesContent() = runComposeUiTest {
        var expanded by mutableStateOf(false)
        setContent {
            CupertinoDisclosureGroup(
                expanded = expanded,
                onExpandedChange = { expanded = it },
                label = { CupertinoText("Advanced") },
            ) {
                CupertinoText("Child row")
            }
        }

        onNodeWithText("Child row").assertDoesNotExist()
        onNodeWithText("Advanced").assert(stateDescription("Collapsed"))

        onNodeWithText("Advanced").performClick()
        waitForIdle()

        assertTrue(expanded)
        onNodeWithText("Child row").assertExists()
        onNodeWithText("Advanced").assert(stateDescription("Expanded"))

        onNodeWithText("Advanced").performClick()
        waitForIdle()
        assertFalse(expanded)
        onNodeWithText("Child row").assertDoesNotExist()
    }

    @Test
    fun exposesExpandAndCollapseAccessibilityActions() = runComposeUiTest {
        var expanded by mutableStateOf(false)
        setContent {
            CupertinoDisclosureGroup(
                expanded = expanded,
                onExpandedChange = { expanded = it },
                label = { CupertinoText("Advanced") },
            ) { CupertinoText("Child row") }
        }

        onNodeWithText("Advanced").performSemanticsAction(SemanticsActions.Expand)
        waitForIdle()
        assertTrue(expanded)

        onNodeWithText("Advanced").performSemanticsAction(SemanticsActions.Collapse)
        waitForIdle()
        assertFalse(expanded)
    }

    @Test
    fun disabledGroupIgnoresTaps() = runComposeUiTest {
        var expanded by mutableStateOf(false)
        setContent {
            CupertinoDisclosureGroup(
                expanded = expanded,
                onExpandedChange = { expanded = it },
                enabled = false,
                label = { CupertinoText("Advanced") },
            ) { CupertinoText("Child row") }
        }

        onNodeWithText("Advanced").assertIsNotEnabled()
        onNodeWithText("Advanced").assert(SemanticsMatcher.keyNotDefined(SemanticsActions.Expand))
        onNodeWithText("Advanced").performClick()
        waitForIdle()
        assertFalse(expanded)
    }

    private fun stateDescription(value: String) =
        SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, value)
}
