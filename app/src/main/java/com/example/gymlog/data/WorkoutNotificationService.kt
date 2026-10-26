package com.example.gymlog.data

import android.app.Service
import android.content.Intent
import android.os.IBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 只负责「让训练通知活着」的前台服务。
 *
 * 不持有业务状态：内容一律来自 [ActiveWorkoutNotifier]。
 * 进程被回收后系统重启本服务时 intent 为 null，此时由 notifier 读草稿快照复原并校验，
 * 校验不过就立即 stopSelf，避免留下属于已结束训练的残留通知。
 *
 * 服务类型用 `specialUse`（见 AndroidManifest）：本 App 不读取任何传感器，
 * `health` 要求的运行时权限与实际功能无关，因此不采用。
 */
class WorkoutNotificationService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var clockTicker: Job? = null

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
        observeClock(state.clock?.running == true)
        // 尽力恢复：系统若愿意重启本服务，就按快照复原；不把它当成保证
        return START_STICKY
    }

    /**
     * 独立计时器在跑时，每秒按锚点重绘一次那一行文字。
     *
     * 这只做「显示更新」：时间值由 [WorkoutNotificationClock] 用与
     * `SessionEditorViewModel.tickClock()` 相同的公式算出来，不产生第二套计时。
     * 计时器停下 / 归零 / 进入保存页时循环自动退出，平时一秒钟也不会额外唤醒。
     */
    private fun observeClock(initialRunning: Boolean) {
        var wasRunning = initialRunning
        if (wasRunning) startTicker()
        scope.launch {
            ActiveWorkoutNotifier.state.collect { state ->
                val running = state != null &&
                    state.phase == WorkoutNotificationPhase.RUNNING &&
                    state.clock?.running == true
                if (running && !wasRunning) startTicker()
                if (!running && wasRunning) stopTicker()
                wasRunning = running
            }
        }
    }

    private fun startTicker() {
        if (clockTicker?.isActive == true) return
        clockTicker = scope.launch {
            while (isActive) {
                delay(1_000)
                if (!ActiveWorkoutNotifier.tickClockText(this@WorkoutNotificationService)) break
            }
        }
    }

    private fun stopTicker() {
        clockTicker?.cancel()
        clockTicker = null
    }

    override fun onDestroy() {
        stopTicker()
        scope.cancel()
        ActiveWorkoutNotifier.onServiceDestroyed()
        super.onDestroy()
    }

    companion object {
        const val ACTION_SHOW = "com.example.gymlog.action.SHOW_WORKOUT_NOTIFICATION"
        const val ACTION_STOP = "com.example.gymlog.action.STOP_WORKOUT_NOTIFICATION"
    }
}
