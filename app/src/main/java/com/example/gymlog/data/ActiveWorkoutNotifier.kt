package com.example.gymlog.data

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.os.Build
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.gymlog.MainActivity
import com.example.gymlog.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 「正在训练」通知的唯一出口。
 *
 * 设计要点：
 * 1. 时间只有 `SessionEditorViewModel` 一个来源。这里只保存发布瞬间的 (累计时长, 单调时钟)，
 *    运行中由系统 chronometer 接着走，[WorkoutNotificationState.elapsedAt] 用与
 *    `totalElapsed()` 相同的算法补齐，绝不另开一套计时。
 * 2. [publish] 只在状态真正变化时重新投递（[WorkoutNotificationState.signature]），
 *    运行中不随每秒走动重发，避免后台唤醒。
 * 3. 用户把通知划掉（Android 14+ 允许）后不立即重发；等下一次真实状态变化
 *    （暂停/继续/换动作/进入保存页）再显示，尊重用户意图。
 * 4. 清理时写一条 tombstone，服务被系统重启后不会把已结束的训练重新变成通知。
 */
object ActiveWorkoutNotifier {
    const val CHANNEL_ID = "workout_active"
    const val NOTIFICATION_ID = 9420
    private const val TAG = "ActiveWorkoutNotifier"

    private const val PREFS = "workout_notification"
    private const val KEY_TOMBSTONE = "tombstone_draft_key"

    private const val REQUEST_OPEN = 9421
    private const val REQUEST_DISMISS = 9422

    private val _state = MutableStateFlow<WorkoutNotificationState?>(null)
    val state: StateFlow<WorkoutNotificationState?> = _state.asStateFlow()

    /** 服务当前是否处于前台态（用于决定「重新 startForegroundService」还是「直接 notify」）。 */
    private var serviceForeground = false

    /** 用户划掉过通知：在下次状态变化前不再显示。 */
    private var dismissedByUser = false

    /** 上一次真正投递出去的「渲染内容」指纹，用来避免 UI 与服务每秒重绘重复投递。 */
    private var lastPostedKey: String? = null

    // ---- 由界面侧调用 ----------------------------------------------------

    /**
     * 发布当前训练状态。
     *
     * 只有「正在进行的**新**训练草稿」才允许出现通知：`sessionId != null`
     * 意味着这是已保存记录（或编辑历史记录），一律清理，避免出现假通知。
     */
    fun publish(
        context: Context,
        draftKey: String?,
        sessionId: String?,
        phase: WorkoutNotificationPhase,
        elapsedMillis: Long,
        exerciseName: String?,
        clock: WorkoutNotificationClock? = null,
    ) {
        if (draftKey.isNullOrBlank() || sessionId != null) {
            clear(context, draftKey)
            return
        }
        val app = context.applicationContext
        val next = WorkoutNotificationState(
            draftKey = draftKey,
            phase = phase,
            elapsedMillis = elapsedMillis.coerceAtLeast(0L),
            exerciseName = exerciseName?.takeIf { it.isNotBlank() },
            anchoredAtElapsedRealtime = SystemClock.elapsedRealtime(),
            clock = clock,
        )
        val previous = _state.value
        // 「结构变化」= 新的一场训练 / 相位切换 / 换动作 / 计时器启停或重置。
        // 运行中的秒数不算结构变化，否则用户划掉的通知会被每秒弹回来。
        val structuralChanged = previous == null || previous.signature != next.signature
        _state.value = next
        if (structuralChanged) dismissedByUser = false
        if (dismissedByUser) return
        if (serviceForeground) post(app, next, force = structuralChanged, tag = "publish") else startService(app)
    }

    /**
     * 服务每秒调用一次：只按锚点重绘独立计时器那一行，**不修改任何业务状态**。
     *
     * 返回 false 表示不需要再 tick（计时器已停 / 已归零 / 相位不是 RUNNING）。
     * 内容没变时 [post] 会自动跳过，所以不会重复打扰。
     */
    /**
     * 倒计时到点时由服务调用一次：把「组歇 · 00:00」换成「组歇 · 00:00 · 已结束」。
     *
     * 只重投一次，不写任何业务状态；数字本身由系统 Chronometer 渲染，不依赖这次调用。
     */
    fun postClockEnd(context: Context) {
        val current = _state.value ?: return
        if (!serviceForeground) return
        post(context.applicationContext, current, force = false, tag = "clockEnd")
    }

