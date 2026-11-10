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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.isUnspecified
import kotlin.jvm.JvmInline
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * How a [CupertinoLabel] presents its title and icon — the equivalent of SwiftUI's `LabelStyle`.
 *
 * [Automatic] defers to the style provided by an enclosing container through
 * [LocalCupertinoLabelStyle] / [ProvideCupertinoLabelStyle] (the `.labelStyle(_:)` modifier in
 * SwiftUI), and falls back to [TitleAndIcon] when no container sets one.
 */
@ExperimentalCupertinoApi
@Immutable
@JvmInline
value class CupertinoLabelStyle private constructor(private val value: Int) {
    override fun toString(): String = when (this) {
        Automatic -> "Automatic"
        TitleAndIcon -> "TitleAndIcon"
        TitleOnly -> "TitleOnly"
        IconOnly -> "IconOnly"
        else -> "Unknown"
    }

    companion object {
        /** Use the style of the enclosing container, or [TitleAndIcon] when there is none. */
        val Automatic = CupertinoLabelStyle(0)

        /** Show the icon followed by the title. */
        val TitleAndIcon = CupertinoLabelStyle(1)

        /** Show only the title. The icon is not composed and is invisible to accessibility. */
        val TitleOnly = CupertinoLabelStyle(2)

        /** Show only the icon. The title remains the accessibility label of the icon. */
        val IconOnly = CupertinoLabelStyle(3)
    }
}

/**
 * The [CupertinoLabelStyle] that [CupertinoLabel]s with [CupertinoLabelStyle.Automatic] adopt.
 * Containers such as toolbars provide [CupertinoLabelStyle.IconOnly] here to collapse labels into icons.
 *
 * @see ProvideCupertinoLabelStyle
 */
@ExperimentalCupertinoApi
val LocalCupertinoLabelStyle: ProvidableCompositionLocal<CupertinoLabelStyle> =
    staticCompositionLocalOf { CupertinoLabelStyle.Automatic }

/**
 * Sets the [CupertinoLabelStyle] for every [CupertinoLabel] in [content] that does not specify its own
 * style — the equivalent of SwiftUI's `.labelStyle(_:)` modifier applied to a container.
 */
@ExperimentalCupertinoApi
@Composable
fun ProvideCupertinoLabelStyle(
    style: CupertinoLabelStyle,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalCupertinoLabelStyle provides style, content = content)
}

/**
 * Resolves the style a label is drawn with: an explicit [style] wins over the [inherited] container
 * style, and [CupertinoLabelStyle.Automatic] everywhere resolves to [CupertinoLabelStyle.TitleAndIcon].
 * The result is never [CupertinoLabelStyle.Automatic].
 */
@OptIn(ExperimentalCupertinoApi::class)
internal fun resolveCupertinoLabelStyle(
    style: CupertinoLabelStyle,
    inherited: CupertinoLabelStyle,
): CupertinoLabelStyle = when {
    style != CupertinoLabelStyle.Automatic -> style
    inherited != CupertinoLabelStyle.Automatic -> inherited
    else -> CupertinoLabelStyle.TitleAndIcon
}

/** Default values for [CupertinoLabel]. */
@ExperimentalCupertinoApi
@Immutable
object CupertinoLabelDefaults {
    /** Gap between the icon and the title. */
    val Spacing: Dp = 8.dp

    /** Icon size relative to the font size of the current text style (body 17 sp → ~20 dp). */
    const val IconScale: Float = 1.2f

    /**
     * The icon size matching [textStyle], so the icon scales with Dynamic Type like an SF Symbol does.
     * Falls back to [CupertinoIconDefaults.MediumSize] when the style has no font size.
     */
    @Composable
    fun iconSize(textStyle: TextStyle = LocalTextStyle.current): Dp =
        iconSizeFor(textStyle, LocalDensity.current)
}

@OptIn(ExperimentalCupertinoApi::class)
internal fun iconSizeFor(textStyle: TextStyle, density: Density): Dp {
    val fontSize = textStyle.fontSize
    if (fontSize.isUnspecified || !fontSize.isSp) return CupertinoIconDefaults.MediumSize
    return with(density) { fontSize.toDp() } * CupertinoLabelDefaults.IconScale
}

/** How the icon is aligned against the title. */
internal enum class CupertinoLabelIconAlignment {
    /** Center of the icon on the center of the first text line, like SwiftUI. */
    FirstLine,

    /** Icon and title both centered in the label height, like `Row(CenterVertically)`. */
    Center,
}

