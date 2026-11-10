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
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertRangeInfoEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCupertinoApi::class, ExperimentalTestApi::class)
class CupertinoGaugeTest {

    private fun assertClose(expected: Float, actual: Float, eps: Float = 1e-4f) {
        assertTrue(abs(expected - actual) <= eps, "expected $expected but was $actual")
    }

    // region fraction mapping

    @Test
    fun fractionMapsValueLinearlyInsideRange() {
        assertClose(0f, gaugeFraction(0f, 0f..1f))
        assertClose(0.25f, gaugeFraction(0.25f, 0f..1f))
        assertClose(1f, gaugeFraction(1f, 0f..1f))
        assertClose(0.5f, gaugeFraction(75f, 50f..100f))
    }

    @Test
    fun fractionClampsOutOfRangeValues() {
        assertEquals(0f, gaugeFraction(-3f, 0f..1f))
        assertEquals(1f, gaugeFraction(42f, 0f..1f))
        assertEquals(1f, gaugeFraction(Float.POSITIVE_INFINITY, 0f..1f))
        assertEquals(0f, gaugeFraction(Float.NEGATIVE_INFINITY, 0f..1f))
    }

    @Test
    fun fractionOfDegenerateRangeIsZeroWithoutDividingByZero() {
        assertEquals(0f, gaugeFraction(5f, 5f..5f))
        assertEquals(0f, gaugeFraction(7f, 5f..5f))
        assertEquals(0f, gaugeFraction(0f, 0f..0f))
    }

    @Test
    fun fractionSupportsNegativeRanges() {
        assertClose(0.5f, gaugeFraction(-5f, -10f..0f))
        assertClose(0.25f, gaugeFraction(-15f, -20f..0f))
        assertEquals(0f, gaugeFraction(-30f, -20f..-10f))
    }

    @Test
    fun fractionOfReversedRangeIsMeasuredFromStartTowardEnd() {
        // A "reversed" range has start > endInclusive; the gauge fills from start toward end.
        assertClose(0.75f, gaugeFraction(0.25f, 1f..0f))
        assertEquals(0f, gaugeFraction(2f, 1f..0f))
        assertEquals(1f, gaugeFraction(-1f, 1f..0f))
    }

    @Test
    fun fractionTreatsNaNAsEmpty() {
        assertEquals(0f, gaugeFraction(Float.NaN, 0f..1f))
        assertEquals(0f, gaugeFraction(0.5f, Float.NaN..1f))
        assertEquals(0f, gaugeFraction(0.5f, 0f..Float.NaN))
    }

    @Test
    fun fractionOfInfiniteRangeIsZero() {
        assertEquals(0f, gaugeFraction(0.5f, 0f..Float.POSITIVE_INFINITY))
        assertEquals(0f, gaugeFraction(0.5f, Float.NEGATIVE_INFINITY..Float.POSITIVE_INFINITY))
    }

    // endregion

    // region angle mapping

    @Test
    fun openRingIndicatorSweepsTwoHundredSeventyDegreesFromBottomStart() {
        assertClose(135f, gaugeOpenRingIndicatorAngle(0f))
        assertClose(270f, gaugeOpenRingIndicatorAngle(0.5f))
        assertClose(405f, gaugeOpenRingIndicatorAngle(1f))
    }

    @Test
    fun openRingIndicatorAngleClampsAndIgnoresNaN() {
        assertClose(135f, gaugeOpenRingIndicatorAngle(-1f))
        assertClose(405f, gaugeOpenRingIndicatorAngle(3f))
        assertClose(135f, gaugeOpenRingIndicatorAngle(Float.NaN))
    }

    @Test
    fun capacityRingSweepFillsClockwiseFullCircle() {
        assertClose(0f, gaugeCapacitySweepAngle(0f))
        assertClose(90f, gaugeCapacitySweepAngle(0.25f))
        assertClose(360f, gaugeCapacitySweepAngle(1f))
        assertClose(360f, gaugeCapacitySweepAngle(2f))
        assertClose(0f, gaugeCapacitySweepAngle(Float.NaN))
    }

