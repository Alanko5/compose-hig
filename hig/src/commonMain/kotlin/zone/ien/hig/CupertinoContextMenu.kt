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

/**
 * Contains the implementation of the Cupertino-style context menu (the Compose counterpart of
 * UIKit's `UIContextMenuInteraction` and SwiftUI's `.contextMenu`).
 */
package zone.ien.hig

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.spring
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.lerp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.capsule.ContinuousRoundedRectangle
import zone.ien.hig.theme.CupertinoTheme
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * State of a [CupertinoContextMenuBox]. Use it to present or dismiss the menu programmatically
 * (for example from a toolbar button or a keyboard shortcut) and to observe whether it is visible.
 *
 * @param initiallyVisible whether the menu is visible on first composition.
 */
@Stable
@ExperimentalCupertinoApi
class CupertinoContextMenuState(initiallyVisible: Boolean = false) {

    /** Whether the context menu is currently requested to be visible. */
    var isVisible: Boolean by mutableStateOf(initiallyVisible)
        private set

    /** Presents the context menu. */
    fun show() {
        isVisible = true
    }

    /** Dismisses the context menu. */
    fun dismiss() {
        isVisible = false
    }
}

/**
 * Creates and remembers a [CupertinoContextMenuState].
 *
 * @param initiallyVisible whether the menu is visible on first composition.
 */
@Composable
@ExperimentalCupertinoApi
fun rememberCupertinoContextMenuState(initiallyVisible: Boolean = false): CupertinoContextMenuState =
    remember { CupertinoContextMenuState(initiallyVisible) }

/**
 * A Cupertino-style context menu, the Compose equivalent of UIKit's `UIContextMenuInteraction` and
 * SwiftUI's `.contextMenu`.
 *
 * A long press on [content] (or a secondary click with a mouse) lifts the content with a spring
 * animation, blurs and dims everything behind it and shows a glass menu above or below the lifted
 * content. The menu is dismissed by tapping outside it, by the back gesture / Escape key, or after
 * an enabled menu action is selected. Opening the menu with a long press performs a haptic impact
 * (programmatic [CupertinoContextMenuState.show] calls and the accessibility action do not, like UIKit).
 *
 * The menu reuses [CupertinoMenuScope] and its items ([MenuAction], [MenuPickerAction], [MenuSection],
 * [MenuTitle], [MenuDivider]) from [CupertinoDropdownMenu]; actions dismiss the menu automatically after
 * their `onClick` runs, so you do not need to dismiss it yourself.
 *
 * For accessibility, an enabled box merges its [content] into a single accessibility element (like
 * SwiftUI's `.contextMenu`) that exposes a custom action and a long-click action labelled
 * [showMenuActionLabel], so the menu can be opened without a long press. Descendants that merge their own
 * semantics (for example a `clickable` child) stay separate elements. The open menu supports the
 * semantics dismiss action.
 *
 * The background blur reads the nearest [CupertinoScaffold] (the same source as [CupertinoDropdownMenu]);
 * outside a scaffold, or when you pass a backdrop that records nothing, only the dim is visible.
 *
 * A native iOS variant backed by `UIContextMenuInteraction` is intentionally not provided: UIKit can only
 * lift a `UIView` it owns and has no public API to present a context menu programmatically, so it cannot
 * preview Compose content drawn by the Compose host view.
 *
 * ```kotlin
 * CupertinoContextMenuBox(
 *     menuContent = {
 *         MenuAction(onClick = { copy() }) { CupertinoText("Copy") }
 *         MenuDivider()
 *         MenuAction(onClick = { delete() }, contentColor = CupertinoColors.systemRed) {
 *             CupertinoText("Delete")
 *         }
 *     },
 * ) {
 *     PhotoCard()
 * }
 * ```
 *
 * @param menuContent the menu items, built with the [CupertinoMenuScope] item composables.
 * @param modifier the [Modifier] applied to the content container. It is applied outside the lifted
 * snapshot, so decoration that should be lifted with the content (a background, a border) belongs in
 * [content], not here.
 * @param enabled whether long press, secondary click and the accessibility action open the menu. Disabling
 * the box while the menu is open dismisses it.
 * @param state the state used to observe or control the menu's visibility.
 * @param menuWidth the preferred width of the menu panel; it is clamped to the window.
 * @param previewShape the shape the lifted preview is clipped to.
 * @param colors the scrim and menu container colors. See [CupertinoContextMenuDefaults.colors].
 * @param backdrop the background that is blurred behind the lifted preview and read by the menu glass.
 * @param showMenuActionLabel the label of the accessibility custom action that opens the menu.
 * @param preview optional content shown in place of the lifted [content] while the menu is open (like
 * `contextMenu(menuItems:preview:)`). When `null`, a snapshot of [content] is lifted.
 * @param content the content that responds to the long press.
 */
