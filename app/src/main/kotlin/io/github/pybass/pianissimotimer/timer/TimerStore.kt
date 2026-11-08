package io.github.pybass.pianissimotimer.timer

import android.content.Context
import io.github.pybass.pianissimotimer.bell.cancelBells
import io.github.pybass.pianissimotimer.bell.isBellScheduled
import io.github.pybass.pianissimotimer.bell.pauseBells
import io.github.pybass.pianissimotimer.bell.scheduleBells
import io.github.pybass.pianissimotimer.data.AppData
import io.github.pybass.pianissimotimer.data.appData
import io.github.pybass.pianissimotimer.sound.Sound
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// The only place that changes the saved timers and the timer state. Each state change updates the bells, then saves
// the state, so it survives a kill.
class TimerStore(private val context: Context) {
    // The saved data as the screens should show it.
    val data: Flow<AppData> = context.appData.data.map { data ->
        val state = data.state
        // A saved Running whose last bell Android has dropped would count down to no bell.
        val dropped = state is TimerState.Running && !isBellScheduled(context, state.bells.lastIndex)
        if (dropped) data.copy(state = TimerState.Idle) else data
    }

    // Adds the timer and chooses it. A taken name is ignored, so a double tap cannot add a timer twice.
    suspend fun add(timer: Timer) = update { data ->
        if (data.isTimerNameTaken(timer.name)) return@update data
        data.copy(timers = data.timers + timer, selectedTimerId = timer.id)
    }

    suspend fun select(id: String) = update { it.copy(selectedTimerId = id) }

    // Puts `timer` in place of the one with the same id.
    suspend fun replace(timer: Timer) = update { data ->
        data.copy(timers = data.timers.map { if (it.id == timer.id) timer else it })
    }

    // The last timer is never deleted. A deleted chosen timer gives way to the first one left.
    suspend fun delete(id: String) = update { data ->
        if (data.timers.size == 1) return@update data
        val timers = data.timers.filter { it.id != id }
        data.copy(timers = timers, selectedTimerId = if (data.selectedTimerId == id) timers.first().id else data.selectedTimerId)
    }

    // Hands Android one bell per step, all at once (docs/spec.md 5.3).
    suspend fun start(timer: Timer, sounds: List<Sound>) {
        val now = System.currentTimeMillis()
        val bells = timer.bells(sounds, start = now)
        scheduleBells(context, bells, from = 0)
        save(TimerState.Running(startedAt = now, bells = bells))
    }

    suspend fun pause(running: TimerState.Running) {
        val now = System.currentTimeMillis()
        // Every bell is already due: there is nothing left to pause.
        if (running.bells.last().dueAt <= now) return save(TimerState.Idle)
        val paused = TimerState.Paused(running.startedAt, pausedAt = now, bells = running.bells)
        pauseBells(context, paused.left, stepsLeft = running.bells.size - paused.step)
        save(paused)
    }

    suspend fun resume(paused: TimerState.Paused) {
        // Every bell and the start move on by the length of the pause; the bells that rang before it stay in the past.
        val shift = System.currentTimeMillis() - paused.pausedAt
        val bells = paused.bells.map { it.copy(dueAt = it.dueAt + shift) }
        scheduleBells(context, bells, from = paused.step)
        save(TimerState.Running(paused.startedAt + shift, bells))
    }

    suspend fun stop() {
        cancelBells(context)
        save(TimerState.Idle)
    }

    // The last bell has rung; it plays to its end.
    suspend fun done() = save(TimerState.Idle)

    private suspend fun update(change: (AppData) -> AppData) {
        context.appData.updateData { change(it) }
    }

    private suspend fun save(state: TimerState) = update { it.copy(state = state) }
}
