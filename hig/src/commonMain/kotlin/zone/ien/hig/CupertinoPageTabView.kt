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
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerScope
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import zone.ien.hig.theme.CupertinoTheme
import kotlin.math.roundToInt

/**
 * Controls when the page index of a [CupertinoPageTabView] is shown.
 *
 * Mirrors SwiftUI `PageTabViewStyle.IndexDisplayMode`.
 */
@ExperimentalCupertinoApi
enum class CupertinoPageIndexDisplayMode {
    /** Shows the index only when there is more than one page. */
    Automatic,

    /** Always shows the index, even for a single page. */
    Always,

    /** Never shows the index. */
    Never,
}

/**
 * Controls when the capsule background behind the page index dots is shown.
 *
 * Mirrors SwiftUI `.indexViewStyle(.page(backgroundDisplayMode:))`.
 */
@ExperimentalCupertinoApi
enum class CupertinoPageIndexBackgroundDisplayMode {
    /** Platform default. Like iOS, this behaves as [Interactive]. */
    Automatic,

    /** Always shows the background. */
    Always,

    /**
     * Shows the background only while the user touches or scrubs the index, then hides it after
     * [CupertinoPageTabViewDefaults.BackgroundHideDelayMillis].
     */
    Interactive,

    /** Never shows the background. */
    Never,
}

/**
 * Cupertino page-style tab view, the equivalent of SwiftUI `TabView` with `.tabViewStyle(.page)`.
 *
 * Pages are laid out in a [HorizontalPager] and a [CupertinoPageControl] index is overlaid at the
 * bottom. Tapping a dot, scrubbing across the index or using accessibility adjust actions changes
 * the page. Page order and the index mirror in right-to-left layouts.
 *
 * @param state the [PagerState] that holds the current page and page count
 * @param modifier the [Modifier] to be applied to this tab view
 * @param indexDisplayMode when the page index is shown. See [CupertinoPageIndexDisplayMode].
 * @param backgroundDisplayMode when the capsule behind the index is shown. See
 * [CupertinoPageIndexBackgroundDisplayMode].
 * @param userScrollEnabled whether the user can change pages by swiping or with the index. Pages
 * can still be changed programmatically through [state].
 * @param colors [CupertinoPageTabViewColors] used for the index dots. See
 * [CupertinoPageTabViewDefaults.colors].
 * @param indexPadding padding around the index, measured from the bottom of this tab view
 * @param pageDescription builds the accessibility value for the index, for example "Page 2 of 5".
 * The page index is zero-based.
 * @param key a stable key for each page. See [HorizontalPager].
 * @param pageContent the content of each page
 */
@ExperimentalCupertinoApi
@Composable
fun CupertinoPageTabView(
    state: PagerState,
    modifier: Modifier = Modifier,
    indexDisplayMode: CupertinoPageIndexDisplayMode = CupertinoPageIndexDisplayMode.Automatic,
    backgroundDisplayMode: CupertinoPageIndexBackgroundDisplayMode =
        CupertinoPageIndexBackgroundDisplayMode.Automatic,
    userScrollEnabled: Boolean = true,
    colors: CupertinoPageTabViewColors = CupertinoPageTabViewDefaults.colors(),
    indexPadding: PaddingValues = CupertinoPageTabViewDefaults.IndexPadding,
    pageDescription: (page: Int, pageCount: Int) -> String = { page, count ->
        "Page ${page + 1} of $count"
    },
    key: ((index: Int) -> Any)? = null,
    pageContent: @Composable PagerScope.(page: Int) -> Unit,
) {
    Box(modifier) {
        HorizontalPager(
            state = state,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = userScrollEnabled,
            key = key,
            pageContent = pageContent,
        )
        val pageCount = state.pageCount
        if (isPageIndexVisible(indexDisplayMode, pageCount)) {
            PageIndex(
                state = state,
                pageCount = pageCount,
                backgroundDisplayMode = backgroundDisplayMode,
                enabled = userScrollEnabled,
                colors = colors,
                pageDescription = pageDescription,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(indexPadding),
            )
        }
    }
}

