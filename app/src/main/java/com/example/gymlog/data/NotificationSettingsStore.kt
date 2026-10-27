package com.example.gymlog.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * App-level preference for the independent rest timer's finish alert.
 *
 * This is intentionally separate from the Android notification permission and
 * from vendor battery/background restrictions: it only controls whether this
 * app schedules the timer-finished alert at all.
 */
class NotificationSettingsStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val state = MutableStateFlow(prefs.getBoolean(KEY_COUNTDOWN_ALERTS, true))
    val countdownAlertsEnabled: StateFlow<Boolean> = state.asStateFlow()

    fun setCountdownAlertsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_COUNTDOWN_ALERTS, enabled).apply()
        state.value = enabled
    }

    companion object {
        const val PREFS_NAME = "gymlog_notifications"
        private const val KEY_COUNTDOWN_ALERTS = "countdown_alerts_enabled"
    }
}
