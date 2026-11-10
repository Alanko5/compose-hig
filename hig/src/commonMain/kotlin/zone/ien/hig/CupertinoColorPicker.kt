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

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.capsule.ContinuousCapsule
import com.kyant.capsule.ContinuousRoundedRectangle
import zone.ien.hig.icons.CupertinoIcons
import zone.ien.hig.icons.outlined.Plus
import zone.ien.hig.icons.outlined.Xmark
import zone.ien.hig.theme.CupertinoTheme
import kotlin.math.roundToInt

/**
 * The views of the colour picker sheet, switched with its segmented control.
 */
@ExperimentalCupertinoApi
enum class CupertinoColorPickerMode {
    /** A grid of system colours: a grayscale row and twelve hues from dark to light. */
    Grid,

    /** A two-dimensional hue / brightness field with a draggable loupe. */
    Spectrum,

    /** Red, green and blue sliders with a hexadecimal text field. */
    Sliders,
}

/**
 * Cupertino colour picker row, the equivalent of SwiftUI `ColorPicker`.
 *
 * Shows [label] followed by a colour well. Tapping the row presents a [CupertinoColorPickerSheet]
 * titled [title] that edits [selection].
 *
 * On iOS, `CupertinoColorPickerNative` from `hig-native` presents the system
 * `UIColorPickerViewController` instead; this composable always uses the Compose picker.
 *
 * @param selection the currently selected colour
 * @param onSelectionChange called with every new colour while the user edits it
 * @param modifier the [Modifier] to be applied to this row
 * @param supportsOpacity whether the picker shows an opacity slider. When `false`, picked colours
 * are always opaque
 * @param enabled controls the enabled state of this row. When `false`, the row does not respond to
 * input and is visually dimmed
 * @param title the title of the presented sheet
 * @param recentColors colours shown in the recent colours row of the sheet (state hoisted)
 * @param onRecentColorsChange called when the user adds the current colour to the recent colours.
 * When `null` and [recentColors] is empty, the recent colours row is hidden
 * @param colors [CupertinoColorPickerColors] used by the sheet and the well. See
 * [CupertinoColorPickerDefaults.colors]
 * @param label the label displayed at the start of the row, usually a [CupertinoText]
 */
@Composable
@ExperimentalCupertinoApi
fun CupertinoColorPicker(
    selection: Color,
    onSelectionChange: (Color) -> Unit,
    modifier: Modifier = Modifier,
    supportsOpacity: Boolean = true,
    enabled: Boolean = true,
    title: String = CupertinoColorPickerDefaults.Title,
    recentColors: List<Color> = emptyList(),
    onRecentColorsChange: ((List<Color>) -> Unit)? = null,
    colors: CupertinoColorPickerColors = CupertinoColorPickerDefaults.colors(),
    label: @Composable () -> Unit,
) {
    var presented by rememberSaveable { mutableStateOf(false) }
    val shown = if (supportsOpacity) selection else selection.copy(alpha = 1f)
    val description = colorAccessibilityDescription(shown, supportsOpacity)
    Row(
        modifier =
            modifier
                .heightIn(min = CupertinoColorPickerTokens.RowMinHeight)
                .semantics(mergeDescendants = true) { stateDescription = description }
                .clickable(
                    enabled = enabled,
                    onClickLabel = "Choose color",
                    role = Role.Button,
                    interactionSource = null,
                    indication = null,
                ) { presented = true }
                .alpha(if (enabled) 1f else CupertinoColorPickerTokens.DisabledAlpha),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.weight(1f)) {
            CompositionLocalProvider(LocalContentColor provides CupertinoTheme.colorScheme.label) {
                ProvideTextStyle(CupertinoTheme.typography.body, label)
            }
        }
        ColorWellSwatch(color = shown, colors = colors)
    }
    CupertinoColorPickerSheet(
        selection = selection,
        onSelectionChange = onSelectionChange,
        onDismissRequest = { presented = false },
        visible = presented,
        supportsOpacity = supportsOpacity,
        title = title,
        recentColors = recentColors,
        onRecentColorsChange = onRecentColorsChange,
        colors = colors,
    )
}

