package com.example.gymlog.data

enum class DayKind {
    TRAINING,
    REST,
    INDULGE,
    LAZY,
    ;

    companion object {
        fun fromStored(value: String?): DayKind? = entries.find { it.name == value }
    }
}

object DayKindLogic {
    fun trainedEpochDays(sessions: List<SessionDetail>): Set<Long> {
        val days = HashSet<Long>()
        for (detail in sessions) {
            if (detail.exercises.any { it.sets.isNotEmpty() }) {
                days += detail.session.dateEpochDay
            }
        }
        return days
    }

    fun resolved(hasTraining: Boolean, stored: DayKind?): DayKind? {
        return stored ?: if (hasTraining) DayKind.TRAINING else null
    }

    /**
     * Value to persist after the user taps [tapped].
     * A null result means "no explicit choice": a day with sets still shows as training.
     * Tapping the explicit choice again clears it.
     */
    fun nextStored(hasTraining: Boolean, stored: DayKind?, tapped: DayKind): DayKind? {
        val showing = resolved(hasTraining, stored)
        if (showing == tapped && stored != null) return null
        if (tapped == DayKind.TRAINING && hasTraining) return null
        if (showing == tapped) return stored
        return tapped
    }

    fun resolveAll(sessions: List<SessionDetail>, stored: Map<Long, DayKind>): Map<Long, DayKind> {
        val trained = trainedEpochDays(sessions)
        val days = HashSet<Long>(trained.size + stored.size)
        days.addAll(trained)
        days.addAll(stored.keys)
        val resolved = HashMap<Long, DayKind>(days.size)
        for (day in days) {
            val kind = resolved(day in trained, stored[day]) ?: continue
            resolved[day] = kind
        }
        return resolved
    }
}
