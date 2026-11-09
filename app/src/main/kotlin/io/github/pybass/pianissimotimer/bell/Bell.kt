package io.github.pybass.pianissimotimer.bell

import io.github.pybass.pianissimotimer.data.json
import io.github.pybass.pianissimotimer.sound.Sound
import kotlinx.serialization.Serializable

// A bell as Android carries it: when it rings, in ms since the epoch, and what it plays. The sound is a copy, so the
// bell rings as it was at Start even if the library changes later (docs/spec.md 1.10). `volumeDb` is 0 or less.
@Serializable
data class Bell(val dueAt: Long, val sound: Sound, val volumeDb: Int) {
    fun toJson() = json.encodeToString(this)

    companion object {
        // An alarm from an older build may carry an unreadable bell; the service then plays its default sound.
        fun fromJson(text: String): Bell? = try {
            json.decodeFromString<Bell>(text)
        } catch (e: IllegalArgumentException) {
            null
        }
    }
}