/**
 * A standard label for user interface items, consisting of an icon with a title — the equivalent of
 * SwiftUI's `Label`.
 *
 * The icon sits before the title (mirrored in right-to-left layouts) and is centered on the first line
 * of the title, so multi-line titles keep the icon next to their first line. Both slots inherit
 * [LocalContentColor] and [LocalTextStyle], so a label adopts the color and typography of the row,
 * button or menu it is placed in.
 *
 * How the label is presented is decided by [style]; with [CupertinoLabelStyle.Automatic] it follows
 * [LocalCupertinoLabelStyle], which lets a container (for example a toolbar) collapse all of its labels
 * into icons through [ProvideCupertinoLabelStyle].
 *
 * Accessibility: in [CupertinoLabelStyle.TitleAndIcon] the icon is hidden from accessibility because the
 * title already describes it; in [CupertinoLabelStyle.TitleOnly] the icon is not composed at all; in
 * [CupertinoLabelStyle.IconOnly] the title is still composed (but not drawn) on top of the icon so screen
 * readers keep announcing it.
 *
 * ```
 * CupertinoLabel(
 *     title = { CupertinoText("Favorites") },
 *     icon = { CupertinoIcon(CupertinoIcons.Default.Heart, contentDescription = null) },
 * )
 * ```
 *
 * @param title the title of the label, typically a [CupertinoText].
 * @param icon the icon of the label, typically a [CupertinoIcon] with a null content description.
 * @param modifier the [Modifier] to be applied to this label.
 * @param style how the label is presented, see [CupertinoLabelStyle].
 * @param spacing the gap between the icon and the title.
 */
@ExperimentalCupertinoApi
@Composable
fun CupertinoLabel(
    title: @Composable () -> Unit,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    style: CupertinoLabelStyle = CupertinoLabelStyle.Automatic,
    spacing: Dp = CupertinoLabelDefaults.Spacing,
) {
    CupertinoLabelLayout(
        title = title,
        icon = icon,
        modifier = modifier,
        style = resolveCupertinoLabelStyle(style, LocalCupertinoLabelStyle.current),
        spacing = spacing,
        iconAlignment = CupertinoLabelIconAlignment.FirstLine,
    )
}

/**
 * A [CupertinoLabel] showing [text] next to [icon].
 *
 * The icon is sized from the current [LocalTextStyle] (see [CupertinoLabelDefaults.iconSize]) unless
 * [iconSize] is given, and tinted with [iconTint]. In [CupertinoLabelStyle.IconOnly], [text] becomes the
 * content description of the icon.
 *
 * In right-to-left layouts the label mirrors the icon's *position*, not its glyph: the glyph is flipped
 * only when [icon] has [ImageVector.autoMirror] set. For directional icons (chevrons, arrows) pass a
 * vector with `autoMirror = true`, or choose the icon from `LocalLayoutDirection`.
 *
 * ```
 * CupertinoLabel(text = "Favorites", icon = CupertinoIcons.Default.Heart)
 * ```
 *
 * @param text the title of the label.
 * @param icon the icon of the label.
 * @param modifier the [Modifier] to be applied to this label.
 * @param style how the label is presented, see [CupertinoLabelStyle].
 * @param spacing the gap between the icon and the title.
 * @param iconSize the size of the icon; [Dp.Unspecified] derives it from the current text style.
 * @param iconTint the tint of the icon; [Color.Unspecified] draws it untinted.
 * @param textColor the color of the title; [Color.Unspecified] uses [LocalContentColor].
 * @param maxLines the maximum number of lines of the title.
 */
@ExperimentalCupertinoApi
@Composable
fun CupertinoLabel(
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    style: CupertinoLabelStyle = CupertinoLabelStyle.Automatic,
    spacing: Dp = CupertinoLabelDefaults.Spacing,
    iconSize: Dp = Dp.Unspecified,
    iconTint: Color = LocalContentColor.current,
    textColor: Color = Color.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
) {
    val resolved = resolveCupertinoLabelStyle(style, LocalCupertinoLabelStyle.current)
    val size = if (iconSize.isSpecified) iconSize else CupertinoLabelDefaults.iconSize()
    val iconOnly = resolved == CupertinoLabelStyle.IconOnly
    CupertinoLabelLayout(
        // In IconOnly the icon carries the text itself, so no hidden title is needed.
        title = if (iconOnly) null else {
            { CupertinoText(text = text, color = textColor, maxLines = maxLines) }
        },
        icon = {
            CupertinoIcon(
                imageVector = icon,
                contentDescription = if (iconOnly) text else null,
                tint = iconTint,
                modifier = Modifier.size(size),
            )
        },
        modifier = modifier,
        style = resolved,
        spacing = spacing,
        iconAlignment = CupertinoLabelIconAlignment.FirstLine,
        clearIconSemantics = !iconOnly,
    )
}

private const val TitleId = "title"
private const val IconId = "icon"

/**
 * Layout shared by the public overloads and by components that reuse the label internally.
 *
 * @param style an already resolved style (never Automatic).
 * @param title null when the caller handles the IconOnly accessibility label itself.
 * @param clearIconSemantics whether icon semantics are hidden in TitleAndIcon / IconOnly.
 */
