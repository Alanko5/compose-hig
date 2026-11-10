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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class CupertinoSearchTokenEditingTest {
    private val photos = "Photos"
    private val favorites = "Favorites"

    @Test
    fun backspaceOnEmptyTextSelectsLastToken() {
        val state = SearchTokenEditState(listOf(photos, favorites), text = "")

        val next = state.reduce(SearchTokenEditAction.Backspace)

        assertEquals(listOf(photos, favorites), next.tokens)
        assertEquals(1, next.selectedTokenIndex)
    }

    @Test
    fun secondBackspaceDeletesSelectedToken() {
        val state = SearchTokenEditState(listOf(photos, favorites), text = "")

        val next = state
            .reduce(SearchTokenEditAction.Backspace)
            .reduce(SearchTokenEditAction.Backspace)

        assertEquals(listOf(photos), next.tokens)
        assertNull(next.selectedTokenIndex)
    }

    @Test
    fun repeatedBackspaceRemovesTokensFromTheEnd() {
        var state = SearchTokenEditState(listOf(photos, favorites), text = "")

        repeat(4) { state = state.reduce(SearchTokenEditAction.Backspace) }

        assertEquals(emptyList(), state.tokens)
        assertNull(state.selectedTokenIndex)
    }

    @Test
    fun backspaceWithTextIsNotHandledByReducer() {
        val state = SearchTokenEditState(listOf(photos), text = "cat")

        assertSame(state, state.reduce(SearchTokenEditAction.Backspace))
    }

    @Test
    fun backspaceWithoutTokensIsNotHandled() {
        val state = SearchTokenEditState(emptyList<String>(), text = "")

        assertSame(state, state.reduce(SearchTokenEditAction.Backspace))
    }

    @Test
    fun backspaceDeletesExplicitlySelectedTokenEvenWithText() {
        val state = SearchTokenEditState(listOf(photos, favorites), text = "cat", selectedTokenIndex = 0)

        val next = state.reduce(SearchTokenEditAction.Backspace)

        assertEquals(listOf(favorites), next.tokens)
        assertEquals("cat", next.text)
        assertNull(next.selectedTokenIndex)
    }

    @Test
    fun typingDeselectsToken() {
        val state = SearchTokenEditState(listOf(photos), text = "", selectedTokenIndex = 0)

        val next = state.reduce(SearchTokenEditAction.TextChange("a"))

        assertEquals("a", next.text)
        assertEquals(listOf(photos), next.tokens)
        assertNull(next.selectedTokenIndex)
    }

    @Test
    fun textChangeWithoutSelectionOnlyUpdatesText() {
        val state = SearchTokenEditState(listOf(photos), text = "c")

        assertEquals(
            SearchTokenEditState(listOf(photos), "ca"),
            state.reduce(SearchTokenEditAction.TextChange("ca")),
        )
    }

    @Test
    fun selectTokenSelectsAndIgnoresInvalidIndex() {
        val state = SearchTokenEditState(listOf(photos, favorites), text = "")

        assertEquals(0, state.reduce(SearchTokenEditAction.SelectToken(0)).selectedTokenIndex)
        assertSame(state, state.reduce(SearchTokenEditAction.SelectToken(5)))
        assertSame(state, state.reduce(SearchTokenEditAction.SelectToken(-1)))
    }

    @Test
    fun clearSelectionDeselects() {
        val state = SearchTokenEditState(listOf(photos), text = "", selectedTokenIndex = 0)

        assertNull(state.reduce(SearchTokenEditAction.ClearSelection).selectedTokenIndex)
    }

    @Test
    fun removeTokenKeepsSelectionPointingAtSameToken() {
        val state = SearchTokenEditState(listOf(photos, favorites, "Videos"), text = "", selectedTokenIndex = 2)

        val next = state.reduce(SearchTokenEditAction.RemoveToken(0))

        assertEquals(listOf(favorites, "Videos"), next.tokens)
        assertEquals(1, next.selectedTokenIndex)
    }

    @Test
    fun removingSelectedTokenClearsSelection() {
        val state = SearchTokenEditState(listOf(photos, favorites), text = "", selectedTokenIndex = 1)

        val next = state.reduce(SearchTokenEditAction.RemoveToken(1))

        assertEquals(listOf(photos), next.tokens)
        assertNull(next.selectedTokenIndex)
    }

    @Test
    fun removeTokenIgnoresInvalidIndex() {
        val state = SearchTokenEditState(listOf(photos), text = "")

        assertSame(state, state.reduce(SearchTokenEditAction.RemoveToken(3)))
    }

    @Test
    fun insertTokenAppendsByDefaultAndDeselects() {
        val state = SearchTokenEditState(listOf(photos), text = "x", selectedTokenIndex = 0)

        val next = state.reduce(SearchTokenEditAction.InsertToken(favorites))

        assertEquals(listOf(photos, favorites), next.tokens)
        assertEquals("x", next.text)
        assertNull(next.selectedTokenIndex)
    }

    @Test
    fun insertTokenAtIndexIsCoercedIntoRange() {
        val state = SearchTokenEditState(listOf(photos), text = "")

        assertEquals(
            listOf(favorites, photos),
            state.reduce(SearchTokenEditAction.InsertToken(favorites, 0)).tokens,
        )
        assertEquals(
            listOf(photos, favorites),
            state.reduce(SearchTokenEditAction.InsertToken(favorites, 99)).tokens,
        )
    }

    @Test
    fun clearAllRemovesTextTokensAndSelection() {
        val state = SearchTokenEditState(listOf(photos), text = "x", selectedTokenIndex = 0)

        assertEquals(
            SearchTokenEditState(emptyList<String>(), ""),
            state.reduce(SearchTokenEditAction.ClearAll),
        )
    }

    @Test
    fun outOfRangeSelectionIsDroppedWhenNormalized() {
        assertNull(SearchTokenEditState(listOf(photos), "", selectedTokenIndex = 3).normalized().selectedTokenIndex)
        assertNull(normalizedSelectedTokenIndex(3, tokenCount = 1))
        assertNull(normalizedSelectedTokenIndex(null, tokenCount = 1))
        assertEquals(0, normalizedSelectedTokenIndex(0, tokenCount = 1))
    }

    @Test
    fun textChangeAppliesEvenWhenTheCallerHoldsAnOlderState() {
        // A caller that reduces twice against the same (stale) state must not lose the second edit.
        val state = SearchTokenEditState(listOf(photos), text = "a")

        state.reduce(SearchTokenEditAction.TextChange("ab"))
        val second = state.reduce(SearchTokenEditAction.TextChange("a"))

        assertEquals("a", second.text)
        assertEquals("ab", state.reduce(SearchTokenEditAction.TextChange("ab")).text)
    }

    private val sentinel = SearchTokenSentinel.toString()

    @Test
    fun sentinelShiftsSelectionAndComposition() {
        val field = TextFieldValue("ab", TextRange(1, 2), composition = TextRange(0, 2)).withSearchTokenSentinel()

        assertEquals(sentinel + "ab", field.text)
        assertEquals(TextRange(2, 3), field.selection)
        assertEquals(TextRange(1, 3), field.composition)
    }

    @Test
    fun deletingTheSentinelIsABackspaceAtStart() {
        assertEquals(
            SearchTokenFieldEdit.BackspaceAtStart,
            interpretSearchTokenFieldEdit(TextFieldValue("", TextRange.Zero), currentText = ""),
        )
        assertEquals(
            SearchTokenFieldEdit.BackspaceAtStart,
            interpretSearchTokenFieldEdit(TextFieldValue("cat", TextRange.Zero), currentText = "cat"),
        )
    }

    @Test
    fun regularEditsAreMappedToUserText() {
        assertEquals(
            SearchTokenFieldEdit.Value(TextFieldValue("ca", TextRange(2), composition = TextRange(0, 2))),
            interpretSearchTokenFieldEdit(
                TextFieldValue(sentinel + "ca", TextRange(3), composition = TextRange(1, 3)),
                currentText = "c",
            ),
        )
        // Caret before the sentinel is moved to the start of the user text.
        assertEquals(
            SearchTokenFieldEdit.Value(TextFieldValue("c", TextRange(0))),
            interpretSearchTokenFieldEdit(TextFieldValue(sentinel + "c", TextRange(0)), currentText = "c"),
        )
    }

    @Test
    fun textInsertedBeforeOrReplacingTheSentinelIsKept() {
        assertEquals(
            SearchTokenFieldEdit.Value(TextFieldValue("xc", TextRange(1))),
            interpretSearchTokenFieldEdit(TextFieldValue("x" + sentinel + "c", TextRange(1)), currentText = "c"),
        )
        // Select all + type replaces the sentinel too.
        assertEquals(
            SearchTokenFieldEdit.Value(TextFieldValue("d", TextRange(1))),
            interpretSearchTokenFieldEdit(TextFieldValue("d", TextRange(1)), currentText = "cat"),
        )
    }

    @Test
    fun sentinelVisualTransformationHidesTheSentinelFromTheCallersTransformation() {
        val transformed = SearchTokenSentinelVisualTransformation(PasswordVisualTransformation('*'))
            .filter(AnnotatedString(sentinel + "abc"))

        assertEquals(sentinel + "***", transformed.text.text)
        assertEquals(0, transformed.offsetMapping.originalToTransformed(0))
        assertEquals(3, transformed.offsetMapping.originalToTransformed(3))
        assertEquals(4, transformed.offsetMapping.transformedToOriginal(4))
    }

    @Test
    fun backspaceAtTextStartSelectsThenDeletesEvenWithText() {
        val state = SearchTokenEditState(listOf(photos, favorites), text = "cat")

        assertSame(state, state.reduce(SearchTokenEditAction.Backspace))
        val selected = state.reduce(SearchTokenEditAction.BackspaceAtTextStart)
        assertEquals(1, selected.selectedTokenIndex)
        val deleted = selected.reduce(SearchTokenEditAction.BackspaceAtTextStart)
        assertEquals(SearchTokenEditState(listOf(photos), "cat"), deleted)
    }
}
