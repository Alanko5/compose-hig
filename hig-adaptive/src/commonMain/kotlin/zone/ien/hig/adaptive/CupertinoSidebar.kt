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

package zone.ien.hig.adaptive

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.recalculateWindowInsets
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import zone.ien.hig.CupertinoIcon
import zone.ien.hig.CupertinoLiquidIconButton
import zone.ien.hig.CupertinoScaffoldDefaults
import zone.ien.hig.ExperimentalCupertinoApi
import zone.ien.hig.LocalCupertinoRightSystemArea
import zone.ien.hig.section.LazySectionScope
import zone.ien.hig.theme.CupertinoTheme
import zone.ien.hig.utils.cupertinoSidebarMaterial
import zone.ien.hig.utils.rememberDefaultBackdrop
import kotlin.math.roundToInt

/**
 * Requested visibility of the sidebar column on expanded width, the equivalent of SwiftUI's
 * `NavigationSplitViewVisibility`.
 *
 * On compact width the sidebar and the detail never share the screen; there [CupertinoSidebarState.compactColumn]
 * decides which column is shown and the visibility is ignored.
 */
@ExperimentalCupertinoApi
enum class CupertinoSidebarVisibility {
    /** Let the scaffold decide. Currently the sidebar is shown whenever there is room for it. */
    Automatic,

    /** The sidebar is shown next to the detail. */
    Shown,

    /** Only the detail is shown; the sidebar can be revealed with [CupertinoSidebarToggleButton]. */
    Hidden,
}

/** Width class the [CupertinoSidebarScaffold] resolved from its own incoming constraints. */
@ExperimentalCupertinoApi
enum class CupertinoSidebarWidthClass {
    /** Phone-like width: the sidebar becomes the root screen and the detail is pushed on top of it. */
    Compact,

    /** Tablet, foldable or desktop width: the sidebar is a persistent leading column. */
    Expanded,
}

/** The column shown on compact width, the equivalent of SwiftUI's `NavigationSplitViewColumn`. */
@ExperimentalCupertinoApi
enum class CupertinoSidebarColumn {
    /** The sidebar list is the visible root screen. */
    Sidebar,

    /** The detail is pushed on top of the sidebar list. */
    Detail,
}

/**
 * State of a [CupertinoSidebarScaffold].
 *
 * Holds the requested expanded [visibility], the column shown on compact width ([compactColumn]) and the
 * [widthClass] last resolved by the scaffold. Create it with [rememberCupertinoSidebarState] so it survives
 * configuration changes.
 *
 * @param initialVisibility initial [visibility].
 * @param initialCompactColumn initial [compactColumn].
 */