/**
 * Convenience overload of [CupertinoPageTabView] that remembers its own [PagerState].
 *
 * @param pageCount the number of pages
 * @param initialPage the page shown first
 * @param onPageChange called when the settled current page changes
 * @see CupertinoPageTabView
 */
@ExperimentalCupertinoApi
@Composable
fun CupertinoPageTabView(
    pageCount: Int,
    modifier: Modifier = Modifier,
    initialPage: Int = 0,
    onPageChange: ((page: Int) -> Unit)? = null,
    indexDisplayMode: CupertinoPageIndexDisplayMode = CupertinoPageIndexDisplayMode.Automatic,
    backgroundDisplayMode: CupertinoPageIndexBackgroundDisplayMode =
        CupertinoPageIndexBackgroundDisplayMode.Automatic,
    userScrollEnabled: Boolean = true,
    colors: CupertinoPageTabViewColors = CupertinoPageTabViewDefaults.colors(),
    indexPadding: PaddingValues = CupertinoPageTabViewDefaults.IndexPadding,
    pageDescription: (page: Int, pageCount: Int) -> String = { page, count ->
        "Page ${page + 1} of $count"
    },
    key: ((index: Int) -> Any)? = null,
    pageContent: @Composable PagerScope.(page: Int) -> Unit,
) {
    val state = rememberPagerState(initialPage = initialPage) { pageCount }
    val currentOnPageChange by rememberUpdatedState(onPageChange)
    LaunchedEffect(state) {
        snapshotFlow { state.settledPage }
            .collect { currentOnPageChange?.invoke(it) }
    }
    CupertinoPageTabView(
        state = state,
        modifier = modifier,
        indexDisplayMode = indexDisplayMode,
        backgroundDisplayMode = backgroundDisplayMode,
        userScrollEnabled = userScrollEnabled,
        colors = colors,
        indexPadding = indexPadding,
        pageDescription = pageDescription,
        key = key,
        pageContent = pageContent,
    )
}

/**
 * Colors used by the page index of a [CupertinoPageTabView].
 *
 * @see CupertinoPageTabViewDefaults.colors
 */
@ExperimentalCupertinoApi
@Immutable
class CupertinoPageTabViewColors internal constructor(
    /** Color of the dot for the current page. */
    val currentIndicatorColor: Color,
    /** Color of the dots for the other pages. */
    val indicatorColor: Color,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CupertinoPageTabViewColors) return false
        return currentIndicatorColor == other.currentIndicatorColor &&
            indicatorColor == other.indicatorColor
    }

    override fun hashCode(): Int = 31 * currentIndicatorColor.hashCode() + indicatorColor.hashCode()
}

/** Default values used by [CupertinoPageTabView]. */
@ExperimentalCupertinoApi
object CupertinoPageTabViewDefaults {
    /** Padding between the index and the bottom edge of the tab view. */
    val IndexPadding: PaddingValues = PaddingValues(bottom = 8.dp)

    /**
     * How long the index background stays visible after an interaction ends in
     * [CupertinoPageIndexBackgroundDisplayMode.Interactive] mode.
     */
    const val BackgroundHideDelayMillis: Long = 2_000L

    /**
     * Creates [CupertinoPageTabViewColors].
     *
     * Over full-bleed imagery, iOS uses white dots: pass `Color.White` and
     * `Color.White.copy(alpha = 0.4f)`.
     *
     * @param currentIndicatorColor color of the dot for the current page
     * @param indicatorColor color of the dots for the other pages
     */
    @Composable
    @ReadOnlyComposable
    fun colors(
        currentIndicatorColor: Color = CupertinoTheme.colorScheme.label,
        indicatorColor: Color = CupertinoTheme.colorScheme.tertiaryLabel,
    ): CupertinoPageTabViewColors = CupertinoPageTabViewColors(currentIndicatorColor, indicatorColor)
}