    /** 训练结束（保存成功 / 舍弃 / 删除）后调用，彻底清理。 */
    fun clear(context: Context, draftKey: String?) {
        val app = context.applicationContext
        val current = _state.value
        // 不同草稿的延迟清理请求不能清掉新训练的通知
        if (current != null && draftKey != null && current.draftKey != draftKey) return
        if (current != null && draftKey != null) rememberTombstone(app, draftKey)
        _state.value = null
        dismissedByUser = false
        serviceForeground = false
        lastPostedKey = null
        runCatching { NotificationManagerCompat.from(app).cancel(NOTIFICATION_ID) }
        runCatching { app.stopService(Intent(app, WorkoutNotificationService::class.java)) }
    }

    // ---- 由服务调用 ------------------------------------------------------

    /**
     * 服务启动/重启时拿「应当显示的状态」。
     *
     * 优先用内存里的最新状态；内存为空说明进程被回收过，此时读草稿快照复原。
     * 复原前逐项校验，任何一项不成立都返回 null（服务随即自杀，不留下残留通知）。
     */
    fun resolveForService(context: Context): WorkoutNotificationState? {
        _state.value?.let { return it }
        val snapshot = runCatching { SessionDraftStore(context.filesDir).load() }.getOrNull() ?: return null
        // 编辑已保存记录：不当成「正在训练」
        if (snapshot.sessionId != null) return null
        if (isTombstoned(context, snapshot.draftKey)) return null
        if (!snapshot.timerRunning && !snapshot.finishing) return null
        val phase = if (snapshot.finishing) {
            WorkoutNotificationPhase.FINISHING
        } else {
            WorkoutNotificationPhase.RUNNING
        }
        val elapsed = if (snapshot.finishing) {
            snapshot.finishedDurationMillis
        } else {
            val gap = (System.currentTimeMillis() - snapshot.savedAtEpochMillis).coerceAtLeast(0L)
            snapshot.totalElapsedMillis + if (snapshot.timerRunning) gap else 0L
        }
        val restored = WorkoutNotificationState(
            draftKey = snapshot.draftKey,
            phase = phase,
            elapsedMillis = elapsed,
            exerciseName = null,
            anchoredAtElapsedRealtime = SystemClock.elapsedRealtime(),
        )
        // 复原出来的状态也留在内存里，避免后续每次 onStartCommand 都读盘
        _state.value = restored
        return restored
    }