/**
 * Cupertino colour well: a circular swatch with a rainbow ring that presents a
 * [CupertinoColorPickerSheet] when tapped. Equivalent of `UIColorWell`.
 *
 * @param selection the currently selected colour
 * @param onSelectionChange called with every new colour while the user edits it
 * @param modifier the [Modifier] to be applied to this well
 * @param supportsOpacity whether the picker shows an opacity slider
 * @param enabled controls the enabled state of this well
 * @param title the title of the presented sheet, also used as the accessibility label
 * @param recentColors colours shown in the recent colours row of the sheet (state hoisted)
 * @param onRecentColorsChange called when the user adds the current colour to the recent colours
 * @param colors [CupertinoColorPickerColors] used by the sheet and the well
 */
@Composable
@ExperimentalCupertinoApi
fun CupertinoColorWell(
    selection: Color,
    onSelectionChange: (Color) -> Unit,
    modifier: Modifier = Modifier,
    supportsOpacity: Boolean = true,
    enabled: Boolean = true,
    title: String = CupertinoColorPickerDefaults.Title,
    recentColors: List<Color> = emptyList(),
    onRecentColorsChange: ((List<Color>) -> Unit)? = null,
    colors: CupertinoColorPickerColors = CupertinoColorPickerDefaults.colors(),
) {
    var presented by rememberSaveable { mutableStateOf(false) }
    val shown = if (supportsOpacity) selection else selection.copy(alpha = 1f)
    val description = colorAccessibilityDescription(shown, supportsOpacity)
    Box(
        modifier =
            modifier
                .sizeIn(minWidth = CupertinoColorPickerTokens.MinTouchTarget, minHeight = CupertinoColorPickerTokens.MinTouchTarget)
                .semantics {
                    contentDescription = title
                    stateDescription = description
                    if (!enabled) disabled()
                }.clickable(
                    enabled = enabled,
                    role = Role.Button,
                    interactionSource = null,
                    indication = null,
                ) { presented = true }
                .alpha(if (enabled) 1f else CupertinoColorPickerTokens.DisabledAlpha),
        contentAlignment = Alignment.Center,
    ) {
        ColorWellSwatch(color = shown, colors = colors)
    }
    CupertinoColorPickerSheet(
        selection = selection,
        onSelectionChange = onSelectionChange,
        onDismissRequest = { presented = false },
        visible = presented,
        supportsOpacity = supportsOpacity,
        title = title,
        recentColors = recentColors,
        onRecentColorsChange = onRecentColorsChange,
        colors = colors,
    )
}

/**
 * Presents the Cupertino colour picker in a modal sheet, like `UIColorPickerViewController`.
 *
 * The sheet has a [title] with a close button, a segmented control switching between
 * [CupertinoColorPickerMode]s, an optional opacity slider with a checkerboard track, a preview
 * swatch of the current colour and an optional row of recent colours.
 *
 * @param selection the currently selected colour
 * @param onSelectionChange called with every new colour while the user edits it
 * @param onDismissRequest called when the user closes the sheet (close button, tap outside or back)
 * @param visible whether the sheet is presented. Changes are animated
 * @param supportsOpacity whether the opacity slider is shown. When `false`, picked colours are opaque
 * @param title the title shown at the top of the sheet
 * @param initialMode the view shown when the sheet is first presented
 * @param recentColors colours shown in the recent colours row (state hoisted, not persisted)
 * @param onRecentColorsChange called with the updated list when the user adds the current colour.
 * When `null` and [recentColors] is empty, the row is hidden
 * @param colors [CupertinoColorPickerColors] used by the sheet. See [CupertinoColorPickerDefaults.colors]
 * @param properties dialog properties of the presentation. Only `dismissOnBackPress` and
 * `dismissOnClickOutside` are honoured; the sheet is always shown full width
 */
