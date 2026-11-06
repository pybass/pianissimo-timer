@file:Suppress("FunctionName")

package io.github.pybass.pianissimotimer.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.pybass.pianissimotimer.MAX_DIGITS
import io.github.pybass.pianissimotimer.R
import io.github.pybass.pianissimotimer.digitsToSeconds
import io.github.pybass.pianissimotimer.secondsToDigits
import io.github.pybass.pianissimotimer.sound.Sound
import io.github.pybass.pianissimotimer.sound.SoundLibrary
import io.github.pybass.pianissimotimer.timer.Step
import io.github.pybass.pianissimotimer.ui.components.Keypad
import io.github.pybass.pianissimotimer.ui.components.PillButton
import io.github.pybass.pianissimotimer.ui.components.SoundBar
import io.github.pybass.pianissimotimer.ui.components.StepBar
import io.github.pybass.pianissimotimer.ui.components.TypedTime
import io.github.pybass.pianissimotimer.ui.theme.Ink
import io.github.pybass.pianissimotimer.ui.theme.Muted
import io.github.pybass.pianissimotimer.ui.theme.StepsWidth

// One step: its length on the keypad, its sound and volume on the sound row. Every change goes out through `onChange`
// at once, except a time of 0:00: that stays on the screen, and the screen cannot be left until a time is typed
// (docs/spec.md 5.1). Done only goes back. `steps` are all the steps of the timer named `timerName`, for the bar;
// `number` is the step's place among them, from 1. `canDelete` is false for the only step of the timer, which hides
// Delete.
@Composable
fun StepScreen(
    timerName: String,
    steps: List<Step>,
    number: Int,
    step: Step,
    onChange: (Step) -> Unit,
    sounds: SoundLibrary,
    onNewSound: () -> Unit,
    onEditSound: (Sound) -> Unit,
    onListen: (Sound) -> Unit,
    onDelete: () -> Unit,
    canDelete: Boolean,
    onDone: () -> Unit,
) {
    // The typed digits, kept apart from the saved seconds so typing works as in docs/spec.md 3.1 and 3.2.
    var digits by rememberSaveable { mutableStateOf(secondsToDigits(step.seconds)) }

    // Leading zeros are dropped, so empty digits mean 0:00. The step keeps its last time until a new one is typed.
    fun type(new: String) {
        digits = new
        if (new.isNotEmpty()) onChange(step.copy(seconds = digitsToSeconds(new)))
    }
    // The system back gesture goes back, as Done does, and waits like Done while the time is 0:00.
    BackHandler { if (digits.isNotEmpty()) onDone() }
    // The free height goes under the rows, so Done sits at the bottom like Start on the timer screen.
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(16.dp))
        // The step is what is being edited; the timer's name is its context. Delete is a quiet icon at the edge: far
        // from the thumb, found when wanted.
        Box(Modifier.width(StepsWidth), contentAlignment = Alignment.Center) {
            // Padded on both sides by the icon's width, so a long name is cut before it and stays centered.
            Row(
                Modifier.padding(horizontal = 48.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Step $number", color = Ink, fontSize = 17.sp)
                Text(timerName, color = Muted, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (canDelete) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.align(Alignment.CenterEnd),
                    colors = IconButtonDefaults.iconButtonColors(contentColor = Muted),
                ) {
                    Icon(painterResource(R.drawable.ic_delete), contentDescription = "Delete step")
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        // This step lit among the others; it grows as the digits come.
        StepBar(steps, current = number - 1)
        Spacer(Modifier.height(10.dp))
        TypedTime(digits)
        Spacer(Modifier.height(12.dp))
        Keypad(
            // Leading zeros carry no value; dropping them keeps all six places for real digits.
            onDigits = { type((digits + it).trimStart('0').take(MAX_DIGITS)) },
            onDelete = { type(digits.dropLast(1)) },
        )
        Spacer(Modifier.height(16.dp))
        // The step's row of the timer screen, opened, between the same hairlines.
        Column(Modifier.width(StepsWidth)) {
            HorizontalDivider(color = Ink.copy(alpha = 0.12f))
            SoundBar(
                sounds,
                step.sound,
                onSelect = { onChange(step.copy(sound = it)) },
                onNewSound,
                onEditSound,
                step.volumeDb,
                onVolumeChange = { onChange(step.copy(volumeDb = it)) },
                onListen,
            )
            HorizontalDivider(color = Ink.copy(alpha = 0.12f))
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.BottomCenter) {
            PillButton("Done", enabled = digits.isNotEmpty(), modifier = Modifier.width(StepsWidth), onClick = onDone)
        }
        Spacer(Modifier.height(32.dp))
    }
}