    @Test
    fun indicatorOffsetLiesOnArc() {
        val center = Offset(50f, 50f)
        val top = gaugePointOnCircle(center, radius = 40f, angleDegrees = 270f)
        assertClose(50f, top.x, 1e-3f)
        assertClose(10f, top.y, 1e-3f)
        val right = gaugePointOnCircle(center, radius = 40f, angleDegrees = 0f)
        assertClose(90f, right.x, 1e-3f)
        assertClose(50f, right.y, 1e-3f)
    }

    @Test
    fun capAngleIsHalfStrokeSeenFromCenter() {
        // 6 px stroke on a 26 px radius: asin(3/26) ≈ 6.63°.
        assertClose(6.6256f, gaugeCapAngleDegrees(6f, 26f), 1e-3f)
        assertEquals(0f, gaugeCapAngleDegrees(6f, 0f))
        assertEquals(0f, gaugeCapAngleDegrees(0f, 26f))
    }

    @Test
    fun openRingGradientClosesInsideTheGapWithTheFirstColor() {
        val stops = gaugeOpenRingColorStops(listOf(Color.Green, Color.Yellow, Color.Red))
        assertEquals(0f to Color.Green, stops.first())
        assertEquals(0.75f to Color.Red, stops[2])
        // Angles just below 360° (where the start cap is drawn) sample the first color.
        assertEquals(1f to Color.Green, stops.last())
        assertTrue(stops.zipWithNext().all { (a, b) -> a.first <= b.first }, "stops must be ordered")
    }

    @Test
    fun capacityRingGradientKeepsCapsOnTheirOwnEnds() {
        val stops = gaugeCapacityRingColorStops(listOf(Color.Green, Color.Red), capDegrees = 36f)
        assertEquals(
            listOf(0f to Color.Green, 0.1f to Color.Green, 0.9f to Color.Red, 1f to Color.Red),
            stops.map { (p, c) -> ((p * 1000f).roundToInt() / 1000f) to c },
        )
    }

    // endregion

    // region semantics mapping

    @Test
    fun semanticsRangeInfoClampsValueAndOrdersRange() {
        assertEquals(ProgressBarRangeInfo(0.4f, 0f..1f), gaugeRangeInfo(0.4f, 0f..1f))
        assertEquals(ProgressBarRangeInfo(1f, 0f..1f), gaugeRangeInfo(9f, 0f..1f))
        assertEquals(ProgressBarRangeInfo(0.75f, 0f..1f), gaugeRangeInfo(0.25f, 1f..0f))
        assertEquals(ProgressBarRangeInfo(-10f, -10f..0f), gaugeRangeInfo(Float.NaN, -10f..0f))
        assertEquals(ProgressBarRangeInfo(5f, 5f..5f), gaugeRangeInfo(8f, 5f..5f))
    }

    @Test
    fun semanticsRangeInfoOfReversedRangeIsTheFilledFraction() {
        // The fill runs from start (100) toward end (0); 75 is a quarter of the way, matching the drawn fill.
        assertEquals(ProgressBarRangeInfo(0.25f, 0f..1f), gaugeRangeInfo(75f, 100f..0f))
        assertEquals(ProgressBarRangeInfo(1f, 0f..1f), gaugeRangeInfo(-5f, 100f..0f))
    }

    @Test
    fun semanticsRangeInfoOfInfiniteRangeIsEmptyFraction() {
        assertEquals(ProgressBarRangeInfo(0f, 0f..1f), gaugeRangeInfo(0.5f, 0f..Float.POSITIVE_INFINITY))
        assertEquals(
            ProgressBarRangeInfo(0f, 0f..1f),
            gaugeRangeInfo(0.5f, Float.NEGATIVE_INFINITY..Float.POSITIVE_INFINITY),
        )
    }

    @Test
    fun stateDescriptionIsRoundedPercentOfFraction() {
        assertEquals("0%", gaugeStateDescription(0f))
        assertEquals("33%", gaugeStateDescription(1f / 3f))
        assertEquals("100%", gaugeStateDescription(1f))
    }

    // endregion

    // region UI