@ExperimentalCupertinoApi
@Stable
class CupertinoSidebarState(
    initialVisibility: CupertinoSidebarVisibility = CupertinoSidebarVisibility.Automatic,
    initialCompactColumn: CupertinoSidebarColumn = CupertinoSidebarColumn.Sidebar,
) {
    /** Requested sidebar visibility on expanded width. */
    var visibility: CupertinoSidebarVisibility by mutableStateOf(initialVisibility)

    /** Column shown on compact width. */
    var compactColumn: CupertinoSidebarColumn by mutableStateOf(initialCompactColumn)

    /** Width class resolved by the scaffold during its last layout. */
    var widthClass: CupertinoSidebarWidthClass by mutableStateOf(CupertinoSidebarWidthClass.Expanded)
        internal set

    /** Whether the sidebar column is currently (or about to be) visible. */
    val isSidebarVisible: Boolean
        get() = resolveCupertinoSidebarPanes(widthClass, visibility, compactColumn).sidebar

    /** Whether the detail column is currently (or about to be) visible. */
    val isDetailVisible: Boolean
        get() = resolveCupertinoSidebarPanes(widthClass, visibility, compactColumn).detail

    /** Shows or hides the sidebar on expanded width. Does nothing on compact width. */
    fun toggleSidebar() {
        visibility = toggledCupertinoSidebarVisibility(widthClass, resolvedVisibility())
    }

    /** Pushes the detail on compact width. On expanded width the detail is always visible. */
    fun showDetail() {
        compactColumn = CupertinoSidebarColumn.Detail
    }

    /** Pops back to the sidebar on compact width and reveals a hidden sidebar on expanded width. */
    fun showSidebar() {
        compactColumn = CupertinoSidebarColumn.Sidebar
        if (visibility == CupertinoSidebarVisibility.Hidden) visibility = CupertinoSidebarVisibility.Shown
    }

    /**
     * Performs the scaffold's back navigation: on compact width with the detail pushed it pops back to the
     * sidebar. Returns `false` when there is nothing to go back to, so the caller can let the system handle it.
     */
    fun navigateBack(): Boolean = when (cupertinoSidebarBackAction(widthClass, compactColumn)) {
        CupertinoSidebarBackAction.ShowSidebar -> {
            compactColumn = CupertinoSidebarColumn.Sidebar
            true
        }
        CupertinoSidebarBackAction.None -> false
    }

    private fun resolvedVisibility() =
        if (isSidebarVisible) CupertinoSidebarVisibility.Shown else CupertinoSidebarVisibility.Hidden

    companion object {
        /** [Saver] keeping [visibility] and [compactColumn]. */
        val Saver: Saver<CupertinoSidebarState, List<Int>> = Saver(
            save = { listOf(it.visibility.ordinal, it.compactColumn.ordinal) },
            restore = {
                CupertinoSidebarState(
                    CupertinoSidebarVisibility.entries[it[0]],
                    CupertinoSidebarColumn.entries[it[1]],
                )
            },
        )
    }
}

/**
 * Creates and remembers a [CupertinoSidebarState].
 *
 * @param initialVisibility initial sidebar visibility on expanded width.
 * @param initialCompactColumn column shown first on compact width.
 */
@ExperimentalCupertinoApi
@Composable
fun rememberCupertinoSidebarState(
    initialVisibility: CupertinoSidebarVisibility = CupertinoSidebarVisibility.Automatic,
    initialCompactColumn: CupertinoSidebarColumn = CupertinoSidebarColumn.Sidebar,
): CupertinoSidebarState = rememberSaveable(saver = CupertinoSidebarState.Saver) {
    CupertinoSidebarState(initialVisibility, initialCompactColumn)
}

/**
 * Receiver of the `sidebar` slot of [CupertinoSidebarScaffold]. It is a regular [LazyListScope], so the sidebar is
 * built from `section(style = SectionStyle.Sidebar) { ... }` blocks, plus [destination] which binds a
 * [LazySectionScope.sidebarItem] row to the scaffold's single selection.
 */
@ExperimentalCupertinoApi
interface CupertinoSidebarScope : LazyListScope {
    /**
     * Adds a sidebar row that is selected when [key] equals the scaffold's `selectedKey`. Clicking it calls the
     * scaffold's `onSelect` with [key] and pushes the detail on compact width.
     *
     * Rendering is delegated to [LazySectionScope.sidebarItem], so the row uses the regular sidebar selected style
     * and exposes the selected state to accessibility services.
     *
     * @param key stable key of the destination; also used as the lazy item key.
     * @param enabled whether the row can be selected.
     * @param leadingContent icon shown before the title.
     * @param trailingContent content shown at the end, for example an unread count.
     * @param title row title.
     */
    fun LazySectionScope.destination(
        key: Any,
        enabled: Boolean = true,
        leadingContent: @Composable () -> Unit = {},
        trailingContent: @Composable () -> Unit = {},
        title: @Composable () -> Unit,
    )
}

