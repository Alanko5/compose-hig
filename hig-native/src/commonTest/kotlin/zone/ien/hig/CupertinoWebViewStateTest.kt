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

import androidx.compose.runtime.saveable.SaverScope
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCupertinoApi::class)
class CupertinoWebViewStateTest {

    private class RecordingController(
        private val engineState: Any? = null,
        private val canRestore: Boolean = true,
    ) : CupertinoWebViewController {
        val calls = mutableListOf<String>()
        override fun loadUrl(url: String) { calls += "load:$url" }
        override fun reload() { calls += "reload" }
        override fun stopLoading() { calls += "stop" }
        override fun goBack() { calls += "back" }
        override fun goForward() { calls += "forward" }
        override fun saveState(): Any? = engineState
        override fun restoreState(saved: Any): Boolean {
            calls += "restore:$saved"
            return canRestore
        }
    }

    private val saverScope = SaverScope { true }

    @Test
    fun initialStateExposesTheRequestedUrlAndIsIdle() {
        val state = CupertinoWebViewState("https://example.com")
        assertEquals("https://example.com", state.url)
        assertFalse(state.isLoading)
        assertEquals(0f, state.progress)
        assertFalse(state.canGoBack)
        assertFalse(state.canGoForward)
        assertNull(state.title)
        assertNull(state.lastError)
    }

    @Test
    fun pageStartedBeginsLoadingAndClearsThePreviousError() {
        val state = CupertinoWebViewState("https://example.com")
        state.onError(CupertinoWebViewError(-2, "offline", "https://example.com"))
        state.onProgressChanged(0.7f)

        state.onPageStarted("https://example.com/next")

        assertTrue(state.isLoading)
        assertEquals(0f, state.progress)
        assertNull(state.lastError)
        assertEquals("https://example.com/next", state.url)
    }

    @Test
    fun pageFinishedCompletesProgressAndKeepsAnErrorReportedDuringTheLoad() {
        val state = CupertinoWebViewState("https://example.com")
        state.onPageStarted("https://example.com")
        val error = CupertinoWebViewError(-6, "Connection refused", "https://example.com")
        state.onError(error)

        state.onPageFinished("https://example.com")

        assertFalse(state.isLoading)
        assertEquals(1f, state.progress)
        assertEquals(error, state.lastError)
    }

    @Test
    fun progressIsClampedToTheUnitRangeAndIgnoresNaN() {
        val state = CupertinoWebViewState("")
        state.onProgressChanged(1.5f)
        assertEquals(1f, state.progress)
        state.onProgressChanged(-0.2f)
        assertEquals(0f, state.progress)
        state.onProgressChanged(0.4f)
        state.onProgressChanged(Float.NaN)
        assertEquals(0.4f, state.progress)
    }

    @Test
    fun errorStopsLoading() {
        val state = CupertinoWebViewState("https://example.com")
        state.onPageStarted("https://example.com")
        state.onError(CupertinoWebViewError(-6, "Connection refused", null))
        assertFalse(state.isLoading)
        assertEquals(-6, state.lastError?.code)
    }

    @Test
    fun blankTitlesAreReportedAsNull() {
        val state = CupertinoWebViewState("https://example.com")
        state.onTitleChanged("Example")
        assertEquals("Example", state.title)
        state.onTitleChanged("   ")
        assertNull(state.title)
    }

    @Test
    fun historyAndUrlUpdatesAreApplied() {
        val state = CupertinoWebViewState("https://example.com")
        state.onHistoryChanged(canGoBack = true, canGoForward = false)
        state.onUrlChanged("https://example.com/a")
        assertTrue(state.canGoBack)
        assertFalse(state.canGoForward)
        assertEquals("https://example.com/a", state.url)
        state.onUrlChanged(null)
        assertEquals("https://example.com/a", state.url)
    }

    @Test
    fun attachingAControllerLoadsTheInitialUrlOnce() {
        val state = CupertinoWebViewState("https://example.com")
        val controller = RecordingController()
        state.attach(controller)
        assertEquals(listOf("load:https://example.com"), controller.calls)
    }

    @Test
    fun attachingWithABlankUrlDoesNotLoadAnything() {
        val state = CupertinoWebViewState("  ")
        val controller = RecordingController()
        state.attach(controller)
        assertTrue(controller.calls.isEmpty())
    }

    @Test
    fun navigatorCommandsAreForwardedToTheAttachedController() {
        val state = CupertinoWebViewState("https://example.com")
        val controller = RecordingController()
        state.attach(controller)
        controller.calls.clear()

        state.navigator.loadUrl("https://example.org")
        state.navigator.reload()
        state.navigator.stopLoading()
        state.navigator.goBack()
        state.navigator.goForward()

        assertEquals(listOf("load:https://example.org", "reload", "stop", "back", "forward"), controller.calls)
    }

    @Test
    fun loadUrlBeforeAttachIsDeferredAndReplacesTheInitialUrl() {
        val state = CupertinoWebViewState("https://example.com")
        state.navigator.loadUrl("https://example.org")
        assertEquals("https://example.org", state.url)

        val controller = RecordingController()
        state.attach(controller)
        assertEquals(listOf("load:https://example.org"), controller.calls)
    }

