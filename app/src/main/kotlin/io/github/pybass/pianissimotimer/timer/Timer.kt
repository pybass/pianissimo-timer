package io.github.pybass.pianissimotimer.timer

import io.github.pybass.pianissimotimer.bell.Bell
import io.github.pybass.pianissimotimer.sound.DefaultSound
import io.github.pybass.pianissimotimer.sound.Sound
import io.github.pybass.pianissimotimer.sound.soundFor
import kotlinx.serialization.Serializable

// `soundId` names a sound in the library; the step keeps no copy, so an edit of the sound reaches every step that
// uses it.
@Serializable
data class Step(val seconds: Int, val soundId: String, val volumeDb: Int)

// What the wheels offer: up to 9:59:59, and a volume only down. A saved value outside is repaired to the field's
// default (docs/data.md).
val StepSecondsRange = 1..35999
val VolumeDbRange = -45..0

// A step as the app makes it: 25 minutes with the sound `soundId` at full volume, so a timer can start at once.
fun newStep(soundId: String) = Step(seconds = 25 * 60, soundId = soundId, volumeDb = 0)

// A named list of steps that ring one after the other. `id` never changes; the name may.
@Serializable
data class Timer(val id: String, val name: String, val steps: List<Step>) {
    val seconds get() = steps.sumOf { it.seconds }

    fun withStep(index: Int, step: Step) = copy(steps = steps.mapIndexed { i, s -> if (i == index) step else s })

    // The last step is never removed. The caller checks.
    fun withoutStep(index: Int) = copy(steps = steps.filterIndexed { i, _ -> i != index })

    // The bells of a timer that starts at `start`, in ms since the epoch: the steps follow each other without a gap,
    // with the sounds as they are now.
    fun bells(sounds: List<Sound>, start: Long): List<Bell> {
        var due = start
        return steps.map { step ->
            due += step.seconds * 1000L
            Bell(due, sounds.soundFor(step.soundId), step.volumeDb)
        }
    }
}

// A timer as the app makes it: one new step. The first timer and every new one look the same; a new timer is not a
// copy of the chosen one.
fun newTimer(id: String, name: String, soundId: String) = Timer(id, name, listOf(newStep(soundId)))

// The timer the library starts with. Its id is fixed, like the ids of the default sounds.
val DefaultTimer = newTimer("timer", "Timer", DefaultSound.id)
