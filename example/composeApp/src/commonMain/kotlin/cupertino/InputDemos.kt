@file:OptIn(zone.ien.hig.ExperimentalCupertinoApi::class)

package cupertino

import IsIos
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import zone.ien.hig.CupertinoBorderedSecureTextField
import zone.ien.hig.CupertinoBorderedTextField
import zone.ien.hig.CupertinoButton
import zone.ien.hig.CupertinoButtonDefaults
import zone.ien.hig.CupertinoColorPicker
import zone.ien.hig.CupertinoColorPickerNative
import zone.ien.hig.CupertinoColorWell
import zone.ien.hig.CupertinoDatePicker
import zone.ien.hig.CupertinoDateTimePicker
import zone.ien.hig.CupertinoSearchTextField
import zone.ien.hig.CupertinoSearchToken
import zone.ien.hig.CupertinoSecureTextField
import zone.ien.hig.CupertinoText
import zone.ien.hig.CupertinoTextField
import zone.ien.hig.CupertinoWheelPicker
import zone.ien.hig.DatePickerStyle
import zone.ien.hig.icons.CupertinoIcons
import zone.ien.hig.icons.filled.Heart
import zone.ien.hig.icons.filled.Star
import zone.ien.hig.rememberCupertinoDatePickerState
import zone.ien.hig.rememberCupertinoDateTimePickerState
import zone.ien.hig.rememberCupertinoPickerState
import zone.ien.hig.rememberCupertinoSearchTextFieldState
import zone.ien.hig.rememberCupertinoTimePickerState
import zone.ien.hig.theme.CupertinoTheme

@Composable
internal fun TextFieldDemo() {
    var text by rememberSaveable { mutableStateOf("") }
    var enabled by rememberSaveable { mutableStateOf(true) }
    var readOnly by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf(false) }
    var singleLine by rememberSaveable { mutableStateOf(true) }
    val password = rememberTextFieldState()
    val focusManager = LocalFocusManager.current
    DemoSection("Preview", "Both fields share the same value.") {
        CupertinoText(text = "Plain field")
        CupertinoTextField(
            value = text, onValueChange = { text = it }, enabled = enabled, readOnly = readOnly,
            isError = error, singleLine = singleLine, modifier = Modifier.fillMaxWidth(),
            placeholder = { CupertinoText(text = "Enter your name") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        )
        CupertinoText(text = "Bordered field")
        CupertinoBorderedTextField(
            value = text, onValueChange = { text = it }, enabled = enabled, readOnly = readOnly,
            isError = error, singleLine = singleLine, modifier = Modifier.fillMaxWidth(),
            placeholder = { CupertinoText(text = "Enter your name") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        )
        if (error) CupertinoText(text = "This field has a validation error.")
        DemoResult("Input: ${text.ifEmpty { "(empty)" }} · ${text.length} characters")
        CupertinoButton(onClick = { text = "" }, enabled = enabled && !readOnly && text.isNotEmpty(),
            colors = CupertinoButtonDefaults.tintedButtonColors()) { CupertinoText(text = "Clear input") }
    }
    DemoSection("Properties") {
        DemoToggle("Enabled", enabled) { enabled = it }
        DemoToggle("Read only", readOnly) { readOnly = it }
        DemoToggle("Error", error) { error = it }
        DemoToggle("Single line", singleLine) { singleLine = it }
    }
    DemoSection("Secure fields", "Secure fields mask the same shared password.") {
        CupertinoSecureTextField(state = password, enabled = enabled, readOnly = readOnly,
            placeholder = { CupertinoText(text = "Password") }, modifier = Modifier.fillMaxWidth())
        CupertinoBorderedSecureTextField(state = password, enabled = enabled, readOnly = readOnly,
            placeholder = { CupertinoText(text = "Password") }, modifier = Modifier.fillMaxWidth())
        DemoResult("Password length: ${password.text.length}")
    }
}

@Composable
internal fun SearchFieldDemo(scrollState: ScrollableState) {
    var query by rememberSaveable { mutableStateOf("") }
    var enabled by rememberSaveable { mutableStateOf(true) }
    val searchState = rememberCupertinoSearchTextFieldState(scrollableState = scrollState,
        blockScrollWhenFocusedAndEmpty = false)
    val focusManager = LocalFocusManager.current
    val results = listOf("Seoul", "Busan", "Incheon", "Daejeon", "Daegu", "Gwangju", "Jeju").filter { it.contains(query.trim(), ignoreCase = true) }
    DemoSection("Preview", "Search for a city. Cancel clears the query and focus.") {
        CupertinoSearchTextField(
            value = query, onValueChange = { query = it }, state = searchState, enabled = enabled,
            paddingValues = PaddingValues(0.dp), modifier = Modifier.fillMaxWidth(),
            placeholder = { CupertinoText(text = "Search cities") },
            cancelButton = {
                CupertinoButton(onClick = { query = ""; focusManager.clearFocus() },
                    colors = CupertinoButtonDefaults.plainButtonColors()) { CupertinoText(text = "Cancel") }
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    CupertinoButton(onClick = { query = "" }, enabled = enabled,
                        colors = CupertinoButtonDefaults.plainButtonColors()) { CupertinoText(text = "Clear") }
                }
            },
        )
        DemoResult("Query: ${query.ifEmpty { "(none)" }} · ${results.size} results")
        if (results.isEmpty()) CupertinoText(text = "No cities found.")
        results.forEach { CupertinoText(text = it) }
    }
    DemoSection("Properties") { DemoToggle("Enabled", enabled) { enabled = it } }
}

