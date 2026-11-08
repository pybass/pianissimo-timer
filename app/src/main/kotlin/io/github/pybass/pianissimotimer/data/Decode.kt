package io.github.pybass.pianissimotimer.data

import androidx.datastore.preferences.core.Preferences
import io.github.pybass.pianissimotimer.sound.DefaultSounds
import io.github.pybass.pianissimotimer.sound.SoundLibrary
import io.github.pybass.pianissimotimer.sound.json
import io.github.pybass.pianissimotimer.timer.DefaultTimer
import io.github.pybass.pianissimotimer.timer.TimerLibrary
import io.github.pybass.pianissimotimer.timer.TimerState

// The saved JSON read back. Each reader has a fallback for JSON it cannot read or that breaks a rule of the spec: a
// crash here would repeat on every launch.

internal fun decodeTimers(prefs: Preferences): TimerLibrary {
    val library = try {
        prefs[TIMERS]?.let { json.decodeFromString<TimerLibrary>(it) }
    } catch (e: IllegalArgumentException) {
        null
    }
    return library?.takeIf { lib ->
        lib.timers.any { it.name == lib.selected } &&
            lib.timers.all { timer -> timer.steps.isNotEmpty() && timer.steps.all { it.seconds > 0 } }
    } ?: TimerLibrary(listOf(DefaultTimer), selected = DefaultTimer.name)
}

internal fun decodeSounds(prefs: Preferences): SoundLibrary {
    val library = try {
        prefs[SOUNDS]?.let { json.decodeFromString<SoundLibrary>(it) }
    } catch (e: IllegalArgumentException) {
        null
    }
    return library?.takeIf { it.sounds.isNotEmpty() } ?: SoundLibrary(DefaultSounds)
}

// Null when the saved state cannot be read, as after an update that added a field; no saved state is Idle.
internal fun decodeState(prefs: Preferences): TimerState? {
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
