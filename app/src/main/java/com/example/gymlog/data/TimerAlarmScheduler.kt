package com.example.gymlog.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.getSystemService
import com.example.gymlog.MainActivity

/**
 * Hands the countdown's finish moment to the system alarm clock.
 *
 * A coroutine cannot be trusted to still be alive when the app is in the
 * background: the process may be cached or killed, and Doze suspends the CPU.
 * [AlarmManager.setAlarmClock] keeps the delivery time exact even in low-power
 * modes and fires a broadcast that starts the app process again if needed.
 *
 * Requires USE_EXACT_ALARM (declared in the manifest; granted at install time
 * and not revocable by the user, which is what makes it the right choice over
 * SCHEDULE_EXACT_ALARM now that Android 14 denies the latter by default).
 */
class TimerAlarmScheduler(private val appContext: Context) {
    private val alarmManager: AlarmManager? get() = appContext.getSystemService()

    /** Schedules the alert to fire after [remainingMillis]. Replaces any previous one. */
    fun scheduleCountdown(remainingMillis: Long) {
        cancel()
        val manager = alarmManager ?: return
        if (remainingMillis <= 0L) return
        val triggerAt = System.currentTimeMillis() + remainingMillis
        runCatching {
            manager.setAlarmClock(
                AlarmManager.AlarmClockInfo(triggerAt, statusBarIntent()),
                alarmIntent(),
            )
        }
    }

    fun cancel() {
        runCatching { alarmManager?.cancel(alarmIntent()) }
    }

    /** Clears a delivered finish notification and any vibration still running. */
    fun cancelAlerts() {
        TimerAlerts.cancel(appContext)
    }

    fun canScheduleExactAlarms(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return alarmManager?.canScheduleExactAlarms() != false
    }

    private fun alarmIntent(): PendingIntent {
        val intent = Intent(appContext, TimerAlarmReceiver::class.java)
            .setAction(TimerAlarmReceiver.ACTION_TIMER_FINISHED)
        return PendingIntent.getBroadcast(appContext, REQUEST_ALARM, intent, pendingFlags())
    }

    private fun statusBarIntent(): PendingIntent {
        val intent = Intent(appContext, MainActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        return PendingIntent.getActivity(appContext, REQUEST_SHOW, intent, pendingFlags())
    }

    private fun pendingFlags(): Int {
        return PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_IMMUTABLE
            } else {
                0
            }
    }

    private companion object {
        const val REQUEST_ALARM = 9401
        const val REQUEST_SHOW = 9402
    }
}
