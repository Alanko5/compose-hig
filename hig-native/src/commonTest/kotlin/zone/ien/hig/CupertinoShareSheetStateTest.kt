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
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.assertNotNull
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TestTimeSource

@OptIn(ExperimentalCupertinoApi::class, ExperimentalTestApi::class)
class CupertinoShareSheetStateTest {

    private class FakePresenter : CupertinoSharePresenter {
        val requests = mutableListOf<Pair<CupertinoShareRequest, Rect?>>()
        var pending: ((CupertinoShareResult) -> Unit)? = null

        override fun present(
            request: CupertinoShareRequest,
            anchorBounds: Rect?,
            onResult: (CupertinoShareResult) -> Unit,
        ) {
            requests += request to anchorBounds
            pending = onResult
        }
    }

    @Test
    fun shareForwardsRequestAndAnchorAndReportsResult() {
        val presenter = FakePresenter()
        val results = mutableListOf<CupertinoShareResult>()
        val state = CupertinoShareSheetState(presenter).apply { onResult = { results += it } }
        state.anchorBounds = Rect(1f, 2f, 3f, 4f)

        state.share(listOf(CupertinoShareItem.Text("Hi")), subject = "S")

        assertTrue(state.isPresented)
        assertEquals(CupertinoShareRequest(listOf(CupertinoShareItem.Text("Hi")), "S"), presenter.requests.single().first)
        assertEquals(Rect(1f, 2f, 3f, 4f), presenter.requests.single().second)

        presenter.pending!!(CupertinoShareResult.Shared)

        assertFalse(state.isPresented)
        assertEquals(CupertinoShareResult.Shared, state.lastResult)
        assertEquals(listOf(CupertinoShareResult.Shared), results)
    }

    @Test
    fun shareIsIgnoredWhileASheetIsPresented() {
        val presenter = FakePresenter()
        val state = CupertinoShareSheetState(presenter)

        state.share(listOf(CupertinoShareItem.Text("a")))
        state.share(listOf(CupertinoShareItem.Text("b")))

        assertEquals(1, presenter.requests.size)
    }

    @Test
    fun emptyItemsAreReportedAsUnavailableWithoutPresenting() {
        val presenter = FakePresenter()
        val results = mutableListOf<CupertinoShareResult>()
        val state = CupertinoShareSheetState(presenter).apply { onResult = { results += it } }

        state.share(emptyList())

        assertTrue(presenter.requests.isEmpty())
        assertFalse(state.isPresented)
        assertEquals(listOf(CupertinoShareResult.Unavailable), results)
    }

    @Test
    fun aSecondResultFromThePlatformIsIgnored() {
        val presenter = FakePresenter()
        val results = mutableListOf<CupertinoShareResult>()
        val state = CupertinoShareSheetState(presenter).apply { onResult = { results += it } }

        state.share(listOf(CupertinoShareItem.Url("https://ien.zone")))
        val callback = presenter.pending!!
        callback(CupertinoShareResult.Dismissed)
        callback(CupertinoShareResult.Shared)

        assertEquals(listOf(CupertinoShareResult.Dismissed), results)
    }

    @Test
    fun abandonedRequestAllowsANewShareAndDropsItsLateResult() {
        val presenter = FakePresenter()
        val results = mutableListOf<CupertinoShareResult>()
        val time = TestTimeSource()
        val state = CupertinoShareSheetState(presenter, time).apply { onResult = { results += it } }

        state.share(listOf(CupertinoShareItem.Text("a")))
        val stale = presenter.pending!!
        state.abandonPendingRequest()
        assertFalse(state.isPresented)

        time += 1000.milliseconds
        state.share(listOf(CupertinoShareItem.Text("b")))
        assertEquals(2, presenter.requests.size)
        assertTrue(state.isPresented)

        stale(CupertinoShareResult.Shared)
        assertTrue(state.isPresented)
        assertTrue(results.isEmpty())

        presenter.pending!!(CupertinoShareResult.Dismissed)
        assertEquals(listOf(CupertinoShareResult.Dismissed), results)
    }

    @Test
    fun aRepeatedShareRightAfterASynchronousResultIsIgnored() {
        val requests = mutableListOf<CupertinoShareRequest>()
        val presenter = CupertinoSharePresenter { request, _, onResult ->
            requests += request
            onResult(CupertinoShareResult.Presented)
        }
        val time = TestTimeSource()
        val state = CupertinoShareSheetState(presenter, time)

        state.share(listOf(CupertinoShareItem.Text("a")))
        time += 100.milliseconds
        state.share(listOf(CupertinoShareItem.Text("a")))
        assertEquals(1, requests.size)

        time += CupertinoShareSheetState.RepeatSuppression
        state.share(listOf(CupertinoShareItem.Text("a")))
        assertEquals(2, requests.size)
        assertEquals(CupertinoShareResult.Presented, state.lastResult)
    }

    @Test
    fun lastResultStartsEmpty() {
        assertNull(CupertinoShareSheetState(FakePresenter()).lastResult)
    }

    @Test
    fun shareLinkSharesItsItemsAnchoredToItself() = runComposeUiTest {
        val presenter = FakePresenter()
        val state = CupertinoShareSheetState(presenter)
        setContent {
            CupertinoShareLink(
                state = state,
                items = listOf(CupertinoShareItem.Url("https://ien.zone")),
                subject = "compose-hig",
            )
        }

        onNodeWithText("Share").performClick()
        waitForIdle()

        val (request, anchor) = presenter.requests.single()
        assertEquals(CupertinoShareRequest(listOf(CupertinoShareItem.Url("https://ien.zone")), "compose-hig"), request)
        assertNotNull(anchor)
        assertTrue(anchor.width > 0f && anchor.height > 0f)
        assertTrue(state.isPresented)
    }
}
