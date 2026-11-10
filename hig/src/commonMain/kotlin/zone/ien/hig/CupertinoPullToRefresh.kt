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

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * iOS-like pull to refresh container, the equivalent of `UIRefreshControl` / SwiftUI `.refreshable`.
 *
 * Wrap a vertically scrolling container (e.g. `LazyColumn` or a `Column` with `verticalScroll`) in this box.
 * When the content is scrolled to its top, pulling further moves the content down with rubber-band resistance
 * and progressively reveals the spokes of an activity indicator above it. Crossing the refresh threshold plays
 * a [CupertinoHapticFeedback.ImpactMedium] haptic once; releasing past it calls [onRefresh]. The indicator then
 * spins and stays visible for as long as [isRefreshing] is `true`, and springs back once it becomes `false`.
 *
 * Setting [isRefreshing] to `true` without a gesture (iOS `beginRefreshing()`) also reveals the spinner.
 *
 * Mouse wheel and trackpad scrolling past the top pull too (there is no fling at the end of such a scroll): the pull
 * is released once no further scroll arrives for a short moment, exactly as if a finger had been lifted there.
 *
 * Accessibility: like `UIRefreshControl`, the box places a small accessibility element at its top, ahead of the
 * content in traversal order. While idle it reads [refreshActionLabel] and can be activated (double tap) or used through
 * its "Refresh" custom action to refresh without the gesture; while [isRefreshing] is `true` it reads
 * [refreshingDescription] and announces it politely.
 *
 * Works together with [CupertinoNavigationTitle]: because the content itself moves down while pulling, the large
 * title placed as the first item of the scrolling content grows exactly like it does when overscrolling on iOS.
 * When used inside [CupertinoScaffold] behind a translucent top bar, pad this box (not only the list) with the
 * scaffold's top padding, otherwise the indicator is revealed underneath the top bar.
 *
 * @param isRefreshing whether a refresh is currently in progress. Must be updated by the caller, usually set to
 * `true` in [onRefresh] and back to `false` when loading finishes.
 * @param onRefresh called when the user releases past the threshold or invokes the "Refresh" accessibility action.
 * Not called while [isRefreshing] is already `true`.
 * @param modifier the [Modifier] to be applied to this container.
 * @param state the state of the pull gesture, see [rememberCupertinoPullToRefreshState].
 * @param enabled when `false` the pull gesture and the accessibility action are disabled. A refresh in progress
 * is still displayed.
 * @param refreshActionLabel label of the accessibility element and its custom action while idle.
 * @param refreshingDescription text announced by screen readers while refreshing.
 * @param indicator the indicator drawn behind the content, top-center aligned. Read [state] to follow the pull.
 * Defaults to [CupertinoPullToRefreshDefaults.Indicator].
 * @param content the scrolling content. Its [BoxScope] is the box itself: when the box has a fixed or filled size,
 * `Modifier.align(...)` inside [content] aligns within the whole box.
 */