@OptIn(ExperimentalCupertinoApi::class)
@Composable
private fun PageIndex(
    state: PagerState,
    pageCount: Int,
    backgroundDisplayMode: CupertinoPageIndexBackgroundDisplayMode,
    enabled: Boolean,
    colors: CupertinoPageTabViewColors,
    pageDescription: (page: Int, pageCount: Int) -> String,
    modifier: Modifier,
) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val layoutDirection = LocalLayoutDirection.current
    val currentPage = state.currentPage.coerceIn(0, pageCount - 1)
    var background by remember { mutableStateOf(PageIndexBackgroundState()) }

    LaunchedEffect(background) {
        if (background.isAwaitingHide) {
            delay(CupertinoPageTabViewDefaults.BackgroundHideDelayMillis)
            background = background.reduce(PageIndexBackgroundEvent.HideTimeoutElapsed)
        }
    }

    val backgroundVisible = isPageIndexBackgroundVisible(backgroundDisplayMode, background)
    val backgroundAlpha by animateFloatAsState(
        targetValue = if (backgroundVisible) 1f else 0f,
        animationSpec = spring(),
        label = "PageIndexBackground",
    )

    val selectPage: (Int) -> Unit = { page ->
        if (enabled && page in 0 until pageCount) {
            scope.launch { state.animateScrollToPage(page) }
        }
    }
    val currentSelectPage by rememberUpdatedState(selectPage)

    CupertinoPageControlLayout(
        pageCount = pageCount,
        currentPage = currentPage,
        onPageSelected = selectPage,
        pageDescription = pageDescription,
        selectedIndicatorColor = colors.currentIndicatorColor,
        indicatorColor = colors.indicatorColor,
        backgroundAlpha = { backgroundAlpha },
        modifier = modifier
            .clearAndSetSemantics {
                pageIndexSemantics(
                    currentPage = currentPage,
                    pageCount = pageCount,
                    enabled = enabled,
                    description = pageDescription(currentPage, pageCount),
                    backgroundVisible = backgroundVisible,
                    onSelect = currentSelectPage,
                )
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                // Observes presses without consuming them so dot taps and scrubbing still work.
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    background = background.reduce(PageIndexBackgroundEvent.InteractionStarted)
                    try {
                        do {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                        } while (event.changes.any { it.pressed })
                    } finally {
                        background = background.reduce(PageIndexBackgroundEvent.InteractionEnded)
                    }
                }
            }
            .pointerInput(enabled, pageCount, layoutDirection) {
                if (!enabled) return@pointerInput
                val stepPx = PageIndexScrubStep.toPx()
                var startPage = 0
                var lastPage = 0
                var distance = 0f
                detectHorizontalDragGestures(
                    onDragStart = {
                        startPage = state.currentPage
                        lastPage = startPage
                        distance = 0f
                    },
                ) { change, dragAmount ->
                    change.consume()
                    distance += dragAmount
                    val page = scrubbedPage(startPage, distance, stepPx, pageCount, layoutDirection)
                    if (page != lastPage) {
                        lastPage = page
                        haptic.performSelectionChanged()
                        currentSelectPage(page)
                    }
                }
            },
    )
}

@OptIn(InternalCupertinoApi::class)
private fun HapticFeedback.performSelectionChanged() {
    performHapticFeedback(CupertinoHapticFeedback.SelectionChanged)
}

private fun SemanticsPropertyReceiver.pageIndexSemantics(
    currentPage: Int,
    pageCount: Int,
    enabled: Boolean,
    description: String,
    backgroundVisible: Boolean,
    onSelect: (Int) -> Unit,
) {
    stateDescription = description
    progressBarRangeInfo = ProgressBarRangeInfo(
        current = currentPage.toFloat(),
        range = 0f..(pageCount - 1).toFloat(),
        steps = (pageCount - 2).coerceAtLeast(0),
    )
    this[PageIndexBackgroundVisibleKey] = backgroundVisible
    if (enabled) {
        setProgress { value ->
            val target = value.roundToInt().coerceIn(0, pageCount - 1)
            if (target != currentPage) onSelect(target)
            true
        }
        customActions = buildList {
            if (currentPage < pageCount - 1) {
                add(CustomAccessibilityAction("Next page") { onSelect(currentPage + 1); true })
            }
            if (currentPage > 0) {
                add(CustomAccessibilityAction("Previous page") { onSelect(currentPage - 1); true })
            }
        }
    } else {
        disabled()
    }
}

