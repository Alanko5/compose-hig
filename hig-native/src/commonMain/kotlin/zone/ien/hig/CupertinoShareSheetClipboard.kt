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

@file:Suppress("DEPRECATION")

package zone.ien.hig

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString

/**
 * Fallback used on desktop, macOS, js and wasmJs where there is no system share sheet: text and URL items are copied
 * to the clipboard ([CupertinoShareResult.CopiedToClipboard]); a request that contains only files reports
 * [CupertinoShareResult.Unavailable].
 *
 * Uses the synchronous [ClipboardManager] (deprecated in favour of the suspending `Clipboard`, whose `ClipEntry`
 * has no common text factory) so the result can be delivered synchronously from the click handler.
 * On web the write is fire-and-forget: a write the browser denies is still reported as copied.
 */
@OptIn(ExperimentalCupertinoApi::class)
internal class ClipboardSharePresenter(
    private val clipboard: ClipboardManager,
) : CupertinoSharePresenter {
    override fun present(
        request: CupertinoShareRequest,
        anchorBounds: androidx.compose.ui.geometry.Rect?,
        onResult: (CupertinoShareResult) -> Unit,
    ) {
        val text = clipboardShareText(request.items)
        if (text == null) {
            onResult(CupertinoShareResult.Unavailable)
            return
        }
        val result = runCatching { clipboard.setText(AnnotatedString(text)) }
        onResult(if (result.isSuccess) CupertinoShareResult.CopiedToClipboard else CupertinoShareResult.Unavailable)
    }
}

@Composable
internal fun rememberClipboardSharePresenter(): CupertinoSharePresenter {
    val clipboard = LocalClipboardManager.current
    return remember(clipboard) { ClipboardSharePresenter(clipboard) }
}
