@file:OptIn(zone.ien.hig.ExperimentalCupertinoApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)

package cupertino

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import zone.ien.hig.CupertinoBottomAppBar
import zone.ien.hig.CupertinoBottomAppBarAction
import zone.ien.hig.CupertinoButton
import zone.ien.hig.CupertinoButtonDefaults
import zone.ien.hig.CupertinoDisclosureGroup
import zone.ien.hig.CupertinoHorizontalDivider
import zone.ien.hig.CupertinoIcon
import zone.ien.hig.CupertinoIconButton
import zone.ien.hig.CupertinoLabel
import zone.ien.hig.CupertinoLabelStyle
import zone.ien.hig.CupertinoLiquidIconButton
import zone.ien.hig.CupertinoNavigateBackLiquidButton
import zone.ien.hig.CupertinoNavigationBar
import zone.ien.hig.CupertinoNavigationBarItem
import zone.ien.hig.CupertinoNavigationBarItemData
import zone.ien.hig.CupertinoNavigationBarNative
import zone.ien.hig.CupertinoNavigationTitle
import zone.ien.hig.CupertinoPageIndexBackgroundDisplayMode
import zone.ien.hig.CupertinoPageIndexDisplayMode
import zone.ien.hig.CupertinoPageTabView
import zone.ien.hig.CupertinoPageTabViewDefaults
import zone.ien.hig.CupertinoProgressIndicator
import zone.ien.hig.CupertinoScaffold
import zone.ien.hig.CupertinoSearchTextField
import zone.ien.hig.CupertinoSheetState
import zone.ien.hig.CupertinoSurface
import zone.ien.hig.CupertinoSwipeBox
import zone.ien.hig.CupertinoText
import zone.ien.hig.CupertinoTopAppBar
import zone.ien.hig.CupertinoVerticalDivider
import zone.ien.hig.CupertinoWebView
import zone.ien.hig.CupertinoWebViewDefaults
import zone.ien.hig.ExperimentalCupertinoApi
import zone.ien.hig.LocalCupertinoRightSystemArea
import zone.ien.hig.ProvideCupertinoLabelStyle
import zone.ien.hig.adaptive.CupertinoSidebarScaffold
import zone.ien.hig.adaptive.CupertinoSidebarScope
import zone.ien.hig.adaptive.CupertinoSidebarState
import zone.ien.hig.adaptive.CupertinoSidebarToggleButton
import zone.ien.hig.adaptive.CupertinoSidebarVisibility
import zone.ien.hig.adaptive.CupertinoSidebarWidthClass
import zone.ien.hig.adaptive.rememberCupertinoSidebarState
import zone.ien.hig.composeapp.generated.resources.Res
import zone.ien.hig.composeapp.generated.resources.catalog_landscape
import zone.ien.hig.icons.CupertinoIcons
import zone.ien.hig.icons.filled.Bell
import zone.ien.hig.icons.filled.Gearshape
import zone.ien.hig.icons.filled.Lock
import zone.ien.hig.icons.filled.Star
import zone.ien.hig.icons.filled.Trash
import zone.ien.hig.icons.outlined.Archivebox
import zone.ien.hig.icons.outlined.ArrowClockwise
import zone.ien.hig.icons.outlined.ChevronBackward
import zone.ien.hig.icons.outlined.ChevronForward
import zone.ien.hig.icons.outlined.Envelope
import zone.ien.hig.icons.outlined.Flag
import zone.ien.hig.icons.outlined.Heart
import zone.ien.hig.icons.outlined.Paperplane
import zone.ien.hig.icons.outlined.Pencil
import zone.ien.hig.icons.outlined.RectangleStack
import zone.ien.hig.icons.outlined.SquareAndArrowUp
import zone.ien.hig.icons.outlined.Star
import zone.ien.hig.icons.outlined.Trash
import zone.ien.hig.icons.outlined.TrayAndArrowDown
import zone.ien.hig.icons.outlined.Xmark
import zone.ien.hig.rememberCupertinoWebViewState
import zone.ien.hig.section.CupertinoSection
import zone.ien.hig.section.LazySectionScope
import zone.ien.hig.section.SectionDisclosureGroup
import zone.ien.hig.section.SectionItem
import zone.ien.hig.section.SectionStyle
import zone.ien.hig.section.disclosureGroup
import zone.ien.hig.section.section
import zone.ien.hig.swipebox.CupertinoSwipeBoxItem
import zone.ien.hig.swipebox.rememberCupertinoSwipeBoxState
import zone.ien.hig.theme.CupertinoColors
import zone.ien.hig.theme.CupertinoTheme
import zone.ien.hig.theme.systemBlue
import zone.ien.hig.theme.systemIndigo
import zone.ien.hig.theme.systemOrange
import zone.ien.hig.theme.systemRed
import zone.ien.hig.theme.systemTeal
import zone.ien.hig.utils.rememberDefaultBackdrop

