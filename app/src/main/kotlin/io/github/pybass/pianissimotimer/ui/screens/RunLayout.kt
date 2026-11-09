@file:Suppress("FunctionName")

package io.github.pybass.pianissimotimer.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.pybass.pianissimotimer.bell.Bell
import io.github.pybass.pianissimotimer.formatLeft
import io.github.pybass.pianissimotimer.formatSeconds
import io.github.pybass.pianissimotimer.ui.components.PillButton
import io.github.pybass.pianissimotimer.ui.components.TimeText
import io.github.pybass.pianissimotimer.ui.theme.Action
import io.github.pybass.pianissimotimer.ui.theme.Brass
import io.github.pybass.pianissimotimer.ui.theme.Ink
import io.github.pybass.pianissimotimer.ui.theme.Muted
import io.github.pybass.pianissimotimer.ui.theme.Paper
import io.github.pybass.pianissimotimer.ui.theme.StepsWidth
import io.github.pybass.pianissimotimer.ui.theme.TimeStyle
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin

// The countdown and the paused screen share this: the time left of the current step, the time left in all, the steps
// as a list to read, and Stop next to the main button, named by `primary` (Pause or Resume). `left` and `leftInAll`
// are in ms. A one-step timer gets no list and no "in all": both would repeat the time above.
// Its caption names the sound instead.
@Composable
fun RunLayout(
    bells: List<Bell>,
    start: Long,
    step: Int,
    left: Long,
    leftInAll: Long,
    paused: Boolean,
    primary: String,
    onPrimary: () -> Unit,
    onStop: () -> Unit,
) {
    // A long list follows the current step and keeps the step before it in view, so the user sees what just rang.
    // 49 dp is a row with its divider.
    val scrollTo = (step - 1).coerceAtLeast(0) * with(LocalDensity.current) { 49.dp.roundToPx() }
    // Starts at the right place, so the list does not run there from the top each time Pause swaps the screen.
    val scroll = remember { ScrollState(scrollTo) }
    LaunchedEffect(scrollTo) { scroll.animateScrollTo(scrollTo) }
    // Built like the timer screen: the buttons stay at the bottom and a long list scrolls above them.
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.weight(1f))
            val from = if (step == 0) start else bells[step - 1].dueAt
            val length = bells[step].dueAt - from
            ProgressArc(progress = if (length > 0) (1 - left.toFloat() / length).coerceIn(0f, 1f) else 1f, paused)
            TimeText(formatLeft(left), muted = paused)
            val about = if (bells.size == 1) "${bells[0].sound.name} at the end" else "${formatLeft(leftInAll)} in all"
            val parts = listOfNotNull("Paused".takeIf { paused }, about)
            Text(parts.joinToString(", "), color = Muted, fontSize = 15.sp, style = TextStyle(fontFeatureSettings = "tnum"))
            Spacer(Modifier.height(28.dp))
            // The last child without a weight, as on the timer screen: it gets the height left and scrolls inside it.
            if (bells.size > 1) {
                Column(Modifier.width(StepsWidth).verticalScroll(scroll)) {
                    bells.forEachIndexed { i, bell ->
                        if (i > 0) HorizontalDivider(color = Ink.copy(alpha = 0.12f))
                        val from = if (i == 0) start else bells[i - 1].dueAt
                        BellRow(i, bell, seconds = ((bell.dueAt - from) / 1000).toInt(), done = i < step, current = i == step)
                    }
                }
            }
            Spacer(Modifier.weight(2f))
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.width(StepsWidth), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PillButton("Stop", outlined = true, modifier = Modifier.weight(1f), onClick = onStop)
            PillButton(primary, modifier = Modifier.weight(1.6f), onClick = onPrimary)
        }
        Spacer(Modifier.height(32.dp))
    }
}

// The fermata over the time: the dot runs along the arc from left to right as the step goes by; `progress` is 0..1.
// Paused, the track is dashed and the dot hollow, the same language as the dashed bar of an unset step.
@Composable
private fun ProgressArc(progress: Float, paused: Boolean) {
    Canvas(Modifier.size(260.dp, 72.dp)) {
        // A flat arc, like a fermata, not a half circle: a chord of the full width with a rise of 60 dp.
        val rise = 60.dp.toPx()
        val half = size.width / 2
        val r = (rise * rise + half * half) / (2 * rise)
        val halfSweep = Math.toDegrees(asin((half / r).toDouble())).toFloat()
        val startAngle = 270f - halfSweep
        // 8 dp above the arc's top: room for the dot.
        val center = Offset(half, 8.dp.toPx() + r)
        val topLeft = center - Offset(r, r)
        val arcSize = Size(2 * r, 2 * r)
        if (paused) {
            val dash = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 6.dp.toPx()))
            val style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, pathEffect = dash)
            drawArc(Muted.copy(alpha = 0.7f), startAngle, 2 * halfSweep, false, topLeft, arcSize, style = style)
        } else {
            // Thin lines: the time under the arc stays the heaviest thing on the screen.
            val track = Stroke(2.dp.toPx(), cap = StrokeCap.Round)
            drawArc(Action.copy(alpha = 0.22f), startAngle, 2 * halfSweep, false, topLeft, arcSize, style = track)
            val done = Stroke(3.dp.toPx(), cap = StrokeCap.Round)
            drawArc(Action, startAngle, 2 * halfSweep * progress, false, topLeft, arcSize, style = done)
        }
        val angle = Math.toRadians((startAngle + 2 * halfSweep * progress).toDouble())
        val head = center + Offset(r * cos(angle).toFloat(), r * sin(angle).toFloat())
        if (paused) {
            drawCircle(Paper, 6.5.dp.toPx(), head)
            drawCircle(Brass, 6.5.dp.toPx(), head, style = Stroke(2.5.dp.toPx()))
        } else {
            drawCircle(Brass, 6.5.dp.toPx(), head)
        }
    }
}

// The same columns as a row of the timer screen, without the volume and Listen. A done step is muted; the current
// one gets a dot in the margin, so the columns stay where the timer screen has them.
@Composable
private fun BellRow(index: Int, bell: Bell, seconds: Int, done: Boolean, current: Boolean) {
    val color = if (done) Muted else Ink
    Row(
        Modifier.fillMaxWidth().height(48.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "${index + 1}",
            Modifier.width(16.dp).drawBehind {
                if (current) drawCircle(Brass, radius = 3.5.dp.toPx(), center = Offset(-12.dp.toPx(), size.height / 2))
            },
            color = if (current) Action else Muted,
            fontSize = 14.sp,
            style = TextStyle(fontFeatureSettings = "tnum"),
        )
        Text(
            formatSeconds(seconds),
            Modifier.width(88.dp),
            color = color,
            fontSize = 22.sp,
            style = TimeStyle,
        )
        Text(
            bell.sound.name,
            Modifier.weight(1f),
            color = color,
            fontSize = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
