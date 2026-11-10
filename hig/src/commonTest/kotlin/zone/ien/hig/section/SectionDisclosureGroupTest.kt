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

package zone.ien.hig.section

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import zone.ien.hig.CupertinoDisclosureGroupDefaults
import zone.ien.hig.CupertinoText
import zone.ien.hig.ExperimentalCupertinoApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class, ExperimentalCupertinoApi::class)
class SectionDisclosureGroupTest {
    @Test
    fun lazyGroupAddsOnlyHeaderWhenCollapsed() {
        val scope = LazySectionScopeImpl()
        scope.disclosureGroup(expanded = false, onExpandedChange = {}, title = {}) {
            item { }
            item { }
        }
        assertEquals(1, scope.items.size)
    }

    @Test
    fun lazyGroupAddsIndentedChildrenWhenExpanded() {
        val scope = LazySectionScopeImpl()
        scope.disclosureGroup(expanded = true, onExpandedChange = {}, title = {}) {
            item(dividerPadding = 16.dp) { }
            item(dividerPadding = 16.dp) { }
        }
        assertEquals(3, scope.items.size)
        assertEquals(16.dp + CupertinoDisclosureGroupDefaults.ContentIndent, scope.items[1].dividerPadding)
    }

    @Test
    fun nestedLazyGroupsStackIndent() {
        val scope = LazySectionScopeImpl()
        scope.disclosureGroup(expanded = true, onExpandedChange = {}, title = {}) {
            disclosureGroup(expanded = true, onExpandedChange = {}, title = {}, dividerPadding = 16.dp) {
                item(dividerPadding = 16.dp) { }
            }
        }
        assertEquals(3, scope.items.size)
        assertEquals(16.dp + CupertinoDisclosureGroupDefaults.ContentIndent, scope.items[1].dividerPadding)
        assertEquals(16.dp + CupertinoDisclosureGroupDefaults.ContentIndent * 2, scope.items[2].dividerPadding)
    }

    @Test
    fun indentedPaddingAddsToStartSideOnly() {
        val padding = IndentedPaddingValues(PaddingValues(horizontal = 16.dp, vertical = 4.dp), 20.dp)
        assertEquals(36.dp, padding.calculateLeftPadding(LayoutDirection.Ltr))
        assertEquals(16.dp, padding.calculateRightPadding(LayoutDirection.Ltr))
        assertEquals(16.dp, padding.calculateLeftPadding(LayoutDirection.Rtl))
        assertEquals(36.dp, padding.calculateRightPadding(LayoutDirection.Rtl))
        assertEquals(4.dp, padding.calculateTopPadding())
        assertEquals(4.dp, padding.calculateBottomPadding())
    }

    @Test
    fun sectionGroupTogglesChildren() = runComposeUiTest {
        var expanded by mutableStateOf(false)
        setContent {
            CupertinoSection {
                SectionDisclosureGroup(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    title = { CupertinoText("Network") },
                ) {
                    SectionItem { CupertinoText("Wi-Fi") }
                    SectionItem { CupertinoText("Bluetooth") }
                }
            }
        }

        onNodeWithText("Wi-Fi").assertDoesNotExist()
        onNodeWithText("Network").performClick()
        waitForIdle()
        assertTrue(expanded)
        onNodeWithText("Wi-Fi").assertExists()
        onNodeWithText("Bluetooth").assertExists()
    }

    @Test
    fun sectionHeaderHeightMatchesSectionItem() = runComposeUiTest {
        setContent {
            CupertinoSection {
                SectionDisclosureGroup(
                    expanded = false,
                    onExpandedChange = {},
                    modifier = Modifier.testTag("header"),
                    title = { CupertinoText("Network") },
                ) {
                    SectionItem { CupertinoText("Wi-Fi") }
                }
                SectionItem(modifier = Modifier.testTag("item")) { CupertinoText("Other") }
            }
        }

        val header = onNodeWithTag("header").getBoundsInRoot()
        val item = onNodeWithTag("item").getBoundsInRoot()
        assertEquals(item.height, header.height)
    }

    @Test
    fun lazySidebarItemIsIndentedInsideGroup() = runComposeUiTest {
        setContent {
            LazyColumn {
                section(style = SectionStyle.Sidebar) {
                    disclosureGroup(expanded = true, onExpandedChange = {}, title = { CupertinoText("Group") }) {
                        sidebarItem(selected = false, onClick = {}) { CupertinoText("Child") }
                    }
                    sidebarItem(selected = false, onClick = {}) { CupertinoText("Sibling") }
                }
            }
        }

        val child = onNodeWithText("Child").getBoundsInRoot().left
        val sibling = onNodeWithText("Sibling").getBoundsInRoot().left
        val delta = (child - sibling - CupertinoDisclosureGroupDefaults.ContentIndent).value
        assertTrue(kotlin.math.abs(delta) <= 1f, "expected child indented by ContentIndent, got ${child - sibling}")
    }
}
