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
import kotlin.test.assertNull

@OptIn(ExperimentalCupertinoApi::class)
class CupertinoWebViewUrlTest {

    private fun normalize(input: String) = CupertinoWebViewDefaults.normalizeUrl(input)

    @Test
    fun blankInputHasNoUrl() {
        assertNull(normalize(""))
        assertNull(normalize("   \n"))
    }

    @Test
    fun absoluteUrlsAreKeptAndTrimmed() {
        assertEquals("https://example.com/a?b=c", normalize("  https://example.com/a?b=c "))
        assertEquals("http://example.com", normalize("http://example.com"))
        assertEquals("HTTPS://Example.com", normalize("HTTPS://Example.com"))
        assertEquals("about:blank", normalize("about:blank"))
        assertEquals("file:///android_asset/index.html", normalize("file:///android_asset/index.html"))
    }

    @Test
    fun bareDomainsGetHttps() {
        assertEquals("https://example.com", normalize("example.com"))
        assertEquals("https://www.apple.com/iphone", normalize("www.apple.com/iphone"))
        assertEquals("https://sub.example.co.uk:8443/x", normalize("sub.example.co.uk:8443/x"))
    }

    @Test
    fun localHostsAndIpAddressesGetHttp() {
        assertEquals("http://localhost:8080", normalize("localhost:8080"))
        assertEquals("http://localhost", normalize("localhost"))
        assertEquals("http://192.168.0.1/admin", normalize("192.168.0.1/admin"))
        assertEquals("http://10.0.2.2:3000", normalize("10.0.2.2:3000"))
    }

    @Test
    fun anythingElseBecomesAnEncodedSearch() {
        assertEquals("https://duckduckgo.com/?q=compose+multiplatform", normalize("compose multiplatform"))
        assertEquals("https://duckduckgo.com/?q=kotlin", normalize("kotlin"))
        assertEquals("https://duckduckgo.com/?q=a%26b%3Dc%2Fd", normalize("a&b=c/d"))
        assertEquals("https://duckduckgo.com/?q=%C4%8Daj", normalize("čaj"))
        assertEquals("https://duckduckgo.com/?q=example.com+is+down", normalize("example.com is down"))
    }

    @Test
    fun aDotWithoutAValidTopLevelDomainIsASearch() {
        assertEquals("https://duckduckgo.com/?q=v1.2", normalize("v1.2"))
        assertEquals("https://duckduckgo.com/?q=file.", normalize("file."))
    }

    @Test
    fun customSearchTemplateIsUsed() {
        assertEquals(
            "https://www.google.com/search?q=hello+world",
            CupertinoWebViewDefaults.normalizeUrl("hello world", searchUrl = "https://www.google.com/search?q="),
        )
        assertNull(CupertinoWebViewDefaults.normalizeUrl("hello world", searchUrl = null))
    }
}