@Composable
@ExperimentalCupertinoApi
fun CupertinoContextMenuBox(
    menuContent: @Composable CupertinoMenuScope.() -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    state: CupertinoContextMenuState = rememberCupertinoContextMenuState(),
    menuWidth: Dp = CupertinoContextMenuDefaults.MenuWidth,
    previewShape: Shape = CupertinoContextMenuDefaults.PreviewShape,
    colors: CupertinoContextMenuColors = CupertinoContextMenuDefaults.colors(),
    backdrop: Backdrop = CupertinoContextMenuDefaults.rememberBackdrop(),
    showMenuActionLabel: String = CupertinoContextMenuDefaults.ShowMenuActionLabel,
    preview: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val contentLayer = rememberGraphicsLayer()
    var snapshot by remember { mutableStateOf<ImageBitmap?>(null) }
    var anchorBounds by remember { mutableStateOf(IntRect.Zero) }
    val transitionState = remember { MutableTransitionState(false) }

    val ready = preview != null || snapshot != null
    transitionState.targetState = state.isVisible && ready
    val presented = transitionState.currentState || transitionState.targetState

    LaunchedEffect(enabled) {
        if (!enabled) state.dismiss()
    }
    // Keyed on the preview's presence too, so switching `preview` to null while the menu is open takes a
    // snapshot instead of leaving the menu requested but never presented.
    LaunchedEffect(state.isVisible, preview == null) {
        if (state.isVisible && preview == null) {
            // On first composition (or when shown before the content was ever drawn) the layer is still
            // empty: wait a few frames for it to be recorded rather than lifting a blank preview.
            var frames = 0
            while (!contentLayer.size.isNonEmpty() && frames < SnapshotMaxWaitFrames) {
                withFrameNanos { }
                frames++
            }
            val size = contentLayer.size
            snapshot = if (size.isNonEmpty()) contentLayer.toImageBitmap() else ImageBitmap(1, 1)
        }
    }
    LaunchedEffect(presented) {
        if (!presented && !state.isVisible) snapshot = null
    }

    val currentState by rememberUpdatedState(state)
    val currentHaptic by rememberUpdatedState(haptic)
    Box(
        modifier = modifier
            .onGloballyPositioned {
                // Unclipped bounds: a row partially scrolled under a clipping parent still lifts at its full size.
                anchorBounds = IntRect(it.positionInWindow().round(), it.size)
            }
            .then(
                if (enabled) {
                    Modifier
                        .contextMenuGestures { longPress ->
                            if (longPress) {
                                @OptIn(InternalCupertinoApi::class)
                                currentHaptic.performHapticFeedback(CupertinoHapticFeedback.ImpactMedium)
                            }
                            currentState.show()
                        }
                        .semantics(mergeDescendants = true) {
                            customActions = listOf(
                                CustomAccessibilityAction(showMenuActionLabel) {
                                    currentState.show()
                                    true
                                },
                            )
                            onLongClick(label = showMenuActionLabel) {
                                currentState.show()
                                true
                            }
                        }
                } else {
                    Modifier
                },
            )
            .drawWithContent {
                contentLayer.record { this@drawWithContent.drawContent() }
                if (!presented) drawLayer(contentLayer)
            },
        propagateMinConstraints = true,
    ) {
        content()

        if (presented) {
            Popup(
                popupPositionProvider = WindowOriginPositionProvider,
                onDismissRequest = { currentState.dismiss() },
                properties = PopupProperties(focusable = true, clippingEnabled = false),
            ) {
                CupertinoContextMenuOverlay(
                    transitionState = transitionState,
                    anchorBounds = anchorBounds,
                    snapshot = snapshot,
                    preview = preview,
                    previewShape = previewShape,
                    menuWidth = menuWidth,
                    colors = colors,
                    backdrop = backdrop,
                    onDismissRequest = { currentState.dismiss() },
                    menuContent = menuContent,
                )
            }
        }
    }
}

