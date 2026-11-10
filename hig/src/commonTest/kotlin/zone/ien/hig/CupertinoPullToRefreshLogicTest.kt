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

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCupertinoApi::class)
class CupertinoPullToRefreshLogicTest {
    private val d = 800f
    private val c = 0.55f

    @Test
    fun rubberBandIsZeroAtZero() {
        assertEquals(0f, cupertinoRubberBand(0f, d, c))
    }

    @Test
    fun rubberBandIsZeroForNegativeOverscrollOrEmptyDimension() {
        assertEquals(0f, cupertinoRubberBand(-50f, d, c))
        assertEquals(0f, cupertinoRubberBand(100f, 0f, c))
        assertEquals(0f, cupertinoRubberBand(100f, -10f, c))
    }

    @Test
    fun rubberBandMatchesFormula() {
        val x = 300f
        val expected = (1f - 1f / (x * c / d + 1f)) * d
        assertTrue(abs(expected - cupertinoRubberBand(x, d, c)) < 0.001f)
    }

    @Test
    fun rubberBandIsStrictlyMonotonicAndResists() {
        var previous = 0f
        var x = 1f
        while (x < 100_000f) {
            val value = cupertinoRubberBand(x, d, c)
            assertTrue(value > previous, "not monotonic at x=$x")
            assertTrue(value < x, "rubber band must resist at x=$x")
            previous = value
            x *= 1.5f
        }
    }

    @Test
    fun rubberBandIsBoundedByDimensionAndApproachesIt() {
        assertTrue(cupertinoRubberBand(1e9f, d, c) <= d)
        assertTrue(cupertinoRubberBand(1e7f, d, c) > d * 0.99f)
    }

    @Test
    fun rubberBandSlopeAtOriginIsCoefficient() {
        val x = 1f
        assertTrue(abs(cupertinoRubberBand(x, d, c) / x - c) < 0.002f)
    }

    @Test
    fun rubberBandInverseRoundTrips() {
        listOf(0f, 1f, 50f, 300f, 2_000f).forEach { x ->
            val y = cupertinoRubberBand(x, d, c)
            assertTrue(abs(cupertinoRubberBandInverse(y, d, c) - x) < x * 0.001f + 0.01f, "x=$x")
        }
        assertEquals(0f, cupertinoRubberBandInverse(-5f, d, c))
    }

    private fun state(): CupertinoPullToRefreshState =
        CupertinoPullToRefreshState().apply {
            updateMetrics(thresholdPx = 100f, refreshingOffsetPx = 60f, containerPx = d)
        }

    /** Raw finger travel needed to reach [distance] after resistance. */
    private fun travelFor(distance: Float) = cupertinoRubberBandInverse(distance, d, c)

    @Test
    fun pullBelowThresholdNeitherCrossesNorRefreshes() {
        val state = state()
        val step = state.onPull(travelFor(80f), isRefreshing = false)
        assertFalse(step.crossedThreshold)
        assertTrue(state.distanceFraction in 0.79f..0.81f)
        assertFalse(state.onRelease(isRefreshing = false))
    }

    @Test
    fun thresholdCrossingIsReportedExactlyOncePerCrossing() {
        val state = state()
        var crossings = 0
        repeat(200) { if (state.onPull(5f, isRefreshing = false).crossedThreshold) crossings++ }
        assertEquals(1, crossings)
        assertTrue(state.distanceFraction > 1f)

        // Push back below the threshold, then cross again.
        while (state.distanceFraction >= 0.5f) {
            if (state.onPull(-5f, isRefreshing = false).crossedThreshold) crossings++
        }
        assertEquals(1, crossings)
        repeat(200) { if (state.onPull(5f, isRefreshing = false).crossedThreshold) crossings++ }
        assertEquals(2, crossings)
    }

    @Test
    fun releasingPastThresholdRequestsRefresh() {
        val state = state()
        state.onPull(travelFor(120f), isRefreshing = false)
        assertTrue(state.onRelease(isRefreshing = false))
    }

    @Test
    fun noCrossingOrRefreshWhileAlreadyRefreshing() {
        val state = state()
        val step = state.onPull(travelFor(150f), isRefreshing = true)
        assertFalse(step.crossedThreshold)
        assertFalse(state.onRelease(isRefreshing = true))
    }

    @Test
    fun pullConsumesAllDownwardDeltaAndUnwindsOnlyWhatWasPulled() {
        val state = state()
        assertEquals(40f, state.onPull(40f, isRefreshing = false).consumed)
        assertEquals(-40f, state.onPull(-100f, isRefreshing = false).consumed)
        assertEquals(0f, state.distance)
        assertEquals(0f, state.onPull(-10f, isRefreshing = false).consumed)
    }

    @Test
    fun snapToPlacesDistanceExactly() {
        val state = state()
        state.snapTo(60f)
        assertTrue(abs(state.distance - 60f) < 0.01f)
        assertTrue(abs(state.distanceFraction - 0.6f) < 0.001f)
    }

    @Test
    fun smallContainerStillAllowsReachingThreshold() {
        val state = CupertinoPullToRefreshState().apply {
            updateMetrics(thresholdPx = 100f, refreshingOffsetPx = 60f, containerPx = 50f)
        }
        var crossed = false
        repeat(1_000) { if (state.onPull(5f, isRefreshing = false).crossedThreshold) crossed = true }
        assertTrue(crossed)
    }
}
