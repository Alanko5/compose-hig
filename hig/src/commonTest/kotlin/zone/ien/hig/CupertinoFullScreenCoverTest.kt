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

import androidx.compose.ui.graphics.Color
import zone.ien.hig.theme.darkColorScheme
import zone.ien.hig.theme.lightColorScheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCupertinoApi::class)
class CupertinoFullScreenCoverTest {

    // region pure logic

    @Test
    fun coverStaysInCompositionUntilHiddenAnimationSettles() {
        assertFalse(fullScreenCoverInComposition(isPresented = false, settledHidden = true))
        assertTrue(fullScreenCoverInComposition(isPresented = true, settledHidden = true))
        assertTrue(fullScreenCoverInComposition(isPresented = false, settledHidden = false))
        assertTrue(fullScreenCoverInComposition(isPresented = true, settledHidden = false))
    }

    @Test
    fun interactiveDismissIsReportedOnlyWhenPresentedShownAndSettledHidden() {
        assertTrue(isInteractiveFullScreenCoverDismiss(isPresented = true, wasShown = true, settledHidden = true))
        // Before the first show the sheet rests hidden; that is not a dismissal.
        assertFalse(isInteractiveFullScreenCoverDismiss(isPresented = true, wasShown = false, settledHidden = true))
        // Programmatic dismissal (caller already set isPresented = false).
        assertFalse(isInteractiveFullScreenCoverDismiss(isPresented = false, wasShown = true, settledHidden = true))
        // Still dragging / animating.
        assertFalse(isInteractiveFullScreenCoverDismiss(isPresented = true, wasShown = true, settledHidden = false))
    }

    @Test
    fun systemBarAppearanceFollowsContainerLuminance() {
        assertFalse(fullScreenCoverUsesLightSystemBars(Color.Black))
        assertTrue(fullScreenCoverUsesLightSystemBars(Color.White))
        assertTrue(fullScreenCoverUsesLightSystemBars(lightColorScheme().systemBackground))
        assertFalse(fullScreenCoverUsesLightSystemBars(darkColorScheme().systemBackground))
        assertFalse(fullScreenCoverUsesLightSystemBars(Color(0xFF1C1C1E)))
    }

    @Test
    fun propertiesDefaultToIosBehaviour() {
        val properties = CupertinoFullScreenCoverProperties()
        assertTrue(properties.dismissOnBackPress)
        assertFalse(properties.interactiveDismissEnabled)
        assertNull(properties.paneTitle)
        assertEquals(properties, CupertinoFullScreenCoverProperties(dismissOnBackPress = true))
    }

    @Test
    fun hiddenSheetFollowsResizedWindowSynchronously() {
        // The Android cover dialog is laid out once at its initial window height and again after it
        // grows to edge-to-edge. The resulting anchor change must snap in place: a snap deferred to a
        // later dispatch would cancel the show() animation started in between, leaving the cover
        // hidden below the window.
        val state = CupertinoSheetState(presentationStyle = PresentationStyle.Fullscreen)
        val initialAnchors = mapOf<CupertinoSheetValue, Float>(CupertinoSheetValue.Hidden to 2274f, CupertinoSheetValue.Expanded to 0f)
        val resizedAnchors = mapOf<CupertinoSheetValue, Float>(CupertinoSheetValue.Hidden to 2400f, CupertinoSheetValue.Expanded to 0f)
        state.swipeableState.updateAnchors(initialAnchors)
        assertEquals(2274f, state.offset)

        var deferredSnaps = 0
        var animations = 0
        val handler =
            BottomSheetScaffoldAnchorChangeHandler(
                state = state,
                animateTo = { _, _ -> animations++ },
                snapTo = { deferredSnaps++ },
            )
        state.swipeableState.updateAnchors(resizedAnchors)
        handler.onAnchorsChanged(CupertinoSheetValue.Hidden, initialAnchors, resizedAnchors)

        assertEquals(2400f, state.offset)
        assertEquals(CupertinoSheetValue.Hidden, state.currentValue)
        assertEquals(0, deferredSnaps)
        assertEquals(0, animations)
    }

    // endregion
}
