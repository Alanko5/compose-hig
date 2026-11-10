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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertRangeInfoEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCupertinoApi::class, ExperimentalTestApi::class)
class CupertinoColorPickerTest {
    private val red = Color.Red

    @Test
    fun gridSwatchesExposeDescriptionAndSelectionAndKeepOpacity() = runComposeUiTest {
        var selection by mutableStateOf(Color.White.copy(alpha = 0.5f))
        setContent {
            TestTheme {
                CupertinoColorPickerContent(
                    selection = selection,
                    onSelectionChange = { selection = it },
                    onDismissRequest = {},
                    mode = CupertinoColorPickerMode.Grid,
                    onModeChange = {},
                    supportsOpacity = true,
                    title = CupertinoColorPickerDefaults.Title,
                    recentColors = emptyList(),
                    onRecentColorsChange = null,
                    colors = CupertinoColorPickerDefaults.colors(),
                    modifier = Modifier.width(390.dp),
                )
            }
        }

        onNodeWithContentDescription("White, #FFFFFF").assertIsSelected()
        onNodeWithContentDescription("Black, #000000").assertIsNotSelected().performClick()

        runOnIdle {
            assertEquals("#000000", selection.toHexString())
            assertEquals(50, selection.opacityPercent())
        }
        onNodeWithContentDescription("Black, #000000").assertIsSelected()
    }

    @Test
    fun segmentedControlSwitchesToSlidersThatExposeAndAcceptValues() = runComposeUiTest {
        var selection by mutableStateOf(red)
        var mode by mutableStateOf(CupertinoColorPickerMode.Grid)
        setContent {
            TestTheme {
                CupertinoColorPickerContent(
                    selection = selection,
                    onSelectionChange = { selection = it },
                    onDismissRequest = {},
                    mode = mode,
                    onModeChange = { mode = it },
                    supportsOpacity = true,
                    title = CupertinoColorPickerDefaults.Title,
                    recentColors = emptyList(),
                    onRecentColorsChange = null,
                    colors = CupertinoColorPickerDefaults.colors(),
                    modifier = Modifier.width(390.dp),
                )
            }
        }

        onNodeWithText("Sliders").performClick()
        runOnIdle { assertEquals(CupertinoColorPickerMode.Sliders, mode) }

        val green = onNode(hasContentDescription("Green") and hasProgressBarRangeInfo(ProgressBarRangeInfo(0f, 0f..255f, 0)))
        green.assertRangeInfoEquals(ProgressBarRangeInfo(0f, 0f..255f, 0))
        green.performSemanticsAction(SemanticsActions.SetProgress) { it(128f) }
        runOnIdle { assertEquals("#FF8000", selection.toHexString()) }

        onNode(hasContentDescription("Opacity")).assertRangeInfoEquals(ProgressBarRangeInfo(100f, 0f..100f, 0))
        onNode(hasContentDescription("Opacity")).performSemanticsAction(SemanticsActions.SetProgress) { it(25f) }
        runOnIdle {
            assertEquals("#FF8000", selection.toHexString())
            assertEquals(25, selection.opacityPercent())
        }
    }

    @Test
    fun hexFieldAppliesValidInputOnly() = runComposeUiTest {
        var selection by mutableStateOf(red)
        setContent {
            TestTheme {
                CupertinoColorPickerContent(
                    selection = selection,
                    onSelectionChange = { selection = it },
                    onDismissRequest = {},
                    mode = CupertinoColorPickerMode.Sliders,
                    onModeChange = {},
                    supportsOpacity = false,
                    title = CupertinoColorPickerDefaults.Title,
                    recentColors = emptyList(),
                    onRecentColorsChange = null,
                    colors = CupertinoColorPickerDefaults.colors(),
                    modifier = Modifier.width(390.dp),
                )
            }
        }

        val field = onNode(hasSetTextAction())
        field.performTextReplacement("12")
        runOnIdle { assertEquals("#FF0000", selection.toHexString()) }
        field.performTextReplacement("#00ff7f")
        runOnIdle { assertEquals("#00FF7F", selection.toHexString()) }
        // Without opacity support no opacity slider is shown.
        onNode(hasContentDescription("Opacity")).assertDoesNotExist()
    }

