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

import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.rememberCoroutineScope
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.highlight.Highlight
import zone.ien.hig.utils.LocalCupertinoBackdrop
import zone.ien.hig.utils.InteractiveHighlight
import zone.ien.hig.utils.cupertinoGlassEffects
import zone.ien.hig.utils.glassEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animate
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.dp
import zone.ien.hig.theme.isDark
import zone.ien.hig.CupertinoButtonDefaults.plainButtonColors
import zone.ien.hig.icons.CupertinoIcons
import zone.ien.hig.icons.filled.XmarkCircle
import zone.ien.hig.icons.outlined.MagnifyingGlass
import zone.ien.hig.section.CupertinoSectionTokens
import zone.ien.hig.theme.CupertinoColors
import zone.ien.hig.theme.CupertinoTheme
import zone.ien.hig.theme.systemRed
import zone.ien.hig.utils.CupertinoGlassDefaults
import com.kyant.capsule.ContinuousCapsule
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * State of the [CupertinoSearchTextField]
 *
 * @param initiallyExpanded is search field is expanded after composition
 * @param scrollableState state of the scrollable container below the text field
 * @param collapse controls if search field should be collapsed on scroll.
 * Native behavior (default) prevents collapsing when search is focused
 * @param blockScrollWhenFocusedAndEmpty when search field is focused and search is empty
 * [scrollableState] will be blocked (native behavior)
 * */
@Composable
fun rememberCupertinoSearchTextFieldState(
    initiallyExpanded: Boolean = true,
    scrollableState: ScrollableState? = null,
    collapse: (CupertinoSearchTextFieldState) -> Boolean = { !it.isFocused },
    blockScrollWhenFocusedAndEmpty: Boolean = true,
): CupertinoSearchTextFieldState {
    val density = LocalDensity.current

    val updatedCollapse by rememberUpdatedState(collapse)

    return remember(scrollableState, blockScrollWhenFocusedAndEmpty) {
        CupertinoSearchTextFieldState(
            initiallyExpanded = initiallyExpanded,
            collapse = {
                updatedCollapse(it)
            },
            blockScrollWhenSearchIsFocusedAndEmpty = blockScrollWhenFocusedAndEmpty,
            scrollableState = scrollableState,
        ).apply {
            maxHeightPx = density.run { CupertinoSearchTextFieldTokens.MaxHeight.toPx() }
        }
    }
}

/**
 * Single line search text field like the UIKit/SwiftUI one. Supports slide-in Cancel button and collapsing
 * via nested scroll connection.
 *
 * Whenever the user edits the text, [onValueChange] is called with the most up to date state
 * represented by [String] with which developer is expected to update their state.
 *
 * Unlike [TextFieldValue] overload, this composable does not let the developer to control
 * selection, cursor and text composition information. Please check [TextFieldValue] and
 * corresponding [BasicTextField] overload for more information.
 *
 * @param value the input [String] text to be shown in the text field
 * @param onValueChange the callback that is triggered when the input service updates the text. An
 * updated text comes as a parameter of the callback
 * @param modifier optional [Modifier] for this text field.
 * @param enabled controls the enabled state of the [BasicTextField]. When `false`, the text
 * field will be neither editable nor focusable, the input of the text field will not be selectable
 * @param readOnly controls the editable state of the [BasicTextField]. When `true`, the text
 * field can not be modified, however, a user can focus it and copy text from it. Read-only text
 * fields are usually used to display pre-filled forms that user can not edit
 * @param textStyle Style configuration that applies at character level such as color, font etc.
 * @param keyboardOptions software keyboard options that contains configuration such as
 * [KeyboardType] and [ImeAction].
 * @param keyboardActions when the input service emits an IME action, the corresponding callback
 * is called. Note that this IME action may be different from what you specified in
 * [KeyboardOptions.imeAction].
 * @param visualTransformation The visual transformation filter for changing the visual
 * representation of the input. By default no visual transformation is applied.
 * @param interactionSource the [MutableInteractionSource] representing the stream of
 * [Interaction]s for this TextField. You can create and pass in your own remembered
 * [MutableInteractionSource] if you want to observe [Interaction]s and customize the
 * appearance / behavior of this TextField in different [Interaction]s.
 */
