package com.example.gymlog.data

fun List<SessionDetail>.orderedForDay(
    measurements: List<BodyMeasurementEntity>,
): List<SessionDetail> {
    if (isEmpty()) return this
    val ordered = mapIndexed { index, detail ->
        val exercises = detail.exercises.orderedByVolume(detail.session.dateEpochDay, measurements)
        val updated = detail.copy(exercises = exercises)
        DayRow(
            detail = updated,
            volume = exercises.sumOf { it.volume(detail.session.dateEpochDay, measurements) },
            part = dominantPart(exercises, detail.session.dateEpochDay, measurements),
            index = index,
        )
    }
    val partVolume = ordered.groupBy { it.part }.mapValues { (_, rows) -> rows.sumOf { it.volume } }
    return ordered.sortedWith(
        compareByDescending<DayRow> { partVolume[it.part] ?: 0.0 }
            .thenBy { ExerciseClassification.partSortOrder(it.part) }
            .thenByDescending { it.volume }
            .thenBy { it.index },
    ).map { it.detail }
}

fun List<SessionDetail>.partVolumes(
    measurements: List<BodyMeasurementEntity>,
): List<Pair<String, Double>> {
    val totals = LinkedHashMap<String, Double>()
    forEach { detail ->
        detail.exercises.forEach { item ->
            val volume = item.volume(detail.session.dateEpochDay, measurements)
            val part = item.exerciseGroupingKey()
            totals[part] = (totals[part] ?: 0.0) + volume
        }
    }
    return totals.entries
        .sortedWith(
            compareByDescending<Map.Entry<String, Double>> { it.value }
                .thenBy { ExerciseClassification.partSortOrder(it.key) },
        )
        .map { (part, volume) ->
            ExerciseClassification.partLabel(part) to volume
        }
}

private fun List<SessionExercise>.orderedByVolume(
    dateEpochDay: Long,
    measurements: List<BodyMeasurementEntity>,
): List<SessionExercise> {
    if (size < 2) return this
    val volumes = map { it.volume(dateEpochDay, measurements) }
    val partVolume = indices.groupBy { this[it].exerciseGroupingKey() }
        .mapValues { (_, indexes) -> indexes.sumOf { volumes[it] } }
    return withIndex().sortedWith(
        compareByDescending<IndexedValue<SessionExercise>> { partVolume[it.value.exerciseGroupingKey()] ?: 0.0 }
            .thenBy { ExerciseClassification.partSortOrder(it.value.exerciseGroupingKey()) }
            .thenByDescending { volumes[it.index] }
            .thenBy { it.index },
    ).map { it.value }
}

private fun dominantPart(
    exercises: List<SessionExercise>,
    dateEpochDay: Long,
    measurements: List<BodyMeasurementEntity>,
): String {
    return exercises
        .groupBy { it.exerciseGroupingKey() }
        .mapValues { (_, items) -> items.sumOf { it.volume(dateEpochDay, measurements) } }
        .entries
        .sortedWith(
            compareByDescending<Map.Entry<String, Double>> { it.value }
                .thenBy { ExerciseClassification.partSortOrder(it.key) },
        )
        .firstOrNull()
        ?.key
        .orEmpty()
}

private fun SessionExercise.exerciseGroupingKey(): String =
    ExerciseClassification.exerciseGroupingKey(exercise)

private fun SessionExercise.volume(
    dateEpochDay: Long,
    measurements: List<BodyMeasurementEntity>,
): Double {
    val bodyWeight = WeightLogic.bodyWeightKg(measurements, dateEpochDay)
    return WeightLogic.totalVolume(
        sets.map { set ->
            WeightLogic.effectiveKg(
                WeightMode.fromStorage(set.weightMode),
                set.weightKg,
                bodyWeight,
            ) to set.reps
        },
    )
}

private data class DayRow(
    val detail: SessionDetail,
    val volume: Double,
    val part: String,
    val index: Int,
)
