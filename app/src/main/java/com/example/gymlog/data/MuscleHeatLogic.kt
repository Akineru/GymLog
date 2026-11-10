package com.example.gymlog.data

import java.time.LocalDate

enum class MuscleHeatRange {
    WEEK,
    MONTH,
    RECOVERY,
}

/**
 * 恢复三态。只按自然日差估算，不做小时级精度，也不代表生理测量。
 */
enum class MuscleRecoveryState {
    RECOVERING,
    RECOVERED,
    STALE,
}

data class MuscleRecoveryInfo(
    val state: MuscleRecoveryState,
    /** 最近一次练到这块肌肉的日期（主练或次要都算）。 */
    val lastDateEpochDay: Long,
    /** 按窗口推算的恢复日 = 上次训练日 + 窗口天数。 */
    val expectedRecoveryEpochDay: Long,
)

/**
 * 热力分档。数值是「主练课次当量」，不是组数：
 * 一节课里该肌群只要出现主练动作就记 1，只有次练记 0.5，多动作取较高者。
 */
enum class MuscleHeatLevel {
    NONE,
    LOW,
    FAIR,
    MEDIUM,
    HIGH,
    PEAK,
}

data class MuscleHeatTouch(
    val sessionId: String,
    val dateEpochDay: Long,
    val exerciseId: String,
    val exerciseName: String,
    val muscleId: String,
    val role: MuscleRole,
    val setCount: Int,
)

data class MuscleHeatExercise(
    val exerciseId: String,
    val name: String,
    val role: MuscleRole,
    val sessionCount: Int,
    val setCount: Int,
)

data class MuscleHeatStat(
    val muscleId: String,
    val sessionCount: Int,
    val score: Double,
    val setCount: Int,
    val lastDateEpochDay: Long?,
    val exercises: List<MuscleHeatExercise>,
    val level: MuscleHeatLevel,
)

object MuscleHeatLogic {
    /** 超过这么多天没练，就不再算「已恢复」，改判「久未训练」。 */
    const val STALE_DAYS = 14L

    /** 大肌群恢复窗口（天）。 */
    private const val LONG_WINDOW_DAYS = 3

    /** 小肌群恢复窗口（天）；只以次要角色练到时再减半。 */
    private const val SHORT_WINDOW_DAYS = 2

    /** 小肌群；不在这张表里的（含未知 id）都按大肌群算。 */
    private val SMALL_MUSCLES = setOf(
        "deltoid_anterior",
        "deltoid_lateral",
        "deltoid_posterior",
        "biceps_brachii",
        "triceps_brachii",
        "forearm",
        "rectus_abdominis",
        "external_oblique",
        "gastrocnemius",
    )

    fun period(today: LocalDate, range: MuscleHeatRange): ClosedRange<LocalDate> {
        val start = when (range) {
            MuscleHeatRange.WEEK -> today.minusDays(6)
            MuscleHeatRange.MONTH -> today.minusDays(29)
            // 恢复看的是「相对今天」，没有区间概念。
            MuscleHeatRange.RECOVERY -> today
        }
        return start..today
    }

    fun levelFor(score: Double): MuscleHeatLevel = when {
        score <= 0.0 -> MuscleHeatLevel.NONE
        score <= 0.5 -> MuscleHeatLevel.LOW
        score <= 1.5 -> MuscleHeatLevel.FAIR
        score <= 3.5 -> MuscleHeatLevel.MEDIUM
        score <= 6.5 -> MuscleHeatLevel.HIGH
        else -> MuscleHeatLevel.PEAK
    }

    fun touches(
        sessions: List<SessionDetail>,
        storedLinks: List<MuscleLink>,
    ): List<MuscleHeatTouch> {
        return sessions.flatMap { detail ->
            detail.exercises.mapNotNull { item ->
                val sets = item.sets.size
                if (sets <= 0) return@mapNotNull null
                item to sets
            }.flatMap { (item, sets) ->
                MuscleMapLogic.linksFor(item.exercise.id, item.exercise.isCustom, storedLinks)
                    .map { link ->
                        MuscleHeatTouch(
                            sessionId = detail.session.id,
                            dateEpochDay = detail.session.dateEpochDay,
                            exerciseId = item.exercise.id,
                            exerciseName = item.exercise.name,
                            muscleId = link.muscleId,
                            role = link.role,
                            setCount = sets,
                        )
                    }
            }
        }
    }

