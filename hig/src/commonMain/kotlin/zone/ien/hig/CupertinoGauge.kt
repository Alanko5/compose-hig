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

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import zone.ien.hig.theme.CupertinoTheme
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Visual style of a [CupertinoGauge], mirroring SwiftUI's `GaugeStyle`.
 *
 * - [LinearCapacity] — a horizontal capsule bar that fills from the start edge (SwiftUI `.linearCapacity`).
 * - [AccessoryLinear] — a thin tinted track with a hollow dot marking the value (SwiftUI `.accessoryLinear`).
 * - [AccessoryCircular] — an open ring (270° arc, gap at the bottom) with a hollow dot marking the value and the
 *   current value label in the middle (SwiftUI `.accessoryCircular`).
 * - [AccessoryCircularCapacity] — a closed ring that fills clockwise from the top (SwiftUI `.accessoryCircularCapacity`).
 */
@ExperimentalCupertinoApi
@Immutable
class CupertinoGaugeStyle private constructor(
    private val name: String,
) {
    override fun toString(): String = "CupertinoGaugeStyle.$name"

    companion object {
        /** Horizontal capsule bar with a fill, like SwiftUI `.linearCapacity`. This is the default style. */
        val LinearCapacity: CupertinoGaugeStyle = CupertinoGaugeStyle("LinearCapacity")

        /** Thin tinted track with a dot indicator, like SwiftUI `.accessoryLinear`. */
        val AccessoryLinear: CupertinoGaugeStyle = CupertinoGaugeStyle("AccessoryLinear")

        /** Open 270° ring with a dot indicator and the value in the middle, like SwiftUI `.accessoryCircular`. */
        val AccessoryCircular: CupertinoGaugeStyle = CupertinoGaugeStyle("AccessoryCircular")

        /** Closed ring filling clockwise from the top, like SwiftUI `.accessoryCircularCapacity`. */
        val AccessoryCircularCapacity: CupertinoGaugeStyle = CupertinoGaugeStyle("AccessoryCircularCapacity")
    }
}

/**
 * Cupertino Design Gauge.
 *
 * A gauge shows a value within a range, the Compose counterpart of SwiftUI's `Gauge`. Unlike a slider it is
 * read-only. Choose a look with [style]; see [CupertinoGaugeStyle].
 *
 * The value is mapped to a fraction of [range] measured from `range.start` toward `range.endInclusive`:
 * out-of-range values are clamped, `NaN` (or a `NaN`/infinite bound) shows an empty gauge, a zero-width range
 * (`min == max`) shows an empty gauge, and a "reversed" range such as `1f..0f` fills from `start` toward `end`.
 *
 * The tint can be a solid color or a gradient (the equivalent of SwiftUI `.tint(Gradient(...))`), see
 * [CupertinoGaugeDefaults.colors]. Linear styles fill from the start edge, so they mirror in right-to-left
 * layouts; circular styles always run clockwise, like their SwiftUI counterparts, so their minimum and maximum
 * value labels stay at the bottom-left and bottom-right ends of the ring in right-to-left layouts too.
 *
 * For accessibility the gauge is a single merged node exposing `progressBarRangeInfo` with the value and range
 * (for a reversed range, or a range with an infinite bound, the range info is the filled fraction in `0f..1f`, so it
 * always agrees with what is drawn).
 * When no [currentValueLabel] is provided, a percentage `stateDescription` is announced instead.
 *
 * @param value the current value. It is clamped to [range] for display.
 * @param modifier the [Modifier] to be applied to this gauge. Circular styles default to
 * [CupertinoGaugeDefaults.CircularSize]; set a `size` on this modifier to change it. If the modifier gives the
 * circular gauge a non-square area (for example `fillMaxWidth()`), the ring and its labels are drawn in the largest
 * square that fits, centered in that area.
 * @param range the range of valid values. Defaults to `0f..1f`.
 * @param style the visual style of the gauge. See [CupertinoGaugeStyle].
 * @param enabled controls the enabled appearance of this gauge. When `false`, it is drawn in a desaturated
 * color and reported as disabled to accessibility services.
 * @param colors [CupertinoGaugeColors] used to draw the gauge. See [CupertinoGaugeDefaults.colors].
 * @param label a view describing the purpose of the gauge.
 * @param currentValueLabel a view describing the current value.
 * @param minimumValueLabel a view describing the lower bound of [range].
 * @param maximumValueLabel a view describing the upper bound of [range].
 */
