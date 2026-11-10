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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextLayoutResult
import zone.ien.hig.icons.CupertinoIcons
import zone.ien.hig.icons.outlined.ChevronBackward
import zone.ien.hig.icons.outlined.Heart
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCupertinoApi::class, ExperimentalTestApi::class)
class CupertinoLabelTest {
    @Composable
    private fun SlotLabel(style: CupertinoLabelStyle = CupertinoLabelStyle.Automatic) {
        CupertinoLabel(
            title = { CupertinoText("Favorites") },
            icon = { CupertinoIcon(CupertinoIcons.Default.Heart, contentDescription = "Heart") },
            style = style,
            modifier = Modifier.testTag("label"),
        )
    }

    @Test
    fun titleAndIconHidesIconFromAccessibility() = runComposeUiTest {
        setContent { SlotLabel(CupertinoLabelStyle.TitleAndIcon) }
        onNodeWithText("Favorites").assertIsDisplayed()
        onAllNodesWithContentDescription("Heart").assertCountEquals(0)
    }

    @Test
    fun titleOnlyDoesNotComposeTheIcon() = runComposeUiTest {
        var iconComposed = false
        setContent {
            CupertinoLabel(
                title = { CupertinoText("Favorites") },
                icon = { iconComposed = true },
                style = CupertinoLabelStyle.TitleOnly,
            )
        }
        onNodeWithText("Favorites").assertIsDisplayed()
        assertEquals(false, iconComposed)
    }

    @Test
    fun iconOnlyStringLabelUsesTitleAsContentDescription() = runComposeUiTest {
        setContent {
            CupertinoLabel(text = "Favorites", icon = CupertinoIcons.Default.Heart, style = CupertinoLabelStyle.IconOnly)
        }
        onNodeWithContentDescription("Favorites").assertIsDisplayed()
        onAllNodesWithText("Favorites", useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    fun iconOnlySlotLabelKeepsHiddenTitleWithinIconBounds() = runComposeUiTest {
        setContent { SlotLabel(CupertinoLabelStyle.IconOnly) }
        val label = onNodeWithTag("label").getBoundsInRoot()
        val title = onNodeWithText("Favorites", useUnmergedTree = true).getBoundsInRoot()
        onNodeWithText("Favorites").assertExists()
        assertTrue(title.right <= label.right && title.bottom <= label.bottom)
        onAllNodesWithContentDescription("Heart").assertCountEquals(0)
    }

    @Test
    fun containerStyleIsAdoptedUnlessOverridden() = runComposeUiTest {
        setContent {
            ProvideCupertinoLabelStyle(CupertinoLabelStyle.IconOnly) {
                Column {
                    CupertinoLabel(text = "Share", icon = CupertinoIcons.Default.Heart)
                    CupertinoLabel(
                        text = "Edit",
                        icon = CupertinoIcons.Default.Heart,
                        style = CupertinoLabelStyle.TitleAndIcon,
                    )
                }
            }
        }
        onNodeWithContentDescription("Share").assertIsDisplayed()
        onNodeWithText("Edit").assertIsDisplayed()
    }

    @Test
    fun iconLeadsTheTitleInLtr() = runComposeUiTest {
        var icon = Rect.Zero
        setContent {
            CupertinoLabel(
                title = { CupertinoText("Favorites", modifier = Modifier.testTag("title")) },
                icon = { Box(Modifier.size(20.dp).onGloballyPositioned { icon = it.boundsInRoot() }) },
            )
        }
        val title = onNodeWithTag("title", useUnmergedTree = true).getBoundsInRoot()
        with(density) { assertTrue(icon.right <= title.left.toPx()) }
    }

    @Test
    fun iconIsPlacedAfterTheTitleInRtl() = runComposeUiTest {
        var icon = Rect.Zero
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                CupertinoLabel(
                    title = { CupertinoText("Favorites", modifier = Modifier.testTag("title")) },
                    icon = { Box(Modifier.size(20.dp).onGloballyPositioned { icon = it.boundsInRoot() }) },
                )
            }
        }
        val title = onNodeWithTag("title", useUnmergedTree = true).getBoundsInRoot()
        with(density) { assertTrue(icon.left >= title.right.toPx()) }
    }

