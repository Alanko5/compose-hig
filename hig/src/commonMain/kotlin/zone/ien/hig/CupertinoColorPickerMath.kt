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
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/*
 * Pure colour math used by the Cupertino colour picker. Everything here works in sRGB with
 * 8-bit channels, which is also what Compose stores for sRGB colours.
 */

/**
 * Hue (degrees, `0 until 360`), saturation (`0..1`) and brightness (`0..1`) of a colour.
 */
internal data class ColorPickerHsb(
    val hue: Float,
    val saturation: Float,
    val brightness: Float,
)

/** sRGB channels of a colour as integers in `0..255`. */
internal data class ColorPickerRgb255(
    val red: Int,
    val green: Int,
    val blue: Int,
    val alpha: Int,
)

/** Rounds a `0..1` channel value to the nearest `0..255` integer, clamping out-of-range input. */
internal fun Float.toColorComponent255(): Int {
    if (isNaN()) return 0
    return (coerceIn(0f, 1f) * 255f).roundToInt()
}

/** Creates an sRGB colour from `0..255` channels, clamping out-of-range input. */
internal fun rgb255ToColor(
    red: Int,
    green: Int,
    blue: Int,
    alpha: Int = 255,
): Color =
    Color(
        red = red.coerceIn(0, 255),
        green = green.coerceIn(0, 255),
        blue = blue.coerceIn(0, 255),
        alpha = alpha.coerceIn(0, 255),
    )

/** Returns the `0..255` sRGB channels of this colour. */
internal fun Color.toRgb255(): ColorPickerRgb255 {
    val srgb = convert(ColorSpaces.Srgb)
    return ColorPickerRgb255(
        red = srgb.red.toColorComponent255(),
        green = srgb.green.toColorComponent255(),
        blue = srgb.blue.toColorComponent255(),
        alpha = srgb.alpha.toColorComponent255(),
    )
}

private fun normalizeHue(hue: Float): Float {
    if (hue.isNaN() || hue.isInfinite()) return 0f
    val wrapped = hue % 360f
    return if (wrapped < 0f) wrapped + 360f else wrapped
}

/**
 * Converts HSB to an sRGB colour. The hue wraps around the circle; saturation, brightness and
 * alpha are clamped to `0..1`.
 */
internal fun hsbToColor(
    hue: Float,
    saturation: Float,
    brightness: Float,
    alpha: Float = 1f,
): Color {
    val h = normalizeHue(hue) / 60f
    val s = saturation.coerceIn(0f, 1f)
    val v = brightness.coerceIn(0f, 1f)
    val sector = floor(h).toInt() % 6
    val fraction = h - floor(h)
    val p = v * (1f - s)
    val q = v * (1f - s * fraction)
    val t = v * (1f - s * (1f - fraction))
    val (r, g, b) =
        when (sector) {
            0 -> Triple(v, t, p)
            1 -> Triple(q, v, p)
            2 -> Triple(p, v, t)
            3 -> Triple(p, q, v)
            4 -> Triple(t, p, v)
            else -> Triple(v, p, q)
        }
    return Color(
        red = r.toColorComponent255(),
        green = g.toColorComponent255(),
        blue = b.toColorComponent255(),
        alpha = alpha.toColorComponent255(),
    )
}

/** Converts this colour to HSB. Achromatic colours report a hue of `0`. */
internal fun Color.toHsb(): ColorPickerHsb {
    val srgb = convert(ColorSpaces.Srgb)
    val r = srgb.red.coerceIn(0f, 1f)
    val g = srgb.green.coerceIn(0f, 1f)
    val b = srgb.blue.coerceIn(0f, 1f)
    val maxC = max(r, max(g, b))
    val minC = min(r, min(g, b))
    val delta = maxC - minC
    val hue =
        when {
            delta == 0f -> 0f
            maxC == r -> 60f * (((g - b) / delta) % 6f)
            maxC == g -> 60f * ((b - r) / delta + 2f)
            else -> 60f * ((r - g) / delta + 4f)
        }
    return ColorPickerHsb(
        hue = normalizeHue(hue),
        saturation = if (maxC == 0f) 0f else delta / maxC,
        brightness = maxC,
    )
}

private const val HexDigits = "0123456789ABCDEF"

private fun StringBuilder.appendHexByte(value: Int) {
    append(HexDigits[(value shr 4) and 0xF])
    append(HexDigits[value and 0xF])
}

/** Formats this colour as `#RRGGBB`, or `#RRGGBBAA` when [includeAlpha] is `true`. */
internal fun Color.toHexString(includeAlpha: Boolean = false): String {
    val rgb = toRgb255()
    return buildString {
        append('#')
        appendHexByte(rgb.red)
        appendHexByte(rgb.green)
        appendHexByte(rgb.blue)
        if (includeAlpha) appendHexByte(rgb.alpha)
    }
}

private fun hexValue(c: Char): Int = HexDigits.indexOf(c.uppercaseChar())

/**
 * Parses `#RGB`, `#RGBA`, `#RRGGBB` or `#RRGGBBAA` (the `#` is optional, surrounding whitespace is
 * ignored). Returns `null` for anything else.
 */
internal fun parseHexColor(text: String): Color? {
    val digits = text.trim().removePrefix("#")
    if (digits.isEmpty() || digits.any { hexValue(it) < 0 }) return null
    val values = digits.map(::hexValue)
    return when (digits.length) {
        3, 4 -> {
            fun expand(i: Int) = values[i] * 17
            rgb255ToColor(expand(0), expand(1), expand(2), if (digits.length == 4) expand(3) else 255)
        }
        6, 8 -> {
            fun byte(i: Int) = values[i * 2] * 16 + values[i * 2 + 1]
            rgb255ToColor(byte(0), byte(1), byte(2), if (digits.length == 8) byte(3) else 255)
        }
        else -> null
    }
}

