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

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Pure editing model behind the token-aware [CupertinoSearchTextField].
 *
 * Mirrors `UISearchTextField`: the tokens sit before the text, a backspace with an empty text selects the
 * last token, a backspace while a token is selected deletes it, and typing deselects.
 */
internal data class SearchTokenEditState<T>(
    val tokens: List<T>,
    val text: String,
    val selectedTokenIndex: Int? = null,
) {
    /** Drops a selection that no longer points at an existing token. */
    fun normalized(): SearchTokenEditState<T> {
        val index = normalizedSelectedTokenIndex(selectedTokenIndex, tokens.size)
        return if (index == selectedTokenIndex) this else copy(selectedTokenIndex = index)
    }
}

internal sealed interface SearchTokenEditAction<out T> {
    /** Backspace key. Only handled when a token is selected or the text is empty. */
    data object Backspace : SearchTokenEditAction<Nothing>

    /**
     * Backspace with the caret at the start of the text (reported by deleting [SearchTokenSentinel]). Like
     * [Backspace], but also selects the last token when there is text after the caret.
     */
    data object BackspaceAtTextStart : SearchTokenEditAction<Nothing>

    /** The text field reported new text. Deselects any token. */
    data class TextChange(val text: String) : SearchTokenEditAction<Nothing>

    data class SelectToken(val index: Int) : SearchTokenEditAction<Nothing>

    data object ClearSelection : SearchTokenEditAction<Nothing>

    data class RemoveToken(val index: Int) : SearchTokenEditAction<Nothing>

    /** Inserts [token] at [index] (coerced into range), or appends it when [index] is `null`. */
    data class InsertToken<T>(val token: T, val index: Int? = null) : SearchTokenEditAction<T>

    /** Clear button: removes the text, every token and the selection. */
    data object ClearAll : SearchTokenEditAction<Nothing>
}

internal fun normalizedSelectedTokenIndex(index: Int?, tokenCount: Int): Int? =
    index?.takeIf { it in 0 until tokenCount }

/**
 * Applies [action] and returns the new state. When the action does not apply (for example a backspace
 * while text is present and no token is selected) the very same instance is returned, so callers can
 * use reference equality to decide whether the event was consumed.
 */
internal fun <T> SearchTokenEditState<T>.reduce(action: SearchTokenEditAction<T>): SearchTokenEditState<T> {
    val state = normalized()
    return when (action) {
        SearchTokenEditAction.Backspace, SearchTokenEditAction.BackspaceAtTextStart -> {
            val selected = state.selectedTokenIndex
            val caretAtStart = action == SearchTokenEditAction.BackspaceAtTextStart || state.text.isEmpty()
            when {
                selected != null -> state.copy(
                    tokens = state.tokens.filterIndexed { i, _ -> i != selected },
                    selectedTokenIndex = null,
                )
                caretAtStart && state.tokens.isNotEmpty() ->
                    state.copy(selectedTokenIndex = state.tokens.lastIndex)
                else -> this
            }
        }

        // Never "not applicable": the result always carries the reported text and drops the selection.
        is SearchTokenEditAction.TextChange ->
            state.copy(text = action.text, selectedTokenIndex = null)

        is SearchTokenEditAction.SelectToken ->
            if (action.index in state.tokens.indices) state.copy(selectedTokenIndex = action.index) else this

        SearchTokenEditAction.ClearSelection ->
            if (state.selectedTokenIndex == null) state else state.copy(selectedTokenIndex = null)

        is SearchTokenEditAction.RemoveToken -> {
            if (action.index !in state.tokens.indices) return this
            val selected = state.selectedTokenIndex
            state.copy(
                tokens = state.tokens.filterIndexed { i, _ -> i != action.index },
                selectedTokenIndex = when {
                    selected == null || selected == action.index -> null
                    selected > action.index -> selected - 1
                    else -> selected
                },
            )
        }

        is SearchTokenEditAction.InsertToken -> {
            val index = (action.index ?: state.tokens.size).coerceIn(0, state.tokens.size)
            state.copy(
                tokens = state.tokens.toMutableList().apply { add(index, action.token) },
                selectedTokenIndex = null,
            )
        }

        SearchTokenEditAction.ClearAll ->
            state.copy(tokens = emptyList(), text = "", selectedTokenIndex = null)
    }
}

/**
 * Zero-width space the token field keeps in front of the user's text while tokens are present.
 *
 * Soft keyboards (the iOS keyboard and many Android IMEs) report a backspace as an edit of the text rather
 * than as a key event, and an edit of an empty text is no edit at all. With the sentinel in front, a backspace
 * at the start of the text deletes the sentinel, which [interpretSearchTokenFieldEdit] turns into
 * [SearchTokenFieldEdit.BackspaceAtStart].
 */
internal const val SearchTokenSentinel: Char = '\u200B'

/** Result of an edit of the sentinel-prefixed text field, in user-text coordinates. */
internal sealed interface SearchTokenFieldEdit {
    /** A regular edit; [value] has no sentinel. */
    data class Value(val value: TextFieldValue) : SearchTokenFieldEdit

    /** Only the sentinel was deleted (backspace with the caret at the start of the text). */
    data object BackspaceAtStart : SearchTokenFieldEdit
}

/** Converts a user-text [TextFieldValue] to what the inner text field shows: the sentinel plus the text. */
internal fun TextFieldValue.withSearchTokenSentinel(): TextFieldValue =
    TextFieldValue(
        text = SearchTokenSentinel + text,
        selection = TextRange(selection.start + 1, selection.end + 1),
        composition = composition?.let { TextRange(it.start + 1, it.end + 1) },
    )

/**
 * Interprets a new value reported by the sentinel-prefixed text field, given the [currentText] (without the
 * sentinel) it was showing.
 */
internal fun interpretSearchTokenFieldEdit(field: TextFieldValue, currentText: String): SearchTokenFieldEdit {
    val text = field.text
    val sentinelIndex = text.indexOf(SearchTokenSentinel)
    if (sentinelIndex < 0 && text == currentText && field.selection == TextRange.Zero) {
        return SearchTokenFieldEdit.BackspaceAtStart
    }
    // Drop the sentinel wherever it ended up (normally at 0; text may have been inserted before it).
    val userText = if (sentinelIndex < 0) text else text.removeRange(sentinelIndex, sentinelIndex + 1)
    fun map(offset: Int): Int =
        (if (sentinelIndex in 0 until offset) offset - 1 else offset).coerceIn(0, userText.length)
    return SearchTokenFieldEdit.Value(
        TextFieldValue(
            text = userText,
            selection = TextRange(map(field.selection.start), map(field.selection.end)),
            composition = field.composition
                ?.let { TextRange(map(it.start), map(it.end)) }
                ?.takeUnless { it.collapsed },
        ),
    )
}

/**
 * Applies [inner] to the text after the sentinel and keeps the sentinel in front, so a caller's
 * [VisualTransformation] never sees it.
 */
internal data class SearchTokenSentinelVisualTransformation(
    val inner: VisualTransformation,
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        if (text.isEmpty() || text[0] != SearchTokenSentinel) return inner.filter(text)
        val transformed = inner.filter(text.subSequence(1, text.length))
        val mapping = transformed.offsetMapping
        return TransformedText(
            text = AnnotatedString(SearchTokenSentinel.toString()) + transformed.text,
            offsetMapping = object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int =
                    if (offset <= 0) 0 else mapping.originalToTransformed(offset - 1) + 1

                override fun transformedToOriginal(offset: Int): Int =
                    if (offset <= 0) 0 else mapping.transformedToOriginal(offset - 1) + 1
            },
        )
    }
}