@Composable
@ExperimentalCupertinoApi
@OptIn(ExperimentalCupertinoApi::class)
fun CupertinoSearchTextField(
    value: String,
    onValueChange: (String) -> Unit,
    state: CupertinoSearchTextFieldState = rememberCupertinoSearchTextFieldState(),
    colors: CupertinoTextFieldColors = CupertinoSearchTextFieldDefaults.colors(),
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues = CupertinoSearchTextFieldDefaults.PaddingValues,
    shape: Shape = CupertinoSearchTextFieldDefaults.shape,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    keyboardOptions: KeyboardOptions =
        remember {
            KeyboardOptions(imeAction = ImeAction.Search)
        },
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    placeholder: @Composable () -> Unit = {
        CupertinoText("Search")
    },
    cancelButton: @Composable (() -> Unit)? = {
        CupertinoSearchTextFieldDefaults.cancelButton(
            onValueChange = onValueChange,
            interactionSource = interactionSource,
        )
    },
    leadingIcon: @Composable () -> Unit = {
        CupertinoSearchTextFieldDefaults.leadingIcon()
    },
    trailingIcon: @Composable () -> Unit = {},
) {
    SearchTextFieldImpl(
        value = value,
        onValueChange = onValueChange,
        state = state,
        colors = colors,
        modifier = modifier,
        paddingValues = paddingValues,
        shape = shape,
        enabled = enabled,
        readOnly = readOnly,
        textStyle = textStyle,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        interactionSource = interactionSource,
        placeholder = placeholder,
        cancelButton = cancelButton,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        tokenConfig = null,
    )
}

/**
 * Search text field with search tokens, the equivalent of `UISearchTextField.tokens` and SwiftUI's
 * `.searchable(text:tokens:)`.
 *
 * The [tokens] are drawn as tinted capsule chips (icon + text) before the text cursor and scroll
 * horizontally once they take more than ~60% of the field. Editing follows `UISearchTextField`:
 * - a backspace with the caret at the start of the text (for example while the text is empty) **selects**
 *   the last token (it is highlighted);
 * - a second backspace **deletes** the selected token;
 * - typing while a token is selected deselects it (the typed text is kept, the token stays);
 * - tapping a token selects it and focuses the field, so the next backspace deletes it;
 * - the clear (x) button removes the text and every token, and the default cancel button does the same.
 *
 * Backspace works with hardware keyboards (key events) and with soft keyboards that report a deletion as a
 * text edit (the iOS keyboard, Android IMEs that call `deleteSurroundingText`): while tokens are present
 * and the field is editable, the field keeps an invisible zero-width space (U+200B) in front of the text,
 * and deleting it counts as a backspace. The text passed to [onValueChange] never contains it, but the
 * field's own accessibility text and selection do, so a caret placed before it is moved after it.
 *
 * When [readOnly] or not [enabled], tokens cannot be selected or removed by the user.
 *
 * Every token is exposed to accessibility services with [tokenContentDescription] ("<text>, token") and a
 * [tokenRemoveActionLabel] ("Remove <text>") custom action.
 *
 * Tokens are not created from typed text automatically; add them yourself (e.g. from suggestion buttons
 * below the field) by passing a new list.
 *
 * @param value the text typed after the tokens
 * @param onValueChange called with the new text
 * @param tokens the tokens currently in the field, in display order
 * @param onTokensChange called with the new token list when the user removes a token (backspace,
 * clear button, cancel button or the accessibility remove action)
 * @param tokenContentDescription accessibility description of a token
 * @param tokenRemoveActionLabel label of the accessibility custom action that removes a token
 * @param tokenSelectActionLabel accessibility label of a token's click (select) action
 * @param tokenColors colors of the default token chip
 * @param token renders one token chip; `selected` is `true` for the token the next backspace deletes.
 * Selection, click and accessibility handling are added around it.
 * @see CupertinoSearchTextField for the remaining parameters
 */
