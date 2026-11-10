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

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Mouse wheel pulls. Desktop only: the pixel distance of one wheel step differs per platform
 * (on the Apple native targets `scroll(-5f)` already refreshes or barely moves), so the step
 * counts below are calibrated for the desktop (JVM) wheel.
 */
@OptIn(ExperimentalCupertinoApi::class, ExperimentalTestApi::class)
class CupertinoPullToRefreshWheelTest {
    @Test
    fun mouseWheelPullPastTheTopIsReleasedWithoutAFling() = runComposeUiTest {
        var refreshes by mutableIntStateOf(0)
        lateinit var state: CupertinoPullToRefreshState
        // Start slightly scrolled: the wheel scroll then runs into the top and its leftover pulls.
        val listState = LazyListState(firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 10)
        setContent {
            state = rememberCupertinoPullToRefreshState()
            CupertinoPullToRefreshBox(
                isRefreshing = false,
                onRefresh = { refreshes++ },
                state = state,
                modifier = Modifier.size(300.dp, 500.dp).testTag("ptr"),
            ) {
                LazyColumn(Modifier.fillMaxWidth().height(500.dp), state = listState) {
                    items(50) { CupertinoText("Row $it") }
                }
            }
        }

        mainClock.autoAdvance = false
        onNodeWithTag("ptr").performMouseInput {
            moveTo(center)
            scroll(-5f)
        }
        var peak = 0f
        repeat(10) {
            mainClock.advanceTimeBy(16)
            peak = maxOf(peak, state.distance)
        }
        assertTrue(peak > 0f, "a wheel scroll past the top should pull")

        // No fling follows a wheel scroll; the pull must still be released once the wheel stops.
        mainClock.autoAdvance = true
        waitForIdle()
        assertEquals(0f, state.distance)
        assertTrue(!state.isDragging)
        assertEquals(0, refreshes)
    }

    @Test
    fun longMouseWheelPullRefreshesOnce() = runComposeUiTest {
        var refreshes by mutableIntStateOf(0)
        val listState = LazyListState(firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 10)
        setContent {
            CupertinoPullToRefreshBox(
                isRefreshing = false,
                onRefresh = { refreshes++ },
                modifier = Modifier.size(300.dp, 500.dp).testTag("ptr"),
            ) {
                LazyColumn(Modifier.fillMaxWidth().height(500.dp), state = listState) {
                    items(50) { CupertinoText("Row $it") }
                }
            }
        }

        onNodeWithTag("ptr").performMouseInput {
            moveTo(center)
            // Far enough past the top that the rubber-banded pull passes the threshold.
            scroll(-40f)
        }
        waitForIdle()
        assertEquals(1, refreshes)
    }
}
