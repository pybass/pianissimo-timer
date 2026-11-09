@file:Suppress("FunctionName")

package io.github.pybass.pianissimotimer.ui.components

import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import io.github.pybass.pianissimotimer.timer.StepSecondsRange
import kotlin.math.abs

private val RowHeight = 84.dp

// The hours wheel ends where the longest step does: 9.
private val LastHour = StepSecondsRange.last / 3600

// Three wheels, h:mm:ss. The number in the middle of each is the value; the neighbours above and below shrink and
// fade with their distance, so the time reads as a wheel at rest. `seconds` is the whole time; `onChange` gets the
// new whole time.
@Composable
fun TimeWheel(seconds: Int, onChange: (Int) -> Unit) {
    val h = (seconds / 3600).coerceAtMost(LastHour)
    val m = seconds % 3600 / 60
    val s = seconds % 60
    Row(verticalAlignment = Alignment.CenterVertically) {
        // Hours stay muted while 0: a place not in use.
        DigitWheel(h, last = LastHour, digits = 1, mutedZero = true) { onChange(it * 3600 + m * 60 + s) }
        TimeText(":")
        DigitWheel(m, last = 59, digits = 2) { onChange(h * 3600 + it * 60 + s) }
        TimeText(":")
        DigitWheel(s, last = 59, digits = 2) { onChange(h * 3600 + m * 60 + it) }
    }
}

// One wheel of 0..`last`, each shown with `digits` places. As `VolumeWheel`: a snapping list whose middle item is the
// value, a tick of the haptics at each change.
@Composable
private fun DigitWheel(value: Int, last: Int, digits: Int, mutedZero: Boolean = false, onChange: (Int) -> Unit) {
    val state = rememberLazyListState(initialFirstVisibleItemIndex = value)
    val centered by remember {
        derivedStateOf {
            val info = state.layoutInfo
            val middle = (info.viewportStartOffset + info.viewportEndOffset) / 2
            info.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2 - middle) }?.index
                ?: state.firstVisibleItemIndex
        }
    }
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(centered) {
        if (centered != value) {
            haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
            onChange(centered)
        }
    }
    // The padding of one row above and below lets 0 and `last` reach the middle.
    LazyColumn(
        Modifier.width(if (digits == 1) 56.dp else 108.dp).height(RowHeight * 3),
        state = state,
        contentPadding = PaddingValues(vertical = RowHeight),
        flingBehavior = rememberSnapFlingBehavior(state),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        items(last + 1) { i ->
            Box(
                Modifier.height(RowHeight).graphicsLayer {
                    // Distance from the middle in rows: 0 for the value, 1 for a neighbour at rest. Read here, not in
                    // composition, so a turn of the wheel only redraws.
                    val info = state.layoutInfo
                    val item = info.visibleItemsInfo.firstOrNull { it.index == i } ?: return@graphicsLayer
                    val middle = (info.viewportStartOffset + info.viewportEndOffset) / 2f
                    val distance = abs(item.offset + item.size / 2f - middle) / item.size
                    val near = distance.coerceAtMost(1f)
                    val far = (distance - 1f).coerceIn(0f, 1f)
                    // A neighbour is half the size and faint; the row after it has faded out.
                    scaleX = 1f - 0.5f * near - 0.2f * far
                    scaleY = scaleX
                    alpha = (1f - 0.6f * near - 0.4f * far).coerceAtLeast(0f)
                },
                contentAlignment = Alignment.Center,
            ) {
                TimeText(i.toString().padStart(digits, '0'), muted = mutedZero && i == 0)
            }
        }
    }
}
