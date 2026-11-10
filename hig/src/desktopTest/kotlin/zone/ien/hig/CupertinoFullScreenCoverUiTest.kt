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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import zone.ien.hig.theme.CupertinoTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * UI tests for [CupertinoFullScreenCover]. Desktop only: the cover is built on [CupertinoBottomSheetScaffold],
 * whose scaffold reads `LocalUIViewController` on iOS, which `runComposeUiTest` does not provide.
 */
@OptIn(ExperimentalTestApi::class, ExperimentalCupertinoApi::class)
class CupertinoFullScreenCoverUiTest {

    // region UI

    @Test
    fun coverIsShownWhenPresentedAndRemovedAfterDismiss() = runComposeUiTest {
        mainClock.autoAdvance = false
        var presented by mutableStateOf(false)
        var dismissCount = 0
        setContent {
            CupertinoTheme {
                Box(Modifier.size(360.dp, 640.dp)) {
                    CupertinoText(text = "Parent")
                    CupertinoFullScreenCover(
                        isPresented = presented,
                        onDismissRequest = { presented = false },
                        onDismiss = { dismissCount++ },
                    ) {
                        CupertinoButton(onClick = { presented = false }) { CupertinoText(text = "Close") }
                    }
                }
            }
        }
        mainClock.advanceTimeBy(100)
        assertEquals(0, onAllNodesWithTextCount("Close"))

        runOnIdle { presented = true }
        mainClock.advanceTimeBy(1500)
        onNodeWithText("Close").assertIsDisplayed()

        onNodeWithText("Close").performClick()
        mainClock.advanceTimeBy(1500)
        assertEquals(0, onAllNodesWithTextCount("Close"))
        assertEquals(1, dismissCount)
    }

    @Test
    fun contentKeepsStateWhilePresented() = runComposeUiTest {
        mainClock.autoAdvance = false
        setContent {
            CupertinoTheme {
                CupertinoFullScreenCover(isPresented = true, onDismissRequest = {}) {
                    var taps by remember { mutableIntStateOf(0) }
                    CupertinoButton(onClick = { taps++ }) { CupertinoText(text = "Taps $taps") }
                }
            }
        }
        mainClock.advanceTimeBy(1500)
        onNodeWithText("Taps 0").performClick()
        mainClock.advanceTimeBy(100)
        onNodeWithText("Taps 1").performClick()
        mainClock.advanceTimeBy(100)
        onNodeWithText("Taps 2").assertIsDisplayed()
    }

    @Test
    fun itemOverloadSwapsContentAndKeepsLastItemUntilDismissed() = runComposeUiTest {
        mainClock.autoAdvance = false
        var item by mutableStateOf<String?>(null)
        setContent {
            CupertinoTheme {
                CupertinoFullScreenCover(item = item, onDismissRequest = { item = null }) {
                    CupertinoText(text = "Item $it")
                }
            }
        }
        mainClock.advanceTimeBy(100)
        runOnIdle { item = "A" }
        mainClock.advanceTimeBy(1500)
        onNodeWithText("Item A").assertIsDisplayed()

        runOnIdle { item = "B" }
        mainClock.advanceTimeBy(100)
        onNodeWithText("Item B").assertIsDisplayed()
        assertEquals(0, onAllNodesWithTextCount("Item A"))

        runOnIdle { item = null }
        // During the exit animation the last item is still rendered.
        mainClock.advanceTimeBy(16)
        assertEquals(1, onAllNodesWithTextCount("Item B"))
        mainClock.advanceTimeBy(1500)
        assertEquals(0, onAllNodesWithTextCount("Item B"))
    }

    @Test
    fun interactiveSwipeRequestsDismissAndRepresentsWhenCallerKeepsItPresented() = runComposeUiTest {
        mainClock.autoAdvance = false
        var requests = 0
        setContent {
            CupertinoTheme {
                CupertinoFullScreenCover(
                    isPresented = true,
                    onDismissRequest = { requests++ },
                    properties = CupertinoFullScreenCoverProperties(interactiveDismissEnabled = true),
                ) {
                    Box(Modifier.fillMaxSize().testTag("cover")) { CupertinoText(text = "Cover") }
                }
            }
        }
        mainClock.advanceTimeBy(1500)
        onNodeWithText("Cover").assertIsDisplayed()

        onNodeWithTag("cover").performTouchInput { swipeDown() }
        mainClock.advanceTimeBy(3000)
        assertEquals(1, requests)
        onNodeWithText("Cover").assertIsDisplayed()
    }

    @Test
    fun interactiveSwipeDismissesWhenCallerClearsIsPresented() = runComposeUiTest {
        mainClock.autoAdvance = false
        var presented by mutableStateOf(true)
        var dismissed = 0
        setContent {
            CupertinoTheme {
                CupertinoFullScreenCover(
                    isPresented = presented,
                    onDismissRequest = { presented = false },
                    onDismiss = { dismissed++ },
                    properties = CupertinoFullScreenCoverProperties(interactiveDismissEnabled = true),
                ) {
                    Box(Modifier.fillMaxSize().testTag("cover")) { CupertinoText(text = "Cover") }
                }
            }
        }
        mainClock.advanceTimeBy(1500)
        onNodeWithTag("cover").performTouchInput { swipeDown() }
        mainClock.advanceTimeBy(3000)
        assertFalse(presented)
        assertEquals(1, dismissed)
        assertEquals(0, onAllNodesWithTextCount("Cover"))
    }

    @Test
    fun accessibilityDismissActionRoutesToOnDismissRequestOnlyWhenBackDismisses() = runComposeUiTest {
        mainClock.autoAdvance = false
        var requests = 0
        var dismissOnBack by mutableStateOf(true)
        setContent {
            CupertinoTheme {
                CupertinoFullScreenCover(
                    isPresented = true,
                    onDismissRequest = { requests++ },
                    properties = CupertinoFullScreenCoverProperties(
                        dismissOnBackPress = dismissOnBack,
                        paneTitle = "Camera",
                    ),
                ) {
                    CupertinoText(text = "Cover")
                }
            }
        }
        mainClock.advanceTimeBy(1500)
        val pane = onNode(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, "Camera"))
        pane.performSemanticsAction(SemanticsActions.Dismiss)
        assertEquals(1, requests)

        runOnIdle { dismissOnBack = false }
        mainClock.advanceTimeBy(100)
        pane.assert(SemanticsMatcher.keyNotDefined(SemanticsActions.Dismiss))
    }

    // endregion

    private fun androidx.compose.ui.test.ComposeUiTest.onAllNodesWithTextCount(text: String): Int =
        onAllNodes(androidx.compose.ui.test.hasText(text)).fetchSemanticsNodes().size
}
