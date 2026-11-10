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

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCupertinoApi::class, ExperimentalTestApi::class)
class CupertinoSearchTextFieldTokensTest {
    private val photos = CupertinoSearchToken("Photos")
    private val favorites = CupertinoSearchToken("Favorites")

    @Test
    fun backspaceOnEmptyTextSelectsThenDeletesLastToken() = runComposeUiTest {
        var text by mutableStateOf("")
        var tokens by mutableStateOf(listOf(photos, favorites))

        setContent {
            TestTheme {
                CupertinoSearchTextField(
                    value = text,
                    onValueChange = { text = it },
                    tokens = tokens,
                    onTokensChange = { tokens = it },
                    modifier = Modifier.width(360.dp),
                )
            }
        }

        val field = onNode(hasSetTextAction())
        field.performSemanticsAction(SemanticsActions.RequestFocus) { it() }

        field.performKeyInput { pressKey(Key.Backspace) }
        waitForIdle()
        assertEquals(listOf(photos, favorites), tokens)
        onNodeWithContentDescription("Favorites, token").assertIsSelected()

        field.performKeyInput { pressKey(Key.Backspace) }
        waitForIdle()
        assertEquals(listOf(photos), tokens)
        onNodeWithContentDescription("Photos, token").assertIsNotSelected()
    }

    @Test
    fun typingDeselectsSelectedToken() = runComposeUiTest {
        var text by mutableStateOf("")
        var tokens by mutableStateOf(listOf(photos))

        setContent {
            TestTheme {
                CupertinoSearchTextField(
                    value = text,
                    onValueChange = { text = it },
                    tokens = tokens,
                    onTokensChange = { tokens = it },
                    modifier = Modifier.width(360.dp),
                )
            }
        }

        val field = onNode(hasSetTextAction())
        field.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        field.performKeyInput { pressKey(Key.Backspace) }
        onNodeWithContentDescription("Photos, token").assertIsSelected()

        field.performTextInput("cat")
        waitForIdle()

        assertEquals("cat", text)
        assertEquals(listOf(photos), tokens)
        onNodeWithContentDescription("Photos, token").assertIsNotSelected()
    }

    @Test
    fun backspaceWithTextEditsTextOnly() = runComposeUiTest {
        var text by mutableStateOf("ab")
        var tokens by mutableStateOf(listOf(photos))

        setContent {
            TestTheme {
                CupertinoSearchTextField(
                    value = text,
                    onValueChange = { text = it },
                    tokens = tokens,
                    onTokensChange = { tokens = it },
                    modifier = Modifier.width(360.dp),
                )
            }
        }

        val field = onNode(hasSetTextAction())
        field.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        field.performKeyInput { pressKey(Key.MoveEnd) }
        field.performKeyInput { pressKey(Key.Backspace) }
        waitForIdle()

        assertEquals("a", text)
        assertEquals(listOf(photos), tokens)
        onNodeWithContentDescription("Photos, token").assertIsNotSelected()
    }

