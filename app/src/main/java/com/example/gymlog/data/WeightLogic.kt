package com.example.gymlog.data

object WeightLogic {
    const val STEP_KG = 2.5

    fun parseKg(text: String): Double? {
        val value = parseSignedKg(text) ?: return null
        if (value < 0.0) return null
        return value
    }

    fun parseSignedKg(text: String): Double? {
        val normalized = text.trim().replace(',', '.')
        if (normalized.isEmpty()) return null
        val value = normalized.toDoubleOrNull() ?: return null
        if (value.isNaN() || value.isInfinite()) return null
        return value
    }

    fun formatKg(value: Double): String {
        val rounded = Math.round(value * 1000.0) / 1000.0
        return if (rounded % 1.0 == 0.0) {
            rounded.toLong().toString()
        } else {
            rounded.toString()
        }
    }

    fun stepKg(currentText: String, increase: Boolean): String {
        val current = parseKg(currentText) ?: 0.0
        val next = if (increase) current + STEP_KG else current - STEP_KG
        return formatKg(maxOf(0.0, next))
    }

    fun stepSignedKg(currentText: String, increase: Boolean): String {
        val current = parseSignedKg(currentText) ?: 0.0
        val next = if (increase) current + STEP_KG else current - STEP_KG
        return formatKg(next)
    }

    fun stepReps(currentText: String, increase: Boolean): String {
        val current = currentText.trim().toIntOrNull()?.coerceAtLeast(0) ?: 0
        val next = if (increase) current + 1 else current - 1
        return maxOf(0, next).toString()
    }

    fun storedWeightKg(
        mode: WeightMode,
        normalText: String,
        extraText: String,
    ): Result<Double?> {
        return when (mode) {
            WeightMode.BODYWEIGHT -> Result.success(null)
            WeightMode.WEIGHTED -> {
                val value = parseKg(normalText)
                    ?: return Result.failure(IllegalArgumentException("WEIGHT_REQUIRED"))
                Result.success(value)
            }
            WeightMode.BODYWEIGHT_PLUS -> {
                val value = parseSignedKg(extraText)
                    ?: return Result.failure(IllegalArgumentException("EXTRA_WEIGHT_REQUIRED"))
                Result.success(value)
            }
        }
    }

    fun bodyWeightKg(measurements: List<BodyMeasurementEntity>, onEpochDay: Long): Double {
        val recorded = measurements
            .filter { it.recordedEpochDay <= onEpochDay && it.weightKg != null }
            .maxWithOrNull(compareBy<BodyMeasurementEntity>({ it.recordedEpochDay }, { it.createdAt }))
            ?.weightKg
        return recorded?.takeIf { it > 0.0 } ?: 0.0
    }

    fun effectiveKg(mode: WeightMode, storedKg: Double?, bodyWeightKg: Double): Double {
        return when (mode) {
            WeightMode.WEIGHTED -> storedKg ?: 0.0
            WeightMode.BODYWEIGHT -> bodyWeightKg
            WeightMode.BODYWEIGHT_PLUS -> bodyWeightKg + (storedKg ?: 0.0)
        }
    }

    fun setVolume(weightKg: Double?, reps: Int?): Double {
        val weight = weightKg ?: 0.0
        val count = reps ?: 0
        if (weight <= 0.0 || count <= 0) return 0.0
        return weight * count
    }

    fun totalVolume(sets: List<Pair<Double?, Int?>>): Double {
        return sets.sumOf { setVolume(it.first, it.second) }
    }

    fun displayVolume(volume: Double): String {
        return "${formatKg(volume)} kg"
    }

    fun displaySet(weightKg: Double?): String {
        return "${formatKg(weightKg ?: 0.0)} kg"
    }

    fun defaultModeForExercise(exercise: ExerciseEntity): WeightMode {
        val stored = WeightMode.fromStorage(exercise.weightMode)
        return if (stored == WeightMode.BODYWEIGHT_PLUS) WeightMode.WEIGHTED else stored
    }
}