    @Test
    fun centerLayoutMatchesTheSwipeActionRow() = runComposeUiTest {
        var legacyIcon = Rect.Zero
        var labelIcon = Rect.Zero
        setContent {
            Column {
                Box(Modifier.testTag("legacy")) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        SwipeIcon { legacyIcon = it }
                        CupertinoText("Delete", fontSize = 12.sp, maxLines = 1, modifier = Modifier.testTag("legacyText"))
                    }
                }
                Box(Modifier.testTag("label")) {
                    CupertinoLabelLayout(
                        title = { CupertinoText("Delete", fontSize = 12.sp, maxLines = 1, modifier = Modifier.testTag("labelText")) },
                        icon = { SwipeIcon { labelIcon = it } },
                        modifier = Modifier,
                        style = CupertinoLabelStyle.TitleAndIcon,
                        spacing = 4.dp,
                        iconAlignment = CupertinoLabelIconAlignment.Center,
                        clearIconSemantics = false,
                    )
                }
            }
        }
        assertSameLayout("legacy", "legacyText", legacyIcon, "label", "labelText", labelIcon)
    }

    @Test
    fun centerLayoutMatchesTheBackButtonRow() = runComposeUiTest {
        var legacyIcon = Rect.Zero
        var labelIcon = Rect.Zero
        setContent {
            Column {
                Box(Modifier.testTag("legacy")) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BackIcon(Modifier.padding(end = 6.dp)) { legacyIcon = it }
                        CupertinoText("Back", modifier = Modifier.testTag("legacyText"))
                    }
                }
                Box(Modifier.testTag("label")) {
                    CupertinoLabelLayout(
                        title = { CupertinoText("Back", modifier = Modifier.testTag("labelText")) },
                        icon = { BackIcon(Modifier) { labelIcon = it } },
                        modifier = Modifier,
                        style = CupertinoLabelStyle.TitleAndIcon,
                        spacing = 6.dp,
                        iconAlignment = CupertinoLabelIconAlignment.Center,
                    )
                }
            }
        }
        assertSameLayout("legacy", "legacyText", legacyIcon, "label", "labelText", labelIcon)
    }

    @Test
    fun navigateBackButtonLaysOutAMultiChildTitleInARowLikeMain() = runComposeUiTest {
        setContent {
            Column {
                CupertinoNavigateBackButton(
                    onClick = {},
                    modifier = Modifier.testTag("button"),
                    icon = CupertinoIcons.Default.ChevronBackward,
                ) {
                    CupertinoText("Back", modifier = Modifier.testTag("first"))
                    CupertinoText("2", modifier = Modifier.testTag("second"))
                }
                // main's body: the title children are siblings of the icon in one Row inside the button.
                CupertinoButton(
                    onClick = {},
                    colors = CupertinoButtonDefaults.plainButtonColors(),
                    contentPadding = PaddingValues(8.dp, 4.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BackIcon(Modifier.padding(end = 6.dp)) {}
                        CupertinoText("Back", modifier = Modifier.testTag("refFirst"))
                        CupertinoText("2", modifier = Modifier.testTag("refSecond"))
                    }
                }
            }
        }
        val first = onNodeWithTag("first", useUnmergedTree = true).getBoundsInRoot()
        val second = onNodeWithTag("second", useUnmergedTree = true).getBoundsInRoot()
        assertTrue(second.left >= first.right, "title children must not overlap: $first vs $second")

        // Same relative layout as main's Row: no gap between the children, same widths and vertical centering.
        val refFirst = onNodeWithTag("refFirst", useUnmergedTree = true).getBoundsInRoot()
        val refSecond = onNodeWithTag("refSecond", useUnmergedTree = true).getBoundsInRoot()
        assertEquals(refSecond.left - refFirst.left, second.left - first.left)
        assertEquals(refSecond.right - refSecond.left, second.right - second.left)
        assertEquals(refSecond.top - refFirst.top, second.top - first.top)
    }

    @Test
    fun firstLineAlignmentCentersTheIconOnTheFirstLineOfAWrappedTitle() = runComposeUiTest {
        var layout: TextLayoutResult? = null
        var iconBounds = Rect.Zero
        var titleBounds = Rect.Zero
        setContent {
            CupertinoLabel(
                title = {
                    CupertinoText(
                        "Favorites from every album on this device",
                        modifier = Modifier.onGloballyPositioned { titleBounds = it.boundsInRoot() },
                        onTextLayout = { layout = it },
                    )
                },
                icon = {
                    CupertinoIcon(
                        CupertinoIcons.Default.Heart,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp).onGloballyPositioned { iconBounds = it.boundsInRoot() },
                    )
                },
                style = CupertinoLabelStyle.TitleAndIcon,
                modifier = Modifier.width(120.dp),
            )
        }
        waitForIdle()

        val text = layout!!
        assertTrue(text.lineCount > 1, "the title must wrap for this test, got ${text.lineCount} line(s)")
        val firstLineTop = titleBounds.top + text.getLineTop(0)
        val firstLineBottom = titleBounds.top + text.getLineBottom(0)
        val iconCenter = iconBounds.center.y
        assertTrue(
            iconCenter in firstLineTop..firstLineBottom,
            "icon center $iconCenter should be within the first line [$firstLineTop, $firstLineBottom]",
        )
        assertTrue(iconBounds.bottom < titleBounds.bottom, "the icon must not be centered on the whole title")
    }

    @Test
    fun navigateBackButtonStillExposesItsTitleOnTheButton() = runComposeUiTest {
        setContent { CupertinoNavigateBackButton(onClick = {}) { CupertinoText("Back") } }
        onNode(hasClickAction() and hasText("Back")).assertIsDisplayed()
    }

    @Composable
    private fun SwipeIcon(onBounds: (Rect) -> Unit) {
        CupertinoIcon(
            imageVector = CupertinoIcons.Default.Heart,
            contentDescription = null,
            modifier = Modifier.requiredSize(16.dp).onGloballyPositioned { onBounds(it.boundsInRoot()) },
        )
    }

    @Composable
    private fun BackIcon(padding: Modifier, onBounds: (Rect) -> Unit) {
        CupertinoIcon(
            imageVector = CupertinoIcons.Default.ChevronBackward,
            contentDescription = null,
            modifier = Modifier.height(CupertinoIconDefaults.MediumSize)
                .then(padding)
                // Measured inside the padding: the drawn glyph, which must not move.
                .onGloballyPositioned { onBounds(it.boundsInRoot()) },
        )
    }

    private fun androidx.compose.ui.test.ComposeUiTest.assertSameLayout(
        legacy: String,
        legacyText: String,
        legacyIcon: Rect,
        label: String,
        labelText: String,
        labelIcon: Rect,
    ) {
        val legacyBox = onNodeWithTag(legacy).getBoundsInRoot()
        val labelBox = onNodeWithTag(label).getBoundsInRoot()
        assertEquals((legacyBox.right - legacyBox.left), (labelBox.right - labelBox.left))
        assertEquals((legacyBox.bottom - legacyBox.top), (labelBox.bottom - labelBox.top))
        val dy = labelBox.top - legacyBox.top
        val legacyTextBounds = onNodeWithTag(legacyText, useUnmergedTree = true).getBoundsInRoot()
        val labelTextBounds = onNodeWithTag(labelText, useUnmergedTree = true).getBoundsInRoot()
        assertEquals(legacyTextBounds.left, labelTextBounds.left)
        assertEquals(legacyTextBounds.top + dy, labelTextBounds.top)
        assertEquals((legacyTextBounds.right - legacyTextBounds.left), (labelTextBounds.right - labelTextBounds.left))
        assertEquals((legacyTextBounds.bottom - legacyTextBounds.top), (labelTextBounds.bottom - labelTextBounds.top))
        val dyPx = with(density) { dy.toPx() }
        assertEquals(legacyIcon.left, labelIcon.left)
        assertEquals(legacyIcon.top + dyPx, labelIcon.top)
        assertEquals(legacyIcon.height, labelIcon.height)
        assertEquals(legacyIcon.width, labelIcon.width)
    }
}