@Composable
internal fun BottomSheetDemo(state: CupertinoSheetState, onShow: () -> Unit) {
    DemoSection("Preview", "Drag the handle to resize the sheet or swipe down to dismiss.") {
        CupertinoButton(onClick = onShow) { CupertinoText(text = "Show bottom sheet") }
        DemoResult("State: ${state.currentValue} · Target: ${state.targetValue}")
    }
    DemoSection("Behavior") {
        CupertinoText(text = "PresentationDetent.Medium / Large")
        CupertinoText(text = "The sheet contains 100 scrollable items.")
        CupertinoText(text = "Tap Done, tap outside or swipe down to dismiss.")
    }
}

@Composable
internal fun SwipeBoxDemo(scrollState: ScrollableState) {
    var fullSwipe by rememberSaveable { mutableStateOf(true) }
    var twoWay by rememberSaveable { mutableStateOf(true) }
    var deleted by rememberSaveable { mutableStateOf(false) }
    var favorite by rememberSaveable { mutableStateOf(false) }
    var result by rememberSaveable { mutableStateOf("Swipe the row to reveal actions") }
    var revision by rememberSaveable { mutableIntStateOf(0) }
    DemoSection("Preview", "Swipe left to delete or right to favorite.") {
        if (!deleted) key(revision, twoWay, fullSwipe) {
            val state = rememberCupertinoSwipeBoxState(scrollableState = scrollState)
            val toggleFavorite = { favorite = !favorite; result = if (favorite) "Added to favorites" else "Removed from favorites" }
            val delete = { deleted = true; result = "Item deleted" }
            CupertinoSwipeBox(
                state = state, modifier = Modifier.fillMaxWidth(), height = 72.dp,
                startToEndFullSwipeEnabled = fullSwipe, endToStartFullSwipeEnabled = fullSwipe,
                actionItemBuilder = {
                    if (twoWay) start(onClick = toggleFavorite) {
                        CupertinoSwipeBoxItem(color = CupertinoColors.systemBlue, onClick = toggleFavorite,
                            icon = CupertinoIcons.Default.Heart, onClickLabel = "Toggle favorite")
                    }
                    end(onClick = delete) {
                        CupertinoSwipeBoxItem(color = CupertinoColors.systemRed, onClick = delete,
                            icon = CupertinoIcons.Filled.Trash, onClickLabel = "Item deleted")
                    }
                },
            ) {
                Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center) {
                    CupertinoText(text = if (favorite) "Favorite photo" else "Travel photo")
                }
            }
        }
        DemoResult(result)
        CupertinoButton(onClick = { deleted = false; favorite = false; revision++; result = "Item restored" }) {
            CupertinoText(text = "Restore item")
        }
    }
    DemoSection("Properties") {
        DemoToggle("Full swipe action", fullSwipe) { fullSwipe = it }
        DemoToggle("Actions on both sides", twoWay) { twoWay = it }
    }
}

