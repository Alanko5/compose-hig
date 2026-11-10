@file:OptIn(zone.ien.hig.ExperimentalCupertinoApi::class)

package cupertino

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import zone.ien.hig.CupertinoActionSheet
import zone.ien.hig.CupertinoActionSheetNative
import zone.ien.hig.CupertinoAlertDialog
import zone.ien.hig.CupertinoAlertDialogNative
import zone.ien.hig.CupertinoButton
import zone.ien.hig.CupertinoButtonDefaults
import zone.ien.hig.CupertinoContextMenuBox
import zone.ien.hig.CupertinoDropdownMenu
import zone.ien.hig.CupertinoDropdownMenuNative
import zone.ien.hig.CupertinoFullScreenCover
import zone.ien.hig.CupertinoFullScreenCoverDefaults
import zone.ien.hig.CupertinoFullScreenCoverProperties
import zone.ien.hig.CupertinoIcon
import zone.ien.hig.CupertinoLiquidAlertDialog
import zone.ien.hig.CupertinoMenuItemData
import zone.ien.hig.CupertinoMenuSectionData
import zone.ien.hig.CupertinoPopover
import zone.ien.hig.CupertinoPopoverEdge
import zone.ien.hig.CupertinoPopupButton
import zone.ien.hig.CupertinoShareItem
import zone.ien.hig.CupertinoShareLink
import zone.ien.hig.CupertinoShareResult
import zone.ien.hig.CupertinoShareSheetState
import zone.ien.hig.CupertinoSurface
import zone.ien.hig.CupertinoText
import zone.ien.hig.MenuAction
import zone.ien.hig.MenuDivider
import zone.ien.hig.MenuPickerAction
import zone.ien.hig.MenuSection
import zone.ien.hig.cancel
import zone.ien.hig.composeapp.generated.resources.Res
import zone.ien.hig.cupertinoShareSheetAnchor
import zone.ien.hig.default
import zone.ien.hig.destructive
import zone.ien.hig.icons.CupertinoIcons
import zone.ien.hig.icons.outlined.SquareAndArrowUp
import zone.ien.hig.icons.outlined.Star
import zone.ien.hig.icons.outlined.Trash
import zone.ien.hig.rememberCupertinoContextMenuState
import zone.ien.hig.rememberCupertinoShareSheetState
import zone.ien.hig.theme.CupertinoColors
import zone.ien.hig.theme.CupertinoTheme
import zone.ien.hig.theme.systemRed
import zone.ien.hig.utils.rememberDefaultBackdrop

@Composable
internal fun AlertDialogDemo() {
    var kind by rememberSaveable { mutableStateOf("Standard") }
    var visible by rememberSaveable { mutableStateOf(false) }
    var result by rememberSaveable { mutableStateOf("No action yet") }
    val finish: (String) -> Unit = { result = it; visible = false }
    if (visible) {
        when (kind) {
            "Native" -> CupertinoAlertDialogNative(
                onDismissRequest = { finish("Dismissed") },
                title = "Save changes?", message = "Your changes will be saved to this collection.",
            ) {
                cancel(onClick = { finish("Save cancelled") }, title = "Cancel")
                default(onClick = { finish("Changes saved") }, title = "Save")
            }
            "Liquid Glass" -> CupertinoLiquidAlertDialog(
                onDismissRequest = { finish("Dismissed") },
                title = { CupertinoText(text = "Save changes?") },
                message = { CupertinoText(text = "Your changes will be saved to this collection.") },
                backdrop = LocalDemoBackdrop.current ?: rememberDefaultBackdrop(),
            ) {
                cancel(onClick = { finish("Save cancelled") }) { CupertinoText(text = "Cancel") }
                default(onClick = { finish("Changes saved") }) { CupertinoText(text = "Save") }
            }
            else -> CupertinoAlertDialog(
                onDismissRequest = { finish("Dismissed") },
                title = { CupertinoText(text = "Save changes?") },
                message = { CupertinoText(text = "Your changes will be saved to this collection.") },
            ) {
                cancel(onClick = { finish("Save cancelled") }) { CupertinoText(text = "Cancel") }
                default(onClick = { finish("Changes saved") }) { CupertinoText(text = "Save") }
            }
        }
    }
    DemoSection("Preview") {
        CupertinoButton(onClick = { visible = true }) { CupertinoText(text = "Show alert") }
        DemoResult(result)
    }
    DemoSection("Properties") {
        DemoChoice("Implementation", listOf("Standard", "Liquid Glass", "Native"), kind, { kind = it }) { it }
        NativeImplementationNote(kind == "Native")
    }
}

