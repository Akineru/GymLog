package com.example.gymlog.data

/**
 * 通知要表达的三种训练状态。与训练总计时的业务语义一一对应，不引入新语义：
 * - [RUNNING]：总计时正在走，通知用系统 chronometer 显示
 * - [PAUSED]：总计时被暂停（编辑时长时），通知显示冻结的时间
 * - [FINISHING]：已点「完成」进入保存页，时间按现有规则冻结
 */
enum class WorkoutNotificationPhase {
    RUNNING,
    PAUSED,
    FINISHING,
}

/** 独立计时器的两种模式。与 `WorkoutClockMode` 对应，但放在 data 层避免反向依赖 UI。 */
enum class WorkoutClockKind {
    /** 倒计时（组歇） */
    COUNTDOWN,

    /** 正计时（秒表） */
    STOPWATCH,
}

enum class WorkoutClockDisplayState {
    RUNNING,
    PAUSED,
    FINISHED,
}

data class WorkoutClockDisplay(
    val kind: WorkoutClockKind,
    val state: WorkoutClockDisplayState,
    val millis: Long,
)

/**
 * 通知里要展示的独立计时器快照。
 *
 * 只保存「快照瞬间的显示值 + 单调时钟锚点」，渲染时用与
 * `SessionEditorViewModel.tickClock()` 完全相同的公式还原：
 * 倒计时 `base - (now - anchor)`，正计时 `base + (now - anchor)`。
 * 因此通知不是第二套计时器，而是同一时间基准的一个显示视图。
 */
data class WorkoutNotificationClock(
    val kind: WorkoutClockKind,
    val running: Boolean,
    /** 这一次配置是否真正启动过。false = 未启动 / 已重置，整行不显示。 */
    val started: Boolean,
    val displayMillis: Long,
    val durationMillis: Long,
    val anchoredAtElapsedRealtime: Long,
) {
    /**
     * 当前时刻该显示什么。返回 null 表示**不显示计时器行**。
     *
     * 是否「启动过」由 [started] 明确给出，不再从「显示值是否等于初始值」反推 ——
     * 那样会把「刚开始 100ms 内就暂停」或「暂停在初始时长」误判成未启动。
     */
    fun resolve(nowElapsedRealtime: Long): WorkoutClockDisplay? {
        if (!started) return null
        val delta = if (running) {
            (nowElapsedRealtime - anchoredAtElapsedRealtime).coerceAtLeast(0L)
        } else {
            0L
        }
        val millis = when (kind) {
            WorkoutClockKind.COUNTDOWN -> (displayMillis - delta).coerceAtLeast(0L)
            WorkoutClockKind.STOPWATCH -> (displayMillis + delta).coerceAtLeast(0L)
        }
        if (running) {
            // 归零即结束，绝不显示负数，也不再显示「还在跑」
            if (kind == WorkoutClockKind.COUNTDOWN && millis <= 0L) {
                return WorkoutClockDisplay(kind, WorkoutClockDisplayState.FINISHED, 0L)
            }
            return WorkoutClockDisplay(kind, WorkoutClockDisplayState.RUNNING, millis)
        }
        if (kind == WorkoutClockKind.COUNTDOWN && millis <= 0L) {
            return WorkoutClockDisplay(kind, WorkoutClockDisplayState.FINISHED, 0L)
        }
        return WorkoutClockDisplay(kind, WorkoutClockDisplayState.PAUSED, millis)
    }
}

/**
 * 是否值得投递一次通知的纯逻辑。
 *
 * 抽出来的原因：这里是「用户划掉通知后不能被弹回来」的唯一闸门，
 * 而它同时被三条路径调用（界面推送 / 服务每秒重绘 / 回前台补投），
 * 放在 Android 单例里没法用普通单测守住。
 */
object NotificationPostPolicy {
    /**
     * @param dismissedByUser 用户是否划掉过这条通知（Android 14+ 允许）
     * @param force 结构性变化（相位/动作/计时器启停或重置），必须重发
     * @param contentKey 本次要渲染出来的内容指纹
     * @param lastPostedKey 上一次真正投递出去的内容指纹
     *
     * 注意：划掉状态优先级最高 —— 即使内容是新的、甚至是 [force]，
     * 只要用户划掉过就不再投递。恢复显示只能靠 [ActiveWorkoutNotifier.publish]
     * 里「结构变化 → 清除划掉标记」那条路径。
     */
    fun shouldPost(
        dismissedByUser: Boolean,
        force: Boolean,
        contentKey: String,
        lastPostedKey: String?,
    ): Boolean {
        if (dismissedByUser) return false
        return force || contentKey != lastPostedKey
    }
}

/**
 * 通知状态的唯一数据形状。
 *
 * [elapsedMillis] 是发布瞬间的累计时长；[anchoredAtElapsedRealtime] 是发布瞬间的单调时钟，
 * 用来在服务稍后重建通知时把这段时间补回来 —— 补的方式与
 * `SessionEditorViewModel.totalElapsed()` 完全一致（累计值 + 单调时钟差值），
 * 因此通知不会变成第二套计时。
 */
data class WorkoutNotificationState(
    val draftKey: String,
    val phase: WorkoutNotificationPhase,
    val elapsedMillis: Long,
    val exerciseName: String?,
    val anchoredAtElapsedRealtime: Long,
    val clock: WorkoutNotificationClock? = null,
) {
    /** 当前时刻应显示的训练总时长。运行中才继续累加，暂停与待保存都返回冻结值。 */
    fun elapsedAt(nowElapsedRealtime: Long): Long {
        if (phase != WorkoutNotificationPhase.RUNNING) return elapsedMillis
        val delta = (nowElapsedRealtime - anchoredAtElapsedRealtime).coerceAtLeast(0L)
        return elapsedMillis + delta
    }

    /**
     * 用于判断「是不是一次新的状态切换」。
     *
     * 刻意**不包含**「运行中的秒」：那一秒一秒的走动交给系统 chronometer 与
     * [WorkoutNotificationClock] 的时序重绘，否则每秒推送会变成后台唤醒。
     * 也只有这个签名变化，才会清除「用户划掉过通知」的标记 ——
     * 否则用户划掉后会被每秒的文本刷新弹回来。
     */
    val signature: String
        get() = buildString {
            append(draftKey).append('|').append(phase.name).append('|')
            append(exerciseName.orEmpty()).append('|')
            clock?.let {
                append("clock=").append(it.kind.name).append('|')
                append(it.running).append('|').append(it.durationMillis / 1000L)
            }
            if (phase != WorkoutNotificationPhase.RUNNING) append(elapsedMillis)
        }
}
