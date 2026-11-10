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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCupertinoApi::class, ExperimentalTestApi::class)
class CupertinoPageTabViewTest {

    // region pure logic

    @Test
    fun automaticIndexIsHiddenForSinglePage() {
        val mode = CupertinoPageIndexDisplayMode.Automatic
        assertFalse(isPageIndexVisible(mode, pageCount = 0))
        assertFalse(isPageIndexVisible(mode, pageCount = 1))
        assertTrue(isPageIndexVisible(mode, pageCount = 2))
    }

    @Test
    fun alwaysAndNeverIndexModes() {
        assertTrue(isPageIndexVisible(CupertinoPageIndexDisplayMode.Always, pageCount = 1))
        assertFalse(isPageIndexVisible(CupertinoPageIndexDisplayMode.Always, pageCount = 0))
        assertFalse(isPageIndexVisible(CupertinoPageIndexDisplayMode.Never, pageCount = 5))
    }

    @Test
    fun backgroundStateMachineShowsWhileInteractingAndHidesAfterTimeout() {
        var state = PageIndexBackgroundState()
        assertFalse(state.visible)

        state = state.reduce(PageIndexBackgroundEvent.InteractionStarted)
        assertTrue(state.visible)
        assertTrue(state.isAwaitingHide.not())

        state = state.reduce(PageIndexBackgroundEvent.InteractionEnded)
        assertTrue(state.visible)
        assertTrue(state.isAwaitingHide)

        state = state.reduce(PageIndexBackgroundEvent.HideTimeoutElapsed)
        assertFalse(state.visible)
        assertFalse(state.isAwaitingHide)
    }

    @Test
    fun backgroundTimeoutIsIgnoredWhileInteractionIsActive() {
        var state = PageIndexBackgroundState()
            .reduce(PageIndexBackgroundEvent.InteractionStarted)
            .reduce(PageIndexBackgroundEvent.HideTimeoutElapsed)
        assertTrue(state.visible)

        // Ending more interactions than started never goes negative.
        state = state
            .reduce(PageIndexBackgroundEvent.InteractionEnded)
            .reduce(PageIndexBackgroundEvent.InteractionEnded)
        assertEquals(0, state.activeInteractions)
    }

    @Test
    fun everyEventRestartsTheHideTimer() {
        val waiting = PageIndexBackgroundState()
            .reduce(PageIndexBackgroundEvent.InteractionStarted)
            .reduce(PageIndexBackgroundEvent.InteractionEnded)
        val afterTap = waiting
            .reduce(PageIndexBackgroundEvent.InteractionStarted)
            .reduce(PageIndexBackgroundEvent.InteractionEnded)

        // Same visible/idle shape, but a different value so a keyed timer restarts.
        assertEquals(waiting.visible, afterTap.visible)
        assertEquals(waiting.activeInteractions, afterTap.activeInteractions)
        assertTrue(waiting != afterTap)
    }

    @Test
    fun backgroundDisplayModeMapping() {
        val idle = PageIndexBackgroundState()
        val active = idle.reduce(PageIndexBackgroundEvent.InteractionStarted)
        CupertinoPageIndexBackgroundDisplayMode.entries.forEach { mode ->
            val expectedIdle = mode == CupertinoPageIndexBackgroundDisplayMode.Always
            val expectedActive = mode != CupertinoPageIndexBackgroundDisplayMode.Never
            assertEquals(expectedIdle, isPageIndexBackgroundVisible(mode, idle), "$mode idle")
            assertEquals(expectedActive, isPageIndexBackgroundVisible(mode, active), "$mode active")
        }
    }

    @Test
    fun scrubbingMapsDragDistanceToPagesAndMirrorsInRtl() {
        val step = 10f
        assertEquals(2, scrubbedPage(2, dragDistance = 9f, stepPx = step, pageCount = 5, LayoutDirection.Ltr))
        assertEquals(3, scrubbedPage(2, dragDistance = 10f, stepPx = step, pageCount = 5, LayoutDirection.Ltr))
        assertEquals(0, scrubbedPage(2, dragDistance = -25f, stepPx = step, pageCount = 5, LayoutDirection.Ltr))
        assertEquals(4, scrubbedPage(2, dragDistance = 500f, stepPx = step, pageCount = 5, LayoutDirection.Ltr))
        assertEquals(1, scrubbedPage(2, dragDistance = 10f, stepPx = step, pageCount = 5, LayoutDirection.Rtl))
        assertEquals(4, scrubbedPage(2, dragDistance = -20f, stepPx = step, pageCount = 5, LayoutDirection.Rtl))
    }

    // endregion

    // region compose UI