/** Keeps only hexadecimal digits of [text] (upper-cased), truncated to [maxLength]. */
internal fun sanitizeHexInput(text: String, maxLength: Int): String =
    text.filter { hexValue(it) >= 0 }.uppercase().take(maxLength)

/** Opacity of this colour as a whole percentage. */
internal fun Color.opacityPercent(): Int = (alpha.coerceIn(0f, 1f) * 100f).roundToInt()

// ---------------------------------------------------------------------------------------------
// Spectrum
// ---------------------------------------------------------------------------------------------

private const val MaxSpectrumX = 1f - 1e-4f

/**
 * Maps a normalized position on the spectrum field to HSB. `x` runs through the hue circle; the
 * top half of `y` fades from white to the fully saturated hue, the bottom half from the hue to black.
 */
internal fun spectrumHsbAt(x: Float, y: Float): ColorPickerHsb {
    val cx = x.coerceIn(0f, MaxSpectrumX)
    val cy = y.coerceIn(0f, 1f)
    return if (cy <= 0.5f) {
        ColorPickerHsb(hue = cx * 360f, saturation = cy * 2f, brightness = 1f)
    } else {
        ColorPickerHsb(hue = cx * 360f, saturation = 1f, brightness = 1f - (cy - 0.5f) * 2f)
    }
}

/**
 * Inverse of [spectrumHsbAt]. Colours that are not on the spectrum surface (neither saturation
 * nor brightness is 1) are projected onto the band that changes their RGB value the least: the
 * top band moves the brightest channel by `1 - b`, the bottom band moves the darkest channel by
 * `b * (1 - s)`. Black and dark greys therefore land at the bottom, light greys at the top.
 */
internal fun spectrumPositionOf(hsb: ColorPickerHsb): Pair<Float, Float> {
    val x = (hsb.hue / 360f).coerceIn(0f, MaxSpectrumX)
    val s = hsb.saturation.coerceIn(0f, 1f)
    val b = hsb.brightness.coerceIn(0f, 1f)
    val y = if (1f - b <= b * (1f - s)) s / 2f else 0.5f + (1f - b) / 2f
    return x to y
}

// ---------------------------------------------------------------------------------------------
// Grid palette
// ---------------------------------------------------------------------------------------------

private val PaletteHues = listOf(200f, 218f, 248f, 278f, 318f, 354f, 12f, 28f, 40f, 52f, 64f, 88f)

private val PaletteShades =
    listOf(
        1f to 0.30f,
        1f to 0.45f,
        1f to 0.62f,
        1f to 0.80f,
        1f to 1f,
        0.75f to 1f,
        0.55f to 1f,
        0.36f to 1f,
        0.18f to 1f,
    )

/**
 * The colour grid shown by the picker: a grayscale row (white to black) followed by nine rows of
 * twelve hues, from dark to light. This approximates the iOS system palette.
 */
internal val CupertinoColorPickerPalette: List<List<Color>> by lazy {
    val grayscale = List(12) { index -> hsbToColor(0f, 0f, 1f - index / 11f) }
    listOf(grayscale) +
        PaletteShades.map { (saturation, brightness) ->
            PaletteHues.map { hue -> hsbToColor(hue, saturation, brightness) }
        }
}

// ---------------------------------------------------------------------------------------------
// Accessibility
// ---------------------------------------------------------------------------------------------

/** A short English colour name (for example "Dark blue") used for accessibility descriptions. */
internal fun colorAccessibilityName(color: Color): String {
    val hsb = color.toHsb()
    if (hsb.brightness < 0.06f) return "Black"
    if (hsb.saturation < 0.08f) {
        return when {
            hsb.brightness >= 0.97f -> "White"
            hsb.brightness > 0.66f -> "Light gray"
            hsb.brightness < 0.33f -> "Dark gray"
            else -> "Gray"
        }
    }
    val hue = hsb.hue
    val base =
        when {
            hue < 15f || hue >= 335f -> "red"
            hue < 45f -> "orange"
            hue < 70f -> "yellow"
            hue < 160f -> "green"
            hue < 200f -> "cyan"
            hue < 255f -> "blue"
            hue < 290f -> "purple"
            else -> "pink"
        }
    val modifier =
        when {
            hsb.brightness < 0.45f -> "Dark "
            hsb.saturation < 0.45f -> "Light "
            else -> ""
        }
    return if (modifier.isEmpty()) base.replaceFirstChar { it.uppercaseChar() } else modifier + base
}

/** Colour name, hex value and (optionally) opacity, e.g. "Red, #FF0000, 50% opacity". */
internal fun colorAccessibilityDescription(color: Color, includeOpacity: Boolean): String {
    val base = "${colorAccessibilityName(color)}, ${color.toHexString()}"
    val opacity = color.opacityPercent()
    return if (includeOpacity && opacity < 100) "$base, $opacity% opacity" else base
}

/** Moves [color] to the front of [recent], removing duplicates and keeping at most [maxCount]. */
internal fun addRecentColor(
    recent: List<Color>,
    color: Color,
    maxCount: Int = 10,
): List<Color> = (listOf(color) + recent.filter { it != color }).take(maxCount)

/** Whether [a] and [b] have the same 8-bit sRGB channels, ignoring alpha. */
internal fun sameRgb(a: Color, b: Color): Boolean {
    val x = a.toRgb255()
    val y = b.toRgb255()
    return x.red == y.red && x.green == y.green && x.blue == y.blue
}
