package com.example.gymlog

import com.example.gymlog.data.BodyMeasurementEntity
import com.example.gymlog.data.DailyVolumeRow
import com.example.gymlog.data.PerformedSet
import com.example.gymlog.data.TrendLogic
import com.example.gymlog.data.WeightMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class TrendLogicTest {
    @Test
    fun weightUsesLatestMeasurementEachDay() {
        val day = LocalDate.of(2026, 10, 11).toEpochDay()
        val points = TrendLogic.weightSeries(
            listOf(
                measurement(day, weight = 70.0, createdAt = 1),
                measurement(day, weight = 71.5, createdAt = 2),
            ),
        )
        assertEquals(1, points.size)
        assertEquals(71.5, points[0].value, 0.0)
    }

    @Test
    fun bmiCarriesForwardLastHeight() {
        val day1 = LocalDate.of(2026, 10, 1).toEpochDay()
        val day2 = LocalDate.of(2026, 10, 11).toEpochDay()
        val points = TrendLogic.bmiSeries(
            listOf(
                measurement(day1, height = 180.0, weight = 72.0),
                measurement(day2, weight = 73.8),
            ),
        )
        assertEquals(2, points.size)
        assertEquals(72.0 / (1.8 * 1.8), points[0].value, 0.0001)
        assertEquals(73.8 / (1.8 * 1.8), points[1].value, 0.0001)
    }

    @Test
    fun volumeKeepsPositiveDaysInOrder() {
        val points = TrendLogic.volumeSeries(
            listOf(
                DailyVolumeRow(20, 0.0),
                DailyVolumeRow(10, 800.0),
                DailyVolumeRow(15, 400.0),
            ),
        )
        assertEquals(listOf(10L, 15L), points.map { it.dateEpochDay })
        assertEquals(800.0, points[0].value, 0.0)
    }

    @Test
    fun exerciseVolumeSumsSetsOnTheSameDay() {
        val points = TrendLogic.exerciseVolumeSeries(
            listOf(
                set("a", 80.0, 8, date = 12),
                set("b", 80.0, 6, date = 12),
                set("c", 0.0, 10, date = 11),
                set("d", 50.0, 10, date = 10),
            ),
        )
        assertEquals(listOf(10L, 12L), points.map { it.dateEpochDay })
        assertEquals(500.0, points[0].value, 0.0)
        assertEquals(80.0 * 8 + 80.0 * 6, points[1].value, 0.0)
    }

    @Test
    fun runningPrOnlyMovesUp() {
        val points = TrendLogic.runningPrSeries(
            listOf(
                set("a", 80.0, 8, date = 10),
                set("b", 70.0, 10, date = 11),
                set("c", 85.0, 6, date = 12),
            ),
            maxEffort = false,
        )
        assertEquals(listOf(10L, 12L), points.map { it.dateEpochDay })
        assertEquals(80.0, points[0].value, 0.0)
        assertEquals(85.0, points[1].value, 0.0)
    }

    @Test
    fun equalPerformanceOnALaterDateDoesNotAddATrendPoint() {
        val points = TrendLogic.runningPrSeries(
            listOf(
                set("a", 80.0, 8, date = 10),
                set("b", 80.0, 8, date = 12),
            ),
            maxEffort = false,
        )
        assertEquals(listOf(10L), points.map { it.dateEpochDay })
    }

    @Test
    fun maxPrSeriesUsesHeaviestValidSetRegardlessOfReps() {
        // 极限 PR 不再只看 1 次：120kg×5 就是最重的一组成绩
        val points = TrendLogic.runningPrSeries(
            listOf(
                set("a", 100.0, 1, date = 10),
                set("b", 120.0, 5, date = 11),
                set("c", 110.0, 1, date = 12),
            ),
            maxEffort = true,
        )
        assertEquals(listOf(10L, 11L), points.map { it.dateEpochDay })
        assertEquals(100.0, points[0].value, 0.0)
        assertEquals(120.0, points[1].value, 0.0)
    }

    private fun measurement(
        day: Long,
        height: Double? = null,
        weight: Double? = null,
        fat: Double? = null,
        createdAt: Long = 1,
    ): BodyMeasurementEntity {
        return BodyMeasurementEntity(
            id = "$day-$createdAt",
            recordedEpochDay = day,
            heightCm = height,
            weightKg = weight,
            bodyFatPercent = fat,
            createdAt = createdAt,
        )
    }

    private fun set(
        sessionId: String,
        weightKg: Double?,
        reps: Int,
        date: Long,
    ): PerformedSet {
        return PerformedSet(
            sessionId = sessionId,
            dateEpochDay = date,
            weightKg = weightKg,
            reps = reps,
            weightMode = WeightMode.WEIGHTED.name,
        )
    }
}