@Composable
@ExperimentalCupertinoApi
fun CupertinoColorPickerSheet(
    selection: Color,
    onSelectionChange: (Color) -> Unit,
    onDismissRequest: () -> Unit,
    visible: Boolean = true,
    supportsOpacity: Boolean = true,
    title: String = CupertinoColorPickerDefaults.Title,
    initialMode: CupertinoColorPickerMode = CupertinoColorPickerMode.Grid,
    recentColors: List<Color> = emptyList(),
    onRecentColorsChange: ((List<Color>) -> Unit)? = null,
    colors: CupertinoColorPickerColors = CupertinoColorPickerDefaults.colors(),
    properties: DialogProperties = DialogProperties(),
) {
    var mode by rememberSaveable { mutableStateOf(initialMode) }
    val shape = CupertinoBottomSheetDefaults.shape
    AnimatedSheet(
        visible = visible,
        onDismissRequest = onDismissRequest,
        properties = properties,
    ) { updatePanelBounds ->
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)),
            contentAlignment = Alignment.BottomCenter,
        ) {
            // Keeps the panel clear of the status bar and cutout on short windows; it scrolls instead.
            val maxSheetHeight = maxHeight * CupertinoColorPickerTokens.SheetMaxHeightFraction
            CupertinoColorPickerContent(
                selection = selection,
                onSelectionChange = onSelectionChange,
                onDismissRequest = onDismissRequest,
                mode = mode,
                onModeChange = { mode = it },
                supportsOpacity = supportsOpacity,
                title = title,
                recentColors = recentColors,
                onRecentColorsChange = onRecentColorsChange,
                colors = colors,
                modifier =
                    Modifier
                        .padding(CupertinoColorPickerTokens.SheetOuterPadding)
                        .widthIn(max = CupertinoColorPickerTokens.SheetMaxWidth)
                        .fillMaxWidth()
                        .heightIn(max = maxSheetHeight)
                        .onGloballyPositioned { updatePanelBounds(it.boundsInRoot()) }
                        .clip(shape)
                        .background(colors.containerColor)
                        .verticalScroll(rememberScrollState()),
            )
        }
    }
}

/**
 * The content of the colour picker sheet without its presentation.
 */
@Composable
@ExperimentalCupertinoApi
internal fun CupertinoColorPickerContent(
    selection: Color,
    onSelectionChange: (Color) -> Unit,
    onDismissRequest: () -> Unit,
    mode: CupertinoColorPickerMode,
    onModeChange: (CupertinoColorPickerMode) -> Unit,
    supportsOpacity: Boolean,
    title: String,
    recentColors: List<Color>,
    onRecentColorsChange: ((List<Color>) -> Unit)?,
    colors: CupertinoColorPickerColors,
    modifier: Modifier = Modifier,
) {
    val updatedOnSelectionChange by rememberUpdatedState(onSelectionChange)
    val opacity = if (supportsOpacity) selection.alpha else 1f
    // Without opacity support an incoming translucent colour is shown (and kept) opaque.
    val shown = if (supportsOpacity) selection else selection.copy(alpha = 1f)
    // Emits a new colour, keeping the current opacity unless [keepAlpha] is false.
    val emit: (Color, Boolean) -> Unit = { color, keepAlpha ->
        val alpha = if (!supportsOpacity) 1f else if (keepAlpha) opacity else color.alpha
        updatedOnSelectionChange(color.copy(alpha = alpha))
    }

    // Hue survives achromatic colours (white, gray, black) so the spectrum does not jump.
    val hsb = selection.toHsb()
    var rememberedHue by remember { mutableFloatStateOf(hsb.hue) }
    val chromatic = hsb.saturation > 0.01f && hsb.brightness > 0.01f
    val hue = if (chromatic) hsb.hue else rememberedHue
    SideEffect { if (chromatic) rememberedHue = hsb.hue }

    CompositionLocalProvider(LocalContentColor provides colors.titleColor) {
        Column(
            modifier = modifier.padding(horizontal = CupertinoColorPickerTokens.ContentPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Header(title = title, onDismissRequest = onDismissRequest, colors = colors)
            CupertinoSegmentedControl(
                selectedTabIndex = mode.ordinal,
                modifier = Modifier.fillMaxWidth(),
                paddingValues = PaddingValues(0.dp),
            ) {
                CupertinoColorPickerMode.entries.forEach { entry ->
                    CupertinoSegmentedControlTab(
                        onClick = { onModeChange(entry) },
                        isSelected = entry == mode,
                    ) {
                        CupertinoText(entry.label)
                    }
                }
            }
            when (mode) {
                CupertinoColorPickerMode.Grid ->
                    ColorGrid(
                        selection = selection,
                        onPick = { emit(it, true) },
                    )
                CupertinoColorPickerMode.Spectrum ->
                    ColorSpectrum(
                        selection = selection,
                        hue = hue,
                        onPick = { h -> emit(hsbToColor(h.hue, h.saturation, h.brightness), true) },
                    )
                CupertinoColorPickerMode.Sliders ->
                    ColorSliders(
                        selection = selection,
                        supportsOpacity = supportsOpacity,
                        onPick = emit,
                        colors = colors,
                    )
            }
            if (supportsOpacity) {
                OpacitySlider(
                    selection = selection,
                    onOpacityChange = { alpha -> updatedOnSelectionChange(selection.copy(alpha = alpha)) },
                    colors = colors,
                )
            }
            CupertinoHorizontalDivider(color = colors.separatorColor)
            PreviewRow(
                selection = shown,
                supportsOpacity = supportsOpacity,
                recentColors = recentColors,
                onRecentColorsChange = onRecentColorsChange,
                onPick = { emit(it, false) },
                colors = colors,
            )
            Spacer(Modifier.height(4.dp))
        }
    }
}

@OptIn(ExperimentalCupertinoApi::class)
private val CupertinoColorPickerMode.label: String
    get() =
        when (this) {
            CupertinoColorPickerMode.Grid -> "Grid"
            CupertinoColorPickerMode.Spectrum -> "Spectrum"
            CupertinoColorPickerMode.Sliders -> "Sliders"
        }

@Composable
private fun Header(
    title: String,
    onDismissRequest: () -> Unit,
    colors: CupertinoColorPickerColors,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .heightIn(min = CupertinoColorPickerTokens.MinTouchTarget),
    ) {
        CupertinoText(
            text = title,
            style = CupertinoTheme.typography.headline,
            color = colors.titleColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.Center).semantics { heading() },
        )
        Box(
            modifier =
                Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = (CupertinoColorPickerTokens.MinTouchTarget - CupertinoColorPickerTokens.CloseButtonSize) / 2)
                    .size(CupertinoColorPickerTokens.MinTouchTarget)
                    .clickable(
                        role = Role.Button,
                        interactionSource = null,
                        indication = null,
                        onClick = onDismissRequest,
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(CupertinoColorPickerTokens.CloseButtonSize)
                    .clip(CircleShape)
                    .background(colors.closeButtonContainerColor),
                contentAlignment = Alignment.Center,
            ) {
                CupertinoIcon(
                    imageVector = CupertinoIcons.Outlined.Xmark,
                    contentDescription = "Close",
                    tint = colors.closeButtonIconColor,
                    modifier = Modifier.size(12.dp),
                )
            }
        }
    }
}