@Composable
internal fun ActionSheetDemo() {
    var native by rememberSaveable { mutableStateOf(false) }
    var visible by rememberSaveable { mutableStateOf(false) }
    var result by rememberSaveable { mutableStateOf("No selection yet") }
    val finish: (String) -> Unit = { result = it; visible = false }
    if (native) {
        CupertinoActionSheetNative(visible = visible, onDismissRequest = { finish("Sheet dismissed") },
            title = "Manage photo", message = "Choose an action for this photo.") {
            default(onClick = { finish("Photo copied") }, title = "Copy")
            destructive(onClick = { finish("Photo deleted") }, title = "Delete")
            cancel(onClick = { finish("Cancelled") }, title = "Cancel")
        }
    } else {
        CupertinoActionSheet(visible = visible, onDismissRequest = { finish("Sheet dismissed") },
            title = { CupertinoText(text = "Manage photo") },
            message = { CupertinoText(text = "Choose an action for this photo.") }) {
            default(onClick = { finish("Photo copied") }) { CupertinoText(text = "Copy") }
            destructive(onClick = { finish("Photo deleted") }) { CupertinoText(text = "Delete") }
            cancel(onClick = { finish("Cancelled") }) { CupertinoText(text = "Cancel") }
        }
    }
    DemoSection("Preview") {
        CupertinoButton(onClick = { visible = true }) { CupertinoText(text = "Show action sheet") }
        DemoResult(result)
    }
    DemoSection("Properties", "Try a standard, destructive or cancel action.") {
        DemoToggle("Native implementation", native) { native = it }
        NativeImplementationNote(native)
    }
}

@Composable
internal fun MenuDemo() {
    var native by rememberSaveable { mutableStateOf(false) }
    var expanded by rememberSaveable { mutableStateOf(false) }
    var result by rememberSaveable { mutableStateOf("No selection yet") }
    val finish: (String) -> Unit = { result = it; expanded = false }
    DemoSection("Preview", "Choose an action. Disabled items ignore interaction.") {
        Box {
            CupertinoButton(onClick = { expanded = true }) { CupertinoText(text = "Show menu") }
            if (native) {
                CupertinoDropdownMenuNative(expanded = expanded, onDismissRequest = { expanded = false },
                    sections = listOf(
                        CupertinoMenuSectionData(title = "Manage photo", items = listOf(
                            CupertinoMenuItemData(title = "Share", onClick = { finish("Share selected") }),
                            CupertinoMenuItemData(title = "Unavailable", enabled = false, onClick = { finish("Disabled selected") }),
                            CupertinoMenuItemData(title = "Delete", isDestructive = true, onClick = { finish("Delete selected") }),
                        )),
                    ))
            } else {
                CupertinoDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    MenuSection(title = { CupertinoText(text = "Manage photo") }) {
                        MenuAction(onClick = { finish("Share selected") }) { CupertinoText(text = "Share") }
                        MenuAction(onClick = { finish("Disabled selected") }, enabled = false) { CupertinoText(text = "Unavailable") }
                    }
                    MenuDivider()
                    MenuAction(onClick = { finish("Delete selected") }, contentColor = CupertinoColors.systemRed) {
                        CupertinoText(text = "Delete")
                    }
                }
            }
        }
        DemoResult(result)
    }
    DemoSection("Properties") {
        DemoToggle("Native implementation", native) { native = it }
        NativeImplementationNote(native)
    }
}

