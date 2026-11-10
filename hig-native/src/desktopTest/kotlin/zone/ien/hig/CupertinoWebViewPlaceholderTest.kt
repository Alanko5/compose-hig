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

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import zone.ien.hig.theme.CupertinoTheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class, ExperimentalCupertinoApi::class)
class CupertinoWebViewPlaceholderTest {

    private class RecordingUriHandler : UriHandler {
        val opened = mutableListOf<String>()
        override fun openUri(uri: String) {
            opened += uri
        }
    }

    @Test
    fun showsTheUrlAndOpensItInTheBrowser() = runComposeUiTest {
        val uriHandler = RecordingUriHandler()
        setContent {
            CupertinoTheme {
                CompositionLocalProvider(LocalUriHandler provides uriHandler) {
                    CupertinoWebView(state = rememberCupertinoWebViewState("https://example.com"))
                }
            }
        }

        onNodeWithText("Web content isn't available on Desktop").assertIsDisplayed()
        onNodeWithText("https://example.com").assertIsDisplayed()
        onNodeWithText("Open in Browser").performClick()

        assertEquals(listOf("https://example.com"), uriHandler.opened)
    }

    @Test
    fun navigatorLoadUrlUpdatesThePlaceholder() = runComposeUiTest {
        val state = CupertinoWebViewState("https://example.com")
        setContent {
            CupertinoTheme {
                CupertinoWebView(state = state)
            }
        }

        state.navigator.loadUrl("https://kotlinlang.org")
        waitForIdle()

        onNodeWithText("https://kotlinlang.org").assertIsDisplayed()
        assertEquals("https://kotlinlang.org", state.url)
    }

    @Test
    fun openInBrowserFailureDoesNotCrash() = runComposeUiTest {
        val failingHandler = object : UriHandler {
            override fun openUri(uri: String) = throw UnsupportedOperationException("no browser")
        }
        setContent {
            CupertinoTheme {
                CompositionLocalProvider(LocalUriHandler provides failingHandler) {
                    CupertinoWebView(state = rememberCupertinoWebViewState("https://example.com"))
                }
            }
        }

        onNodeWithText("Open in Browser").performClick()
        onNodeWithText("https://example.com").assertIsDisplayed()
    }

    @Test
    fun swappingTheStateRebindsTheView() = runComposeUiTest {
        val first = CupertinoWebViewState("https://example.com")
        val second = CupertinoWebViewState("https://kotlinlang.org")
        var current by mutableStateOf(first)
        setContent {
            CupertinoTheme {
                CupertinoWebView(state = current)
            }
        }

        current = second
        waitForIdle()
        onNodeWithText("https://kotlinlang.org").assertIsDisplayed()

        // The old state is detached: its loadUrl is only remembered, the new one drives the view.
        first.navigator.loadUrl("https://example.org")
        second.navigator.loadUrl("https://jetbrains.com")
        waitForIdle()
        onNodeWithText("https://jetbrains.com").assertIsDisplayed()
        onNodeWithText("https://example.org").assertDoesNotExist()
    }

    @Test
    fun blankUrlHidesTheOpenButton() = runComposeUiTest {
        setContent {
            CupertinoTheme {
                CupertinoWebView(state = rememberCupertinoWebViewState(""))
            }
        }

        onNodeWithText("Web content isn't available on Desktop").assertIsDisplayed()
        onNodeWithText("Open in Browser").assertDoesNotExist()
    }
}
