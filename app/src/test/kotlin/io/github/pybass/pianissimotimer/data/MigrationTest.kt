package io.github.pybass.pianissimotimer.data

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

// The chain itself, with made-up steps; each real step gets its own test with the frozen JSON of its version.
class MigrationTest {
    // Step v writes its number into `trace`, after the one before: 1 → "1", 2 → "12".
    private val steps: List<Migration> = (1..3).map { v ->
        { tree: JsonObject -> JsonObject(tree + ("trace" to JsonPrimitive((tree["trace"]?.jsonPrimitive?.content ?: "") + v))) }
    }
    private val v1 = JsonObject(mapOf("version" to JsonPrimitive(1)))

    @Test
    fun `every step from the saved version runs in order and sets the version`() {
        val v4 = migrate(v1, from = 1, to = 4, steps = steps)
        assertEquals("123", v4["trace"]?.jsonPrimitive?.content)
        assertEquals(4, v4["version"]?.jsonPrimitive?.content?.toInt())
    }

    @Test
    fun `a document of a later version skips the steps before it`() {
        val v2 = JsonObject(mapOf("version" to JsonPrimitive(2)))
        assertEquals("23", migrate(v2, from = 2, to = 4, steps = steps)["trace"]?.jsonPrimitive?.content)
    }

    @Test
    fun `a document of the current version is left as it is`() = assertEquals(v1, migrate(v1, from = 1, to = 1, steps = steps))

    @Test
    fun `the chain reaches the current version`() = assertEquals(FORMAT_VERSION - 1, MIGRATIONS.size)
}