@Composable
internal fun ContextMenuDemo() {
    var enabled by rememberSaveable { mutableStateOf(true) }
    var customPreview by rememberSaveable { mutableStateOf(false) }
    var favorite by rememberSaveable { mutableStateOf(false) }
    var result by rememberSaveable { mutableStateOf("No action yet") }
    val state = rememberCupertinoContextMenuState()
    DemoSection("Preview", "Long press the note, or right-click it with a mouse.") {
        CupertinoContextMenuBox(
            enabled = enabled,
            state = state,
            preview = if (customPreview) {
                { ContextMenuDemoNote(expanded = true) }
            } else {
                null
            },
            menuContent = {
                MenuAction(
                    onClick = { result = "Note shared" },
                    leadingIcon = { CupertinoIcon(CupertinoIcons.Default.SquareAndArrowUp, contentDescription = null) },
                ) { CupertinoText(text = "Share") }
                MenuAction(
                    onClick = { favorite = !favorite; result = if (favorite) "Added to favorites" else "Removed from favorites" },
                    leadingIcon = { CupertinoIcon(CupertinoIcons.Default.Star, contentDescription = null) },
                ) { CupertinoText(text = if (favorite) "Unfavorite" else "Favorite") }
                MenuAction(onClick = {}, enabled = false) { CupertinoText(text = "Move (unavailable)") }
                MenuDivider()
                MenuAction(
                    onClick = { result = "Note deleted" },
                    contentColor = CupertinoColors.systemRed,
                    leadingIcon = { CupertinoIcon(CupertinoIcons.Default.Trash, contentDescription = null) },
                ) { CupertinoText(text = "Delete") }
            },
        ) {
            ContextMenuDemoNote(expanded = false)
        }
        CupertinoButton(onClick = { state.show() }, enabled = enabled) { CupertinoText(text = "Show menu programmatically") }
        DemoResult(result)
    }
    DemoSection("Properties", "Screen readers can open the menu with the \"Show menu\" action.") {
        DemoToggle("Enabled", enabled) { enabled = it }
        DemoToggle("Custom preview", customPreview) { customPreview = it }
    }
}

@Composable
private fun ContextMenuDemoNote(expanded: Boolean) {
    CupertinoSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = CupertinoTheme.shapes.large,
        color = CupertinoTheme.colorScheme.secondarySystemGroupedBackground,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            CupertinoText(text = "Trip ideas", style = CupertinoTheme.typography.headline)
            CupertinoText(
                text = if (expanded) {
                    "Lisbon in spring · Tram 28 at sunrise · Pastéis de Belém · Sintra day trip · Fado night in Alfama"
                } else {
                    "Lisbon in spring · Tram 28 at sunrise…"
                },
                style = CupertinoTheme.typography.subhead,
                color = CupertinoTheme.colorScheme.secondaryLabel,
            )
        }
    }
}

@Composable
internal fun PopupButtonDemo() {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var enabled by rememberSaveable { mutableStateOf(true) }
    var sort by rememberSaveable { mutableStateOf("Name") }
    val original = listOf("Landscape", "Family", "Travel")
    val items = if (sort == "Name") original.sorted() else original.reversed()
    DemoSection("Preview", "Choose how to sort the collection.") {
        CupertinoPopupButton(expanded = expanded, onExpandedChange = { expanded = it }, enabled = enabled,
            title = { CupertinoText(text = sort) }) {
            listOf("Name", "Recent").forEach { option ->
                MenuPickerAction(isSelected = sort == option, onClick = { sort = option; expanded = false },
                    title = { CupertinoText(text = option) })
            }
        }
        items.forEach { CupertinoText(text = it) }
        DemoResult("Sort: $sort · expanded = $expanded")
    }
    DemoSection("Properties") { DemoToggle("Enabled", enabled) { enabled = it } }
}

