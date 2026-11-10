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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class SectionItemsLayoutTest {
    @Test
    fun itemsAreStackedWithDividersBetween() = runComposeUiTest {
        setContent {
            Box(Modifier.width(300.dp)) {
                SectionItemsLayout(showDivider = true, dividerPadding = PaddingValues(0.dp)) {
                    Box(Modifier.fillMaxWidth().height(40.dp).testTag("a"))
                    Box(Modifier.fillMaxWidth().height(40.dp).testTag("b"))
                }
            }
        }
        val a = onNodeWithTag("a").getBoundsInRoot()
        val b = onNodeWithTag("b").getBoundsInRoot()
        assertEquals(0.dp, a.top)
        // b starts below a plus a divider (divider height > 0)
        assertTrue(b.top > a.bottom, "expected a divider between items, b.top=${b.top} a.bottom=${a.bottom}")
    }

    @Test
    fun noDividersWhenDisabled() = runComposeUiTest {
        setContent {
            Box(Modifier.width(300.dp)) {
                SectionItemsLayout(showDivider = false, dividerPadding = PaddingValues(0.dp)) {
                    Box(Modifier.fillMaxWidth().height(40.dp).testTag("a"))
                    Box(Modifier.fillMaxWidth().height(40.dp).testTag("b"))
                }
            }
        }
        assertEquals(onNodeWithTag("a").getBoundsInRoot().bottom, onNodeWithTag("b").getBoundsInRoot().top)
    }

    @Test
    fun startIndentShiftsAndNarrowsItems() = runComposeUiTest {
        setContent {
            Box(Modifier.width(300.dp)) {
                SectionItemsLayout(
                    showDivider = false,
                    dividerPadding = PaddingValues(0.dp),
                    startIndent = 20.dp,
                ) {
                    Box(Modifier.fillMaxWidth().height(40.dp).testTag("a"))
                }
            }
        }
        val a = onNodeWithTag("a").getBoundsInRoot()
        assertEquals(20.dp, a.left)
        assertEquals(300.dp, a.right)
    }
}
