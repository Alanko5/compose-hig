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

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import zone.ien.hig.icons.CupertinoIcons
import zone.ien.hig.icons.outlined.SquareAndArrowUp
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * Something that can be passed to [CupertinoShareSheetState.share] or [CupertinoShareLink].
 */
@ExperimentalCupertinoApi
sealed interface CupertinoShareItem {

    /**
     * Plain text.
     *
     * iOS shares it as an `NSString`; Android puts it into `Intent.EXTRA_TEXT` (joined with other text and URL items
     * by new lines).
     */
    data class Text(val text: String) : CupertinoShareItem

    /**
     * A web (or any other scheme) link.
     *
     * iOS shares it as an `NSURL`, which enables link previews and "Open in" targets; Android sends it as text.
     */
    data class Url(val url: String) : CupertinoShareItem

    /**
     * A file such as an image, a video or a document.
     *
     * - **Android**: [uri] must be a `content://` URI, typically produced by `androidx.core.content.FileProvider`
     *   declared in your app manifest. Read permission is granted to the receiving app automatically
     *   (`FLAG_GRANT_READ_URI_PERMISSION` + `ClipData`). `file://` URIs and raw paths are rejected and the share
     *   reports [CupertinoShareResult.Unavailable], because Android forbids exposing them to other apps.
     * - **iOS**: an absolute file path (`/var/.../photo.png`) or a `file://` URL of a file your app can read.
     * - **Desktop / web / macOS**: not supported by the clipboard fallback.
     *
     * @param uri `content://` URI (Android), absolute path or `file://` URL (iOS)
     * @param mimeType MIME type of the file. When `null` it is inferred from the file extension, falling back to
     * a wildcard type on Android.
     */
    data class File(val uri: String, val mimeType: String? = null) : CupertinoShareItem
}

/**
 * Outcome of a share request.
 */
@ExperimentalCupertinoApi
enum class CupertinoShareResult {
    /** The user picked a share target and the target reported success (iOS). */
    Shared,

    /** The user closed the share sheet without sharing (iOS). */
    Dismissed,

    /**
     * The system share sheet was shown, but the platform does not report what happened next. Android's chooser never
     * tells the app whether something was actually shared, so this is the only result Android delivers on success.
     */
    Presented,

    /**
     * The content ended up on the clipboard: the user chose "Copy" on iOS, or the clipboard fallback was used on
     * desktop, macOS and web where no system share sheet is available. On web the browser may still deny the write
     * (no user activation or permission), which the clipboard API does not report back.
     */
    CopiedToClipboard,

    /**
     * Nothing could be shared: no items, no app can handle them (Android), a file item on a platform that can only
     * copy text, a non `content://` file URI on Android, or the sheet failed with an error (iOS).
     */
    Unavailable,
}

/**
 * State and launcher for the system share sheet. Create it with [rememberCupertinoShareSheetState].
 *
 * | Platform | Presentation | Result |
 * |---|---|---|
 * | iOS | `UIActivityViewController` presented from the top-most view controller; on iPad as a popover anchored to [cupertinoShareSheetAnchor] | [CupertinoShareResult.Shared], [CupertinoShareResult.Dismissed], [CupertinoShareResult.CopiedToClipboard] |
 * | Android | `Intent.createChooser` with `ACTION_SEND` / `ACTION_SEND_MULTIPLE` | [CupertinoShareResult.Presented] (Android does not report the outcome) |
 * | Desktop, macOS, web | Text and URLs are copied to the clipboard | [CupertinoShareResult.CopiedToClipboard], or [CupertinoShareResult.Unavailable] when only files are shared |
 */
