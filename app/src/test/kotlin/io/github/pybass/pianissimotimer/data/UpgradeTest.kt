package io.github.pybass.pianissimotimer.data

import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.pybass.pianissimotimer.sound.DefaultSounds
import io.github.pybass.pianissimotimer.sound.SoundLibrary
import io.github.pybass.pianissimotimer.timer.DefaultTimer
import io.github.pybass.pianissimotimer.timer.TimerLibrary
import io.github.pybass.pianissimotimer.timer.TimerState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private val defaultTimers = TimerLibrary(listOf(DefaultTimer), selected = DefaultTimer.name)
private val defaultSounds = SoundLibrary(DefaultSounds)

// A key no version has, where fake steps leave their mark.
private val TRAIL = stringPreferencesKey("trail")

private fun mark(letter: String): UpgradeStep = { it[TRAIL] = (it[TRAIL] ?: "") + letter }

// The chain itself, with fake steps. Each real step gets its own test: the frozen JSON of its version in, the values
// of the next version out.
class UpgradeTest {
    private val current = preferencesOf(
        VERSION to DATA_VERSION,
        TIMERS to V1.TIMERS_JSON,
        SOUNDS to V1.SOUNDS_JSON,
        STATE to V1.RUNNING_JSON,
    )

    @Test
    fun `a fresh install gets the defaults and the current version`() {
        val up = upgrade(preferencesOf())
        assertEquals(DATA_VERSION, up.data[VERSION])
        assertEquals(defaultTimers, decodeTimers(up.data))
        assertEquals(defaultSounds, decodeSounds(up.data))
        assertEquals(TimerState.Idle, decodeState(up.data))
        // A build before version 1 may have left bells with Android; a real fresh install has none to cancel.
        assertTrue(up.stateDropped)
    }

    @Test
    fun `data of the current version is kept as it is`() {
        val up = upgrade(current)
        assertEquals(current.asMap(), up.data.asMap())
        assertFalse(up.stateDropped)
    }

    @Test
    fun `the steps run in order from the saved version and drop the state`() {
        val old = preferencesOf(VERSION to 1, TIMERS to V1.TIMERS_JSON, SOUNDS to V1.SOUNDS_JSON, STATE to V1.RUNNING_JSON)
        val up = upgrade(old, steps = mapOf(1 to mark("a"), 2 to mark("b"), 3 to mark("c")), version = 4)
        assertEquals("abc", up.data[TRAIL])
        assertEquals(4, up.data[VERSION])
        assertEquals(V1.timers, decodeTimers(up.data))
        assertEquals(V1.sounds, decodeSounds(up.data))
        assertEquals(TimerState.Idle, decodeState(up.data))
        assertTrue(up.stateDropped)
    }

    @Test
    fun `a step before the saved version does not run`() {
        val up = upgrade(preferencesOf(VERSION to 2), steps = mapOf(1 to mark("a"), 2 to mark("b")), version = 3)
        assertEquals("b", up.data[TRAIL])
    }

    @Test
    fun `data without a step to the current version is dropped`() {
        val old = preferencesOf(VERSION to 1, TIMERS to V1.TIMERS_JSON, STATE to V1.RUNNING_JSON)
        val up = upgrade(old, steps = emptyMap(), version = 2)
        assertDropped(up, version = 2)
    }

    @Test
    fun `data a step cannot convert is dropped, not thrown`() {
        val old = preferencesOf(VERSION to 1, TIMERS to V1.TIMERS_JSON, STATE to V1.RUNNING_JSON)
        val up = upgrade(old, steps = mapOf<Int, UpgradeStep>(1 to { error("broken") }), version = 2)
        assertDropped(up, version = 2)
    }

    @Test
    fun `data of a newer version is dropped`() {
        val up = upgrade(preferencesOf(VERSION to DATA_VERSION + 1, TIMERS to V1.TIMERS_JSON, STATE to V1.RUNNING_JSON))
        assertDropped(up, version = DATA_VERSION)
    }

    @Test
    fun `unreadable timers give way to the default, the sounds stay`() {
        val up = upgrade(preferencesOf(VERSION to DATA_VERSION, TIMERS to "{not json", SOUNDS to V1.SOUNDS_JSON))
        assertEquals(defaultTimers, decodeTimers(up.data))
        assertEquals(V1.sounds, decodeSounds(up.data))
        assertFalse(up.stateDropped)
    }

    @Test
    fun `timers that break a rule give way to the default`() {
        // The chosen timer is not in the library.
        val timers = V1.TIMERS_JSON.replace(""""selected":"Pomodoro"""", """"selected":"Gone"""")
        val up = upgrade(preferencesOf(VERSION to DATA_VERSION, TIMERS to timers))
        assertEquals(defaultTimers, decodeTimers(up.data))
    }

    @Test
    fun `unreadable sounds give way to the default, the timers stay`() {
        val up = upgrade(preferencesOf(VERSION to DATA_VERSION, TIMERS to V1.TIMERS_JSON, SOUNDS to "{not json"))
        assertEquals(V1.timers, decodeTimers(up.data))
        assertEquals(defaultSounds, decodeSounds(up.data))
    }

    @Test
    fun `an unreadable state becomes idle and is reported dropped`() {
        val up = upgrade(preferencesOf(VERSION to DATA_VERSION, STATE to "{not json"))
        assertEquals(TimerState.Idle, decodeState(up.data))
        assertTrue(up.stateDropped)
    }

    @Test
    fun `a running state without bells becomes idle`() {
        val up = upgrade(preferencesOf(VERSION to DATA_VERSION, STATE to """{"type":"running","bells":[],"start":1}"""))
        assertEquals(TimerState.Idle, decodeState(up.data))
        assertTrue(up.stateDropped)
    }

    // Everything the user had is gone: default libraries, idle, bells to cancel.
    private fun assertDropped(up: Upgraded, version: Int) {
        assertEquals(version, up.data[VERSION])
        assertEquals(defaultTimers, decodeTimers(up.data))
        assertEquals(defaultSounds, decodeSounds(up.data))
        assertEquals(TimerState.Idle, decodeState(up.data))
        assertTrue(up.stateDropped)
    }
}