@OptIn(ExperimentalCupertinoApi::class)
private class CupertinoSidebarScopeImpl(
    scope: LazyListScope,
    private val selectedKey: Any?,
    private val onSelect: (Any) -> Unit,
) : CupertinoSidebarScope, LazyListScope by scope {
    override fun LazySectionScope.destination(
        key: Any,
        enabled: Boolean,
        leadingContent: @Composable () -> Unit,
        trailingContent: @Composable () -> Unit,
        title: @Composable () -> Unit,
    ) = sidebarItem(
        selected = key == selectedKey,
        onClick = { onSelect(key) },
        key = key,
        enabled = enabled,
        leadingContent = leadingContent,
        trailingContent = trailingContent,
        title = title,
    )
}

/**
 * Colors of a [CupertinoSidebarScaffold].
 *
 * @see CupertinoSidebarDefaults.colors
 */
@ExperimentalCupertinoApi
@Immutable
class CupertinoSidebarColors internal constructor(
    /** Background of the sidebar when it is the root screen on compact width, and below the glass material. */
    val containerColor: Color,
    /** Hairline between the sidebar column and the detail on expanded width. */
    val separatorColor: Color,
    /** Background behind the detail column. */
    val detailContainerColor: Color,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CupertinoSidebarColors) return false
        return containerColor == other.containerColor &&
            separatorColor == other.separatorColor &&
            detailContainerColor == other.detailContainerColor
    }

    override fun hashCode(): Int {
        var result = containerColor.hashCode()
        result = 31 * result + separatorColor.hashCode()
        result = 31 * result + detailContainerColor.hashCode()
        return result
    }
}

/** Default values used by [CupertinoSidebarScaffold] and [CupertinoSidebarToggleButton]. */
@ExperimentalCupertinoApi
object CupertinoSidebarDefaults {
    /** Width of the sidebar column on expanded width. */
    val SidebarWidth: Dp = 320.dp

    /** Minimum scaffold width at which the sidebar becomes a persistent column. */
    val ExpandedBreakpoint: Dp = 600.dp

    /**
     * Accessibility pane title of the sidebar group. It is English; pass a localized string to
     * [CupertinoSidebarScaffold]'s `sidebarPaneTitle`.
     */
    const val PaneTitle: String = "Sidebar"

    /** Padding around the sidebar list. */
    val ContentPadding: PaddingValues = PaddingValues(horizontal = 8.dp, vertical = 8.dp)

    /** Toolbar icon (SF Symbol `sidebar.leading`). Mirror it in RTL; [CupertinoSidebarToggleButton] does so. */
    val ToggleIcon: ImageVector by lazy { sidebarLeadingIcon() }

    /**
     * Creates [CupertinoSidebarColors].
     *
     * @param containerColor background of the sidebar list.
     * @param separatorColor hairline between the columns.
     * @param detailContainerColor background behind the detail column.
     */
    @Composable
    @ReadOnlyComposable
    fun colors(
        containerColor: Color = CupertinoTheme.colorScheme.systemBackground,
        separatorColor: Color = CupertinoTheme.colorScheme.separator,
        detailContainerColor: Color = CupertinoTheme.colorScheme.systemBackground,
    ): CupertinoSidebarColors = CupertinoSidebarColors(containerColor, separatorColor, detailContainerColor)
}

