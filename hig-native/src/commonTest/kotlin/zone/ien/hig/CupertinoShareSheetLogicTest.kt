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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCupertinoApi::class)
class CupertinoShareSheetLogicTest {

    @Test
    fun emptyItemsProduceNoIntentPayload() {
        assertNull(shareIntentPayload(emptyList(), subject = "Hi"))
    }

    @Test
    fun textAndUrlAreJoinedIntoASingleSendWithPlainTextMime() {
        val payload = shareIntentPayload(
            listOf(CupertinoShareItem.Text("Look at this"), CupertinoShareItem.Url("https://ien.zone")),
            subject = "Subject",
        )!!
        assertEquals(ShareIntentAction.Send, payload.action)
        assertEquals("text/plain", payload.mimeType)
        assertEquals("Look at this\nhttps://ien.zone", payload.text)
        assertEquals("Subject", payload.subject)
        assertTrue(payload.streamUris.isEmpty())
    }

    @Test
    fun singleFileUsesSendWithItsMimeTypeAndKeepsText() {
        val payload = shareIntentPayload(
            listOf(
                CupertinoShareItem.File("content://app.provider/photo.png"),
                CupertinoShareItem.Text("Caption"),
            ),
            subject = null,
        )!!
        assertEquals(ShareIntentAction.Send, payload.action)
        assertEquals("image/png", payload.mimeType)
        assertEquals("Caption", payload.text)
        assertNull(payload.subject)
        assertEquals(listOf("content://app.provider/photo.png"), payload.streamUris)
    }

    @Test
    fun multipleFilesUseSendMultiple() {
        val payload = shareIntentPayload(
            listOf(
                CupertinoShareItem.File("content://p/a.jpg"),
                CupertinoShareItem.File("content://p/b.png"),
            ),
            subject = null,
        )!!
        assertEquals(ShareIntentAction.SendMultiple, payload.action)
        assertEquals("image/*", payload.mimeType)
        assertNull(payload.text)
        assertEquals(2, payload.streamUris.size)
    }

    @Test
    fun blankSubjectIsDropped() {
        val payload = shareIntentPayload(listOf(CupertinoShareItem.Text("a")), subject = "  ")!!
        assertNull(payload.subject)
    }

    @Test
    fun mimeTypeSelectionForMixedItems() {
        assertEquals("text/plain", shareMimeType(listOf(CupertinoShareItem.Text("a"))))
        assertEquals("text/plain", shareMimeType(listOf(CupertinoShareItem.Url("https://ien.zone"))))
        assertEquals(
            "image/jpeg",
            shareMimeType(listOf(CupertinoShareItem.File("content://p/a.jpg"), CupertinoShareItem.File("content://p/b.JPEG"))),
        )
        assertEquals(
            "image/*",
            shareMimeType(listOf(CupertinoShareItem.File("content://p/a.jpg"), CupertinoShareItem.File("content://p/b.gif"))),
        )
        assertEquals(
            "*/*",
            shareMimeType(listOf(CupertinoShareItem.File("content://p/a.jpg"), CupertinoShareItem.File("content://p/b.pdf"))),
        )
        assertEquals(
            "video/mp4",
            shareMimeType(listOf(CupertinoShareItem.File("content://p/clip", mimeType = "video/mp4"))),
        )
        assertEquals("*/*", shareMimeType(listOf(CupertinoShareItem.File("content://p/blob"))))
    }

    @Test
    fun fileMimeTypeIsInferredFromExtensionIgnoringQueryAndFragment() {
        assertEquals("application/pdf", resolvedMimeType(CupertinoShareItem.File("/tmp/report.pdf?x=1#p")))
        assertEquals("text/plain", resolvedMimeType(CupertinoShareItem.File("file:///tmp/notes.txt")))
        assertEquals("image/heic", resolvedMimeType(CupertinoShareItem.File("/a/b.HEIC")))
        assertNull(resolvedMimeType(CupertinoShareItem.File("content://p/no-extension")))
        assertNull(resolvedMimeType(CupertinoShareItem.File("content://p.with.dots/no-extension")))
        assertEquals("custom/type", resolvedMimeType(CupertinoShareItem.File("/a/b.pdf", mimeType = "custom/type")))
    }

    @Test
    fun onlyContentUrisAreShareableOnAndroid() {
        assertTrue(isAndroidShareableUri("content://zone.ien.share/cache/a.png"))
        assertTrue(isAndroidShareableUri("CONTENT://zone.ien.share/cache/a.png"))
        assertFalse(isAndroidShareableUri("file:///sdcard/a.png"))
        assertFalse(isAndroidShareableUri("/data/user/0/app/cache/a.png"))
        assertFalse(isAndroidShareableUri("https://ien.zone/a.png"))
    }

    @Test
    fun clipboardFallbackCopiesTextAndUrlsButNotFiles() {
        val items = listOf(
            CupertinoShareItem.Text("Hello"),
            CupertinoShareItem.File("/tmp/a.png"),
            CupertinoShareItem.Url("https://ien.zone"),
        )
        assertEquals("Hello\nhttps://ien.zone", clipboardShareText(items))
        assertNull(clipboardShareText(listOf(CupertinoShareItem.File("/tmp/a.png"))))
        assertNull(clipboardShareText(emptyList()))
    }

    @Test
    fun iosCompletionMapping() {
        assertEquals(
            CupertinoShareResult.Shared,
            iosShareResult(completed = true, isCopyToPasteboard = false, hasError = false),
        )
        assertEquals(
            CupertinoShareResult.CopiedToClipboard,
            iosShareResult(completed = true, isCopyToPasteboard = true, hasError = false),
        )
        assertEquals(
            CupertinoShareResult.Dismissed,
            iosShareResult(completed = false, isCopyToPasteboard = false, hasError = false),
        )
        assertEquals(
            CupertinoShareResult.Unavailable,
            iosShareResult(completed = false, isCopyToPasteboard = false, hasError = true),
        )
    }

    @Test
    fun popoverSourceRectConvertsPixelsToPointsAndFallsBackToCenter() {
        assertEquals(
            ShareSourceRect(x = 10.0, y = 20.0, width = 30.0, height = 40.0),
            shareSourceRect(
                anchor = androidx.compose.ui.geometry.Rect(left = 30f, top = 60f, right = 120f, bottom = 180f),
                density = 3f,
                containerWidth = 400.0,
                containerHeight = 800.0,
            ),
        )
        assertEquals(
            ShareSourceRect(x = 200.0, y = 400.0, width = 0.0, height = 0.0),
            shareSourceRect(anchor = null, density = 3f, containerWidth = 400.0, containerHeight = 800.0),
        )
    }
}
