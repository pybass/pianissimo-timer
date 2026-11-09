package io.github.pybass.pianissimotimer.data

import io.github.pybass.pianissimotimer.timer.TimerState
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

// What the app reads from a file it cannot fully use (docs/spec.md 6.2, 6.3). Each case breaks one part of the frozen
// document of the current version.
class DecodeTest {
    private val tree = json.parseToJsonElement(V1.JSON).jsonObject

    // The frozen document with one field replaced; `null` removes it.
    private fun with(field: String, value: JsonElement?) =
        JsonObject(if (value == null) tree - field else tree + (field to value)).toString()

    private val defaultsAndNoBells = AppData.Default.copy(stateDropped = true)

    @Test
    fun `the frozen document reads as is`() = assertEquals(V1.data, decode(V1.JSON))

    @Test
    fun `a file that is not a json document gives the defaults and cancels the bells`() {
        assertEquals(defaultsAndNoBells, decode("{not json"))
        assertEquals(defaultsAndNoBells, decode("[1, 2]"))
    }

    @Test
    fun `a version the app does not know gives the defaults and cancels the bells`() {
        assertEquals(defaultsAndNoBells, decode(with("version", null)))
        assertEquals(defaultsAndNoBells, decode(with("version", JsonPrimitive(0))))
        assertEquals(defaultsAndNoBells, decode(with("version", JsonPrimitive(FORMAT_VERSION + 1))))
    }

    @Test
    fun `a library that breaks a rule gives the default one and keeps the state`() {
        val expected = AppData.Default.copy(state = V1.paused)
        assertEquals(expected, decode(with("timers", JsonArray(emptyList()))))
        assertEquals(expected, decode(with("sounds", JsonArray(emptyList()))))
        assertEquals(expected, decode(with("timers", JsonPrimitive("none"))))
        val zeroStep = """[{"id":"t","name":"T","steps":[{"seconds":0,"soundId":"piano","volumeDb":0}]}]"""
        assertEquals(expected, decode(with("timers", json.parseToJsonElement(zeroStep))))
    }

    @Test
    fun `a chosen timer that is gone gives way to the first one`() {
        val expected = V1.data.copy(selectedTimerId = V1.data.timers.first().id)
        assertEquals(expected, decode(with("selectedTimerId", JsonPrimitive("gone"))))
    }

    @Test
    fun `a state that does not read gives idle and cancels the bells`() {
        val expected = V1.data.copy(state = TimerState.Idle, stateDropped = true)
        assertEquals(expected, decode(with("state", null)))
        assertEquals(expected, decode(with("state", JsonPrimitive("running"))))
        assertEquals(expected, decode(with("state", json.parseToJsonElement("""{"type":"running"}"""))))
        val noBells = """{"type":"paused","startedAt":1762600000000,"pausedAt":1762600600000,"bells":[]}"""
        assertEquals(expected, decode(with("state", json.parseToJsonElement(noBells))))
    }
}
