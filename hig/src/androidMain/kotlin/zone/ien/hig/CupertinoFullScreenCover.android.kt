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

import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowInsetsController
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider

/**
 * Android hosts the cover in an edge-to-edge dialog window (`decorFitsSystemWindows = false`) so the
 * content can draw behind the status and navigation bars and read their insets.
 */
@Composable
internal actual fun FullScreenCoverDialogProperties(dismissOnBackPress: Boolean): DialogProperties =
    DialogProperties(
        dismissOnBackPress = dismissOnBackPress,
        dismissOnClickOutside = false,
        usePlatformDefaultWidth = false,
        decorFitsSystemWindows = false,
    )

@Composable
internal actual fun PrepareFullScreenCoverWindow(lightSystemBars: Boolean, coversWindow: Boolean) {
    // The dialog has its own window, so its bar appearance never affects the content behind it and
    // can be applied for the whole presentation; coversWindow is not needed here.
    val view = LocalView.current
    SideEffect {
        val window = (view.parent as? DialogWindowProvider)?.window ?: return@SideEffect
        if (window.attributes.dimAmount != 0f) window.setDimAmount(0f)
        if (window.attributes.windowAnimations != 0) window.setWindowAnimations(0)
        val attributes = window.attributes
        if (attributes.width != ViewGroup.LayoutParams.MATCH_PARENT ||
            attributes.height != ViewGroup.LayoutParams.MATCH_PARENT
        ) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
    }
    LaunchedEffect(view, lightSystemBars) {
        (view.parent as? DialogWindowProvider)?.window?.setLightSystemBars(lightSystemBars)
    }
}

private fun Window.setLightSystemBars(light: Boolean) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val mask =
            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
        insetsController?.setSystemBarsAppearance(if (light) mask else 0, mask)
    } else {
        @Suppress("DEPRECATION")
        val flags = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        @Suppress("DEPRECATION")
        decorView.systemUiVisibility =
            if (light) decorView.systemUiVisibility or flags else decorView.systemUiVisibility and flags.inv()
    }
}
