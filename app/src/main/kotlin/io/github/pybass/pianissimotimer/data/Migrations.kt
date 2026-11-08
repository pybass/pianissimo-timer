package io.github.pybass.pianissimotimer.data

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import io.github.pybass.pianissimotimer.sound.json
import io.github.pybass.pianissimotimer.timer.TimerState

// The version of the saved format: the keys of the file and the JSON under them. Bump it with every change that the
// data of the version before cannot be read as, add that version's step to UPGRADE_STEPS, and a test that upgrades frozen
// data of the old version. The builds before version 1 saved into two other files, `timer` and `sounds`; those are
// never read and may still lie next to this file.
internal const val DATA_VERSION = 1

// A step turns the data of its version into the data of the next. A step converts only the libraries; the live
// state is dropped on every upgrade, as its bells carry copies of the sounds in the old form.
internal typealias UpgradeStep = (MutablePreferences) -> Unit

internal val UPGRADE_STEPS: Map<Int, UpgradeStep> = emptyMap()

internal class Upgraded(val data: Preferences, val stateDropped: Boolean)

// Brings `data` to `version`: the steps from its saved version run in order, then every value is read back and
// written in its current form, so a value that cannot be read gives way to its default. Data of an unknown version,
// or that a step cannot convert, is dropped as a whole: default timers beat a crash on every launch. `stateDropped`
// tells the caller to cancel the bells of the state that was lost; it is set on a file without a version too, as a
// build before version 1 may have left its bells with Android.
internal fun upgrade(data: Preferences, steps: Map<Int, UpgradeStep> = UPGRADE_STEPS, version: Int = DATA_VERSION): Upgraded {
    val prefs = data.toMutablePreferences()
    val saved = prefs[VERSION]
    if (saved != null && saved != version) {
        val converted = saved < version && runCatching { for (v in saved until version) steps.getValue(v)(prefs) }.isSuccess
        if (!converted) prefs.clear()
        prefs.remove(STATE)
    }
    val state = decodeState(prefs)
    val stateDropped = saved != version || state == null
    prefs[TIMERS] = json.encodeToString(decodeTimers(prefs))
    prefs[SOUNDS] = json.encodeToString(decodeSounds(prefs))
    prefs[STATE] = json.encodeToString<TimerState>(state ?: TimerState.Idle)
    prefs[VERSION] = version
    return Upgraded(prefs, stateDropped)
}