@Composable
@ExperimentalCupertinoApi
@OptIn(ExperimentalCupertinoApi::class)
fun CupertinoSearchTextField(
    value: String,
    onValueChange: (String) -> Unit,
    tokens: List<CupertinoSearchToken>,
    onTokensChange: (List<CupertinoSearchToken>) -> Unit,
    state: CupertinoSearchTextFieldState = rememberCupertinoSearchTextFieldState(),
    colors: CupertinoTextFieldColors = CupertinoSearchTextFieldDefaults.colors(),
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues = CupertinoSearchTextFieldDefaults.PaddingValues,
    shape: Shape = CupertinoSearchTextFieldDefaults.shape,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    keyboardOptions: KeyboardOptions =
        remember {
            KeyboardOptions(imeAction = ImeAction.Search)
        },
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    placeholder: @Composable () -> Unit = {
        CupertinoText("Search")
    },
    cancelButton: @Composable (() -> Unit)? = {
        CupertinoSearchTextFieldDefaults.cancelButton(
            onValueChange = {
                onValueChange(it)
                onTokensChange(emptyList())
            },
            interactionSource = interactionSource,
        )
    },
    leadingIcon: @Composable () -> Unit = {
        CupertinoSearchTextFieldDefaults.leadingIcon()
    },
    trailingIcon: @Composable () -> Unit = {},
    tokenContentDescription: (CupertinoSearchToken) -> String = CupertinoSearchTokenDefaults::contentDescription,
    tokenRemoveActionLabel: (CupertinoSearchToken) -> String = CupertinoSearchTokenDefaults::removeActionLabel,
    tokenSelectActionLabel: String = CupertinoSearchTokenDefaults.SelectActionLabel,
    tokenColors: CupertinoSearchTokenColors = CupertinoSearchTokenDefaults.colors(),
    token: @Composable (token: CupertinoSearchToken, selected: Boolean) -> Unit = { t, selected ->
        CupertinoSearchTokenDefaults.Token(
            token = t,
            selected = selected,
            enabled = enabled,
            colors = tokenColors,
        )
    },
) {
    SearchTextFieldImpl(
        value = value,
        onValueChange = onValueChange,
        state = state,
        colors = colors,
        modifier = modifier,
        paddingValues = paddingValues,
        shape = shape,
        enabled = enabled,
        readOnly = readOnly,
        textStyle = textStyle,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        interactionSource = interactionSource,
        placeholder = placeholder,
        cancelButton = cancelButton,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        tokenConfig = SearchTokenConfig(
            tokens = tokens,
            onTokensChange = onTokensChange,
            token = token,
            labels = SearchTokenLabels(tokenContentDescription, tokenRemoveActionLabel, tokenSelectActionLabel),
        ),
    )
}

@OptIn(ExperimentalCupertinoApi::class)
private class SearchTokenConfig(
    val tokens: List<CupertinoSearchToken>,
    val onTokensChange: (List<CupertinoSearchToken>) -> Unit,
    val token: @Composable (CupertinoSearchToken, Boolean) -> Unit,
    val labels: SearchTokenLabels,
)

@OptIn(ExperimentalCupertinoApi::class)
private class LatestTokenFieldState(
    var tokens: List<CupertinoSearchToken>,
    var text: String,
)

