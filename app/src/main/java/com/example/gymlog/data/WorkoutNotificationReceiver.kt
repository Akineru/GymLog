package com.example.gymlog.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * 只处理「用户把训练通知划掉」这一件事。
 *
 * Android 14 起用户可以把前台服务通知划掉，这里仅做记录：
 * 服务继续跑，通知不强推回去，等下一次真实状态变化（暂停/继续/换动作/进入保存页）再显示。
 */
class WorkoutNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == ACTION_DISMISSED) {
            ActiveWorkoutNotifier.onNotificationDismissed()
        }
    }

    companion object {
        const val ACTION_DISMISSED = "com.example.gymlog.action.WORKOUT_NOTIFICATION_DISMISSED"
    }
}