/**
 * A sidebar + detail scaffold, the equivalent of SwiftUI's two-column `NavigationSplitView` and of a
 * `UISplitViewController` with a sidebar.
 *
 * The scaffold resolves a [CupertinoSidebarWidthClass] from its own width (not the window), so it also works
 * inside panes and multi-window:
 * - **Expanded** (width >= [expandedBreakpoint]): the sidebar is a persistent leading column with the glass
 *   sidebar material. [CupertinoSidebarToggleButton] hides and shows it with a spring animation and the detail
 *   resizes to fill the freed space.
 * - **Compact**: like `NavigationSplitView` on iPhone the scaffold collapses into a navigation stack. The sidebar
 *   list is the root screen; selecting a [CupertinoSidebarScope.destination] pushes the detail with a slide,
 *   and system back (Android back gesture, predictive back) or [CupertinoSidebarState.navigateBack] pops it.
 *   An overlay sidebar was deliberately not chosen: it hides the list behind a button on the screen where it is
 *   the primary navigation, and back/predictive back map naturally onto the pushed detail.
 *
 * The sidebar sits on the leading side, i.e. physically on the right in RTL layouts. Both columns stay composed
 * across width changes, so their remembered state survives rotation or unfolding.
 *
 * @param sidebar sidebar list content, built from `section(style = SectionStyle.Sidebar) { destination(...) }`.
 * @param detail detail column content. It usually contains its own top bar with [CupertinoSidebarToggleButton]
 * (expanded) or a back button calling [CupertinoSidebarState.showSidebar] (compact).
 * @param modifier modifier of the scaffold.
 * @param state scaffold state.
 * @param selectedKey key of the selected [CupertinoSidebarScope.destination], or `null`.
 * @param onSelect called with the key of a clicked destination; the detail is pushed afterwards on compact width.
 * @param sidebarTopBar content placed above the sidebar list, for example a `CupertinoTopAppBar` with a title. When
 * it is provided it owns the top window inset, as `CupertinoTopAppBar` does; when it is `null` the list itself is
 * padded by the top of [contentWindowInsets] so its first row is not drawn under the status bar.
 * @param backdrop backdrop the glass sidebar material samples. Defaults to a plain background layer.
 * @param colors scaffold colors.
 * @param sidebarWidth width of the sidebar column on expanded width.
 * @param expandedBreakpoint minimum width for the expanded layout.
 * @param sidebarListState scroll state of the sidebar list.
 * @param contentPadding padding of the sidebar list.
 * @param contentWindowInsets insets applied to the sidebar list; only the sides the sidebar touches are used.
 * @param sidebarPaneTitle accessibility pane title of the sidebar group. The default is English; pass a localized
 * string.
 */
@ExperimentalCupertinoApi
@Composable
fun CupertinoSidebarScaffold(
    sidebar: CupertinoSidebarScope.() -> Unit,
    detail: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    state: CupertinoSidebarState = rememberCupertinoSidebarState(),
    selectedKey: Any? = null,
    onSelect: (Any) -> Unit = {},
    sidebarTopBar: (@Composable () -> Unit)? = null,
    backdrop: LayerBackdrop? = null,
    colors: CupertinoSidebarColors = CupertinoSidebarDefaults.colors(),
    sidebarWidth: Dp = CupertinoSidebarDefaults.SidebarWidth,
    expandedBreakpoint: Dp = CupertinoSidebarDefaults.ExpandedBreakpoint,
    sidebarListState: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = CupertinoSidebarDefaults.ContentPadding,
    contentWindowInsets: WindowInsets = CupertinoScaffoldDefaults.contentWindowInsets,
    sidebarPaneTitle: String = CupertinoSidebarDefaults.PaneTitle,
) {
    BoxWithConstraints(modifier) {
        val widthClass = cupertinoSidebarWidthClass(maxWidth, expandedBreakpoint)
        SideEffect { state.widthClass = widthClass }
        // Provided so that a [CupertinoSidebarToggleButton] in the detail renders the right variant in the very first
        // frame, before the SideEffect above has updated state.widthClass.
        CompositionLocalProvider(LocalCupertinoSidebarWidthClass provides widthClass) {
            CupertinoSidebarLayout(
                widthClass = widthClass,
                sidebar = sidebar,
                detail = detail,
                state = state,
                selectedKey = selectedKey,
                onSelect = onSelect,
                sidebarTopBar = sidebarTopBar,
                backdrop = backdrop,
                colors = colors,
                sidebarWidth = sidebarWidth,
                sidebarListState = sidebarListState,
                contentPadding = contentPadding,
                contentWindowInsets = contentWindowInsets,
                sidebarPaneTitle = sidebarPaneTitle,
            )
        }
    }
}