@ExperimentalCupertinoApi
@Composable
fun CupertinoGauge(
    value: Float,
    modifier: Modifier = Modifier,
    range: ClosedFloatingPointRange<Float> = 0f..1f,
    style: CupertinoGaugeStyle = CupertinoGaugeStyle.LinearCapacity,
    enabled: Boolean = true,
    colors: CupertinoGaugeColors = CupertinoGaugeDefaults.colors(),
    label: (@Composable () -> Unit)? = null,
    currentValueLabel: (@Composable () -> Unit)? = null,
    minimumValueLabel: (@Composable () -> Unit)? = null,
    maximumValueLabel: (@Composable () -> Unit)? = null,
) {
    val fraction = gaugeFraction(value, range)
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = spring(stiffness = 300f),
        label = "CupertinoGaugeFraction",
    )
    val rangeInfo = gaugeRangeInfo(value, range)
    val hasCurrentValueLabel = currentValueLabel != null
    val semanticsModifier = Modifier.semantics(mergeDescendants = true) {
        progressBarRangeInfo = rangeInfo
        if (!hasCurrentValueLabel) {
            stateDescription = gaugeStateDescription(fraction)
        }
        if (!enabled) disabled()
    }
    val labelColor = colors.labelColor(enabled)
    val secondaryColor = colors.secondaryLabelColor(enabled)

    if (style == CupertinoGaugeStyle.AccessoryCircular || style == CupertinoGaugeStyle.AccessoryCircularCapacity) {
        CircularGauge(
            modifier = modifier.then(semanticsModifier),
            fraction = { animatedFraction },
            capacity = style == CupertinoGaugeStyle.AccessoryCircularCapacity,
            enabled = enabled,
            colors = colors,
            labelColor = labelColor,
            secondaryColor = secondaryColor,
            label = label,
            currentValueLabel = currentValueLabel,
            minimumValueLabel = minimumValueLabel,
            maximumValueLabel = maximumValueLabel,
        )
    } else {
        LinearGauge(
            modifier = modifier.then(semanticsModifier),
            fraction = { animatedFraction },
            capacity = style == CupertinoGaugeStyle.LinearCapacity,
            enabled = enabled,
            colors = colors,
            labelColor = labelColor,
            secondaryColor = secondaryColor,
            label = label,
            currentValueLabel = currentValueLabel,
            minimumValueLabel = minimumValueLabel,
            maximumValueLabel = maximumValueLabel,
        )
    }
}

