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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.flow.first
import zone.ien.hig.theme.CupertinoTheme

/**
 * Presents [content] as a full-screen modal cover, the Compose equivalent of SwiftUI's
 * `fullScreenCover(isPresented:onDismiss:content:)`.
 *
 * The cover slides up from the bottom edge with the same spring used by
 * [CupertinoBottomSheetScaffold] (it is built on the sheet's [PresentationStyle.Fullscreen]
 * machinery) and covers the whole window: there is no card, no rounded corners, no drag handle and
 * no visible parent content behind it once presented.
 *
 * Unlike a sheet, a full-screen cover can only be dismissed by the app: provide a Close / Done
 * button in [content] that sets [isPresented] to `false`. Two exceptions route through
 * [onDismissRequest] (which should also set [isPresented] to `false`):
 * - the system back gesture / button on Android (and Escape on desktop) when
 *   [CupertinoFullScreenCoverProperties.dismissOnBackPress] is `true`;
 * - a downward swipe when [CupertinoFullScreenCoverProperties.interactiveDismissEnabled] is `true`
 *   (off by default, like iOS). If the caller still keeps [isPresented] `true` one frame later, the
 *   cover slides back up.
 *
 * When `dismissOnBackPress` is `true` the cover also exposes an accessibility dismiss action (the
 * VoiceOver escape gesture), routed to [onDismissRequest].
 *
 * The status and navigation bar icons follow the luminance of
 * [CupertinoFullScreenCoverColors.containerColor] (dark icons on a light cover and the reverse),
 * not the app theme. On iOS the appearance is applied once the cover fully covers the window and
 * restored when it starts to leave.
 *
 * The cover is hosted in a full-window dialog, so it can be called from anywhere in the hierarchy and
 * is modal for input and accessibility: content behind it is not reachable while it is presented and
 * focus moves into the cover window. [content] is laid out edge-to-edge (it receives the whole window,
 * including the status and navigation bar areas); apply `Modifier.safeDrawingPadding()` /
 * `windowInsetsPadding(...)` where content should respect the safe area, or use
 * [CupertinoBottomSheetContent] / [CupertinoTopAppBar], which pick up the full-screen insets
 * automatically.
 *
 * [content] stays in composition (keeping its state) while presented and leaves composition only after
 * the exit animation has finished, at which point [onDismiss] is invoked.
 *
 * @param isPresented Whether the cover is presented.
 * @param onDismissRequest Called when the user asks to dismiss the cover with system back or an
 * interactive swipe. It should set [isPresented] to `false`.
 * @param modifier Modifier applied to the cover's container (the full-screen surface).
 * @param properties Behaviour options, see [CupertinoFullScreenCoverProperties].
 * @param colors Container and content colors, see [CupertinoFullScreenCoverDefaults.colors].
 * @param onDismiss Called once the cover has finished its exit animation and left composition,
 * like SwiftUI's `onDismiss`.
 * @param content The content of the cover.
 */
