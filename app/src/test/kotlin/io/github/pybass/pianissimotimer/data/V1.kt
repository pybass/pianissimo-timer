package io.github.pybass.pianissimotimer.data

import io.github.pybass.pianissimotimer.bell.Bell
import io.github.pybass.pianissimotimer.sound.Sound
import io.github.pybass.pianissimotimer.sound.SoundType
import io.github.pybass.pianissimotimer.timer.Step
import io.github.pybass.pianissimotimer.timer.Timer
import io.github.pybass.pianissimotimer.timer.TimerState

// Saved data of version 1 as the user's phone holds it, next to the values it stands for: the example of docs/data.md,
// compacted. The JSON is written by hand, not by the code under test, so a test that compares the two fails when the
// format drifts without a bump of FORMAT_VERSION. A new version gets its own frozen JSON; the old one stays as the
// input of its migration test.
internal object V1 {
    private const val POMODORO_ID = "7a1c2e40-9d7b-4a3f-8e0c-2b6f1d9a5c11"

    private const val PIANO_JSON =
        """{"id":"piano","name":"Piano","type":"piano","note":60,"fadeInSeconds":0.0,"lengthSeconds":5.0}"""
    private const val BELL_JSON =
        """{"id":"bell","name":"Bell","type":"bell","note":72,"fadeInSeconds":0.0,"lengthSeconds":8.0}"""
    private const val SOUNDS_JSON = """[$PIANO_JSON,$BELL_JSON,""" +
        """{"id":"flute","name":"Flute","type":"flute","note":64,"fadeInSeconds":1.5,"lengthSeconds":4.0},""" +
        """{"id":"string","name":"String","type":"string","note":55,"fadeInSeconds":0.0,"lengthSeconds":6.0}]"""

    private const val TIMERS_JSON = """[{"id":"$POMODORO_ID","name":"Pomodoro","steps":[""" +
        """{"seconds":1500,"soundId":"piano","volumeDb":0},{"seconds":300,"soundId":"bell","volumeDb":-6}]},""" +
        """{"id":"timer","name":"Timer","steps":[{"seconds":1500,"soundId":"piano","volumeDb":0}]}]"""

    // Every field written out, not `DefaultSounds` or a constructor default: a changed default is not a changed format.
    private val piano = Sound("piano", "Piano", SoundType.PIANO, note = 60, fadeInSeconds = 0.0, lengthSeconds = 5.0)
    private val bell = Sound("bell", "Bell", SoundType.BELL, note = 72, fadeInSeconds = 0.0, lengthSeconds = 8.0)
    private val sounds = listOf(
        piano,
        bell,
        Sound("flute", "Flute", SoundType.FLUTE, note = 64, fadeInSeconds = 1.5, lengthSeconds = 4.0),
        Sound("string", "String", SoundType.STRING, note = 55, fadeInSeconds = 0.0, lengthSeconds = 6.0),
    )

    private val bells = listOf(
        Bell(dueAt = 1762601500000, sound = piano, volumeDb = 0),
        Bell(dueAt = 1762601800000, sound = bell, volumeDb = -6),
    )
    private const val BELLS_JSON = """[{"dueAt":1762601500000,"sound":$PIANO_JSON,"volumeDb":0},""" +
        """{"dueAt":1762601800000,"sound":$BELL_JSON,"volumeDb":-6}]"""

    val idle: TimerState = TimerState.Idle
    const val IDLE_JSON = """{"type":"idle"}"""

    val running: TimerState = TimerState.Running(startedAt = 1762600000000, bells = bells)
    const val RUNNING_JSON = """{"type":"running","startedAt":1762600000000,"bells":$BELLS_JSON}"""

    val paused: TimerState = TimerState.Paused(startedAt = 1762600000000, pausedAt = 1762600600000, bells = bells)
    const val PAUSED_JSON = """{"type":"paused","startedAt":1762600000000,"pausedAt":1762600600000,"bells":$BELLS_JSON}"""

    val data = AppData(
        version = 1,
        selectedTimerId = POMODORO_ID,
        timers = listOf(
            Timer(
                POMODORO_ID,
                "Pomodoro",
                listOf(
                    Step(seconds = 1500, soundId = "piano", volumeDb = 0),
                    Step(seconds = 300, soundId = "bell", volumeDb = -6),
                ),
            ),
            Timer("timer", "Timer", listOf(Step(seconds = 1500, soundId = "piano", volumeDb = 0))),
        ),
        sounds = sounds,
        state = paused,
    )
    const val JSON = """{"version":1,"selectedTimerId":"$POMODORO_ID","timers":$TIMERS_JSON,""" +
        """"sounds":$SOUNDS_JSON,"state":$PAUSED_JSON}"""
}