    private fun ComposeUiTest.setSlidersContent(
        selection: () -> Color,
        onSelectionChange: (Color) -> Unit,
        supportsOpacity: Boolean = false,
    ) {
        setContent {
            TestTheme {
                CupertinoColorPickerContent(
                    selection = selection(),
                    onSelectionChange = onSelectionChange,
                    onDismissRequest = {},
                    mode = CupertinoColorPickerMode.Sliders,
                    onModeChange = {},
                    supportsOpacity = supportsOpacity,
                    title = CupertinoColorPickerDefaults.Title,
                    recentColors = emptyList(),
                    onRecentColorsChange = null,
                    colors = CupertinoColorPickerDefaults.colors(),
                    modifier = Modifier.width(390.dp),
                )
            }
        }
    }

    @Test
    fun hexFieldAcceptsSixDigitsTypedOneAtATime() = runComposeUiTest {
        var selection by mutableStateOf(red)
        setSlidersContent({ selection }, { selection = it })

        val field = onNode(hasSetTextAction())
        field.performSemanticsAction(SemanticsActions.RequestFocus)
        field.performTextClearance()
        for ((index, char) in "123456".withIndex()) {
            field.performTextInput(char.toString())
            if (index < 5) {
                // Partial values (including the 3-digit short form) are not applied while typing.
                runOnIdle { assertEquals("#FF0000", selection.toHexString()) }
                field.assert(hasText("123456".take(index + 1)))
            }
        }
        runOnIdle { assertEquals("#123456", selection.toHexString()) }
        field.assert(hasText("123456"))
    }

    @Test
    fun hexFieldBackspacingToThreeDigitsKeepsColorUntilDone() = runComposeUiTest {
        var selection by mutableStateOf(red)
        setSlidersContent({ selection }, { selection = it })

        val field = onNode(hasSetTextAction())
        field.assert(hasText("FF0000"))
        field.performSemanticsAction(SemanticsActions.RequestFocus)
        for (text in listOf("FF000", "FF00", "FF0")) {
            field.performTextReplacement(text)
            runOnIdle { assertEquals("#FF0000", selection.toHexString()) }
            field.assert(hasText(text))
        }
        // The short form is expanded only when editing ends.
        field.performImeAction()
        runOnIdle { assertEquals("#FFFF00", selection.toHexString()) }
        field.assert(hasText("FFFF00"))
    }

    @Test
    fun hexFieldRestoresSelectionWhenEditingEndsWithInvalidValue() = runComposeUiTest {
        var selection by mutableStateOf(red)
        setSlidersContent({ selection }, { selection = it })

        val field = onNode(hasSetTextAction())
        field.performSemanticsAction(SemanticsActions.RequestFocus)
        field.performTextReplacement("12")
        field.assert(hasText("12"))
        field.performImeAction()
        runOnIdle { assertEquals("#FF0000", selection.toHexString()) }
        field.assert(hasText("FF0000"))
    }

    @Test
    fun colorWellOpensSheetAndIsInertWhenDisabled() = runComposeUiTest {
        var enabled by mutableStateOf(true)
        setContent {
            TestTheme {
                CupertinoColorWell(
                    selection = red,
                    onSelectionChange = {},
                    enabled = enabled,
                )
            }
        }

        val well = onNodeWithContentDescription(CupertinoColorPickerDefaults.Title)
        well.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Red, #FF0000"))
        well.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        well.assertIsEnabled().assertHasClickAction()
        well.performClick()
        waitForIdle()
        onNodeWithText(CupertinoColorPickerDefaults.Title).assertExists()
        onNodeWithContentDescription("Close").performClick()
        waitForIdle()
        onNodeWithText(CupertinoColorPickerDefaults.Title).assertDoesNotExist()

        enabled = false
        waitForIdle()
        well.assertIsNotEnabled()
        well.performClick()
        waitForIdle()
        onNodeWithText(CupertinoColorPickerDefaults.Title).assertDoesNotExist()
    }