@Composable
@ExperimentalCupertinoApi
fun CupertinoFullScreenCover(
    isPresented: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    properties: CupertinoFullScreenCoverProperties = CupertinoFullScreenCoverProperties(),
    colors: CupertinoFullScreenCoverColors = CupertinoFullScreenCoverDefaults.colors(),
    onDismiss: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val sheetState = rememberCupertinoSheetState(presentationStyle = PresentationStyle.Fullscreen)
    val currentIsPresented by rememberUpdatedState(isPresented)
    val currentOnDismissRequest by rememberUpdatedState(onDismissRequest)
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    // Bumped when an interactive swipe dismissed the cover, so a caller that keeps
    // `isPresented == true` gets the cover presented again.
    var presentationGeneration by remember { mutableIntStateOf(0) }
    var wasComposed by remember { mutableStateOf(false) }

    val settledHidden = sheetState.isSettledHidden
    val inComposition = fullScreenCoverInComposition(isPresented, settledHidden)

    if (inComposition) {
        SideEffect { wasComposed = true }
    }

    // Report the end of the exit animation once.
    LaunchedEffect(inComposition) {
        if (!inComposition && wasComposed) {
            wasComposed = false
            currentOnDismiss?.invoke()
        }
    }

    if (!inComposition) return

    LaunchedEffect(sheetState, isPresented, presentationGeneration) {
        // Wait until the sheet has measured itself, otherwise animateTo would snap.
        snapshotFlow { sheetState.swipeableState.anchors.containsKey(CupertinoSheetValue.Expanded) }
            .first { it }
        if (isPresented) sheetState.show() else sheetState.hide()
    }

    LaunchedEffect(sheetState) {
        var shown = false
        snapshotFlow { sheetState.currentValue to sheetState.swipeableState.isAnimationRunning }
            .collect { (value, animating) ->
                if (value == CupertinoSheetValue.Expanded) shown = true
                if (
                    isInteractiveFullScreenCoverDismiss(
                        isPresented = currentIsPresented,
                        wasShown = shown,
                        settledHidden = value == CupertinoSheetValue.Hidden && !animating,
                    )
                ) {
                    shown = false
                    currentOnDismissRequest()
                    // Give a caller that clears isPresented asynchronously (one frame later) the chance
                    // to do so before re-presenting, so the cover does not bounce up first.
                    withFrameNanos {}
                    if (currentIsPresented) presentationGeneration++
                }
            }
    }

    val haptic = LocalHapticFeedback.current
    val lightSystemBars = fullScreenCoverUsesLightSystemBars(colors.containerColor)
    val coversWindow =
        isPresented &&
            sheetState.currentValue == CupertinoSheetValue.Expanded &&
            sheetState.targetValue == CupertinoSheetValue.Expanded
    Dialog(
        onDismissRequest = { if (currentIsPresented) currentOnDismissRequest() },
        properties = FullScreenCoverDialogProperties(dismissOnBackPress = properties.dismissOnBackPress),
    ) {
        PrepareFullScreenCoverWindow(lightSystemBars = lightSystemBars, coversWindow = coversWindow)
        CompositionLocalProvider(LocalHapticFeedback provides haptic) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val coverHeight = maxHeight
                CupertinoBottomSheetScaffold(
                    sheetContent = {
                        // Lay the content out at the full window height and let the sheet clip it,
                        // so it slides rigidly instead of being re-measured every frame.
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .wrapContentHeight(Alignment.Top, unbounded = true)
                                .height(coverHeight)
                                .background(colors.containerColor)
                                .semantics {
                                    isTraversalGroup = true
                                    properties.paneTitle?.let { paneTitle = it }
                                    if (properties.dismissOnBackPress) {
                                        dismiss {
                                            if (currentIsPresented) currentOnDismissRequest()
                                            true
                                        }
                                    }
                                }.then(modifier),
                        ) {
                            CompositionLocalProvider(LocalContentColor provides colors.contentColor) {
                                content()
                            }
                        }
                    },
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    scaffoldState = rememberCupertinoBottomSheetScaffoldState(sheetState),
                    colors =
                        CupertinoBottomSheetScaffoldDefaults.colors(
                            sheetContainerColor = colors.containerColor,
                            sheetContentColor = colors.contentColor,
                            containerColor = Color.Transparent,
                            contentColor = colors.contentColor,
                            scrimColor = Color.Transparent,
                        ),
                    sheetShape = RectangleShape,
                    sheetDragHandle = null,
                    sheetSwipeEnabled = properties.interactiveDismissEnabled,
                    bottomBarBackdropEnabled = false,
                    content = {},
                )
            }
        }
    }
}

/**
 * Presents a full-screen cover for a non-null [item], the Compose equivalent of SwiftUI's
 * `fullScreenCover(item:onDismiss:content:)`.
 *
 * The cover is presented while [item] is non-null. While it slides out after [item] became `null`,
 * the last non-null item keeps being rendered. When [item] changes from one non-null value to
 * another while presented, the content is swapped in place and its state is reset (the content is
 * keyed by the item).
 *
 * See the `isPresented` overload of [CupertinoFullScreenCover] for behaviour, insets and dismissal.
 *
 * @param item The item to present, or `null` to dismiss.
 * @param onDismissRequest Called on system back or interactive swipe; it should set [item] to `null`.
 * @param modifier Modifier applied to the cover's container.
 * @param properties Behaviour options, see [CupertinoFullScreenCoverProperties].
 * @param colors Container and content colors, see [CupertinoFullScreenCoverDefaults.colors].
 * @param onDismiss Called once the cover has finished its exit animation.
 * @param content The content for the presented item.
 */
@Composable
@ExperimentalCupertinoApi
fun <T : Any> CupertinoFullScreenCover(
    item: T?,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    properties: CupertinoFullScreenCoverProperties = CupertinoFullScreenCoverProperties(),
    colors: CupertinoFullScreenCoverColors = CupertinoFullScreenCoverDefaults.colors(),
    onDismiss: (() -> Unit)? = null,
    content: @Composable (T) -> Unit,
) {
    // The last committed non-null item, rendered while the cover slides out after item became null.
    var lastItem by remember { mutableStateOf<T?>(null) }
    if (item != null) SideEffect { lastItem = item }
    val displayed = item ?: lastItem
    CupertinoFullScreenCover(
        isPresented = item != null,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        properties = properties,
        colors = colors,
        onDismiss = {
            lastItem = null
            onDismiss?.invoke()
        },
    ) {
        if (displayed != null) {
            key(displayed) { content(displayed) }
        }
    }
}

