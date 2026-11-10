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
import androidx.compose.ui.graphics.Color

/**
 * Presents a colour picker while this composable is in the composition.
 *
 * On iOS this presents the system `UIColorPickerViewController`; on other platforms it presents
 * the Compose [CupertinoColorPickerSheet].
 *
 * The parameter names follow the original iOS-only API so existing callers keep compiling. They map
 * to the `hig` colour picker as [color] to `selection`, [onColorChanged] to `onSelectionChange` and
 * [supportsAlpha] to `supportsOpacity`.
 *
 * @param color the currently selected colour
 * @param onColorChanged called with every colour the user picks
 * @param onDismissRequest called when the user closes the picker. Remove this composable from
 * the composition in response
 * @param supportsAlpha whether the picker lets the user change the opacity
 */
@Composable
@ExperimentalCupertinoApi
expect fun CupertinoColorPickerNative(
    color: Color,
    onColorChanged: (Color) -> Unit,
    onDismissRequest: () -> Unit,
    supportsAlpha: Boolean = true,
)
