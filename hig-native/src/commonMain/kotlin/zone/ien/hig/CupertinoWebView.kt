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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import zone.ien.hig.theme.CupertinoTheme

/**
 * Displays web content with the platform's native web engine.
 *
 * - **iOS**: a `WKWebView` hosted in a `UIKitView`. Swipe-to-go-back/forward gestures follow
 *   [CupertinoWebViewSettings.allowsBackForwardNavigationGestures].
 * - **Android**: an `android.webkit.WebView` hosted in an `AndroidView`. The host app must declare
 *   `<uses-permission android:name="android.permission.INTERNET" />` in its manifest to load
 *   remote pages. System back is not routed to the web history by default; wire it into your
 *   app's back handling, e.g. `BackHandler(enabled = state.canGoBack) { state.navigator.goBack() }`
 *   from androidx.activity (see [CupertinoWebViewSettings.handleBackPress] for the opt-in
 *   alternative and its limits).
 * - **Desktop, macOS, JS and Wasm**: there is no embeddable web engine in Compose Multiplatform for
 *   these targets, so a themed placeholder is rendered instead. It shows the current URL and an
 *   "Open in Browser" button that hands the URL to [LocalUriHandler]. [onCreated] and [onDispose]
 *   are never called there.
 *
 * Links with a scheme other than `http`, `https`, `file`, `about`, `data` or `blob` (for example
 * `tel:`, `mailto:` or `intent:`) are not handed to other apps: Android shows its error page for
 * them and iOS does not open them. Opening them in other apps is not built in.
 *
 * Observe loading, progress, title, history and errors through [state]; drive navigation through
 * [CupertinoWebViewState.navigator].
 *
 * ```kotlin
 * val state = rememberCupertinoWebViewState("https://developer.apple.com")
 * Column {
 *     if (state.isLoading) CupertinoProgressIndicator(progress = state.progress)
 *     CupertinoWebView(state = state, modifier = Modifier.weight(1f))
 * }
 * ```
 *
 * @param state the state holder created by [rememberCupertinoWebViewState]. A state must be bound
 * to at most one [CupertinoWebView] at a time. Passing a different state replaces the native view
 * with a new one bound to that state.
 * @param modifier the [Modifier] applied to the web view.
 * @param settings engine settings such as JavaScript support. See [CupertinoWebViewSettings].
 * @param colors colors used for the web view background and the placeholder. The background is
 * shown before the first paint and behind transparent pages, so it follows the theme.
 * @param onCreated called once with the native view (`WKWebView` on iOS,
 * `android.webkit.WebView` on Android) right after it is created, for advanced configuration
 * such as JavaScript interfaces or cookies.
 * @param onDispose called with the native view right before it is released.
 */
@ExperimentalCupertinoApi
@Composable
fun CupertinoWebView(
    state: CupertinoWebViewState,
    modifier: Modifier = Modifier,
    settings: CupertinoWebViewSettings = CupertinoWebViewDefaults.settings(),
    colors: CupertinoWebViewColors = CupertinoWebViewDefaults.colors(),
    onCreated: (CupertinoPlatformWebView) -> Unit = {},
    onDispose: (CupertinoPlatformWebView) -> Unit = {},
) {
    // The native view and its clients are bound to one state, so a new state needs a new view.
    key(state) {
        PlatformCupertinoWebView(
            state = state,
            modifier = modifier,
            settings = settings,
            colors = colors,
            onCreated = onCreated,
            onDispose = onDispose,
        )
    }
}

/**
 * The native view backing a [CupertinoWebView]: `platform.WebKit.WKWebView` on iOS and
 * `android.webkit.WebView` on Android. On targets without a web engine it is an opaque type that is
 * never instantiated.
 */
@ExperimentalCupertinoApi
expect open class CupertinoPlatformWebView

@OptIn(ExperimentalCupertinoApi::class)
@Composable
internal expect fun PlatformCupertinoWebView(
    state: CupertinoWebViewState,
    modifier: Modifier,
    settings: CupertinoWebViewSettings,
    colors: CupertinoWebViewColors,
    onCreated: (CupertinoPlatformWebView) -> Unit,
    onDispose: (CupertinoPlatformWebView) -> Unit,
)