    /** 在服务里真正把通知挂到前台。失败（例如后台启动被拒）返回 false，由服务自行停止。 */
    fun startForeground(service: Service, state: WorkoutNotificationState): Boolean {
        val notification = build(service, state)
        val ok = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                service.startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
                )
            } else {
                service.startForeground(NOTIFICATION_ID, notification)
            }
            true
        }.onFailure { Log.w(TAG, "startForeground failed", it) }.getOrDefault(false)
        serviceForeground = ok
        return ok
    }

    fun stopForeground(service: Service) {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                service.stopForeground(Service.STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                service.stopForeground(true)
            }
        }
        serviceForeground = false
    }

    /** 用户从通知栏划掉。不重发，等服务下一次状态变化。 */
    fun onNotificationDismissed() {
        dismissedByUser = true
    }

    /**
     * 回到前台时补投一次。
     *
     * 典型场景：训练进行中，用户去系统设置里把通知权限打开再回来。此时 Compose 状态没有变化，
     * 不会触发 [publish]；而权限被拒期间投递的通知从未真正显示过，系统也不会替我们补上。
     * 用户主动划掉过的通知不会因此被强行拉回（[dismissedByUser] 会跳过）。
     */
    fun refresh(context: Context) {
        val current = _state.value ?: return
        if (dismissedByUser) return
        val app = context.applicationContext
        if (!canPost(app)) return
        if (serviceForeground) post(app, current, force = false, tag = "refresh") else startService(app)
    }

    /**
     * 满足 `startForegroundService()` 的 5 秒契约后立刻撤下。
     *
     * 服务被拉起但当前没有有效训练时（已保存/已舍弃的竞态），必须走这一步：
     * 直接 stopSelf() 会让系统抛 ForegroundServiceDidNotStartInTimeException 杀掉进程。
     * 占位通知在同一帧内被移除，正常情况下用户看不到。
     */
    fun fulfillForegroundStart(service: Service) {
        runCatching {
            ensureChannel(service)
            val placeholder = NotificationCompat.Builder(service, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_timer)
                .setContentTitle(service.getString(R.string.app_name))
                .setSilent(true)
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                service.startForeground(
                    NOTIFICATION_ID,
                    placeholder,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
                )
            } else {
                service.startForeground(NOTIFICATION_ID, placeholder)
            }
            service.stopForeground(Service.STOP_FOREGROUND_REMOVE)
        }.onFailure { Log.w(TAG, "fulfillForegroundStart failed", it) }
        serviceForeground = false
    }

    fun onServiceDestroyed() {
        serviceForeground = false
    }

    // ---- 内部实现 -------------------------------------------------------

    private fun startService(context: Context) {
        val intent = Intent(context, WorkoutNotificationService::class.java)
            .setAction(WorkoutNotificationService.ACTION_SHOW)
        runCatching { ContextCompat.startForegroundService(context, intent) }
            .onFailure { Log.w(TAG, "startForegroundService failed", it) }
    }

    private fun post(
        context: Context,
        state: WorkoutNotificationState,
        force: Boolean,
        tag: String = "",
    ): String {
        if (!canPost(context)) return "skip:notifications-disabled".also {
        }
        val now = SystemClock.elapsedRealtime()
        val key = contentKey(context, state, now)
        // UI 每秒推一次、服务每秒重绘一次，这里统一按「渲染出来的内容」去重；
        // 同时挡住「用户划掉后又把通知弹回来」——独立计时器每秒刷新也必须走这道闸门。
        if (!NotificationPostPolicy.shouldPost(dismissedByUser, force, key, lastPostedKey)) {
            val reason = if (dismissedByUser) "dismissed" else "same-key"
            return "skip:$reason"
        }
        val result = runCatching {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, build(context, state, now))
            lastPostedKey = key
        }
        return result.fold(
            onSuccess = {
                "posted"
            },
            onFailure = { error ->
                Log.w(TAG, "notify failed id=$NOTIFICATION_ID", error)
                "failed:${error.javaClass.simpleName}"
            },
        )
    }

    /** 渲染指纹：只有「真正显示出来的文字」变了，才值得重新投递。 */
    /** 通知文字颜色：跟随系统夜间模式（与通知栏底色一致），不跟 App 内的主题设置。 */
    private fun notificationInk(context: Context): Pair<Int, Int> {
        val night = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        return if (night) {
            0xFFFFFFFF.toInt() to 0xFFB3FFFFFF.toInt()
        } else {
            0xDE000000.toInt() to 0x99000000.toInt()
        }
    }

    /** 深浅色切换后重投一次，让自绘视图的配色跟上。 */
    fun postThemeRefresh(context: Context) {
        val current = _state.value ?: return
        if (!serviceForeground) return
        post(context.applicationContext, current, force = true, tag = "theme")
    }

    private fun contentKey(context: Context, state: WorkoutNotificationState, now: Long): String {
        val clock = clockText(context, state, now).orEmpty()
        val action = state.exerciseName.orEmpty()
        return "${state.signature}|$action|$clock"
    }

    private fun canPost(context: Context): Boolean =
        runCatching { NotificationManagerCompat.from(context).areNotificationsEnabled() }.getOrDefault(true)

    private fun build(
        context: Context,
        state: WorkoutNotificationState,
        now: Long = SystemClock.elapsedRealtime(),
    ): Notification {
        ensureChannel(context)
        val running = state.phase == WorkoutNotificationPhase.RUNNING
        val elapsed = state.elapsedAt(now)
        val duration = durationText(context, elapsed)
        val name = state.exerciseName
        val title = when (state.phase) {
            WorkoutNotificationPhase.FINISHING -> context.getString(R.string.workout_notification_title_pending)
            else -> context.getString(R.string.workout_notification_title_running)
        }
        val actionText = when (state.phase) {
            WorkoutNotificationPhase.FINISHING ->
                context.getString(R.string.workout_notification_pending_text, duration)
            WorkoutNotificationPhase.PAUSED -> if (name != null) {
                context.getString(R.string.workout_notification_paused_named, duration, name)
            } else {
                context.getString(R.string.workout_notification_paused, duration)
            }
            WorkoutNotificationPhase.RUNNING -> name?.let {
                context.getString(R.string.workout_notification_running_named, it)
            } ?: context.getString(R.string.workout_notification_running)
        }
        // 训练已结束待保存时不再展示独立计时器：那一刻它没有指导意义
        val display = if (state.phase == WorkoutNotificationPhase.FINISHING) {
            null
        } else {
            state.clock?.resolve(now)
        }
        val clockLine = if (state.phase == WorkoutNotificationPhase.FINISHING) {
            null
        } else {
            clockText(context, state, now)
        }

        val content = RemoteViews(context.packageName, R.layout.notification_workout_content)
        // RemoteViews 用本 App 的主题充气，?attr/textColorPrimary 不会跟随通知栏的深浅色，
        // 这里按系统夜间模式显式上色，保证深色通知栏里也是白字。
        val (primaryInk, secondaryInk) = notificationInk(context)
        content.setTextColor(R.id.notification_title, primaryInk)
        content.setTextColor(R.id.notification_total, secondaryInk)
        content.setTextColor(R.id.notification_action, secondaryInk)
        content.setTextColor(R.id.notification_clock_label, primaryInk)
        content.setTextColor(R.id.notification_clock_chronometer, primaryInk)
        content.setTextColor(R.id.notification_clock_text, primaryInk)
        content.setTextViewText(R.id.notification_title, title)
        // 训练总时长：运行中由系统走秒；暂停 / 待保存用同一基准冻结在一处
        content.setChronometerCountDown(R.id.notification_total, false)
        content.setChronometer(
            R.id.notification_total,
            SystemClock.elapsedRealtime() - elapsed,
            null,
            running,
        )
        content.setTextViewText(R.id.notification_action, actionText)

        if (display != null && display.state == WorkoutClockDisplayState.RUNNING) {
            val countdown = display.kind == WorkoutClockKind.COUNTDOWN
            content.setTextViewText(
                R.id.notification_clock_label,
                context.getString(
                    if (countdown) {
                        R.string.workout_notification_clock_countdown_label
                    } else {
                        R.string.workout_notification_clock_stopwatch_label
                    },
                ),
            )
            content.setViewVisibility(R.id.notification_clock_label, View.VISIBLE)
            content.setViewVisibility(R.id.notification_clock_text, View.GONE)
            content.setViewVisibility(R.id.notification_clock_chronometer, View.VISIBLE)
            // 倒计时多一个「/ 02:00」的目标，让「已歇 00:35」读得出进度
            if (countdown) {
                content.setTextViewText(
                    R.id.notification_clock_suffix,
                    context.getString(
                        R.string.workout_notification_clock_countdown_suffix,
                        WorkoutClockLogic.formatClock(display.durationMillis),
                    ),
                )
                content.setViewVisibility(R.id.notification_clock_suffix, View.VISIBLE)
            } else {
                content.setViewVisibility(R.id.notification_clock_suffix, View.GONE)
            }
            val spec = display.chronometerSpec(SystemClock.elapsedRealtime())
            if (spec != null) {
                content.setChronometerCountDown(R.id.notification_clock_chronometer, spec.countDown)
                content.setChronometer(
                    R.id.notification_clock_chronometer,
                    spec.baseElapsedRealtime,
                    null,
                    true,
                )
            }
        } else if (clockLine != null) {
            // 暂停 / 已到点：显示冻结文本，不走秒
            content.setViewVisibility(R.id.notification_clock_label, View.GONE)
            content.setViewVisibility(R.id.notification_clock_chronometer, View.GONE)
            content.setViewVisibility(R.id.notification_clock_suffix, View.GONE)
            content.setViewVisibility(R.id.notification_clock_text, View.VISIBLE)
            content.setTextViewText(R.id.notification_clock_text, clockLine)
        } else {
            content.setViewVisibility(R.id.notification_clock_row, View.GONE)
        }

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(content)
            .setCustomBigContentView(content)
            .setContentTitle(title)
            .setContentText(listOfNotNull(actionText, clockLine).joinToString(" · "))
            .setContentIntent(openIntent(context))
            .setDeleteIntent(dismissIntent(context))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setShowWhen(false)
            .setUsesChronometer(false)
            .build()
    }

    /** 独立计时器那一行；null 表示「未启动 / 已重置」——整行不显示。 */
    private fun clockText(
        context: Context,
        state: WorkoutNotificationState,
        now: Long,
    ): String? {
        val display = state.clock?.resolve(now) ?: return null
        return when (display.kind) {
            WorkoutClockKind.COUNTDOWN -> {
                val target = WorkoutClockLogic.formatClock(display.durationMillis)
                when (display.state) {
                    // 跑着的时候数字由 Chronometer 画，这里只给折叠态/无障碍用的固定文案，
                    // 也正因为不含秒数，contentKey 不会每秒变，避免「每秒重投 → 卡一秒再跳」。
                    WorkoutClockDisplayState.RUNNING ->
                        context.getString(R.string.workout_notification_clock_countdown_running, target)
                    WorkoutClockDisplayState.PAUSED -> context.getString(
                        R.string.workout_notification_clock_countdown_paused,
                        WorkoutClockLogic.formatClock(display.elapsedMillis),
                        target,
                    )
                    WorkoutClockDisplayState.FINISHED -> context.getString(
                        R.string.workout_notification_clock_countdown_finished,
                        target,
                        target,
                    )
                }
            }
            WorkoutClockKind.STOPWATCH -> {
                val time = WorkoutClockLogic.formatClock(display.millis)
                when (display.state) {
                    WorkoutClockDisplayState.PAUSED ->
                        context.getString(R.string.workout_notification_clock_stopwatch_paused, time)
                    // 秒表只增不减，不存在「结束」语义
                    else -> context.getString(R.string.workout_notification_clock_stopwatch, time)
                }
            }
        }
    }

    private fun durationText(context: Context, millis: Long): String {
        val totalSeconds = millis.coerceAtLeast(0L) / 1000L
        return context.getString(
            R.string.workout_notification_duration,
            totalSeconds / 60L,
            totalSeconds % 60L,
        )
    }

    private fun openIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .setAction(ACTION_OPEN_ACTIVE_WORKOUT)
            .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        return PendingIntent.getActivity(context, REQUEST_OPEN, intent, pendingFlags())
    }

    private fun dismissIntent(context: Context): PendingIntent {
        val intent = Intent(context, WorkoutNotificationReceiver::class.java)
            .setAction(WorkoutNotificationReceiver.ACTION_DISMISSED)
        return PendingIntent.getBroadcast(context, REQUEST_DISMISS, intent, pendingFlags())
    }

    private fun pendingFlags(): Int =
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.workout_notification_channel),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.workout_notification_channel_desc)
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(channel)
    }

    private fun rememberTombstone(context: Context, draftKey: String) {
        runCatching {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_TOMBSTONE, draftKey)
                .apply()
        }
    }

    private fun isTombstoned(context: Context, draftKey: String): Boolean {
        val saved = runCatching {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_TOMBSTONE, null)
        }.getOrNull()
        return saved != null && saved == draftKey
    }

    /** 通知点击后带回 MainActivity 的 action。 */
    const val ACTION_OPEN_ACTIVE_WORKOUT = "com.example.gymlog.action.OPEN_ACTIVE_WORKOUT"
}