private data class DemoPhoto(val name: String, val kind: String, val favorite: Boolean, val starred: Boolean)

private val demoPhotos = listOf(
    DemoPhoto("Beach sunset", "Photos", favorite = true, starred = false),
    DemoPhoto("Beach volleyball", "Videos", favorite = false, starred = true),
    DemoPhoto("City skyline", "Photos", favorite = false, starred = true),
    DemoPhoto("Concert", "Videos", favorite = true, starred = true),
    DemoPhoto("Mountain lake", "Photos", favorite = true, starred = true),
)

@Composable
internal fun SearchTokensDemo() {
    val suggestions = listOf(
        CupertinoSearchToken("Photos"),
        CupertinoSearchToken("Videos"),
        CupertinoSearchToken("Favorites", icon = CupertinoIcons.Filled.Heart),
        CupertinoSearchToken("Starred", icon = CupertinoIcons.Filled.Star),
    )
    var query by rememberSaveable { mutableStateOf("") }
    var tokenNames by rememberSaveable { mutableStateOf(listOf("Favorites")) }
    var enabled by rememberSaveable { mutableStateOf(true) }
    val tokens = tokenNames.mapNotNull { name -> suggestions.firstOrNull { it.text == name } }
    val results = demoPhotos.filter { photo ->
        photo.name.contains(query.trim(), ignoreCase = true) && tokens.all { token ->
            when (token.text) {
                "Favorites" -> photo.favorite
                "Starred" -> photo.starred
                else -> photo.kind == token.text
            }
        }
    }
    DemoSection(
        "Preview",
        "Add filters from the suggestions. With the caret at the start of the query, backspace selects the last token and a second backspace removes it. Tapping a token selects it.",
    ) {
        CupertinoSearchTextField(
            value = query,
            onValueChange = { query = it },
            tokens = tokens,
            onTokensChange = { new -> tokenNames = new.map { it.text } },
            enabled = enabled,
            paddingValues = PaddingValues(0.dp),
            modifier = Modifier.fillMaxWidth(),
            placeholder = { CupertinoText(text = "Search library") },
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            suggestions.filter { it.text !in tokenNames }.forEach { suggestion ->
                CupertinoButton(
                    onClick = { tokenNames = tokenNames + suggestion.text },
                    enabled = enabled,
                    colors = CupertinoButtonDefaults.tintedButtonColors(),
                ) { CupertinoText(text = "+ ${suggestion.text}") }
            }
        }
        DemoResult("Tokens: ${tokenNames.ifEmpty { listOf("(none)") }.joinToString()} · ${results.size} results")
        if (results.isEmpty()) CupertinoText(text = "No items match.")
        results.forEach { CupertinoText(text = "${it.name} · ${it.kind}") }
    }
    DemoSection("Properties") { DemoToggle("Enabled", enabled) { enabled = it } }
}

@Composable
internal fun WheelPickerDemo() {
    var infinite by rememberSaveable { mutableStateOf(true) }
    var rotation by rememberSaveable { mutableStateOf(true) }
    var enabled by rememberSaveable { mutableStateOf(true) }
    val values = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    var selected by rememberSaveable { mutableIntStateOf(0) }
    DemoSection("Preview", "Drag the wheel to select a day.") {
        key(infinite) {
            val state = rememberCupertinoPickerState(infinite = infinite, initiallySelectedItemIndex = selected)
            val selectedIndex = state.selectedItemIndex(values.size)
            androidx.compose.runtime.LaunchedEffect(selectedIndex) {
                selected = selectedIndex
            }
            CupertinoWheelPicker(state = state, items = values, enabled = enabled, withRotation = rotation,
                modifier = Modifier.fillMaxWidth()) { CupertinoText(text = it) }
            DemoResult("Selected: ${values[selectedIndex]} · Index: $selectedIndex")
        }
    }
    DemoSection("Properties") {
        DemoToggle("Enabled", enabled) { enabled = it }
        DemoToggle("Infinite scrolling", infinite) { infinite = it }
        DemoToggle("Wheel rotation", rotation) { rotation = it }
    }
}

