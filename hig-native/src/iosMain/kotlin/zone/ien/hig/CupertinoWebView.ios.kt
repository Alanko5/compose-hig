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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.cinterop.readValue
import kotlinx.coroutines.delay
import platform.CoreGraphics.CGRectZero
import platform.Foundation.NSError
import platform.Foundation.NSURL
import platform.Foundation.NSURLErrorCancelled
import platform.Foundation.NSURLErrorDomain
import platform.Foundation.NSURLErrorFailingURLErrorKey
import platform.Foundation.NSURLErrorFailingURLStringErrorKey
import platform.Foundation.NSURLRequest
import platform.UIKit.UIUserInterfaceStyle
import platform.WebKit.WKNavigation
import platform.WebKit.WKNavigationAction
import platform.WebKit.WKNavigationActionPolicy
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKUIDelegateProtocol
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.WebKit.WKWebpagePreferences
import platform.WebKit.WKWindowFeatures
import platform.darwin.NSObject
import zone.ien.hig.theme.CupertinoTheme

actual typealias CupertinoPlatformWebView = WKWebView

/** How often title, URL and history are re-read while no page is loading (e.g. `pushState`). */
private const val IdleSyncIntervalMillis = 500L

@OptIn(ExperimentalCupertinoApi::class, ExperimentalForeignApi::class)
@Composable
internal actual fun PlatformCupertinoWebView(
    state: CupertinoWebViewState,
    modifier: Modifier,
    settings: CupertinoWebViewSettings,
    colors: CupertinoWebViewColors,
    onCreated: (CupertinoPlatformWebView) -> Unit,
    onDispose: (CupertinoPlatformWebView) -> Unit,
) {
    val currentOnCreated by rememberUpdatedState(onCreated)
    val currentOnDispose by rememberUpdatedState(onDispose)
    val dark = CupertinoTheme.colorScheme.isDark
    // Holds the navigation delegate, which WebKit only references weakly.
    var controller by remember { mutableStateOf<IosWebViewController?>(null) }

    // Kotlin/Native cannot override NSObject's KVO callback, so progress, title, URL, history and
    // the loading flag are sampled: every frame while loading, and at a low rate otherwise.
    // Unchanged values do not invalidate snapshot state, so this does not cause recompositions on
    // its own. Because sync() also mirrors `WKWebView.loading`, the per-frame branch always ends
    // when the engine stops loading, even without a delegate callback (e.g. after stopLoading()).
    LaunchedEffect(controller) {
        val current = controller ?: return@LaunchedEffect
        while (true) {
            if (state.isLoading) {
                withFrameNanos { current.sync() }
            } else {
                // Skip the work while the view is off-screen; the next on-screen sample catches up.
                if (current.isInWindow) current.sync()
                delay(IdleSyncIntervalMillis)
            }
        }
    }

    UIKitView(
        factory = {
            val configuration = WKWebViewConfiguration().apply {
                defaultWebpagePreferences.allowsContentJavaScript = settings.javaScriptEnabled
            }
            val webView = WKWebView(frame = CGRectZero.readValue(), configuration = configuration)
            val created = IosWebViewController(webView, state, settings)
            webView.navigationDelegate = created.navigationDelegate
            webView.UIDelegate = created.uiDelegate
            webView.applySettings(settings)
            webView.applyColors(colors, dark)
            controller = created
            currentOnCreated(webView)
            state.attach(created)
            webView
        },
        modifier = modifier,
        update = { webView ->
            controller?.settings = settings
            webView.applySettings(settings)
            webView.applyColors(colors, dark)
        },
        onRelease = { webView ->
            controller?.let(state::detach)
            controller = null
            currentOnDispose(webView)
            webView.stopLoading()
            webView.navigationDelegate = null
            webView.UIDelegate = null
        },
        properties = UIKitInteropProperties(
            interactionMode = UIKitInteropInteractionMode.NonCooperative,
            isNativeAccessibilityEnabled = true,
        ),
    )
}

@OptIn(ExperimentalCupertinoApi::class)
private fun WKWebView.applySettings(settings: CupertinoWebViewSettings) {
    allowsBackForwardNavigationGestures = settings.allowsBackForwardNavigationGestures
    if (customUserAgent != settings.userAgent) customUserAgent = settings.userAgent
}

@OptIn(ExperimentalCupertinoApi::class)
private fun WKWebView.applyColors(colors: CupertinoWebViewColors, dark: Boolean) {
    val background = colors.containerColor.toUIColor()
    // A non-opaque web view shows its background color until the page paints and behind
    // transparent pages, instead of a white flash in dark mode.
    setOpaque(false)
    backgroundColor = background
    scrollView.backgroundColor = background
    underPageBackgroundColor = background
    // Drives `prefers-color-scheme` for the page.
    overrideUserInterfaceStyle = if (dark) {
        UIUserInterfaceStyle.UIUserInterfaceStyleDark
    } else {
        UIUserInterfaceStyle.UIUserInterfaceStyleLight
    }
}

