package io.github.pybass.pianissimotimer.data

import io.github.pybass.pianissimotimer.sound.DefaultSounds
import io.github.pybass.pianissimotimer.sound.Sound
import io.github.pybass.pianissimotimer.sound.SoundType
import io.github.pybass.pianissimotimer.timer.DefaultTimer
import io.github.pybass.pianissimotimer.timer.Step
import io.github.pybass.pianissimotimer.timer.newStep
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

// What a value the app would never write becomes on read (docs/data.md). Each case changes one value of the frozen
// document: the flute, or the first step of the first timer.
class RepairTest {
    private fun sound(change: Sound.() -> Sound) =
        V1.data.copy(sounds = V1.data.sounds.map { if (it.id == "flute") it.change() else it })

    private fun step(change: Step.() -> Step): AppData {
        val t = V1.data.timers.first()
        return V1.data.copy(timers = listOf(t.withStep(0, t.steps.first().change())) + V1.data.timers.drop(1))
    }

    @Test
    fun `the frozen document is left as it is`() = assertEquals(V1.data, V1.data.repaired())

    @Test
    fun `a sound setting outside its range gives way to its default`() {
        assertEquals(sound { copy(note = 60) }, sound { copy(note = 35) }.repaired())
        assertEquals(sound { copy(note = 60) }, sound { copy(note = 85) }.repaired())
        assertEquals(sound { copy(fadeInSeconds = 0.0) }, sound { copy(fadeInSeconds = -0.1) }.repaired())
        assertEquals(sound { copy(fadeInSeconds = 0.0) }, sound { copy(fadeInSeconds = 10.1) }.repaired())
        assertEquals(sound { copy(lengthSeconds = 5.0) }, sound { copy(lengthSeconds = 0.9) }.repaired())
        assertEquals(sound { copy(lengthSeconds = 5.0) }, sound { copy(lengthSeconds = 12.1) }.repaired())
        assertEquals(sound { copy(lengthSeconds = 5.0) }, sound { copy(lengthSeconds = Double.NaN) }.repaired())
    }

    @Test
    fun `a sound setting at the end of its range is kept`() {
        for (s in listOf(
            sound { copy(note = 36) },
            sound { copy(note = 84) },
            sound { copy(fadeInSeconds = 0.0) },
            sound { copy(fadeInSeconds = 10.0) },
            sound { copy(lengthSeconds = 1.0) },
            sound { copy(lengthSeconds = 12.0) },
        )) {
            assertEquals(s, s.repaired())
        }
    }

    @Test
    fun `a step value outside its range gives way to its default`() {
        assertEquals(step { copy(seconds = 1500) }, step { copy(seconds = 0) }.repaired())
        assertEquals(step { copy(seconds = 1500) }, step { copy(seconds = 36000) }.repaired())
        assertEquals(step { copy(volumeDb = 0) }, step { copy(volumeDb = 1) }.repaired())
        assertEquals(step { copy(volumeDb = 0) }, step { copy(volumeDb = -46) }.repaired())
    }

    @Test
    fun `a step value at the end of its range is kept`() {
        for (s in listOf(
            step { copy(seconds = 1) },
            step { copy(seconds = 35999) },
            step { copy(volumeDb = 0) },
            step { copy(volumeDb = -45) },
        )) {
            assertEquals(s, s.repaired())
        }
    }

    @Test
    fun `a sound type the app does not know reads as the default type`() {
        val text = """{"id":"x","name":"X","type":"harp","note":60,"fadeInSeconds":0.0,"lengthSeconds":5.0}"""
        assertEquals(SoundType.PIANO, json.decodeFromString<Sound>(text).type)
    }

    @Test
    fun `an item with a repeated id is kept under a new one`() {
        val twice = V1.data.copy(
            sounds = V1.data.sounds + V1.data.sounds.first(),
            timers = V1.data.timers + V1.data.timers.first(),
        )
        val repaired = twice.repaired()
        assertEquals(twice.sounds.size, repaired.sounds.distinctBy { it.id }.size)
        assertEquals(twice.timers.size, repaired.timers.distinctBy { it.id }.size)
        assertNotEquals(repaired.sounds.first().id, repaired.sounds.last().id)
        assertEquals(repaired.sounds.first().copy(id = repaired.sounds.last().id), repaired.sounds.last())
        assertEquals(repaired.timers.first().copy(id = repaired.timers.last().id), repaired.timers.last())
        assertEquals(twice.selectedTimerId, repaired.selectedTimerId)
    }

    @Test
    fun `a timer with no steps gets the step of a new timer with the first sound`() {
        val none = V1.data.copy(timers = V1.data.timers.map { it.copy(steps = emptyList()) })
        val expected = V1.data.copy(timers = V1.data.timers.map { it.copy(steps = listOf(newStep("piano"))) })
        assertEquals(expected, none.repaired())
    }

    @Test
    fun `no sounds or no timers give the defaults`() {
        assertEquals(V1.data.copy(sounds = DefaultSounds), V1.data.copy(sounds = emptyList()).repaired())
        val expected = V1.data.copy(timers = listOf(DefaultTimer), selectedTimerId = DefaultTimer.id)
        assertEquals(expected, V1.data.copy(timers = emptyList()).repaired())
    }

    @Test
    fun `a chosen timer that is gone gives way to the first one`() {
        val expected = V1.data.copy(selectedTimerId = V1.data.timers.first().id)
        assertEquals(expected, V1.data.copy(selectedTimerId = "gone").repaired())
    }
}