@Composable
internal fun PopoverDemo() {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var edge by rememberSaveable { mutableStateOf(CupertinoPopoverEdge.Top) }
    var anchor by rememberSaveable { mutableStateOf("Center") }
    var width by rememberSaveable { mutableStateOf(240) }
    var result by rememberSaveable { mutableStateOf("Open the popover to inspect its position") }
    DemoSection("Preview", "The popover adjusts to the space around its anchor.") {
        Box(Modifier.fillMaxWidth(), contentAlignment = when (anchor) {
            "Start" -> Alignment.CenterStart
            "End" -> Alignment.CenterEnd
            else -> Alignment.Center
        }) {
            Box {
                CupertinoButton(onClick = { expanded = true; result = "Popover opened" }) { CupertinoText(text = "Show popover") }
                CupertinoPopover(expanded = expanded, onDismissRequest = { expanded = false; result = "Dismissed outside" },
                    edge = edge, width = width.dp, backdrop = LocalDemoBackdrop.current ?: rememberDefaultBackdrop(), contentPadding = PaddingValues(20.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        CupertinoText(text = "More information")
                        CupertinoText(text = "The arrow points to the anchor.")
                        CupertinoButton(onClick = { expanded = false; result = "Confirmed" }) { CupertinoText(text = "OK") }
                    }
                }
            }
        }
        DemoResult(result)
    }
    DemoSection("Properties") {
        DemoChoice("Edge", CupertinoPopoverEdge.entries, edge, { edge = it }) { it.name }
        DemoChoice("Anchor", listOf("Start", "Center", "End"), anchor, { anchor = it }) { it }
        DemoChoice("Width", listOf(160, 240, 320), width, { width = it }) { "${it}dp" }
    }
}

@Composable
internal fun ShareSheetDemo() {
    var result by rememberSaveable { mutableStateOf("Share something to see the result") }
    val onResult: (CupertinoShareResult) -> Unit = { result = "Result: ${it.name}" }
    val writeFile = rememberDemoShareFileWriter()
    DemoSection("Preview", "iOS and Android open the system share sheet. Desktop and web copy text and links to the clipboard.") {
        CupertinoShareLink(
            items = listOf(CupertinoShareItem.Url("https://github.com/ienground/compose-hig")),
            subject = "compose-hig",
            onResult = onResult,
        )
        DemoResult(result)
    }
    DemoSection("Content", "Each button anchors its own sheet, so the iPad popover points at the button you tapped.") {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ShareDemoButton("Text", onResult) {
                share(CupertinoShareItem.Text("Hello from compose-hig!"))
            }
            ShareDemoButton("Text + link", onResult) {
                share(
                    items = listOf(
                        CupertinoShareItem.Text("Apple-style components for Compose Multiplatform"),
                        CupertinoShareItem.Url("https://github.com/ienground/compose-hig"),
                    ),
                    subject = "Have a look at compose-hig",
                )
            }
            ShareDemoButton("Image", onResult) {
                val uri = writeFile("compose-hig-landscape.jpg", Res.readBytes("drawable/catalog_landscape.jpg"))
                if (uri == null) {
                    result = "Files cannot be shared on this platform"
                } else {
                    share(listOf(CupertinoShareItem.File(uri, mimeType = "image/jpeg"), CupertinoShareItem.Text("Made with compose-hig")))
                }
            }
        }
    }
    DemoSection("Disabled", "A share link without items is disabled automatically.") {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CupertinoShareLink(items = listOf(CupertinoShareItem.Text("Unused")), enabled = false)
            CupertinoShareLink(items = emptyList()) { CupertinoText(text = "Nothing to share") }
        }
    }
}