@Composable
@ExperimentalCupertinoApi
fun CupertinoPullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    state: CupertinoPullToRefreshState = rememberCupertinoPullToRefreshState(),
    enabled: Boolean = true,
    refreshActionLabel: String = CupertinoPullToRefreshDefaults.RefreshActionLabel,
    refreshingDescription: String = CupertinoPullToRefreshDefaults.RefreshingDescription,
    indicator: @Composable () -> Unit = {
        CupertinoPullToRefreshDefaults.Indicator(state = state, isRefreshing = isRefreshing)
    },
    content: @Composable BoxScope.() -> Unit,
) {
    val density = LocalDensity.current
    val thresholdPx = with(density) { CupertinoPullToRefreshDefaults.RefreshThreshold.toPx() }
    val refreshingOffsetPx = with(density) { CupertinoPullToRefreshDefaults.RefreshingOffset.toPx() }
    var containerPx by remember { mutableFloatStateOf(0f) }
    SideEffect {
        state.updateMetrics(thresholdPx, refreshingOffsetPx, containerPx)
    }

    val haptics = LocalHapticFeedback.current
    val currentOnRefresh by rememberUpdatedState(onRefresh)
    val currentIsRefreshing by rememberUpdatedState(isRefreshing)
    val currentEnabled by rememberUpdatedState(enabled)
    val currentHaptics by rememberUpdatedState(haptics)
    val scope = rememberCoroutineScope()

    LaunchedEffect(isRefreshing, state) {
        if (isRefreshing) {
            state.animateTo(state.refreshingOffsetPx)
        } else if (state.distance > 0f && !state.isDragging) {
            state.isFinishing = true
            try {
                state.animateTo(0f)
            } finally {
                state.isFinishing = false
            }
        }
    }

    val connection = remember(state, scope) {
        CupertinoPullToRefreshConnection(
            state = state,
            scope = scope,
            isEnabled = { currentEnabled },
            isRefreshing = { currentIsRefreshing },
            haptics = { currentHaptics },
            onRefresh = { currentOnRefresh() },
        )
    }

    Box(
        modifier = modifier
            .onSizeChanged { containerPx = it.height.toFloat() }
            .pointerInput(connection) {
                // Observe (never consume) whether a pointer is pressed, to tell drags from wheel/trackpad scrolling.
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    connection.isPointerPressed = true
                    try {
                        do {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                        } while (event.changes.any { it.pressed })
                    } finally {
                        connection.isPointerPressed = false
                    }
                }
            }
            .nestedScroll(connection)
            .clipToBounds(),
        // Gives content the box's own minimum size, so `align` inside content works within the whole box.
        propagateMinConstraints = true,
    ) {
        Box(Modifier.align(Alignment.TopCenter), contentAlignment = Alignment.TopCenter) {
            indicator()
        }
        Box(Modifier.offset { IntOffset(0, state.distance.roundToInt()) }) {
            content()
        }
        // The accessibility element of the refresh control. It is drawn above the content (so it is never treated as
        // covered by it) and first in traversal order, with a tiny but non-empty size: zero-size and transparent nodes
        // are skipped by the platform accessibility bridges.
        if (enabled || isRefreshing) {
            Box(
                Modifier
                    .wrapContentSize(Alignment.TopCenter)
                    .size(1.dp)
                    .semantics {
                        traversalIndex = -1f
                        if (isRefreshing) {
                            contentDescription = refreshingDescription
                            liveRegion = LiveRegionMode.Polite
                        } else {
                            contentDescription = refreshActionLabel
                            val refresh = {
                                if (currentIsRefreshing || !currentEnabled) {
                                    false
                                } else {
                                    currentOnRefresh()
                                    true
                                }
                            }
                            onClick(label = refreshActionLabel, action = refresh)
                            customActions = listOf(CustomAccessibilityAction(refreshActionLabel, refresh))
                        }
                    },
            )
        }
    }
}

/**
 * Creates and remembers a [CupertinoPullToRefreshState].
 */
@Composable
@ExperimentalCupertinoApi
fun rememberCupertinoPullToRefreshState(): CupertinoPullToRefreshState =
    remember { CupertinoPullToRefreshState() }

/**
 * State of [CupertinoPullToRefreshBox].
 *
 * Exposes the current pull [distance] (after rubber-band resistance) and [distanceFraction] relative to the refresh
 * threshold, so custom indicators can follow the gesture.
 */
@Stable
@ExperimentalCupertinoApi
class CupertinoPullToRefreshState internal constructor() {
    /** Finger travel matching [distance] for the current [limitPx]; recomputed when the limit changes. */
    private var rawDistance = 0f
    private var thresholdPx by mutableFloatStateOf(0f)
    private var containerPx by mutableFloatStateOf(0f)

    internal var refreshingOffsetPx by mutableFloatStateOf(0f)
        private set

    internal var isDragging by mutableStateOf(false)

    /** `true` while the indicator animates away after a refresh finished. */
    internal var isFinishing by mutableStateOf(false)

    private var animationJob: Job? = null

    /** Rubber-band limit: the container height, but never so small that the threshold can't be reached. */
    private val limitPx: Float
        get() = max(containerPx, thresholdPx * MinLimitToThresholdRatio)

    /**
     * Current visible pull distance in pixels, already resisted by the rubber band. It is the primary state, so it
     * does not jump when the container is resized (for example by the keyboard) while the spinner is held.
     */
    var distance: Float by mutableFloatStateOf(0f)
        private set

    /**
     * [distance] relative to the refresh threshold: `0` at rest, `1` at the threshold, greater than `1` past it.
     */
    val distanceFraction: Float
        get() = if (thresholdPx > 0f) distance / thresholdPx else 0f

    internal fun updateMetrics(thresholdPx: Float, refreshingOffsetPx: Float, containerPx: Float) {
        val oldLimit = limitPx
        this.thresholdPx = thresholdPx
        this.refreshingOffsetPx = refreshingOffsetPx
        this.containerPx = containerPx
        if (limitPx != oldLimit) rawDistance = travelFor(distance)
    }

    private fun travelFor(distance: Float): Float =
        cupertinoRubberBandInverse(distance, limitPx, CupertinoPullToRefreshDefaults.RubberBandCoefficient)