    @Test
    fun disabledColorPickerRowDoesNotOpenSheet() = runComposeUiTest {
        setContent {
            TestTheme {
                CupertinoColorPicker(
                    selection = red,
                    onSelectionChange = {},
                    enabled = false,
                    modifier = Modifier.width(390.dp),
                ) {
                    CupertinoText("Tint")
                }
            }
        }

        val row = onNode(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Red, #FF0000"))
        row.assertIsNotEnabled()
        row.performClick()
        waitForIdle()
        onNodeWithText(CupertinoColorPickerDefaults.Title).assertDoesNotExist()
    }

    @Test
    fun opaquePickerShowsTranslucentSelectionAsOpaque() = runComposeUiTest {
        setSlidersContent({ red.copy(alpha = 0.5f) }, {}, supportsOpacity = false)
        onNodeWithContentDescription("Current color, Red, #FF0000").assertExists()
        onNode(hasContentDescription("Red")).assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "255"))
    }

    @Test
    fun spectrumExposesAdjustableActions() = runComposeUiTest {
        var selection by mutableStateOf(red)
        setContent {
            TestTheme {
                CupertinoColorPickerContent(
                    selection = selection,
                    onSelectionChange = { selection = it },
                    onDismissRequest = {},
                    mode = CupertinoColorPickerMode.Spectrum,
                    onModeChange = {},
                    supportsOpacity = true,
                    title = CupertinoColorPickerDefaults.Title,
                    recentColors = emptyList(),
                    onRecentColorsChange = null,
                    colors = CupertinoColorPickerDefaults.colors(),
                    modifier = Modifier.width(390.dp),
                )
            }
        }

        val spectrum = onNodeWithContentDescription("Color spectrum")
        spectrum.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Red, #FF0000"))
        val actions = spectrum.fetchSemanticsNode().config[SemanticsActions.CustomActions]
        assertEquals(listOf("Increase hue", "Decrease hue", "Lighter", "Darker"), actions.map { it.label })

        // Red sits at (0, 0.5). Darker moves y by 0.1 to brightness 0.8.
        runOnIdle { actions.first { it.label == "Darker" }.action() }
        runOnIdle { assertEquals("#CC0000", selection.toHexString()) }
        // Increase hue moves x by 1/24, a hue of 15 degrees at the same brightness.
        runOnIdle { actions.first { it.label == "Increase hue" }.action() }
        runOnIdle { assertEquals("#CC3300", selection.toHexString()) }
    }

    @Test
    fun recentColorsAreHoistedAndSelectable() = runComposeUiTest {
        var selection by mutableStateOf(red)
        var recent by mutableStateOf(listOf(Color.Blue))
        setContent {
            TestTheme {
                CupertinoColorPickerContent(
                    selection = selection,
                    onSelectionChange = { selection = it },
                    onDismissRequest = {},
                    mode = CupertinoColorPickerMode.Grid,
                    onModeChange = {},
                    supportsOpacity = true,
                    title = CupertinoColorPickerDefaults.Title,
                    recentColors = recent,
                    onRecentColorsChange = { recent = it },
                    colors = CupertinoColorPickerDefaults.colors(),
                    modifier = Modifier.width(390.dp),
                )
            }
        }

        onNodeWithContentDescription("Add to recent colors").performClick()
        runOnIdle { assertEquals(listOf(Color.Red, Color.Blue), recent) }
        onNodeWithContentDescription("Recent color, Blue, #0000FF").performClick()
        runOnIdle { assertEquals("#0000FF", selection.toHexString()) }
    }

    @Test
    fun colorPickerRowOpensSheetAndCloseButtonDismissesIt() = runComposeUiTest {
        var selection by mutableStateOf(red)
        setContent {
            TestTheme {
                CupertinoColorPicker(
                    selection = selection,
                    onSelectionChange = { selection = it },
                    modifier = Modifier.width(390.dp),
                ) {
                    CupertinoText("Tint")
                }
            }
        }

        onNodeWithText("Tint").assertExists()
        onNodeWithText(CupertinoColorPickerDefaults.Title).assertDoesNotExist()
        onNode(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Red, #FF0000")).performClick()
        waitForIdle()
        onNodeWithText(CupertinoColorPickerDefaults.Title).assertExists()
        onNodeWithContentDescription("Close").performClick()
        waitForIdle()
        onNodeWithText(CupertinoColorPickerDefaults.Title).assertDoesNotExist()
    }
}

// CupertinoTheme applies the status bar appearance through LocalUIViewController on iOS, which
// runComposeUiTest does not provide, so it cannot be used in common tests. The components read
// the default theme locals without it.
@Composable
private fun TestTheme(content: @Composable () -> Unit) = content()
