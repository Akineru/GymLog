package com.example.gymlog.data

/**
 * 动作详情页历史列表里的一组。
 *
 * 重量按训练当天的体重换算成「有效重量」，与统计页、日历配色用的是同一套
 * [WeightLogic.effectiveKg] 规则，因此这里的容量与其它页面必然一致。
 */
data class ExerciseHistorySetView(
    val setIndex: Int,
    val weightMode: WeightMode,
    val storedWeightKg: Double?,
    val effectiveKg: Double,
    val reps: Int?,
) {
    /** 与 [WeightLogic.setVolume] 同一口径：重量或次数非正时贡献 0。 */
    val volumeKg: Double get() = WeightLogic.setVolume(effectiveKg, reps)
}

/** 一次训练里该动作的全部组，按日期倒序展示。 */
data class ExerciseHistoryEntry(
    val sessionId: String,
    val dateEpochDay: Long,
    val createdAt: Long,
    val sets: List<ExerciseHistorySetView>,
) {
    /** 组数沿用现有统计口径：有几组就记几组，不因重量/次数为 0 而丢弃。 */
    val setCount: Int get() = sets.size

    val volumeKg: Double get() = sets.sumOf { it.volumeKg }

    /**
     * 各组次数完全一致时返回该次数，否则 null。
     * 只要有一组次数为空就返回 null —— 宁可显示逐组明细，也不猜一个统一值。
     */
    val uniformReps: Int?
        get() {
            if (sets.isEmpty()) return null
            if (sets.any { it.reps == null }) return null
            return sets.mapNotNull { it.reps }.distinct().singleOrNull()
        }

    val totalReps: Int get() = sets.sumOf { it.reps ?: 0 }
}

object ExerciseHistoryLogic {
    /**
     * 把扁平的历史行按训练场次聚合成条目。
     *
     * - 只做分组与排序，不改动任何数据；
     * - 自重 / 负重自重的有效重量按当天的体重换算（[WeightLogic.bodyWeightKg]）；
     * - 排序：日期倒序 → 同一天按创建时间倒序，与历史列表一致。
     */
    fun entries(
        rows: List<ExerciseHistorySetRow>,
        measurements: List<BodyMeasurementEntity>,
    ): List<ExerciseHistoryEntry> {
        if (rows.isEmpty()) return emptyList()
        return rows.groupBy { it.sessionId }
            .map { (sessionId, sessionRows) ->
                val first = sessionRows.first()
                val bodyWeight = WeightLogic.bodyWeightKg(measurements, first.dateEpochDay)
                ExerciseHistoryEntry(
                    sessionId = sessionId,
                    dateEpochDay = first.dateEpochDay,
                    createdAt = first.createdAt,
                    sets = sessionRows.sortedBy { it.setIndex }.map { row ->
                        val mode = WeightMode.fromStorage(row.weightMode)
                        ExerciseHistorySetView(
                            setIndex = row.setIndex,
                            weightMode = mode,
                            storedWeightKg = row.weightKg,
                            effectiveKg = WeightLogic.effectiveKg(mode, row.weightKg, bodyWeight),
                            reps = row.reps,
                        )
                    },
                )
            }
            .sortedWith(
                compareByDescending<ExerciseHistoryEntry> { it.dateEpochDay }
                    .thenByDescending { it.createdAt },
            )
    }
}
