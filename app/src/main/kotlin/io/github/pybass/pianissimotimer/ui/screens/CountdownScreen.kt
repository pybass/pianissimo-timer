@file:Suppress("FunctionName")

package io.github.pybass.pianissimotimer.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.github.pybass.pianissimotimer.timer.TimerState
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

// Counts down to the bell of the current step, then moves on to the next. `onDone` runs once the last bell is due.
@Composable
fun CountdownScreen(running: TimerState.Running, onPause: () -> Unit, onStop: () -> Unit, onDone: () -> Unit) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val last = running.bells.last().due
    // The time left is derived from the bells' times, not counted down: Android pauses the app with the screen off,
    // and a counter would fall behind.
    LaunchedEffect(last) {
        while (now < last) {
            // Wake when the shown second changes, so a slow wake-up never makes the display skip a second. Every due
            // is a whole number of seconds before `last`, so one schedule serves every step.
            delay(((last - now - 1) % 1000 + 1).milliseconds)
            now = System.currentTimeMillis()
        }
        onDone()
    }
    val step = running.step(now)
    RunLayout(
        bells = running.bells,
        start = running.start,
        step = step,
        left = running.bells[step].due - now,
        leftInAll = last - now,
        paused = false,
        primary = "Pause",
        onPrimary = onPause,
        onStop = onStop,
    )
}
