package io.github.pybass.pianissimotimer.timer

import io.github.pybass.pianissimotimer.bell.Bell
import io.github.pybass.pianissimotimer.sound.DefaultSound
import io.github.pybass.pianissimotimer.sound.SoundLibrary
import kotlinx.serialization.Serializable

// `sound` is the name of a sound in the library, not a copy: an edit of the sound reaches every step that uses it.
// `volumeDb` is 0 or less.
@Serializable
data class Step(val seconds: Int, val sound: String, val volumeDb: Int)

// A named list of steps that ring one after the other.
@Serializable
data class Timer(val name: String, val steps: List<Step>) {
    val seconds get() = steps.sumOf { it.seconds }

    fun withStep(index: Int, step: Step) = copy(steps = steps.mapIndexed { i, s -> if (i == index) step else s })

    // The last step is never removed. The caller checks.
    fun withoutStep(index: Int) = copy(steps = steps.filterIndexed { i, _ -> i != index })

    // The bells of a timer that starts at `start`, in ms since the epoch: the steps follow each other without a gap,
    // with the sounds as they are now.
    fun bells(sounds: SoundLibrary, start: Long): List<Bell> {
        var due = start
        return steps.map { step ->
            due += step.seconds * 1000L
            Bell(due, sounds.soundFor(step.sound), step.volumeDb)
        }
    }
}

// A timer as the app makes it: one step of 25 minutes with `sound` at full volume, so it can start at once. The first
// timer and every new one look the same; a new timer is not a copy of the chosen one.
fun newTimer(name: String, sound: String) = Timer(name, listOf(Step(seconds = 25 * 60, sound = sound, volumeDb = 0)))

// The timer the library starts with.
val DefaultTimer = newTimer("Timer", DefaultSound.name)

// The timers to choose from and the name of the chosen one. It always holds at least one timer, and no two names
// differ only in case.
@Serializable
data class TimerLibrary(val timers: List<Timer>, val selected: String) {
    val selectedTimer get() = timers.first { it.name == selected }

    fun isTaken(name: String) = timers.any { it.name.equals(name, ignoreCase = true) }

    // "Timer", then "Timer 2" and on, skipping taken names.
    fun newName() = generateSequence(1) { it + 1 }.map { if (it == 1) "Timer" else "Timer $it" }.first { !isTaken(it) }
}