/**
 * Creates and remembers a [CupertinoWebViewState]. The state is saved with `rememberSaveable`, so
 * the current page survives configuration changes and process death: the [CupertinoWebViewState.url]
 * and [CupertinoWebViewState.title] on every platform, and on Android also the back/forward
 * history (through `WebView.saveState`).
 *
 * @param url the initial URL. Changing it later has no effect; call
 * [CupertinoWebViewNavigator.loadUrl] on [CupertinoWebViewState.navigator] to navigate.
 */
@ExperimentalCupertinoApi
@Composable
fun rememberCupertinoWebViewState(url: String): CupertinoWebViewState =
    rememberSaveable(saver = CupertinoWebViewState.Saver) { CupertinoWebViewState(url) }

/**
 * State of a [CupertinoWebView]. All properties are snapshot-backed and therefore observable from
 * composition.
 *
 * The state outlives the native view: if the [CupertinoWebView] leaves the composition and comes
 * back, the last known [url] is loaded again. On Android the back/forward history is restored as
 * well; on iOS it is not.
 */
@ExperimentalCupertinoApi
@Stable
class CupertinoWebViewState internal constructor(initialUrl: String) {

    /** The URL of the current main-frame page, or the requested URL before loading starts. */
    var url: String by mutableStateOf(initialUrl)
        private set

    /** Whether the main frame is currently loading. */
    var isLoading: Boolean by mutableStateOf(false)
        private set

    /** Estimated loading progress of the current page, always within `0f..1f`. */
    var progress: Float by mutableFloatStateOf(0f)
        private set

    /** Whether there is a previous page in the back history. */
    var canGoBack: Boolean by mutableStateOf(false)
        private set

    /** Whether there is a next page in the forward history. */
    var canGoForward: Boolean by mutableStateOf(false)
        private set

    /** The document title of the current page, or `null` if it has none (yet). */
    var title: String? by mutableStateOf(null)
        private set

    /**
     * The last main-frame loading error. Cleared when the next page load starts.
     */
    var lastError: CupertinoWebViewError? by mutableStateOf(null)
        private set

    /** Commands for navigating the web view bound to this state. */
    val navigator: CupertinoWebViewNavigator = CupertinoWebViewNavigator(this)

    private var controller: CupertinoWebViewController? = null

    /**
     * Engine state (Android: a `WebView.saveState` bundle) captured when the last native view was
     * released or restored from saved instance state; restored into the next attached view.
     */
    private var pendingEngineState: Any? = null

    internal fun attach(controller: CupertinoWebViewController) {
        this.controller = controller
        val engineState = pendingEngineState
        pendingEngineState = null
        if (engineState != null && controller.restoreState(engineState)) return
        if (url.isNotBlank()) controller.loadUrl(url)
    }

    internal fun detach(controller: CupertinoWebViewController) {
        if (this.controller !== controller) return
        pendingEngineState = controller.saveState()
        this.controller = null
        isLoading = false
        canGoBack = false
        canGoForward = false
    }

    internal fun requestLoad(url: String) {
        this.url = url
        val current = controller
        if (current == null) {
            // An explicit navigation wins over history captured from an earlier view.
            pendingEngineState = null
        } else {
            current.loadUrl(url)
        }
    }

    /** The engine state to save: from the attached view, or the one waiting to be restored. */
    internal fun engineStateToSave(): Any? = controller?.saveState() ?: pendingEngineState

    internal fun withController(block: CupertinoWebViewController.() -> Unit) {
        controller?.block()
    }

    internal fun onPageStarted(url: String?) {
        onUrlChanged(url)
        isLoading = true
        progress = 0f
        lastError = null
    }

    internal fun onPageFinished(url: String?) {
        onUrlChanged(url)
        isLoading = false
        progress = 1f
    }

    /**
     * Mirrors the engine's own loading flag. Only a transition to "not loading" is applied, which
     * ends a load that the engine stopped without a finish or error callback (e.g. after
     * [CupertinoWebViewNavigator.stopLoading] or a cancelled navigation). Progress and errors are
     * left untouched.
     */
    internal fun onLoadingChanged(loading: Boolean) {
        if (!loading && isLoading) isLoading = false
    }

