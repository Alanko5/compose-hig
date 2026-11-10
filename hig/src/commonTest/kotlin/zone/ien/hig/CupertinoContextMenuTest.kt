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

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.unit.IntSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.click
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.rightClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import zone.ien.hig.theme.LocalColorScheme
import zone.ien.hig.theme.lightColorScheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCupertinoApi::class, ExperimentalTestApi::class, InternalCupertinoApi::class)
class CupertinoContextMenuTest {

    private class RecordingHaptics : HapticFeedback {
        val performed = mutableListOf<HapticFeedbackType>()
        override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
            performed += hapticFeedbackType
        }
    }

    private fun SemanticsNodeInteraction.showMenuAction() =
        fetchSemanticsNode().config.getOrElse(SemanticsActions.CustomActions) { emptyList() }
            .firstOrNull { it.label == CupertinoContextMenuDefaults.ShowMenuActionLabel }

    private fun androidx.compose.ui.test.ComposeUiTest.setMenu(
        state: CupertinoContextMenuState,
        enabled: Boolean = true,
        haptics: HapticFeedback = RecordingHaptics(),
        onCopy: () -> Unit = {},
        onContentClick: () -> Unit = {},
    ) = setContent {
        CompositionLocalProvider(
            LocalColorScheme provides lightColorScheme(),
            LocalHapticFeedback provides haptics,
        ) {
            Box(Modifier.size(400.dp, 800.dp)) {
                CupertinoContextMenuBox(
                    menuContent = {
                        MenuAction(onClick = onCopy) { CupertinoText(text = "Copy") }
                        MenuDivider()
                        MenuAction(onClick = {}, enabled = false) { CupertinoText(text = "Unavailable") }
                    },
                    modifier = Modifier.padding(40.dp).testTag("target"),
                    enabled = enabled,
                    state = state,
                ) {
                    Box(Modifier.size(160.dp, 80.dp).clickable(onClick = onContentClick))
                }
            }
        }
    }

    @Test
    fun accessibilityActionOpensMenuAndSelectingAnItemDismissesIt() = runComposeUiTest {
        val state = CupertinoContextMenuState()
        val haptics = RecordingHaptics()
        var copied = 0
        setMenu(state, haptics = haptics, onCopy = { copied++ })

        val action = onNodeWithTag("target").showMenuAction()
        runOnIdle { assertTrue(action!!.action()) }
        waitForIdle()

        assertTrue(state.isVisible)
        // Like UIKit, only the long-press lift performs the impact.
        assertEquals(emptyList(), haptics.performed)
        onNodeWithText("Copy").performClick()
        waitForIdle()

        assertEquals(1, copied)
        assertFalse(state.isVisible)
        onAllNodesWithText("Copy").assertCountEquals(0)
    }

    @Test
    fun longPressOpensMenuWithoutClickingTheContent() = runComposeUiTest {
        val state = CupertinoContextMenuState()
        val haptics = RecordingHaptics()
        var contentClicks = 0
        setMenu(state, haptics = haptics, onContentClick = { contentClicks++ })

        onNodeWithTag("target").performTouchInput { longClick() }
        waitForIdle()

        assertTrue(state.isVisible)
        assertEquals(0, contentClicks)
        assertEquals(listOf(CupertinoHapticFeedback.ImpactMedium), haptics.performed)
        // The anchor sits at the top-start of the window, so the menu opens below it, aligned to its start.
        val target = onNodeWithTag("target").getBoundsInRoot()
        val item = onNodeWithText("Copy").getBoundsInRoot()
        assertTrue(item.top > target.bottom, "menu item ${item.top} should be below the preview ${target.bottom}")
        assertTrue(item.left >= target.left, "menu should align with the preview's leading edge")
    }

    @Test
    fun tapStillReachesTheContent() = runComposeUiTest {
        val state = CupertinoContextMenuState()
        var contentClicks = 0
        setMenu(state, onContentClick = { contentClicks++ })

        onNodeWithTag("target").performClick()
        waitForIdle()

        assertEquals(1, contentClicks)
        assertFalse(state.isVisible)
    }

    @Test
    fun secondaryClickOpensMenu() = runComposeUiTest {
        val state = CupertinoContextMenuState()
        val haptics = RecordingHaptics()
        setMenu(state, haptics = haptics)

        onNodeWithTag("target").performMouseInput { rightClick() }
        waitForIdle()

        assertTrue(state.isVisible)
        onNodeWithText("Copy").assertExists()
        assertEquals(emptyList(), haptics.performed)
    }

    @Test
    fun disabledItemDoesNotDismiss() = runComposeUiTest {
        val state = CupertinoContextMenuState(initiallyVisible = true)
        setMenu(state)
        waitForIdle()

        onNodeWithText("Unavailable").performClick()
        waitForIdle()

        assertTrue(state.isVisible)
    }

    @Test
    fun dismissActionAndOutsideTapCloseTheMenu() = runComposeUiTest {
        val state = CupertinoContextMenuState(initiallyVisible = true)
        setMenu(state)
        waitForIdle()

        onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.Dismiss)).performSemanticsAction(SemanticsActions.Dismiss)
        waitForIdle()
        assertFalse(state.isVisible)
        onAllNodesWithText("Copy").assertCountEquals(0)

        runOnIdle { state.show() }
        waitForIdle()
        onNodeWithText("Copy").assertExists()
        onAllNodes(isRoot())[1].performTouchInput { click(bottomRight - Offset(5f, 5f)) }
        waitForIdle()
        assertFalse(state.isVisible)
    }

    @Test
    fun escapeKeyClosesTheMenu() = runComposeUiTest {
        val state = CupertinoContextMenuState(initiallyVisible = true)
        setMenu(state)
        waitForIdle()

        onNodeWithText("Copy").requestFocus()
        onNodeWithText("Copy").performKeyInput { pressKey(Key.Escape) }
        waitForIdle()

        assertFalse(state.isVisible)
    }

    @Test
    fun disabledBoxHasNoActionAndIgnoresLongPress() = runComposeUiTest {
        val state = CupertinoContextMenuState()
        setMenu(state, enabled = false)

        assertEquals(null, onNodeWithTag("target").showMenuAction())
        onNodeWithTag("target").performTouchInput { longClick() }
        waitForIdle()

        assertFalse(state.isVisible)
        onAllNodesWithText("Copy").assertCountEquals(0)
    }

    @Test
    fun showMenuActionIsOnTheMergedNodeOfTextContent() = runComposeUiTest {
        val state = CupertinoContextMenuState()
        setContent {
            CompositionLocalProvider(LocalColorScheme provides lightColorScheme()) {
                CupertinoContextMenuBox(
                    menuContent = { MenuAction(onClick = {}) { CupertinoText(text = "Copy") } },
                    state = state,
                ) {
                    Column {
                        CupertinoText(text = "Note title")
                        CupertinoText(text = "Note body")
                    }
                }
            }
        }

        // A screen reader focuses the merged node that carries the content's text; the action must be there.
        val node = onNodeWithText("Note title")
        val config = node.fetchSemanticsNode().config
        assertEquals(
            listOf("Note title", "Note body"),
            config[SemanticsProperties.Text].map { it.text },
        )
        assertTrue(config.contains(SemanticsActions.OnLongClick))
        runOnIdle { assertTrue(node.showMenuAction()!!.action()) }
        waitForIdle()

        assertTrue(state.isVisible)
        onNodeWithText("Copy").assertExists()
    }

    @Test
    fun partiallyScrolledContentLiftsAtItsFullSize() = runComposeUiTest {
        val state = CupertinoContextMenuState()
        val scrollState = ScrollState(0)
        var contentSize = IntSize.Zero
        setContent {
            CompositionLocalProvider(LocalColorScheme provides lightColorScheme()) {
                Column(Modifier.size(400.dp, 300.dp).verticalScroll(scrollState)) {
                    Spacer(Modifier.height(200.dp))
                    CupertinoContextMenuBox(
                        menuContent = { MenuAction(onClick = {}) { CupertinoText(text = "Copy") } },
                        state = state,
                    ) {
                        Box(Modifier.size(160.dp, 100.dp).onSizeChanged { contentSize = it })
                    }
                    Spacer(Modifier.height(1000.dp))
                }
            }
        }
        // Scroll so that the top half of the box is hidden under the viewport's top edge.
        val scrollPx = with(density) { 250.dp.roundToPx() }
        runOnIdle { scrollState.dispatchRawDelta(scrollPx.toFloat()) }
        waitForIdle()

        runOnIdle { state.show() }
        waitForIdle()

        assertEquals(contentSize, onNodeWithTag(ContextMenuPreviewTestTag).fetchSemanticsNode().size)
    }

    @Test
    fun initiallyVisibleMenuLiftsTheDrawnContent() = runComposeUiTest {
        val state = CupertinoContextMenuState(initiallyVisible = true)
        setContent {
            CompositionLocalProvider(LocalColorScheme provides lightColorScheme()) {
                Box(Modifier.size(400.dp, 800.dp)) {
                    CupertinoContextMenuBox(
                        menuContent = { MenuAction(onClick = {}) { CupertinoText(text = "Copy") } },
                        modifier = Modifier.padding(40.dp),
                        state = state,
                    ) {
                        Box(Modifier.size(160.dp, 80.dp).background(Color.Red))
                    }
                }
            }
        }
        waitForIdle()
        mainClock.advanceTimeBy(1_000)
        waitForIdle()

        val preview = onNodeWithTag(ContextMenuPreviewTestTag)
        assertEquals(
            IntSize(with(density) { 160.dp.roundToPx() }, with(density) { 80.dp.roundToPx() }),
            preview.fetchSemanticsNode().size,
        )
        val pixels = preview.captureToImage().toPixelMap()
        assertEquals(Color.Red, pixels[pixels.width / 2, pixels.height / 2])
    }

    @Test
    fun switchingPreviewToNullWhileOpenKeepsTheMenu() = runComposeUiTest {
        val state = CupertinoContextMenuState(initiallyVisible = true)
        var customPreview by mutableStateOf(true)
        setContent {
            CompositionLocalProvider(LocalColorScheme provides lightColorScheme()) {
                CupertinoContextMenuBox(
                    menuContent = { MenuAction(onClick = {}) { CupertinoText(text = "Copy") } },
                    state = state,
                    preview = if (customPreview) ({ Box(Modifier.size(100.dp)) }) else null,
                ) {
                    Box(Modifier.size(160.dp, 80.dp))
                }
            }
        }
        waitForIdle()
        onNodeWithText("Copy").assertExists()

        customPreview = false
        waitForIdle()
        mainClock.advanceTimeBy(1_000)
        waitForIdle()

        assertTrue(state.isVisible)
        onNodeWithText("Copy").assertExists()
    }

    @Test
    fun dropdownMenuActionsDoNotDismissTheMenuThemselves() = runComposeUiTest {
        var clicks = 0
        var dismissRequests = 0
        setContent {
            CompositionLocalProvider(LocalColorScheme provides lightColorScheme()) {
                Box(Modifier.size(400.dp, 800.dp)) {
                    CupertinoDropdownMenu(expanded = true, onDismissRequest = { dismissRequests++ }) {
                        MenuAction(onClick = { clicks++ }) { CupertinoText(text = "Item") }
                    }
                }
            }
        }
        waitForIdle()

        onNodeWithText("Item").performClick()
        waitForIdle()

        assertEquals(1, clicks)
        assertEquals(0, dismissRequests)
        onNodeWithText("Item").assertExists()
    }
}
