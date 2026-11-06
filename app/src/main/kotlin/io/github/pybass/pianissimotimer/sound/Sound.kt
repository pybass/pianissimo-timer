package io.github.pybass.pianissimotimer.sound

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// The JSON of the saved library and of the sound a bell carries.
// encodeDefaults: without it a field equal to its default is left out, and a later change of a default would silently
// change the saved sounds. ignoreUnknownKeys: JSON saved before a field was removed still reads.
internal val json = Json {
    encodeDefaults = true
    ignoreUnknownKeys = true
}

// How a sound is made. The tone and a short soft start belong to the type; the note, the rise and the length are
// settable.
// The names describe the character, not a real instrument: a synthesizer never becomes one.
@Serializable
enum class SoundType(val label: String) {
    BELL("Bell"),
    PIANO("Piano"),
    FLUTE("Flute"),
    STRING("String"),
}

// A sound for the bell: a name and the settings of the synthesizer. Made to be gentle in silence: no strike, a soft
// start, nothing sharp. The defaults are the app's default sound.
@Serializable
data class Sound(
    val name: String,
    val type: SoundType = SoundType.PIANO,
    // MIDI note number; 60 is C4, 262 Hz.
    val note: Int = 60,
    // Seconds the loudness grows before the sound starts to fade; 0 leaves only the soft start of the type.
    val rise: Double = 0.0,
    // Seconds to fade by 60 dB.
    val length: Double = 5.0,
) {
    fun toJson() = json.encodeToString(this)

    fun samples() = synthesize(type, note, rise, length)

    companion object {
        fun fromJson(text: String) = json.decodeFromString<Sound>(text)
    }
}

// The sounds the library starts with, one per type. Named after their types, as no sound is built in: like any
// sound, the user may rename, edit or delete them. The notes are G3, C4, E4 and C5, a C major chord, so the steps of
// one timer agree whatever their sounds. The bell is high and long to let its shimmer be heard; the flute swells like
// a breath, as its tone never changes; the string is low, where a pluck keeps its overtones.
val DefaultSounds = listOf(
    Sound("Piano"),
    Sound("Bell", SoundType.BELL, note = 72, length = 8.0),
    Sound("Flute", SoundType.FLUTE, note = 64, rise = 1.5, length = 4.0),
    Sound("String", SoundType.STRING, note = 55, length = 6.0),
)

// What a bell plays when its own sound is missing, and the sound of a new timer in a new library.
val DefaultSound = DefaultSounds.first()

// The sounds to choose from. It always holds at least one sound, and no two names differ only in case.
@Serializable
data class SoundLibrary(val sounds: List<Sound>) {
    // A step names its sound; a renamed or deleted sound leaves a name nobody has, which falls back to the first
    // sound (docs/spec.md 5.2).
    fun soundFor(name: String) = sounds.firstOrNull { it.name == name } ?: sounds.first()

    fun isTaken(name: String) = sounds.any { it.name.equals(name, ignoreCase = true) }

    // "Sound", then "Sound 2" and on, skipping taken names, like timers.
    fun newName() = generateSequence(1) { it + 1 }.map { if (it == 1) "Sound" else "Sound $it" }.first { !isTaken(it) }
}
