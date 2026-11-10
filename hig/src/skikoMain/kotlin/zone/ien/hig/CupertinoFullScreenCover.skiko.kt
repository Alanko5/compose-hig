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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.DialogProperties

/**
 * Skiko targets (iOS, macOS, desktop, web) host the cover in a full-window dialog layer without
 * platform insets, so the content is edge-to-edge and reads the safe-area insets itself.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal actual fun FullScreenCoverDialogProperties(dismissOnBackPress: Boolean): DialogProperties =
    DialogProperties(
        usePlatformInsets = false,
        dismissOnBackPress = dismissOnBackPress,
        dismissOnClickOutside = false,
        usePlatformDefaultWidth = false,
        scrimColor = Color.Transparent,
        animateTransition = false,
    )

/**
 * On iOS the status bar style belongs to the hosting view controller, shared with the content
 * behind the cover, so it is switched only while the cover fully covers the window and restored
 * (by [SystemBarAppearance]'s dispose) as soon as it starts to leave. A no-op on other skiko targets.
 */
@OptIn(InternalCupertinoApi::class)
@Composable
internal actual fun PrepareFullScreenCoverWindow(lightSystemBars: Boolean, coversWindow: Boolean) {
    if (coversWindow) SystemBarAppearance(dark = !lightSystemBars)
}
