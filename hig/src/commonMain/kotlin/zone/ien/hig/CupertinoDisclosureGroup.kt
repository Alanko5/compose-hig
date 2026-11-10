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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import zone.ien.hig.icons.CupertinoIcons
import zone.ien.hig.icons.outlined.ChevronForward
import zone.ien.hig.section.CupertinoSectionTokens
import zone.ien.hig.theme.CupertinoTheme

/**
 * iOS-style disclosure group: a tappable label row with a chevron that rotates down while
 * [content] is shown below it.
 *
 * For rows inside a section use `SectionScope.SectionDisclosureGroup` or
 * `LazySectionScope.disclosureGroup` from `zone.ien.hig.section`.
 *
 * Collapsed content leaves composition, so state remembered inside it is reset; hoist state that must survive collapsing.
 *
 * @param expanded whether [content] is shown.
 * @param onExpandedChange called with the requested state when the row is tapped or an
 * accessibility expand/collapse action runs.
 * @param enabled when false the row ignores input and is drawn dimmed.
 * @param label row content, laid out in a [Row]; the chevron is appended at the end.
 * @param content children shown when expanded, indented by [CupertinoDisclosureGroupDefaults.ContentIndent].
 */
@ExperimentalCupertinoApi
@Composable
fun CupertinoDisclosureGroup(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: CupertinoDisclosureGroupColors = CupertinoDisclosureGroupDefaults.colors(),
    interactionSource: MutableInteractionSource? = null,
    label: @Composable RowScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier) {
        CupertinoDisclosureGroupHeader(
            expanded = expanded,
            onExpandedChange = onExpandedChange,
            enabled = enabled,
            colors = colors,
            interactionSource = interactionSource,
            minHeight = CupertinoDisclosureGroupDefaults.MinHeight,
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.fillMaxWidth(),
            label = label,
        )
        AnimatedVisibility(
            visible = expanded,
            enter = CupertinoDisclosureGroupDefaults.EnterTransition,
            exit = CupertinoDisclosureGroupDefaults.ExitTransition,
        ) {
            Column(
                modifier = Modifier.padding(start = CupertinoDisclosureGroupDefaults.ContentIndent),
                content = content,
            )
        }
    }
}

/** Label and chevron colors of a disclosure group, for enabled and disabled states. */
@Immutable
@ExperimentalCupertinoApi
class CupertinoDisclosureGroupColors internal constructor(
    private val labelColor: Color,
    private val disabledLabelColor: Color,
    private val chevronColor: Color,
    private val disabledChevronColor: Color,
) {
    fun labelColor(enabled: Boolean): Color = if (enabled) labelColor else disabledLabelColor

    fun chevronColor(enabled: Boolean): Color = if (enabled) chevronColor else disabledChevronColor

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CupertinoDisclosureGroupColors) return false
        return labelColor == other.labelColor &&
            disabledLabelColor == other.disabledLabelColor &&
            chevronColor == other.chevronColor &&
            disabledChevronColor == other.disabledChevronColor
    }

    override fun hashCode(): Int {
        var result = labelColor.hashCode()
        result = 31 * result + disabledLabelColor.hashCode()
        result = 31 * result + chevronColor.hashCode()
        result = 31 * result + disabledChevronColor.hashCode()
        return result
    }
}

@ExperimentalCupertinoApi
object CupertinoDisclosureGroupDefaults {
    /** Minimum height of the header row (matches iOS list rows). */
    val MinHeight: Dp = 44.dp

    /** Start indent applied to children. */
    val ContentIndent: Dp = 20.dp

    /** Size of the trailing chevron icon. */
    val ChevronSize: Dp = 14.dp

    /** State description announced for an expanded group. */
    const val ExpandedStateDescription: String = "Expanded"

    /** State description announced for a collapsed group. */
    const val CollapsedStateDescription: String = "Collapsed"

    /** Default transition used when content is revealed. */
    val EnterTransition: EnterTransition =
        expandVertically(
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            expandFrom = Alignment.Top,
        ) +
            fadeIn(spring(stiffness = Spring.StiffnessMediumLow))

    /** Default transition used when content is hidden. */
    val ExitTransition: ExitTransition =
        shrinkVertically(
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            shrinkTowards = Alignment.Top,
        ) +
            fadeOut(spring(stiffness = Spring.StiffnessMediumLow))

    /** Creates [CupertinoDisclosureGroupColors] with the given label and chevron colors. */
    @Composable
    @ReadOnlyComposable
    fun colors(
        labelColor: Color = CupertinoTheme.colorScheme.label,
        disabledLabelColor: Color = CupertinoTheme.colorScheme.secondaryLabel,
        chevronColor: Color = CupertinoTheme.colorScheme.accent,
        disabledChevronColor: Color = CupertinoTheme.colorScheme.tertiaryLabel,
    ): CupertinoDisclosureGroupColors =
        CupertinoDisclosureGroupColors(
            labelColor = labelColor,
            disabledLabelColor = disabledLabelColor,
            chevronColor = chevronColor,
            disabledChevronColor = disabledChevronColor,
        )
}

/** Chevron angle in degrees for [ChevronForward] (which always points right). */
internal fun disclosureChevronRotation(expanded: Boolean, layoutDirection: LayoutDirection): Float =
    when {
        expanded -> 90f
        layoutDirection == LayoutDirection.Ltr -> 0f
        else -> 180f
    }

@OptIn(ExperimentalCupertinoApi::class)
@Composable
internal fun CupertinoDisclosureGroupHeader(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    enabled: Boolean,
    colors: CupertinoDisclosureGroupColors,
    interactionSource: MutableInteractionSource?,
    minHeight: Dp,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    label: @Composable RowScope.() -> Unit,
) {
    val updatedOnExpandedChange by rememberUpdatedState(onExpandedChange)
    val rotation by animateFloatAsState(
        targetValue = disclosureChevronRotation(expanded, LocalLayoutDirection.current),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "DisclosureChevronRotation",
    )

    Row(
        modifier =
            modifier
                .heightIn(min = minHeight)
                .clickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    enabled = enabled,
                    role = Role.Button,
                    onClick = { updatedOnExpandedChange(!expanded) },
                )
                .semantics(mergeDescendants = true) {
                    stateDescription =
                        if (expanded) {
                            CupertinoDisclosureGroupDefaults.ExpandedStateDescription
                        } else {
                            CupertinoDisclosureGroupDefaults.CollapsedStateDescription
                        }
                    if (enabled) {
                        if (expanded) {
                            collapse { updatedOnExpandedChange(false); true }
                        } else {
                            expand { updatedOnExpandedChange(true); true }
                        }
                    }
                }
                .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CupertinoSectionTokens.InlinePadding),
    ) {
        CompositionLocalProvider(LocalContentColor provides colors.labelColor(enabled)) {
            ProvideTextStyle(CupertinoTheme.typography.body) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(CupertinoSectionTokens.HorizontalPadding),
                    content = label,
                )
            }
        }
        CupertinoIcon(
            imageVector = CupertinoIcons.Default.ChevronForward,
            contentDescription = null,
            tint = colors.chevronColor(enabled),
            modifier =
                Modifier
                    .size(CupertinoDisclosureGroupDefaults.ChevronSize)
                    .graphicsLayer { rotationZ = rotation },
        )
    }
}
