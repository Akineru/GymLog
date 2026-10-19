package com.example.gymlog

import com.example.gymlog.data.PerformedSet
import com.example.gymlog.data.PersonalRecordLogic
import com.example.gymlog.data.WeightMode
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PersonalRecordLogicTest {
    @Test
    fun oneRepBecomesMaxPr() {
        val prs = PersonalRecordLogic.fromSessions(
            listOf(set("s1", weightKg = 100.0, reps = 1)),
        )
        assertEquals(100.0, prs.max!!.weightKg!!, 0.0)
        assertEquals(1, prs.max!!.reps)
        assertNull(prs.working)
    }

    @Test
    fun twoOrMoreRepsBecomeWorkingPr() {
        val prs = PersonalRecordLogic.fromSessions(
            listOf(
                set("s1", weightKg = 80.0, reps = 8),
                set("s1", weightKg = 80.0, reps = 7),
                set("s1", weightKg = 75.0, reps = 8),
            ),
        )
        assertEquals(80.0, prs.working!!.weightKg!!, 0.0)
        assertEquals(8, prs.working!!.reps)
        assertNull(prs.max)
    }

    @Test
    fun twoRepsCountAsWorkingPr() {
        val prs = PersonalRecordLogic.fromSessions(
            listOf(set("s1", weightKg = 120.0, reps = 2)),
        )
        assertEquals(120.0, prs.working!!.weightKg!!, 0.0)
        assertEquals(2, prs.working!!.reps)
        assertNull(prs.max)
    }

    @Test
    fun sameSessionCanUpdateBothPrTypes() {
        val prs = PersonalRecordLogic.fromSessions(
            listOf(
                set("s1", weightKg = 100.0, reps = 1),
                set("s1", weightKg = 80.0, reps = 8),
            ),
        )
        assertEquals(100.0, prs.max!!.weightKg!!, 0.0)
        assertEquals(80.0, prs.working!!.weightKg!!, 0.0)
        assertEquals(8, prs.working!!.reps)
    }

    @Test
    fun heavierMaxPrWins() {
        val prs = PersonalRecordLogic.fromSessions(
            listOf(
                set("s1", weightKg = 100.0, reps = 1, date = 10),
                set("s2", weightKg = 110.0, reps = 1, date = 11),
                set("s3", weightKg = 110.0, reps = 2, date = 12),
            ),
        )
        assertEquals(110.0, prs.max!!.weightKg!!, 0.0)
        assertEquals(1, prs.max!!.reps)
        assertEquals(11, prs.max!!.dateEpochDay)
        assertEquals(110.0, prs.working!!.weightKg!!, 0.0)
        assertEquals(2, prs.working!!.reps)
    }

    @Test
    fun equalPrKeepsLatestDate() {
        val prs = PersonalRecordLogic.fromSessions(
            listOf(
                set("s1", weightKg = 100.0, reps = 1, date = 5),
                set("s2", weightKg = 100.0, reps = 1, date = 20),
            ),
        )
        assertEquals(20, prs.max!!.dateEpochDay)
    }

    @Test
    fun equalPrKeepsLatestDateRegardlessOfInputOrder() {
        val prs = PersonalRecordLogic.fromSessions(
            listOf(
                set("later", weightKg = 80.0, reps = 8, date = 20),
                set("earlier", weightKg = 80.0, reps = 8, date = 5),
            ),
        )
        assertEquals(20, prs.working!!.dateEpochDay)
    }

    @Test
    fun equalWeightWithMoreRepsStillWinsOverLatestDate() {
        val prs = PersonalRecordLogic.fromSessions(
            listOf(
                set("more-reps", weightKg = 100.0, reps = 8, date = 5),
                set("later", weightKg = 100.0, reps = 7, date = 20),
            ),
        )
        assertEquals(8, prs.working!!.reps)
        assertEquals(5, prs.working!!.dateEpochDay)
    }

    @Test
    fun bodyweightWorkingPrUsesReps() {
        val prs = PersonalRecordLogic.fromSessions(
            listOf(
                set("s1", weightKg = null, reps = 12, date = oct11, mode = WeightMode.BODYWEIGHT),
                set("s2", weightKg = null, reps = 20, date = oct11, mode = WeightMode.BODYWEIGHT),
            ),
        )
        assertNull(prs.max)
        assertEquals(20, prs.working!!.reps)
        assertEquals("0 kg×20 · 2026年10月11日", PersonalRecordLogic.displayWorking(prs.working!!))
    }

    @Test
    fun maxPrOmitsRepsAndShowsDate() {
        val prs = PersonalRecordLogic.fromSessions(
            listOf(set("s1", weightKg = 100.0, reps = 1, date = jan1)),
        )
        assertEquals("100 kg · 2026年1月1日", PersonalRecordLogic.displayMax(prs.max!!))
    }

    private val oct11 = LocalDate.of(2026, 10, 11).toEpochDay()
    private val jan1 = LocalDate.of(2026, 1, 1).toEpochDay()

    private fun set(
        sessionId: String,
        weightKg: Double?,
        reps: Int,
        date: Long = 1,
        mode: WeightMode = WeightMode.WEIGHTED,
    ): PerformedSet {
        return PerformedSet(
            sessionId = sessionId,
            dateEpochDay = date,
            weightKg = weightKg,
            reps = reps,
            weightMode = mode.name,
        )
    }
}
