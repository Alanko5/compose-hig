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

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CupertinoColorPickerMathTest {
    private fun assertHsb(expected: ColorPickerHsb, actual: ColorPickerHsb, tolerance: Float = 0.01f) {
        assertEquals(expected.hue, actual.hue, 0.6f, "hue of $actual")
        assertEquals(expected.saturation, actual.saturation, tolerance, "saturation of $actual")
        assertEquals(expected.brightness, actual.brightness, tolerance, "brightness of $actual")
    }

    @Test
    fun hsbToColorProducesPrimaryAndSecondaryColors() {
        assertEquals("#FF0000", hsbToColor(0f, 1f, 1f).toHexString())
        assertEquals("#FFFF00", hsbToColor(60f, 1f, 1f).toHexString())
        assertEquals("#00FF00", hsbToColor(120f, 1f, 1f).toHexString())
        assertEquals("#00FFFF", hsbToColor(180f, 1f, 1f).toHexString())
        assertEquals("#0000FF", hsbToColor(240f, 1f, 1f).toHexString())
        assertEquals("#FF00FF", hsbToColor(300f, 1f, 1f).toHexString())
        assertEquals("#FFFFFF", hsbToColor(123f, 0f, 1f).toHexString())
        assertEquals("#000000", hsbToColor(200f, 1f, 0f).toHexString())
        assertEquals("#808080", hsbToColor(0f, 0f, 0.5f).toHexString())
    }

    @Test
    fun hsbToColorWrapsHueAndClampsSaturationAndBrightness() {
        assertEquals(hsbToColor(0f, 1f, 1f), hsbToColor(360f, 1f, 1f))
        assertEquals(hsbToColor(300f, 1f, 1f), hsbToColor(-60f, 1f, 1f))
        assertEquals(hsbToColor(30f, 1f, 1f), hsbToColor(750f, 1f, 1f))
        assertEquals(hsbToColor(30f, 1f, 1f), hsbToColor(30f, 7f, 3f))
        assertEquals(hsbToColor(30f, 0f, 0f), hsbToColor(30f, -1f, -2f))
        assertEquals(0.5f, hsbToColor(0f, 1f, 1f, alpha = 0.5f).alpha, 0.01f)
        assertEquals(1f, hsbToColor(0f, 1f, 1f, alpha = 4f).alpha)
    }

    @Test
    fun colorToHsbMatchesKnownValues() {
        assertHsb(ColorPickerHsb(0f, 1f, 1f), Color.Red.toHsb())
        assertHsb(ColorPickerHsb(120f, 1f, 1f), Color.Green.toHsb())
        assertHsb(ColorPickerHsb(240f, 1f, 1f), Color.Blue.toHsb())
        assertHsb(ColorPickerHsb(0f, 0f, 0f), Color.Black.toHsb())
        assertHsb(ColorPickerHsb(0f, 0f, 1f), Color.White.toHsb())
        assertHsb(ColorPickerHsb(30f, 1f, 1f), rgb255ToColor(255, 128, 0).toHsb(), tolerance = 0.005f)
        assertHsb(ColorPickerHsb(210f, 0.5f, 0.8f), rgb255ToColor(102, 153, 204).toHsb())
    }

    @Test
    fun hsbRoundTripIsStableAcrossTheHueCircle() {
        for (hue in 0 until 360 step 7) {
            for (saturation in listOf(0.2f, 0.5f, 1f)) {
                for (brightness in listOf(0.3f, 0.7f, 1f)) {
                    val color = hsbToColor(hue.toFloat(), saturation, brightness)
                    val back = color.toHsb()
                    // 8-bit quantisation of sRGB colours bounds the error.
                    val hueTolerance = 1.2f / (saturation * brightness)
                    assertEquals(hue.toFloat(), back.hue, hueTolerance, "hue of $back")
                    assertEquals(saturation, back.saturation, 0.01f, "saturation of $back")
                    assertEquals(brightness, back.brightness, 0.01f, "brightness of $back")
                    // Converting the quantised colour again is exactly stable.
                    assertEquals(color, hsbToColor(back.hue, back.saturation, back.brightness))
                }
            }
        }
    }

    @Test
    fun rgbRoundTripIsLosslessForEveryEightBitChannel() {
        for (value in 0..255) {
            val color = rgb255ToColor(value, 255 - value, (value * 7) % 256)
            val rgb = color.toRgb255()
            assertEquals(ColorPickerRgb255(value, 255 - value, (value * 7) % 256, 255), rgb)
            val viaHsb = color.toHsb().let { hsbToColor(it.hue, it.saturation, it.brightness) }
            assertEquals(rgb, viaHsb.toRgb255())
        }
    }

    @Test
    fun componentRoundingUsesNearestIntegerAndClamps() {
        assertEquals(0, 0f.toColorComponent255())
        assertEquals(255, 1f.toColorComponent255())
        assertEquals(128, 0.5f.toColorComponent255())
        assertEquals(127, 0.498f.toColorComponent255())
        assertEquals(2, (1.5f / 255f).toColorComponent255())
        assertEquals(0, (-0.2f).toColorComponent255())
        assertEquals(255, 1.7f.toColorComponent255())
        assertEquals(0, Float.NaN.toColorComponent255())
        assertEquals(ColorPickerRgb255(0, 255, 10, 255), rgb255ToColor(-20, 300, 10, 999).toRgb255())
    }

    @Test
    fun hexFormattingWithAndWithoutAlpha() {
        assertEquals("#3366CC", rgb255ToColor(0x33, 0x66, 0xCC).toHexString())
        assertEquals("#3366CCFF", rgb255ToColor(0x33, 0x66, 0xCC).toHexString(includeAlpha = true))
        assertEquals("#3366CC80", rgb255ToColor(0x33, 0x66, 0xCC, 0x80).toHexString(includeAlpha = true))
        assertEquals("#3366CC", rgb255ToColor(0x33, 0x66, 0xCC, 0x80).toHexString())
        assertEquals("#000000", Color.Black.toHexString())
    }

    @Test
    fun hexParsingAcceptsLongShortAndAlphaForms() {
        assertEquals(ColorPickerRgb255(0x33, 0x66, 0xCC, 255), parseHexColor("#3366CC")?.toRgb255())
        assertEquals(ColorPickerRgb255(0x33, 0x66, 0xCC, 255), parseHexColor("3366cc")?.toRgb255())
        assertEquals(ColorPickerRgb255(0x33, 0x66, 0xCC, 0x80), parseHexColor("#3366CC80")?.toRgb255())
        assertEquals(ColorPickerRgb255(0x33, 0x66, 0xCC, 255), parseHexColor("#36C")?.toRgb255())
        assertEquals(ColorPickerRgb255(0xFF, 0x00, 0x00, 0x88), parseHexColor("#F008")?.toRgb255())
        assertEquals(ColorPickerRgb255(0xAB, 0xCD, 0xEF, 255), parseHexColor("  #abcdef \n")?.toRgb255())
    }

    @Test
    fun hexParsingRejectsInvalidInput() {
        assertNull(parseHexColor(""))
        assertNull(parseHexColor("#"))
        assertNull(parseHexColor("#12"))
        assertNull(parseHexColor("#12345"))
        assertNull(parseHexColor("#1234567"))
        assertNull(parseHexColor("#123456789"))
        assertNull(parseHexColor("#GGGGGG"))
        assertNull(parseHexColor("##123456"))
        assertNull(parseHexColor("0x123456"))
        assertNull(parseHexColor("#12 456"))
    }

    @Test
    fun hexRoundTripIsStable() {
        listOf("#000000", "#FFFFFF", "#007AFF", "#FF3B30", "#5856D6", "#7F7F7F").forEach { hex ->
            assertEquals(hex, parseHexColor(hex)!!.toHexString())
        }
        listOf("#007AFF00", "#007AFF7F", "#12345678").forEach { hex ->
            assertEquals(hex, parseHexColor(hex)!!.toHexString(includeAlpha = true))
        }
    }

    @Test
    fun sanitizeHexInputKeepsOnlyHexDigitsUpToTheLimit() {
        assertEquals("ABC123", sanitizeHexInput("#abc123", maxLength = 6))
        assertEquals("ABC123", sanitizeHexInput("ab-c1 23zz99", maxLength = 6))
        assertEquals("", sanitizeHexInput("xyz", maxLength = 6))
        assertEquals("12345678", sanitizeHexInput("123456789A", maxLength = 8))
    }

    @Test
    fun spectrumMappingCoversWhiteHueAndBlackBands() {
        assertHsb(ColorPickerHsb(0f, 0f, 1f), spectrumHsbAt(0f, 0f))
        assertHsb(ColorPickerHsb(180f, 1f, 1f), spectrumHsbAt(0.5f, 0.5f))
        assertHsb(ColorPickerHsb(90f, 0.5f, 1f), spectrumHsbAt(0.25f, 0.25f))
        assertHsb(ColorPickerHsb(270f, 1f, 0.5f), spectrumHsbAt(0.75f, 0.75f))
        assertHsb(ColorPickerHsb(0f, 1f, 0f), spectrumHsbAt(0f, 1f))
        // Out-of-bounds positions are clamped; the right edge stays just below a full turn.
        assertHsb(spectrumHsbAt(0f, 0f), spectrumHsbAt(-3f, -3f))
        assertTrue(spectrumHsbAt(2f, 0.5f).hue < 360f)
    }

    @Test
    fun spectrumPositionInvertsSpectrumMapping() {
        for (x in listOf(0f, 0.1f, 0.33f, 0.5f, 0.9f)) {
            for (y in listOf(0.05f, 0.25f, 0.5f, 0.6f, 0.95f)) {
                val (px, py) = spectrumPositionOf(spectrumHsbAt(x, y))
                assertEquals(x, px, 0.0001f)
                assertEquals(y, py, 0.0001f)
            }
        }
        // Colours off the spectrum surface are projected onto the nearest band.
        assertEquals(0.25f, spectrumPositionOf(ColorPickerHsb(0f, 0.5f, 0.9f)).second, 0.0001f)
        assertEquals(0.75f, spectrumPositionOf(ColorPickerHsb(0f, 0.9f, 0.5f)).second, 0.0001f)
        // Achromatic colours: black and dark grey at the black bottom edge, white and light grey at the top.
        assertEquals(1f, spectrumPositionOf(Color.Black.toHsb()).second, 0.0001f)
        assertEquals(0.85f, spectrumPositionOf(ColorPickerHsb(0f, 0f, 0.3f)).second, 0.0001f)
        assertEquals(0f, spectrumPositionOf(Color.White.toHsb()).second, 0.0001f)
        assertEquals(0f, spectrumPositionOf(ColorPickerHsb(0f, 0f, 0.8f)).second, 0.0001f)
    }

    @Test
    fun paletteHasGrayscaleRowAndTwelveDistinctHueColumns() {
        val palette = CupertinoColorPickerPalette
        assertEquals(10, palette.size)
        palette.forEach { assertEquals(12, it.size) }
        val gray = palette.first()
        assertEquals("#FFFFFF", gray.first().toHexString())
        assertEquals("#000000", gray.last().toHexString())
        gray.forEach { assertTrue(it.toHsb().saturation < 0.01f) }
        gray.zipWithNext().forEach { (a, b) -> assertTrue(a.toHsb().brightness > b.toHsb().brightness) }
        val all = palette.flatten().map { it.toHexString() }
        assertEquals(all.size, all.toSet().size, "palette colours must be unique")
    }

    @Test
    fun accessibilityNamesDescribeHueAndLightness() {
        assertEquals("Black", colorAccessibilityName(Color.Black))
        assertEquals("White", colorAccessibilityName(Color.White))
        assertEquals("Gray", colorAccessibilityName(rgb255ToColor(128, 128, 128)))
        assertEquals("Light gray", colorAccessibilityName(rgb255ToColor(200, 200, 200)))
        assertEquals("Dark gray", colorAccessibilityName(rgb255ToColor(50, 50, 50)))
        assertEquals("Red", colorAccessibilityName(Color.Red))
        assertEquals("Orange", colorAccessibilityName(rgb255ToColor(255, 149, 0)))
        assertEquals("Yellow", colorAccessibilityName(Color.Yellow))
        assertEquals("Green", colorAccessibilityName(Color.Green))
        assertEquals("Cyan", colorAccessibilityName(Color.Cyan))
        assertEquals("Blue", colorAccessibilityName(Color.Blue))
        assertEquals("Purple", colorAccessibilityName(hsbToColor(275f, 1f, 1f)))
        assertEquals("Pink", colorAccessibilityName(Color.Magenta))
        assertEquals("Dark blue", colorAccessibilityName(hsbToColor(230f, 1f, 0.3f)))
        assertEquals("Light blue", colorAccessibilityName(hsbToColor(220f, 0.3f, 1f)))
    }

    @Test
    fun accessibilityDescriptionIncludesHexAndOpacity() {
        assertEquals("Red, #FF0000", colorAccessibilityDescription(Color.Red, includeOpacity = true))
        assertEquals(
            "Red, #FF0000, 50% opacity",
            colorAccessibilityDescription(Color.Red.copy(alpha = 0.5f), includeOpacity = true),
        )
        assertEquals("Red, #FF0000", colorAccessibilityDescription(Color.Red.copy(alpha = 0.5f), includeOpacity = false))
    }

    @Test
    fun recentColorsMoveToFrontWithoutDuplicatesAndAreCapped() {
        val start = listOf(Color.Red, Color.Green, Color.Blue)
        assertEquals(listOf(Color.Blue, Color.Red, Color.Green), addRecentColor(start, Color.Blue))
        assertEquals(listOf(Color.Yellow, Color.Red, Color.Green), addRecentColor(start, Color.Yellow, maxCount = 3))
        assertEquals(listOf(Color.Red), addRecentColor(emptyList(), Color.Red))
        // Same visible colour with different opacity is a different entry.
        assertEquals(4, addRecentColor(start, Color.Red.copy(alpha = 0.5f)).size)
    }

    @Test
    fun opacityPercentRoundsToNearestInteger() {
        assertEquals(100, Color.Red.opacityPercent())
        assertEquals(0, Color.Red.copy(alpha = 0f).opacityPercent())
        assertEquals(50, Color.Red.copy(alpha = 0.5f).opacityPercent())
    }
}
