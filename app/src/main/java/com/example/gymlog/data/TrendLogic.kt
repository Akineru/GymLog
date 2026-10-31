package com.example.gymlog.data

data class TrendPoint(
    val dateEpochDay: Long,
    val value: Double,
)

object TrendLogic {
    fun weightSeries(measurements: List<BodyMeasurementEntity>): List<TrendPoint> {
        return latestPerDay(measurements).mapNotNull { row ->
            row.weightKg?.takeIf { it > 0.0 }?.let { TrendPoint(row.recordedEpochDay, it) }
        }
    }

    fun bodyFatSeries(measurements: List<BodyMeasurementEntity>): List<TrendPoint> {
        return latestPerDay(measurements).mapNotNull { row ->
            row.bodyFatPercent?.takeIf { it > 0.0 }?.let { TrendPoint(row.recordedEpochDay, it) }
        }
    }

    fun bmiSeries(measurements: List<BodyMeasurementEntity>): List<TrendPoint> {
        var lastHeight: Double? = null
        return latestPerDay(measurements).mapNotNull { row ->
            if (row.heightCm != null && row.heightCm > 0.0) {
                lastHeight = row.heightCm
            }
            BodyMetrics.bmi(lastHeight, row.weightKg)?.let { bmi ->
                TrendPoint(row.recordedEpochDay, bmi)
            }
        }
    }

    fun volumeSeries(rows: List<DailyVolumeRow>): List<TrendPoint> {
        return rows
            .sortedBy { it.dateEpochDay }
            .map { TrendPoint(it.dateEpochDay, it.volume) }
            .filter { it.value > 0.0 }
    }

    fun exerciseVolumeSeries(sets: List<PerformedSet>): List<TrendPoint> {
        return sets
            .groupBy { it.dateEpochDay }
            .toSortedMap()
            .mapNotNull { (day, daySets) ->
                val volume = daySets.sumOf { WeightLogic.setVolume(it.weightKg, it.reps) }
                volume.takeIf { it > 0.0 }?.let { TrendPoint(day, it) }
            }
    }

    fun runningPrSeries(
        sets: List<PerformedSet>,
        maxEffort: Boolean,
        minWorkingReps: Int = PersonalRecordLogic.DEFAULT_MIN_WORKING_REPS,
    ): List<TrendPoint> {
        // 与 PersonalRecordLogic.fromSessions 共用同一套候选规则，避免两处口径不一致
        val kind = if (maxEffort) {
            PersonalRecordLogic.PrKind.MAX
        } else {
            PersonalRecordLogic.PrKind.WORKING
        }
        val relevant = sets
            .filter { set ->
                if (maxEffort) {
                    PersonalRecordLogic.isMaxCandidate(set)
                } else {
                    PersonalRecordLogic.isWorkingCandidate(set, minWorkingReps)
                }
            }
            .sortedWith(compareBy({ it.dateEpochDay }, { it.sessionId }))
        var best: PersonalRecord? = null
        val points = ArrayList<TrendPoint>()
        relevant.forEach { set ->
            val candidate = PersonalRecord(
                weightKg = set.weightKg,
                reps = set.reps,
                weightMode = set.weightMode,
                dateEpochDay = set.dateEpochDay,
            )
            val next = PersonalRecordLogic.keepHigher(best, candidate, kind) ?: return@forEach
            if (next !== best) {
                best = next
                val value = chartValue(next) ?: return@forEach
                val point = TrendPoint(set.dateEpochDay, value)
                if (points.isNotEmpty() && points.last().dateEpochDay == set.dateEpochDay) {
                    points[points.lastIndex] = point
                } else {
                    points += point
                }
            }
        }
        return points
    }

    private fun chartValue(record: PersonalRecord): Double? {
        val weight = record.weightKg ?: return null
        return weight.takeIf { it != 0.0 }
    }

    private fun latestPerDay(measurements: List<BodyMeasurementEntity>): List<BodyMeasurementEntity> {
        return measurements
            .groupBy { it.recordedEpochDay }
            .toSortedMap()
            .values
            .map { dayRows -> dayRows.maxBy { it.createdAt } }
    }
}
