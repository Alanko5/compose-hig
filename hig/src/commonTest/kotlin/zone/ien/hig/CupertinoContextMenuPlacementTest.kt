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

import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CupertinoContextMenuPlacementTest {
    private val window = IntSize(400, 800)

    private fun place(
        anchor: IntRect,
        preview: IntSize = IntSize(anchor.width, anchor.height),
        menu: IntSize = IntSize(250, 200),
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
    ) = calculateContextMenuPlacement(
        anchorBounds = anchor,
        previewSize = preview,
        menuSize = menu,
        windowSize = window,
        margin = 16,
        spacing = 8,
        minPreviewHeight = 80,
        layoutDirection = layoutDirection,
    )

    @Test
    fun menuGoesBelowAndAlignsWithLeadingEdgeWhenThereIsRoom() {
        val placement = place(IntRect(20, 100, 120, 160))
        assertEquals(IntRect(20, 100, 120, 160), placement.previewBounds)
        assertEquals(IntRect(20, 168, 270, 368), placement.menuBounds)
        assertTrue(placement.menuBelow)
    }

    @Test
    fun menuGoesAboveAndAlignsWithTrailingEdgeNearTheBottomRight() {
        val placement = place(IntRect(250, 680, 380, 740))
        assertEquals(IntRect(250, 680, 380, 740), placement.previewBounds)
        assertEquals(IntRect(130, 472, 380, 672), placement.menuBounds)
        assertFalse(placement.menuBelow)
    }

    @Test
    fun previewTallerThanWindowIsScaledToStayFullyVisible() {
        val placement = place(IntRect(0, 0, 400, 1200))
        assertEquals(IntRect(107, 224, 294, 784), placement.previewBounds)
        assertEquals(IntRect(44, 16, 294, 216), placement.menuBounds)
        assertFalse(placement.menuBelow)
        assertInside(placement.previewBounds)
        assertInside(placement.menuBounds)
    }

    @Test
    fun previewShiftsUpWhenMenuFitsOnNeitherSideInUpperHalf() {
        val placement = place(IntRect(50, 150, 350, 550), menu = IntSize(250, 300))
        assertEquals(IntRect(50, 76, 350, 476), placement.previewBounds)
        assertEquals(IntRect(50, 484, 300, 784), placement.menuBounds)
        assertTrue(placement.menuBelow)
    }

    @Test
    fun previewShiftsDownWhenMenuFitsOnNeitherSideInLowerHalf() {
        val placement = place(IntRect(50, 300, 350, 700), menu = IntSize(250, 300))
        assertEquals(IntRect(50, 324, 350, 724), placement.previewBounds)
        assertEquals(IntRect(50, 16, 300, 316), placement.menuBounds)
        assertFalse(placement.menuBelow)
    }

    @Test
    fun centeredPreviewAlignsMenuWithTrailingEdgeInRtl() {
        val placement = place(IntRect(100, 100, 300, 160), layoutDirection = LayoutDirection.Rtl)
        assertEquals(IntRect(50, 168, 300, 368), placement.menuBounds)
    }

    @Test
    fun centeredPreviewAlignsMenuWithLeadingEdgeInLtr() {
        val placement = place(IntRect(100, 100, 300, 160))
        assertEquals(IntRect(100, 168, 350, 368), placement.menuBounds)
    }

    @Test
    fun anchorOutsideWindowIsClampedIntoMargins() {
        val placement = place(IntRect(-50, -40, 50, 20))
        assertEquals(IntRect(16, 16, 116, 76), placement.previewBounds)
        assertEquals(IntRect(16, 84, 266, 284), placement.menuBounds)
    }

    @Test
    fun tallMenuIsClampedAndKeepsMinimumPreviewVisible() {
        val placement = place(IntRect(20, 300, 420, 500), menu = IntSize(250, 2000))
        assertEquals(680, placement.menuBounds.height)
        assertEquals(80, placement.previewBounds.height)
        assertInside(placement.previewBounds)
        assertInside(placement.menuBounds)
    }

    @Test
    fun menuWiderThanWindowIsClampedToMargins() {
        val placement = place(IntRect(20, 100, 120, 160), menu = IntSize(600, 200))
        assertEquals(IntRect(16, 168, 384, 368), placement.menuBounds)
    }

    @Test
    fun menuMaxHeightReservesSpaceForPreview() {
        assertEquals(700, calculateContextMenuMaxHeight(previewHeight = 60, windowHeight = 800, margin = 16, spacing = 8, minPreviewHeight = 80))
        assertEquals(680, calculateContextMenuMaxHeight(previewHeight = 300, windowHeight = 800, margin = 16, spacing = 8, minPreviewHeight = 80))
        assertEquals(0, calculateContextMenuMaxHeight(previewHeight = 300, windowHeight = 50, margin = 16, spacing = 8, minPreviewHeight = 80))
    }

    private fun assertInside(rect: IntRect) {
        assertTrue(
            rect.left >= 16 && rect.top >= 16 && rect.right <= 384 && rect.bottom <= 784,
            "$rect is outside the window margins",
        )
    }
}