    @Test
    fun gaugeExposesRangeInfoAndPercentStateWithoutCurrentValueLabel() = runComposeUiTest {
        setContent {
            TestTheme {
                CupertinoGauge(
                    value = 30f,
                    range = 0f..120f,
                    modifier = Modifier.testTag("gauge"),
                    label = { CupertinoText("Speed") },
                )
            }
        }

        onNodeWithTag("gauge")
            .assertRangeInfoEquals(ProgressBarRangeInfo(30f, 0f..120f))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "25%"))
        onNodeWithText("Speed", useUnmergedTree = true).assertExists()
    }

    @Test
    fun gaugeOmitsStateDescriptionWhenCurrentValueLabelProvided() = runComposeUiTest {
        setContent {
            TestTheme {
                CupertinoGauge(
                    value = 0.5f,
                    style = CupertinoGaugeStyle.AccessoryCircular,
                    modifier = Modifier.testTag("gauge"),
                    currentValueLabel = { CupertinoText("50") },
                    minimumValueLabel = { CupertinoText("0") },
                    maximumValueLabel = { CupertinoText("1") },
                )
            }
        }

        onNodeWithTag("gauge")
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.StateDescription))
            .assertRangeInfoEquals(ProgressBarRangeInfo(0.5f, 0f..1f))
        onNodeWithText("50", useUnmergedTree = true).assertExists()
        onNodeWithText("0", useUnmergedTree = true).assertExists()
    }

    @Test
    fun disabledGaugeIsMarkedDisabled() = runComposeUiTest {
        setContent {
            TestTheme {
                CupertinoGauge(
                    value = 0.5f,
                    enabled = false,
                    style = CupertinoGaugeStyle.AccessoryCircularCapacity,
                    modifier = Modifier.testTag("gauge"),
                )
            }
        }
        onNodeWithTag("gauge").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Disabled))
    }

    @Test
    fun linearCapacityFillsFromStartEdgeInBothDirections() = runComposeUiTest {
        val tint = Color(0xFFFF0000)
        var direction by mutableStateOf(LayoutDirection.Ltr)
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides direction) {
                TestTheme {
                    Box(Modifier.size(200.dp, 40.dp)) {
                        CupertinoGauge(
                            value = 0.25f,
                            style = CupertinoGaugeStyle.LinearCapacity,
                            colors = CupertinoGaugeDefaults.colors(tint = tint, trackColor = Color.Transparent),
                            modifier = Modifier.testTag("gauge").width(200.dp),
                        )
                    }
                }
            }
        }
        mainClock.advanceTimeBy(2_000)

        fun redAt(xFraction: Float): Boolean {
            val pixels = onNodeWithTag("gauge").captureToImage().toPixelMap()
            val c = pixels[(pixels.width * xFraction).toInt(), pixels.height / 2]
            return c.red > 0.8f && c.green < 0.2f && c.alpha > 0.8f
        }

        assertTrue(redAt(0.1f), "LTR: start edge should be filled")
        assertTrue(!redAt(0.9f), "LTR: end edge should be empty")

        direction = LayoutDirection.Rtl
        waitForIdle()
        mainClock.advanceTimeBy(2_000)
        assertTrue(redAt(0.9f), "RTL: start (right) edge should be filled")
        assertTrue(!redAt(0.1f), "RTL: end (left) edge should be empty")
    }

    @Test
    fun circularMinMaxLabelsStayAtTheirArcEndsInRtl() = runComposeUiTest {
        var direction by mutableStateOf(LayoutDirection.Ltr)
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides direction) {
                TestTheme {
                    CupertinoGauge(
                        value = 0.5f,
                        style = CupertinoGaugeStyle.AccessoryCircular,
                        modifier = Modifier.size(100.dp),
                        minimumValueLabel = { CupertinoText("MIN") },
                        maximumValueLabel = { CupertinoText("MAX") },
                    )
                }
            }
        }

        fun assertMinLeftOfMax(case: String) {
            val min = onNodeWithText("MIN", useUnmergedTree = true).getBoundsInRoot()
            val max = onNodeWithText("MAX", useUnmergedTree = true).getBoundsInRoot()
            assertTrue(min.right <= max.left, "$case: min label ($min) should be left of max label ($max)")
        }

        assertMinLeftOfMax("LTR")
        direction = LayoutDirection.Rtl
        waitForIdle()
        assertMinLeftOfMax("RTL")
    }

    @Test
    fun circularLabelsStaySquareWhenGaugeIsWide() = runComposeUiTest {
        setContent {
            TestTheme {
                Box(Modifier.size(300.dp, 200.dp)) {
                    CupertinoGauge(
                        value = 0.5f,
                        style = CupertinoGaugeStyle.AccessoryCircular,
                        modifier = Modifier.testTag("gauge").width(300.dp),
                        minimumValueLabel = { CupertinoText("MIN") },
                        maximumValueLabel = { CupertinoText("MAX") },
                    )
                }
            }
        }
        val gauge = onNodeWithTag("gauge").getBoundsInRoot()
        val min = onNodeWithText("MIN", useUnmergedTree = true).getBoundsInRoot()
        val max = onNodeWithText("MAX", useUnmergedTree = true).getBoundsInRoot()
        // The ring is a centered 58 dp square, so the labels stay near the middle instead of the far edges.
        assertTrue(min.left >= gauge.left + 100.dp, "min label drifted to the edge: $min in $gauge")
        assertTrue(max.right <= gauge.right - 100.dp, "max label drifted to the edge: $max in $gauge")
    }

    /** Samples the pixel at [angleDegrees] (clockwise from 3 o'clock) on the middle of a circular gauge's stroke. */
    private fun PixelMap.ringColorAt(density: Density, angleDegrees: Float): Color {
        val strokePx = with(density) { CupertinoGaugeTokens.CircularStroke.toPx() }
        val radius = (min(width, height) - strokePx) / 2f
        val p = gaugePointOnCircle(Offset(width / 2f, height / 2f), radius, angleDegrees)
        return this[p.x.toInt(), p.y.toInt()]
    }

    private fun Color.isGreen() = green > 0.6f && red < 0.3f && alpha > 0.8f

    @Test
    fun openRingGradientStartCapUsesTheFirstColor() = runComposeUiTest {
        setContent {
            TestTheme {
                CupertinoGauge(
                    // Full value moves the indicator (and its cut-out) to the far end, exposing the start cap.
                    value = 1f,
                    style = CupertinoGaugeStyle.AccessoryCircular,
                    colors = CupertinoGaugeDefaults.colors(gradient = listOf(Color.Green, Color.Red)),
                    modifier = Modifier.testTag("gauge").size(116.dp),
                )
            }
        }
        mainClock.advanceTimeBy(2_000)
        val pixels = onNodeWithTag("gauge").captureToImage().toPixelMap()
        // The start point is at 135°; 2° before it lies inside the round start cap.
        val cap = pixels.ringColorAt(density, GaugeOpenRingStartAngle - 2f)
        assertTrue(cap.isGreen(), "start cap should be the first gradient color but was $cap")
    }

    @Test
    fun capacityRingGradientStartCapUsesTheFirstColor() = runComposeUiTest {
        setContent {
            TestTheme {
                CupertinoGauge(
                    value = 0.5f,
                    style = CupertinoGaugeStyle.AccessoryCircularCapacity,
                    colors = CupertinoGaugeDefaults.colors(
                        gradient = listOf(Color.Green, Color.Red),
                        trackColor = Color.Transparent,
                    ),
                    modifier = Modifier.testTag("gauge").size(116.dp),
                )
            }
        }
        mainClock.advanceTimeBy(2_000)
        val pixels = onNodeWithTag("gauge").captureToImage().toPixelMap()
        // The fill starts at 12 o'clock (270°); 2° before it lies inside the round start cap.
        val cap = pixels.ringColorAt(density, 268f)
        assertTrue(cap.isGreen(), "start cap should be the first gradient color but was $cap")
    }

    // endregion
}

// CupertinoTheme applies the status bar appearance through LocalUIViewController on iOS, which
// runComposeUiTest does not provide, so it cannot be used in common tests. The components read
// the default theme locals without it.
@Composable
private fun TestTheme(content: @Composable () -> Unit) = content()
