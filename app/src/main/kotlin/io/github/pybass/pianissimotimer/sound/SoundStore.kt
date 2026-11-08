package io.github.pybass.pianissimotimer.sound

import android.content.Context
import androidx.datastore.preferences.core.edit
import io.github.pybass.pianissimotimer.data.SOUNDS
import io.github.pybass.pianissimotimer.data.appData
import io.github.pybass.pianissimotimer.data.decodeSounds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// The only place that changes the saved sounds.
class SoundStore(private val context: Context) {
    val library: Flow<SoundLibrary> = context.appData.data.map(::decodeSounds)

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
        context.appData.edit { prefs -> prefs[SOUNDS] = json.encodeToString(change(decodeSounds(prefs))) }
    }
}
