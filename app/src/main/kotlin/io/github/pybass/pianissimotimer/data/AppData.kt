package io.github.pybass.pianissimotimer.data

import io.github.pybass.pianissimotimer.sound.DefaultSounds
import io.github.pybass.pianissimotimer.sound.Sound
import io.github.pybass.pianissimotimer.timer.DefaultTimer
import io.github.pybass.pianissimotimer.timer.Timer
import io.github.pybass.pianissimotimer.timer.TimerState
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import java.util.UUID

// The version of the saved format: the shape of the document below. Bump it with every change that the data of the
// version before cannot be read as; docs/data.md lists what else such a change takes.
internal const val FORMAT_VERSION = 1

// The JSON of the saved file and of the bell Android carries.
// encodeDefaults: without it a field equal to its default is left out, and a later change of a default would silently
// change the saved data. ignoreUnknownKeys: JSON saved before a field was removed still reads.
internal val json = Json {
    encodeDefaults = true
    ignoreUnknownKeys = true
}

// The id of a timer or sound the user makes. The defaults have fixed ids instead.
fun newId(): String = UUID.randomUUID().toString()

// Everything the app saves, as one JSON document; docs/data.md shows it with every field explained. Timers and
// sounds are referenced by id, never by name. There is always at least one timer and one sound, and
// `selectedTimerId` is the id of a timer in `timers`.
@Serializable
data class AppData(
    val version: Int = FORMAT_VERSION,
    val selectedTimerId: String,
    val timers: List<Timer>,
    val sounds: List<Sound>,
    val state: TimerState = TimerState.Idle,
    // Not saved. Set by the reader when the saved state did not make it into `state`, so the store can cancel the
    // bells Android still holds for it (AppDataStore.kt). The screens never see it set.
    @Transient val stateDropped: Boolean = false,
) {
    val selectedTimer get() = timers.first { it.id == selectedTimerId }

    // Names are unique within their list, ignoring case.
    fun isTimerNameTaken(name: String) = timers.any { it.name.equals(name, ignoreCase = true) }

    fun isSoundNameTaken(name: String) = sounds.any { it.name.equals(name, ignoreCase = true) }

    // "Timer", then "Timer 2" and on, skipping taken names.
    fun newTimerName() = newName("Timer", ::isTimerNameTaken)

    fun newSoundName() = newName("Sound", ::isSoundNameTaken)

    companion object {
        // A fresh install: the default timer and sounds, idle.
        val Default = AppData(selectedTimerId = DefaultTimer.id, timers = listOf(DefaultTimer), sounds = DefaultSounds)
    }
}

private fun newName(base: String, isTaken: (String) -> Boolean) =
    generateSequence(1) { it + 1 }.map { if (it == 1) base else "$base $it" }.first { !isTaken(it) }