    internal fun onProgressChanged(progress: Float) {
        if (progress.isNaN()) return
        this.progress = progress.coerceIn(0f, 1f)
    }

    internal fun onUrlChanged(url: String?) {
        if (!url.isNullOrBlank()) this.url = url
    }

    internal fun onTitleChanged(title: String?) {
        this.title = title?.takeIf { it.isNotBlank() }
    }

    internal fun onHistoryChanged(canGoBack: Boolean, canGoForward: Boolean) {
        this.canGoBack = canGoBack
        this.canGoForward = canGoForward
    }

    internal fun onError(error: CupertinoWebViewError) {
        lastError = error
        isLoading = false
    }

    internal companion object {
        /**
         * Saves [url], [title] and the platform engine state. Values that cannot be saved on the
         * platform are never produced: the engine state is a `Bundle` on Android and `null`
         * elsewhere.
         */
        val Saver: Saver<CupertinoWebViewState, Any> = listSaver<CupertinoWebViewState, Any?>(
            save = { state -> listOf(state.url, state.title, state.engineStateToSave()) },
            restore = { saved ->
                CupertinoWebViewState(saved[0] as String).apply {
                    title = saved[1] as String?
                    pendingEngineState = saved[2]
                }
            },
        )
    }
}

/**
 * Navigation commands for a [CupertinoWebView], obtained from [CupertinoWebViewState.navigator].
 * Commands issued while no web view is attached are ignored, except [loadUrl], which is remembered
 * and loaded as soon as the web view is created.
 *
 * All commands must be called on the main thread, because they call the native web view directly.
 */
@ExperimentalCupertinoApi
@Stable
class CupertinoWebViewNavigator internal constructor(private val state: CupertinoWebViewState) {

    /**
     * Loads [url] in the main frame. Use [CupertinoWebViewDefaults.normalizeUrl] to turn free-form
     * user input into a loadable URL.
     */
    fun loadUrl(url: String) = state.requestLoad(url)

    /** Reloads the current page. */
    fun reload() = state.withController { reload() }

    /** Stops loading the current page. */
    fun stopLoading() = state.withController { stopLoading() }

    /** Navigates back in the history if [CupertinoWebViewState.canGoBack]. */
    fun goBack() = state.withController { goBack() }

    /** Navigates forward in the history if [CupertinoWebViewState.canGoForward]. */
    fun goForward() = state.withController { goForward() }
}

/**
 * A main-frame loading error reported by the web engine.
 *
 * Only transport-level failures (no connection, unknown host, TLS, ...) are reported; an HTTP error
 * status such as 404 is a successfully loaded page and does not produce an error.
 *
 * @property code the platform error code: an `NSURLError` code on iOS, a `WebViewClient.ERROR_*`
 * constant on Android.
 * @property description a human-readable, platform-localized description.
 * @property failingUrl the URL that failed, if known.
 */
@ExperimentalCupertinoApi
@Immutable
data class CupertinoWebViewError(
    val code: Int,
    val description: String,
    val failingUrl: String?,
)

/**
 * Settings applied to the native web engine of a [CupertinoWebView]. Changes are applied to an
 * existing web view on recomposition and take effect for the next page load.
 *
 * @property javaScriptEnabled whether pages may run JavaScript.
 * @property domStorageEnabled Android only: whether `localStorage`/`sessionStorage` are available.
 * WKWebView always enables DOM storage.
 * @property allowsBackForwardNavigationGestures iOS only: whether horizontal edge swipes navigate
 * the history, as in Safari.
 * @property handleBackPress Android only, opt-in and meant for single-screen hosts: when `true`, the
 * system back gesture navigates back in the web history while [CupertinoWebViewState.canGoBack] is
 * `true`. It uses the framework `OnBackInvokedDispatcher` on API 33+ and the `KEYCODE_BACK` key
 * event (while the web view has focus) below that, so it needs no androidx.activity dependency.
 * Because it registers directly on the window, outside androidx's `OnBackPressedDispatcher`, its
 * order relative to `BackHandler`s, navigation libraries and sheets is not defined: it can steal
 * back from them or never receive it. In any app with other back consumers leave this `false` and
 * use `BackHandler(enabled = state.canGoBack) { state.navigator.goBack() }` from androidx.activity.
 * @property userAgent a custom user agent, or `null` for the engine default.
 */
