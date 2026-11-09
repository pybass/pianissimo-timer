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
// when it does not read or breaks a rule of docs/data.md; the state when it does not read or was saved by another
// version (spec 6.3).
internal fun decode(text: String): AppData {
    val tree = try {
        json.parseToJsonElement(text) as? JsonObject
    } catch (e: IllegalArgumentException) {
        null
    }
    val saved = (tree?.get("version") as? JsonPrimitive)?.intOrNull
    if (tree == null || saved == null || saved !in 1..FORMAT_VERSION) return AppData.Default.copy(stateDropped = true)
    val current = migrate(tree, from = saved)
    val library = section<AppData>(JsonObject(current - "state"))?.takeIf { it.isValid }?.withChosenTimer() ?: AppData.Default
    val state = if (saved == FORMAT_VERSION) section<TimerState>(current["state"])?.takeIf { it.hasBells } else null
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

// The rules of docs/spec.md 5.1 the screens count on: an empty list crashes the pickers, a 0-second step rings with
// the one before.
private val AppData.isValid
    get() = timers.isNotEmpty() && sounds.isNotEmpty() &&
        timers.all { t -> t.steps.isNotEmpty() && t.steps.all { it.seconds > 0 } }

// A running or paused state with no bells would index past its list on the screen (docs/data.md: at least one).
private val TimerState.hasBells
    get() = when (this) {
        TimerState.Idle -> true
        is TimerState.Running -> bells.isNotEmpty()
        is TimerState.Paused -> bells.isNotEmpty()
    }

// The chosen timer must exist; otherwise the first one (docs/data.md).
private fun AppData.withChosenTimer() =
    if (timers.any { it.id == selectedTimerId }) this else copy(selectedTimerId = timers.first().id)
