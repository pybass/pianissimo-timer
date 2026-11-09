package io.github.pybass.pianissimotimer.data

import android.content.Context
import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import io.github.pybass.pianissimotimer.bell.cancelBells
import io.github.pybass.pianissimotimer.timer.TimerState
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.intOrNull
import java.io.InputStream
import java.io.OutputStream

// The saved file. One store per file in the whole app: a second DataStore on the same file throws.
internal val Context.appData: DataStore<AppData> by dataStore(
    "pianissimo.json",
    AppDataSerializer,
    produceMigrations = { context -> listOf(CancelDroppedBells(context)) },
)

// The bytes of the file as the document of docs/data.md. `defaultValue` is what a store without a file holds: a
// fresh install.
private object AppDataSerializer : Serializer<AppData> {
    override val defaultValue = AppData.Default

    override suspend fun readFrom(input: InputStream) = decode(input.readBytes().decodeToString())

    override suspend fun writeTo(t: AppData, output: OutputStream) = output.write(json.encodeToString(t).encodeToByteArray())
}

// DataStore runs this once, on the first read, before the data reaches anyone. A state that `decode` dropped leaves
// its bells scheduled in Android; they are cancelled here, where there is a Context, and DataStore writes the clean
// document back to the file.
private class CancelDroppedBells(private val context: Context) : DataMigration<AppData> {
    override suspend fun shouldMigrate(currentData: AppData) = currentData.stateDropped

    override suspend fun migrate(currentData: AppData): AppData {
        cancelBells(context)
        return currentData.copy(stateDropped = false)
    }

    override suspend fun cleanUp() {}
}

// The file's text as the document of the current version. What cannot be read gives way to its default, never to a
// crash, which would repeat on every launch (docs/spec.md 6.2): the library (timers, sounds and the chosen timer)
// when it does not read, a value in it when it is out of range (Repair.kt), the state when it does not read, cannot
// be used or was saved by another version (spec 6.3). `to` and `steps` are the chain; only the tests pass their own.
internal fun decode(text: String, to: Int = FORMAT_VERSION, steps: List<Migration> = MIGRATIONS): AppData {
    val tree = try {
        json.parseToJsonElement(text) as? JsonObject
    } catch (e: IllegalArgumentException) {
        null
    }
    val saved = (tree?.get("version") as? JsonPrimitive)?.intOrNull
    if (tree == null || saved == null || saved !in 1..to) return AppData.Default.copy(stateDropped = true)
    // A step that fails on a tree it did not expect would otherwise crash every launch until the file is gone.
    val current = try {
        migrate(tree, from = saved, to = to, steps = steps)
    } catch (e: RuntimeException) {
        return AppData.Default.copy(stateDropped = true)
    }
    val library = section<AppData>(JsonObject(current - "state"))?.repaired() ?: AppData.Default
    val state = if (saved == to) section<TimerState>(current["state"])?.takeIf { it.isUsable } else null
    return library.copy(state = state ?: TimerState.Idle, stateDropped = state == null)
}

// A part of the tree as its class; null when it is missing or does not fit.
private inline fun <reified T> section(element: JsonElement?): T? = element?.let {
    try {
        json.decodeFromJsonElement<T>(it)
    } catch (e: IllegalArgumentException) {
        null
    }
}