@Composable
internal fun NavigationBarDemo() {
    var selected by rememberSaveable { mutableIntStateOf(0) }
    var native by rememberSaveable { mutableStateOf(false) }
    val items = listOf("Photos" to CupertinoIcons.Default.RectangleStack,
        "Favorites" to CupertinoIcons.Default.Heart, "Settings" to CupertinoIcons.Filled.Gearshape)
    DemoSection("Preview") {
        CupertinoText(text = "${items[selected].first}", style = CupertinoTheme.typography.title2)
        Box(Modifier.fillMaxWidth().height(100.dp).testTag("preview-navigation"), contentAlignment = Alignment.Center) {
            if (native) {
                CupertinoNavigationBarNative(
                    selectedTabIndex = { selected }, onTabSelected = { selected = it }, backdrop = LocalDemoBackdrop.current ?: rememberDefaultBackdrop(),
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    items = items.mapIndexed { index, (title, icon) ->
                        CupertinoNavigationBarItemData(onClick = { selected = index }, icon = rememberVectorPainter(icon), label = title)
                    },
                )
            } else {
                CupertinoNavigationBar(
                    selectedTabIndex = { selected }, onTabSelected = { selected = it }, tabsCount = items.size,
                    backdrop = LocalDemoBackdrop.current ?: rememberDefaultBackdrop(),
                    windowInsets = WindowInsets(0, 0, 0, 0),
                ) {
                    items.forEachIndexed { index, (title, icon) ->
                        CupertinoNavigationBarItem(
                            onClick = { selected = index },
                            icon = { CupertinoIcon(imageVector = icon, contentDescription = title) },
                            label = { CupertinoText(text = title) },
                        )
                    }
                }
            }
        }
        DemoResult("selectedTabIndex = $selected · ${items[selected].first}")
    }
    DemoSection("Properties") {
        DemoToggle("Native implementation", native) { native = it }
        NativeImplementationNote(native)
    }
}

@Composable
internal fun TopBarDemo() {
    var centered by rememberSaveable { mutableStateOf(true) }
    var subtitle by rememberSaveable { mutableStateOf(true) }
    var result by rememberSaveable { mutableStateOf("Try the toolbar actions") }
    DemoSection("Preview") {
        CompositionLocalProvider(LocalCupertinoRightSystemArea provides false) {
            CupertinoScaffold(
                modifier = Modifier.fillMaxWidth().height(180.dp),
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                topBar = {
                    CupertinoTopAppBar(
                        title = { CupertinoText(text = "Photos") },
                        subtitle = if (subtitle) { { CupertinoText(text = "12 items") } } else null,
                        isCenterAligned = centered, backdrop = LocalDemoBackdrop.current ?: rememberDefaultBackdrop(), windowInsets = WindowInsets(0, 0, 0, 0),
                        navigationIcon = {
                            CupertinoNavigateBackLiquidButton(
                                onClick = { result = "Back pressed" },
                                backdrop = LocalDemoBackdrop.current ?: rememberDefaultBackdrop(),
                                modifier = Modifier.testTag("preview-toolbar-back").semantics { contentDescription = "Back" },
                            )
                        },
                        actions = {
                            CupertinoLiquidIconButton(
                                onClick = { result = "Edit pressed" },
                                backdrop = LocalDemoBackdrop.current ?: rememberDefaultBackdrop(),
                            ) {
                                CupertinoIcon(CupertinoIcons.Default.Pencil, contentDescription = "Edit")
                            }
                        },
                    )
                },
            ) { padding ->
                Column(Modifier.fillMaxSize().padding(padding)) {
                    CupertinoNavigationTitle { CupertinoText(text = "Photos") }
                }
            }
        }
        DemoResult(result)
    }
    DemoSection("Properties") {
        DemoToggle("Centered title", centered) { centered = it }
        DemoToggle("Subtitle", subtitle) { subtitle = it }
    }
    DemoSection("Bottom toolbar") {
        CupertinoBottomAppBar(windowInsets = WindowInsets(0, 0, 0, 0)) {
            CupertinoBottomAppBarAction(onClick = { result = "Share pressed" }) { CupertinoText(text = "Share") }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { CupertinoText(text = "12 items") }
            CupertinoBottomAppBarAction(onClick = { result = "Delete pressed" }) { CupertinoText(text = "Delete") }
        }
    }
}

