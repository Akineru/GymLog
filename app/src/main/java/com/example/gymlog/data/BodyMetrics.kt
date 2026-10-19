package com.example.gymlog.data

import java.time.LocalDate
import java.time.Period
import kotlin.math.abs
import kotlin.math.round

object BodyMetrics {
    fun ageYears(birthday: LocalDate, onDate: LocalDate): Int? {
        if (birthday.isAfter(onDate)) return null
        return Period.between(birthday, onDate).years
    }

    fun bmi(heightCm: Double?, weightKg: Double?): Double? {
        if (heightCm == null || weightKg == null || heightCm <= 0.0 || weightKg <= 0.0) {
            return null
        }
        val meters = heightCm / 100.0
        return weightKg / (meters * meters)
    }

    fun displayBmi(value: Double): String {
        val rounded = round(value * 10.0) / 10.0
        return if (rounded % 1.0 == 0.0) {
            rounded.toLong().toString()
        } else {
            rounded.toString()
        }
    }

    fun metricsChanged(
        previous: BodyMeasurementEntity?,
        heightCm: Double?,
        weightKg: Double?,
        bodyFatPercent: Double?,
        waistCm: Double?,
        armCm: Double?,
    ): Boolean {
        if (heightCm == null && weightKg == null && bodyFatPercent == null &&
            waistCm == null && armCm == null
        ) {
            return false
        }
        if (previous == null) {
            return true
        }
        return !sameNumber(previous.heightCm, heightCm) ||
            !sameNumber(previous.weightKg, weightKg) ||
            !sameNumber(previous.bodyFatPercent, bodyFatPercent) ||
            !sameNumber(previous.waistCm, waistCm) ||
            !sameNumber(previous.armCm, armCm)
    }

    private fun sameNumber(left: Double?, right: Double?): Boolean {
        if (left == null && right == null) return true
        if (left == null || right == null) return false
        return abs(left - right) < 0.0001
    }
}