    /**
     * Applies a vertical drag delta: positive pulls further, negative pushes back (never below rest).
     * Returns the consumed delta and whether this step crossed the threshold downwards.
     */
    internal fun onPull(delta: Float, isRefreshing: Boolean): PullStep {
        animationJob?.cancel()
        val before = distance
        val newRaw = (rawDistance + delta).coerceAtLeast(0f)
        val consumed = newRaw - rawDistance
        rawDistance = newRaw
        distance = cupertinoRubberBand(newRaw, limitPx, CupertinoPullToRefreshDefaults.RubberBandCoefficient)
        val after = distance
        val crossed = !isRefreshing && delta > 0f && thresholdPx > 0f &&
            before < thresholdPx && after >= thresholdPx
        return PullStep(consumed = consumed, crossedThreshold = crossed)
    }

    /**
     * Pushes the pull back by a negative [delta] one-to-one (no rubber band), never below rest, the way the held
     * spinner scrolls away with the content while refreshing. Returns the consumed delta.
     */
    internal fun unwind(delta: Float): Float {
        animationJob?.cancel()
        val consumed = max(min(delta, 0f), -distance)
        snapTo(distance + consumed)
        return consumed
    }

    /** Returns `true` when releasing now must start a refresh. */
    internal fun onRelease(isRefreshing: Boolean): Boolean =
        !isRefreshing && thresholdPx > 0f && distance >= thresholdPx

    /** Places the indicator at [distance] pixels without animation. */
    internal fun snapTo(distance: Float) {
        this.distance = distance.coerceIn(0f, limitPx * MaxSnapFraction)
        rawDistance = travelFor(this.distance)
    }

    internal suspend fun animateTo(
        target: Float,
        animationSpec: AnimationSpec<Float> = CupertinoPullToRefreshDefaults.AnimationSpec,
    ) {
        coroutineScope {
            animationJob?.cancel()
            animationJob = coroutineContext[Job]
            animate(initialValue = distance, targetValue = target, animationSpec = animationSpec) { value, _ ->
                snapTo(value)
            }
        }
    }

    internal class PullStep(val consumed: Float, val crossedThreshold: Boolean)

    private companion object {
        const val MinLimitToThresholdRatio = 2f
        const val MaxSnapFraction = 0.999f
    }
}

@OptIn(ExperimentalCupertinoApi::class, InternalCupertinoApi::class)
private class CupertinoPullToRefreshConnection(
    private val state: CupertinoPullToRefreshState,
    private val scope: CoroutineScope,
    private val isEnabled: () -> Boolean,
    private val isRefreshing: () -> Boolean,
    private val haptics: () -> HapticFeedback,
    private val onRefresh: () -> Unit,
) : NestedScrollConnection {
    /** Whether a pointer is currently pressed on the box; `false` while scrolling with a mouse wheel or trackpad. */
    var isPointerPressed: Boolean = false

    private var idleReleaseJob: Job? = null

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        // Pushing back while pulled unwinds the pull before the content scrolls.
        if (source != NestedScrollSource.UserInput || available.y >= 0f || state.distance <= 0f) return Offset.Zero
        val refreshing = isRefreshing()
        if (!isEnabled() && !refreshing) return Offset.Zero
        onPulling()
        // While refreshing the held spinner moves with the finger; otherwise the pull unwinds along the rubber band.
        val consumed = if (refreshing) state.unwind(available.y) else state.onPull(available.y, false).consumed
        return Offset(0f, consumed)
    }

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        if (source != NestedScrollSource.UserInput || available.y <= 0f || !isEnabled()) return Offset.Zero
        onPulling()
        val step = state.onPull(available.y, isRefreshing())
        if (step.crossedThreshold) haptics().performHapticFeedback(CupertinoHapticFeedback.ImpactMedium)
        return Offset(0f, step.consumed)
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        idleReleaseJob?.cancel()
        idleReleaseJob = null
        return release(available)
    }

    /**
     * Marks the pull as in progress. Without a pressed pointer (mouse wheel, trackpad) no fling will end the
     * gesture, so release once the scroll events stop.
     */
    private fun onPulling() {
        state.isDragging = true
        idleReleaseJob?.cancel()
        idleReleaseJob = if (isPointerPressed) {
            null
        } else {
            scope.launch {
                // Wait on frames rather than wall-clock time, so the release follows the frame clock.
                val start = withFrameMillis { it }
                var now = start
                while (now - start < IdleReleaseDelayMillis) now = withFrameMillis { it }
                release(Velocity.Zero)
            }
        }
    }

    private suspend fun release(available: Velocity): Velocity {
        val wasDragging = state.isDragging
        state.isDragging = false
        if (!wasDragging || state.distance <= 0f) return Velocity.Zero
        val refreshing = isRefreshing()
        if (refreshing && available.y < 0f && state.distance < state.refreshingOffsetPx) {
            // Flinging the held spinner away while refreshing: let the content take the fling and hide the spinner.
            scope.launch { state.animateTo(0f) }
            return Velocity.Zero
        }
        val startRefresh = isEnabled() && state.onRelease(refreshing)
        if (startRefresh) onRefresh()
        try {
            state.animateTo(if (startRefresh || refreshing) state.refreshingOffsetPx else 0f)
            // onRefresh normally flips isRefreshing; if it didn't, don't keep the spinner hanging.
            if (startRefresh && !isRefreshing()) state.animateTo(0f)
        } catch (e: CancellationException) {
            // Only the indicator animation was cancelled (a new drag took over); propagate real cancellation.
            if (!currentCoroutineContext().isActive) throw e
        }
        return available
    }

    private companion object {
        /** Quiet time after the last wheel/trackpad scroll event that counts as letting go. */
        const val IdleReleaseDelayMillis = 150L
    }
}

