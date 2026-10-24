package com.example.gymlog.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Fires when the countdown's system alarm goes off. The process may have been
 * killed in the meantime — the alarm wakes it up, and all this has to do is
 * hand the alert to the notification manager.
 */
class TimerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_TIMER_FINISHED) return
        TimerAlerts.alert(context.applicationContext)
    }

    companion object {
        const val ACTION_TIMER_FINISHED = "com.example.gymlog.action.TIMER_FINISHED"
    }
}
