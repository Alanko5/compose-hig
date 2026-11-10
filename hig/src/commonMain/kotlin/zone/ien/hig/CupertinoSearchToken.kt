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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.key
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.capsule.ContinuousCapsule
import zone.ien.hig.theme.CupertinoTheme

/**
 * A search token shown as a capsule chip before the text of a [CupertinoSearchTextField], the
 * equivalent of `UISearchToken` / the `tokens` of SwiftUI's `.searchable(text:tokens:)`.
 *
 * @param text label of the token. Also used for accessibility ("<text>, token").
 * @param icon optional leading icon of the chip.
 * @param key identity of the token, used to keep each chip's state when tokens are added or removed.
 * Defaults to [text]; supply your own model id when several tokens share the same label. It must be
 * immutable and implement `equals`/`hashCode` (a `String`, a number, a data class of those).
 */
@Immutable
@ExperimentalCupertinoApi
class CupertinoSearchToken(
    val text: String,
    val icon: ImageVector? = null,
    val key: Any = text,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CupertinoSearchToken) return false
        return text == other.text && icon == other.icon && key == other.key
    }

    override fun hashCode(): Int {
        var result = text.hashCode()
        result = 31 * result + (icon?.hashCode() ?: 0)
        result = 31 * result + key.hashCode()
        return result
    }

    override fun toString(): String = "CupertinoSearchToken(text=$text, key=$key)"
}

/**
 * Colors of the [CupertinoSearchToken] chips of a [CupertinoSearchTextField].
 *
 * @see CupertinoSearchTokenDefaults.colors
 */
@Immutable
@ExperimentalCupertinoApi
class CupertinoSearchTokenColors internal constructor(
    private val containerColor: Color,
    private val contentColor: Color,
    private val selectedContainerColor: Color,
    private val selectedContentColor: Color,
    private val disabledContainerColor: Color,
    private val disabledContentColor: Color,
) {
    internal fun containerColor(enabled: Boolean, selected: Boolean): Color =
        when {
            !enabled -> disabledContainerColor
            selected -> selectedContainerColor
            else -> containerColor
        }

    internal fun contentColor(enabled: Boolean, selected: Boolean): Color =
        when {
            !enabled -> disabledContentColor
            selected -> selectedContentColor
            else -> contentColor
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CupertinoSearchTokenColors) return false
        return containerColor == other.containerColor &&
            contentColor == other.contentColor &&
            selectedContainerColor == other.selectedContainerColor &&
            selectedContentColor == other.selectedContentColor &&
            disabledContainerColor == other.disabledContainerColor &&
            disabledContentColor == other.disabledContentColor
    }

    override fun hashCode(): Int {
        var result = containerColor.hashCode()
        result = 31 * result + contentColor.hashCode()
        result = 31 * result + selectedContainerColor.hashCode()
        result = 31 * result + selectedContentColor.hashCode()
        result = 31 * result + disabledContainerColor.hashCode()
        result = 31 * result + disabledContentColor.hashCode()
        return result
    }
}

/**
 * Defaults of the search token chips used by [CupertinoSearchTextField].
 */
@Immutable
@ExperimentalCupertinoApi
object CupertinoSearchTokenDefaults {
    /** Capsule shape of a token chip. */
    val shape: Shape
        @Composable
        @ReadOnlyComposable
        get() = ContinuousCapsule()

    /** Inner padding of a token chip. */
    val ContentPadding: PaddingValues = PaddingValues(horizontal = 8.dp, vertical = 3.dp)

    /** Size of the token icon. */
    val IconSize: Dp = 14.dp

    /** Default accessibility label of a token's click (select) action. */
    const val SelectActionLabel: String = "Select"

    /** Default accessibility description of [token]: "<text>, token". */
    fun contentDescription(token: CupertinoSearchToken): String = "${token.text}, token"

    /** Default label of the accessibility action that removes [token]: "Remove <text>". */
    fun removeActionLabel(token: CupertinoSearchToken): String = "Remove ${token.text}"