@OptIn(ExperimentalCupertinoApi::class)
@Composable
private fun LinearGauge(
    modifier: Modifier,
    fraction: () -> Float,
    capacity: Boolean,
    enabled: Boolean,
    colors: CupertinoGaugeColors,
    labelColor: Color,
    secondaryColor: Color,
    label: (@Composable () -> Unit)?,
    currentValueLabel: (@Composable () -> Unit)?,
    minimumValueLabel: (@Composable () -> Unit)?,
    maximumValueLabel: (@Composable () -> Unit)?,
) {
    val typography = CupertinoTheme.typography
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(CupertinoGaugeTokens.LinearLabelSpacing),
    ) {
        if (label != null || currentValueLabel != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(CupertinoGaugeTokens.LinearLabelSpacing),
            ) {
                Box(Modifier.weight(1f)) {
                    if (label != null) GaugeSlot(typography.subhead, labelColor, label)
                }
                if (currentValueLabel != null) GaugeSlot(typography.subhead, secondaryColor, currentValueLabel)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CupertinoGaugeTokens.LinearValueLabelSpacing),
        ) {
            if (minimumValueLabel != null) GaugeSlot(typography.caption1, secondaryColor, minimumValueLabel)
            val height =
                if (capacity) CupertinoGaugeTokens.LinearCapacityHeight else CupertinoGaugeTokens.AccessoryLinearIndicatorSize
            Spacer(
                modifier = Modifier
                    .weight(1f)
                    .height(height)
                    // The offscreen layer is only needed for the BlendMode.Clear cut-out of the accessory dot.
                    .then(if (capacity) Modifier else OffscreenLayer)
                    .drawWithCache {
                        val tint = colors.tintBrush(enabled, GaugeGeometry.Linear, size, strokeWidth = 0f)
                        val trackColor = colors.trackColor(enabled)
                        val indicatorColor = colors.indicatorColor(enabled)
                        onDrawBehind {
                            scale(scaleX = if (isRtl) -1f else 1f, scaleY = 1f) {
                                if (capacity) {
                                    drawLinearCapacity(fraction(), tint, trackColor)
                                } else {
                                    drawAccessoryLinear(fraction(), tint, indicatorColor)
                                }
                            }
                        }
                    },
            )
            if (maximumValueLabel != null) GaugeSlot(typography.caption1, secondaryColor, maximumValueLabel)
        }
    }
}

private fun DrawScope.drawLinearCapacity(fraction: Float, tint: Brush, trackColor: Color) {
    val radius = CornerRadius(size.height / 2f)
    drawRoundRect(color = trackColor, cornerRadius = radius)
    if (fraction <= 0f) return
    // Keep the fill at least a full capsule end wide so small values still read as a rounded fill.
    val fillWidth = max(size.width * fraction, min(size.height, size.width))
    clipRect(right = fillWidth) {
        drawRoundRect(brush = tint, size = Size(fillWidth, size.height), cornerRadius = radius)
    }
}

private fun DrawScope.drawAccessoryLinear(fraction: Float, tint: Brush, indicatorColor: Color) {
    val trackHeight = CupertinoGaugeTokens.AccessoryLinearTrackHeight.toPx()
    val indicatorRadius = size.height / 2f
    val centerY = size.height / 2f
    drawRoundRect(
        brush = tint,
        topLeft = Offset(0f, centerY - trackHeight / 2f),
        size = Size(size.width, trackHeight),
        cornerRadius = CornerRadius(trackHeight / 2f),
    )
    val travel = (size.width - 2f * indicatorRadius).coerceAtLeast(0f)
    val center = Offset(indicatorRadius + travel * fraction, centerY)
    drawIndicator(center, indicatorRadius, indicatorColor)
}

/** Draws the hollow dot used by the accessory styles, cutting a gap out of whatever is beneath. */
private fun DrawScope.drawIndicator(center: Offset, outerRadius: Float, color: Color) {
    val ringWidth = CupertinoGaugeTokens.IndicatorRingWidth.toPx()
    drawCircle(color = Color.Black, radius = outerRadius, center = center, blendMode = BlendMode.Clear)
    drawCircle(
        color = color,
        radius = (outerRadius - CupertinoGaugeTokens.IndicatorGap.toPx() - ringWidth / 2f).coerceAtLeast(ringWidth / 2f),
        center = center,
        style = Stroke(width = ringWidth),
    )
}