private val PageIndexScrubStep = 16.dp

/** Exposes whether the index background is shown, for tests. */
internal val PageIndexBackgroundVisibleKey = SemanticsPropertyKey<Boolean>("PageIndexBackgroundVisible")

/** Whether the page index is shown for [mode] and [pageCount]. */
@OptIn(ExperimentalCupertinoApi::class)
internal fun isPageIndexVisible(mode: CupertinoPageIndexDisplayMode, pageCount: Int): Boolean =
    when (mode) {
        CupertinoPageIndexDisplayMode.Automatic -> pageCount > 1
        CupertinoPageIndexDisplayMode.Always -> pageCount > 0
        CupertinoPageIndexDisplayMode.Never -> false
    }

/** Whether the capsule behind the index is shown for [mode] and the interaction [state]. */
@OptIn(ExperimentalCupertinoApi::class)
internal fun isPageIndexBackgroundVisible(
    mode: CupertinoPageIndexBackgroundDisplayMode,
    state: PageIndexBackgroundState,
): Boolean = when (mode) {
    CupertinoPageIndexBackgroundDisplayMode.Always -> true
    CupertinoPageIndexBackgroundDisplayMode.Never -> false
    CupertinoPageIndexBackgroundDisplayMode.Automatic,
    CupertinoPageIndexBackgroundDisplayMode.Interactive,
    -> state.visible
}

/**
 * Maps a horizontal scrub of [dragDistance] pixels that started on [startPage] to a page. Each
 * [stepPx] of travel moves one page; in [LayoutDirection.Rtl] dragging right moves backwards.
 */
internal fun scrubbedPage(
    startPage: Int,
    dragDistance: Float,
    stepPx: Float,
    pageCount: Int,
    layoutDirection: LayoutDirection,
): Int {
    val steps = (dragDistance / stepPx).toInt()
    val delta = if (layoutDirection == LayoutDirection.Rtl) -steps else steps
    return (startPage + delta).coerceIn(0, (pageCount - 1).coerceAtLeast(0))
}

/** Input of the interactive index background state machine. */
internal enum class PageIndexBackgroundEvent { InteractionStarted, InteractionEnded, HideTimeoutElapsed }

/**
 * State machine for the interactive index background.
 *
 * The background appears when an interaction starts and stays while any interaction is active.
 * Once all interactions end, [isAwaitingHide] becomes `true` and the caller waits
 * [CupertinoPageTabViewDefaults.BackgroundHideDelayMillis] before sending
 * [PageIndexBackgroundEvent.HideTimeoutElapsed]. [generation] changes on every interaction event,
 * so a timer keyed on the state restarts even when a tap leaves the visible shape unchanged.
 */
internal data class PageIndexBackgroundState(
    val visible: Boolean = false,
    val activeInteractions: Int = 0,
    val generation: Int = 0,
) {
    val isAwaitingHide: Boolean get() = visible && activeInteractions == 0

    fun reduce(event: PageIndexBackgroundEvent): PageIndexBackgroundState = when (event) {
        PageIndexBackgroundEvent.InteractionStarted ->
            copy(visible = true, activeInteractions = activeInteractions + 1, generation = generation + 1)
        PageIndexBackgroundEvent.InteractionEnded ->
            copy(activeInteractions = (activeInteractions - 1).coerceAtLeast(0), generation = generation + 1)
        PageIndexBackgroundEvent.HideTimeoutElapsed ->
            if (activeInteractions == 0) copy(visible = false) else this
    }
}