    @Test
    fun tokenExposesRemoveCustomAction() = runComposeUiTest {
        var tokens by mutableStateOf(listOf(photos, favorites))

        setContent {
            TestTheme {
                CupertinoSearchTextField(
                    value = "",
                    onValueChange = {},
                    tokens = tokens,
                    onTokensChange = { tokens = it },
                    modifier = Modifier.width(360.dp),
                )
            }
        }

        val token = onNodeWithContentDescription("Photos, token")
        token.assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Selected))
        val actions = token.fetchSemanticsNode().config[SemanticsActions.CustomActions]
        runOnIdle { actions.single { it.label == "Remove Photos" }.action() }
        waitForIdle()

        assertEquals(listOf(favorites), tokens)
    }

    @Test
    fun tappingTokenSelectsIt() = runComposeUiTest {
        setContent {
            TestTheme {
                CupertinoSearchTextField(
                    value = "",
                    onValueChange = {},
                    tokens = listOf(photos, favorites),
                    onTokensChange = {},
                    modifier = Modifier.width(360.dp),
                )
            }
        }

        onNodeWithContentDescription("Photos, token").performClick()
        onNodeWithContentDescription("Photos, token").assertIsSelected()
        onNodeWithContentDescription("Favorites, token").assertIsNotSelected()
    }

    @Test
    fun clearButtonRemovesTextAndTokens() = runComposeUiTest {
        var text by mutableStateOf("cat")
        var tokens by mutableStateOf(listOf(photos))

        setContent {
            TestTheme {
                CupertinoSearchTextField(
                    value = text,
                    onValueChange = { text = it },
                    tokens = tokens,
                    onTokensChange = { tokens = it },
                    modifier = Modifier.width(360.dp),
                )
            }
        }

        onNodeWithContentDescription("검색어 지우기").performClick()
        waitForIdle()

        assertEquals("", text)
        assertEquals(emptyList(), tokens)
    }

    private fun SemanticsNodeInteraction.editableText(): String =
        fetchSemanticsNode().config[SemanticsProperties.EditableText].text

    /** Replaces the field text the way a soft keyboard edit would arrive: as a value change, not a key event. */
    private fun SemanticsNodeInteraction.setTextNow(text: String) =
        fetchSemanticsNode().config[SemanticsActions.SetText].action!!(AnnotatedString(text))

    @Test
    fun softKeyboardDeletionWithoutKeyEventsSelectsThenDeletesLastToken() = runComposeUiTest {
        var text by mutableStateOf("")
        var tokens by mutableStateOf(listOf(photos, favorites))

        setContent {
            TestTheme {
                CupertinoSearchTextField(
                    value = text,
                    onValueChange = { text = it },
                    tokens = tokens,
                    onTokensChange = { tokens = it },
                    modifier = Modifier.width(360.dp),
                )
            }
        }

        val field = onNode(hasSetTextAction())
        field.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        // The field keeps the sentinel in front of the (empty) text while tokens are present.
        assertEquals(SearchTokenSentinel.toString(), field.editableText())

        // A soft-keyboard backspace deletes the sentinel: no key event, just an edit to "".
        runOnIdle { field.setTextNow("") }
        waitForIdle()
        assertEquals("", text)
        assertEquals(listOf(photos, favorites), tokens)
        onNodeWithContentDescription("Favorites, token").assertIsSelected()
        assertEquals(SearchTokenSentinel.toString(), field.editableText())

        runOnIdle { field.setTextNow("") }
        waitForIdle()
        assertEquals("", text)
        assertEquals(listOf(photos), tokens)

        // Typing after the sentinel only reports the user's text.
        runOnIdle { field.setTextNow(SearchTokenSentinel + "cat") }
        waitForIdle()
        assertEquals("cat", text)
        assertEquals(listOf(photos), tokens)
    }

    @Test
    fun editsWithinOneFrameAreNotDropped() = runComposeUiTest {
        var text by mutableStateOf("a")
        var tokens by mutableStateOf(listOf(photos))

        setContent {
            TestTheme {
                CupertinoSearchTextField(
                    value = text,
                    onValueChange = { text = it },
                    tokens = tokens,
                    onTokensChange = { tokens = it },
                    modifier = Modifier.width(360.dp),
                )
            }
        }

        val field = onNode(hasSetTextAction())
        runOnIdle {
            field.setTextNow(SearchTokenSentinel + "ab")
            field.setTextNow(SearchTokenSentinel + "a")
        }
        waitForIdle()

        assertEquals("a", text)
    }

    @Test
    fun readOnlyFieldDoesNotLetTokensBeSelectedOrRemoved() = runComposeUiTest {
        var tokens by mutableStateOf(listOf(photos, favorites))

        setContent {
            TestTheme {
                CupertinoSearchTextField(
                    value = "",
                    onValueChange = {},
                    tokens = tokens,
                    onTokensChange = { tokens = it },
                    readOnly = true,
                    modifier = Modifier.width(360.dp),
                )
            }
        }

        val token = onNodeWithContentDescription("Photos, token")
        assertFalse(token.fetchSemanticsNode().config.contains(SemanticsActions.CustomActions))
        assertTrue(token.fetchSemanticsNode().config.contains(SemanticsProperties.Disabled))
        token.performClick()
        waitForIdle()
        token.assertIsNotSelected()

        val field = onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.EditableText))
        field.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        field.performKeyInput { pressKey(Key.Backspace) }
        waitForIdle()
        onNodeWithContentDescription("Favorites, token").assertIsNotSelected()
        assertEquals(listOf(photos, favorites), tokens)
        // No sentinel in a field the user cannot edit.
        assertEquals("", field.editableText())
    }

    @Test
    fun tappingTokenInUnfocusedFieldFocusesTheField() = runComposeUiTest {
        var tokens by mutableStateOf(listOf(photos, favorites))
        setContent {
            TestTheme {
                CupertinoSearchTextField(
                    value = "",
                    onValueChange = {},
                    tokens = tokens,
                    onTokensChange = { tokens = it },
                    modifier = Modifier.width(360.dp),
                )
            }
        }

        onNodeWithContentDescription("Photos, token").performClick()
        waitForIdle()

        onNode(hasSetTextAction()).assertIsFocused()
        onNodeWithContentDescription("Photos, token").assertIsSelected()
        onNode(hasSetTextAction()).performKeyInput { pressKey(Key.Backspace) }
        waitForIdle()
        assertEquals(listOf(favorites), tokens)
    }

    @Test
    fun tokenChipsKeepTheirStateByKeyWhenAnEarlierTokenIsRemoved() = runComposeUiTest {
        var tokens by mutableStateOf(listOf(photos, favorites))
        var nextId = 0
        val ids = mutableMapOf<String, Int>()

        setContent {
            TestTheme {
                CupertinoSearchTextField(
                    value = "",
                    onValueChange = {},
                    tokens = tokens,
                    onTokensChange = { tokens = it },
                    modifier = Modifier.width(360.dp),
                ) { token, selected ->
                    val id = remember { nextId++ }
                    SideEffect { ids[token.text] = id }
                    CupertinoSearchTokenDefaults.Token(token = token, selected = selected, enabled = true)
                }
            }
        }
        waitForIdle()
        val favoritesId = ids.getValue("Favorites")

        tokens = listOf(favorites)
        waitForIdle()

        assertEquals(favoritesId, ids.getValue("Favorites"))
    }

    @Test
    fun accessibilityStringsCanBeOverridden() = runComposeUiTest {
        var tokens by mutableStateOf(listOf(photos))
        setContent {
            TestTheme {
                CupertinoSearchTextField(
                    value = "",
                    onValueChange = {},
                    tokens = tokens,
                    onTokensChange = { tokens = it },
                    tokenContentDescription = { "Filter ${it.text}" },
                    tokenRemoveActionLabel = { "Delete filter ${it.text}" },
                    tokenSelectActionLabel = "Pick",
                    modifier = Modifier.width(360.dp),
                )
            }
        }

        val token = onNodeWithContentDescription("Filter Photos")
        val config = token.fetchSemanticsNode().config
        assertEquals("Pick", config[SemanticsActions.OnClick].label)
        runOnIdle { config[SemanticsActions.CustomActions].single { it.label == "Delete filter Photos" }.action() }
        waitForIdle()
        assertEquals(emptyList(), tokens)
    }

    @Test
    fun defaultCancelButtonClearsTextAndTokens() = runComposeUiTest {
        var text by mutableStateOf("cat")
        var tokens by mutableStateOf(listOf(photos))

        setContent {
            TestTheme {
                CupertinoSearchTextField(
                    value = text,
                    onValueChange = { text = it },
                    tokens = tokens,
                    onTokensChange = { tokens = it },
                    modifier = Modifier.width(360.dp),
                )
            }
        }

        onNode(hasSetTextAction()).performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        waitForIdle()
        onNodeWithText("Cancel").performClick()
        waitForIdle()

        assertEquals("", text)
        assertEquals(emptyList(), tokens)
    }

    @Test
    fun legacyOverloadIgnoresBackspaceAndKeepsItsClearButton() = runComposeUiTest {
        var text by mutableStateOf("")
        var changes = 0

        setContent {
            TestTheme {
                CupertinoSearchTextField(
                    value = text,
                    onValueChange = {
                        changes++
                        text = it
                    },
                    modifier = Modifier.width(360.dp),
                )
            }
        }

        val field = onNode(hasSetTextAction())
        field.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        field.performKeyInput { pressKey(Key.Backspace) }
        waitForIdle()
        assertEquals(0, changes)
        // No sentinel without tokens.
        assertEquals("", field.editableText())

        field.performTextInput("cat")
        waitForIdle()
        assertEquals("cat", text)
        assertTrue(changes > 0)

        onNodeWithContentDescription("검색어 지우기").performClick()
        waitForIdle()
        assertEquals("", text)
    }

    @Test
    fun backspaceAtTheStartOfNonEmptyTextSelectsTheLastToken() = runComposeUiTest {
        var text by mutableStateOf("cat")
        var tokens by mutableStateOf(listOf(photos, favorites))

        setContent {
            TestTheme {
                CupertinoSearchTextField(
                    value = text,
                    onValueChange = { text = it },
                    tokens = tokens,
                    onTokensChange = { tokens = it },
                    modifier = Modifier.width(360.dp),
                )
            }
        }

        val field = onNode(hasSetTextAction())
        field.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        field.performKeyInput { pressKey(Key.MoveHome) }
        field.performKeyInput { pressKey(Key.Backspace) }
        waitForIdle()

        assertEquals("cat", text)
        onNodeWithContentDescription("Favorites, token").assertIsSelected()
        assertEquals(SearchTokenSentinel + "cat", field.editableText())
    }
}

// CupertinoTheme applies the status bar appearance through LocalUIViewController on iOS, which
// runComposeUiTest does not provide, so it cannot be used in common tests. The components read
// the default theme locals without it.
@Composable
private fun TestTheme(content: @Composable () -> Unit) = content()