@OptIn(ExperimentalCupertinoApi::class)
@Composable
private fun CircularGauge(
    modifier: Modifier,
    fraction: () -> Float,
    capacity: Boolean,
    enabled: Boolean,
    colors: CupertinoGaugeColors,
    labelColor: Color,
    secondaryColor: Color,
    label: (@Composable () -> Unit)?,
    currentValueLabel: (@Composable () -> Unit)?,
    minimumValueLabel: (@Composable () -> Unit)?,
    maximumValueLabel: (@Composable () -> Unit)?,
) {
    val typography = CupertinoTheme.typography
    Box(modifier = modifier.size(CupertinoGaugeDefaults.CircularSize), contentAlignment = Alignment.Center) {
        // Keep the ring and its labels in a centered square even if the caller's modifier makes the box wider or
        // taller than it is high (for example fillMaxWidth()).
        Box(Modifier.aspectRatio(1f)) {
            Spacer(
                modifier = Modifier
                    .fillMaxSize()
                    // The offscreen layer is only needed for the BlendMode.Clear cut-out of the open-ring dot.
                    .then(if (capacity) Modifier else OffscreenLayer)
                    .drawWithCache {
                        val strokeWidth = CupertinoGaugeTokens.CircularStroke.toPx()
                        val trackColor = colors.trackColor(enabled)
                        val indicatorColor = colors.indicatorColor(enabled)
                        if (capacity) {
                            val tint = colors.tintBrush(enabled, GaugeGeometry.CapacityRing, size, strokeWidth)
                            val fullTint = colors.tintBrush(enabled, GaugeGeometry.FullCapacityRing, size, strokeWidth)
                            onDrawBehind { drawCapacityRing(fraction(), tint, fullTint, trackColor) }
                        } else {
                            val tint = colors.tintBrush(enabled, GaugeGeometry.OpenRing, size, strokeWidth)
                            onDrawBehind { drawOpenRing(fraction(), tint, indicatorColor) }
                        }
                    },
            )

            val center = currentValueLabel ?: label
            if (center != null) {
                Box(Modifier.align(Alignment.Center).padding(CupertinoGaugeTokens.CircularStroke + 2.dp)) {
                    GaugeSlot(typography.headline.copy(fontWeight = FontWeight.SemiBold), labelColor, center)
                }
            }
            if (!capacity) {
                val bottomInset = Modifier.padding(horizontal = 2.dp)
                // The ring does not mirror in RTL (it always starts bottom-left and runs clockwise), so the labels
                // use absolute alignment to stay next to their ends of the arc.
                if (minimumValueLabel != null) {
                    Box(Modifier.align(AbsoluteAlignment.BottomLeft).then(bottomInset)) {
                        GaugeSlot(typography.caption2, secondaryColor, minimumValueLabel)
                    }
                }
                if (maximumValueLabel != null) {
                    Box(Modifier.align(AbsoluteAlignment.BottomRight).then(bottomInset)) {
                        GaugeSlot(typography.caption2, secondaryColor, maximumValueLabel)
                    }
                }
                if (currentValueLabel != null && label != null && minimumValueLabel == null && maximumValueLabel == null) {
                    Box(Modifier.align(Alignment.BottomCenter)) {
                        GaugeSlot(typography.caption2, secondaryColor, label)
                    }
                }
            }
        }
    }
}

private val OffscreenLayer = Modifier.graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }

private fun DrawScope.drawOpenRing(fraction: Float, tint: Brush, indicatorColor: Color) {
    val stroke = CupertinoGaugeTokens.CircularStroke.toPx()
    val inset = stroke / 2f
    val arcSize = Size(size.minDimension - stroke, size.minDimension - stroke)
    val topLeft = Offset((size.width - arcSize.width) / 2f, (size.height - arcSize.height) / 2f)
    // Rotate so the arc (and a sweep-gradient tint) starts at angle 0.
    rotate(degrees = GaugeOpenRingStartAngle, pivot = center) {
        drawArc(
            brush = tint,
            startAngle = 0f,
            sweepAngle = GaugeOpenRingSweep,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
    }
    val radius = arcSize.width / 2f
    val indicatorCenter = gaugePointOnCircle(center, radius, gaugeOpenRingIndicatorAngle(fraction))
    drawIndicator(indicatorCenter, inset + CupertinoGaugeTokens.IndicatorGap.toPx(), indicatorColor)
}

private fun DrawScope.drawCapacityRing(fraction: Float, tint: Brush, fullTint: Brush, trackColor: Color) {
    val stroke = CupertinoGaugeTokens.CircularStroke.toPx()
    val arcSize = Size(size.minDimension - stroke, size.minDimension - stroke)
    val topLeft = Offset((size.width - arcSize.width) / 2f, (size.height - arcSize.height) / 2f)
    drawArc(
        color = trackColor,
        startAngle = 0f,
        sweepAngle = 360f,
        useCenter = false,
        topLeft = topLeft,
        size = arcSize,
        style = Stroke(width = stroke),
    )
    val sweep = gaugeCapacitySweepAngle(fraction)
    if (sweep <= 0f) return
    if (sweep >= 360f) {
        // A full ring has no caps; the gradient seam sits at 12 o'clock.
        rotate(degrees = -90f, pivot = center) {
            drawArc(
                brush = fullTint,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Butt),
            )
        }
        return
    }
    // Rotate the frame back by the round cap's angular half-width so the start cap (which reaches behind the start
    // point) is drawn at small positive angles of the sweep gradient instead of wrapping around to its last color.
    val capDegrees = gaugeCapAngleDegrees(stroke, arcSize.width / 2f)
    rotate(degrees = -90f - capDegrees, pivot = center) {
        drawArc(
            brush = tint,
            startAngle = capDegrees,
            sweepAngle = sweep,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
    }
}

@Composable
private fun GaugeSlot(
    style: androidx.compose.ui.text.TextStyle,
    color: Color,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalContentColor provides color) {
        ProvideTextStyle(style, content)
    }
}

