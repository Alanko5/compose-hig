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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/**
 * Presents the Compose [CupertinoColorPickerSheet] on non-iOS platforms.
 *
 * Like the iOS picker, the sheet keeps its own selection, so it stays interactive even when the
 * caller does not feed [onColorChanged] back into [color].
 */
@Composable
@ExperimentalCupertinoApi
actual fun CupertinoColorPickerNative(
    color: Color,
    onColorChanged: (Color) -> Unit,
    onDismissRequest: () -> Unit,
    supportsAlpha: Boolean,
) {
    // Re-keyed on [color] so an external change applies in the same frame.
    var current by remember(color) { mutableStateOf(color) }
    CupertinoColorPickerSheet(
        selection = current,
        onSelectionChange = {
            current = it
            onColorChanged(it)
        },
        onDismissRequest = onDismissRequest,
        supportsOpacity = supportsAlpha,
    )
}
