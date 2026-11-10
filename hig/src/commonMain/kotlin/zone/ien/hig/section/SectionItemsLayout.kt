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

package zone.ien.hig.section

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastSumBy
import zone.ien.hig.CupertinoHorizontalDivider
import zone.ien.hig.theme.CupertinoTheme

/** Whether rows of a section in [style] are separated by dividers. */
@Composable
internal fun sectionShowsDividers(style: SectionStyle): Boolean =
    style != SectionStyle.Sidebar &&
        CupertinoTheme.colorScheme.separator.let { it.isSpecified && it != Color.Transparent }

private enum class SectionItemsSlot { Items, Dividers }

/**
 * Stacks every child emitted by [content] vertically and puts a divider between neighbours.
 *
 * @param startIndent children are narrowed by this amount and shifted towards the end.
 * Used for disclosure group children.
 * Dividers are not shifted by [startIndent]; pass an indented [dividerPadding] if they should align with the items.
 */
@Composable
internal fun SectionItemsLayout(
    showDivider: Boolean,
    dividerPadding: PaddingValues,
    modifier: Modifier = Modifier,
    startIndent: Dp = 0.dp,
    content: @Composable () -> Unit,
) {
    SubcomposeLayout(modifier) { constraints ->
        val measurables = subcompose(SectionItemsSlot.Items, content)

        val dividers =
            subcompose(SectionItemsSlot.Dividers) {
                if (showDivider) {
                    repeat((measurables.size - 1).coerceAtLeast(0)) {
                        CupertinoHorizontalDivider(modifier = Modifier.padding(dividerPadding))
                    }
                }
            }.fastMap { it.measure(constraints) }

        val indentPx = startIndent.roundToPx()
        val itemConstraints = if (indentPx == 0) constraints else constraints.offset(horizontal = -indentPx)
        val placeables = measurables.fastMap { it.measure(itemConstraints) }

        layout(
            width = constraints.maxWidth,
            height = (placeables + dividers).fastSumBy { it.height },
        ) {
            var h = 0
            placeables.fastForEachIndexed { i, p ->
                // indentPx == 0 keeps the original CupertinoSection placement exactly.
                if (indentPx == 0) p.place(0, h) else p.placeRelative(indentPx, h)
                h += p.height

                if (showDivider && i < placeables.lastIndex) {
                    dividers[i].place(0, h)
                    h += dividers[i].height
                }
            }
        }
    }
}