/** Which shape a gauge tint is painted onto; decides how a gradient tint is laid out. */
internal enum class GaugeGeometry { Linear, OpenRing, CapacityRing, FullCapacityRing }

/**
 * Represents the colors used by a [CupertinoGauge].
 *
 * See [CupertinoGaugeDefaults.colors] for the default implementation.
 */
@ExperimentalCupertinoApi
@Immutable
class CupertinoGaugeColors internal constructor(
    private val tintColors: List<Color>?,
    private val tintBrush: Brush?,
    private val trackColor: Color,
    private val indicatorColor: Color,
    private val labelColor: Color,
    private val secondaryLabelColor: Color,
    private val disabledTintColor: Color,
    private val disabledTrackColor: Color,
    private val disabledLabelColor: Color,
) {
    /**
     * The brush used for the filled part of the gauge. A gradient given as colors is laid out along the gauge
     * (left-to-right for linear styles, along the arc for circular ones); a custom [Brush] is used as-is.
     */
    internal fun tintBrush(enabled: Boolean, geometry: GaugeGeometry, size: Size, strokeWidth: Float): Brush {
        if (!enabled) return SolidColor(disabledTintColor)
        tintBrush?.let { return it }
        val stops = tintColors.orEmpty()
        if (stops.size < 2) return SolidColor(stops.firstOrNull() ?: Color.Unspecified)
        val center = Offset(size.width / 2f, size.height / 2f)
        return when (geometry) {
            GaugeGeometry.Linear -> Brush.horizontalGradient(stops, startX = 0f, endX = size.width)
            GaugeGeometry.OpenRing -> {
                // Drawn rotated so the arc starts at angle 0 and spans GaugeOpenRingSweep degrees.
                val colorStops = gaugeOpenRingColorStops(stops)
                Brush.sweepGradient(*colorStops.toTypedArray(), center = center)
            }
            GaugeGeometry.CapacityRing -> {
                // Drawn rotated back by the cap angle, starting at that angle (see drawCapacityRing).
                val radius = (min(size.width, size.height) - strokeWidth) / 2f
                val colorStops = gaugeCapacityRingColorStops(stops, gaugeCapAngleDegrees(strokeWidth, radius))
                Brush.sweepGradient(*colorStops.toTypedArray(), center = center)
            }
            GaugeGeometry.FullCapacityRing -> Brush.sweepGradient(stops, center = center)
        }
    }

    internal fun trackColor(enabled: Boolean): Color = if (enabled) trackColor else disabledTrackColor

    internal fun indicatorColor(enabled: Boolean): Color = if (enabled) indicatorColor else disabledLabelColor

    internal fun labelColor(enabled: Boolean): Color = if (enabled) labelColor else disabledLabelColor

    internal fun secondaryLabelColor(enabled: Boolean): Color = if (enabled) secondaryLabelColor else disabledLabelColor

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CupertinoGaugeColors) return false
        return tintColors == other.tintColors &&
            tintBrush == other.tintBrush &&
            trackColor == other.trackColor &&
            indicatorColor == other.indicatorColor &&
            labelColor == other.labelColor &&
            secondaryLabelColor == other.secondaryLabelColor &&
            disabledTintColor == other.disabledTintColor &&
            disabledTrackColor == other.disabledTrackColor &&
            disabledLabelColor == other.disabledLabelColor
    }

    override fun hashCode(): Int {
        var result = tintColors.hashCode()
        result = 31 * result + tintBrush.hashCode()
        result = 31 * result + trackColor.hashCode()
        result = 31 * result + indicatorColor.hashCode()
        result = 31 * result + labelColor.hashCode()
        result = 31 * result + secondaryLabelColor.hashCode()
        result = 31 * result + disabledTintColor.hashCode()
        result = 31 * result + disabledTrackColor.hashCode()
        result = 31 * result + disabledLabelColor.hashCode()
        return result
    }
}