@ExperimentalCupertinoApi
@Stable
class CupertinoShareSheetState internal constructor(
    internal var presenter: CupertinoSharePresenter,
    private val timeSource: TimeSource = TimeSource.Monotonic,
) {
    internal var onResult: (CupertinoShareResult) -> Unit = {}

    /**
     * Bounds of the anchor (Compose pixels, relative to the root), see [cupertinoShareSheetAnchor]. When several
     * elements are anchored to the same state, the one positioned last wins.
     */
    internal var anchorBounds: Rect? = null

    /** Id of the request whose result is still expected, `0` when none. */
    private var activeRequest = 0L
    private var lastRequest = 0L
    private var lastPresentation: TimeMark? = null

    /** `true` while a share sheet started by this state waits for its result. */
    var isPresented: Boolean by mutableStateOf(false)
        private set

    /** The result of the most recent share, or `null` before the first one finished. */
    var lastResult: CupertinoShareResult? by mutableStateOf(null)
        private set

    /**
     * Shows the system share sheet for [items].
     *
     * Must be called from the main thread, typically from a click handler. Calls made while a sheet from this state
     * is still presented, or within 500 ms of the previous presentation (a fast double tap; Android
     * reports its result before the chooser is even visible), are ignored. The result is delivered to the `onResult` callback of
     * [rememberCupertinoShareSheetState] and stored in [lastResult].
     *
     * @param items the content to share, in the order it should be offered
     * @param subject optional subject, used by mail-like targets (`EXTRA_SUBJECT` on Android, the `subject` of the
     * activity view controller on iOS)
     */
    fun share(items: List<CupertinoShareItem>, subject: String? = null) {
        if (isPresented) return
        if (lastPresentation?.let { it.elapsedNow() < RepeatSuppression } == true) return
        if (items.isEmpty()) {
            deliver(CupertinoShareResult.Unavailable)
            return
        }
        isPresented = true
        lastPresentation = timeSource.markNow()
        val id = ++lastRequest
        activeRequest = id
        presenter.present(CupertinoShareRequest(items, subject), anchorBounds) { result ->
            // Delivers only the first result of the current request; late or repeated callbacks are dropped.
            if (activeRequest == id) {
                activeRequest = 0L
                deliver(result)
            }
        }
    }

    /** Convenience for sharing a single item. */
    fun share(item: CupertinoShareItem, subject: String? = null) = share(listOf(item), subject)

    /**
     * Forgets a request whose result never arrived (e.g. the platform silently refused to present the sheet, or the
     * state left the composition), so that [share] works again. A late result of that request is dropped.
     */
    internal fun abandonPendingRequest() {
        activeRequest = 0L
        isPresented = false
    }

    private fun deliver(result: CupertinoShareResult) {
        isPresented = false
        lastResult = result
        onResult(result)
    }

    internal companion object {
        /** Repeated [share] calls within this time after a presentation are ignored. */
        val RepeatSuppression: Duration = 500.milliseconds
    }
}

/**
 * Creates and remembers a [CupertinoShareSheetState] backed by the platform share UI
 * (`UIActivityViewController` on iOS, the Android chooser, a clipboard fallback elsewhere).
 *
 * ```
 * val shareSheet = rememberCupertinoShareSheetState { result -> println(result) }
 * CupertinoButton(
 *     onClick = { shareSheet.share(listOf(CupertinoShareItem.Url("https://ien.zone"))) },
 *     modifier = Modifier.cupertinoShareSheetAnchor(shareSheet),
 * ) { CupertinoText("Share") }
 * ```
 *
 * @param onResult called once for every [CupertinoShareSheetState.share] call, on the main thread
 */
@ExperimentalCupertinoApi
@Composable
fun rememberCupertinoShareSheetState(
    onResult: (CupertinoShareResult) -> Unit = {},
): CupertinoShareSheetState {
    val presenter = rememberPlatformSharePresenter()
    val currentOnResult by rememberUpdatedState(onResult)
    val state = remember { CupertinoShareSheetState(presenter).apply { this.onResult = { currentOnResult(it) } } }
    SideEffect { state.presenter = presenter }
    DisposableEffect(state) { onDispose { state.abandonPendingRequest() } }
    return state
}

