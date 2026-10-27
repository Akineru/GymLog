package com.example.gymlog.data

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * 只负责「让训练通知活着」的前台服务。
 *
 * 不持有业务状态：内容一律来自 [ActiveWorkoutNotifier]。
 * 进程被回收后系统重启本服务时 intent 为 null，此时由 notifier 读草稿快照复原并校验，
 * 校验不过就立即 stopSelf，避免留下属于已结束训练的残留通知。
 *
 * 服务类型用 `specialUse`（见 AndroidManifest）：本 App 不读取任何传感器，
 * `health` 要求的运行时权限与实际功能无关，因此不采用。
 *
 * **本服务不做任何定时刷新**：通知里两个时间都是系统 Chronometer（见
 * `notification_workout_content.xml`），由 SystemUI 自己走秒。
 * 早先的「每秒重绘」和后来的「到点闹钟 + 唤醒锁」都已移除 ——
 * 实测（OnePlus / Android 16）切后台 2 秒就会被 `OplusHansManager` 冻结整个进程，
 * 闹钟被 `OplusAlarmAdjustment` 推迟约 3 天，唤醒锁也拦不住。
 * 所以显示必须做到「不依赖 App 在零点醒来」：倒计时改成向上走的「已歇 x / 目标」，
 * 任何时刻都不会出现负数，也就不再需要到点唤醒。
 */
class WorkoutNotificationService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val state = if (intent?.action == ACTION_STOP) {
            null
        } else {
            ActiveWorkoutNotifier.resolveForService(this)
        }
        if (state == null) {
            // 没有有效的进行中训练（草稿已保存/已舍弃/被 tombstone 标记/只有历史记录）。
            //
            // 注意：这里不能直接 stopSelf()。若本服务是通过 Context.startForegroundService()
            // 拉起的，系统要求 5 秒内调用 startForeground()，**即使马上停止也一样**，
            // 否则抛 ForegroundServiceDidNotStartInTimeException 并杀掉整个进程。
            // 先走一次「挂前台 → 立即撤下」，满足契约后不留任何通知。
            ActiveWorkoutNotifier.fulfillForegroundStart(this)
            stopSelf()
            return START_NOT_STICKY
        }
        // startForegroundService 之后必须在 5 秒内 startForeground，这里紧跟着做
        val shown = ActiveWorkoutNotifier.startForeground(this, state)
        if (!shown) {
            stopSelf()
            return START_NOT_STICKY
        }
        // 尽力恢复：系统若愿意重启本服务，就按快照复原；不把它当成保证
        return START_STICKY
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        // 深浅色切换会让自绘视图的配色失效，重投一次
        ActiveWorkoutNotifier.postThemeRefresh(this)
    }

    override fun onDestroy() {
        ActiveWorkoutNotifier.onServiceDestroyed()
        super.onDestroy()
    }

    companion object {
        const val ACTION_SHOW = "com.example.gymlog.action.SHOW_WORKOUT_NOTIFICATION"
        const val ACTION_STOP = "com.example.gymlog.action.STOP_WORKOUT_NOTIFICATION"
    }
}
