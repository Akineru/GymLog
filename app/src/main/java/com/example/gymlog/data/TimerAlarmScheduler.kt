package com.example.gymlog.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
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
class TimerAlarmScheduler(
    private val appContext: Context,
    private val settings: NotificationSettingsStore,
) {
    private val alarmManager: AlarmManager? get() = appContext.getSystemService()

    /** Schedules the alert to fire after [remainingMillis]. Replaces any previous one. */
    fun scheduleCountdown(remainingMillis: Long) {
        cancel()
        val manager = alarmManager ?: return
        if (remainingMillis <= 0L) return
        // 关闭应用内开关后，不再为新的倒计时安排任何系统提醒。
        if (!settings.countdownAlertsEnabled.value) return
        val triggerAt = System.currentTimeMillis() + remainingMillis
        // 同一次倒计时的主/备闹钟共享 token；新倒计时会得到新 token，
        // 从而不会被 3 秒去重误伤。
        val token = SystemClock.elapsedRealtime()
        // 主路径：系统闹钟，标准 Android 上最准，也允许在深度 Doze 唤醒。
        runCatching {
            manager.setAlarmClock(
                AlarmManager.AlarmClockInfo(triggerAt, statusBarIntent()),
                alarmIntent(REQUEST_ALARM, token),
            )
        }
        // 备用路径：部分 ROM（实测 OnePlus/ColorOS）会把 setAlarmClock
        // 整体调整到 +2d23h59m，但 setExactAndAllowWhileIdle 不一定被同样处理。
        // 两个 PendingIntent 的 requestCode 不同，互不覆盖；Receiver 会去重。
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                manager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    alarmIntent(REQUEST_FALLBACK, token),
                )
            } else {
                manager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    alarmIntent(REQUEST_FALLBACK, token),
                )
            }
        }
    }

    fun cancel() {
        runCatching { alarmManager?.cancel(alarmIntent(REQUEST_ALARM, 0L)) }
        runCatching { alarmManager?.cancel(alarmIntent(REQUEST_FALLBACK, 0L)) }
    }

    /** Clears a delivered finish notification and any vibration still running. */
    fun cancelAlerts() {
        TimerAlerts.cancel(appContext)
    }

    fun canScheduleExactAlarms(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return alarmManager?.canScheduleExactAlarms() != false
    }

    private fun alarmIntent(requestCode: Int, token: Long): PendingIntent {
        val intent = Intent(appContext, TimerAlarmReceiver::class.java)
            .setAction(TimerAlarmReceiver.ACTION_TIMER_FINISHED)
            .putExtra(TimerAlarmReceiver.EXTRA_ALERT_TOKEN, token)
        return PendingIntent.getBroadcast(appContext, requestCode, intent, pendingFlags())
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
        const val REQUEST_FALLBACK = 9404
    }
}
