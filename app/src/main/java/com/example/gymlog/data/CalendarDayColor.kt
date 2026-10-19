package com.example.gymlog.data

/**
 * Calendar cell color. Ordinary training days use the body-part category with
 * the highest volume. Special day marks replace that color.
 *
 * Tie-break when two categories have the same volume: the earlier entry in
 * [CalendarColorCategory] wins (chest, back, shoulders, legs, arms, other).
 * This does not depend on query order.
 *
 * If several special marks were ever present on one day, [specialDay] keeps
 * lazy, then indulge, then rest. The database stores only one mark per day.
 */
enum class CalendarColorCategory(val argb: Int) {
    CHEST(0xFFE57373.toInt()),
    BACK(0xFF64B5F6.toInt()),
    SHOULDERS(0xFFFFB74D.toInt()),
    LEGS(0xFF81C784.toInt()),
    ARMS(0xFFBA68C8.toInt()),
    OTHER(0xFF90A4AE.toInt()),
}

enum class CalendarSpecialDay(val argb: Int) {
    LAZY(0xFFFFEB3B.toInt()),
    INDULGE(0xFFEC407A.toInt()),
    REST(0xFFD8DDE3.toInt()),
}

sealed interface CalendarDayPaint {
    data class Training(val category: CalendarColorCategory) : CalendarDayPaint

    data class Special(val day: CalendarSpecialDay) : CalendarDayPaint

    data object Empty : CalendarDayPaint

    fun argb(): Int? = when (this) {
        is Training -> category.argb
        is Special -> day.argb
        Empty -> null
    }
}

object CalendarDayColorLogic {
    private val specialPriority = listOf(
        DayKind.LAZY,
        DayKind.INDULGE,
        DayKind.REST,
    )

    fun categoryForPartKey(part: String): CalendarColorCategory = when (part) {
        ExerciseClassification.CHEST.id, BodyPart.CHEST.name -> CalendarColorCategory.CHEST
        ExerciseClassification.BACK.id, BodyPart.BACK.name -> CalendarColorCategory.BACK
        ExerciseClassification.SHOULDERS.id, BodyPart.SHOULDERS.name -> CalendarColorCategory.SHOULDERS
        ExerciseClassification.UPPER_LEGS.id,
        ExerciseClassification.LOWER_LEGS.id,
        ExerciseClassification.LEGACY_LEGS.id,
        BodyPart.LEGS.name,
        -> CalendarColorCategory.LEGS
        ExerciseClassification.UPPER_ARMS.id,
        ExerciseClassification.LOWER_ARMS.id,
        ExerciseClassification.LEGACY_ARMS.id,
        BodyPart.ARMS.name,
        -> CalendarColorCategory.ARMS
        ExerciseClassification.WAIST.id,
        ExerciseClassification.CARDIO.id,
        ExerciseClassification.NECK.id,
        ExerciseClassification.LEGACY_BODY_OTHER.id,
        ExerciseClassification.UNKNOWN_BODY.id,
        BodyPart.ABS.name,
        BodyPart.OTHER.name,
        -> CalendarColorCategory.OTHER
        else -> CalendarColorCategory.OTHER
    }

    fun specialDay(kinds: Collection<DayKind>): CalendarSpecialDay? {
        val chosen = specialPriority.firstOrNull { it in kinds } ?: return null
        return when (chosen) {
            DayKind.LAZY -> CalendarSpecialDay.LAZY
            DayKind.INDULGE -> CalendarSpecialDay.INDULGE
            DayKind.REST -> CalendarSpecialDay.REST
            DayKind.TRAINING -> null
        }
    }

    /**
     * Highest volume wins. Equal volumes use [CalendarColorCategory] ordinal.
     * Categories with no sets are absent. A recorded set with zero volume still
     * occupies its category so a training day is not left uncolored.
     */
    fun dominantCategory(volumes: Map<CalendarColorCategory, Double>): CalendarColorCategory? {
        return volumes.entries
            .sortedWith(
                compareByDescending<Map.Entry<CalendarColorCategory, Double>> { it.value }
                    .thenBy { it.key.ordinal },
            )
            .firstOrNull()
            ?.key
    }

    fun paint(stored: DayKind?, volumes: Map<CalendarColorCategory, Double>): CalendarDayPaint {
        val special = specialDay(listOfNotNull(stored))
        if (special != null) return CalendarDayPaint.Special(special)
        val category = dominantCategory(volumes) ?: return CalendarDayPaint.Empty
        return CalendarDayPaint.Training(category)
    }

    fun paintsFor(
        sessions: List<SessionDetail>,
        measurements: List<BodyMeasurementEntity>,
        stored: Map<Long, DayKind>,
    ): Map<Long, CalendarDayPaint> {
        val volumes = HashMap<Long, MutableMap<CalendarColorCategory, Double>>()
        for (detail in sessions) {
            val day = detail.session.dateEpochDay
            val bodyWeight = WeightLogic.bodyWeightKg(measurements, day)
            for (item in detail.exercises) {
                if (item.sets.isEmpty()) continue
                val category = categoryForPartKey(
                    ExerciseClassification.exerciseGroupingKey(item.exercise),
                )
                val dayVolumes = volumes.getOrPut(day) { HashMap() }
                for (set in item.sets) {
                    val load = WeightLogic.effectiveKg(
                        WeightMode.fromStorage(set.weightMode),
                        set.weightKg,
                        bodyWeight,
                    )
                    val volume = WeightLogic.setVolume(load, set.reps)
                    dayVolumes[category] = (dayVolumes[category] ?: 0.0) + volume
                }
            }
        }
        val days = HashSet<Long>(volumes.size + stored.size)
        days.addAll(volumes.keys)
        days.addAll(stored.keys)
        val paints = HashMap<Long, CalendarDayPaint>(days.size)
        for (day in days) {
            val resolved = paint(stored[day], volumes[day].orEmpty())
            if (resolved !is CalendarDayPaint.Empty) {
                paints[day] = resolved
            }
        }
        return paints
    }
}
