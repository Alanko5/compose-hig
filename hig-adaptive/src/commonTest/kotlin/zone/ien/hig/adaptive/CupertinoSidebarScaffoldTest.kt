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

package zone.ien.hig.adaptive

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigationevent.DirectNavigationEventInput
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import zone.ien.hig.CupertinoText
import zone.ien.hig.ExperimentalCupertinoApi
import zone.ien.hig.section.SectionStyle
import zone.ien.hig.section.section
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCupertinoApi::class, ExperimentalTestApi::class)
class CupertinoSidebarScaffoldTest {

    @Composable
    private fun Mail(
        state: CupertinoSidebarState,
        width: Int,
        windowInsets: WindowInsets = WindowInsets(0),
        sidebarTopBar: (@Composable () -> Unit)? = null,
        onSelected: (Any) -> Unit = {},
    ) {
        var selected by androidx.compose.runtime.remember { mutableStateOf<Any?>("inbox") }
        CupertinoSidebarScaffold(
            modifier = Modifier.size(width.dp, 600.dp),
            state = state,
            contentWindowInsets = windowInsets,
            sidebarTopBar = sidebarTopBar,
            selectedKey = selected,
            onSelect = { selected = it; onSelected(it) },
            sidebar = {
                section(style = SectionStyle.Sidebar) {
                    destination(key = "inbox") { CupertinoText("Inbox") }
                    destination(key = "sent") { CupertinoText("Sent") }
                }
            },
            detail = {
                CupertinoText("Detail of $selected")
                CupertinoSidebarToggleButton(state)
            },
        )
    }

    @Test
    fun expandedShowsBothColumnsAndBindsSelection() = runComposeUiTest {
        val state = CupertinoSidebarState()
        val picked = mutableListOf<Any>()
        setContent { Mail(state, width = 900, onSelected = { picked += it }) }

        onNodeWithText("Inbox").assertIsDisplayed().assertIsSelected()
        onNodeWithText("Sent").assertIsNotSelected()
        onNodeWithText("Detail of inbox").assertIsDisplayed()

        onNodeWithText("Sent").performClick()
        waitForIdle()
        assertEquals(listOf<Any>("sent"), picked)
        onNodeWithText("Sent").assertIsSelected()
        onNodeWithText("Detail of sent").assertIsDisplayed()
        onNodeWithText("Inbox").assertIsDisplayed()
        assertEquals(CupertinoSidebarWidthClass.Expanded, state.widthClass)
    }

    @Test
    fun toggleHidesAndShowsSidebarOnExpanded() = runComposeUiTest {
        val state = CupertinoSidebarState()
        setContent { Mail(state, width = 900) }

        onNodeWithContentDescription("Hide sidebar").performClick()
        waitForIdle()
        assertEquals(CupertinoSidebarVisibility.Hidden, state.visibility)
        onNodeWithText("Inbox").assertIsNotDisplayed()
        onNodeWithText("Detail of inbox").assertIsDisplayed()

        onNodeWithContentDescription("Show sidebar").performClick()
        waitForIdle()
        onNodeWithText("Inbox").assertIsDisplayed()
    }

    @Test
    fun compactPushesDetailAndBackPopsToSidebar() = runComposeUiTest {
        val state = CupertinoSidebarState()
        setContent { Mail(state, width = 390) }

        onNodeWithText("Inbox").assertIsDisplayed()
        onNodeWithText("Detail of inbox").assertIsNotDisplayed()
        onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ContentDescription).and(
            SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf("Hide sidebar")),
        )).assertDoesNotExist()

        onNodeWithText("Sent").performClick()
        waitForIdle()
        assertEquals(CupertinoSidebarColumn.Detail, state.compactColumn)
        onNodeWithText("Detail of sent").assertIsDisplayed()
        onNodeWithText("Sent").assertIsNotDisplayed()

        runOnIdle { assertTrue(state.navigateBack()) }
        waitForIdle()
        onNodeWithText("Sent").assertIsDisplayed()
        onNodeWithText("Detail of sent").assertIsNotDisplayed()
    }

    @Test
    fun sidebarIsOnTheRightInRtl() = runComposeUiTest {
        val state = CupertinoSidebarState()
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Mail(state, width = 900)
            }
        }
        val sidebarLeft = onNodeWithText("Inbox").getUnclippedBoundsInRoot().left
        val detailLeft = onNodeWithText("Detail of inbox").getUnclippedBoundsInRoot().left
        assertTrue(sidebarLeft > detailLeft, "sidebar $sidebarLeft should be right of detail $detailLeft")
    }

    @Test
    fun listIsPaddedByTopInsetOnlyWithoutTopBar() = runComposeUiTest {
        val insets = WindowInsets(top = 40.dp)
        var withTopBar by mutableStateOf(false)
        setContent {
            Mail(
                CupertinoSidebarState(),
                width = 390,
                windowInsets = insets,
                sidebarTopBar = if (withTopBar) ({ androidx.compose.foundation.layout.Spacer(Modifier.height(10.dp)) }) else null,
            )
        }
        val withoutBarTop = onNodeWithText("Inbox").getUnclippedBoundsInRoot().top
        withTopBar = true
        waitForIdle()
        val withBarTop = onNodeWithText("Inbox").getUnclippedBoundsInRoot().top
        // Without a top bar the 40 dp inset pads the list; with a 10 dp bar the bar owns the inset instead.
        assertEquals(30f, (withoutBarTop - withBarTop).value, 1f)
    }

    @Test
    fun systemBackPopsDetailOnlyOnCompactWithDetail() = runComposeUiTest {
        var fallbackCalls = 0
        val dispatcher = NavigationEventDispatcher(onBackCompletedFallback = { fallbackCalls++ })
        val input = DirectNavigationEventInput()
        dispatcher.addInput(input)
        val owner = object : NavigationEventDispatcherOwner {
            override val navigationEventDispatcher = dispatcher
        }
        val state = CupertinoSidebarState()
        var width by mutableStateOf(390)
        setContent {
            CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides owner) {
                Mail(state, width = width)
            }
        }

        // Compact, sidebar shown: nothing to pop, the event falls through.
        runOnIdle { input.backCompleted() }
        waitForIdle()
        assertEquals(1, fallbackCalls)
        assertEquals(CupertinoSidebarColumn.Sidebar, state.compactColumn)

        // Compact, detail pushed: back pops it.
        onNodeWithText("Sent").performClick()
        waitForIdle()
        assertEquals(CupertinoSidebarColumn.Detail, state.compactColumn)
        runOnIdle { input.backCompleted() }
        waitForIdle()
        assertEquals(1, fallbackCalls)
        assertEquals(CupertinoSidebarColumn.Sidebar, state.compactColumn)

        // Expanded: back is left to the enclosing navigation even with compactColumn = Detail.
        runOnIdle { state.showDetail() }
        width = 900
        waitForIdle()
        runOnIdle { input.backCompleted() }
        waitForIdle()
        assertEquals(2, fallbackCalls)
        assertEquals(CupertinoSidebarColumn.Detail, state.compactColumn)
    }

    @Test
    fun toggleIsAbsentOnCompactFromFirstFrame() = runComposeUiTest {
        mainClock.autoAdvance = false
        setContent { Mail(CupertinoSidebarState(), width = 390) }
        onNodeWithContentDescription("Hide sidebar").assertDoesNotExist()
        onNodeWithContentDescription("Show sidebar").assertDoesNotExist()
    }
}
