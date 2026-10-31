package com.example.gymlog.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock

/**
 * Fires when the countdown's system alarm goes off. The process may have been
 * killed in the meantime — the alarm wakes it up, and all this has to do is
 * hand the alert to the notification manager.
 *
 * **收到 ≠ 到点**：备用闹钟在国产 ROM 上会被按批处理窗口提前投递（实测提前约 29 秒），
 * 所以这里先用排闹钟时记下的目标时刻复核一遍；明显提前就只把闹钟重排到真正的结束时刻，
 * 不弹提醒（判定见 [TimerAlertGate]）。
 */
class TimerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_TIMER_FINISHED) return
        val appContext = context.applicationContext
        val token = intent.getLongExtra(EXTRA_ALERT_TOKEN, 0L)
        val atElapsed = intent.getLongExtra(EXTRA_ALERT_AT, 0L)
        val retried = intent.getBooleanExtra(EXTRA_ALERT_RETRY, false)
        val now = SystemClock.elapsedRealtime()
        val verdict = TimerAlertGate.evaluate(atElapsed, now, retried)
        if (!verdict.alert) {
            // 提前到达：不打扰用户，按真实剩余时间补排一次
            runCatching {
                TimerAlarmScheduler(appContext, NotificationSettingsStore(appContext))
                    .scheduleCountdown(verdict.rearmMillis, retried = true)
            }
            return
        }
        // 用户可能在闹钟已排队后关闭了应用内开关；旧的 PendingIntent
        // 仍然可能到达，这里再做一次最终拦截。
        if (!NotificationSettingsStore(appContext).countdownAlertsEnabled.value) return
        // 主闹钟和 OEM 备用闹钟共享 token；同一 token 只提醒一次。
        // 新倒计时会生成新 token，所以不会被上一条倒计时的去重误伤。
        if (!deduper.shouldAlert(token, now)) {
            return
        }
        TimerAlerts.alert(appContext)
    }

    companion object {
        const val ACTION_TIMER_FINISHED = "com.example.gymlog.action.TIMER_FINISHED"
        const val EXTRA_ALERT_TOKEN = "com.example.gymlog.extra.TIMER_ALERT_TOKEN"

        /** 排闹钟时记下的目标时刻（elapsedRealtime 基准），用来复核是否真的到点。 */
        const val EXTRA_ALERT_AT = "com.example.gymlog.extra.TIMER_ALERT_AT"

        /** 本次是不是「已经因为提前而补排过一次」。 */
        const val EXTRA_ALERT_RETRY = "com.example.gymlog.extra.TIMER_ALERT_RETRY"

        private val deduper = TimerAlertDeduper()
    }
}