@Composable
private fun ShareDemoButton(
    title: String,
    onResult: (CupertinoShareResult) -> Unit,
    share: suspend CupertinoShareSheetState.() -> Unit,
) {
    val state = rememberCupertinoShareSheetState(onResult)
    val scope = rememberCoroutineScope()
    CupertinoButton(
        onClick = { scope.launch { state.share() } },
        modifier = Modifier.cupertinoShareSheetAnchor(state),
        colors = CupertinoButtonDefaults.grayButtonColors(),
    ) { CupertinoText(text = title) }
}

private enum class CoverDemoScene(val title: String, val subtitle: String) {
    Camera("Camera", "A full-screen capture surface."),
    Scanner("Document Scanner", "Hold the page inside the frame."),
}

@Composable
internal fun FullScreenCoverDemo() {
    var onboarding by rememberSaveable { mutableStateOf(false) }
    var scene by rememberSaveable { mutableStateOf<CoverDemoScene?>(null) }
    var dismissOnBack by rememberSaveable { mutableStateOf(true) }
    var interactiveDismiss by rememberSaveable { mutableStateOf(false) }
    var result by rememberSaveable { mutableStateOf("No cover presented yet") }
    val properties = CupertinoFullScreenCoverProperties(
        dismissOnBackPress = dismissOnBack,
        interactiveDismissEnabled = interactiveDismiss,
    )

    CupertinoFullScreenCover(
        isPresented = onboarding,
        onDismissRequest = { onboarding = false; result = "Dismissed with back / swipe" },
        properties = properties,
    ) {
        var page by rememberSaveable { mutableStateOf(0) }
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                CupertinoButton(onClick = { onboarding = false; result = "Closed with button" },
                    colors = CupertinoButtonDefaults.plainButtonColors()) { CupertinoText(text = "Close") }
            }
            Spacer(Modifier.weight(1f))
            CupertinoText(text = "Welcome", style = CupertinoTheme.typography.largeTitle)
            CupertinoText(text = "Step ${page + 1} of 3. This page keeps its state while the cover is presented.",
                color = CupertinoTheme.colorScheme.secondaryLabel)
            Spacer(Modifier.weight(1f))
            CupertinoButton(
                onClick = { if (page < 2) page++ else { onboarding = false; result = "Onboarding finished" } },
                modifier = Modifier.fillMaxWidth(),
            ) { CupertinoText(text = if (page < 2) "Continue" else "Get Started") }
        }
    }

    CupertinoFullScreenCover(
        item = scene,
        onDismissRequest = { scene = null; result = "Dismissed with back / swipe" },
        properties = properties,
        colors = CupertinoFullScreenCoverDefaults.colors(containerColor = Color.Black, contentColor = Color.White),
    ) { current ->
        Box(Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp)) {
            CupertinoButton(onClick = { scene = null; result = "Closed ${current.title}" },
                modifier = Modifier.align(Alignment.TopStart),
                colors = CupertinoButtonDefaults.plainButtonColors(contentColor = Color.White)) { CupertinoText(text = "Done") }
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CupertinoText(text = current.title, style = CupertinoTheme.typography.title1)
                CupertinoText(text = current.subtitle)
            }
            val other = CoverDemoScene.entries.first { it != current }
            CupertinoButton(onClick = { scene = other },
                modifier = Modifier.align(Alignment.BottomCenter)) { CupertinoText(text = "Switch to ${other.title}") }
        }
    }

    DemoSection("Preview", "Covers the whole screen; close it from its content.") {
        CupertinoButton(onClick = { onboarding = true }) { CupertinoText(text = "Show onboarding") }
        CupertinoButton(onClick = { scene = CoverDemoScene.Camera }) { CupertinoText(text = "Open camera (item)") }
        DemoResult(result)
    }
    DemoSection("Properties", "Back press calls onDismissRequest on Android (Escape on desktop).") {
        DemoToggle("Dismiss on back press", dismissOnBack) { dismissOnBack = it }
        DemoToggle("Interactive dismiss", interactiveDismiss) { interactiveDismiss = it }
    }
}