@Composable
internal fun DateTimeDemo(component: CatalogComponent) {
    var native by rememberSaveable { mutableStateOf(false) }
    var calendar by rememberSaveable { mutableStateOf(false) }
    var is24Hour by rememberSaveable { mutableStateOf(true) }
    var hour by rememberSaveable { mutableIntStateOf(9) }
    var minute by rememberSaveable { mutableIntStateOf(30) }
    DemoSection("Preview") {
        when (component) {
            CatalogComponent.DatePicker -> {
                val state = rememberCupertinoDatePickerState()
                if (native) DatePickerExample(state, native = true)
                else CupertinoDatePicker(state = state, modifier = Modifier.fillMaxWidth(),
                    style = if (calendar) DatePickerStyle.Pager() else DatePickerStyle.Wheel())
                DemoResult("Date: ${Instant.fromEpochMilliseconds(state.selectedDateMillis).toLocalDateTime(TimeZone.UTC).date}")
            }
            CatalogComponent.TimePicker -> key(is24Hour) {
                val state = rememberCupertinoTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = is24Hour)
                androidx.compose.runtime.LaunchedEffect(state.hour, state.minute) { hour = state.hour; minute = state.minute }
                TimePickerExample(state, native)
                DemoResult("Time: ${state.hour.toString().padStart(2, '0')}:${state.minute.toString().padStart(2, '0')}")
            }
            CatalogComponent.DateTimePicker -> {
                val state = rememberCupertinoDateTimePickerState(initialHour = 9, initialMinute = 30)
                if (native) DateTimePicker(state, native = true)
                else CupertinoDateTimePicker(state = state, modifier = Modifier.fillMaxWidth(),
                    style = if (calendar) DatePickerStyle.Pager() else DatePickerStyle.Wheel())
                DemoResult("Date and time: ${Instant.fromEpochMilliseconds(state.selectedDateTimeMillis).toLocalDateTime(TimeZone.UTC)}")
            }
            else -> Unit
        }
    }
    DemoSection("Properties") {
        DemoToggle("Native implementation", native) { native = it }
        if (!native && component != CatalogComponent.TimePicker) {
            DemoToggle("Calendar style", calendar) { calendar = it }
        }
        if (component == CatalogComponent.TimePicker) {
            DemoToggle("24-hour time", is24Hour) { is24Hour = it }
        }
        NativeImplementationNote(native)
    }
}

@Composable
internal fun NativeImplementationNote(native: Boolean) {
    CupertinoText(text = when {
        !native -> "Rendered with Compose"
        IsIos -> "Rendered with UIKit"
        else -> "Native uses UIKit on iOS and Compose on this platform."
    })
}

@Composable
internal fun ColorPickerDemo() {
    var tint by remember { mutableStateOf(Color(0xFF007AFF)) }
    var background by remember { mutableStateOf(Color(0xFFFF9500).copy(alpha = 0.35f)) }
    var recent by remember { mutableStateOf(listOf(Color(0xFF34C759), Color(0xFFFF3B30))) }
    var enabled by rememberSaveable { mutableStateOf(true) }
    var native by rememberSaveable { mutableStateOf(false) }
    var showNative by remember { mutableStateOf(false) }
    DemoSection("Preview", "Tap a row or the color well to open the picker.") {
        Box(
            Modifier.fillMaxWidth().height(96.dp).clip(CupertinoTheme.shapes.large).background(background),
            contentAlignment = Alignment.Center,
        ) {
            CupertinoText(text = "Hello, colors", color = tint, style = CupertinoTheme.typography.title2)
        }
        CupertinoColorPicker(
            selection = tint, onSelectionChange = { tint = it }, supportsOpacity = false, enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        ) { CupertinoText(text = "Text color") }
        CupertinoColorPicker(
            selection = background, onSelectionChange = { background = it }, enabled = enabled,
            recentColors = recent, onRecentColorsChange = { recent = it },
            modifier = Modifier.fillMaxWidth(),
        ) { CupertinoText(text = "Background (with opacity)") }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CupertinoText(text = "Standalone well", modifier = Modifier.weight(1f))
            CupertinoColorWell(selection = tint, onSelectionChange = { tint = it }, supportsOpacity = false, enabled = enabled)
        }
        DemoResult("Text: ${tint.demoHex()} · Background: ${background.demoHex()} · ${(background.alpha * 100).roundToInt()}%")
    }
    DemoSection("Properties") {
        DemoToggle("Enabled", enabled) { enabled = it }
        DemoToggle("Native implementation", native) { native = it }
        if (native) {
            CupertinoButton(onClick = { showNative = true }, enabled = enabled,
                colors = CupertinoButtonDefaults.tintedButtonColors()) { CupertinoText(text = "Open native picker") }
        }
        NativeImplementationNote(native)
    }
    if (showNative) {
        CupertinoColorPickerNative(
            color = tint,
            onColorChanged = { tint = it },
            onDismissRequest = { showNative = false },
            supportsAlpha = false,
        )
    }
}

private fun Color.demoHex(): String =
    "#" + (toArgb() and 0xFFFFFF).toString(16).uppercase().padStart(6, '0')