@Composable
@OptIn(ExperimentalCupertinoApi::class)
private fun SearchTextFieldImpl(
    value: String,
    onValueChange: (String) -> Unit,
    state: CupertinoSearchTextFieldState,
    colors: CupertinoTextFieldColors,
    modifier: Modifier,
    paddingValues: PaddingValues,
    shape: Shape,
    enabled: Boolean,
    readOnly: Boolean,
    textStyle: TextStyle,
    keyboardOptions: KeyboardOptions,
    keyboardActions: KeyboardActions,
    visualTransformation: VisualTransformation,
    interactionSource: MutableInteractionSource,
    placeholder: @Composable () -> Unit,
    cancelButton: @Composable (() -> Unit)?,
    leadingIcon: @Composable () -> Unit,
    trailingIcon: @Composable () -> Unit,
    tokenConfig: SearchTokenConfig?,
) {
    val focusManager = LocalFocusManager.current
    val tokens = tokenConfig?.tokens.orEmpty()
    val editable = enabled && !readOnly

    var rawSelectedTokenIndex by remember { mutableStateOf<Int?>(null) }
    val editState = SearchTokenEditState(
        tokens = tokens,
        text = value,
        selectedTokenIndex = normalizedSelectedTokenIndex(rawSelectedTokenIndex, tokens.size),
    )
    // What the field is showing right now. Refreshed from the parameters on every composition and updated
    // synchronously by dispatch, so several edits within one frame never compare against a stale value.
    val latest = remember { LatestTokenFieldState(tokens, value) }
    latest.tokens = tokens
    latest.text = value
    val latestOnValueChange by rememberUpdatedState(onValueChange)
    val latestOnTokensChange by rememberUpdatedState(tokenConfig?.onTokensChange)

    // Returns true when the action changed anything (i.e. the event is consumed).
    val dispatch: (SearchTokenEditAction<CupertinoSearchToken>) -> Boolean = remember {
        { action ->
            val current = SearchTokenEditState(latest.tokens, latest.text, rawSelectedTokenIndex).normalized()
            val next = current.reduce(action)
            if (next == current) {
                false
            } else {
                rawSelectedTokenIndex = next.selectedTokenIndex
                if (next.tokens != current.tokens) {
                    latest.tokens = next.tokens
                    latestOnTokensChange?.invoke(next.tokens)
                }
                if (next.text != current.text) {
                    latest.text = next.text
                    latestOnValueChange(next.text)
                }
                true
            }
        }
    }

    // The token field drives a TextFieldValue (as BasicTextField(String) does internally) so it can keep
    // SearchTokenSentinel in front of the text while tokens are present: a soft-keyboard backspace at the
    // start of the text deletes the sentinel, which is detected here as a backspace without any key event.
    val useSentinel = tokenConfig != null && editable && tokens.isNotEmpty()
    var fieldValueState by remember { mutableStateOf(TextFieldValue(text = value)) }
    val fieldValue = fieldValueState.copy(text = value)
    SideEffect {
        if (fieldValue.selection != fieldValueState.selection || fieldValue.composition != fieldValueState.composition) {
            fieldValueState = fieldValue
        }
    }
    val tokenFieldValue = if (useSentinel) fieldValue.withSearchTokenSentinel() else fieldValue
    val tokenVisualTransformation = remember(visualTransformation, useSentinel) {
        if (useSentinel && visualTransformation != VisualTransformation.None) {
            SearchTokenSentinelVisualTransformation(visualTransformation)
        } else {
            visualTransformation
        }
    }
    val onTokenFieldValueChange: (TextFieldValue) -> Unit = { newValue ->
        val edit = if (useSentinel) {
            interpretSearchTokenFieldEdit(newValue, latest.text)
        } else {
            SearchTokenFieldEdit.Value(newValue)
        }
        when (edit) {
            SearchTokenFieldEdit.BackspaceAtStart -> {
                dispatch(SearchTokenEditAction.BackspaceAtTextStart)
                // Rejecting the edit is enough to restore the sentinel: the core text field recomposes after
                // every onValueChange and resets its edit buffer to the value it was given.
                fieldValueState = fieldValueState.copy(selection = TextRange.Zero, composition = null)
            }
            is SearchTokenFieldEdit.Value -> {
                fieldValueState = edit.value
                // Always forwarded when it differs from what the field showed (never gated on stale state).
                if (edit.value.text != latest.text) dispatch(SearchTokenEditAction.TextChange(edit.value.text))
            }
        }
    }
    val fieldFocusRequester = remember { FocusRequester() }

    val density = LocalDensity.current
    val contentLineHeight = when {
        textStyle.lineHeight.isSpecified -> textStyle.lineHeight
        textStyle.fontSize.isSpecified -> textStyle.fontSize * 1.25f
        else -> CupertinoTheme.typography.body.lineHeight
    }
    val expandedHeightPx = with(density) {
        maxOf(
            CupertinoSearchTextFieldTokens.MaxHeight,
            contentLineHeight.toDp() + 8.dp,
        ).toPx()
    }
    SideEffect {
        state.updateMaxHeight(expandedHeightPx)
    }

    val heightDp by remember(state) {
        derivedStateOf {
            val fraction = (1f - state.progress * (1 + state.paddingToHeight)).coerceIn(0f, 1f)
            with(density) { state.maxHeightPx.toDp() * fraction }
        }
    }

    val layoutDirection = LocalLayoutDirection.current

    val padding by remember(state, paddingValues, layoutDirection) {
        derivedStateOf {
            val fraction =
                if (state.progress < 1 - state.paddingToHeight) {
                    1f
                } else {
                    (1f - (state.progress - (1 - state.paddingToHeight)) / (1 - state.paddingToHeight)).coerceIn(0f, 1f)
                }

            paddingValues.copy(
                layoutDirection = layoutDirection,
                top = paddingValues.calculateTopPadding() * fraction,
                bottom = paddingValues.calculateBottomPadding() * fraction,
            )
        }
    }

    val focused by interactionSource.collectIsFocusedAsState()
    val pressed by interactionSource.collectIsPressedAsState()
    val animationScope = rememberCoroutineScope()
    val glassInteraction = remember(animationScope) { InteractiveHighlight(animationScope) }
    val backdrop = LocalCupertinoBackdrop.current
    val glassTint = CupertinoGlassDefaults.tint
    LaunchedEffect(pressed, enabled) {
        if (pressed && enabled) glassInteraction.press() else glassInteraction.release()
    }

    // free focus when text field starts collapsing
    LaunchedEffect(state) {
        snapshotFlow {
            state.progress > Float.MIN_VALUE
        }.distinctUntilChanged().collect {
            if (it) {
                focusManager.clearFocus(true)
            }
        }
    }

    LaunchedEffect(state.isFocused, value) {
        state.canScroll = !state.isFocused || value.isNotEmpty()
    }

    LaunchedEffect(state, interactionSource) {
        snapshotFlow { focused }.collectLatest {
            state.setFocused(it)
            if (!it) rawSelectedTokenIndex = null
        }
    }

    LaunchedEffect(paddingValues, state.maxHeightPx, density) {
        density.run {
            val v =
                (paddingValues.calculateBottomPadding() + paddingValues.calculateTopPadding())
                    .toPx() / state.maxHeightPx

            if (v >= .95f) {
                state.paddingToHeight = 0f
            } else {
                state.paddingToHeight = v
            }
        }
    }

    val totalHeight by remember {
        derivedStateOf {
            heightDp +
                padding.calculateBottomPadding() +
                padding.calculateTopPadding()
        }
    }
    Row(
        modifier =
            modifier
                .height(totalHeight)
                .padding(paddingValues),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val alpha =
            Modifier.graphicsLayer {
                alpha = ((1f - state.progress * 4 / (1 - state.paddingToHeight))).coerceIn(0f, 1f)
            }

        val fieldModifier =
            Modifier
                .weight(1f)
                .graphicsLayer {
                    scaleX = 1f + glassInteraction.pressProgress * 0.01f
                    scaleY = 1f + glassInteraction.pressProgress * 0.015f
                }
                .then(if (backdrop != null) Modifier.drawBackdrop(
                    backdrop = backdrop,
                    shape = { shape },
                    effects = {
                        cupertinoGlassEffects(
                            12.dp.toPx(),
                            CupertinoGlassDefaults.refractionHeight.toPx(),
                            CupertinoGlassDefaults.refractionAmount.toPx(),
                        )
                    },
                    highlight = { Highlight.Default.copy(alpha = 0.35f) },
                    onDrawSurface = { drawRect(glassTint) },
                ).glassEdge(shape, softShadow = true) else Modifier)
                .then(
                    if (tokenConfig != null) {
                        Modifier.onPreviewKeyEvent { event ->
                            editable &&
                                event.type == KeyEventType.KeyDown &&
                                event.key == Key.Backspace &&
                                dispatch(SearchTokenEditAction.Backspace)
                        }
                    } else {
                        Modifier
                    },
                )
                .focusable()
                // after focusable(): targets the inner text field, so tapping a token can focus it
                .focusRequester(fieldFocusRequester)
        val fieldTrailingIcon: @Composable () -> Unit = {
            Box(alpha) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    trailingIcon()
                    if ((value.isNotEmpty() || tokens.isNotEmpty()) && editable) {
                        CupertinoIconButton(
                            onClick = {
                                if (tokenConfig == null) {
                                    onValueChange("")
                                } else {
                                    dispatch(SearchTokenEditAction.ClearAll)
                                }
                            },
                            modifier = Modifier.size(44.dp),
                            colors = plainButtonColors(),
                        ) {
                            CupertinoIcon(
                                imageVector = CupertinoIcons.Filled.XmarkCircle,
                                contentDescription = "검색어 지우기",
                                modifier = Modifier.size(18.dp),
                                tint = colors.trailingIconColor(
                                    enabled = enabled,
                                    isError = false,
                                    interactionSource = interactionSource,
                                ).value,
                            )
                        }
                    }
                }
            }
        }
        val fieldLeadingIcon: @Composable () -> Unit = {
            if (tokenConfig == null) {
                Box(alpha) {
                    leadingIcon()
                }
            } else {
                Row(
                    modifier = alpha,
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    leadingIcon()
                    if (tokens.isNotEmpty()) {
                        CupertinoSearchTokenStrip(
                            tokens = tokens,
                            selectedTokenIndex = editState.selectedTokenIndex,
                            editable = editable,
                            onSelect = {
                                fieldFocusRequester.requestFocus()
                                dispatch(SearchTokenEditAction.SelectToken(it))
                            },
                            onRemove = { dispatch(SearchTokenEditAction.RemoveToken(it)) },
                            tokenContent = tokenConfig.token,
                            labels = tokenConfig.labels,
                        )
                    }
                }
            }
        }
        val fieldPlaceholder: @Composable () -> Unit = {
            // like UISearchTextField, the placeholder is hidden once a token is present
            if (tokens.isEmpty()) {
                Box(alpha) {
                    placeholder()
                }
            }
        }

        if (tokenConfig == null) {
            CupertinoBorderedTextField(
                modifier = fieldModifier,
                value = value,
                onValueChange = onValueChange,
            enabled = enabled,
            readOnly = readOnly,
            colors = colors,
            textStyle = textStyle,
            shape = shape,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = true,
            maxLines = 1,
            minLines = 1,
            paddingValues = PaddingValues(horizontal = 12.dp),
            visualTransformation = visualTransformation,
            interactionSource = interactionSource,
                trailingIcon = fieldTrailingIcon,
                leadingIcon = fieldLeadingIcon,
                placeholder = fieldPlaceholder,
            )
        } else {
            CupertinoBorderedTextField(
                modifier = fieldModifier,
                value = tokenFieldValue,
                onValueChange = onTokenFieldValueChange,
            enabled = enabled,
            readOnly = readOnly,
            colors = colors,
            textStyle = textStyle,
            shape = shape,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = true,
            maxLines = 1,
            minLines = 1,
            paddingValues = PaddingValues(horizontal = 12.dp),
            visualTransformation = tokenVisualTransformation,
            interactionSource = interactionSource,
                trailingIcon = fieldTrailingIcon,
                leadingIcon = fieldLeadingIcon,
                placeholder = fieldPlaceholder,
            )
        }

        if (cancelButton != null) {
            CompositionLocalProvider(
                LocalContentColor provides colors.cursorColor(false).value,
            ) {
                CancelButton(
                    state = state,
                    enabled = enabled && !readOnly,
                    content = cancelButton,
                )
            }
        }
    }
}