@OptIn(ExperimentalCupertinoApi::class)
@Composable
private fun CupertinoSidebarLayout(
    widthClass: CupertinoSidebarWidthClass,
    sidebar: CupertinoSidebarScope.() -> Unit,
    detail: @Composable () -> Unit,
    state: CupertinoSidebarState,
    selectedKey: Any?,
    onSelect: (Any) -> Unit,
    sidebarTopBar: (@Composable () -> Unit)?,
    backdrop: LayerBackdrop?,
    colors: CupertinoSidebarColors,
    sidebarWidth: Dp,
    sidebarListState: LazyListState,
    contentPadding: PaddingValues,
    contentWindowInsets: WindowInsets,
    sidebarPaneTitle: String,
) {
    val panes = resolveCupertinoSidebarPanes(widthClass, state.visibility, state.compactColumn)
    // Keyed on the width class: a size-class change (rotation, unfolding) re-lays out without animating, like
    // UISplitViewController does; only toggles and pushes within one width class animate.
    val sidebarFraction by key(widthClass) {
        animateFloatAsState(
            targetValue = if (panes.sidebar) 1f else 0f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
            label = "CupertinoSidebarFraction",
        )
    }
    val detailFraction by key(widthClass) {
        animateFloatAsState(
            targetValue = if (panes.detail) 1f else 0f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
            label = "CupertinoDetailFraction",
        )
    }

    // Only completion is handled: predictive back progress is not yet reflected in the slide.
    val backState = rememberNavigationEventState(NavigationEventInfo.None)
    NavigationBackHandler(
        state = backState,
        isBackEnabled = cupertinoSidebarBackAction(widthClass, state.compactColumn) != CupertinoSidebarBackAction.None,
        onBackCompleted = { state.navigateBack() },
    )

    val layoutDirection = LocalLayoutDirection.current
    val expanded = widthClass == CupertinoSidebarWidthClass.Expanded
    // Like AdaptiveListDetailPane: in two columns only the physically right column owns the right system area.
    val sidebarOnRight = layoutDirection == LayoutDirection.Rtl
    val parentRightArea = LocalCupertinoRightSystemArea.current
    val fallbackBackdrop = rememberDefaultBackdrop()

    Layout(
        modifier = Modifier.fillMaxSize(),
        content = {
            CompositionLocalProvider(
                LocalCupertinoRightSystemArea provides (parentRightArea && (!expanded || sidebarOnRight)),
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .recalculateWindowInsets()
                        .semantics {
                            paneTitle = sidebarPaneTitle
                            isTraversalGroup = true
                        },
                ) {
                    if (expanded) {
                        if (backdrop == null) {
                            Box(Modifier.fillMaxSize().layerBackdrop(fallbackBackdrop).background(colors.containerColor))
                        }
                        Box(Modifier.fillMaxSize().cupertinoSidebarMaterial(backdrop ?: fallbackBackdrop))
                    } else {
                        Box(Modifier.fillMaxSize().background(colors.containerColor))
                    }
                    val insets = contentWindowInsets.only(
                        cupertinoSidebarListInsetSides(expanded = expanded, hasTopBar = sidebarTopBar != null),
                    )
                    val insetPadding = insets.asPaddingValues()
                    Column(Modifier.fillMaxSize()) {
                        sidebarTopBar?.invoke()
                        LazyColumn(
                            state = sidebarListState,
                            modifier = Modifier.fillMaxWidth().weight(1f).selectableGroup(),
                            contentPadding = PaddingValues(
                                start = contentPadding.calculateStartPadding(layoutDirection) +
                                    insetPadding.calculateStartPadding(layoutDirection),
                                end = contentPadding.calculateEndPadding(layoutDirection) +
                                    insetPadding.calculateEndPadding(layoutDirection),
                                top = contentPadding.calculateTopPadding() + insetPadding.calculateTopPadding(),
                                bottom = contentPadding.calculateBottomPadding() + insetPadding.calculateBottomPadding(),
                            ),
                        ) {
                            CupertinoSidebarScopeImpl(this, selectedKey) { key ->
                                onSelect(key)
                                state.showDetail()
                            }.sidebar()
                        }
                    }
                    if (expanded) {
                        Box(
                            Modifier
                                .align(Alignment.TopEnd)
                                .fillMaxHeight()
                                .width(CupertinoSidebarTokens.SeparatorWidth)
                                .background(colors.separatorColor),
                        )
                    }
                }
            }
            CompositionLocalProvider(
                LocalCupertinoRightSystemArea provides (parentRightArea && (!expanded || !sidebarOnRight)),
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .recalculateWindowInsets()
                        .background(colors.detailContainerColor),
                ) {
                    detail()
                }
            }
        },
    ) { measurables, constraints ->
        val width = if (constraints.hasBoundedWidth) constraints.maxWidth else sidebarWidth.roundToPx() * 2
        val bounds = cupertinoSidebarPaneBounds(
            widthClass = widthClass,
            totalWidth = width,
            sidebarWidth = sidebarWidth.roundToPx(),
            sidebarFraction = sidebarFraction,
            detailFraction = detailFraction,
        )
        fun paneConstraints(paneWidth: Int) = if (constraints.hasBoundedHeight) {
            Constraints.fixed(paneWidth, constraints.maxHeight)
        } else {
            Constraints(minWidth = paneWidth, maxWidth = paneWidth)
        }
        val sidebarPlaceable = measurables[0].measure(paneConstraints(bounds.sidebarWidth))
        val detailPlaceable = measurables[1].measure(paneConstraints(bounds.detailWidth))
        val height = if (constraints.hasBoundedHeight) constraints.maxHeight
            else maxOf(sidebarPlaceable.height, detailPlaceable.height)
        layout(width, height) {
            if (bounds.sidebarPlaced) sidebarPlaceable.placeRelative(bounds.sidebarX, 0)
            if (bounds.detailPlaced) detailPlaceable.placeRelative(bounds.detailX, 0, zIndex = 1f)
        }
    }
}

