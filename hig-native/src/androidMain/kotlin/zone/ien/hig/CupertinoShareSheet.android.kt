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

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalContext

@Composable
internal actual fun rememberPlatformSharePresenter(): CupertinoSharePresenter {
    val context = LocalContext.current
    return remember(context) { AndroidSharePresenter(context) }
}

/**
 * Shows the Android sharesheet (`Intent.createChooser`).
 *
 * Android does not report whether the user actually shared, so a successfully started chooser reports
 * [CupertinoShareResult.Presented].
 */
@OptIn(ExperimentalCupertinoApi::class)
internal class AndroidSharePresenter(private val context: Context) : CupertinoSharePresenter {
    override fun present(
        request: CupertinoShareRequest,
        anchorBounds: Rect?,
        onResult: (CupertinoShareResult) -> Unit,
    ) {
        val payload = shareIntentPayload(request.items, request.subject)
        if (payload == null || !payload.streamUris.all(::isAndroidShareableUri)) {
            onResult(CupertinoShareResult.Unavailable)
            return
        }
        val chooser = Intent.createChooser(payload.toIntent(), null)
        val activity = context.findActivity()
        if (activity == null) chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val result = try {
            (activity ?: context).startActivity(chooser)
            CupertinoShareResult.Presented
        } catch (_: ActivityNotFoundException) {
            CupertinoShareResult.Unavailable
        }
        onResult(result)
    }
}

internal fun ShareIntentPayload.toIntent(): Intent {
    val uris = streamUris.map(Uri::parse)
    return Intent(
        when (action) {
            ShareIntentAction.Send -> Intent.ACTION_SEND
            ShareIntentAction.SendMultiple -> Intent.ACTION_SEND_MULTIPLE
        },
    ).apply {
        type = mimeType
        text?.let { putExtra(Intent.EXTRA_TEXT, it) }
        subject?.let {
            putExtra(Intent.EXTRA_SUBJECT, it)
            // Shown as the preview title of the Android 10+ sharesheet.
            putExtra(Intent.EXTRA_TITLE, it)
        }
        when (uris.size) {
            0 -> Unit
            1 -> putExtra(Intent.EXTRA_STREAM, uris.single())
            else -> putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
        }
        if (uris.isNotEmpty()) {
            // ClipData carries the URI grant through the chooser to the selected app.
            clipData = ClipData.newRawUri(null, uris.first()).apply {
                uris.drop(1).forEach { addItem(ClipData.Item(it)) }
            }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