    @Test
    fun reattachingAfterDetachRestoresTheLastKnownUrl() {
        val state = CupertinoWebViewState("https://example.com")
        val first = RecordingController()
        state.attach(first)
        state.onUrlChanged("https://example.com/deep")
        state.onHistoryChanged(canGoBack = true, canGoForward = true)
        state.onPageStarted("https://example.com/deep")
        state.detach(first)

        assertFalse(state.isLoading)
        assertFalse(state.canGoBack)
        assertFalse(state.canGoForward)

        val second = RecordingController()
        state.attach(second)
        assertEquals(listOf("load:https://example.com/deep"), second.calls)
    }

    @Test
    fun detachingAStaleControllerKeepsTheCurrentOne() {
        val state = CupertinoWebViewState("https://example.com")
        val first = RecordingController()
        val second = RecordingController()
        state.attach(first)
        state.attach(second)
        state.detach(first)
        second.calls.clear()
        state.navigator.reload()
        assertEquals(listOf("reload"), second.calls)
        assertTrue(first.calls.none { it == "reload" })
    }

    @Test
    fun navigationCommandsWithoutAControllerAreIgnored() {
        val state = CupertinoWebViewState("https://example.com")
        state.navigator.reload()
        state.navigator.goBack()
        state.navigator.goForward()
        state.navigator.stopLoading()
        assertEquals("https://example.com", state.url)

        val controller = RecordingController()
        state.attach(controller)
        assertEquals(listOf("load:https://example.com"), controller.calls)
    }

    @Test
    fun loadingEndsWhenTheEngineStopsWithoutACallback() {
        val state = CupertinoWebViewState("https://example.com")
        state.onPageStarted("https://example.com")
        state.onProgressChanged(0.4f)

        state.onLoadingChanged(true)
        assertTrue(state.isLoading)

        state.onLoadingChanged(false)
        assertFalse(state.isLoading)
        assertEquals(0.4f, state.progress)
        assertNull(state.lastError)
    }

    @Test
    fun engineLoadingFlagDoesNotStartALoadOrClearAnError() {
        val state = CupertinoWebViewState("https://example.com")
        val error = CupertinoWebViewError(-6, "Connection refused", null)
        state.onError(error)

        state.onLoadingChanged(true)

        assertFalse(state.isLoading)
        assertEquals(error, state.lastError)
    }

    @Test
    fun saverRestoresUrlTitleAndEngineState() {
        val state = CupertinoWebViewState("https://example.com")
        state.attach(RecordingController(engineState = "history"))
        state.onPageFinished("https://example.com/deep")
        state.onTitleChanged("Deep")

        val saved = with(CupertinoWebViewState.Saver) { saverScope.save(state) }!!
        val restored = CupertinoWebViewState.Saver.restore(saved)!!

        assertEquals("https://example.com/deep", restored.url)
        assertEquals("Deep", restored.title)
        assertFalse(restored.isLoading)
        val controller = RecordingController()
        restored.attach(controller)
        assertEquals(listOf("restore:history"), controller.calls)
    }

    @Test
    fun saverWithoutEngineStateLoadsTheSavedUrl() {
        val state = CupertinoWebViewState("https://example.com")
        state.onUrlChanged("https://example.com/deep")

        val saved = with(CupertinoWebViewState.Saver) { saverScope.save(state) }!!
        val restored = CupertinoWebViewState.Saver.restore(saved)!!

        val controller = RecordingController()
        restored.attach(controller)
        assertEquals(listOf("load:https://example.com/deep"), controller.calls)
    }

    @Test
    fun reattachingRestoresTheEngineStateOfTheReleasedView() {
        val state = CupertinoWebViewState("https://example.com")
        val first = RecordingController(engineState = "history")
        state.attach(first)
        state.detach(first)

        val second = RecordingController()
        state.attach(second)
        assertEquals(listOf("restore:history"), second.calls)

        // The engine state is consumed once.
        state.detach(second)
        val third = RecordingController()
        state.attach(third)
        assertEquals(listOf("load:https://example.com"), third.calls)
    }

    @Test
    fun failedEngineRestoreFallsBackToLoadingTheUrl() {
        val state = CupertinoWebViewState("https://example.com")
        val first = RecordingController(engineState = "history")
        state.attach(first)
        state.detach(first)

        val second = RecordingController(canRestore = false)
        state.attach(second)
        assertEquals(listOf("restore:history", "load:https://example.com"), second.calls)
    }

    @Test
    fun loadUrlWhileDetachedDiscardsTheSavedEngineState() {
        val state = CupertinoWebViewState("https://example.com")
        val first = RecordingController(engineState = "history")
        state.attach(first)
        state.detach(first)

        state.navigator.loadUrl("https://example.org")

        val second = RecordingController()
        state.attach(second)
        assertEquals(listOf("load:https://example.org"), second.calls)
    }
}