/**
 * Marks this element as the anchor of the share sheet started by [state]. On iPad (and other regular width iOS
 * layouts) the sheet is shown as a popover pointing at this element; without an anchor it points at the center of
 * the screen. Has no visual effect on other platforms.
 *
 * Use one state per anchor: when several elements are anchored to the same state, the element positioned last is
 * used for every share.
 */
@ExperimentalCupertinoApi
fun Modifier.cupertinoShareSheetAnchor(state: CupertinoShareSheetState): Modifier =
    onGloballyPositioned { state.anchorBounds = it.boundsInRoot() }

/**
 * A button that opens the system share sheet, the equivalent of SwiftUI's `ShareLink`.
 *
 * The button is automatically used as the popover anchor on iPad.
 *
 * ```
 * CupertinoShareLink(
 *     items = listOf(CupertinoShareItem.Url("https://ien.zone")),
 *     subject = "compose-hig",
 *     onResult = { result -> println(result) },
 * )
 * ```
 *
 * @param items the content to share
 * @param modifier the [Modifier] to be applied to the button
 * @param subject optional subject, see [CupertinoShareSheetState.share]
 * @param enabled whether the button is enabled
 * @param onResult called with the outcome of every share, see [CupertinoShareResult] for platform differences
 * @param colors the button colors, a plain (borderless) button by default like SwiftUI's `ShareLink`
 * @param label the button content, a share icon followed by "Share" by default
 */
@ExperimentalCupertinoApi
@Composable
fun CupertinoShareLink(
    items: List<CupertinoShareItem>,
    modifier: Modifier = Modifier,
    subject: String? = null,
    enabled: Boolean = true,
    onResult: (CupertinoShareResult) -> Unit = {},
    colors: CupertinoButtonColors = CupertinoButtonDefaults.plainButtonColors(),
    label: @Composable RowScope.() -> Unit = { CupertinoShareSheetDefaults.Label() },
) = CupertinoShareLink(
    state = rememberCupertinoShareSheetState(onResult),
    items = items,
    modifier = modifier,
    subject = subject,
    enabled = enabled,
    colors = colors,
    label = label,
)

/**
 * A [CupertinoShareLink] driven by an existing [state], e.g. to observe [CupertinoShareSheetState.lastResult] or to
 * share the same state between several buttons.
 *
 * @param state the state that shows the share sheet and receives the result
 * @param items the content to share
 * @param modifier the [Modifier] to be applied to the button
 * @param subject optional subject, see [CupertinoShareSheetState.share]
 * @param enabled whether the button is enabled
 * @param colors the button colors
 * @param label the button content
 */
@ExperimentalCupertinoApi
@Composable
fun CupertinoShareLink(
    state: CupertinoShareSheetState,
    items: List<CupertinoShareItem>,
    modifier: Modifier = Modifier,
    subject: String? = null,
    enabled: Boolean = true,
    colors: CupertinoButtonColors = CupertinoButtonDefaults.plainButtonColors(),
    label: @Composable RowScope.() -> Unit = { CupertinoShareSheetDefaults.Label() },
) {
    CupertinoButton(
        onClick = { state.share(items, subject) },
        modifier = modifier.cupertinoShareSheetAnchor(state),
        enabled = enabled && items.isNotEmpty(),
        colors = colors,
        content = label,
    )
}

/**
 * Default values used by [CupertinoShareLink].
 */
@ExperimentalCupertinoApi
object CupertinoShareSheetDefaults {

    /** The SF Symbol `square.and.arrow.up` used as the share glyph. */
    val Icon: ImageVector
        get() = CupertinoIcons.Default.SquareAndArrowUp

    /**
     * The default [CupertinoShareLink] label: the share glyph followed by [text].
     *
     * @param text the label text. Pass a localized string; defaults to "Share".
     */
    @Composable
    fun Label(text: String = "Share") {
        CupertinoIcon(imageVector = Icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(6.dp))
        CupertinoText(text = text)
    }
}

/** Creates the platform specific [CupertinoSharePresenter]. */
@Composable
internal expect fun rememberPlatformSharePresenter(): CupertinoSharePresenter
