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

package cupertino

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Matches the `<provider>` declared in example/androidApp/src/main/AndroidManifest.xml. */
private const val ShareAuthoritySuffix = ".cupertino.share"

@Composable
internal actual fun rememberDemoShareFileWriter(): DemoShareFileWriter {
    val context = LocalContext.current.applicationContext
    return remember(context) {
        { name, bytes ->
            withContext(Dispatchers.IO) {
                val file = File(File(context.cacheDir, "shared").apply { mkdirs() }, name)
                file.writeBytes(bytes)
                FileProvider.getUriForFile(context, context.packageName + ShareAuthoritySuffix, file).toString()
            }
        }
    }
}
