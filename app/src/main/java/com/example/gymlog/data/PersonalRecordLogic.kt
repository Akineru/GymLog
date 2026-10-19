package com.example.gymlog.data

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

data class PerformedSet(
    val sessionId: String,
    val dateEpochDay: Long,
    val weightKg: Double?,
    val reps: Int,
    val weightMode: String,
)

data class PersonalRecord(
    val weightKg: Double?,
    val reps: Int,
    val weightMode: String,
    val dateEpochDay: Long,
)

data class ExercisePersonalRecords(
    val working: PersonalRecord?,
    val max: PersonalRecord?,
)

object PersonalRecordLogic {
    fun fromSessions(sets: List<PerformedSet>): ExercisePersonalRecords {
        var working: PersonalRecord? = null
        var max: PersonalRecord? = null
        sets.filter { it.reps > 0 }.forEach { set ->
            val candidate = PersonalRecord(
                weightKg = set.weightKg,
                reps = set.reps,
                weightMode = set.weightMode,
                dateEpochDay = set.dateEpochDay,
            )
            when {
                set.reps == 1 -> max = keepHigher(max, candidate, preferLatestOnTie = true)
                set.reps >= 2 -> working = keepHigher(working, candidate, preferLatestOnTie = true)
            }
        }
        return ExercisePersonalRecords(working = working, max = max)
    }

    fun displayWorking(record: PersonalRecord): String {
        return "${WeightLogic.displaySet(record.weightKg)}×${record.reps} · ${formatDate(record.dateEpochDay)}"
    }

    fun displayMax(record: PersonalRecord): String {
        return "${WeightLogic.displaySet(record.weightKg)} · ${formatDate(record.dateEpochDay)}"
    }

    /**
     * Stored PRs prefer the latest date when weight and reps are equal.
     * The trend chart leaves [preferLatestOnTie] false so an equal performance
     * does not create a new point.
     */
    internal fun keepHigher(
        current: PersonalRecord?,
        candidate: PersonalRecord?,
        preferLatestOnTie: Boolean = false,
    ): PersonalRecord? {
        if (candidate == null) return current
        if (current == null) return candidate
        val cmp = compareRecords(candidate, current, preferLatestOnTie)
        return when {
            cmp > 0 -> candidate
            cmp < 0 -> current
            else -> current
        }
    }

    internal fun compareRecords(
        a: PersonalRecord,
        b: PersonalRecord,
        preferLatestOnTie: Boolean = false,
    ): Int {
        val weightCmp = effectiveWeight(a.weightKg).compareTo(effectiveWeight(b.weightKg))
        if (weightCmp != 0) return weightCmp
        val repsCmp = a.reps.compareTo(b.reps)
        if (repsCmp != 0) return repsCmp
        return if (preferLatestOnTie) a.dateEpochDay.compareTo(b.dateEpochDay) else 0
    }

    private fun effectiveWeight(weightKg: Double?): Double = weightKg ?: 0.0

    private fun formatDate(dateEpochDay: Long): String {
        return LocalDate.ofEpochDay(dateEpochDay).format(dateFormatter)
    }

    private val dateFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy年M月d日", Locale.CHINA)
}
