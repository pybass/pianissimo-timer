package io.github.pybass.pianissimotimer.timer

import io.github.pybass.pianissimotimer.bell.Bell
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Saved as JSON; the `type` field names the state. Both Running and Paused keep the bells, one per step, so a later
// edit of the timer or a sound does not change what rings. `start` is when the first step began, in ms since the
// epoch: a step's progress is measured from it or from the bell before.
@Serializable
sealed interface TimerState {
    @Serializable
    @SerialName("idle")
    data object Idle : TimerState

    @Serializable
    @SerialName("running")
    data class Running(val bells: List<Bell>, val start: Long) : TimerState {
        // The index of the step whose bell is next, or the last step once every bell has rung.
        fun step(now: Long) = bells.next(now)
    }

    // `at` is when Pause was pressed, in ms since the epoch: the bells keep the times they had, and the time left
    // is measured from `at`.
    @Serializable
    @SerialName("paused")
    data class Paused(val bells: List<Bell>, val start: Long, val at: Long) : TimerState {
        val step get() = bells.next(at)

        // The time left of the paused step, in ms.
        val left get() = bells[step].due - at

        // The time left of the whole timer, in ms.
        val leftInAll get() = bells.last().due - at
    }
}

private fun List<Bell>.next(now: Long) = indexOfFirst { it.due > now }.takeIf { it >= 0 } ?: lastIndex
