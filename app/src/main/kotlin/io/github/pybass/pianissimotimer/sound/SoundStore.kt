package io.github.pybass.pianissimotimer.sound

import android.content.Context
import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// One store per file in the whole app: a second DataStore on the same file throws.
private val Context.store by preferencesDataStore("sounds", produceMigrations = { listOf(SoundDataReset) })

private val LIBRARY = stringPreferencesKey("library")
private val VERSION = intPreferencesKey("version")

// Bump when the saved format changes incompatibly. Unversioned data has format 1.
private const val DATA_VERSION = 1

private object SoundDataReset : DataMigration<Preferences> {
    // Validate once at initialization, before any reader or edit can see the saved data.
    override suspend fun shouldMigrate(currentData: Preferences) = true

    override suspend fun migrate(currentData: Preferences): Preferences {
        val prefs = currentData.toMutablePreferences()
        if ((prefs[VERSION] ?: 1) != DATA_VERSION) prefs.clear()
        prefs[LIBRARY] = json.encodeToString(decode(prefs))
        prefs[VERSION] = DATA_VERSION
        return prefs
    }

    override suspend fun cleanUp() = Unit
}

// The only place that changes the saved sounds. The whole library is one JSON string, so each change is atomic.
class SoundStore(private val context: Context) {
    val library: Flow<SoundLibrary> = context.store.data.map(::decode)

    // Adds the sound. A taken name is ignored, so a double tap on Save cannot add a sound twice.
    suspend fun add(sound: Sound) = update { lib ->
        if (lib.isTaken(sound.name)) return@update lib
        lib.copy(sounds = lib.sounds + sound)
    }

    // Puts `sound` in place of the one named `old`. The caller checks the name.
    suspend fun replace(old: String, sound: Sound) = update { lib ->
        lib.copy(sounds = lib.sounds.map { if (it.name == old) sound else it })
    }

    // The last sound is never deleted.
    suspend fun delete(name: String) = update { lib ->
        if (lib.sounds.size == 1) return@update lib
        lib.copy(sounds = lib.sounds.filter { it.name != name })
    }

    private suspend fun update(change: (SoundLibrary) -> SoundLibrary) {
        context.store.edit { prefs -> prefs[LIBRARY] = json.encodeToString(change(decode(prefs))) }
    }
}

private fun decode(prefs: Preferences): SoundLibrary {
    val library = try {
        prefs[LIBRARY]?.let { json.decodeFromString<SoundLibrary>(it) }
    } catch (e: IllegalArgumentException) {
        null
    }
    return library?.takeIf { it.sounds.isNotEmpty() } ?: SoundLibrary(DefaultSounds)
}