/**
 * Colors used by [CupertinoContextMenuBox].
 *
 * @property scrimColor the dim drawn over the blurred background.
 * @property menuContainerColor the tint of the menu glass panel.
 */
@Immutable
@ExperimentalCupertinoApi
class CupertinoContextMenuColors internal constructor(
    val scrimColor: Color,
    val menuContainerColor: Color,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CupertinoContextMenuColors) return false
        return scrimColor == other.scrimColor && menuContainerColor == other.menuContainerColor
    }

    override fun hashCode(): Int = 31 * scrimColor.hashCode() + menuContainerColor.hashCode()
}

/**
 * Default values for [CupertinoContextMenuBox].
 */
@Immutable
@ExperimentalCupertinoApi
object CupertinoContextMenuDefaults {
    /** Preferred width of the context menu panel. */
    val MenuWidth: Dp = 250.dp

    /** Radius of the background blur behind the lifted preview. */
    val BlurRadius: Dp = 24.dp

    /** Default label of the accessibility action that opens the menu. */
    const val ShowMenuActionLabel: String = "Show menu"

    /** Shape the lifted preview is clipped to. */
    val PreviewShape: Shape
        @Composable @ReadOnlyComposable
        get() = ContinuousRoundedRectangle(16.dp)

    /** Dim drawn over the blurred background. */
    val ScrimColor: Color
        @Composable @ReadOnlyComposable
        get() = Color.Black.copy(alpha = if (CupertinoTheme.colorScheme.isDark) .4f else .2f)

    /**
     * Creates [CupertinoContextMenuColors].
     *
     * @param scrimColor the dim drawn over the blurred background.
     * @param menuContainerColor the tint of the menu glass panel.
     */
    @Composable
    @ReadOnlyComposable
    fun colors(
        scrimColor: Color = ScrimColor,
        menuContainerColor: Color = CupertinoDropdownMenuDefaults.ContainerColor,
    ): CupertinoContextMenuColors = CupertinoContextMenuColors(
        scrimColor = scrimColor,
        menuContainerColor = menuContainerColor,
    )

    /**
     * Reads the nearest [CupertinoScaffold] at the menu's window position, falling back to the theme
     * background outside a scaffold. Same source as [CupertinoDropdownMenuDefaults.rememberBackdrop].
     */
    @Composable
    fun rememberBackdrop(): Backdrop = CupertinoDropdownMenuDefaults.rememberBackdrop()
}

/**
 * Where the lifted preview and the menu are placed, in window coordinates.
 *
 * @property previewBounds the final bounds of the lifted preview (already scaled to fit).
 * @property menuBounds the bounds of the menu panel (its height already clamped).
 * @property menuBelow whether the menu sits below the preview.
 */
@Immutable
internal data class CupertinoContextMenuPlacement(
    val previewBounds: IntRect,
    val menuBounds: IntRect,
    val menuBelow: Boolean,
)

/**
 * Maximum menu height that still leaves room for [spacing] and at least [minPreviewHeight] (or the whole
 * preview, when it is shorter) inside the window's vertical [margin]s.
 */
internal fun calculateContextMenuMaxHeight(
    previewHeight: Int,
    windowHeight: Int,
    margin: Int,
    spacing: Int,
    minPreviewHeight: Int,
): Int {
    val available = windowHeight - margin * 2
    return (available - spacing - min(previewHeight, minPreviewHeight)).coerceAtLeast(0)
}

/**
 * Places the lifted preview and the menu like UIKit does:
 *
 * - The menu is clamped to [calculateContextMenuMaxHeight]; the preview is scaled down (uniformly,
 *   around its center) when it does not fit next to the menu or is wider than the window, so it always
 *   stays fully visible.
 * - The preview stays centered on [anchorBounds] (clamped into the margins). The menu goes below it when
 *   it fits, otherwise above; when it fits on neither side the preview is shifted so the menu fits below
 *   (anchor in the upper half) or above (lower half).
 * - The menu aligns with the preview's leading edge when the preview sits in the left half of the window
 *   and with its trailing edge in the right half; a centered preview uses the layout direction's start.
 */