@Composable
internal fun FoundationDemo() {
    DemoSection("Typography", "Use Appearance in the catalog to change text size and theme.") {
        val typography = CupertinoTheme.typography
        listOf("Large Title" to typography.largeTitle, "Title 1" to typography.title1,
            "Title 2" to typography.title2, "Title 3" to typography.title3, "Headline" to typography.headline,
            "Body" to typography.body, "Callout" to typography.callout, "Subhead" to typography.subhead,
            "Footnote" to typography.footnote, "Caption 1" to typography.caption1, "Caption 2" to typography.caption2)
            .forEach { (name, style) -> CupertinoText(text = "$name · Hello", style = style) }
    }
    DemoSection("Surfaces and dividers") {
        val colors = CupertinoTheme.colorScheme
        listOf("systemBackground" to colors.systemBackground,
            "secondarySystemBackground" to colors.secondarySystemBackground,
            "tertiarySystemBackground" to colors.tertiarySystemBackground).forEach { (name, color) ->
            CupertinoSurface(color = color, modifier = Modifier.fillMaxWidth()) {
                CupertinoText(text = name, modifier = Modifier.padding(16.dp))
            }
        }
        CupertinoHorizontalDivider()
        Row(Modifier.height(40.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            CupertinoIcon(imageVector = CupertinoIcons.Default.Heart, contentDescription = "Heart", tint = colors.accent)
            CupertinoVerticalDivider()
            CupertinoText(text = "Secondary label", color = colors.secondaryLabel)
        }
    }
}

@OptIn(ExperimentalCupertinoApi::class)
@Composable
internal fun DisclosureGroupDemo() {
    var enabled by rememberSaveable { mutableStateOf(true) }
    var standalone by rememberSaveable { mutableStateOf(false) }
    var network by rememberSaveable { mutableStateOf(true) }
    var advanced by rememberSaveable { mutableStateOf(false) }
    var lazyGroup by rememberSaveable { mutableStateOf(false) }

    DemoSection("Preview", "Tap a row to show or hide its children.") {
        CupertinoDisclosureGroup(
            expanded = standalone,
            onExpandedChange = { standalone = it },
            enabled = enabled,
            modifier = Modifier.testTag("preview-disclosure"),
            label = { CupertinoText("Standalone group") },
        ) {
            CupertinoText("First child")
            CupertinoText("Second child")
        }
        DemoResult("expanded = $standalone")
    }
    DemoSection("In a section") {
        CupertinoSection {
            SectionDisclosureGroup(
                expanded = network,
                onExpandedChange = { network = it },
                enabled = enabled,
                title = { CupertinoText("Network") },
            ) {
                SectionItem { CupertinoText("Wi-Fi") }
                SectionItem { CupertinoText("Bluetooth") }
                SectionDisclosureGroup(
                    expanded = advanced,
                    onExpandedChange = { advanced = it },
                    enabled = enabled,
                    title = { CupertinoText("Advanced") },
                ) {
                    SectionItem { CupertinoText("Proxy") }
                    SectionItem { CupertinoText("DNS") }
                }
            }
            SectionItem { CupertinoText("Battery") }
        }
    }
    DemoSection("In a lazy section") {
        LazyColumn(Modifier.fillMaxWidth().height(320.dp)) {
            section {
                disclosureGroup(
                    expanded = lazyGroup,
                    onExpandedChange = { lazyGroup = it },
                    enabled = enabled,
                    key = "lazy-group",
                    title = { CupertinoText("Favorites") },
                ) {
                    item(key = "lazy-1") { CupertinoText("Photos", Modifier.padding(it)) }
                    item(key = "lazy-2") { CupertinoText("Documents", Modifier.padding(it)) }
                }
                item(key = "lazy-3") { CupertinoText("Recents", Modifier.padding(it)) }
            }
        }
    }
    DemoSection("Properties") { DemoToggle("Enabled", enabled) { enabled = it } }
}

internal const val WebViewDemoHomeUrl = "https://developer.apple.com/design/human-interface-guidelines/"

@Composable
internal fun WebViewDemo() {
    val state = rememberCupertinoWebViewState(WebViewDemoHomeUrl)
    var address by rememberSaveable { mutableStateOf(WebViewDemoHomeUrl) }
    var javaScript by rememberSaveable { mutableStateOf(true) }
    var systemBack by rememberSaveable { mutableStateOf(false) }
    var swipeGestures by rememberSaveable { mutableStateOf(true) }
    val focusManager = LocalFocusManager.current
    LaunchedEffect(state.url) { address = state.url }

    fun go(input: String) {
        CupertinoWebViewDefaults.normalizeUrl(input)?.let(state.navigator::loadUrl)
        focusManager.clearFocus()
    }

    DemoSection("Preview", "Address bar, progress and history controls bound to CupertinoWebViewState. Scroll this screen from outside the page.") {
        CompositionLocalProvider(LocalCupertinoRightSystemArea provides false) {
            CupertinoScaffold(
                modifier = Modifier.fillMaxWidth().height(400.dp),
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                topBar = {
                    CupertinoTopAppBar(
                        title = {
                            CupertinoText(text = state.title ?: "Web View", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        },
                        // The section's backdrop, read inside the section; the screen's own backdrop
                        // contains this bar and must not be sampled by it.
                        backdrop = LocalDemoBackdrop.current ?: rememberDefaultBackdrop(),
                        windowInsets = WindowInsets(0, 0, 0, 0),
                        navigationIcon = {
                            Row {
                                CupertinoIconButton(onClick = state.navigator::goBack, enabled = state.canGoBack,
                                    modifier = Modifier.testTag("web-back")) {
                                    CupertinoIcon(CupertinoIcons.Default.ChevronBackward, contentDescription = "Back")
                                }
                                CupertinoIconButton(onClick = state.navigator::goForward, enabled = state.canGoForward,
                                    modifier = Modifier.testTag("web-forward")) {
                                    CupertinoIcon(CupertinoIcons.Default.ChevronForward, contentDescription = "Forward")
                                }
                            }
                        },
                        actions = {
                            if (state.isLoading) {
                                CupertinoIconButton(onClick = state.navigator::stopLoading) {
                                    CupertinoIcon(CupertinoIcons.Default.Xmark, contentDescription = "Stop")
                                }
                            } else {
                                CupertinoIconButton(onClick = state.navigator::reload, modifier = Modifier.testTag("web-reload")) {
                                    CupertinoIcon(CupertinoIcons.Default.ArrowClockwise, contentDescription = "Reload")
                                }
                            }
                        },
                    )
                },
            ) { padding ->
                Column(Modifier.fillMaxSize().padding(padding), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CupertinoSearchTextField(
                        value = address,
                        onValueChange = { address = it },
                        modifier = Modifier.fillMaxWidth().testTag("web-address"),
                        paddingValues = PaddingValues(0.dp),
                        placeholder = { CupertinoText(text = "Search or enter website") },
                        keyboardActions = KeyboardActions(onSearch = { go(address) }, onGo = { go(address) }),
                        cancelButton = null,
                    )
                    CupertinoProgressIndicator(
                        progress = if (state.isLoading) state.progress else 0f,
                        modifier = Modifier.fillMaxWidth().testTag("web-progress"),
                    )
                    CupertinoWebView(
                        state = state,
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        settings = CupertinoWebViewDefaults.settings(
                            javaScriptEnabled = javaScript,
                            handleBackPress = systemBack,
                            allowsBackForwardNavigationGestures = swipeGestures,
                        ),
                    )
                }
            }
        }
        DemoResult(
            buildString {
                append(if (state.isLoading) "Loading ${(state.progress * 100).toInt()}%" else "Idle")
                append(" · back ${if (state.canGoBack) "yes" else "no"}")
                append(" · forward ${if (state.canGoForward) "yes" else "no"}")
                state.lastError?.let { append(" · error ${it.code}: ${it.description}") }
            },
        )
    }
    DemoSection("Settings", "JavaScript applies from the next page load. Swipe gestures are iOS only. System back is Android only and opt-in: it registers on the window, so its order against other back handlers is undefined.") {
        DemoToggle("JavaScript", javaScript) { javaScript = it }
        DemoToggle("System back navigates history", systemBack) { systemBack = it }
        DemoToggle("Back/forward swipe gestures", swipeGestures) { swipeGestures = it }
    }
    DemoSection("Edge cases", "Free-form input is normalised: bare domains get https, other text becomes a search.") {
        CupertinoButton(onClick = { go("https://unreachable.invalid/") }, colors = CupertinoButtonDefaults.tintedButtonColors(),
            modifier = Modifier.fillMaxWidth()) {
            CupertinoText(text = "Load an unreachable host")
        }
        CupertinoButton(onClick = { go("compose multiplatform") }, colors = CupertinoButtonDefaults.tintedButtonColors(),
            modifier = Modifier.fillMaxWidth()) {
            CupertinoText(text = "Search \"compose multiplatform\"")
        }
        CupertinoButton(onClick = { go(WebViewDemoHomeUrl) }, colors = CupertinoButtonDefaults.tintedButtonColors(),
            modifier = Modifier.fillMaxWidth()) {
            CupertinoText(text = "Home")
        }
    }
}

@Composable
internal fun LabelDemo() {
    val styles = listOf(
        "Automatic" to CupertinoLabelStyle.Automatic,
        "Title and icon" to CupertinoLabelStyle.TitleAndIcon,
        "Title only" to CupertinoLabelStyle.TitleOnly,
        "Icon only" to CupertinoLabelStyle.IconOnly,
    )
    DemoSection("Preview", "The same label in each label style.") {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            styles.forEach { (_, style) ->
                CupertinoLabel(text = "Favorites", icon = CupertinoIcons.Default.Heart, style = style,
                    iconTint = CupertinoTheme.colorScheme.accent)
            }
        }
    }
    DemoSection("Styles") {
        styles.forEach { (name, style) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                CupertinoText(text = name, style = CupertinoTheme.typography.footnote,
                    color = CupertinoTheme.colorScheme.secondaryLabel, modifier = Modifier.weight(1f))
                CupertinoLabel(text = "Favorites", icon = CupertinoIcons.Default.Heart, style = style)
            }
        }
    }
    DemoSection("Toolbar", "ProvideCupertinoLabelStyle(IconOnly) collapses Automatic labels into icons; " +
        "the titles stay as accessibility labels.") {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            ProvideCupertinoLabelStyle(CupertinoLabelStyle.IconOnly) {
                CompositionLocalProvider(zone.ien.hig.LocalContentColor provides CupertinoTheme.colorScheme.accent) {
                    CupertinoLabel(text = "Share", icon = CupertinoIcons.Default.SquareAndArrowUp)
                    CupertinoLabel(text = "Favorite", icon = CupertinoIcons.Default.Star)
                    CupertinoLabel(text = "Edit", icon = CupertinoIcons.Default.Pencil)
                    CupertinoLabel(text = "Delete", icon = CupertinoIcons.Filled.Trash)
                }
            }
        }
    }
    DemoSection("Text styles and wrapping", "The icon scales with the text style and stays on the first line.") {
        val typography = CupertinoTheme.typography
        listOf(typography.title2, typography.body, typography.footnote).forEach { style ->
            zone.ien.hig.ProvideTextStyle(style) {
                CupertinoLabel(text = "Starred", icon = CupertinoIcons.Default.Star)
            }
        }
        CupertinoLabel(
            text = "A long title wraps onto several lines while the icon stays aligned with the first one.",
            icon = CupertinoIcons.Default.Star,
        )
    }
    DemoSection("Right to left") {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CupertinoLabel(text = "المفضلة", icon = CupertinoIcons.Default.Heart)
                CupertinoLabel(text = "Favorites", icon = CupertinoIcons.Default.Heart, style = CupertinoLabelStyle.TitleOnly)
            }
        }
    }
}