@Composable
private fun RowScope.CancelButton(
    state: CupertinoSearchTextFieldState,
    enabled: Boolean,
    content: @Composable () -> Unit,
) {
    val progressIsZero by remember {
        derivedStateOf {
            state.progress == 0f
        }
    }

    AnimatedVisibility(
        visible = enabled && state.isFocused && progressIsZero,
        enter =
            fadeIn() +
                expandHorizontally(expandFrom = Alignment.Start, clip = false),
        exit =
            fadeOut() +
                shrinkHorizontally(shrinkTowards = Alignment.Start, clip = false),
    ) {
        content()
    }
}

@Immutable
object CupertinoSearchTextFieldDefaults {
    val shape: Shape
        @Composable
        @ReadOnlyComposable
        get() = ContinuousCapsule()

    val PaddingValues =
        PaddingValues(
            horizontal = CupertinoSectionTokens.HorizontalPadding,
        )

    @Composable
    fun leadingIcon(
        imageVector: ImageVector = CupertinoIcons.Outlined.MagnifyingGlass,
        rotateWithLayoutDirection: Boolean = true,
    ) {
        val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

        CupertinoIcon(
            modifier =
                Modifier
                    .size(CupertinoSearchTextFieldTokens.LeadingIconSize)
                    .graphicsLayer {
                        rotationY =
                            if (isRtl && rotateWithLayoutDirection) {
                                180f
                            } else {
                                0f
                            }
                    },
            imageVector = imageVector,
            contentDescription = null,
        )
    }

