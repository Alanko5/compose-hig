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

import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView

actual typealias CupertinoPlatformWebView = WebView

@OptIn(ExperimentalCupertinoApi::class)
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

    var webView by remember { mutableStateOf<WebView?>(null) }

    WebViewBackHandler(
        webView = webView,
        enabled = settings.handleBackPress && state.canGoBack,
        onBack = state.navigator::goBack,
    )

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                val controller = AndroidWebViewController(this, state)
                tag = controller
                webViewClient = StateWebViewClient(state)
                webChromeClient = StateWebChromeClient(state)
                applySettings(settings)
                setBackgroundColor(colors.containerColor.toArgb())
                setOnKeyListener { _, keyCode, event ->
                    // Pre-predictive-back path (API < 33, or apps not opted in to OnBackInvokedCallback).
                    val handles = keyCode == KeyEvent.KEYCODE_BACK &&
                        controller.appliedSettings?.handleBackPress == true && canGoBack()
                    if (handles && event.action == KeyEvent.ACTION_UP) goBack()
                    handles
                }
                webView = this
                currentOnCreated(this)
                state.attach(controller)
            }
        },
        modifier = modifier,
        update = { view ->
            view.applySettings(settings)
            view.setBackgroundColor(colors.containerColor.toArgb())
        },
        onRelease = { released ->
            (released.tag as? AndroidWebViewController)?.let(state::detach)
            if (webView === released) webView = null
            currentOnDispose(released)
            released.stopLoading()
            released.destroy()
        },
    )
}

/**
 * Routes the system back gesture to the web history without depending on androidx.activity:
 * on API 33+ an [OnBackInvokedCallback] is registered on the web view's window while [enabled];
 * on older releases (or apps not opted in to predictive back) the web view's key listener handles
 * `KEYCODE_BACK` while it has focus.
 */
@Composable
private fun WebViewBackHandler(webView: WebView?, enabled: Boolean, onBack: () -> Unit) {
    val currentOnBack by rememberUpdatedState(onBack)
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    DisposableEffect(webView, enabled) {
        val view = webView?.takeIf { enabled } ?: return@DisposableEffect onDispose {}
        val callback = OnBackInvokedCallback { currentOnBack() }
        var dispatcher: OnBackInvokedDispatcher? = null
        fun register() {
            if (dispatcher != null) return
            dispatcher = view.findOnBackInvokedDispatcher()
            dispatcher?.registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT, callback)
        }
        fun unregister() {
            dispatcher?.unregisterOnBackInvokedCallback(callback)
            dispatcher = null
        }
        val attachListener = object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) = register()
            override fun onViewDetachedFromWindow(v: View) = unregister()
        }
        view.addOnAttachStateChangeListener(attachListener)
        if (view.isAttachedToWindow) register()
        onDispose {
            view.removeOnAttachStateChangeListener(attachListener)
            unregister()
        }
    }
}

private fun WebView.applySettings(settings: CupertinoWebViewSettings) {
    val controller = tag as? AndroidWebViewController
    if (controller?.appliedSettings == settings) return
    val webSettings: WebSettings = this.settings
    @Suppress("SetJavaScriptEnabled")
    webSettings.javaScriptEnabled = settings.javaScriptEnabled
    webSettings.domStorageEnabled = settings.domStorageEnabled
    // A null user agent restores the engine default.
    webSettings.userAgentString = settings.userAgent
    controller?.appliedSettings = settings
}

@OptIn(ExperimentalCupertinoApi::class)
private class AndroidWebViewController(
    private val webView: WebView,
    private val state: CupertinoWebViewState,
) : CupertinoWebViewController {
    var appliedSettings: CupertinoWebViewSettings? = null

    override fun loadUrl(url: String) = webView.loadUrl(url)
    override fun reload() = webView.reload()
    override fun stopLoading() {
        webView.stopLoading()
        // Do not rely on onPageFinished after a stop to end the loading state.
        state.onLoadingChanged(false)
    }

    /** The back/forward history as a [Bundle], which `rememberSaveable` can store. */
    override fun saveState(): Any? {
        val bundle = Bundle()
        return if (webView.saveState(bundle) != null) bundle else null
    }

    override fun restoreState(saved: Any): Boolean {
        val bundle = saved as? Bundle ?: return false
        return webView.restoreState(bundle) != null
    }
    override fun goBack() {
        if (webView.canGoBack()) webView.goBack()
    }
    override fun goForward() {
        if (webView.canGoForward()) webView.goForward()
    }
}

@OptIn(ExperimentalCupertinoApi::class)
private class StateWebViewClient(private val state: CupertinoWebViewState) : WebViewClient() {
    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
        state.onPageStarted(url)
        state.onHistoryChanged(view.canGoBack(), view.canGoForward())
    }

    override fun onPageFinished(view: WebView, url: String?) {
        state.onPageFinished(url)
        state.onTitleChanged(view.title)
        state.onHistoryChanged(view.canGoBack(), view.canGoForward())
    }

    override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
        state.onUrlChanged(url)
        state.onHistoryChanged(view.canGoBack(), view.canGoForward())
    }

    override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
        if (!request.isForMainFrame) return
        state.onError(
            CupertinoWebViewError(
                code = error.errorCode,
                description = error.description?.toString().orEmpty(),
                failingUrl = request.url?.toString(),
            ),
        )
    }
}

@OptIn(ExperimentalCupertinoApi::class)
private class StateWebChromeClient(private val state: CupertinoWebViewState) : WebChromeClient() {
    override fun onProgressChanged(view: WebView, newProgress: Int) {
        state.onProgressChanged(newProgress / 100f)
    }

    override fun onReceivedTitle(view: WebView, title: String?) {
        state.onTitleChanged(title)
    }
}