/**
 * Contains the default values used by [CupertinoGauge].
 */
@ExperimentalCupertinoApi
@Immutable
object CupertinoGaugeDefaults {
    /** Default diameter of the circular gauge styles. */
    val CircularSize: Dp = 58.dp

    /**
     * Creates [CupertinoGaugeColors] with a solid [tint].
     *
     * @param tint color of the filled part (capacity styles) or the whole track (accessory styles).
     * @param trackColor color of the unfilled track of the capacity styles.
     * @param indicatorColor color of the hollow dot of the accessory styles.
     * @param labelColor content color of the `label` and of the centered value of circular styles.
     * @param secondaryLabelColor content color of the current, minimum and maximum value labels.
     * @param disabledTintColor tint used when the gauge is disabled.
     * @param disabledTrackColor track color used when the gauge is disabled.
     * @param disabledLabelColor content and indicator color used when the gauge is disabled.
     */
    @Composable
    @ReadOnlyComposable
    fun colors(
        tint: Color = CupertinoTheme.colorScheme.accent,
        trackColor: Color = CupertinoTheme.colorScheme.systemFill,
        indicatorColor: Color = CupertinoTheme.colorScheme.label,
        labelColor: Color = CupertinoTheme.colorScheme.label,
        secondaryLabelColor: Color = CupertinoTheme.colorScheme.secondaryLabel,
        disabledTintColor: Color = CupertinoTheme.colorScheme.tertiaryLabel,
        disabledTrackColor: Color = CupertinoTheme.colorScheme.quaternarySystemFill,
        disabledLabelColor: Color = CupertinoTheme.colorScheme.tertiaryLabel,
    ): CupertinoGaugeColors =
        CupertinoGaugeColors(
            tintColors = listOf(tint),
            tintBrush = null,
            trackColor = trackColor,
            indicatorColor = indicatorColor,
            labelColor = labelColor,
            secondaryLabelColor = secondaryLabelColor,
            disabledTintColor = disabledTintColor,
            disabledTrackColor = disabledTrackColor,
            disabledLabelColor = disabledLabelColor,
        )