    /**
     * Creates [CupertinoSearchTokenColors]. By default a token is an accent-tinted capsule; the selected token
     * (the one the next backspace deletes) is a solid accent capsule with white content.
     */
    @Composable
    @ReadOnlyComposable
    fun colors(
        containerColor: Color = CupertinoTheme.colorScheme.accent.copy(alpha = .16f),
        contentColor: Color = CupertinoTheme.colorScheme.accent,
        selectedContainerColor: Color = CupertinoTheme.colorScheme.accent,
        selectedContentColor: Color = Color.White,
        disabledContainerColor: Color = CupertinoTheme.colorScheme.tertiarySystemFill,
        disabledContentColor: Color = CupertinoTheme.colorScheme.tertiaryLabel,
    ): CupertinoSearchTokenColors =
        CupertinoSearchTokenColors(
            containerColor = containerColor,
            contentColor = contentColor,
            selectedContainerColor = selectedContainerColor,
            selectedContentColor = selectedContentColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor,
        )

    /**
     * Default token chip: a tinted capsule with an optional icon and the token text.
     *
     * Interaction and accessibility are handled by [CupertinoSearchTextField]; this only draws the chip.
     */
    @Composable
    fun Token(
        token: CupertinoSearchToken,
        selected: Boolean,
        enabled: Boolean,
        colors: CupertinoSearchTokenColors = colors(),
    ) {
        val contentColor = colors.contentColor(enabled, selected)
        Row(
            modifier = Modifier
                .background(colors.containerColor(enabled, selected), shape)
                .padding(ContentPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            token.icon?.let {
                CupertinoIcon(
                    imageVector = it,
                    contentDescription = null,
                    modifier = Modifier.size(IconSize),
                    tint = contentColor,
                )
            }
            CupertinoText(
                text = token.text,
                color = contentColor,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

/** Accessibility strings of the token strip. */
@OptIn(ExperimentalCupertinoApi::class)
internal class SearchTokenLabels(
    val contentDescription: (CupertinoSearchToken) -> String,
    val removeAction: (CupertinoSearchToken) -> String,
    val selectAction: String,
)

/** Fraction of the field's content width the token strip may take before it scrolls. */
private const val MaxTokenStripFraction = .6f

/**
 * Horizontally scrollable strip of token chips rendered before the text cursor.
 */
@OptIn(ExperimentalCupertinoApi::class)
@Composable
internal fun CupertinoSearchTokenStrip(
    tokens: List<CupertinoSearchToken>,
    selectedTokenIndex: Int?,
    editable: Boolean,
    onSelect: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    tokenContent: @Composable (token: CupertinoSearchToken, selected: Boolean) -> Unit,
    labels: SearchTokenLabels,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    // keep the newest token (and the cursor right after it) visible
    LaunchedEffect(tokens.size) {
        withFrameNanos { }
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Row(
        modifier = modifier
            .layout { measurable, constraints ->
                val maxWidth = if (constraints.hasBoundedWidth) {
                    (constraints.maxWidth * MaxTokenStripFraction).toInt()
                } else {
                    constraints.maxWidth
                }
                val placeable = measurable.measure(constraints.copy(minWidth = 0, maxWidth = maxWidth))
                layout(placeable.width, placeable.height) { placeable.placeRelative(0, 0) }
            }
            .horizontalScroll(scrollState),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        tokens.forEachIndexed { index, token ->
            key(token.key) {
                val selected = index == selectedTokenIndex
                // Each token is its own merging boundary so it is announced separately instead of being
                // merged into the text field node.
                Box(
                    Modifier
                        .semantics(mergeDescendants = true) {
                            contentDescription = labels.contentDescription(token)
                            this.selected = selected
                            if (editable) {
                                customActions = listOf(
                                    CustomAccessibilityAction(labels.removeAction(token)) {
                                        onRemove(index)
                                        true
                                    },
                                )
                            }
                        }
                        .clickable(
                            // a read-only or disabled field shows its tokens but does not let the user change them
                            enabled = editable,
                            onClickLabel = labels.selectAction,
                            interactionSource = null,
                            indication = null,
                        ) { onSelect(index) },
                ) {
                    Box(Modifier.clearAndSetSemantics { }) {
                        tokenContent(token, selected)
                    }
                }
            }
        }
    }
}
