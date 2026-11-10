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

import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCupertinoApi::class)
class CupertinoLabelLogicTest {
    @Test
    fun explicitStyleWinsOverInheritedStyle() {
        assertEquals(
            CupertinoLabelStyle.TitleOnly,
            resolveCupertinoLabelStyle(CupertinoLabelStyle.TitleOnly, CupertinoLabelStyle.IconOnly),
        )
    }

    @Test
    fun automaticStyleAdoptsInheritedStyle() {
        assertEquals(
            CupertinoLabelStyle.IconOnly,
            resolveCupertinoLabelStyle(CupertinoLabelStyle.Automatic, CupertinoLabelStyle.IconOnly),
        )
    }

    @Test
    fun automaticEverywhereResolvesToTitleAndIcon() {
        assertEquals(
            CupertinoLabelStyle.TitleAndIcon,
            resolveCupertinoLabelStyle(CupertinoLabelStyle.Automatic, CupertinoLabelStyle.Automatic),
        )
    }

    @Test
    fun iconSizeScalesWithFontSize() {
        assertEquals(20.4f, iconSizeFor(TextStyle(fontSize = 17.sp), Density(2f)).value, 0.001f)
        assertEquals(CupertinoIconDefaults.MediumSize, iconSizeFor(TextStyle.Default, Density(1f)))
    }

    @Test
    fun centerAlignmentMatchesRowCenterVertically() {
        val offsets = computeLabelVerticalOffsets(
            iconHeight = 16, titleHeight = 15, firstBaseline = 12, lastBaseline = 12,
            alignment = CupertinoLabelIconAlignment.Center,
        )
        assertEquals(LabelVerticalOffsets(iconTop = 0, titleTop = 1, contentHeight = 16), offsets)
    }

    @Test
    fun singleLineFirstLineAlignmentEqualsCentering() {
        val firstLine = computeLabelVerticalOffsets(20, 22, 17, 17, CupertinoLabelIconAlignment.FirstLine)
        val center = computeLabelVerticalOffsets(20, 22, 17, 17, CupertinoLabelIconAlignment.Center)
        assertEquals(center, firstLine)
    }

    @Test
    fun titleWithoutBaselinesIsCentered() {
        val offsets = computeLabelVerticalOffsets(
            20, 40, AlignmentLine.Unspecified, AlignmentLine.Unspecified, CupertinoLabelIconAlignment.FirstLine,
        )
        assertEquals(LabelVerticalOffsets(iconTop = 10, titleTop = 0, contentHeight = 40), offsets)
    }

    @Test
    fun multiLineIconCentersOnFirstLine() {
        // Three 22px lines: baselines at 17 and 61, descent 5 -> first line spans 0..22, center 11.
        val offsets = computeLabelVerticalOffsets(20, 66, 17, 61, CupertinoLabelIconAlignment.FirstLine)
        assertEquals(LabelVerticalOffsets(iconTop = 1, titleTop = 0, contentHeight = 66), offsets)
    }

    @Test
    fun tallIconPushesMultiLineTitleDown() {
        // First line center 11, icon 40 tall -> icon would start at -9, so the title moves down by 9.
        val offsets = computeLabelVerticalOffsets(40, 44, 17, 39, CupertinoLabelIconAlignment.FirstLine)
        assertEquals(LabelVerticalOffsets(iconTop = 0, titleTop = 9, contentHeight = 53), offsets)
    }

    @Test
    fun defaultsAndNamesAreStable() {
        assertEquals("IconOnly", CupertinoLabelStyle.IconOnly.toString())
        assertEquals(8.dp, CupertinoLabelDefaults.Spacing)
    }
}
