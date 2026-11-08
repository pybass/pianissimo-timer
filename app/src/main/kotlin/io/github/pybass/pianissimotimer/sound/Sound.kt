package io.github.pybass.pianissimotimer.sound

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// How a sound is made. The tone and a short soft start belong to the type; the note, the rise and the length are
// settable. Saved as lowercase words, so a rename of a constant does not change the file.
// The names describe the character, not a real instrument: a synthesizer never becomes one.
@Serializable
enum class SoundType(val label: String) {
    @SerialName("bell")
    BELL("Bell"),

    @SerialName("piano")
    PIANO("Piano"),

    @SerialName("flute")
    FLUTE("Flute"),

    @SerialName("string")
    STRING("String"),
}

// A sound for the bell: a name and the settings of the synthesizer. Made to be gentle in silence: no strike, a soft
// start, nothing sharp. The defaults are the app's default sound. `id` never changes; the name may.
@Serializable
data class Sound(
    val id: String,
    val name: String,
    val type: SoundType = SoundType.PIANO,
    // MIDI note number; 60 is C4, 262 Hz.
    val note: Int = 60,
    // Seconds the loudness grows before the sound starts to fade; 0 leaves only the soft start of the type.
    val rise: Double = 0.0,
    // Seconds to fade by 60 dB.
    val length: Double = 5.0,
) {
    fun samples() = synthesize(type, note, rise, length)
}

// The sounds the library starts with, one per type, with fixed ids. Named after their types, as no sound is built in:
// like any sound, the user may rename, edit or delete them. The notes are G3, C4, E4 and C5, a C major chord, so the
// steps of one timer agree whatever their sounds. The bell is high and long to let its shimmer be heard; the flute
// swells like a breath, as its tone never changes; the string is low, where a pluck keeps its overtones.
val DefaultSounds = listOf(
    Sound("piano", "Piano"),
    Sound("bell", "Bell", SoundType.BELL, note = 72, length = 8.0),
    Sound("flute", "Flute", SoundType.FLUTE, note = 64, rise = 1.5, length = 4.0),
    Sound("string", "String", SoundType.STRING, note = 55, length = 6.0),
)

// What a bell plays when its own sound is missing, and the sound of a new timer in a new library.
val DefaultSound = DefaultSounds.first()

// A step names its sound by id, not a copy; a deleted sound leaves an id nobody has, which falls back to the first
// sound (docs/spec.md 5.2).
fun List<Sound>.soundFor(id: String) = firstOrNull { it.id == id } ?: first()