@OptIn(ExperimentalCupertinoApi::class, ExperimentalForeignApi::class)
private class IosWebViewController(
    private val webView: WKWebView,
    private val state: CupertinoWebViewState,
    var settings: CupertinoWebViewSettings,
) : CupertinoWebViewController {

    // WebKit references both delegates weakly; this controller keeps them alive.
    val navigationDelegate = NavigationDelegate()
    val uiDelegate = UIDelegate()

    val isInWindow: Boolean get() = webView.window != null

    /** Copies the web view's observable properties into [state]. */
    fun sync() {
        if (state.isLoading) state.onProgressChanged(webView.estimatedProgress.toFloat())
        state.onLoadingChanged(webView.loading)
        state.onTitleChanged(webView.title)
        state.onUrlChanged(webView.URL?.absoluteString)
        syncHistory()
    }

    override fun loadUrl(url: String) {
        val nsUrl = NSURL.URLWithString(url)
        if (nsUrl == null) {
            state.onError(CupertinoWebViewError(code = -1000, description = "Invalid URL", failingUrl = url))
            return
        }
        if (nsUrl.isFileURL()) {
            webView.loadFileURL(nsUrl, allowingReadAccessToURL = nsUrl)
        } else {
            webView.loadRequest(NSURLRequest.requestWithURL(nsUrl))
        }
    }

    override fun reload() {
        webView.reload()
    }

    override fun stopLoading() {
        webView.stopLoading()
        state.onLoadingChanged(webView.loading)
    }

    override fun goBack() {
        if (webView.canGoBack) webView.goBack()
    }

    override fun goForward() {
        if (webView.canGoForward) webView.goForward()
    }

    private fun syncHistory() {
        state.onHistoryChanged(webView.canGoBack, webView.canGoForward)
    }

    private fun reportError(error: NSError) {
        // A navigation cancelled by a newer one (fast taps, redirects) is not a failure. If nothing
        // replaced it (stopLoading(), a cancelled policy decision), the load has ended.
        if (error.domain == NSURLErrorDomain && error.code == NSURLErrorCancelled) {
            state.onLoadingChanged(webView.loading)
            return
        }
        state.onError(
            CupertinoWebViewError(
                code = error.code.toInt(),
                description = error.localizedDescription,
                failingUrl = error.failingUrl() ?: webView.URL?.absoluteString,
            ),
        )
    }

    /**
     * The URL that failed. For a provisional failure `webView.URL` is still the previous page, so
     * the error's own user info is preferred.
     */
    private fun NSError.failingUrl(): String? {
        val info = userInfo
        return info[NSURLErrorFailingURLStringErrorKey] as? String
            ?: (info[NSURLErrorFailingURLErrorKey] as? NSURL)?.absoluteString
    }

    /**
     * Opens links that ask for a new window (`target="_blank"`, `window.open`) in the same web
     * view, as Android's WebView does by default. Without a UI delegate WKWebView drops them.
     */
    inner class UIDelegate : NSObject(), WKUIDelegateProtocol {
        override fun webView(
            webView: WKWebView,
            createWebViewWithConfiguration: WKWebViewConfiguration,
            forNavigationAction: WKNavigationAction,
            windowFeatures: WKWindowFeatures,
        ): WKWebView? {
            if (forNavigationAction.targetFrame == null) {
                webView.loadRequest(forNavigationAction.request)
            }
            return null
        }
    }

    inner class NavigationDelegate : NSObject(), WKNavigationDelegateProtocol {
        override fun webView(
            webView: WKWebView,
            decidePolicyForNavigationAction: WKNavigationAction,
            preferences: WKWebpagePreferences,
            decisionHandler: (WKNavigationActionPolicy, WKWebpagePreferences?) -> Unit,
        ) {
            preferences.allowsContentJavaScript = settings.javaScriptEnabled
            decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyAllow, preferences)
        }

        @ObjCSignatureOverride
        override fun webView(webView: WKWebView, didStartProvisionalNavigation: WKNavigation?) {
            state.onPageStarted(webView.URL?.absoluteString)
            syncHistory()
        }

        @ObjCSignatureOverride
        override fun webView(webView: WKWebView, didFinishNavigation: WKNavigation?) {
            state.onPageFinished(webView.URL?.absoluteString)
            state.onTitleChanged(webView.title)
            syncHistory()
        }

        @ObjCSignatureOverride
        override fun webView(webView: WKWebView, didFailProvisionalNavigation: WKNavigation?, withError: NSError) {
            reportError(withError)
        }

        @ObjCSignatureOverride
        override fun webView(webView: WKWebView, didFailNavigation: WKNavigation?, withError: NSError) {
            reportError(withError)
        }
    }
}