@Composable
@OptIn(InternalCupertinoApi::class)
private fun ColorGrid(
    selection: Color,
    onPick: (Color) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val selectedRgb = selection.toRgb255()
    Column(
        Modifier
            .fillMaxWidth()
            .clip(ContinuousRoundedRectangle(CupertinoColorPickerTokens.GridCornerRadius))
            .selectableGroup(),
    ) {
        GridSwatches.forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { item ->
                    val swatch = item.color
                    val selected =
                        item.rgb.red == selectedRgb.red &&
                            item.rgb.green == selectedRgb.green &&
                            item.rgb.blue == selectedRgb.blue
                    val description = item.description
                    val indicator = item.indicator
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .background(swatch)
                            .selectable(
                                selected = selected,
                                interactionSource = null,
                                indication = null,
                                role = Role.RadioButton,
                            ) {
                                haptic.performHapticFeedback(CupertinoHapticFeedback.SelectionChanged)
                                onPick(swatch)
                            }.semantics { contentDescription = description }
                            .drawWithContent {
                                drawContent()
                                if (selected) {
                                    val stroke = 3.dp.toPx()
                                    drawRoundRect(
                                        color = indicator,
                                        topLeft = Offset(stroke / 2, stroke / 2),
                                        size = Size(size.width - stroke, size.height - stroke),
                                        cornerRadius = CornerRadius(4.dp.toPx()),
                                        style = Stroke(stroke),
                                    )
                                }
                            },
                    )
                }
            }
        }
    }
}

/** A palette swatch with the values the grid needs precomputed once. */
private class GridSwatch(
    val color: Color,
) {
    val rgb = color.toRgb255()
    val description = colorAccessibilityDescription(color, includeOpacity = false)
    val indicator = if (color.luminance() > 0.6f) Color.Black else Color.White
}

private val GridSwatches: List<List<GridSwatch>> by lazy {
    CupertinoColorPickerPalette.map { row -> row.map(::GridSwatch) }
}

private val SpectrumHueStops =
    listOf(
        Color(0xFFFF0000),
        Color(0xFFFFFF00),
        Color(0xFF00FF00),
        Color(0xFF00FFFF),
        Color(0xFF0000FF),
        Color(0xFFFF00FF),
        Color(0xFFFF0000),
    )

