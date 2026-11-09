@file:Suppress("FunctionName")

package io.github.pybass.pianissimotimer.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.pybass.pianissimotimer.data.AppData
import io.github.pybass.pianissimotimer.sound.Sound
import io.github.pybass.pianissimotimer.timer.Step
import io.github.pybass.pianissimotimer.timer.Timer
import io.github.pybass.pianissimotimer.ui.components.AddStepRow
import io.github.pybass.pianissimotimer.ui.components.PillButton
import io.github.pybass.pianissimotimer.ui.components.SoundBar
import io.github.pybass.pianissimotimer.ui.components.TimeWheel
import io.github.pybass.pianissimotimer.ui.components.TimerPicker
import io.github.pybass.pianissimotimer.ui.theme.Ink
import io.github.pybass.pianissimotimer.ui.theme.StepsWidth

// The chosen timer while it has one step: a plain timer, as in the Clock app. The time is set on the wheel right
// here and the sound on the row under it; no step screen opens. From two steps on, `TimerScreen` shows the list
// instead. Every change goes out through `onChange` at once, except a time of 0:00:00: that stays on the wheel, and
// Start waits for a time (docs/spec.md 5.1).
@Composable
fun OneStepScreen(
    data: AppData,
    onSelectTimer: (String) -> Unit,
    onNewTimer: () -> Unit,
    onRenameTimer: (Timer, String) -> Unit,
    onDeleteTimer: (Timer) -> Unit,
    step: Step,
    onChange: (Step) -> Unit,
    sounds: List<Sound>,
    onNewSound: () -> Unit,
    onEditSound: (Sound) -> Unit,
    onListen: (Sound) -> Unit,
    onAddStep: () -> Unit,
    onStart: () -> Unit,
) {
    // The time on the wheel, kept apart from the saved seconds: the step keeps its last time while the wheel shows
    // 0:00:00. Keyed by the timer's id: this screen stays on while another timer is chosen, and the wheel must then
    // show that timer's time, not the old one.
    var seconds by rememberSaveable(data.selectedTimerId) { mutableStateOf(step.seconds) }

    fun turn(new: Int) {
        seconds = new
        if (new > 0) onChange(step.copy(seconds = new))
    }
    // Built like the timer screen: the wheel and the rows sit a little above the middle, Start at the bottom.
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.weight(1f))
            TimerPicker(data, onSelectTimer, onNewTimer, onRenameTimer, onDeleteTimer)
            // The wheel reads its time once, when it is built; a new timer must build a new wheel.
            key(data.selectedTimerId) { TimeWheel(seconds) { turn(it) } }
            Spacer(Modifier.height(16.dp))
            Column(Modifier.width(StepsWidth)) {
                HorizontalDivider(color = Ink.copy(alpha = 0.12f))
                SoundBar(
                    sounds,
                    step.soundId,
                    onSelect = { onChange(step.copy(soundId = it)) },
                    onNewSound,
                    onEditSound,
                    step.volumeDb,
                    onVolumeChange = { onChange(step.copy(volumeDb = it)) },
                    onListen,
                )
                HorizontalDivider(color = Ink.copy(alpha = 0.12f))
                Spacer(Modifier.height(8.dp))
                AddStepRow(onAddStep, inList = false)
            }
            Spacer(Modifier.weight(2f))
        }
        Spacer(Modifier.height(16.dp))
        PillButton("Start", enabled = seconds > 0, modifier = Modifier.width(StepsWidth), onClick = onStart)
        Spacer(Modifier.height(32.dp))
    }
}