@ExperimentalCupertinoApi
@Immutable
class CupertinoWebViewSettings internal constructor(
    val javaScriptEnabled: Boolean,
    val domStorageEnabled: Boolean,
    val allowsBackForwardNavigationGestures: Boolean,
    val handleBackPress: Boolean,
    val userAgent: String?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CupertinoWebViewSettings) return false
        return javaScriptEnabled == other.javaScriptEnabled &&
            domStorageEnabled == other.domStorageEnabled &&
            allowsBackForwardNavigationGestures == other.allowsBackForwardNavigationGestures &&
            handleBackPress == other.handleBackPress &&
            userAgent == other.userAgent
    }

    override fun hashCode(): Int {
        var result = javaScriptEnabled.hashCode()
        result = 31 * result + domStorageEnabled.hashCode()
        result = 31 * result + allowsBackForwardNavigationGestures.hashCode()
        result = 31 * result + handleBackPress.hashCode()
        result = 31 * result + (userAgent?.hashCode() ?: 0)
        return result
    }
}

/**
 * Colors used by [CupertinoWebView].
 *
 * @property containerColor the background behind the page; visible before the first paint and
 * behind transparent pages.
 * @property placeholderContentColor the color of secondary text in the placeholder shown on targets
 * without a web engine.
 */
@ExperimentalCupertinoApi
@Immutable
class CupertinoWebViewColors internal constructor(
    val containerColor: Color,
    val placeholderContentColor: Color,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CupertinoWebViewColors) return false
        return containerColor == other.containerColor &&
            placeholderContentColor == other.placeholderContentColor
    }

    override fun hashCode(): Int = 31 * containerColor.hashCode() + placeholderContentColor.hashCode()
}

/** Defaults used by [CupertinoWebView]. */
@ExperimentalCupertinoApi
object CupertinoWebViewDefaults {

    /** The search URL prefix used by [normalizeUrl] for input that is not a URL. */
    const val DefaultSearchUrl: String = "https://duckduckgo.com/?q="

    /** Creates [CupertinoWebViewSettings]; see its properties for details. */
    fun settings(
        javaScriptEnabled: Boolean = true,
        domStorageEnabled: Boolean = true,
        allowsBackForwardNavigationGestures: Boolean = true,
        handleBackPress: Boolean = false,
        userAgent: String? = null,
    ): CupertinoWebViewSettings = CupertinoWebViewSettings(
        javaScriptEnabled = javaScriptEnabled,
        domStorageEnabled = domStorageEnabled,
        allowsBackForwardNavigationGestures = allowsBackForwardNavigationGestures,
        handleBackPress = handleBackPress,
        userAgent = userAgent,
    )

    /**
     * Creates [CupertinoWebViewColors].
     *
     * @param containerColor background behind the page, the system background by default.
     * @param placeholderContentColor secondary text color of the placeholder on unsupported targets.
     */
    @Composable
    @ReadOnlyComposable
    fun colors(
        containerColor: Color = CupertinoTheme.colorScheme.systemBackground,
        placeholderContentColor: Color = CupertinoTheme.colorScheme.secondaryLabel,
    ): CupertinoWebViewColors = CupertinoWebViewColors(
        containerColor = containerColor,
        placeholderContentColor = placeholderContentColor,
    )

