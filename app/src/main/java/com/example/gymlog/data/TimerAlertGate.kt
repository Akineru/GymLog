package com.example.gymlog.data

/**
 * 「这一刻能不能弹到点提醒」的判定。
 *
 * 为什么需要它：闹钟是**双路**的（`setAlarmClock` 主 + `setExactAndAllowWhileIdle` 备），
 * 而国产 ROM 会把第二条按批处理窗口（约 30 秒为一档）**提前**投递，
 * 于是接收器一收到就弹会出现「还剩 29 秒就提示到点」。
 * 所以接收器不能相信「收到即到点」，必须拿排闹钟时记下的**目标时刻**对一遍。
 *
 * 目标时刻用 `SystemClock.elapsedRealtime()`（单调、跨进程存活、不受改系统时间影响），
 * 与倒计时本身用的是同一基准。
 */
object TimerAlertGate {

    /** 允许提前的容差：闹钟偶尔会早几十毫秒，这不该被当成「提前到点」。 */
    const val EARLY_TOLERANCE_MILLIS = 1_000L

    /**
     * @param atElapsed 排闹钟时记下的「该响的时刻」（elapsedRealtime 基准）。0 表示老数据/未知。
     * @param nowElapsed 现在（elapsedRealtime 基准）。
     * @param retried 本次是不是「已经补排过一次」的那条闹钟。
     */
    fun evaluate(atElapsed: Long, nowElapsed: Long, retried: Boolean): Verdict {
        // 没有目标时刻可比：只能照旧提醒，不能因为缺信息就不提醒
        if (atElapsed <= 0L) return Verdict(alert = true, rearmMillis = 0L)
        val remaining = atElapsed - nowElapsed
        // 已到点（或在容差内）：正常提醒
        if (remaining <= EARLY_TOLERANCE_MILLIS) return Verdict(alert = true, rearmMillis = 0L)
        // 明显提前，且已经补排过一次：不再重排，宁可早一点响也不能永远不响
        if (retried) return Verdict(alert = true, rearmMillis = 0L)
        // 明显提前：不提醒，按真实剩余时间重新排一次
        return Verdict(alert = false, rearmMillis = remaining)
    }

    data class Verdict(
        /** true = 现在就提醒；false = 只补排闹钟。 */
        val alert: Boolean,
        /** 需要补排时，按这个剩余毫秒重排。 */
        val rearmMillis: Long,
    )
}
