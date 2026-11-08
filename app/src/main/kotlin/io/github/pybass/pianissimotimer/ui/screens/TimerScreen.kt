@file:Suppress("FunctionName")

package io.github.pybass.pianissimotimer.ui.screens

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.pybass.pianissimotimer.R
import io.github.pybass.pianissimotimer.data.AppData
import io.github.pybass.pianissimotimer.formatSeconds
import io.github.pybass.pianissimotimer.sound.Sound
import io.github.pybass.pianissimotimer.sound.soundFor
import io.github.pybass.pianissimotimer.timer.Step
import io.github.pybass.pianissimotimer.timer.Timer
import io.github.pybass.pianissimotimer.ui.components.ListenButton
import io.github.pybass.pianissimotimer.ui.components.PillButton
import io.github.pybass.pianissimotimer.ui.components.StepBar
import io.github.pybass.pianissimotimer.ui.components.TimeText
import io.github.pybass.pianissimotimer.ui.components.TimerPicker
import io.github.pybass.pianissimotimer.ui.theme.Action
import io.github.pybass.pianissimotimer.ui.theme.Ink
import io.github.pybass.pianissimotimer.ui.theme.Muted
import io.github.pybass.pianissimotimer.ui.theme.StepsWidth
import io.github.pybass.pianissimotimer.ui.theme.TimeStyle

// The chosen timer: its total time, a bar with the length of each step next to the others, and a row per step. A tap
// on a row opens the step; `onEditStep` and `onListen` get the step's index. `stepsScroll` is how far the list of
// steps is scrolled; the caller keeps it, so it outlives this screen.
@Composable
fun TimerScreen(
    data: AppData,
    onSelectTimer: (String) -> Unit,
    onNewTimer: () -> Unit,
    onRenameTimer: (Timer, String) -> Unit,
    onDeleteTimer: (Timer) -> Unit,
    stepsScroll: ScrollState,
    onEditStep: (Int) -> Unit,
    onAddStep: () -> Unit,
    onListen: (Int) -> Unit,
    onStart: () -> Unit,
) {
    val timer = data.selectedTimer
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        // Everything above Start. It gets the height Start leaves, so a long list cannot push Start off the screen.
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            // The two spacers share the free height 1 to 2, so the time and the steps sit a little above the middle.
            Spacer(Modifier.weight(1f))
            // The name is a caption to the time, so the two stay close.
            TimerPicker(data, onSelectTimer, onNewTimer, onRenameTimer, onDeleteTimer)
            TimeText(formatSeconds(timer.seconds))
            Spacer(Modifier.height(28.dp))
            StepBar(timer.steps)
            Spacer(Modifier.height(28.dp))
            // A Column measures its children without a weight in order, each within the height the earlier ones left.
            // The list must be the last of them: it then gets the rest and scrolls inside it. One more after it would
            // get no height.
            Column(Modifier.width(StepsWidth).verticalScroll(stepsScroll)) {
                timer.steps.forEachIndexed { i, step ->
                    if (i > 0) HorizontalDivider(color = Ink.copy(alpha = 0.12f))
                    StepRow(i, step, data.sounds, onClick = { onEditStep(i) }, onListen = { onListen(i) })
                }
                HorizontalDivider(color = Ink.copy(alpha = 0.12f))
                Row(
                    Modifier.fillMaxWidth().height(48.dp).clickable(onClick = onAddStep),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(painterResource(R.drawable.ic_add), contentDescription = null, tint = Action)
                    Text("Add step", color = Action, fontSize = 16.sp)
                }
            }
            Spacer(Modifier.weight(2f))
        }
        Spacer(Modifier.height(16.dp))
        PillButton("Start", modifier = Modifier.width(StepsWidth), onClick = onStart)
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun StepRow(index: Int, step: Step, sounds: List<Sound>, onClick: () -> Unit, onListen: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(56.dp).clickable(onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "${index + 1}",
            Modifier.width(16.dp),
            color = Muted,
            fontSize = 14.sp,
            style = TextStyle(fontFeatureSettings = "tnum"),
        )
        Text(
            formatSeconds(step.seconds),
            Modifier.width(88.dp),
            color = Ink,
            fontSize = 22.sp,
            style = TimeStyle,
        )
        Text(
            sounds.soundFor(step.soundId).name,
            Modifier.weight(1f),
            color = Ink,
            fontSize = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        // U+2212, the typographic minus: a hyphen looks too short next to digits.
        val db = if (step.volumeDb < 0) "−${-step.volumeDb}" else "0"
        Text("$db dB", color = Muted, fontSize = 14.sp, style = TextStyle(fontFeatureSettings = "tnum"))
        ListenButton(onListen)
    }
}