/**
 * Toolbar button that shows or hides the sidebar of a [CupertinoSidebarScaffold] on expanded width, the equivalent
 * of the `sidebar.leading` button of a `NavigationSplitView`. It renders nothing on compact width, where the detail
 * shows a back button instead.
 *
 * Its content description reads [showDescription] or [hideDescription] depending on the action it performs. The
 * defaults are English; pass localized strings.
 *
 * @param state state of the scaffold to control.
 * @param modifier modifier of the button.
 * @param backdrop backdrop of the glass button, usually the one of the surrounding top bar.
 * @param showDescription content description while the sidebar is hidden.
 * @param hideDescription content description while the sidebar is shown.
 */
@ExperimentalCupertinoApi
@Composable
fun CupertinoSidebarToggleButton(
    state: CupertinoSidebarState,
    modifier: Modifier = Modifier,
    backdrop: Backdrop = rememberDefaultBackdrop(),
    showDescription: String = cupertinoSidebarToggleDescription(sidebarVisible = false),
    hideDescription: String = cupertinoSidebarToggleDescription(sidebarVisible = true),
) {
    val widthClass = LocalCupertinoSidebarWidthClass.current ?: state.widthClass
    if (widthClass == CupertinoSidebarWidthClass.Compact) return
    val description = if (state.isSidebarVisible) hideDescription else showDescription
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    CupertinoLiquidIconButton(
        onClick = state::toggleSidebar,
        modifier = modifier.semantics { contentDescription = description },
        backdrop = backdrop,
    ) {
        CupertinoIcon(
            imageVector = CupertinoSidebarDefaults.ToggleIcon,
            contentDescription = null,
            modifier = if (rtl) Modifier.graphicsLayer { scaleX = -1f } else Modifier,
        )
    }
}