/**
 * iOS rubber-band resistance used by `UIScrollView`: `(1 - 1 / (x * c / d + 1)) * d`.
 *
 * @param overscroll raw finger travel `x` beyond the edge, in pixels. Negative values are treated as `0`.
 * @param dimension the limit `d` the result approaches, usually the scroll view dimension.
 * @param coefficient the resistance `c`; the initial slope of the curve.
 */
internal fun cupertinoRubberBand(overscroll: Float, dimension: Float, coefficient: Float): Float {
    if (overscroll <= 0f || dimension <= 0f) return 0f
    return (1f - 1f / (overscroll * coefficient / dimension + 1f)) * dimension
}

/** Inverse of [cupertinoRubberBand]: the raw overscroll needed to show [distance]. */
internal fun cupertinoRubberBandInverse(distance: Float, dimension: Float, coefficient: Float): Float {
    if (distance <= 0f || dimension <= 0f) return 0f
    val y = distance.coerceAtMost(dimension * 0.9999f)
    return dimension / coefficient * y / (dimension - y)
}

/**
 * Defaults used by [CupertinoPullToRefreshBox].
 */
@Immutable
@ExperimentalCupertinoApi
object CupertinoPullToRefreshDefaults {
    /** Pull distance (after resistance) that triggers a refresh on release. */
    val RefreshThreshold: Dp = 80.dp

    /** Height of the revealed area that holds the spinner while refreshing. */
    val RefreshingOffset: Dp = 56.dp

    /** Size of the default indicator. */
    val IndicatorSize: Dp = CupertinoActivityIndicatorDefaults.RegularSize

    /** Rubber-band resistance coefficient used by `UIScrollView`. */
    const val RubberBandCoefficient: Float = 0.55f

    const val RefreshActionLabel: String = "Refresh"

    const val RefreshingDescription: String = "Refreshing"

    /** Critically damped spring used to settle the indicator. */
    val AnimationSpec: AnimationSpec<Float> = spring(dampingRatio = 1f, stiffness = Spring.StiffnessMediumLow)

    /** Default indicator color. */
    val color: Color
        @Composable
        @ReadOnlyComposable
        get() = CupertinoActivityIndicatorDefaults.color

    /**
     * The default iOS indicator: an activity indicator whose spokes appear one by one while pulling and which spins
     * once the threshold is reached and while refreshing. It stays centered in the revealed area.
     *
     * @param state the pull state of the surrounding [CupertinoPullToRefreshBox].
     * @param isRefreshing whether a refresh is in progress.
     * @param modifier the [Modifier] to be applied to the indicator.
     * @param color color of the spokes.
     * @param size indicator size.
     */
    @Composable
    fun Indicator(
        state: CupertinoPullToRefreshState,
        isRefreshing: Boolean,
        modifier: Modifier = Modifier,
        color: Color = this.color,
        size: Dp = IndicatorSize,
    ) {
        val sizePx = with(LocalDensity.current) { size.toPx() }
        val visible by remember(state) { derivedStateOf { state.distance > 0.5f } }
        if (!visible && !isRefreshing) return
        val spinning by remember(state) { derivedStateOf { state.distanceFraction >= 1f || state.isFinishing } }
        Box(
            modifier = modifier
                .graphicsLayer {
                    val revealed = state.distance
                    translationY = (minOf(revealed, state.refreshingOffsetPx) - sizePx) / 2f
                    if (state.isFinishing && state.refreshingOffsetPx > 0f) {
                        val fraction = (revealed / state.refreshingOffsetPx).coerceIn(0f, 1f)
                        alpha = fraction
                        scaleX = fraction
                        scaleY = fraction
                    }
                }
                .size(size),
        ) {
            if (isRefreshing || spinning) {
                CupertinoActivityIndicator(size = size, color = color)
            } else {
                val progress = state.distanceFraction.coerceIn(0f, MaxPullProgress)
                CupertinoActivityIndicator(progress = progress, size = size, color = color)
            }
        }
    }

    private const val MaxPullProgress = 0.999f
}