internal fun calculateContextMenuPlacement(
    anchorBounds: IntRect,
    previewSize: IntSize,
    menuSize: IntSize,
    windowSize: IntSize,
    margin: Int,
    spacing: Int,
    minPreviewHeight: Int,
    layoutDirection: LayoutDirection,
): CupertinoContextMenuPlacement {
    val windowWidth = windowSize.width
    val windowHeight = windowSize.height
    val availableWidth = (windowWidth - margin * 2).coerceAtLeast(0)
    val availableHeight = (windowHeight - margin * 2).coerceAtLeast(0)

    val menuMaxHeight = calculateContextMenuMaxHeight(previewSize.height, windowHeight, margin, spacing, minPreviewHeight)
    val menuHeight = min(menuSize.height, menuMaxHeight)
    val menuWidth = min(menuSize.width, availableWidth)

    val maxPreviewHeight = (availableHeight - spacing - menuHeight).coerceAtLeast(0)
    var scale = 1f
    if (previewSize.height > maxPreviewHeight && previewSize.height > 0) {
        scale = min(scale, maxPreviewHeight.toFloat() / previewSize.height)
    }
    if (previewSize.width > availableWidth && previewSize.width > 0) {
        scale = min(scale, availableWidth.toFloat() / previewSize.width)
    }
    val previewWidth = (previewSize.width * scale).roundToInt()
    val previewHeight = (previewSize.height * scale).roundToInt()

    val centerX = (anchorBounds.left + anchorBounds.right) / 2
    val centerY = (anchorBounds.top + anchorBounds.bottom) / 2
    val previewLeft = (centerX - previewWidth / 2)
        .coerceIn(margin, max(margin, windowWidth - margin - previewWidth))
    val naturalTop = (centerY - previewHeight / 2)
        .coerceIn(margin, max(margin, windowHeight - margin - previewHeight))

    val fitsBelow = naturalTop + previewHeight + spacing + menuHeight <= windowHeight - margin
    val fitsAbove = naturalTop - spacing - menuHeight >= margin
    val menuBelow: Boolean
    val previewTop: Int
    when {
        fitsBelow -> {
            menuBelow = true
            previewTop = naturalTop
        }
        fitsAbove -> {
            menuBelow = false
            previewTop = naturalTop
        }
        centerY * 2 <= windowHeight -> {
            menuBelow = true
            previewTop = (windowHeight - margin - menuHeight - spacing - previewHeight).coerceAtLeast(margin)
        }
        else -> {
            menuBelow = false
            previewTop = margin + menuHeight + spacing
        }
    }

    val previewCenterTwice = previewLeft * 2 + previewWidth
    val alignLeft = when {
        previewCenterTwice < windowWidth -> true
        previewCenterTwice > windowWidth -> false
        else -> layoutDirection == LayoutDirection.Ltr
    }
    val menuLeft = (if (alignLeft) previewLeft else previewLeft + previewWidth - menuWidth)
        .coerceIn(margin, max(margin, windowWidth - margin - menuWidth))
    val menuTop = if (menuBelow) previewTop + previewHeight + spacing else previewTop - spacing - menuHeight

    return CupertinoContextMenuPlacement(
        previewBounds = IntRect(previewLeft, previewTop, previewLeft + previewWidth, previewTop + previewHeight),
        menuBounds = IntRect(menuLeft, menuTop, menuLeft + menuWidth, menuTop + menuHeight),
        menuBelow = menuBelow,
    )
}

/**
 * Opens the menu on a long press (any pointer) or a secondary-button press (mouse). Observes the
 * [PointerEventPass.Initial] pass so it works over clickable children, and consumes the rest of the
 * gesture once the menu opens so those children do not also receive a click.
 */
