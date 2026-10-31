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
 *
 * **排时钟记下目标时刻**（`EXTRA_ALERT_AT`，elapsedRealtime 基准），接收器据此判断
 * 「是不是真的到点了」—— 因为备用闹钟在国产 ROM 上可能被提前投递（见 [TimerAlertGate]）。
 */
class TimerAlarmScheduler(
    private val appContext: Context,
    private val settings: NotificationSettingsStore,
) {
    private val alarmManager: AlarmManager? get() = appContext.getSystemService()

    /** Schedules the alert to fire after [remainingMillis]. Replaces any previous one. */
    fun scheduleCountdown(remainingMillis: Long, retried: Boolean = false) {
        cancel()
        val manager = alarmManager ?: return
        if (remainingMillis <= 0L) return
        // 关闭应用内开关后，不再为新的倒计时安排任何系统提醒。
        if (!settings.countdownAlertsEnabled.value) return
        // RTC 给系统用，elapsed 给接收器校验用：两者是同一次「剩余」的两种写法
        val triggerAt = System.currentTimeMillis() + remainingMillis
        val atElapsed = SystemClock.elapsedRealtime() + remainingMillis
        // 同一次倒计时的主/备闹钟共享 token；新倒计时会得到新 token，
        // 从而不会被 3 秒去重误伤。
        val token = SystemClock.elapsedRealtime()
        // 主路径：系统闹钟，标准 Android 上最准，也允许在深度 Doze 唤醒。
        runCatching {
            manager.setAlarmClock(
                AlarmManager.AlarmClockInfo(triggerAt, statusBarIntent()),
                alarmIntent(REQUEST_ALARM, token, atElapsed, retried),
            )
        }
        // 备用路径：部分 ROM（实测 OnePlus/ColorOS）会把 setAlarmClock
        // 整体调整到 +2d23h59m，但 setExactAndAllowWhileIdle 不一定被同样处理。
        //
        // **基准必须用 ELAPSED_REALTIME**：如果手机时钟偏了、之后又被 NTP 校正
        // （实测某机差近 5 小时），RTC 基准的闹钟会按「墙钟跳变」提前或推迟响，
        // 提前量正好等于跳变量——这正是「有时提前 29 秒、有时提前 35 秒」的来源。
        // 用 elapsedRealtime 后，这一路不受改时间影响；主路径（setAlarmClock 只收 RTC）
        // 万一被跳变带偏，由接收器的 [TimerAlertGate] 复核并补排。
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                manager.setExactAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    atElapsed,
                    alarmIntent(REQUEST_FALLBACK, token, atElapsed, retried),
                )
            } else {
                manager.setExact(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    atElapsed,
                    alarmIntent(REQUEST_FALLBACK, token, atElapsed, retried),
                )
            }
        }
    }

    fun cancel() {
        runCatching { alarmManager?.cancel(alarmIntent(REQUEST_ALARM, 0L, 0L, false)) }
        runCatching { alarmManager?.cancel(alarmIntent(REQUEST_FALLBACK, 0L, 0L, false)) }
    }

    /** Clears a delivered finish notification and any vibration still running. */
    fun cancelAlerts() {
        TimerAlerts.cancel(appContext)
    }

    fun canScheduleExactAlarms(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return alarmManager?.canScheduleExactAlarms() != false
    }

    private fun alarmIntent(
        requestCode: Int,
        token: Long,
        atElapsed: Long,
        retried: Boolean,
    ): PendingIntent {
        // 注意：extras 不参与 PendingIntent 的身份比较，cancel() 用 token=0 也能取消同一条。
        val intent = Intent(appContext, TimerAlarmReceiver::class.java)
            .setAction(TimerAlarmReceiver.ACTION_TIMER_FINISHED)
            .putExtra(TimerAlarmReceiver.EXTRA_ALERT_TOKEN, token)
            .putExtra(TimerAlarmReceiver.EXTRA_ALERT_AT, atElapsed)
            .putExtra(TimerAlarmReceiver.EXTRA_ALERT_RETRY, retried)
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
