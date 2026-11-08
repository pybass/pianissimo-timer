package io.github.pybass.pianissimotimer.data

import android.content.Context
import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.pybass.pianissimotimer.bell.cancelBells

// Everything the app saves, in one file. One store per file in the whole app: a second DataStore on the same file
// throws. DataStore runs the migration once, before the first read or write sees the data.
internal val Context.appData by preferencesDataStore("pianissimo", produceMigrations = { listOf(Upgrade(it)) })

// The keys of the file. Each library and the state is one JSON string, so each change is atomic.
internal val TIMERS = stringPreferencesKey("timers")
internal val SOUNDS = stringPreferencesKey("sounds")
internal val STATE = stringPreferencesKey("state")
internal val VERSION = intPreferencesKey("version")

// Runs `upgrade` on the saved data and cancels the bells of a state it dropped: an Idle app must not ring.
private class Upgrade(private val context: Context) : DataMigration<Preferences> {
    // Every launch: the data is read back and checked even when its version is current.
    override suspend fun shouldMigrate(currentData: Preferences) = true

    override suspend fun migrate(currentData: Preferences): Preferences {
        val upgraded = upgrade(currentData)
        if (upgraded.stateDropped) cancelBells(context)
        return upgraded.data
    }

    override suspend fun cleanUp() = Unit
}