@Composable
private fun ColorSpectrum(
    selection: Color,
    hue: Float,
    onPick: (ColorPickerHsb) -> Unit,
) {
    val updatedOnPick by rememberUpdatedState(onPick)
    // The loupe keeps its own position while dragging so it does not snap when the picked colour
    // is achromatic (for example along the white top edge).
    var localPosition by remember { mutableStateOf<Offset?>(null) }
    var lastEmitted by remember { mutableStateOf<Color?>(null) }
    var dragging by remember { mutableStateOf(false) }
    val hsb = selection.toHsb()
    val position =
        localPosition?.takeIf { lastEmitted != null && sameRgb(lastEmitted!!, selection) }
            ?: spectrumPositionOf(hsb.copy(hue = hue)).let { (x, y) -> Offset(x, y) }

    val pick: (Offset) -> Unit = { normalized ->
        val clamped = Offset(normalized.x.coerceIn(0f, 1f), normalized.y.coerceIn(0f, 1f))
        val picked = spectrumHsbAt(clamped.x, clamped.y)
        localPosition = clamped
        lastEmitted = hsbToColor(picked.hue, picked.saturation, picked.brightness)
        updatedOnPick(picked)
    }
    val description = colorAccessibilityDescription(selection, includeOpacity = false)
    val currentPosition by rememberUpdatedState(position)
    val loupeRadius by animateDpAsState(
        targetValue = if (dragging) CupertinoColorPickerTokens.LoupeDraggingRadius else CupertinoColorPickerTokens.LoupeRadius,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
    )

    Canvas(
        Modifier
            .fillMaxWidth()
            .aspectRatio(CupertinoColorPickerTokens.FieldAspectRatio)
            .clip(ContinuousRoundedRectangle(CupertinoColorPickerTokens.GridCornerRadius))
            .semantics {
                contentDescription = "Color spectrum"
                stateDescription = description
                customActions =
                    listOf(
                        CustomAccessibilityAction("Increase hue") { pick(currentPosition + Offset(1f / 24f, 0f)); true },
                        CustomAccessibilityAction("Decrease hue") { pick(currentPosition - Offset(1f / 24f, 0f)); true },
                        CustomAccessibilityAction("Lighter") { pick(currentPosition - Offset(0f, 0.1f)); true },
                        CustomAccessibilityAction("Darker") { pick(currentPosition + Offset(0f, 0.1f)); true },
                    )
            }.pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    dragging = true
                    pick(Offset(down.position.x / size.width, down.position.y / size.height))
                    drag(down.id) { change ->
                        change.consume()
                        pick(Offset(change.position.x / size.width, change.position.y / size.height))
                    }
                    dragging = false
                }
            },
    ) {
        drawRect(Brush.horizontalGradient(SpectrumHueStops))
        drawRect(Brush.verticalGradient(0f to Color.White, 0.5f to Color.White.copy(alpha = 0f)))
        drawRect(Brush.verticalGradient(0.5f to Color.Black.copy(alpha = 0f), 1f to Color.Black))
        val center = Offset(position.x * size.width, position.y * size.height)
        val radius = loupeRadius.toPx()
        drawCircle(Color.Black.copy(alpha = 0.18f), radius + 1.5.dp.toPx(), center)
        drawCircle(selection.copy(alpha = 1f), radius, center)
        drawCircle(Color.White, radius, center, style = Stroke(3.dp.toPx()))
    }
}

@Composable
private fun ColorSliders(
    selection: Color,
    supportsOpacity: Boolean,
    onPick: (Color, Boolean) -> Unit,
    colors: CupertinoColorPickerColors,
) {
    val rgb = selection.toRgb255()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ChannelSlider(
            name = "Red",
            value = rgb.red,
            start = rgb255ToColor(0, rgb.green, rgb.blue),
            end = rgb255ToColor(255, rgb.green, rgb.blue),
            onValueChange = { onPick(rgb255ToColor(it, rgb.green, rgb.blue), true) },
            colors = colors,
        )
        ChannelSlider(
            name = "Green",
            value = rgb.green,
            start = rgb255ToColor(rgb.red, 0, rgb.blue),
            end = rgb255ToColor(rgb.red, 255, rgb.blue),
            onValueChange = { onPick(rgb255ToColor(rgb.red, it, rgb.blue), true) },
            colors = colors,
        )
        ChannelSlider(
            name = "Blue",
            value = rgb.blue,
            start = rgb255ToColor(rgb.red, rgb.green, 0),
            end = rgb255ToColor(rgb.red, rgb.green, 255),
            onValueChange = { onPick(rgb255ToColor(rgb.red, rgb.green, it), true) },
            colors = colors,
        )
        HexField(selection = selection, supportsOpacity = supportsOpacity, onPick = onPick, colors = colors)
    }
}