    /**
     * Creates [CupertinoGaugeColors] with a gradient tint, the equivalent of SwiftUI
     * `.tint(Gradient(colors: ...))`. The colors are spread evenly along the gauge: from the start edge to the end
     * edge for linear styles (mirrored in RTL) and clockwise along the arc for circular styles.
     *
     * @param gradient the gradient colors, in order from the minimum to the maximum value. Must not be empty.
     * @see colors for the other parameters.
     */
    @Composable
    @ReadOnlyComposable
    fun colors(
        gradient: List<Color>,
        trackColor: Color = CupertinoTheme.colorScheme.systemFill,
        indicatorColor: Color = CupertinoTheme.colorScheme.label,
        labelColor: Color = CupertinoTheme.colorScheme.label,
        secondaryLabelColor: Color = CupertinoTheme.colorScheme.secondaryLabel,
        disabledTintColor: Color = CupertinoTheme.colorScheme.tertiaryLabel,
        disabledTrackColor: Color = CupertinoTheme.colorScheme.quaternarySystemFill,
        disabledLabelColor: Color = CupertinoTheme.colorScheme.tertiaryLabel,
    ): CupertinoGaugeColors {
        require(gradient.isNotEmpty()) { "gradient must contain at least one color" }
        return CupertinoGaugeColors(
            tintColors = gradient,
            tintBrush = null,
            trackColor = trackColor,
            indicatorColor = indicatorColor,
            labelColor = labelColor,
            secondaryLabelColor = secondaryLabelColor,
            disabledTintColor = disabledTintColor,
            disabledTrackColor = disabledTrackColor,
            disabledLabelColor = disabledLabelColor,
        )
    }

    /**
     * Creates [CupertinoGaugeColors] with an arbitrary [tint] [Brush], used exactly as given in the gauge's own
     * coordinate space. Prefer the `gradient` overload for gradients that should follow the gauge's shape
     * (for example along a circular arc); use this one for custom shaders or precisely positioned gradients.
     *
     * @see colors for the other parameters.
     */
    @Composable
    @ReadOnlyComposable
    fun colors(
        tint: Brush,
        trackColor: Color = CupertinoTheme.colorScheme.systemFill,
        indicatorColor: Color = CupertinoTheme.colorScheme.label,
        labelColor: Color = CupertinoTheme.colorScheme.label,
        secondaryLabelColor: Color = CupertinoTheme.colorScheme.secondaryLabel,
        disabledTintColor: Color = CupertinoTheme.colorScheme.tertiaryLabel,
        disabledTrackColor: Color = CupertinoTheme.colorScheme.quaternarySystemFill,
        disabledLabelColor: Color = CupertinoTheme.colorScheme.tertiaryLabel,
    ): CupertinoGaugeColors =
        CupertinoGaugeColors(
            tintColors = null,
            tintBrush = tint,
            trackColor = trackColor,
            indicatorColor = indicatorColor,
            labelColor = labelColor,
            secondaryLabelColor = secondaryLabelColor,
            disabledTintColor = disabledTintColor,
            disabledTrackColor = disabledTrackColor,
            disabledLabelColor = disabledLabelColor,
        )
}

internal object CupertinoGaugeTokens {
    val LinearCapacityHeight = 6.dp
    val AccessoryLinearTrackHeight = 5.dp
    val AccessoryLinearIndicatorSize = 11.dp
    val CircularStroke = 6.dp
    val IndicatorRingWidth = 1.5.dp
    val IndicatorGap = 1.5.dp
    val LinearLabelSpacing = 4.dp
    val LinearValueLabelSpacing = 8.dp
}

/** Angle (degrees, clockwise from 3 o'clock) at which the open ring starts: bottom-left. */
internal const val GaugeOpenRingStartAngle = 135f

/** Angular length of the open ring, leaving a 90° gap at the bottom. */
internal const val GaugeOpenRingSweep = 270f

/**
 * Maps [value] to a fraction in `0f..1f` of [range], measured from `range.start` toward `range.endInclusive`.
 *
 * - Values outside the range are clamped.
 * - A reversed range (`start > endInclusive`) is supported: the fraction grows toward `endInclusive`.
 * - `NaN` values, `NaN` or infinite bounds and zero-width ranges (`min == max`) yield `0f`.
 */
internal fun gaugeFraction(value: Float, range: ClosedFloatingPointRange<Float>): Float {
    val start = range.start
    val end = range.endInclusive
    if (value.isNaN() || !start.isFinite() || !end.isFinite()) return 0f
    val span = end - start
    if (span == 0f || !span.isFinite()) return 0f
    val fraction = (value - start) / span
    return if (fraction.isNaN()) 0f else fraction.coerceIn(0f, 1f)
}

