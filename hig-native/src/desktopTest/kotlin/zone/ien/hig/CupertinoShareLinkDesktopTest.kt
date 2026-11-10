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

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.AnnotatedString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** The desktop actual is the clipboard fallback. */
@OptIn(ExperimentalCupertinoApi::class, ExperimentalTestApi::class)
class CupertinoShareLinkDesktopTest {

    private class FakeClipboard : ClipboardManager {
        var copied: AnnotatedString? = null
        override fun getText(): AnnotatedString? = copied
        override fun setText(annotatedString: AnnotatedString) {
            copied = annotatedString
        }
    }

    @Test
    fun shareLinkCopiesTextAndUrlToTheClipboard() = runComposeUiTest {
        val clipboard = FakeClipboard()
        var result: CupertinoShareResult? = null
        setContent {
            CompositionLocalProvider(LocalClipboardManager provides clipboard) {
                CupertinoShareLink(
                    items = listOf(CupertinoShareItem.Text("Hello"), CupertinoShareItem.Url("https://ien.zone")),
                    onResult = { result = it },
                )
            }
        }

        onNodeWithText("Share").assertHasClickAction().performClick()
        waitForIdle()

        assertEquals("Hello\nhttps://ien.zone", clipboard.copied?.text)
        assertEquals(CupertinoShareResult.CopiedToClipboard, result)
    }

    @Test
    fun filesOnlyAreUnavailableAndLeaveTheClipboardUntouched() = runComposeUiTest {
        val clipboard = FakeClipboard()
        lateinit var state: CupertinoShareSheetState
        setContent {
            CompositionLocalProvider(LocalClipboardManager provides clipboard) {
                state = rememberCupertinoShareSheetState()
                CupertinoShareLink(
                    state = state,
                    items = listOf(CupertinoShareItem.File("/tmp/photo.png")),
                ) { CupertinoText("Send photo") }
            }
        }

        onNodeWithText("Send photo").performClick()
        waitForIdle()

        assertNull(clipboard.copied)
        assertEquals(CupertinoShareResult.Unavailable, state.lastResult)
    }

    @Test
    fun shareLinkWithoutItemsIsDisabled() = runComposeUiTest {
        setContent {
            CupertinoShareLink(items = emptyList())
        }
        onNodeWithText("Share").assertIsNotEnabled()
    }
}
