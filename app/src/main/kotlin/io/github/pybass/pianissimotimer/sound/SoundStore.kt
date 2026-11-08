package io.github.pybass.pianissimotimer.sound

import android.content.Context
import io.github.pybass.pianissimotimer.data.AppData
import io.github.pybass.pianissimotimer.data.appData

// The only place that changes the saved sounds.
class SoundStore(private val context: Context) {
    // Adds the sound. A taken name is ignored, so a double tap on Save cannot add a sound twice.
    suspend fun add(sound: Sound) = update { data ->
        if (data.isSoundNameTaken(sound.name)) return@update data
        data.copy(sounds = data.sounds + sound)
    }

    // Puts `sound` in place of the one with the same id. The caller checks the name.
    suspend fun replace(sound: Sound) = update { data ->
        data.copy(sounds = data.sounds.map { if (it.id == sound.id) sound else it })
    }

    // The last sound is never deleted.
    suspend fun delete(id: String) = update { data ->
        if (data.sounds.size == 1) return@update data
        data.copy(sounds = data.sounds.filter { it.id != id })
    }

    private suspend fun update(change: (AppData) -> AppData) {
        context.appData.updateData { change(it) }
    }
}