    fun aggregate(
        touches: List<MuscleHeatTouch>,
        startEpochDay: Long,
        endEpochDay: Long,
    ): Map<String, MuscleHeatStat> {
        val inRange = touches.filter { it.dateEpochDay in startEpochDay..endEpochDay }
        return inRange.groupBy { it.muscleId }.mapValues { (muscleId, rows) ->
            summarize(muscleId, rows)
        }
    }

    /**
     * 按「最近一次训练」估算每块肌肉的恢复状态。
     *
     * 入参必须是**全量** touches（不能先按周 / 月窗口过滤）：恢复看的是历史里的最近一次。
     * 从没练过的肌肉不出现在返回表里，图上保持未着色的灰底。
     */
    fun recovery(
        touches: List<MuscleHeatTouch>,
        today: LocalDate,
    ): Map<String, MuscleRecoveryInfo> {
        val todayEpochDay = today.toEpochDay()
        return touches.groupBy { it.muscleId }.mapValues { (muscleId, rows) ->
            val lastDate = rows.maxOf { it.dateEpochDay }
            val latestRows = rows.filter { it.dateEpochDay == lastDate }
            // 最近那节课只以次要肌群出现才减半；既有主练又有次要时按主练算。
            val secondaryOnly = latestRows.none { it.role == MuscleRole.PRIMARY }
            val window = windowDays(muscleId).let { if (secondaryOnly) it / 2 else it }
            val days = todayEpochDay - lastDate
            val state = when {
                days < window -> MuscleRecoveryState.RECOVERING
                days <= STALE_DAYS -> MuscleRecoveryState.RECOVERED
                else -> MuscleRecoveryState.STALE
            }
            MuscleRecoveryInfo(
                state = state,
                lastDateEpochDay = lastDate,
                expectedRecoveryEpochDay = lastDate + window,
            )
        }
    }

    /**
     * 每块肌肉只保留「最近一次训练那天」的记录再汇总。
     * 恢复模式下底部详情要用它，动作列表才能和「上次训练 M月d日」对得上。
     */
    fun latestSessionStats(touches: List<MuscleHeatTouch>): Map<String, MuscleHeatStat> {
        return touches.groupBy { it.muscleId }.mapValues { (muscleId, rows) ->
            val lastDate = rows.maxOf { it.dateEpochDay }
            summarize(muscleId, rows.filter { it.dateEpochDay == lastDate })
        }
    }

    private fun windowDays(muscleId: String): Int =
        if (muscleId in SMALL_MUSCLES) SHORT_WINDOW_DAYS else LONG_WINDOW_DAYS

    private fun summarize(muscleId: String, rows: List<MuscleHeatTouch>): MuscleHeatStat {
        val bySession = rows.groupBy { it.sessionId }
        var score = 0.0
        var setCount = 0
        var lastDate = Long.MIN_VALUE
        val exercises = linkedMapOf<String, ExerciseAccum>()
        bySession.forEach { (_, sessionRows) ->
            val primary = sessionRows.any { it.role == MuscleRole.PRIMARY }
            score += if (primary) 1.0 else 0.5
            val day = sessionRows.maxOf { it.dateEpochDay }
            if (day > lastDate) lastDate = day
            sessionRows.distinctBy { it.exerciseId }.forEach { row ->
                setCount += row.setCount
                val accum = exercises.getOrPut(row.exerciseId) {
                    ExerciseAccum(row.exerciseName, row.role)
                }
                accum.sessions += 1
                accum.sets += row.setCount
                if (row.role == MuscleRole.PRIMARY) accum.role = MuscleRole.PRIMARY
                if (accum.name.isBlank()) accum.name = row.exerciseName
            }
        }
        val exerciseRows = exercises.map { (id, accum) ->
            MuscleHeatExercise(
                exerciseId = id,
                name = accum.name,
                role = accum.role,
                sessionCount = accum.sessions,
                setCount = accum.sets,
            )
        }.sortedWith(compareByDescending<MuscleHeatExercise> { it.setCount }.thenBy { it.name })
        return MuscleHeatStat(
            muscleId = muscleId,
            sessionCount = bySession.size,
            score = score,
            setCount = setCount,
            lastDateEpochDay = lastDate.takeIf { it != Long.MIN_VALUE },
            exercises = exerciseRows,
            level = levelFor(score),
        )
    }

    private class ExerciseAccum(
        var name: String,
        var role: MuscleRole,
    ) {
        var sessions: Int = 0
        var sets: Int = 0
    }
}
