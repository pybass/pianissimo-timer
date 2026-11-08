package io.github.pybass.pianissimotimer.data

import io.github.pybass.pianissimotimer.sound.SoundLibrary
import io.github.pybass.pianissimotimer.sound.json
import io.github.pybass.pianissimotimer.timer.TimerLibrary
import io.github.pybass.pianissimotimer.timer.TimerState
import kotlin.test.Test
import kotlin.test.assertEquals

// The code writes and reads exactly the frozen JSON of the current version. A failure here means the saved format
// changed: bump DATA_VERSION, add a step and freeze the new JSON. Never fix it by editing the old JSON.
class SavedFormatTest {
    @Test
    fun `timers are written as the frozen json`() = assertEquals(V1.TIMERS_JSON, json.encodeToString(V1.timers))

    @Test
    fun `timers are read from the frozen json`() = assertEquals(V1.timers, json.decodeFromString<TimerLibrary>(V1.TIMERS_JSON))

    @Test
    fun `sounds are written as the frozen json`() = assertEquals(V1.SOUNDS_JSON, json.encodeToString(V1.sounds))

    @Test
    fun `sounds are read from the frozen json`() = assertEquals(V1.sounds, json.decodeFromString<SoundLibrary>(V1.SOUNDS_JSON))

    @Test
    fun `each state is written as the frozen json`() {
        assertEquals(V1.IDLE_JSON, json.encodeToString(V1.idle))
        assertEquals(V1.RUNNING_JSON, json.encodeToString(V1.running))
        assertEquals(V1.PAUSED_JSON, json.encodeToString(V1.paused))
    }

    @Test
    fun `each state is read from the frozen json`() {
        assertEquals(V1.idle, json.decodeFromString<TimerState>(V1.IDLE_JSON))
        assertEquals(V1.running, json.decodeFromString<TimerState>(V1.RUNNING_JSON))
        assertEquals(V1.paused, json.decodeFromString<TimerState>(V1.PAUSED_JSON))
    }
}
