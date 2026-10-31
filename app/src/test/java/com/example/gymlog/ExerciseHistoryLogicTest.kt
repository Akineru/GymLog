package com.example.gymlog

import com.example.gymlog.data.BodyMeasurementEntity
import com.example.gymlog.data.ExerciseHistoryLogic
import com.example.gymlog.data.ExerciseHistorySetRow
import com.example.gymlog.data.WeightMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 动作详情页「历史记录」的分组与统计口径。
 *
 * 容量必须与统计页、日历配色一致：`有效重量 × 次数`，自重按当天体重换算。
 * 组数沿用「有几组算几组」，各组次数不一致时不能合并成同一个数字。
 */
class ExerciseHistoryLogicTest {

    private fun row(
        sessionId: String,
        date: Long,
        setIndex: Int,
        weightKg: Double?,
        reps: Int?,
        mode: WeightMode = WeightMode.WEIGHTED,
        createdAt: Long = 0L,
    ) = ExerciseHistorySetRow(
        sessionId = sessionId,
        dateEpochDay = date,
        createdAt = createdAt,
        setIndex = setIndex,
        weightMode = mode.name,
        weightKg = weightKg,
        reps = reps,
    )

    private fun measurement(day: Long, weightKg: Double) = BodyMeasurementEntity(
        id = "m-$day",
        recordedEpochDay = day,
        heightCm = 175.0,
        weightKg = weightKg,
        bodyFatPercent = 15.0,
        createdAt = 1L,
    )

    @Test
    fun groupsBySessionAndSortsNewestFirst() {
        val entries = ExerciseHistoryLogic.entries(
            rows = listOf(
                row("s1", date = 10, setIndex = 0, weightKg = 60.0, reps = 8),
                row("s2", date = 12, setIndex = 0, weightKg = 60.0, reps = 8),
                row("s1", date = 10, setIndex = 1, weightKg = 60.0, reps = 8),
            ),
            measurements = emptyList(),
        )
        assertEquals(listOf("s2", "s1"), entries.map { it.sessionId })
        assertEquals(2, entries[1].setCount)
    }

    @Test
    fun sameDaySessionsSortByCreatedAtDescending() {
        val entries = ExerciseHistoryLogic.entries(
            rows = listOf(
                row("old", date = 10, createdAt = 100, setIndex = 0, weightKg = 50.0, reps = 5),
                row("new", date = 10, createdAt = 200, setIndex = 0, weightKg = 50.0, reps = 5),
            ),
            measurements = emptyList(),
        )
        assertEquals(listOf("new", "old"), entries.map { it.sessionId })
    }

    @Test
    fun volumeMatchesWeightTimesReps() {
        val entries = ExerciseHistoryLogic.entries(
            rows = listOf(
                row("s1", date = 10, setIndex = 0, weightKg = 60.0, reps = 8),
                row("s1", date = 10, setIndex = 1, weightKg = 80.0, reps = 5),
            ),
            measurements = emptyList(),
        )
        // 60×8 + 80×5 = 480 + 400
        assertEquals(880.0, entries.single().volumeKg, 0.0001)
    }

    @Test
    fun bodyweightSetsUseBodyWeightOfThatDay() {
        val entries = ExerciseHistoryLogic.entries(
            rows = listOf(
                row(
                    "s1",
                    date = 10,
                    setIndex = 0,
                    mode = WeightMode.BODYWEIGHT,
                    weightKg = null,
                    reps = 10,
                ),
            ),
            measurements = listOf(measurement(day = 8, weightKg = 70.0)),
        )
        assertEquals(700.0, entries.single().volumeKg, 0.0001)
    }

    @Test
    fun weightedBodyweightAddsExtraLoad() {
        val entries = ExerciseHistoryLogic.entries(
            rows = listOf(
                row(
                    "s1",
                    date = 10,
                    setIndex = 0,
                    mode = WeightMode.BODYWEIGHT_PLUS,
                    weightKg = 20.0,
                    reps = 8,
                ),
            ),
            measurements = listOf(measurement(day = 9, weightKg = 70.0)),
        )
        // (70 + 20) × 8
        assertEquals(720.0, entries.single().volumeKg, 0.0001)
    }

    @Test
    fun uniformRepsIsNullWhenSetsDiffer() {
        val uniform = ExerciseHistoryLogic.entries(
            rows = listOf(
                row("s1", date = 10, setIndex = 0, weightKg = 60.0, reps = 8),
                row("s1", date = 10, setIndex = 1, weightKg = 60.0, reps = 8),
            ),
            measurements = emptyList(),
        ).single()
        assertEquals(8, uniform.uniformReps)

        val mixed = ExerciseHistoryLogic.entries(
            rows = listOf(
                row("s1", date = 10, setIndex = 0, weightKg = 60.0, reps = 8),
                row("s1", date = 10, setIndex = 1, weightKg = 60.0, reps = 6),
            ),
            measurements = emptyList(),
        ).single()
        assertNull(mixed.uniformReps)
    }

    @Test
    fun missingRepsMakesTheSummaryNonUniform() {
        val entry = ExerciseHistoryLogic.entries(
            rows = listOf(
                row("s1", date = 10, setIndex = 0, weightKg = 60.0, reps = 8),
                row("s1", date = 10, setIndex = 1, weightKg = 60.0, reps = null),
            ),
            measurements = emptyList(),
        ).single()
        // 有一组没记次数就不能说「× 8 次」，否则会误报
        assertNull(entry.uniformReps)
        assertEquals(2, entry.setCount)
    }

    @Test
    fun setsAreOrderedBySetIndex() {
        val entry = ExerciseHistoryLogic.entries(
            rows = listOf(
                row("s1", date = 10, setIndex = 2, weightKg = 70.0, reps = 5),
                row("s1", date = 10, setIndex = 0, weightKg = 60.0, reps = 8),
                row("s1", date = 10, setIndex = 1, weightKg = 65.0, reps = 6),
            ),
            measurements = emptyList(),
        ).single()
        assertEquals(listOf(0, 1, 2), entry.sets.map { it.setIndex })
        assertEquals(listOf(60.0, 65.0, 70.0), entry.sets.map { it.storedWeightKg })
    }

    @Test
    fun emptyRowsProduceNoEntries() {
        assertTrue(ExerciseHistoryLogic.entries(emptyList(), emptyList()).isEmpty())
    }
}
