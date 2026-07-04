package com.zacksimpson.reminders.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.abs

private val EDGE_WIDTH = 30.dp
private val DRAG_THRESHOLD = 80.dp

/**
 * Left-edge swipe-to-go-back gesture. LightOS doesn't provide an OS-level gesture-nav
 * back-swipe — swiping the edge does nothing at all, confirmed on-device — so this
 * reimplements the same behavior RN's SwipeBackContainer.tsx provided (edge-only start,
 * horizontal-dominant drag, single trigger past a threshold), using Compose's raw pointer
 * input rather than react-native-gesture-handler. Touches starting outside the edge zone
 * are never consumed, so scrolling and taps elsewhere are unaffected.
 */
@Composable
fun SwipeBackContainer(
    enabled: Boolean = true,
    onSwipeBack: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    val density = LocalDensity.current
    val edgeWidthPx = with(density) { EDGE_WIDTH.toPx() }
    val dragThresholdPx = with(density) { DRAG_THRESHOLD.toPx() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (down.position.x > edgeWidthPx) return@awaitEachGesture

                    var totalX = 0f
                    var totalY = 0f
                    var triggered = false

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (change.changedToUpIgnoreConsumed()) break

                        val dx = change.position.x - change.previousPosition.x
                        val dy = change.position.y - change.previousPosition.y
                        totalX += dx
                        totalY += dy

                        if (!triggered && abs(totalY) > abs(totalX) * 1.5f) {
                            break
                        }

                        if (!triggered && totalX > dragThresholdPx) {
                            triggered = true
                            onSwipeBack()
                        }
                    }
                }
            },
        content = content,
    )
}