/** Width class resolved by the enclosing [CupertinoSidebarScaffold] in the current composition, if any. */
@OptIn(ExperimentalCupertinoApi::class)
internal val LocalCupertinoSidebarWidthClass = staticCompositionLocalOf<CupertinoSidebarWidthClass?> { null }

internal object CupertinoSidebarTokens {
    val SeparatorWidth = 0.5.dp
    const val CompactParallax = 0.3f
}

// ---- Pure logic ------------------------------------------------------------------------------------------------

/** Which columns are visible. */
internal data class CupertinoSidebarPanes(val sidebar: Boolean, val detail: Boolean)

@OptIn(ExperimentalCupertinoApi::class)
internal fun cupertinoSidebarWidthClass(width: Dp, expandedBreakpoint: Dp): CupertinoSidebarWidthClass =
    if (width >= expandedBreakpoint) CupertinoSidebarWidthClass.Expanded else CupertinoSidebarWidthClass.Compact

@OptIn(ExperimentalCupertinoApi::class)
internal fun resolveCupertinoSidebarPanes(
    widthClass: CupertinoSidebarWidthClass,
    visibility: CupertinoSidebarVisibility,
    compactColumn: CupertinoSidebarColumn,
): CupertinoSidebarPanes = when (widthClass) {
    CupertinoSidebarWidthClass.Expanded -> CupertinoSidebarPanes(
        sidebar = visibility != CupertinoSidebarVisibility.Hidden,
        detail = true,
    )
    CupertinoSidebarWidthClass.Compact -> CupertinoSidebarPanes(
        sidebar = compactColumn == CupertinoSidebarColumn.Sidebar,
        detail = compactColumn == CupertinoSidebarColumn.Detail,
    )
}

@OptIn(ExperimentalCupertinoApi::class)
internal fun toggledCupertinoSidebarVisibility(
    widthClass: CupertinoSidebarWidthClass,
    visibility: CupertinoSidebarVisibility,
): CupertinoSidebarVisibility = when {
    widthClass == CupertinoSidebarWidthClass.Compact -> visibility
    visibility == CupertinoSidebarVisibility.Hidden -> CupertinoSidebarVisibility.Shown
    else -> CupertinoSidebarVisibility.Hidden
}

/** What system back does inside the scaffold. */
internal enum class CupertinoSidebarBackAction {
    /** Pop the pushed detail and show the sidebar list. */
    ShowSidebar,

    /** Nothing to pop; back is left to the enclosing navigation. */
    None,
}

@OptIn(ExperimentalCupertinoApi::class)
internal fun cupertinoSidebarBackAction(
    widthClass: CupertinoSidebarWidthClass,
    compactColumn: CupertinoSidebarColumn,
): CupertinoSidebarBackAction =
    if (widthClass == CupertinoSidebarWidthClass.Compact && compactColumn == CupertinoSidebarColumn.Detail) {
        CupertinoSidebarBackAction.ShowSidebar
    } else {
        CupertinoSidebarBackAction.None
    }

/**
 * Window-inset sides the sidebar list pads itself with. The top inset is only applied when there is no
 * `sidebarTopBar`; a provided top bar owns it. The end side is only applied on compact width, where the list spans
 * the whole screen.
 */
internal fun cupertinoSidebarListInsetSides(expanded: Boolean, hasTopBar: Boolean): WindowInsetsSides {
    val horizontal = if (expanded) WindowInsetsSides.Start else WindowInsetsSides.Horizontal
    val sides = horizontal + WindowInsetsSides.Bottom
    return if (hasTopBar) sides else sides + WindowInsetsSides.Top
}

