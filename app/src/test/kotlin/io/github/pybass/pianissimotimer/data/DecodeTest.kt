package io.github.pybass.pianissimotimer.data

import io.github.pybass.pianissimotimer.timer.DefaultTimer
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

    // The two tests below bring the v1 document to a made-up version 2 with a made-up step: there is no real step yet.

    @Test
    fun `a step that fails gives the defaults and cancels the bells`() {
        val failing = listOf<Migration> { throw IllegalStateException("not the tree the step knows") }
        assertEquals(defaultsAndNoBells, decode(V1.JSON, to = 2, steps = failing))
    }

    @Test
    fun `a version change keeps the library and drops the state`() {
        val unchanged = listOf<Migration> { it }
        val expected = V1.data.copy(version = 2, state = TimerState.Idle, stateDropped = true)
        assertEquals(expected, decode(V1.JSON, to = 2, steps = unchanged))
    }

    @Test
    fun `a library that does not read gives the default one and keeps the state`() {
        assertEquals(AppData.Default.copy(state = V1.paused), decode(with("timers", JsonPrimitive("none"))))
    }

    // One case each; RepairTest has the rest.
    @Test
    fun `a library that reads is repaired`() {
        val expected = V1.data.copy(timers = listOf(DefaultTimer), selectedTimerId = DefaultTimer.id)
        assertEquals(expected, decode(with("timers", JsonArray(emptyList()))))
        val zeroStep = V1.data.timers.first().let { it.withStep(0, it.steps.first().copy(seconds = 0)) }
        val zero = V1.data.copy(timers = listOf(zeroStep) + V1.data.timers.drop(1))
        assertEquals(V1.data, decode(json.encodeToString(zero)))
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

    // A bell is not repaired: it already stands in Android as it is.
    @Test
    fun `a state with a bell the app would never write gives idle and cancels the bells`() {
        val expected = V1.data.copy(state = TimerState.Idle, stateDropped = true)
        val loud = """{"type":"running","startedAt":1762600000000,"bells":[{"dueAt":1762601500000,""" +
            """"sound":{"id":"piano","name":"Piano","type":"piano","note":60,"fadeInSeconds":0.0,"lengthSeconds":5.0},""" +
            """"volumeDb":3}]}"""
        assertEquals(expected, decode(with("state", json.parseToJsonElement(loud))))
        val long = loud
            .replace(""""lengthSeconds":5.0""", """"lengthSeconds":-3.0""")
            .replace(""""volumeDb":3""", """"volumeDb":0""")
        assertEquals(expected, decode(with("state", json.parseToJsonElement(long))))
    }
}