/**
 * Behaviour options of [CupertinoFullScreenCover].
 *
 * @param dismissOnBackPress Whether the Android system back (and Escape on desktop) calls
 * `onDismissRequest`. Defaults to `true`, as Android users expect back to close a full-screen
 * screen; set to `false` to require an explicit button in the content.
 * @param interactiveDismissEnabled Whether the cover can be dragged down to dismiss it. Defaults
 * to `false`, matching iOS where a full-screen cover cannot be swiped away.
 * @param paneTitle Accessibility pane title announced when the cover appears, or `null` (default)
 * for none. Pass a localized string; content can also provide its own title semantics.
 */
@Immutable
@ExperimentalCupertinoApi
class CupertinoFullScreenCoverProperties(
    val dismissOnBackPress: Boolean = true,
    val interactiveDismissEnabled: Boolean = false,
    val paneTitle: String? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CupertinoFullScreenCoverProperties) return false
        return dismissOnBackPress == other.dismissOnBackPress &&
            interactiveDismissEnabled == other.interactiveDismissEnabled &&
            paneTitle == other.paneTitle
    }

    override fun hashCode(): Int {
        var result = dismissOnBackPress.hashCode()
        result = 31 * result + interactiveDismissEnabled.hashCode()
        result = 31 * result + paneTitle.hashCode()
        return result
    }

    override fun toString(): String =
        "CupertinoFullScreenCoverProperties(dismissOnBackPress=$dismissOnBackPress, " +
            "interactiveDismissEnabled=$interactiveDismissEnabled, paneTitle=$paneTitle)"
}

/**
 * Colors of [CupertinoFullScreenCover]. Create with [CupertinoFullScreenCoverDefaults.colors].
 */
@Immutable
@ExperimentalCupertinoApi
class CupertinoFullScreenCoverColors internal constructor(
    val containerColor: Color,
    val contentColor: Color,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CupertinoFullScreenCoverColors) return false
        return containerColor == other.containerColor && contentColor == other.contentColor
    }

    override fun hashCode(): Int = 31 * containerColor.hashCode() + contentColor.hashCode()
}

/**
 * Defaults used by [CupertinoFullScreenCover].
 */
@Immutable
@ExperimentalCupertinoApi
object CupertinoFullScreenCoverDefaults {
    /**
     * Creates [CupertinoFullScreenCoverColors].
     *
     * @param containerColor Opaque background of the cover, `systemBackground` by default.
     * @param contentColor Default content color inside the cover.
     */
    @Composable
    @ReadOnlyComposable
    fun colors(
        containerColor: Color = CupertinoTheme.colorScheme.systemBackground,
        contentColor: Color = CupertinoTheme.colorScheme.label,
    ): CupertinoFullScreenCoverColors =
        CupertinoFullScreenCoverColors(
            containerColor = containerColor,
            contentColor = contentColor,
        )
}

/**
 * Whether the system bars over a cover with [containerColor] should use the light appearance
 * (dark icons). Derived from the container, not the theme, so a black camera cover in light mode
 * gets light icons.
 */
internal fun fullScreenCoverUsesLightSystemBars(containerColor: Color): Boolean =
    containerColor.luminance() > 0.5f

/**
 * Whether the cover (its dialog window and content) must be in composition: while presented, and
 * after dismissal until the sheet has settled at its hidden anchor.
 */
internal fun fullScreenCoverInComposition(
    isPresented: Boolean,
    settledHidden: Boolean,
): Boolean = isPresented || !settledHidden

/**
 * Whether the sheet came to rest hidden on its own (an interactive swipe) while the caller still
 * considers it presented, which must be reported through `onDismissRequest`.
 */
internal fun isInteractiveFullScreenCoverDismiss(
    isPresented: Boolean,
    wasShown: Boolean,
    settledHidden: Boolean,
): Boolean = isPresented && wasShown && settledHidden

internal val CupertinoSheetState.isSettledHidden: Boolean
    get() =
        currentValue == CupertinoSheetValue.Hidden &&
            targetValue == CupertinoSheetValue.Hidden &&
            !swipeableState.isAnimationRunning

/** Dialog properties of the full-window, edge-to-edge window hosting a [CupertinoFullScreenCover]. */
@Composable
internal expect fun FullScreenCoverDialogProperties(dismissOnBackPress: Boolean): DialogProperties

/**
 * Prepares the platform window hosting a [CupertinoFullScreenCover] (no dim, no window animation,
 * bar icons). [lightSystemBars] requests dark bar icons. [coversWindow] is `true` while the cover is
 * settled fully presented; platforms whose bar appearance is shared with the content behind the
 * cover (iOS) apply it only then.
 */
@Composable
internal expect fun PrepareFullScreenCoverWindow(lightSystemBars: Boolean, coversWindow: Boolean)
