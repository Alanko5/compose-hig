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

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performCustomAccessibilityActionWithLabel
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCupertinoApi::class, ExperimentalTestApi::class, InternalCupertinoApi::class)
class CupertinoPullToRefreshTest {
    private class RecordingHaptics : HapticFeedback {
        val events = mutableListOf<HapticFeedbackType>()

        override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
            events += hapticFeedbackType
        }
    }

    @Test
    fun pullingPastThresholdRefreshesWithSingleHaptic() = runComposeUiTest {
        var refreshes by mutableIntStateOf(0)
        val haptics = RecordingHaptics()

        setContent {
            CompositionLocalProvider(LocalHapticFeedback provides haptics) {
                CupertinoPullToRefreshBox(
                    isRefreshing = false,
                    onRefresh = { refreshes++ },
                    modifier = Modifier.size(300.dp, 500.dp).testTag("ptr"),
                ) {
                    LazyColumn(Modifier.fillMaxWidth().height(500.dp)) {
                        items(50) { CupertinoText("Row $it") }
                    }
                }
            }
        }

        onNodeWithTag("ptr").performTouchInput {
            swipeDown(startY = top + 5f, endY = bottom - 5f, durationMillis = 600)
        }
        waitForIdle()

        assertEquals(1, refreshes)
        assertEquals(1, haptics.events.count { it == CupertinoHapticFeedback.ImpactMedium })
    }

    @Test
    fun shortPullDoesNotRefresh() = runComposeUiTest {
        var refreshes by mutableIntStateOf(0)
        val haptics = RecordingHaptics()

        setContent {
            CompositionLocalProvider(LocalHapticFeedback provides haptics) {
                CupertinoPullToRefreshBox(
                    isRefreshing = false,
                    onRefresh = { refreshes++ },
                    modifier = Modifier.size(300.dp, 500.dp).testTag("ptr"),
                ) {
                    LazyColumn(Modifier.fillMaxWidth().height(500.dp)) {
                        items(50) { CupertinoText("Row $it") }
                    }
                }
            }
        }

        onNodeWithTag("ptr").performTouchInput {
            swipeDown(startY = top + 5f, endY = top + 60f, durationMillis = 400)
        }
        waitForIdle()

        assertEquals(0, refreshes)
        assertTrue(haptics.events.isEmpty())
    }

    @Test
    fun refreshCustomActionRefreshesAndRefreshingStateIsAnnounced() = runComposeUiTest {
        var isRefreshing by mutableStateOf(false)
        var refreshes by mutableIntStateOf(0)

        setContent {
            CupertinoPullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    refreshes++
                    isRefreshing = true
                },
                modifier = Modifier.size(300.dp, 500.dp).testTag("ptr"),
            ) {
                LazyColumn(Modifier.fillMaxWidth().height(500.dp)) {
                    items(5) { CupertinoText("Row $it") }
                }
            }
        }

        // The refresh control is its own accessibility element (it is what a screen reader focuses), not the container.
        onNodeWithContentDescription("Refresh").performCustomAccessibilityActionWithLabel("Refresh")
        waitForIdle()

        assertEquals(1, refreshes)
        onNodeWithContentDescription("Refreshing")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.CustomActions))
    }

    @Test
    fun refreshElementIsAFocusableLeafAheadOfTheContent() = runComposeUiTest {
        var refreshes by mutableIntStateOf(0)
        setContent {
            CupertinoPullToRefreshBox(
                isRefreshing = false,
                onRefresh = { refreshes++ },
                modifier = Modifier.size(300.dp, 500.dp).testTag("ptr"),
            ) {
                LazyColumn(Modifier.fillMaxWidth().height(500.dp)) {
                    items(5) { CupertinoText("Row $it") }
                }
            }
        }

        val element = onNodeWithContentDescription("Refresh")
        // A speaking leaf (content description + click) is what TalkBack/VoiceOver can focus; the container is not.
        element
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.OnClick))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.CustomActions))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.TraversalIndex, -1f))
        onNodeWithTag("ptr").assert(SemanticsMatcher.keyNotDefined(SemanticsActions.CustomActions))
        // Non-empty bounds at the top of the box: empty nodes are dropped by the platform accessibility bridges.
        val bounds = element.getBoundsInRoot()
        val box = onNodeWithTag("ptr").getBoundsInRoot()
        assertTrue(bounds.right > bounds.left && bounds.bottom > bounds.top, "bounds=$bounds")
        assertEquals(box.top, bounds.top)
        // The element is placed after (above) the content, so it is not treated as covered by the first row.
        val children = onNodeWithTag("ptr").fetchSemanticsNode().children
        assertEquals("Refresh", children.last().config.getOrNull(SemanticsProperties.ContentDescription)?.single())

        element.performSemanticsAction(SemanticsActions.OnClick)
        waitForIdle()
        assertEquals(1, refreshes)
    }

    @Test
    fun heldSpinnerKeepsItsOffsetWhenTheContainerIsResized() = runComposeUiTest {
        var height by mutableStateOf(150.dp)
        lateinit var state: CupertinoPullToRefreshState
        var refreshingOffsetPx = 0f
        setContent {
            state = rememberCupertinoPullToRefreshState()
            refreshingOffsetPx = with(LocalDensity.current) { CupertinoPullToRefreshDefaults.RefreshingOffset.toPx() }
            CupertinoPullToRefreshBox(
                isRefreshing = true,
                onRefresh = {},
                state = state,
                modifier = Modifier.size(300.dp, height),
            ) {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(5) { CupertinoText("Row $it") }
                }
            }
        }
        waitForIdle()
        assertTrue(abs(state.distance - refreshingOffsetPx) < 1f, "distance=${state.distance}")
        height = 700.dp
        waitForIdle()
        assertTrue(abs(state.distance - refreshingOffsetPx) < 1f, "after resize distance=${state.distance}")
    }

    @Test
    fun contentAlignsWithinTheWholeBox() = runComposeUiTest {
        setContent {
            CupertinoPullToRefreshBox(
                isRefreshing = false,
                onRefresh = {},
                modifier = Modifier.size(300.dp, 500.dp).testTag("ptr"),
            ) {
                CupertinoText("Empty", Modifier.align(Alignment.Center).testTag("empty"))
            }
        }
        val box = onNodeWithTag("ptr").getBoundsInRoot()
        val text = onNodeWithTag("empty").getBoundsInRoot()
        val boxCenterY = (box.top + box.bottom) / 2
        val textCenterY = (text.top + text.bottom) / 2
        assertTrue(abs((boxCenterY - textCenterY).value) < 1f, "box=$box text=$text")
    }

    @Test
    fun pushingUpWhileRefreshingMovesTheSpinnerOneToOne() = runComposeUiTest {
        lateinit var state: CupertinoPullToRefreshState
        var refreshingOffsetPx = 0f
        setContent {
            state = rememberCupertinoPullToRefreshState()
            refreshingOffsetPx = with(LocalDensity.current) { CupertinoPullToRefreshDefaults.RefreshingOffset.toPx() }
            CupertinoPullToRefreshBox(
                isRefreshing = true,
                onRefresh = {},
                state = state,
                modifier = Modifier.size(300.dp, 500.dp).testTag("ptr"),
            ) {
                LazyColumn(Modifier.fillMaxWidth().height(500.dp)) {
                    items(50) { CupertinoText("Row $it") }
                }
            }
        }
        waitForIdle()
        val push = refreshingOffsetPx / 2f
        onNodeWithTag("ptr").performTouchInput {
            down(center)
            // Move past touch slop first, then push up by `push`.
            moveBy(Offset(0f, -viewConfiguration.touchSlop - 1f))
        }
        waitForIdle()
        val before = state.distance
        onNodeWithTag("ptr").performTouchInput { moveBy(Offset(0f, -push)) }
        waitForIdle()
        val after = state.distance
        onNodeWithTag("ptr").performTouchInput { up() }
        assertTrue(abs((before - after) - push) < 1.5f, "before=$before after=$after push=$push")
    }
}