private class OnboardingPage(val title: String, val subtitle: String, val icon: ImageVector?, val color: @Composable () -> Color)

@Composable
internal fun PageTabViewDemo() {
    var indexMode by rememberSaveable { mutableStateOf(CupertinoPageIndexDisplayMode.Automatic) }
    var backgroundMode by rememberSaveable { mutableStateOf(CupertinoPageIndexBackgroundDisplayMode.Automatic) }
    var pageCount by rememberSaveable { mutableIntStateOf(4) }
    var userScrollEnabled by rememberSaveable { mutableStateOf(true) }
    val isDark = CupertinoTheme.colorScheme.isDark
    val pages = listOf(
        OnboardingPage("Welcome", "Swipe to explore what's new.", CupertinoIcons.Filled.Star) { CupertinoColors.systemBlue(isDark) },
        OnboardingPage("Stay in the loop", "Get notified about what matters.", CupertinoIcons.Filled.Bell) { CupertinoColors.systemOrange(isDark) },
        OnboardingPage("Private by design", "Your data stays on your device.", CupertinoIcons.Filled.Lock) { CupertinoColors.systemIndigo(isDark) },
        OnboardingPage("Ready to go", "Photo pages work too.", null) { CupertinoColors.systemTeal(isDark) },
    )
    key(pageCount) {
        val state = rememberPagerState { pageCount }
        DemoSection("Preview", "Swipe the pages, tap a dot or scrub across the index.") {
            CupertinoPageTabView(
                state = state,
                modifier = Modifier.fillMaxWidth().height(320.dp).clip(CupertinoTheme.shapes.large).testTag("page-tab-view"),
                indexDisplayMode = indexMode,
                backgroundDisplayMode = backgroundMode,
                userScrollEnabled = userScrollEnabled,
                colors = CupertinoPageTabViewDefaults.colors(
                    currentIndicatorColor = Color.White,
                    indicatorColor = Color.White.copy(alpha = .4f),
                ),
            ) { index ->
                val page = pages[index % pages.size]
                Box(Modifier.fillMaxSize().background(page.color()), contentAlignment = Alignment.Center) {
                    if (page.icon == null) {
                        Image(painterResource(Res.drawable.catalog_landscape), contentDescription = null,
                            modifier = Modifier.matchParentSize(), contentScale = ContentScale.Crop)
                    }
                    Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        page.icon?.let { CupertinoIcon(it, contentDescription = null, tint = Color.White, modifier = Modifier.size(56.dp)) }
                        CupertinoText(text = page.title, color = Color.White, style = CupertinoTheme.typography.title1)
                        CupertinoText(text = page.subtitle, color = Color.White.copy(alpha = .85f),
                            style = CupertinoTheme.typography.subhead)
                    }
                }
            }
            DemoResult("currentPage = ${state.currentPage} · pageCount = $pageCount")
        }
    }
    DemoSection("Default colors", "System label colors with the background always shown.") {
        CupertinoPageTabView(
            pageCount = 3,
            modifier = Modifier.fillMaxWidth().height(140.dp),
            backgroundDisplayMode = CupertinoPageIndexBackgroundDisplayMode.Always,
        ) { index ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CupertinoText(text = "Page ${index + 1}", style = CupertinoTheme.typography.title2)
            }
        }
    }
    DemoSection("Properties") {
        DemoChoice("Index", CupertinoPageIndexDisplayMode.entries, indexMode, { indexMode = it }) { it.name }
        DemoChoice("Background", CupertinoPageIndexBackgroundDisplayMode.entries, backgroundMode, { backgroundMode = it }) { it.name }
        DemoChoice("Page count", listOf(1, 2, 4, 12), pageCount, { pageCount = it }) { "$it" }
        DemoToggle("User scroll enabled", userScrollEnabled) { userScrollEnabled = it }
    }
}

