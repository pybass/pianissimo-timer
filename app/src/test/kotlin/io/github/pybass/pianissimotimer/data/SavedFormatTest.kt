package io.github.pybass.pianissimotimer.data

import io.github.pybass.pianissimotimer.timer.TimerState
import kotlin.test.Test
import kotlin.test.assertEquals

// The code writes and reads exactly the frozen JSON of the current version. A failure here means the saved format
// changed: bump FORMAT_VERSION, add a migration step and freeze the new JSON. Never fix it by editing the old JSON.
class SavedFormatTest {
    @Test
    fun `the document is written as the frozen json`() = assertEquals(V1.JSON, json.encodeToString(V1.data))

    @Test
    fun `the document is read from the frozen json`() = assertEquals(V1.data, json.decodeFromString<AppData>(V1.JSON))

    // Paused is covered by the document.
    @Test
    fun `the other states are written as the frozen json`() {
        assertEquals(V1.IDLE_JSON, json.encodeToString(V1.idle))
        assertEquals(V1.RUNNING_JSON, json.encodeToString(V1.running))
    }

    @Test
    fun `the other states are read from the frozen json`() {
        assertEquals(V1.idle, json.decodeFromString<TimerState>(V1.IDLE_JSON))
        assertEquals(V1.running, json.decodeFromString<TimerState>(V1.RUNNING_JSON))
    }
}
