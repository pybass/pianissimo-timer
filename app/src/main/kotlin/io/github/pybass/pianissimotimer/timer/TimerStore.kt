package io.github.pybass.pianissimotimer.timer

import android.content.Context
import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.pybass.pianissimotimer.bell.cancelBells
import io.github.pybass.pianissimotimer.bell.isBellScheduled
import io.github.pybass.pianissimotimer.bell.pauseBells
import io.github.pybass.pianissimotimer.bell.scheduleBells
import io.github.pybass.pianissimotimer.sound.SoundLibrary
import io.github.pybass.pianissimotimer.sound.json
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// One store per file in the whole app: a second DataStore on the same file throws.
private val Context.store by preferencesDataStore("timer", produceMigrations = { listOf(TimerDataReset(it)) })

private val LIBRARY = stringPreferencesKey("library")
private val STATE = stringPreferencesKey("state")
private val VERSION = intPreferencesKey("version")

// Bump when the saved format changes incompatibly, including the bells in the state. Unversioned data has format 1.
private const val DATA_VERSION = 1

private class TimerDataReset(private val context: Context) : DataMigration<Preferences> {
    // Validate once at initialization, before any reader or edit can see the saved data.
    override suspend fun shouldMigrate(currentData: Preferences) = true

    override suspend fun migrate(currentData: Preferences): Preferences {
        val prefs = currentData.toMutablePreferences()
        val reset = (prefs[VERSION] ?: 1) != DATA_VERSION
        val state = if (reset) null else decodeState(prefs)
        // An unreadable state becomes Idle below, so its bells must not stay.
        if (state == null) cancelBells(context)
        if (reset) prefs.clear()
        prefs[LIBRARY] = json.encodeToString(decodeLibrary(prefs))
        prefs[STATE] = json.encodeToString<TimerState>(state ?: TimerState.Idle)
        prefs[VERSION] = DATA_VERSION
        return prefs
    }

    override suspend fun cleanUp() = Unit
}

// The only place that changes the saved timers and the timer state. Each is one JSON string, so each change is
// atomic. Each state change updates the bells, then saves the state, so it survives a kill.
class TimerStore(private val context: Context) {
    val library: Flow<TimerLibrary> = context.store.data.map(::decodeLibrary)

    val state: Flow<TimerState> = context.store.data.map { prefs ->
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
        context.store.edit { prefs -> prefs[LIBRARY] = json.encodeToString(change(decodeLibrary(prefs))) }
    }

    private suspend fun save(state: TimerState) {
        context.store.edit { prefs -> prefs[STATE] = json.encodeToString<TimerState>(state) }
    }
}

private fun decodeLibrary(prefs: Preferences): TimerLibrary {
    val library = try {
        prefs[LIBRARY]?.let { json.decodeFromString<TimerLibrary>(it) }
    } catch (e: IllegalArgumentException) {
        null
    }
    return library?.takeIf { lib ->
        lib.timers.any { it.name == lib.selected } &&
            lib.timers.all { timer -> timer.steps.isNotEmpty() && timer.steps.all { it.seconds > 0 } }
    } ?: TimerLibrary(listOf(DefaultTimer), selected = DefaultTimer.name)
}

// Null when the saved state cannot be read, as after an update that added a field: a crash here would repeat on every
// launch.
private fun decodeState(prefs: Preferences): TimerState? {
    val text = prefs[STATE] ?: return TimerState.Idle
    return try {
        json.decodeFromString<TimerState>(text).takeIf {
            when (it) {
                TimerState.Idle -> true
                is TimerState.Running -> it.bells.isNotEmpty()
                is TimerState.Paused -> it.bells.isNotEmpty()
            }
        }
    } catch (e: IllegalArgumentException) {
        null
    }
}