internal fun cupertinoSidebarToggleDescription(sidebarVisible: Boolean): String =
    if (sidebarVisible) "Hide sidebar" else "Show sidebar"

/** Horizontal geometry of both columns in layout-direction-relative pixels. */
internal data class CupertinoSidebarPaneBounds(
    val sidebarX: Int,
    val sidebarWidth: Int,
    val detailX: Int,
    val detailWidth: Int,
    val sidebarPlaced: Boolean,
    val detailPlaced: Boolean,
)

@OptIn(ExperimentalCupertinoApi::class)
internal fun cupertinoSidebarPaneBounds(
    widthClass: CupertinoSidebarWidthClass,
    totalWidth: Int,
    sidebarWidth: Int,
    sidebarFraction: Float,
    detailFraction: Float,
): CupertinoSidebarPaneBounds {
    val s = sidebarFraction.coerceIn(0f, 1f)
    val d = detailFraction.coerceIn(0f, 1f)
    return when (widthClass) {
        CupertinoSidebarWidthClass.Expanded -> {
            val sidebar = sidebarWidth.coerceIn(0, totalWidth)
            val visible = (sidebar * s).roundToInt()
            CupertinoSidebarPaneBounds(
                sidebarX = visible - sidebar,
                sidebarWidth = sidebar,
                detailX = visible,
                detailWidth = totalWidth - visible,
                sidebarPlaced = s > 0f,
                detailPlaced = true,
            )
        }
        CupertinoSidebarWidthClass.Compact -> CupertinoSidebarPaneBounds(
            sidebarX = -(totalWidth * CupertinoSidebarTokens.CompactParallax * d).roundToInt(),
            sidebarWidth = totalWidth,
            detailX = (totalWidth * (1f - d)).roundToInt(),
            detailWidth = totalWidth,
            sidebarPlaced = s > 0f,
            detailPlaced = d > 0f,
        )
    }
}

private fun sidebarLeadingIcon(): ImageVector = ImageVector.Builder(
    name = "SidebarLeading",
    defaultWidth = 27.6328.dp,
    defaultHeight = 21.5977.dp,
    viewportWidth = 27.6328f,
    viewportHeight = 21.5977f,
).apply {
    path(fill = SolidColor(Color.Black)) {
        moveTo(3.6797f, 21.5977f)
        lineTo(23.9531f, 21.5977f)
        curveTo(26.4141f, 21.5977f, 27.6328f, 20.3789f, 27.6328f, 17.9648f)
        lineTo(27.6328f, 3.6563f)
        curveTo(27.6328f, 1.2422f, 26.4141f, 0.0234f, 23.9531f, 0.0234f)
        lineTo(3.6797f, 0.0234f)
        curveTo(1.2305f, 0.0234f, 0.0f, 1.2305f, 0.0f, 3.6563f)
        lineTo(0.0f, 17.9648f)
        curveTo(0.0f, 20.3906f, 1.2305f, 21.5977f, 3.6797f, 21.5977f)
        close()
        moveTo(3.7031f, 19.7109f)
        curveTo(2.5313f, 19.7109f, 1.8867f, 19.0898f, 1.8867f, 17.8711f)
        lineTo(1.8867f, 3.75f)
        curveTo(1.8867f, 2.5313f, 2.5313f, 1.9102f, 3.7031f, 1.9102f)
        lineTo(23.9297f, 1.9102f)
        curveTo(25.0898f, 1.9102f, 25.7461f, 2.5313f, 25.7461f, 3.75f)
        lineTo(25.7461f, 17.8711f)
        curveTo(25.7461f, 19.0898f, 25.0898f, 19.7109f, 23.9297f, 19.7109f)
        close()
        moveTo(8.9297f, 20.0742f)
        lineTo(10.7695f, 20.0742f)
        lineTo(10.7695f, 1.5586f)
        lineTo(8.9297f, 1.5586f)
        close()
    }
}.build()
