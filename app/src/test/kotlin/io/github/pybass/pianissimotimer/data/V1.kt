package io.github.pybass.pianissimotimer.data

import io.github.pybass.pianissimotimer.bell.Bell
import io.github.pybass.pianissimotimer.sound.DefaultSounds
import io.github.pybass.pianissimotimer.sound.Sound
import io.github.pybass.pianissimotimer.sound.SoundLibrary
import io.github.pybass.pianissimotimer.timer.Step
import io.github.pybass.pianissimotimer.timer.Timer
import io.github.pybass.pianissimotimer.timer.TimerLibrary
import io.github.pybass.pianissimotimer.timer.TimerState

// Saved data of version 1 as the user's phone holds it, next to the values it stands for. The JSON is written by
// hand, not by the code under test, so a test that compares the two fails when the format drifts without a bump of
// DATA_VERSION. A new version gets its own frozen JSON; the old one stays as the input of its upgrade test.
internal object V1 {
    val timers = TimerLibrary(
        timers = listOf(
            Timer(
                "Pomodoro",
                listOf(Step(seconds = 1500, sound = "Piano", volumeDb = 0), Step(seconds = 300, sound = "Bell", volumeDb = -6)),
            ),
            Timer("Timer", listOf(Step(seconds = 1500, sound = "Piano", volumeDb = 0))),
        ),
        selected = "Pomodoro",
    )
    const val TIMERS_JSON =
        """{"timers":[{"name":"Pomodoro","steps":[{"seconds":1500,"sound":"Piano","volumeDb":0},""" +
            """{"seconds":300,"sound":"Bell","volumeDb":-6}]},""" +
            """{"name":"Timer","steps":[{"seconds":1500,"sound":"Piano","volumeDb":0}]}],"selected":"Pomodoro"}"""

    val sounds = SoundLibrary(DefaultSounds)
    const val SOUNDS_JSON =
        """{"sounds":[{"name":"Piano","type":"PIANO","note":60,"rise":0.0,"length":5.0},""" +
            """{"name":"Bell","type":"BELL","note":72,"rise":0.0,"length":8.0},""" +
            """{"name":"Flute","type":"FLUTE","note":64,"rise":1.5,"length":4.0},""" +
            """{"name":"String","type":"STRING","note":55,"rise":0.0,"length":6.0}]}"""

    private val bells = listOf(Bell(due = 1762601500000, sound = Sound("Piano"), volumeDb = 0))
    private const val BELLS_JSON =
        """[{"due":1762601500000,"sound":{"name":"Piano","type":"PIANO","note":60,"rise":0.0,"length":5.0},"volumeDb":0}]"""

    val idle: TimerState = TimerState.Idle
    const val IDLE_JSON = """{"type":"idle"}"""

    val running: TimerState = TimerState.Running(bells, start = 1762600000000)
    const val RUNNING_JSON = """{"type":"running","bells":$BELLS_JSON,"start":1762600000000}"""

    val paused: TimerState = TimerState.Paused(bells, start = 1762600000000, at = 1762600600000)
    const val PAUSED_JSON = """{"type":"paused","bells":$BELLS_JSON,"start":1762600000000,"at":1762600600000}"""
}
