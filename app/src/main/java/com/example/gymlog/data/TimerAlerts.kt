package com.example.gymlog.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import com.example.gymlog.R
import com.example.gymlog.MainActivity
import android.app.PendingIntent
import android.content.Intent

/**
 * The user-visible finish alert: a notification with vibration.
 *
 * [android.os.Vibrator.vibrate] is documented as foreground-only — the system
 * silently drops it for background apps unless a ringtone / notification /
 * alarm usage is declared on API 33+. A notification is delivered by the
 * system itself, so its vibration plays even when the app is backgrounded or
 * the screen is locked. The direct Vibrator call stays only as a fallback for
 * the case where the user denied the notification permission.
 */
object TimerAlerts {
    private const val CHANNEL_ID = "timer_finished"
    private const val NOTIFICATION_ID = 9401
    private val VIBRATION_PATTERN = longArrayOf(0, 600, 400, 600)

    fun alert(context: Context) {
        val appContext = context.applicationContext
        ensureChannel(appContext)
        if (canPostNotifications(appContext)) {
            runCatching {
                NotificationManagerCompat.from(appContext).notify(
                    NOTIFICATION_ID,
                    NotificationCompat.Builder(appContext, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_stat_timer)
                        .setContentTitle(appContext.getString(R.string.timer_finished_title))
                        .setContentText(appContext.getString(R.string.timer_finished_text))
                        .setCategory(NotificationCompat.CATEGORY_ALARM)
                        .setPriority(NotificationCompat.PRIORITY_MAX)
                        .setAutoCancel(true)
                        .setContentIntent(contentIntent(appContext))
                        .setVibrate(VIBRATION_PATTERN)
                        .build(),
                )
            }
        } else {
            vibrateDirectly(appContext)
        }
    }

    fun canPostNotifications(context: Context): Boolean {
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    /** Removes a finished-timer notification and stops any vibration still playing. */
    fun cancel(context: Context) {
        val appContext = context.applicationContext
        runCatching {
            NotificationManagerCompat.from(appContext).cancel(NOTIFICATION_ID)
        }
        runCatching {
            vibrator(appContext)?.cancel()
        }
    }

    private fun contentIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_IMMUTABLE
            } else {
                0
            }
        return PendingIntent.getActivity(context, 9403, intent, flags)
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService<NotificationManager>() ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.timer_finished_channel),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.timer_finished_text)
            enableVibration(true)
            vibrationPattern = VIBRATION_PATTERN
        }
        manager.createNotificationChannel(channel)
    }

    private fun vibrateDirectly(context: Context) {
        runCatching {
            val vibrator = vibrator(context) ?: return
            if (!vibrator.hasVibrator()) return
            vibrator.vibrate(VibrationEffect.createOneShot(600L, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    private fun vibrator(context: Context): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }
    }
}
