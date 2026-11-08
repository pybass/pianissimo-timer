package io.github.pybass.pianissimotimer.bell

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

// `bells` is one per step; the ones before `from` have rung already and are not scheduled. A bell replaces a scheduled
// one with the same index.
// Lint knows only SCHEDULE_EXACT_ALARM; the manifest's USE_EXACT_ALARM grants the same at install and cannot be revoked.
@SuppressLint("MissingPermission")
fun scheduleBells(context: Context, bells: List<Bell>, from: Int) {
    val alarms = context.getSystemService(AlarmManager::class.java)
    for (i in from..bells.lastIndex) {
        val intent = bellIntent(context, i, bells[i], nextDue = bells.getOrNull(i + 1)?.dueAt, stepsLeft = bells.lastIndex - i)
        alarms.setAlarmClock(AlarmManager.AlarmClockInfo(bells[i].dueAt, null), intent)
    }
    showWaitingNotification(context, bells[from].dueAt, stepsLeft = bells.size - from)
}

// Cancels the scheduled bells and silences one that is ringing. `cancelAll` drops every alarm of the app, so no bell
// stays, not even one the saved state does not know of (docs/spec.md 1.4).
fun cancelBells(context: Context) {
    context.getSystemService(AlarmManager::class.java).cancelAll()
    hideWaitingNotification(context)
    context.stopService(Intent(context, BellService::class.java))
}

// Cancels the scheduled bells but keeps a ringing one: pausing must not silence an earlier bell. `left` is in ms.
fun pauseBells(context: Context, left: Long, stepsLeft: Int) {
    context.getSystemService(AlarmManager::class.java).cancelAll()
    showPausedNotification(context, left, stepsLeft)
}

// False once Android has dropped the bell of step `index`: after a phone restart or Force stop, or some time after it
// rang. FLAG_NO_CREATE only looks up the PendingIntent; the mutability flag must match bellIntent's.
fun isBellScheduled(context: Context, index: Int) = PendingIntent.getForegroundService(
    context,
    index,
    Intent(context, BellService::class.java),
    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE,
) != null

// Equal intents with the same request code give the same PendingIntent, so the one built here replaces the bell of
// step `index`. Extras do not count for equality: UPDATE_CURRENT puts the new bell, next due and steps left in.
private fun bellIntent(context: Context, index: Int, bell: Bell, nextDue: Long?, stepsLeft: Int): PendingIntent {
    val intent = Intent(context, BellService::class.java)
    intent.putExtra(BELL, bell.toJson())
    if (nextDue != null) intent.putExtra(NEXT_DUE, nextDue)
    intent.putExtra(STEPS_LEFT, stepsLeft)
    val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    return PendingIntent.getForegroundService(context, index, intent, flags)
}
