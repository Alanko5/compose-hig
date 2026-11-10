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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import zone.ien.hig.CupertinoDisclosureGroupColors
import zone.ien.hig.CupertinoDisclosureGroupDefaults
import zone.ien.hig.CupertinoDisclosureGroupHeader
import zone.ien.hig.CupertinoHorizontalDivider
import zone.ien.hig.ExperimentalCupertinoApi

private object DisclosureGroupHeaderContentType

/**
 * Disclosure group row for [CupertinoSection]. Children are separated by dividers and
 * indented by [CupertinoDisclosureGroupDefaults.ContentIndent].
 *
 * Collapsed content leaves composition, so state remembered inside it is reset; hoist state that must survive collapsing.
 *
 * @param icon leading content, e.g. a [CupertinoLinkIcon].
 * @param content child rows, e.g. [SectionItem], [SectionLink] or another [SectionDisclosureGroup].
 */
@ExperimentalCupertinoApi
@Composable
fun SectionScope.SectionDisclosureGroup(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: @Composable () -> Unit = {},
    colors: CupertinoDisclosureGroupColors = CupertinoDisclosureGroupDefaults.colors(),
    interactionSource: MutableInteractionSource? = null,
    title: @Composable () -> Unit,
    content: @Composable SectionScope.() -> Unit,
) {
    val showDivider = sectionShowsDividers(LocalSectionStyle.current)
    Column(modifier.fillMaxWidth()) {
        CupertinoDisclosureGroupHeader(
            expanded = expanded,
            onExpandedChange = onExpandedChange,
            enabled = enabled,
            colors = colors,
            interactionSource = interactionSource,
            minHeight = CupertinoSectionTokens.MinHeight,
            contentPadding = CupertinoSectionDefaults.PaddingValues,
            modifier = Modifier.fillMaxWidth(),
        ) {
            icon()
            title()
        }
        AnimatedVisibility(
            visible = expanded,
            enter = CupertinoDisclosureGroupDefaults.EnterTransition,
            exit = CupertinoDisclosureGroupDefaults.ExitTransition,
        ) {
            Column {
                if (showDivider) {
                    CupertinoHorizontalDivider(
                        modifier = Modifier.padding(start = CupertinoSectionDefaults.DividerPadding),
                    )
                }
                SectionItemsLayout(
                    showDivider = showDivider,
                    dividerPadding = PaddingValues(
                        start = CupertinoSectionDefaults.DividerPadding +
                            CupertinoDisclosureGroupDefaults.ContentIndent,
                    ),
                    startIndent = CupertinoDisclosureGroupDefaults.ContentIndent,
                ) {
                    content(SectionScopeImpl)
                }
            }
        }
    }
}

/**
 * Disclosure group for [androidx.compose.foundation.lazy.LazyListScope.section].
 *
 * The header is one lazy item; while [expanded], every item added in [content] becomes its own
 * lazy item, indented by [CupertinoDisclosureGroupDefaults.ContentIndent]. Groups can be nested.
 *
 * Children appear/disappear without a height animation (lazy items); the chevron still animates.
 */
@ExperimentalCupertinoApi
fun LazySectionScope.disclosureGroup(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    key: Any? = null,
    enabled: Boolean = true,
    icon: (@Composable () -> Unit)? = null,
    dividerPadding: Dp =
        if (icon != null) {
            CupertinoSectionDefaults.DividerPaddingWithIcon
        } else {
            CupertinoSectionDefaults.DividerPadding
        },
    title: @Composable () -> Unit,
    content: LazySectionScope.() -> Unit,
) {
    item(
        key = key,
        contentType = DisclosureGroupHeaderContentType,
        dividerPadding = dividerPadding,
    ) { padding ->
        CupertinoDisclosureGroupHeader(
            expanded = expanded,
            onExpandedChange = onExpandedChange,
            enabled = enabled,
            colors = CupertinoDisclosureGroupDefaults.colors(),
            interactionSource = null,
            minHeight = CupertinoSectionTokens.MinHeight,
            contentPadding = padding,
            modifier = Modifier.fillMaxWidth(),
        ) {
            icon?.invoke()
            title()
        }
    }
    if (expanded) {
        IndentedLazySectionScope(this, CupertinoDisclosureGroupDefaults.ContentIndent).content()
    }
}

/** Forwards items to [parent], adding [indent] to their start padding and divider padding. */
internal class IndentedLazySectionScope(
    private val parent: LazySectionScope,
    private val indent: Dp,
) : LazySectionScope {
    override fun item(
        key: Any?,
        contentType: Any?,
        dividerPadding: Dp,
        content: @Composable (padding: PaddingValues) -> Unit,
    ) {
        parent.item(
            key = key,
            contentType = contentType,
            dividerPadding = dividerPadding + indent,
        ) { padding ->
            content(IndentedPaddingValues(padding, indent))
        }
    }

    override fun sidebarItem(
        selected: Boolean,
        onClick: () -> Unit,
        key: Any?,
        contentType: Any?,
        enabled: Boolean,
        leadingContent: @Composable () -> Unit,
        trailingContent: @Composable () -> Unit,
        title: @Composable () -> Unit,
    ) {
        parent.item(
            key = key,
            contentType = contentType,
            dividerPadding = 0.dp,
        ) {
            SidebarItemRow(
                modifier = Modifier.fillMaxWidth().padding(start = indent),
                selected = selected,
                onClick = onClick,
                enabled = enabled,
                leadingContent = leadingContent,
                trailingContent = trailingContent,
                title = title,
            )
        }
    }
}

/** [base] with [indent] added on the start side (left in LTR, right in RTL). */
internal class IndentedPaddingValues(
    private val base: PaddingValues,
    private val indent: Dp,
) : PaddingValues {
    override fun calculateLeftPadding(layoutDirection: LayoutDirection): Dp =
        base.calculateLeftPadding(layoutDirection) + if (layoutDirection == LayoutDirection.Ltr) indent else 0.dp

    override fun calculateRightPadding(layoutDirection: LayoutDirection): Dp =
        base.calculateRightPadding(layoutDirection) + if (layoutDirection == LayoutDirection.Rtl) indent else 0.dp

    override fun calculateTopPadding(): Dp = base.calculateTopPadding()

    override fun calculateBottomPadding(): Dp = base.calculateBottomPadding()
}