private data class DemoMailbox(
    val key: String,
    val title: String,
    val icon: ImageVector,
    val unread: Int = 0,
    val enabled: Boolean = true,
)

private enum class DemoSidebarWidth(val title: String) { Full("Full width"), Phone("Phone (360 dp)") }

private val demoMailboxes = listOf(
    DemoMailbox("inbox", "Inbox", CupertinoIcons.Default.TrayAndArrowDown, unread = 12),
    DemoMailbox("vip", "VIP", CupertinoIcons.Default.Star, unread = 2),
    DemoMailbox("flagged", "Flagged", CupertinoIcons.Default.Flag),
    DemoMailbox("drafts", "Drafts", CupertinoIcons.Default.Pencil),
    DemoMailbox("sent", "Sent", CupertinoIcons.Default.Paperplane),
)

private val demoFolders = listOf(
    DemoMailbox("archive", "Archive", CupertinoIcons.Default.Archivebox),
    DemoMailbox("trash", "Trash", CupertinoIcons.Default.Trash),
    DemoMailbox("junk", "Junk (disabled)", CupertinoIcons.Default.Envelope, enabled = false),
)

@Composable
internal fun SidebarDemo() {
    var width by rememberSaveable { mutableStateOf(DemoSidebarWidth.Full) }
    var rtl by rememberSaveable { mutableStateOf(false) }
    var startHidden by rememberSaveable { mutableStateOf(false) }
    var selected by rememberSaveable { mutableStateOf<String?>("inbox") }
    DemoSection(
        "Mail",
        "At 600 dp or more the mailboxes are a persistent sidebar column with a toggle. Narrower widths " +
            "collapse into a list that pushes the mailbox; Back returns to the list.",
    ) {
        DemoChoice("Width", DemoSidebarWidth.entries, width, { width = it }) { it.title }
        DemoToggle("Right-to-left", rtl) { rtl = it }
        DemoToggle("Start with sidebar hidden", startHidden) { startHidden = it }
        key(startHidden) {
            val state = rememberCupertinoSidebarState(
                initialVisibility = if (startHidden) CupertinoSidebarVisibility.Hidden else CupertinoSidebarVisibility.Automatic,
            )
            CompositionLocalProvider(LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CupertinoSurface(
                        modifier = Modifier
                            .then(if (width == DemoSidebarWidth.Phone) Modifier.width(360.dp) else Modifier.fillMaxWidth())
                            .height(560.dp)
                            .testTag("sidebar-demo"),
                        shape = CupertinoTheme.shapes.large,
                    ) {
                        CupertinoSidebarScaffold(
                            state = state,
                            selectedKey = selected,
                            onSelect = { selected = it as String },
                            sidebarWidth = 260.dp,
                            contentWindowInsets = WindowInsets(0),
                            sidebarTopBar = {
                                CupertinoText(
                                    text = "Mailboxes",
                                    style = CupertinoTheme.typography.title2,
                                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 4.dp),
                                )
                            },
                            sidebar = {
                                section(style = SectionStyle.Sidebar, title = { CupertinoText("Favorites") }) {
                                    demoMailboxes.forEach { mailboxRow(this, it) }
                                }
                                section(style = SectionStyle.Sidebar, title = { CupertinoText("iCloud") }) {
                                    demoFolders.forEach { mailboxRow(this, it) }
                                }
                            },
                            detail = { MailboxDetail(state, (demoMailboxes + demoFolders).firstOrNull { it.key == selected }) },
                        )
                    }
                }
            }
            DemoResult(
                "Selected: ${selected ?: "none"} · ${state.widthClass} · sidebar " +
                    if (state.isSidebarVisible) "shown" else "hidden",
            )
        }
    }
}

