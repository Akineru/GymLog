package com.example.gymlog.data

import java.time.LocalDate

enum class MuscleHeatRange {
    WEEK,
    MONTH,
}

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
    fun period(today: LocalDate, range: MuscleHeatRange): ClosedRange<LocalDate> {
        val start = when (range) {
            MuscleHeatRange.WEEK -> today.minusDays(6)
            MuscleHeatRange.MONTH -> today.minusDays(29)
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