@Composable
private fun SliderLabel(
    text: String,
    value: String,
    colors: CupertinoColorPickerColors,
) {
    Row(Modifier.fillMaxWidth()) {
        CupertinoText(
            text = text.uppercase(),
            style = CupertinoTheme.typography.footnote,
            fontWeight = FontWeight.SemiBold,
            color = colors.labelColor,
            modifier = Modifier.weight(1f),
        )
        CupertinoText(
            text = value,
            style = CupertinoTheme.typography.footnote,
            color = colors.labelColor,
        )
    }
}

@Composable
private fun ChannelSlider(
    name: String,
    value: Int,
    start: Color,
    end: Color,
    onValueChange: (Int) -> Unit,
    colors: CupertinoColorPickerColors,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SliderLabel(name, value.toString(), colors)
        GradientSlider(
            value = value.toFloat(),
            valueRange = 0f..255f,
            onValueChange = { onValueChange(it.roundToInt().coerceIn(0, 255)) },
            brushColors = listOf(start, end),
            checkerboard = false,
            colors = colors,
            modifier =
                Modifier.semantics {
                    contentDescription = name
                    stateDescription = value.toString()
                },
        )
    }
}

@Composable
private fun OpacitySlider(
    selection: Color,
    onOpacityChange: (Float) -> Unit,
    colors: CupertinoColorPickerColors,
) {
    val percent = selection.opacityPercent()
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SliderLabel("Opacity", "$percent%", colors)
        GradientSlider(
            value = percent.toFloat(),
            valueRange = 0f..100f,
            onValueChange = { onOpacityChange((it.roundToInt().coerceIn(0, 100)) / 100f) },
            brushColors = listOf(selection.copy(alpha = 0f), selection.copy(alpha = 1f)),
            checkerboard = true,
            colors = colors,
            modifier =
                Modifier.semantics {
                    contentDescription = "Opacity"
                    stateDescription = "$percent%"
                },
        )
    }
}

@Composable
private fun GradientSlider(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    brushColors: List<Color>,
    checkerboard: Boolean,
    colors: CupertinoColorPickerColors,
    modifier: Modifier = Modifier,
) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val gradient = if (isRtl) brushColors.reversed() else brushColors
    val backdrop = rememberLayerBackdrop()
    CupertinoSlider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        backdrop = backdrop,
        modifier = modifier,
        track = { _, trackBackdrop ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(CupertinoColorPickerTokens.TrackHeight)
                    .layerBackdrop(trackBackdrop)
                    .clip(ContinuousCapsule())
                    .drawBehind {
                        if (checkerboard) drawCheckerboard(colors.checkerboardColor)
                        drawRect(Brush.horizontalGradient(gradient))
                    },
            )
        },
    )
}

@Composable
private fun HexField(
    selection: Color,
    supportsOpacity: Boolean,
    onPick: (Color, Boolean) -> Unit,
    colors: CupertinoColorPickerColors,
) {
    val canonical = selection.toHexString().removePrefix("#")
    val updatedOnPick by rememberUpdatedState(onPick)
    // While focused the field shows what the user typed; otherwise it mirrors the selection. The
    // typed text is never replaced mid-entry, only when it holds a complete value that no longer
    // matches the selection (for example after a slider moved).
    var text by remember { mutableStateOf(canonical) }
    var focused by remember { mutableStateOf(false) }
    val typedIsStale =
        (text.length == 6 || text.length == 8) &&
            parseHexColor(text)?.let { !sameRgb(it, selection) } != false
    val displayed = if (focused && !typedIsStale) text else canonical
    val focusManager = LocalFocusManager.current
    val maxLength = if (supportsOpacity) 8 else 6
    // Complete values are applied while typing; the short forms (#RGB, #RGBA) only when editing
    // ends (Done or focus loss). Anything else is discarded and the field shows the selection again.
    val commit: () -> Unit = {
        if (text.length == 3 || text.length == 4) {
            parseHexColor(text)?.let { updatedOnPick(it, text.length == 3) }
        }
    }
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CupertinoText(
            text = "sRGB Hex Color #",
            style = CupertinoTheme.typography.body,
            color = colors.titleColor,
            modifier = Modifier.weight(1f),
        )
        CupertinoBorderedTextField(
            value = displayed,
            onValueChange = { input ->
                val sanitized = sanitizeHexInput(input, maxLength)
                text = sanitized
                // Only complete values are applied while typing; short forms wait for Done or blur.
                if (sanitized.length == 6 || sanitized.length == 8) {
                    parseHexColor(sanitized)?.let { onPick(it, sanitized.length == 6) }
                }
            },
            singleLine = true,
            textStyle = CupertinoTheme.typography.body.copy(textAlign = TextAlign.Center),
            keyboardOptions =
                KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Ascii,
                    imeAction = ImeAction.Done,
                ),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            modifier =
                Modifier
                    .width(CupertinoColorPickerTokens.HexFieldWidth)
                    .onFocusChanged { state ->
                        if (state.hasFocus == focused) return@onFocusChanged
                        if (state.hasFocus) {
                            text = canonical
                        } else {
                            commit()
                        }
                        focused = state.hasFocus
                    }.semantics { contentDescription = "Hex color" },
        )
    }
}

