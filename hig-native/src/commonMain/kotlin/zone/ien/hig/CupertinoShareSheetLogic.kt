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

import androidx.compose.ui.geometry.Rect

// Pure, platform independent logic behind CupertinoShareSheet. Kept here so it can be unit tested in commonTest.

/** A single share invocation: what to share and the optional subject (mail subject / chooser title). */
@OptIn(ExperimentalCupertinoApi::class)
internal data class CupertinoShareRequest(
    val items: List<CupertinoShareItem>,
    val subject: String? = null,
)

/**
 * Platform bridge that actually shows a share UI.
 *
 * Implementations must call [present]'s `onResult` exactly once per request (extra calls are ignored by
 * [CupertinoShareSheetState]) and must be invoked from the main thread.
 */
@OptIn(ExperimentalCupertinoApi::class)
internal fun interface CupertinoSharePresenter {
    fun present(
        request: CupertinoShareRequest,
        anchorBounds: Rect?,
        onResult: (CupertinoShareResult) -> Unit,
    )
}

/** The Android intent action to use for a share. */
internal enum class ShareIntentAction { Send, SendMultiple }

/**
 * Platform neutral description of an Android `ACTION_SEND` / `ACTION_SEND_MULTIPLE` intent.
 *
 * @property text all [CupertinoShareItem.Text] and [CupertinoShareItem.Url] items joined with new lines (`EXTRA_TEXT`)
 * @property streamUris URIs of all [CupertinoShareItem.File] items (`EXTRA_STREAM` + `ClipData`)
 */
internal data class ShareIntentPayload(
    val action: ShareIntentAction,
    val mimeType: String,
    val text: String?,
    val subject: String?,
    val streamUris: List<String>,
)

@OptIn(ExperimentalCupertinoApi::class)
internal fun shareIntentPayload(items: List<CupertinoShareItem>, subject: String?): ShareIntentPayload? {
    if (items.isEmpty()) return null
    val streams = items.filterIsInstance<CupertinoShareItem.File>().map { it.uri }
    return ShareIntentPayload(
        action = if (streams.size > 1) ShareIntentAction.SendMultiple else ShareIntentAction.Send,
        mimeType = shareMimeType(items),
        text = clipboardShareText(items),
        subject = subject?.takeIf { it.isNotBlank() },
        streamUris = streams,
    )
}

/**
 * Chooses a single MIME type describing every item.
 *
 * - Only text / URLs → `text/plain`.
 * - Files present → the common type of all files (`image/png`), the top level wildcard when only the subtype
 *   differs (e.g. "image/any"), otherwise [MimeAny]. Files whose type is unknown force [MimeAny]. Text sent next to
 *   files travels in `EXTRA_TEXT` and does not influence the type.
 */
@OptIn(ExperimentalCupertinoApi::class)
internal fun shareMimeType(items: List<CupertinoShareItem>): String {
    val files = items.filterIsInstance<CupertinoShareItem.File>()
    if (files.isEmpty()) return MimeTextPlain
    val types = files.map { resolvedMimeType(it) ?: return MimeAny }.map { it.lowercase() }.distinct()
    if (types.size == 1) return types.single()
    val topLevels = types.map { it.substringBefore('/') }.distinct()
    return if (topLevels.size == 1) "${topLevels.single()}/*" else MimeAny
}

/** The explicit [CupertinoShareItem.File.mimeType] or one inferred from the file extension, or `null` when unknown. */
@OptIn(ExperimentalCupertinoApi::class)
internal fun resolvedMimeType(file: CupertinoShareItem.File): String? {
    file.mimeType?.let { return it }
    val path = file.uri.substringBefore('#').substringBefore('?')
    val hasScheme = path.contains("://")
    val withoutScheme = path.substringAfter("://")
    val lastSegment = withoutScheme.substringAfterLast('/')
    // "content://authority.with.dots" has no path, the dots belong to the authority.
    if (!lastSegment.contains('.') || (hasScheme && withoutScheme == lastSegment)) return null
    val extension = lastSegment.substringAfterLast('.').lowercase()
    return MimeTypesByExtension[extension]
}

/** Android can only grant access to `content://` URIs (e.g. from a `FileProvider`); `file://` and raw paths would throw. */
internal fun isAndroidShareableUri(uri: String): Boolean = uri.startsWith("content://", ignoreCase = true)

/** Text that the clipboard fallback copies: every text / URL item on its own line, or `null` when there is none. */
@OptIn(ExperimentalCupertinoApi::class)
internal fun clipboardShareText(items: List<CupertinoShareItem>): String? =
    items.mapNotNull {
        when (it) {
            is CupertinoShareItem.Text -> it.text
            is CupertinoShareItem.Url -> it.url
            is CupertinoShareItem.File -> null
        }
    }.takeIf { it.isNotEmpty() }?.joinToString("\n")

/** Maps the `UIActivityViewController.completionWithItemsHandler` arguments to a [CupertinoShareResult]. */
@OptIn(ExperimentalCupertinoApi::class)
internal fun iosShareResult(completed: Boolean, isCopyToPasteboard: Boolean, hasError: Boolean): CupertinoShareResult =
    when {
        hasError -> CupertinoShareResult.Unavailable
        !completed -> CupertinoShareResult.Dismissed
        isCopyToPasteboard -> CupertinoShareResult.CopiedToClipboard
        else -> CupertinoShareResult.Shared
    }

/** A rectangle in UIKit points. */
internal data class ShareSourceRect(val x: Double, val y: Double, val width: Double, val height: Double)

/**
 * Converts the anchor bounds (Compose pixels, relative to the root) to the popover `sourceRect` in points.
 * Without an anchor the popover points at the center of the container.
 */
internal fun shareSourceRect(
    anchor: Rect?,
    density: Float,
    containerWidth: Double,
    containerHeight: Double,
): ShareSourceRect {
    if (anchor == null || density <= 0f) {
        return ShareSourceRect(containerWidth / 2, containerHeight / 2, 0.0, 0.0)
    }
    return ShareSourceRect(
        x = (anchor.left / density).toDouble(),
        y = (anchor.top / density).toDouble(),
        width = (anchor.width / density).toDouble(),
        height = (anchor.height / density).toDouble(),
    )
}

internal const val MimeTextPlain = "text/plain"
internal const val MimeAny = "*/*"

private val MimeTypesByExtension = mapOf(
    "png" to "image/png",
    "jpg" to "image/jpeg",
    "jpeg" to "image/jpeg",
    "gif" to "image/gif",
    "webp" to "image/webp",
    "heic" to "image/heic",
    "heif" to "image/heif",
    "bmp" to "image/bmp",
    "svg" to "image/svg+xml",
    "mp4" to "video/mp4",
    "m4v" to "video/x-m4v",
    "mov" to "video/quicktime",
    "webm" to "video/webm",
    "mp3" to "audio/mpeg",
    "m4a" to "audio/mp4",
    "aac" to "audio/aac",
    "wav" to "audio/wav",
    "ogg" to "audio/ogg",
    "pdf" to "application/pdf",
    "zip" to "application/zip",
    "json" to "application/json",
    "txt" to "text/plain",
    "csv" to "text/csv",
    "html" to "text/html",
    "htm" to "text/html",
    "md" to "text/markdown",
    "vcf" to "text/vcard",
    "ics" to "text/calendar",
)