    /**
     * Default plain cancel button that clears input and hides keyboard on click
     * */
    @OptIn(ExperimentalCupertinoApi::class)
    @Composable
    fun cancelButton(
        onValueChange: (String) -> Unit,
        colors: CupertinoButtonColors = plainButtonColors(),
        interactionSource: MutableInteractionSource,
        content: @Composable RowScope.() -> Unit = { CupertinoText("Cancel") },
    ) {
        val focusManager = LocalFocusManager.current

        CupertinoButton(
            modifier = Modifier.padding(start = 4.dp),
            colors = colors,
            interactionSource = interactionSource,
            contentPadding = PaddingValues(6.dp, 4.dp, 0.dp, 4.dp),
            onClick = {
                onValueChange("")
                focusManager.clearFocus(true)
            },
            content = content,
        )
    }

    @Composable
    @ReadOnlyComposable
    fun colors(
        focusedTextColor: Color = CupertinoTheme.colorScheme.label,
        unfocusedTextColor: Color = CupertinoTheme.colorScheme.label,
        disabledTextColor: Color = CupertinoTheme.colorScheme.secondaryLabel,
        errorTextColor: Color = CupertinoColors.systemRed,
        focusedContainerColor: Color =
            if (LocalCupertinoBackdrop.current != null) Color.Transparent else CupertinoGlassDefaults.tint,
        unfocusedContainerColor: Color = focusedContainerColor,
        disabledContainerColor: Color = unfocusedContainerColor.copy(alpha = unfocusedContainerColor.alpha * .65f),
        errorContainerColor: Color = disabledContainerColor,
        cursorColor: Color = CupertinoTheme.colorScheme.accent,
        errorCursorColor: Color = errorTextColor,
        selectionColors: TextSelectionColors =
            TextSelectionColors(cursorColor, cursorColor.copy(alpha = .25f)),
        focusedBorderColor: Color = if (LocalCupertinoBackdrop.current != null) Color.Transparent else CupertinoGlassDefaults.border,
        unfocusedBorderColor: Color = focusedBorderColor,
        disabledBorderColor: Color = unfocusedBorderColor.copy(alpha = unfocusedBorderColor.alpha * .65f),
        errorBorderColor: Color = errorTextColor,
        focusedLeadingIconColor: Color = CupertinoTheme.colorScheme.label,
        unfocusedLeadingIconColor: Color = focusedLeadingIconColor,
        disabledLeadingIconColor: Color = CupertinoTheme.colorScheme.tertiaryLabel,
        errorLeadingIconColor: Color = focusedLeadingIconColor,
        focusedTrailingIconColor: Color = focusedLeadingIconColor,
        unfocusedTrailingIconColor: Color = focusedTrailingIconColor,
        disabledTrailingIconColor: Color = CupertinoTheme.colorScheme.tertiaryLabel,
        errorTrailingIconColor: Color = focusedTrailingIconColor,
        focusedPlaceholderColor: Color = CupertinoTheme.colorScheme.secondaryLabel,
        unfocusedPlaceholderColor: Color = focusedPlaceholderColor,
        disabledPlaceholderColor: Color = CupertinoTheme.colorScheme.tertiaryLabel,
        errorPlaceholderColor: Color = focusedPlaceholderColor,
    ): CupertinoTextFieldColors =
        CupertinoTextFieldColors(
            focusedTextColor = focusedTextColor,
            unfocusedTextColor = unfocusedTextColor,
            disabledTextColor = disabledTextColor,
            errorTextColor = errorTextColor,
            focusedContainerColor = focusedContainerColor,
            unfocusedContainerColor = unfocusedContainerColor,
            disabledContainerColor = disabledContainerColor,
            errorContainerColor = errorContainerColor,
            cursorColor = cursorColor,
            errorCursorColor = errorCursorColor,
            textSelectionColors = selectionColors,
            focusedIndicatorColor = focusedBorderColor,
            unfocusedIndicatorColor = unfocusedBorderColor,
            disabledIndicatorColor = disabledBorderColor,
            errorIndicatorColor = errorBorderColor,
            focusedLeadingIconColor = focusedLeadingIconColor,
            unfocusedLeadingIconColor = unfocusedLeadingIconColor,
            disabledLeadingIconColor = disabledLeadingIconColor,
            errorLeadingIconColor = errorLeadingIconColor,
            focusedTrailingIconColor = focusedTrailingIconColor,
            unfocusedTrailingIconColor = unfocusedTrailingIconColor,
            disabledTrailingIconColor = disabledTrailingIconColor,
            errorTrailingIconColor = errorTrailingIconColor,
            focusedPlaceholderColor = focusedPlaceholderColor,
            unfocusedPlaceholderColor = unfocusedPlaceholderColor,
            disabledPlaceholderColor = disabledPlaceholderColor,
            errorPlaceholderColor = errorPlaceholderColor,
        )
}

