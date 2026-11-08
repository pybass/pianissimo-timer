package io.github.pybass.pianissimotimer.timer

import android.content.Context
import androidx.datastore.preferences.core.edit
import io.github.pybass.pianissimotimer.bell.cancelBells
import io.github.pybass.pianissimotimer.bell.isBellScheduled
import io.github.pybass.pianissimotimer.bell.pauseBells
import io.github.pybass.pianissimotimer.bell.scheduleBells
import io.github.pybass.pianissimotimer.data.STATE
import io.github.pybass.pianissimotimer.data.TIMERS
import io.github.pybass.pianissimotimer.data.appData
import io.github.pybass.pianissimotimer.data.decodeState
import io.github.pybass.pianissimotimer.data.decodeTimers
import io.github.pybass.pianissimotimer.sound.SoundLibrary
import io.github.pybass.pianissimotimer.sound.json
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// The only place that changes the saved timers and the timer state. Each state change updates the bells, then saves
// the state, so it survives a kill.
class TimerStore(private val context: Context) {
    val library: Flow<TimerLibrary> = context.appData.data.map(::decodeTimers)

    val state: Flow<TimerState> = context.appData.data.map { prefs ->
        val state = decodeState(prefs) ?: TimerState.Idle
        // A saved Running whose last bell Android has dropped would count down to no bell.
        if (state is TimerState.Running && !isBellScheduled(context, state.bells.lastIndex)) TimerState.Idle else state
    }

    // Adds the timer and chooses it. A taken name is ignored, so a double tap cannot add a timer twice.
    suspend fun add(timer: Timer) = update { lib ->
        if (lib.isTaken(timer.name)) return@update lib
        lib.copy(timers = lib.timers + timer, selected = timer.name)
    }

    suspend fun select(name: String) = update { it.copy(selected = name) }

    // Puts `timer` in place of the one named `old`; a renamed chosen timer stays chosen.
    suspend fun replace(old: String, timer: Timer) = update { lib ->
        lib.copy(
            timers = lib.timers.map { if (it.name == old) timer else it },
            selected = if (lib.selected == old) timer.name else lib.selected,
        )
    }

    // The last timer is never deleted. A deleted chosen timer gives way to the first one left.
    suspend fun delete(name: String) = update { lib ->
        if (lib.timers.size == 1) return@update lib
        val timers = lib.timers.filter { it.name != name }
        lib.copy(timers = timers, selected = if (lib.selected == name) timers.first().name else lib.selected)
    }

    // Hands Android one bell per step, all at once (docs/spec.md 5.3).
    suspend fun start(timer: Timer, sounds: SoundLibrary) {
        val now = System.currentTimeMillis()
        val bells = timer.bells(sounds, start = now)
        scheduleBells(context, bells, from = 0)
        save(TimerState.Running(bells, start = now))
    }

    suspend fun pause(running: TimerState.Running) {
        val now = System.currentTimeMillis()
        // Every bell is already due: there is nothing left to pause.
        if (running.bells.last().due <= now) return save(TimerState.Idle)
        val paused = TimerState.Paused(running.bells, running.start, at = now)
        pauseBells(context, paused.left, stepsLeft = running.bells.size - paused.step)
        save(paused)
    }

    suspend fun resume(paused: TimerState.Paused) {
        // Every bell and the start move on by the length of the pause; the bells that rang before it stay in the past.
        val shift = System.currentTimeMillis() - paused.at
        val bells = paused.bells.map { it.copy(due = it.due + shift) }
        scheduleBells(context, bells, from = paused.step)
        save(TimerState.Running(bells, paused.start + shift))
    }

    suspend fun stop() {
        cancelBells(context)
        save(TimerState.Idle)
    }

    // The last bell has rung; it plays to its end.
    suspend fun done() = save(TimerState.Idle)

    private suspend fun update(change: (TimerLibrary) -> TimerLibrary) {
        context.appData.edit { prefs -> prefs[TIMERS] = json.encodeToString(change(decodeTimers(prefs))) }
    }

    private suspend fun save(state: TimerState) {
        context.appData.edit { prefs -> prefs[STATE] = json.encodeToString<TimerState>(state) }
    }
}