    /**
     * Turns free-form address bar input into a loadable URL, like Safari's address field:
     *
     * - blank input returns `null`;
     * - input with an `http`, `https`, `file`, `about` or `data` scheme is returned trimmed;
     * - `localhost` and IPv4 hosts (optionally with port and path) get `http://`;
     * - host names with a dot and an alphabetic top-level domain get `https://`;
     * - anything else is treated as a search query appended, form-encoded, to [searchUrl], or
     *   returns `null` when [searchUrl] is `null`.
     */
    fun normalizeUrl(input: String, searchUrl: String? = DefaultSearchUrl): String? {
        val text = input.trim()
        if (text.isEmpty()) return null
        if (SchemeRegex.containsMatchIn(text)) return text
        if (text.none { it.isWhitespace() }) {
            val host = text.substringBefore('/').substringBefore('?').substringBefore('#')
            val hostName = host.substringBefore(':')
            val port = host.substringAfter(':', missingDelimiterValue = "")
            val validPort = !host.contains(':') || (port.isNotEmpty() && port.all { it.isDigit() })
            if (validPort) {
                if (hostName.equals("localhost", ignoreCase = true) || Ipv4Regex.matches(hostName)) {
                    return "http://$text"
                }
                if (DomainRegex.matches(hostName)) return "https://$text"
            }
        }
        return searchUrl?.let { it + formEncode(text) }
    }

    private val SchemeRegex = Regex("^(https?|file|about|data):", RegexOption.IGNORE_CASE)
    private val Ipv4Regex = Regex("^(\\d{1,3})(\\.\\d{1,3}){3}$")
    private val DomainRegex = Regex("^([a-zA-Z0-9]([a-zA-Z0-9-]*[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,}$")

    private fun formEncode(text: String): String = buildString {
        text.encodeToByteArray().forEach { byte ->
            val c = byte.toInt() and 0xFF
            when {
                c == ' '.code -> append('+')
                c in 'a'.code..'z'.code || c in 'A'.code..'Z'.code || c in '0'.code..'9'.code ||
                    c == '-'.code || c == '_'.code || c == '.'.code || c == '~'.code -> append(c.toChar())
                else -> {
                    append('%')
                    append(HexDigits[c shr 4])
                    append(HexDigits[c and 0x0F])
                }
            }
        }
    }

    private const val HexDigits = "0123456789ABCDEF"
}

/** Platform bridge used by [CupertinoWebViewNavigator]. */
internal interface CupertinoWebViewController {
    fun loadUrl(url: String)
    fun reload()
    fun stopLoading()
    fun goBack()
    fun goForward()

    /** Returns the engine state to save (a platform-saveable value), or `null`. */
    fun saveState(): Any? = null

    /** Restores [saved] from [saveState]; returns `false` if nothing was restored. */
    fun restoreState(saved: Any): Boolean = false
}

/**
 * Placeholder shown on targets without an embeddable web engine. It keeps [state] in sync with
 * [CupertinoWebViewNavigator.loadUrl] so address bars built on top of it still work.
 */
@OptIn(ExperimentalCupertinoApi::class)
@Composable
internal fun CupertinoWebViewPlaceholder(
    state: CupertinoWebViewState,
    modifier: Modifier,
    colors: CupertinoWebViewColors,
    platformName: String,
) {
    val uriHandler = LocalUriHandler.current
    DisposableEffect(state) {
        val controller = object : CupertinoWebViewController {
            override fun loadUrl(url: String) = Unit
            override fun reload() = Unit
            override fun stopLoading() = Unit
            override fun goBack() = Unit
            override fun goForward() = Unit
        }
        state.attach(controller)
        onDispose { state.detach(controller) }
    }
    CupertinoSurface(
        modifier = modifier,
        color = colors.containerColor,
    ) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CupertinoText(
                    text = "Web content isn't available on $platformName",
                    style = CupertinoTheme.typography.headline,
                    textAlign = TextAlign.Center,
                )
                if (state.url.isNotBlank()) {
                    CupertinoText(
                        text = state.url,
                        style = CupertinoTheme.typography.footnote,
                        color = colors.placeholderContentColor,
                        textAlign = TextAlign.Center,
                        maxLines = 3,
                    )
                    CupertinoButton(
                        // openUri throws when no browser can be launched (e.g. headless desktop).
                        onClick = { runCatching { uriHandler.openUri(state.url) } },
                    ) {
                        CupertinoText("Open in Browser")
                    }
                }
            }
        }
    }
}
