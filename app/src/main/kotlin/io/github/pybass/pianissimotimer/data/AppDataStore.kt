package io.github.pybass.pianissimotimer.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import java.io.InputStream
import java.io.OutputStream

// The saved file. One store per file in the whole app: a second DataStore on the same file throws.
internal val Context.appData: DataStore<AppData> by dataStore("pianissimo.json", AppDataSerializer)

// The bytes of the file as the document of docs/data.md. `defaultValue` is what a store without a file holds: a
// fresh install.
private object AppDataSerializer : Serializer<AppData> {
    override val defaultValue = AppData.Default

    override suspend fun readFrom(input: InputStream) = decode(input.readBytes().decodeToString())

    override suspend fun writeTo(t: AppData, output: OutputStream) = output.write(json.encodeToString(t).encodeToByteArray())
}

// The document read back. A file the app cannot read gives way to the defaults: a crash here would repeat on every
// launch (docs/spec.md 6.2).
internal fun decode(text: String): AppData = try {
    json.decodeFromString<AppData>(text)
} catch (e: IllegalArgumentException) {
    AppData.Default
}
