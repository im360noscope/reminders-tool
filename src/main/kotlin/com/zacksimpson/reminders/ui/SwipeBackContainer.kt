package com.zacksimpson.reminders.ui

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.abs

private val EDGE_WIDTH = 10.dp
private val DRAG_THRESHOLD = 80.dp

/**
 * Left-edge swipe-to-go-back gesture. LightOS doesn't provide an OS-level gesture-nav
 * back-swipe — swiping the edge does nothing at all, confirmed on-device — so this
 * reimplements the same behavior RN's SwipeBackContainer.tsx provided (edge-only start,
 * horizontal-dominant drag, single trigger past a threshold). RN's version got this for
 * free from react-native-gesture-handler's native cross-gesture arbitration
 * (`activeOffsetX` lets an underlying Pressable still win below the activation
 * threshold); Compose's plain pointerInput has no equivalent, so this is a deliberately
 * simple approximation, not a full port of that arbitration behavior.
 *
 * The gesture detector lives on its own narrow strip along the left edge, layered on top
 * of (not wrapped around) [content], rather than spanning the whole screen. That means it
 * only ever competes for touches that start in that strip; everywhere else, [content] (a
 * scrolling list, a button) is the sole recipient. EDGE_WIDTH is kept small (10dp, not a
 * generous 30dp) specifically so it sits inside the dead margin before any real tap
 * target — list-row checkboxes and LightTopBar's back button both start around 12-13dp
 * from the edge, and a wider strip was overlapping their tap area, not just its own.
 * Uses Compose's own detectHorizontalDragGestures rather than a hand-rolled pointer loop
 * — its built-in touch-slop detection already discriminates vertical-dominant drags and
 * doesn't consume them, so a scroll that starts inside the strip is left alone; only a
 * drag it actually recognizes as horizontal gets consumed.
 *
 * If LightOS or the SDK ever ships real edge-swipe-back support, this whole file and its
 * call sites are meant to be deleted/replaced wholesale, not evolved in place — no known
 * plan for that exists yet (checked the SDK repo directly: no changelog, roadmap, or
 * TODOs mention it), so this stays intentionally simple rather than over-investing in
 * matching RNGH's arbitration quality.
 */
@Composable
fun SwipeBackContainer(
    enabled: Boolean = true,
    onSwipeBack: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    val density = LocalDensity.current
    val dragThresholdPx = remember(density) { with(density) { DRAG_THRESHOLD.toPx() } }

    Box(modifier = Modifier.fillMaxSize()) {
        content()

        if (enabled) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .width(EDGE_WIDTH)
                    .pointerInput(onSwipeBack) {
                        var totalX = 0f
                        var totalY = 0f
                        var triggered = false
                        detectHorizontalDragGestures(
                            onDragStart = {
                                totalX = 0f
                                totalY = 0f
                                triggered = false
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                totalX += dragAmount
                                totalY += change.positionChange().y
                                if (!triggered && totalX > dragThresholdPx && abs(totalY) < abs(totalX) * 1.5f) {
                                    triggered = true
                                    onSwipeBack()
                                }
                            },
                        )
                    },
            )
        }
    }
}
