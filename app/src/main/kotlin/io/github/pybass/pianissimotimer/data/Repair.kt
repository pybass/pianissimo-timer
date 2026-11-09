package io.github.pybass.pianissimotimer.data

import io.github.pybass.pianissimotimer.bell.Bell
import io.github.pybass.pianissimotimer.sound.DefaultSounds
import io.github.pybass.pianissimotimer.sound.FadeInRange
import io.github.pybass.pianissimotimer.sound.LengthRange
import io.github.pybass.pianissimotimer.sound.NoteRange
import io.github.pybass.pianissimotimer.sound.Sound
import io.github.pybass.pianissimotimer.timer.DefaultTimer
import io.github.pybass.pianissimotimer.timer.Step
import io.github.pybass.pianissimotimer.timer.StepSecondsRange
import io.github.pybass.pianissimotimer.timer.Timer
import io.github.pybass.pianissimotimer.timer.TimerState
import io.github.pybass.pianissimotimer.timer.VolumeDbRange
import io.github.pybass.pianissimotimer.timer.newStep

// A saved value the app would never write gives way to the default of its field, never to the loss of its item or
// of the library (docs/spec.md 6.2, docs/data.md). Only values are repaired: an item that does not decode at all
// takes the library down to the default one (AppDataStore.kt), and only a broken migration step could write one.
internal fun AppData.repaired(): AppData {
    val sounds = sounds.map { it.repaired() }.withUniqueIds({ it.id }) { s, id -> s.copy(id = id) }.ifEmpty { DefaultSounds }
    val timers = timers.map { it.repaired(sounds.first().id) }.withUniqueIds({ it.id }) { t, id -> t.copy(id = id) }
        .ifEmpty { listOf(DefaultTimer) }
    val selected = if (timers.any { it.id == selectedTimerId }) selectedTimerId else timers.first().id
    return copy(selectedTimerId = selected, timers = timers, sounds = sounds)
}

// A state is not repaired: its bells are copies made at Start and already stand in Android. One the screens or the
// player could not use is dropped to idle, and the bells with it (AppDataStore.kt).
internal val TimerState.isUsable
    get() = when (this) {
        TimerState.Idle -> true
        is TimerState.Running -> bells.areUsable
        is TimerState.Paused -> bells.areUsable
    }

// At least one bell (docs/data.md), each with a sound and a volume the app could have written.
private val List<Bell>.areUsable
    get() = isNotEmpty() && all { it.sound.repaired() == it.sound && it.volumeDb in VolumeDbRange }

// `Sound(id, name)` is the defaults. `in` on a range is false for NaN, so it is repaired too.
private fun Sound.repaired(): Sound {
    val d = Sound(id, name)
    return copy(
        note = note.takeIf { it in NoteRange } ?: d.note,
        fadeInSeconds = fadeInSeconds.takeIf { it in FadeInRange } ?: d.fadeInSeconds,
        lengthSeconds = lengthSeconds.takeIf { it in LengthRange } ?: d.lengthSeconds,
    )
}

// A timer with no steps gets the step of a new timer, with the first sound.
private fun Timer.repaired(firstSoundId: String) =
    copy(steps = steps.map { it.repaired() }.ifEmpty { listOf(newStep(firstSoundId)) })

// `soundId` is left alone: a sound that is gone falls back when the step plays (docs/spec.md 5.2).
private fun Step.repaired(): Step {
    val d = newStep(soundId)
    return copy(
        seconds = seconds.takeIf { it in StepSecondsRange } ?: d.seconds,
        volumeDb = volumeDb.takeIf { it in VolumeDbRange } ?: d.volumeDb,
    )
}

// Every id once: the item that repeats one is kept under a new id.
private fun <T> List<T>.withUniqueIds(id: (T) -> String, withId: (T, String) -> T): List<T> {
    val seen = mutableSetOf<String>()
    return map { if (seen.add(id(it))) it else withId(it, newId()) }
}