private fun CupertinoSidebarScope.mailboxRow(section: LazySectionScope, box: DemoMailbox) = with(section) {
    destination(
        key = box.key,
        enabled = box.enabled,
        leadingContent = { CupertinoIcon(box.icon, contentDescription = null) },
        trailingContent = { if (box.unread > 0) CupertinoText(box.unread.toString()) },
    ) { CupertinoText(box.title) }
}

@Composable
private fun MailboxDetail(state: CupertinoSidebarState, box: DemoMailbox?) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (state.widthClass == CupertinoSidebarWidthClass.Compact) {
                CupertinoNavigateBackLiquidButton(
                    onClick = state::showSidebar,
                    backdrop = rememberDefaultBackdrop(),
                    modifier = Modifier.semantics { contentDescription = "Mailboxes" },
                )
            } else {
                CupertinoSidebarToggleButton(state)
            }
            CupertinoText(text = box?.title ?: "No mailbox", style = CupertinoTheme.typography.headline)
        }
        CupertinoHorizontalDivider()
        if (box == null) {
            CupertinoText("Select a mailbox", Modifier.padding(20.dp), color = CupertinoTheme.colorScheme.secondaryLabel)
        } else {
            repeat(6) { index ->
                Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp)) {
                    CupertinoText("Sender ${index + 1}", style = CupertinoTheme.typography.headline)
                    CupertinoText(
                        "${box.title} message ${index + 1}",
                        style = CupertinoTheme.typography.subhead,
                        color = CupertinoTheme.colorScheme.secondaryLabel,
                    )
                }
                CupertinoHorizontalDivider(Modifier.padding(start = 20.dp))
            }
        }
    }
}
