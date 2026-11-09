package com.example.gymlog

import com.example.gymlog.data.PerformedSet
import com.example.gymlog.ui.stats.WEEKS_SHOWN
import com.example.gymlog.ui.stats.averageGapDays
import com.example.gymlog.ui.stats.sessionRows
import com.example.gymlog.ui.stats.weeklyActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

/** 训练足迹页下方的「练习频率」和「训练记录」。 */
class TrainingTrendHistoryTest {

    private fun set(sessionId: String, dateEpochDay: Long, weightKg: Double, reps: Int) =
        PerformedSet(
            sessionId = sessionId,
            dateEpochDay = dateEpochDay,
            weightKg = weightKg,
            reps = reps,
            weightMode = "WEIGHTED",
        )

    @Test
    fun `每次训练聚合成一行并按日期倒序`() {
        val rows = sessionRows(
            listOf(
                set("s1", 100L, 60.0, 10),
                set("s1", 100L, 65.0, 8),
                set("s2", 108L, 70.0, 5),
                set("s2", 108L, 60.0, 10),
            ),
        )

        assertEquals(2, rows.size)
        assertEquals("s2", rows[0].sessionId)
        assertEquals(108L, rows[0].dateEpochDay)
        assertEquals(2, rows[0].setCount)
        assertEquals(15, rows[0].totalReps)
        assertEquals(70.0, rows[0].bestWeightKg, 0.001)
        assertEquals(5, rows[0].bestReps)
        assertEquals(70.0 * 5 + 60.0 * 10, rows[0].volumeKg, 0.001)

        assertEquals("s1", rows[1].sessionId)
        assertEquals(65.0, rows[1].bestWeightKg, 0.001)
        assertEquals(8, rows[1].bestReps)
        assertEquals(60.0 * 10 + 65.0 * 8, rows[1].volumeKg, 0.001)
    }

    @Test
    fun `没有记录时返回空列表`() {
        assertEquals(emptyList<Any>(), sessionRows(emptyList()))
    }

    @Test
    fun `频率按周统计并包含本周`() {
        // 2026-11-08 是周日；本周从 11-02（周一）开始。
        val today = LocalDate.of(2026, 11, 8)
        val weeks = weeklyActivity(
            trainedDays = listOf(
                LocalDate.of(2026, 10, 28).toEpochDay(), // 上周，周三
                LocalDate.of(2026, 10, 29).toEpochDay(), // 上周，周四
                LocalDate.of(2026, 11, 2).toEpochDay(), // 本周，周一
                LocalDate.of(2026, 11, 8).toEpochDay(), // 本周，周日
                LocalDate.of(2026, 11, 8).toEpochDay(), // 同一天重复不应重复计数
            ),
            todayEpochDay = today.toEpochDay(),
        )

        assertEquals(WEEKS_SHOWN, weeks.size)
        assertEquals(LocalDate.of(2026, 11, 2), LocalDate.ofEpochDay(weeks.last().startEpochDay))
        assertEquals(2, weeks.last().days)
        assertEquals(2, weeks[weeks.lastIndex - 1].days)
        assertEquals(0, weeks.first().days)
    }

    @Test
    fun `窗口外的旧记录不进频率条`() {
        val today = LocalDate.of(2026, 11, 8)
        val weeks = weeklyActivity(
            trainedDays = listOf(LocalDate.of(2025, 1, 1).toEpochDay()),
            todayEpochDay = today.toEpochDay(),
        )
        assertEquals(0, weeks.sumOf { it.days })
    }

    @Test
    fun `平均间隔取相邻两次训练的天数`() {
        val days = listOf(0L, 6L, 12L)
        assertEquals(6, averageGapDays(days))
    }

    @Test
    fun `练不到两次没有平均间隔`() {
        assertNull(averageGapDays(emptyList()))
        assertNull(averageGapDays(listOf(5L)))
        assertNull(averageGapDays(listOf(5L, 5L)))
    }
}
