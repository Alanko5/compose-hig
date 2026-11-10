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

@file:OptIn(ExperimentalCupertinoApi::class)

package zone.ien.hig.adaptive

import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.ui.unit.dp
import zone.ien.hig.ExperimentalCupertinoApi
import zone.ien.hig.adaptive.CupertinoSidebarColumn.Detail
import zone.ien.hig.adaptive.CupertinoSidebarColumn.Sidebar
import zone.ien.hig.adaptive.CupertinoSidebarVisibility.Automatic
import zone.ien.hig.adaptive.CupertinoSidebarVisibility.Hidden
import zone.ien.hig.adaptive.CupertinoSidebarVisibility.Shown
import zone.ien.hig.adaptive.CupertinoSidebarWidthClass.Compact
import zone.ien.hig.adaptive.CupertinoSidebarWidthClass.Expanded
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CupertinoSidebarLogicTest {

    @Test
    fun listTakesTopInsetOnlyWithoutTopBar() {
        val start = WindowInsetsSides.Start
        val horizontal = WindowInsetsSides.Horizontal
        val bottom = WindowInsetsSides.Bottom
        val top = WindowInsetsSides.Top
        assertEquals(start + bottom + top, cupertinoSidebarListInsetSides(expanded = true, hasTopBar = false))
        assertEquals(start + bottom, cupertinoSidebarListInsetSides(expanded = true, hasTopBar = true))
        assertEquals(horizontal + bottom + top, cupertinoSidebarListInsetSides(expanded = false, hasTopBar = false))
        assertEquals(horizontal + bottom, cupertinoSidebarListInsetSides(expanded = false, hasTopBar = true))
    }

    @Test
    fun widthClassSwitchesAtBreakpoint() {
        assertEquals(Compact, cupertinoSidebarWidthClass(599.dp, 600.dp))
        assertEquals(Expanded, cupertinoSidebarWidthClass(600.dp, 600.dp))
        assertEquals(Expanded, cupertinoSidebarWidthClass(1024.dp, 600.dp))
    }

    @Test
    fun expandedShowsBothPanesUnlessHidden() {
        assertEquals(CupertinoSidebarPanes(sidebar = true, detail = true), resolveCupertinoSidebarPanes(Expanded, Automatic, Sidebar))
        assertEquals(CupertinoSidebarPanes(sidebar = true, detail = true), resolveCupertinoSidebarPanes(Expanded, Shown, Detail))
        assertEquals(CupertinoSidebarPanes(sidebar = false, detail = true), resolveCupertinoSidebarPanes(Expanded, Hidden, Sidebar))
        assertEquals(CupertinoSidebarPanes(sidebar = false, detail = true), resolveCupertinoSidebarPanes(Expanded, Hidden, Detail))
    }

    @Test
    fun compactShowsExactlyOneColumnRegardlessOfVisibility() {
        CupertinoSidebarVisibility.entries.forEach { visibility ->
            assertEquals(CupertinoSidebarPanes(sidebar = true, detail = false), resolveCupertinoSidebarPanes(Compact, visibility, Sidebar))
            assertEquals(CupertinoSidebarPanes(sidebar = false, detail = true), resolveCupertinoSidebarPanes(Compact, visibility, Detail))
        }
    }

    @Test
    fun toggleFlipsVisibilityOnExpandedOnly() {
        assertEquals(Hidden, toggledCupertinoSidebarVisibility(Expanded, Automatic))
        assertEquals(Hidden, toggledCupertinoSidebarVisibility(Expanded, Shown))
        assertEquals(Shown, toggledCupertinoSidebarVisibility(Expanded, Hidden))
        CupertinoSidebarVisibility.entries.forEach {
            assertEquals(it, toggledCupertinoSidebarVisibility(Compact, it))
        }
    }

    @Test
    fun backPopsToSidebarOnlyInCompactDetail() {
        assertEquals(CupertinoSidebarBackAction.ShowSidebar, cupertinoSidebarBackAction(Compact, Detail))
        assertEquals(CupertinoSidebarBackAction.None, cupertinoSidebarBackAction(Compact, Sidebar))
        assertEquals(CupertinoSidebarBackAction.None, cupertinoSidebarBackAction(Expanded, Detail))
        assertEquals(CupertinoSidebarBackAction.None, cupertinoSidebarBackAction(Expanded, Sidebar))
    }

    @Test
    fun toggleDescriptionDescribesNextAction() {
        assertEquals("Hide sidebar", cupertinoSidebarToggleDescription(sidebarVisible = true))
        assertEquals("Show sidebar", cupertinoSidebarToggleDescription(sidebarVisible = false))
    }

    @Test
    fun expandedBoundsSlideSidebarAndResizeDetail() {
        val shown = cupertinoSidebarPaneBounds(Expanded, totalWidth = 1000, sidebarWidth = 300, sidebarFraction = 1f, detailFraction = 1f)
        assertEquals(CupertinoSidebarPaneBounds(0, 300, 300, 700, sidebarPlaced = true, detailPlaced = true), shown)

        val half = cupertinoSidebarPaneBounds(Expanded, totalWidth = 1000, sidebarWidth = 300, sidebarFraction = 0.5f, detailFraction = 1f)
        assertEquals(CupertinoSidebarPaneBounds(-150, 300, 150, 850, sidebarPlaced = true, detailPlaced = true), half)

        val hidden = cupertinoSidebarPaneBounds(Expanded, totalWidth = 1000, sidebarWidth = 300, sidebarFraction = 0f, detailFraction = 1f)
        assertEquals(CupertinoSidebarPaneBounds(-300, 300, 0, 1000, sidebarPlaced = false, detailPlaced = true), hidden)
    }

    @Test
    fun expandedSidebarNeverExceedsTotalWidth() {
        val bounds = cupertinoSidebarPaneBounds(Expanded, totalWidth = 200, sidebarWidth = 300, sidebarFraction = 1f, detailFraction = 1f)
        assertEquals(200, bounds.sidebarWidth)
        assertEquals(0, bounds.detailWidth)
    }

    @Test
    fun compactBoundsPushDetailOverSidebar() {
        val root = cupertinoSidebarPaneBounds(Compact, totalWidth = 400, sidebarWidth = 300, sidebarFraction = 1f, detailFraction = 0f)
        assertEquals(CupertinoSidebarPaneBounds(0, 400, 400, 400, sidebarPlaced = true, detailPlaced = false), root)

        val pushed = cupertinoSidebarPaneBounds(Compact, totalWidth = 400, sidebarWidth = 300, sidebarFraction = 0f, detailFraction = 1f)
        assertEquals(CupertinoSidebarPaneBounds(-120, 400, 0, 400, sidebarPlaced = false, detailPlaced = true), pushed)

        val mid = cupertinoSidebarPaneBounds(Compact, totalWidth = 400, sidebarWidth = 300, sidebarFraction = 0.5f, detailFraction = 0.5f)
        assertEquals(-60, mid.sidebarX)
        assertEquals(200, mid.detailX)
        assertTrue(mid.sidebarPlaced)
        assertTrue(mid.detailPlaced)
    }

    @Test
    fun stateTogglesAndNavigatesBack() {
        val state = CupertinoSidebarState()
        assertTrue(state.isSidebarVisible)
        state.toggleSidebar()
        assertEquals(Hidden, state.visibility)
        assertFalse(state.isSidebarVisible)
        state.toggleSidebar()
        assertEquals(Shown, state.visibility)

        state.widthClass = Compact
        state.showDetail()
        assertTrue(state.isDetailVisible)
        assertFalse(state.isSidebarVisible)
        assertTrue(state.navigateBack())
        assertTrue(state.isSidebarVisible)
        assertFalse(state.navigateBack())
    }

    @Test
    fun showSidebarRevealsHiddenSidebarOnExpanded() {
        val state = CupertinoSidebarState(initialVisibility = Hidden, initialCompactColumn = Detail)
        assertFalse(state.isSidebarVisible)
        state.showSidebar()
        assertEquals(Shown, state.visibility)
        assertEquals(Sidebar, state.compactColumn)
    }
}