@Composable
private fun PreviewRow(
    selection: Color,
    supportsOpacity: Boolean,
    recentColors: List<Color>,
    onRecentColorsChange: ((List<Color>) -> Unit)?,
    onPick: (Color) -> Unit,
    colors: CupertinoColorPickerColors,
) {
    val description = colorAccessibilityDescription(selection, supportsOpacity)
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            Modifier
                .size(CupertinoColorPickerTokens.PreviewSize)
                .clip(ContinuousRoundedRectangle(CupertinoColorPickerTokens.PreviewCornerRadius))
                .drawBehind {
                    drawCheckerboard(colors.checkerboardColor)
                    drawRect(selection)
                }.semantics { contentDescription = "Current color, $description" },
        )
        if (recentColors.isNotEmpty() || onRecentColorsChange != null) {
            Row(
                Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Each 30 dp swatch sits in a 44 dp touch target.
                if (onRecentColorsChange != null) {
                    Box(
                        Modifier
                            .size(CupertinoColorPickerTokens.MinTouchTarget)
                            .clickable(role = Role.Button, interactionSource = null, indication = null) {
                                onRecentColorsChange(addRecentColor(recentColors, selection, CupertinoColorPickerDefaults.MaxRecentColors))
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            Modifier
                                .size(CupertinoColorPickerTokens.RecentSwatchSize)
                                .clip(CircleShape)
                                .background(colors.closeButtonContainerColor),
                            contentAlignment = Alignment.Center,
                        ) {
                            CupertinoIcon(
                                imageVector = CupertinoIcons.Outlined.Plus,
                                contentDescription = "Add to recent colors",
                                tint = colors.closeButtonIconColor,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                }
                recentColors.forEach { recent ->
                    val shownRecent = if (supportsOpacity) recent else recent.copy(alpha = 1f)
                    val recentDescription = colorAccessibilityDescription(shownRecent, supportsOpacity)
                    Box(
                        Modifier
                            .size(CupertinoColorPickerTokens.MinTouchTarget)
                            .semantics(mergeDescendants = true) {
                                contentDescription = "Recent color, $recentDescription"
                            }.clickable(role = Role.Button, interactionSource = null, indication = null) { onPick(recent) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            Modifier
                                .size(CupertinoColorPickerTokens.RecentSwatchSize)
                                .clip(CircleShape)
                                .drawBehind {
                                    drawCheckerboard(colors.checkerboardColor)
                                    drawRect(shownRecent)
                                },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorWellSwatch(
    color: Color,
    colors: CupertinoColorPickerColors,
    modifier: Modifier = Modifier,
    size: Dp = CupertinoColorPickerDefaults.WellSize,
) {
    Canvas(modifier.size(size)) {
        val ring = 3.dp.toPx()
        val gap = 2.dp.toPx()
        drawCircle(
            brush = Brush.sweepGradient(SpectrumHueStops),
            radius = this.size.minDimension / 2f - ring / 2f,
            style = Stroke(ring),
        )
        val innerRadius = this.size.minDimension / 2f - ring - gap
        val path = Path().apply { addOval(Rect(center, innerRadius)) }
        clipPath(path) {
            drawCheckerboard(colors.checkerboardColor)
        }
        drawCircle(color, innerRadius)
    }
}

/** Draws a two-tone checkerboard (white and [color]) used behind translucent colours. */
private fun DrawScope.drawCheckerboard(color: Color) {
    val cell = CupertinoColorPickerTokens.CheckerboardCell.toPx()
    drawRect(Color.White)
    val columns = (size.width / cell).toInt() + 1
    val rows = (size.height / cell).toInt() + 1
    for (row in 0 until rows) {
        for (column in 0 until columns) {
            if ((row + column) % 2 == 1) {
                drawRect(color, topLeft = Offset(column * cell, row * cell), size = Size(cell, cell))
            }
        }
    }
}

/**
 * Colours used by [CupertinoColorPicker], [CupertinoColorWell] and [CupertinoColorPickerSheet].
 *
 * See [CupertinoColorPickerDefaults.colors].
 */
@Immutable
@ExperimentalCupertinoApi
class CupertinoColorPickerColors internal constructor(
    val containerColor: Color,
    val titleColor: Color,
    val labelColor: Color,
    val closeButtonContainerColor: Color,
    val closeButtonIconColor: Color,
    val separatorColor: Color,
    val checkerboardColor: Color,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CupertinoColorPickerColors) return false
        return containerColor == other.containerColor &&
            titleColor == other.titleColor &&
            labelColor == other.labelColor &&
            closeButtonContainerColor == other.closeButtonContainerColor &&
            closeButtonIconColor == other.closeButtonIconColor &&
            separatorColor == other.separatorColor &&
            checkerboardColor == other.checkerboardColor
    }

    override fun hashCode(): Int {
        var result = containerColor.hashCode()
        result = 31 * result + titleColor.hashCode()
        result = 31 * result + labelColor.hashCode()
        result = 31 * result + closeButtonContainerColor.hashCode()
        result = 31 * result + closeButtonIconColor.hashCode()
        result = 31 * result + separatorColor.hashCode()
        result = 31 * result + checkerboardColor.hashCode()
        return result
    }
}

/**
 * Defaults used by [CupertinoColorPicker], [CupertinoColorWell] and [CupertinoColorPickerSheet].
 */
@Immutable
@ExperimentalCupertinoApi
object CupertinoColorPickerDefaults {
    /** Default sheet title, matching `UIColorPickerViewController`. */
    const val Title: String = "Colors"

    /** Default number of recent colours kept when the user adds one. */
    const val MaxRecentColors: Int = 10

    /** Diameter of the colour well. */
    val WellSize: Dp = 28.dp

    /**
     * Creates [CupertinoColorPickerColors].
     *
     * @param containerColor background of the sheet
     * @param titleColor colour of the title and primary text
     * @param labelColor colour of the slider captions and values
     * @param closeButtonContainerColor background of the close and add buttons
     * @param closeButtonIconColor icon colour of the close and add buttons
     * @param separatorColor colour of the divider above the preview row
     * @param checkerboardColor dark squares of the checkerboard drawn behind translucent colours
     */
    @Composable
    @ReadOnlyComposable
    fun colors(
        containerColor: Color = CupertinoTheme.colorScheme.secondarySystemGroupedBackground,
        titleColor: Color = CupertinoTheme.colorScheme.label,
        labelColor: Color = CupertinoTheme.colorScheme.secondaryLabel,
        closeButtonContainerColor: Color = CupertinoTheme.colorScheme.tertiarySystemFill,
        closeButtonIconColor: Color = CupertinoTheme.colorScheme.secondaryLabel,
        separatorColor: Color = CupertinoTheme.colorScheme.separator,
        checkerboardColor: Color = Color(0xFFD1D1D6),
    ): CupertinoColorPickerColors =
        CupertinoColorPickerColors(
            containerColor = containerColor,
            titleColor = titleColor,
            labelColor = labelColor,
            closeButtonContainerColor = closeButtonContainerColor,
            closeButtonIconColor = closeButtonIconColor,
            separatorColor = separatorColor,
            checkerboardColor = checkerboardColor,
        )
}

internal object CupertinoColorPickerTokens {
    val RowMinHeight = 44.dp
    const val DisabledAlpha = 0.4f
    val SheetOuterPadding = 8.dp
    val SheetMaxWidth = 500.dp
    const val SheetMaxHeightFraction = 0.9f
    val MinTouchTarget = 44.dp
    val ContentPadding = 16.dp
    val CloseButtonSize = 30.dp
    val GridCornerRadius = 8.dp
    const val FieldAspectRatio = 12f / 10f
    val LoupeRadius = 14.dp
    val LoupeDraggingRadius = 22.dp
    val TrackHeight = 28.dp
    val HexFieldWidth = 120.dp
    val PreviewSize = 56.dp
    val PreviewCornerRadius = 10.dp
    val RecentSwatchSize = 30.dp
    val CheckerboardCell = 6.dp
}