@OptIn(ExperimentalCupertinoApi::class)
@Composable
internal fun CupertinoLabelLayout(
    title: (@Composable () -> Unit)?,
    icon: @Composable () -> Unit,
    modifier: Modifier,
    style: CupertinoLabelStyle,
    spacing: Dp,
    iconAlignment: CupertinoLabelIconAlignment,
    clearIconSemantics: Boolean = true,
) {
    val showIcon = style != CupertinoLabelStyle.TitleOnly
    val iconOnly = style == CupertinoLabelStyle.IconOnly
    Layout(
        modifier = modifier,
        content = {
            if (showIcon) {
                Box(
                    modifier = Modifier
                        .layoutId(IconId)
                        .then(if (clearIconSemantics) Modifier.clearAndSetSemantics {} else Modifier),
                    propagateMinConstraints = true,
                ) { icon() }
            }
            if (title != null) {
                Box(
                    modifier = Modifier
                        .layoutId(TitleId)
                        // IconOnly keeps the title in the tree for accessibility but never draws it.
                        // Not using alpha = 0: transparent nodes are dropped by Android accessibility.
                        .then(if (iconOnly) Modifier.drawWithContent {} else Modifier),
                    propagateMinConstraints = true,
                ) { title() }
            }
        },
    ) { measurables, constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val iconPlaceable = measurables.firstOrNull { it.layoutId == IconId }?.measure(loose)
        val titleMeasurable = measurables.firstOrNull { it.layoutId == TitleId }

        if (iconOnly) {
            val iconW = iconPlaceable?.width ?: 0
            val iconH = iconPlaceable?.height ?: 0
            // The hidden title is confined to the icon bounds so its accessibility focus matches the icon.
            val hiddenTitle = titleMeasurable?.measure(Constraints(maxWidth = iconW, maxHeight = iconH))
            val width = constraints.constrainWidth(iconW)
            val height = constraints.constrainHeight(iconH)
            return@Layout layout(width, height) {
                val y = Alignment.CenterVertically.align(iconH, height)
                iconPlaceable?.placeRelative(0, y)
                hiddenTitle?.placeRelative(0, y)
            }
        }

        val gap = if (iconPlaceable != null && titleMeasurable != null) spacing.roundToPx() else 0
        val iconW = iconPlaceable?.width ?: 0
        val titleMaxWidth =
            if (loose.hasBoundedWidth) (loose.maxWidth - iconW - gap).coerceAtLeast(0) else Constraints.Infinity
        val titlePlaceable = titleMeasurable?.measure(loose.copy(maxWidth = titleMaxWidth))
        val offsets = labelVerticalOffsets(
            iconHeight = iconPlaceable?.height ?: 0,
            title = titlePlaceable,
            alignment = iconAlignment,
        )
        val width = constraints.constrainWidth(iconW + gap + (titlePlaceable?.width ?: 0))
        val height = constraints.constrainHeight(offsets.contentHeight)
        val shift = Alignment.CenterVertically.align(offsets.contentHeight, height)
        layout(width, height) {
            iconPlaceable?.placeRelative(0, offsets.iconTop + shift)
            titlePlaceable?.placeRelative(iconW + gap, offsets.titleTop + shift)
        }
    }
}

internal data class LabelVerticalOffsets(val iconTop: Int, val titleTop: Int, val contentHeight: Int)

private fun labelVerticalOffsets(
    iconHeight: Int,
    title: Placeable?,
    alignment: CupertinoLabelIconAlignment,
): LabelVerticalOffsets {
    val titleHeight = title?.height ?: 0
    val first = title?.get(FirstBaseline) ?: AlignmentLine.Unspecified
    val last = title?.get(LastBaseline) ?: AlignmentLine.Unspecified
    return computeLabelVerticalOffsets(iconHeight, titleHeight, first, last, alignment)
}

/**
 * Pure vertical layout of a label. With [CupertinoLabelIconAlignment.Center] it matches
 * `Row(verticalAlignment = CenterVertically)` pixel for pixel. With
 * [CupertinoLabelIconAlignment.FirstLine] the icon is centered on the first text line, whose box spans
 * from the top of the title to the first baseline plus the title's descent; for single-line titles
 * this is the same as centering.
 *
 * @param firstBaseline [AlignmentLine.Unspecified] when the title has none.
 */
internal fun computeLabelVerticalOffsets(
    iconHeight: Int,
    titleHeight: Int,
    firstBaseline: Int,
    lastBaseline: Int,
    alignment: CupertinoLabelIconAlignment,
): LabelVerticalOffsets {
    val unspecified = AlignmentLine.Unspecified
    val multiLine = firstBaseline != unspecified && lastBaseline != unspecified && lastBaseline > firstBaseline
    if (alignment == CupertinoLabelIconAlignment.Center || !multiLine) {
        val height = max(iconHeight, titleHeight)
        return LabelVerticalOffsets(
            iconTop = Alignment.CenterVertically.align(iconHeight, height),
            titleTop = Alignment.CenterVertically.align(titleHeight, height),
            contentHeight = height,
        )
    }
    val descent = titleHeight - lastBaseline
    val firstLineCenter = (firstBaseline + descent) / 2f
    val iconTop = (firstLineCenter - iconHeight / 2f).roundToInt()
    return if (iconTop >= 0) {
        LabelVerticalOffsets(iconTop, 0, max(titleHeight, iconTop + iconHeight))
    } else {
        LabelVerticalOffsets(0, -iconTop, max(iconHeight, titleHeight - iconTop))
    }
}