private fun Modifier.contextMenuGestures(onOpen: (longPress: Boolean) -> Unit): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        val pressEvent = awaitPointerEvent(PointerEventPass.Initial)
        if (pressEvent.type != PointerEventType.Press) return@awaitEachGesture
        val down = pressEvent.changes.firstOrNull { it.changedToDownIgnoreConsumed() } ?: return@awaitEachGesture

        val secondary = pressEvent.buttons.isSecondaryPressed
        val open = if (secondary) {
            true
        } else {
            val released = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == down.id } ?: return@withTimeoutOrNull true
                    if (change.changedToUpIgnoreConsumed()) return@withTimeoutOrNull true
                    if ((change.position - down.position).getDistance() > viewConfiguration.touchSlop) {
                        return@withTimeoutOrNull true
                    }
                }
                @Suppress("UNREACHABLE_CODE")
                true
            }
            released == null
        }
        if (!open) return@awaitEachGesture

        down.consume()
        onOpen(!secondary)
        do {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            event.changes.fastForEach { it.consume() }
        } while (event.changes.any { it.pressed })
    }
}

/** Places the popup at the window origin; the overlay fills the window and positions everything itself. */
private object WindowOriginPositionProvider : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset = IntOffset.Zero
}

@Composable
private fun CupertinoContextMenuOverlay(
    transitionState: MutableTransitionState<Boolean>,
    anchorBounds: IntRect,
    snapshot: ImageBitmap?,
    preview: (@Composable () -> Unit)?,
    previewShape: Shape,
    menuWidth: Dp,
    colors: CupertinoContextMenuColors,
    backdrop: Backdrop,
    onDismissRequest: () -> Unit,
    menuContent: @Composable CupertinoMenuScope.() -> Unit,
) {
    val transition = rememberTransition(transitionState, "CupertinoContextMenu")
    val progress by transition.animateFloat(
        transitionSpec = { if (false isTransitioningTo true) LiftSpring else DropSpring },
        label = "ContextMenuProgress",
    ) { if (it) 1f else 0f }
    val currentOnDismissRequest by rememberUpdatedState(onDismissRequest)
    val scope = remember { CupertinoMenuScopeImpl(onActionSelected = { currentOnDismissRequest() }) }
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val insets = WindowInsets.systemBars
    val scrimColor = colors.scrimColor

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    Box(
        Modifier
            .fillMaxSize()
            .onPreviewKeyEvent { event ->
                if (event.key == Key.Escape) {
                    if (event.type == KeyEventType.KeyDown) currentOnDismissRequest()
                    true
                } else {
                    false
                }
            }
            .focusRequester(focusRequester)
            .focusable()
            .semantics {
                dismiss {
                    currentOnDismissRequest()
                    true
                }
            },
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = progress.coerceIn(0f, 1f) }
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RectangleShape },
                    effects = { blur(CupertinoContextMenuDefaults.BlurRadius.toPx()) },
                    highlight = null,
                    shadow = null,
                    onDrawSurface = { drawRect(scrimColor) },
                )
                .pointerInput(Unit) { detectTapGestures { currentOnDismissRequest() } },
        )

        Layout(
            content = {
                Box(
                    Modifier
                        .testTag(ContextMenuPreviewTestTag)
                        .drawBehind {
                            snapshot?.let { drawImage(it) }
                        },
                    propagateMinConstraints = true,
                ) {
                    if (preview != null) preview()
                }
                Box(Modifier.semantics { isTraversalGroup = true }, propagateMinConstraints = true) {
                    CupertinoMenuPanel(
                        width = menuWidth,
                        maxHeight = Dp.Unspecified,
                        containerColor = colors.menuContainerColor,
                        scrollState = scrollState,
                        backdrop = backdrop,
                        content = { scope.menuContent() },
                    )
                }
            },
        ) { measurables, constraints ->
            val window = IntSize(constraints.maxWidth, constraints.maxHeight)
            val safeLeft = insets.getLeft(density, layoutDirection)
            val safeTop = insets.getTop(density)
            val safeRight = insets.getRight(density, layoutDirection)
            val safeBottom = insets.getBottom(density)
            val safeSize = IntSize(
                (window.width - safeLeft - safeRight).coerceAtLeast(0),
                (window.height - safeTop - safeBottom).coerceAtLeast(0),
            )
            val margin = ContextMenuMargin.roundToPx()
            val spacing = ContextMenuSpacing.roundToPx()
            val minPreviewHeight = ContextMenuMinPreviewHeight.roundToPx()
            val availableWidth = (safeSize.width - margin * 2).coerceAtLeast(0)
            val availableHeight = (safeSize.height - margin * 2).coerceAtLeast(0)

            val previewPlaceable = measurables[0].measure(
                if (preview == null) {
                    Constraints.fixed(anchorBounds.width.coerceAtLeast(0), anchorBounds.height.coerceAtLeast(0))
                } else {
                    Constraints(maxWidth = availableWidth, maxHeight = availableHeight)
                },
            )
            val liftedSize = IntSize(
                (previewPlaceable.width * LiftScale).roundToInt(),
                (previewPlaceable.height * LiftScale).roundToInt(),
            )
            val menuMaxHeight = calculateContextMenuMaxHeight(
                liftedSize.height, safeSize.height, margin, spacing, minPreviewHeight,
            )
            val panelWidth = min(menuWidth.roundToPx(), availableWidth)
            val menuPlaceable = measurables[1].measure(
                Constraints(minWidth = panelWidth, maxWidth = panelWidth, maxHeight = menuMaxHeight),
            )

            val placement = calculateContextMenuPlacement(
                anchorBounds = anchorBounds.translate(-safeLeft, -safeTop),
                previewSize = liftedSize,
                menuSize = IntSize(menuPlaceable.width, menuPlaceable.height),
                windowSize = safeSize,
                margin = margin,
                spacing = spacing,
                minPreviewHeight = minPreviewHeight,
                layoutDirection = layoutDirection,
            ).let {
                it.copy(
                    previewBounds = it.previewBounds.translate(safeLeft, safeTop),
                    menuBounds = it.menuBounds.translate(safeLeft, safeTop),
                )
            }

            layout(window.width, window.height) {
                val previewWidth = previewPlaceable.width.coerceAtLeast(1)
                val previewHeight = previewPlaceable.height.coerceAtLeast(1)
                val startScale = if (preview == null) {
                    1f
                } else {
                    min(anchorBounds.width.toFloat() / previewWidth, anchorBounds.height.toFloat() / previewHeight)
                }
                val endScale = placement.previewBounds.width.toFloat() / previewWidth
                val startCenter = anchorBounds.center
                val endCenter = placement.previewBounds.center
                previewPlaceable.placeWithLayer(0, 0) {
                    val p = progress
                    val scale = lerp(startScale, endScale, p)
                    translationX = lerp(startCenter.x.toFloat(), endCenter.x.toFloat(), p) - previewPlaceable.width / 2f
                    translationY = lerp(startCenter.y.toFloat(), endCenter.y.toFloat(), p) - previewPlaceable.height / 2f
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = TransformOrigin.Center
                    alpha = if (preview == null) 1f else p.coerceIn(0f, 1f)
                    shape = previewShape
                    clip = true
                    shadowElevation = ContextMenuPreviewElevation.toPx() * p.coerceIn(0f, 1f)
                }

                val menuOrigin = calculateTransformOrigin(placement.previewBounds, placement.menuBounds)
                menuPlaceable.placeWithLayer(placement.menuBounds.left, placement.menuBounds.top) {
                    val p = progress
                    val scale = lerp(MenuStartScale, 1f, p)
                    scaleX = scale
                    scaleY = scale
                    alpha = p.coerceIn(0f, 1f)
                    transformOrigin = menuOrigin
                    clip = false
                }
            }
        }
    }
}

private val ContextMenuMargin = 16.dp
private val ContextMenuSpacing = 8.dp
private val ContextMenuMinPreviewHeight = 80.dp
private val ContextMenuPreviewElevation = 24.dp
private const val LiftScale = 1.03f
private const val MenuStartScale = .6f
private const val SnapshotMaxWaitFrames = 10

/** Test tag of the lifted preview container inside the overlay. */
internal const val ContextMenuPreviewTestTag = "CupertinoContextMenuPreview"

private fun IntSize.isNonEmpty(): Boolean = width > 0 && height > 0

private val LiftSpring = spring<Float>(
    dampingRatio = 0.72f,
    stiffness = Spring.StiffnessMediumLow,
)

private val DropSpring = spring<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMedium,
)
