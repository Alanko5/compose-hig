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
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.uikit.LocalUIViewController
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSURL
import platform.UIKit.UIActivityItemSourceProtocol
import platform.UIKit.UIActivityTypeCopyToPasteboard
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIViewController
import platform.darwin.NSObject
import platform.UIKit.popoverPresentationController
import zone.ien.hig.theme.CupertinoTheme

@Composable
internal actual fun rememberPlatformSharePresenter(): CupertinoSharePresenter {
    val controller = LocalUIViewController.current
    val density = LocalDensity.current.density
    val dark = CupertinoTheme.colorScheme.isDark
    return remember(controller, density, dark) { IosSharePresenter(controller, density, dark) }
}

/**
 * Presents a `UIActivityViewController` from the top-most presented view controller. On iPad the controller is a
 * popover anchored to the [cupertinoShareSheetAnchor] bounds (or the center of the Compose view).
 */
@OptIn(ExperimentalCupertinoApi::class, ExperimentalForeignApi::class)
internal class IosSharePresenter(
    private val hostController: UIViewController,
    private val density: Float,
    private val dark: Boolean,
) : CupertinoSharePresenter {
    override fun present(
        request: CupertinoShareRequest,
        anchorBounds: Rect?,
        onResult: (CupertinoShareResult) -> Unit,
    ) {
        val items = request.items.mapNotNull { it.toActivityItem() }
        // UIKit refuses (and only logs) a presentation from a controller that is not in a window or is busy
        // dismissing; the completion handler would then never fire, so report it right away.
        val presenter = hostController.topPresentedController()
        if (items.isEmpty() || presenter == null || presenter.viewIfLoaded?.window == null) {
            onResult(CupertinoShareResult.Unavailable)
            return
        }
        val subject = request.subject?.takeIf { it.isNotBlank() }
        // The subject (read by Mail and similar targets) is supplied through the documented UIActivityItemSource
        // protocol, which wraps the first item.
        val activityItems = if (subject == null) items else listOf(SubjectItemSource(items.first(), subject)) + items.drop(1)
        val activityController = UIActivityViewController(activityItems = activityItems, applicationActivities = null)
        activityController.applyTheme(dark)
        activityController.completionWithItemsHandler = { activityType, completed, _, error ->
            onResult(
                iosShareResult(
                    completed = completed,
                    isCopyToPasteboard = activityType != null && activityType == UIActivityTypeCopyToPasteboard,
                    hasError = error != null,
                ),
            )
        }
        activityController.popoverPresentationController?.let { popover ->
            val sourceView = hostController.view
            val rect = sourceView.bounds.useContents {
                shareSourceRect(anchorBounds, density, size.width, size.height)
            }
            popover.sourceView = sourceView
            popover.sourceRect = CGRectMake(rect.x, rect.y, rect.width, rect.height)
        }
        presenter.presentViewController(activityController, animated = true) {
            // Presentation was refused after all: no completionWithItemsHandler will follow.
            if (activityController.presentingViewController == null) onResult(CupertinoShareResult.Unavailable)
        }
    }
}

/**
 * The top-most presented view controller, or `null` when a controller in the chain is being dismissed (neither it
 * nor its presenter can present until the dismissal finishes).
 */
private fun UIViewController.topPresentedController(): UIViewController? {
    var top = this
    while (true) {
        val next = top.presentedViewController ?: return top
        if (next.isBeingDismissed()) return null
        top = next
    }
}

/** Shares [item] and provides [subject] through the documented `UIActivityItemSource` API. */
private class SubjectItemSource(
    private val item: Any,
    private val subject: String,
) : NSObject(), UIActivityItemSourceProtocol {
    override fun activityViewControllerPlaceholderItem(activityViewController: UIActivityViewController): Any = item

    @ObjCSignatureOverride
    override fun activityViewController(
        activityViewController: UIActivityViewController,
        itemForActivityType: String?,
    ): Any? = item

    @ObjCSignatureOverride
    override fun activityViewController(
        activityViewController: UIActivityViewController,
        subjectForActivityType: String?,
    ): String = subject
}

@OptIn(ExperimentalCupertinoApi::class)
private fun CupertinoShareItem.toActivityItem(): Any? = when (this) {
    is CupertinoShareItem.Text -> text
    is CupertinoShareItem.Url -> NSURL.URLWithString(url) ?: url
    is CupertinoShareItem.File -> when {
        uri.startsWith("/") -> NSURL.fileURLWithPath(uri)
        else -> NSURL.URLWithString(uri)
    }
}
