package com.example.gymlog

import com.example.gymlog.data.BodyMetrics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class BodyMetricsTest {
    @Test
    fun ageUsesBirthdayNotAStoredNumber() {
        val birthday = LocalDate.of(2000, 10, 11)
        assertEquals(26, BodyMetrics.ageYears(birthday, LocalDate.of(2026, 10, 11)))
        assertEquals(25, BodyMetrics.ageYears(birthday, LocalDate.of(2026, 10, 10)))
    }

    @Test
    fun futureBirthdayIsRejected() {
        assertNull(BodyMetrics.ageYears(LocalDate.of(2030, 1, 1), LocalDate.of(2026, 10, 11)))
    }

    @Test
    fun bmiUsesHeightAndWeight() {
        val bmi = BodyMetrics.bmi(175.0, 70.0)!!
        assertEquals(22.9, bmi, 0.05)
        assertEquals("22.9", BodyMetrics.displayBmi(bmi))
        assertNull(BodyMetrics.bmi(null, 70.0))
        assertNull(BodyMetrics.bmi(175.0, null))
    }

    @Test
    fun onlyMetricChangesCountAsTimedRecords() {
        val previous = com.example.gymlog.data.BodyMeasurementEntity(
            id = "1",
            recordedEpochDay = 1,
            heightCm = 175.0,
            weightKg = 70.0,
            bodyFatPercent = 15.0,
            createdAt = 1L,
        )
        assertEquals(
            false,
            BodyMetrics.metricsChanged(previous, 175.0, 70.0, 15.0, null, null),
        )
        assertEquals(
            true,
            BodyMetrics.metricsChanged(previous, 175.0, 71.0, 15.0, null, null),
        )
        assertEquals(
            true,
            BodyMetrics.metricsChanged(previous, 175.0, 70.0, 15.0, 80.0, null),
        )
        assertEquals(
            true,
            BodyMetrics.metricsChanged(previous, 175.0, 70.0, 15.0, null, 32.0),
        )
        assertEquals(
            false,
            BodyMetrics.metricsChanged(previous, null, null, null, null, null),
        )
    }
}
