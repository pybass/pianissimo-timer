@file:Suppress("FunctionName")

package io.github.pybass.pianissimotimer.ui.screens

import androidx.compose.runtime.Composable
import io.github.pybass.pianissimotimer.timer.TimerState

@Composable
fun PausedScreen(paused: TimerState.Paused, onResume: () -> Unit, onStop: () -> Unit) {
    RunLayout(
        bells = paused.bells,
        start = paused.start,
        step = paused.step,
        left = paused.left,
        leftInAll = paused.leftInAll,
        paused = true,
        primary = "Resume",
        onPrimary = onResume,
        onStop = onStop,
    )
}