    private fun pageIndex(description: String) =
        SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, description)

    private fun backgroundVisible(visible: Boolean) =
        SemanticsMatcher.expectValue(PageIndexBackgroundVisibleKey, visible)

    @Test
    fun indexExposesPositionAndAdjustableActions() = runComposeUiTest {
        lateinit var state: PagerState
        setContent {
            state = rememberPagerState { 5 }
            CupertinoPageTabView(state = state, modifier = Modifier.size(300.dp, 400.dp)) { page ->
                CupertinoText("Content $page")
            }
        }

        onNode(pageIndex("Page 1 of 5"))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.CustomActions))
        onNodeWithText("Content 0").assertExists()

        val next = onNode(pageIndex("Page 1 of 5")).fetchSemanticsNode()
            .config[SemanticsActions.CustomActions].single { it.label == "Next page" }
        runOnIdle { next.action() }
        waitForIdle()
        assertEquals(1, state.currentPage)
        onNode(pageIndex("Page 2 of 5")).assertExists()

        onNode(pageIndex("Page 2 of 5")).performSemanticsAction(SemanticsActions.SetProgress) { it(4f) }
        waitForIdle()
        assertEquals(4, state.currentPage)
        onNodeWithText("Content 4").assertExists()
    }

    @Test
    fun singlePageHidesIndexInAutomaticModeButNotInAlways() = runComposeUiTest {
        val modeState = mutableStateOf(CupertinoPageIndexDisplayMode.Automatic)
        setContent {
            CupertinoPageTabView(
                pageCount = 1,
                indexDisplayMode = modeState.value,
                modifier = Modifier.size(300.dp, 400.dp),
            ) { CupertinoText("Only") }
        }
        onNode(pageIndex("Page 1 of 1")).assertDoesNotExist()
        modeState.value = CupertinoPageIndexDisplayMode.Always
        waitForIdle()
        onNode(pageIndex("Page 1 of 1")).assertExists()
    }

    @Test
    fun interactiveBackgroundAutoHidesAfterDelay() = runComposeUiTest {
        mainClock.autoAdvance = false
        setContent {
            CupertinoPageTabView(
                pageCount = 3,
                backgroundDisplayMode = CupertinoPageIndexBackgroundDisplayMode.Interactive,
                modifier = Modifier.size(300.dp, 400.dp),
            ) { CupertinoText("Content $it") }
        }
        mainClock.advanceTimeBy(100)
        onNode(pageIndex("Page 1 of 3")).assert(backgroundVisible(false))

        onNode(pageIndex("Page 1 of 3")).performTouchInput { down(center) }
        mainClock.advanceTimeBy(5_000)
        // Still pressed: the background stays regardless of elapsed time.
        onNode(pageIndex("Page 1 of 3")).assert(backgroundVisible(true))

        onNode(pageIndex("Page 1 of 3")).performTouchInput { up() }
        mainClock.advanceTimeBy(CupertinoPageTabViewDefaults.BackgroundHideDelayMillis / 2)
        onNode(SemanticsMatcher.keyIsDefined(PageIndexBackgroundVisibleKey)).assert(backgroundVisible(true))

        mainClock.advanceTimeBy(CupertinoPageTabViewDefaults.BackgroundHideDelayMillis)
        onNode(SemanticsMatcher.keyIsDefined(PageIndexBackgroundVisibleKey)).assert(backgroundVisible(false))
    }

    @Test
    fun disabledIndexHasNoAdjustActions() = runComposeUiTest {
        setContent {
            CupertinoPageTabView(
                pageCount = 3,
                userScrollEnabled = false,
                modifier = Modifier.size(300.dp, 400.dp),
            ) { CupertinoText("Content $it") }
        }
        onNode(pageIndex("Page 1 of 3"))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Disabled))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.SetProgress))
    }

    @Test
    fun alwaysBackgroundIsVisibleWithoutInteraction() = runComposeUiTest {
        setContent {
            CupertinoPageTabView(
                pageCount = 3,
                backgroundDisplayMode = CupertinoPageIndexBackgroundDisplayMode.Always,
                modifier = Modifier.size(300.dp, 400.dp),
            ) { CupertinoText("Content $it") }
        }
        onNode(pageIndex("Page 1 of 3")).assert(backgroundVisible(true))
    }

    @Test
    fun rtlPagerShowsNextPageOnTheLeft() = runComposeUiTest {
        lateinit var state: PagerState
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Box(Modifier.size(300.dp, 400.dp)) {
                    state = rememberPagerState { 3 }
                    CupertinoPageTabView(state = state, modifier = Modifier.testTag("tabs")) {
                        CupertinoText("Content $it")
                    }
                }
            }
        }
        // In RTL, swiping from left to right advances to the next page.
        onNode(pageIndex("Page 1 of 3")).assertExists()
        onNodeWithTag("tabs").performTouchInput { swipeRight() }
        waitForIdle()
        assertEquals(1, state.currentPage)
    }

    // endregion
}
