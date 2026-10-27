package com.example.gymlog.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock

/**
 * Fires when the countdown's system alarm goes off. The process may have been
 * killed in the meantime — the alarm wakes it up, and all this has to do is
 * hand the alert to the notification manager.
 */
class TimerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_TIMER_FINISHED) return
        val appContext = context.applicationContext
        // 用户可能在闹钟已排队后关闭了应用内开关；旧的 PendingIntent
        // 仍然可能到达，这里再做一次最终拦截。
        if (!NotificationSettingsStore(appContext).countdownAlertsEnabled.value) return
        val token = intent.getLongExtra(EXTRA_ALERT_TOKEN, 0L)
        // 主闹钟和 OEM 备用闹钟共享 token；同一 token 只提醒一次。
        // 新倒计时会生成新 token，所以不会被上一条倒计时的去重误伤。
        if (!deduper.shouldAlert(token, SystemClock.elapsedRealtime())) return
        TimerAlerts.alert(appContext)
    }

    companion object {
        const val ACTION_TIMER_FINISHED = "com.example.gymlog.action.TIMER_FINISHED"
        const val EXTRA_ALERT_TOKEN = "com.example.gymlog.extra.TIMER_ALERT_TOKEN"
        private val deduper = TimerAlertDeduper()
    }
}