/** Angle (degrees, clockwise from 3 o'clock) of the open ring indicator for [fraction]. */
internal fun gaugeOpenRingIndicatorAngle(fraction: Float): Float =
    GaugeOpenRingStartAngle + GaugeOpenRingSweep * fraction.sanitizedFraction()

/** Sweep (degrees, clockwise from 12 o'clock) of the closed capacity ring for [fraction]. */
internal fun gaugeCapacitySweepAngle(fraction: Float): Float = 360f * fraction.sanitizedFraction()

/** Point on a circle around [center] at [angleDegrees] (clockwise from 3 o'clock, y pointing down). */
internal fun gaugePointOnCircle(center: Offset, radius: Float, angleDegrees: Float): Offset {
    val radians = angleDegrees * PI.toFloat() / 180f
    return Offset(center.x + radius * cos(radians), center.y + radius * sin(radians))
}

/**
 * Accessibility range info: the range ordered low→high, the value clamped into it (`NaN` → minimum).
 *
 * A reversed range (`start > endInclusive`) or a range with an infinite bound is reported as the filled fraction
 * in `0f..1f` instead, so the announced position always matches the drawn fill.
 */
internal fun gaugeRangeInfo(value: Float, range: ClosedFloatingPointRange<Float>): ProgressBarRangeInfo {
    val start = range.start
    val end = range.endInclusive
    if (start.isInfinite() || end.isInfinite() || start > end) {
        return ProgressBarRangeInfo(gaugeFraction(value, range), 0f..1f)
    }
    val low = min(start, end).let { if (it.isNaN()) 0f else it }
    val high = max(start, end).let { if (it.isNaN()) low else it }
    val current = if (value.isNaN()) low else value.coerceIn(low, high)
    return ProgressBarRangeInfo(current, low..high)
}

/**
 * Angular half-width (degrees) of a round stroke cap of [strokeWidth] on a circle of [radius]: how far the cap
 * reaches behind the start of an arc.
 */
internal fun gaugeCapAngleDegrees(strokeWidth: Float, radius: Float): Float {
    if (radius <= 0f || strokeWidth <= 0f) return 0f
    val ratio = (strokeWidth / 2f / radius).coerceAtMost(1f)
    return asin(ratio) * 180f / PI.toFloat()
}

/**
 * Sweep-gradient stops for the open ring (drawn rotated so the arc starts at angle 0). The colors span the 270° arc;
 * the 90° gap is split between the last color (end cap) and the first color (start cap, which sits just below 360°).
 */
internal fun gaugeOpenRingColorStops(colors: List<Color>): List<Pair<Float, Color>> {
    val end = GaugeOpenRingSweep / 360f
    val gapMiddle = (end + 1f) / 2f
    return colors.mapIndexed { i, c -> (end * i / (colors.size - 1)) to c } +
        listOf(gapMiddle to colors.last(), (gapMiddle + 1e-4f) to colors.first(), 1f to colors.first())
}

/**
 * Sweep-gradient stops for the capacity ring, drawn in a frame rotated back by [capDegrees] and starting at that
 * angle. The colors span `capDegrees..360 - capDegrees`; the first color also covers `0..capDegrees` (the start cap)
 * and the last color the rest up to 360°, so neither cap wraps around to the other end of the gradient.
 */
internal fun gaugeCapacityRingColorStops(colors: List<Color>, capDegrees: Float): List<Pair<Float, Color>> {
    val from = (capDegrees / 360f).coerceIn(0f, 0.25f)
    val to = 1f - from
    return listOf(0f to colors.first()) +
        colors.mapIndexed { i, c -> (from + (to - from) * i / (colors.size - 1)) to c } +
        listOf(1f to colors.last())
}

/** Accessibility state description used when no current value label is given: a rounded percentage. */
internal fun gaugeStateDescription(fraction: Float): String = "${(fraction.sanitizedFraction() * 100f).roundToInt()}%"

private fun Float.sanitizedFraction(): Float = if (isNaN()) 0f else coerceIn(0f, 1f)
