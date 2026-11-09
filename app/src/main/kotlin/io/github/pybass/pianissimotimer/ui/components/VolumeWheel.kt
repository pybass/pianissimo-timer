@file:Suppress("FunctionName")

package io.github.pybass.pianissimotimer.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.pybass.pianissimotimer.R
import io.github.pybass.pianissimotimer.timer.VolumeDbRange
import io.github.pybass.pianissimotimer.ui.theme.Ink
import io.github.pybass.pianissimotimer.ui.theme.Muted
import kotlin.math.abs

// 0 dB plays the sound as made; the app can only turn it down.
private val VolumeSteps = (VolumeDbRange.last downTo VolumeDbRange.first step 3).toList()

private val WheelItemHeight = 24.dp

// A vertical wheel of volume steps; the one in the middle is the value. 0 dB is at the top, so nothing is above it.
// At rest only the value shows, with an arrow to each side it can turn; the neighbours appear while it turns.
@Composable
fun VolumeWheel(volumeDb: Int, onChange: (Int) -> Unit) {
    val state = rememberLazyListState(initialFirstVisibleItemIndex = VolumeSteps.indexOf(volumeDb))
    // It changes while the wheel turns, not only when it stops. Before the first layout no item is placed yet.
    val centered by remember {
        derivedStateOf {
            val info = state.layoutInfo
            val middle = (info.viewportStartOffset + info.viewportEndOffset) / 2
            info.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2 - middle) }?.index
                ?: state.firstVisibleItemIndex
        }
    }
    val neighbours by animateFloatAsState(if (state.isScrollInProgress) 1f else 0f)
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(centered) {
        if (VolumeSteps[centered] != volumeDb) {
            haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
            onChange(VolumeSteps[centered])
        }
    }
    Box(Modifier.width(64.dp).height(WheelItemHeight * 3)) {
        // The padding of one item above and below lets the first and the last step reach the middle.
        LazyColumn(
            Modifier.fillMaxSize(),
            state = state,
            contentPadding = PaddingValues(vertical = WheelItemHeight),
            flingBehavior = rememberSnapFlingBehavior(state),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            items(VolumeSteps.size) { i ->
                // U+2212, the typographic minus: a hyphen looks too short next to digits.
                val db = if (VolumeSteps[i] < 0) "−${-VolumeSteps[i]}" else "0"
                Box(Modifier.height(WheelItemHeight), contentAlignment = Alignment.Center) {
                    if (i == centered) {
                        Text(
                            buildAnnotatedString {
                                append(db)
                                withStyle(SpanStyle(color = Muted, fontSize = 11.sp)) { append(" dB") }
                            },
                            color = Ink,
                            fontSize = 17.sp,
                            style = TextStyle(fontFeatureSettings = "tnum"),
                        )
                    } else {
                        Text(
                            db,
                            Modifier.alpha(neighbours),
                            color = Muted,
                            fontSize = 12.sp,
                            style = TextStyle(fontFeatureSettings = "tnum"),
                        )
                    }
                }
            }
        }
        // The arrows take no touches, so a swipe on them still turns the wheel.
        val arrow = Modifier.padding(vertical = 5.dp).size(14.dp).alpha(1f - neighbours)
        if (centered > 0) {
            Icon(
                painterResource(R.drawable.ic_expand),
                contentDescription = null,
                arrow.align(Alignment.TopCenter).rotate(180f),
                tint = Muted,
            )
        }
        if (centered < VolumeSteps.lastIndex) {
            Icon(
                painterResource(R.drawable.ic_expand),
                contentDescription = null,
                arrow.align(Alignment.BottomCenter),
                tint = Muted,
            )
        }
    }
}