internal object CupertinoSearchTextFieldTokens {
    val MaxHeight = 44.dp

    val LeadingIconSize = 20.dp
}

class CupertinoSearchTextFieldState internal constructor(
    initiallyExpanded: Boolean,
    blockScrollWhenSearchIsFocusedAndEmpty: Boolean,
    private val collapse: (CupertinoSearchTextFieldState) -> Boolean,
    private val scrollableState: ScrollableState?,
) {
    /**
     * Nested scroll connection that should be applied to scrollable container or it's host.
     * Allows to collapse the text field during scrolling
     * */
    val nestedScrollConnection: NestedScrollConnection =
        CupertinoSearchTextFieldNestedScroll(
            state = this,
            scrollableState = scrollableState,
            blockScrollWhenSearchIsFocusedAndEmpty = blockScrollWhenSearchIsFocusedAndEmpty,
        )

    /**
     * Progress of the text field collapse.
     * 0 - expanded, 1 if collapsed
     * */
    var progress by mutableStateOf(if (initiallyExpanded) 0f else 1f)
        private set

    /**
     * Text field focus state
     * */
    var isFocused: Boolean by mutableStateOf(false)
        private set

    internal var canScroll: Boolean by mutableStateOf(true)

    internal var maxHeightPx by mutableStateOf(0f)

    internal var paddingToHeight by mutableStateOf(0f)

    private var collapsedBy by mutableStateOf(0f)

    internal fun setFocused(focused: Boolean) {
        this.isFocused = focused
    }

    internal fun updateMaxHeight(maxHeightPx: Float) {
        if (this.maxHeightPx == maxHeightPx) return

        val collapsedFraction = if (this.maxHeightPx > 0f) {
            collapsedBy / this.maxHeightPx
        } else {
            progress
        }
        collapsedBy = collapsedFraction * maxHeightPx
        this.maxHeightPx = maxHeightPx
        progress = collapsedFraction
    }

    internal fun onScroll(available: Float): Float {
        val oldCollapsed = collapsedBy

        val newCollapsedBy = (oldCollapsed - available).coerceIn(0f, maxHeightPx)

        if (collapse(this) || newCollapsedBy < oldCollapsed) {
            collapsedBy = newCollapsedBy
            progress = if (maxHeightPx == 0f) 0f else collapsedBy / maxHeightPx

            return oldCollapsed - collapsedBy
        }
        return 0f
    }

    internal suspend fun onRelease(): Velocity {
        // text field can't be collapsed if scrollable state can't be scrolled
        val willBeExpanded =
            scrollableState
                ?.let { !it.canScrollForward && !it.canScrollBackward } == true ||
                collapsedBy / (1 - paddingToHeight) <= maxHeightPx * .75

        animate(progress, if (willBeExpanded) 0f else 1f) { v, _ ->
            progress = v
        }

        collapsedBy = if (willBeExpanded) 0f else maxHeightPx

        return Velocity.Zero
    }
}

private class CupertinoSearchTextFieldNestedScroll(
    private val state: CupertinoSearchTextFieldState,
    private val blockScrollWhenSearchIsFocusedAndEmpty: Boolean,
    private val scrollableState: ScrollableState?,
): NestedScrollConnection {
    override fun onPreScroll(
        available: Offset,
        source: NestedScrollSource,
    ): Offset {
        //  consume all scroll when scroll is blocked
        if (blockScrollWhenSearchIsFocusedAndEmpty && !state.canScroll) {
            return available
        }

        // search should collapse after scrollable container reached top
        if (available.y > 0 && scrollableState?.canScrollBackward == true) {
            return Offset.Zero
        }
        return Offset(0f, state.onScroll(available.y))
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource,
    ): Offset =
        super.onPostScroll(consumed, available, source) +
            onPreScroll(available, source)

    override suspend fun onPostFling(
        consumed: Velocity,
        available: Velocity,
    ): Velocity = state.onRelease()
}
