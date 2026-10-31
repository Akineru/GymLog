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
    fun sixOrMoreRepsBecomeWorkingPr() {
        val prs = PersonalRecordLogic.fromSessions(
            listOf(
                set("s1", weightKg = 80.0, reps = 8),
                set("s1", weightKg = 80.0, reps = 7),
                set("s1", weightKg = 75.0, reps = 8),
            ),
        )
        assertEquals(80.0, prs.working!!.weightKg!!, 0.0)
        assertEquals(8, prs.working!!.reps)
        // 新规则下 80×8 同时也是最重有效单组，极限 PR 不再为空
        assertEquals(80.0, prs.max!!.weightKg!!, 0.0)
    }

    @Test
    fun twoRepsNoLongerQualifyForWorkingPrButCountTowardsMaxPr() {
        val prs = PersonalRecordLogic.fromSessions(
            listOf(set("s1", weightKg = 120.0, reps = 2)),
        )
        // 默认门槛 6 次：2 次不够做组 PR
        assertNull(prs.working)
        // 但极限 PR 只看重量，120kg 就是纪录
        assertEquals(120.0, prs.max!!.weightKg!!, 0.0)
        assertEquals(2, prs.max!!.reps)
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
        // 极限 PR 只看重量；同为 110kg 时取最近一次达成的日期
        assertEquals(110.0, prs.max!!.weightKg!!, 0.0)
        assertEquals(12, prs.max!!.dateEpochDay)
        // 2 次不到默认门槛，做组 PR 只有一个 1 次组 ⇒ 空
        assertNull(prs.working)
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
        assertEquals("0 kg×20", PersonalRecordLogic.displayWorking(prs.working!!))
        assertEquals("2026.10.11", PersonalRecordLogic.formatDate(prs.working!!.dateEpochDay))
    }

    @Test
    fun maxPrOmitsRepsAndShowsDate() {
        val prs = PersonalRecordLogic.fromSessions(
            listOf(set("s1", weightKg = 100.0, reps = 1, date = jan1)),
        )
        assertEquals("100 kg", PersonalRecordLogic.displayMax(prs.max!!))
        assertEquals("2026.01.01", PersonalRecordLogic.formatDate(prs.max!!.dateEpochDay))
    }


    // ---- 新规则：极限 PR 只看最重有效单组，做组 PR 才看次数门槛 ----

    @Test
    fun a_maxPrIgnoresRepCount() {
        // 需求 A：40×12 / 50×8 / 60×3 ⇒ 极限 PR 60kg
        val prs = PersonalRecordLogic.fromSessions(
            listOf(
                set("s1", weightKg = 40.0, reps = 12),
                set("s1", weightKg = 50.0, reps = 8),
                set("s1", weightKg = 60.0, reps = 3),
            ),
        )
        assertEquals(60.0, prs.max!!.weightKg!!, 0.0)
        assertEquals(3, prs.max!!.reps)
        // 做组 PR 只能在 ≥6 次里挑，50×8 胜出
        assertEquals(50.0, prs.working!!.weightKg!!, 0.0)
        assertEquals(8, prs.working!!.reps)
    }

    @Test
    fun b_defaultThresholdIsSix() {
        // 需求 B：3/5 次不合格，6/8 次合格
        val prs = PersonalRecordLogic.fromSessions(
            listOf(
                set("s1", weightKg = 40.0, reps = 3),
                set("s2", weightKg = 45.0, reps = 5),
                set("s3", weightKg = 50.0, reps = 6),
                set("s4", weightKg = 55.0, reps = 8),
            ),
        )
        assertEquals(55.0, prs.working!!.weightKg!!, 0.0)
        assertEquals(8, prs.working!!.reps)
    }

    @Test
    fun c_lowerThresholdIncludesFourAndFiveReps() {
        // 需求 C：门槛改成 4 后，4/5 次也能参与
        val prs = PersonalRecordLogic.fromSessions(
            listOf(
                set("s1", weightKg = 40.0, reps = 3),
                set("s2", weightKg = 45.0, reps = 5),
                set("s3", weightKg = 50.0, reps = 4),
            ),
            minWorkingReps = 4,
        )
        assertEquals(50.0, prs.working!!.weightKg!!, 0.0)
        assertEquals(4, prs.working!!.reps)
    }

    @Test
    fun d_maxPrIsIndependentOfTheWorkingThreshold() {
        // 需求 D：门槛 4 和 6 下，极限 PR 必须都是 60kg
        val sets = listOf(
            set("s1", weightKg = 40.0, reps = 4),
            set("s2", weightKg = 50.0, reps = 3),
            set("s3", weightKg = 60.0, reps = 8),
        )
        val thresholdFour = PersonalRecordLogic.fromSessions(sets, minWorkingReps = 4)
        val thresholdSix = PersonalRecordLogic.fromSessions(sets, minWorkingReps = 6)
        assertEquals(60.0, thresholdFour.max!!.weightKg!!, 0.0)
        assertEquals(60.0, thresholdSix.max!!.weightKg!!, 0.0)
        assertEquals(thresholdFour.max!!, thresholdSix.max!!)
    }

    @Test
    fun thresholdIsClampedToValidRange() {
        // 计算层也做一次兜底：非法门槛被收敛到 1..30，不会让候选集合异常
        // 0 次会被收敛成 1 次，于是「1 次」的组也算做组 PR（而不是空集合或崩溃）
        assertEquals(
            1,
            PersonalRecordLogic.normalizeMinWorkingReps(0),
        )
        assertEquals(
            50.0,
            PersonalRecordLogic.fromSessions(listOf(set("s", 50.0, 1)), minWorkingReps = 0)
                .working!!.weightKg!!,
            0.0,
        )
        // 默认门槛 6 下同一个组不进做组 PR
        assertNull(PersonalRecordLogic.fromSessions(listOf(set("s", 50.0, 1))).working)
        assertEquals(
            PersonalRecordLogic.MIN_WORKING_REPS,
            PersonalRecordLogic.normalizeMinWorkingReps(-5),
        )
        assertEquals(
            PersonalRecordLogic.MAX_WORKING_REPS,
            PersonalRecordLogic.normalizeMinWorkingReps(999),
        )
    }

    @Test
    fun pureBodyweightSetsNeverBecomeMaxPr() {
        // 自重模式有效重量为 0，不能出现「极限 PR 0 kg」
        val prs = PersonalRecordLogic.fromSessions(
            listOf(
                set("s1", weightKg = null, reps = 3, mode = WeightMode.BODYWEIGHT),
                set("s2", weightKg = null, reps = 8, mode = WeightMode.BODYWEIGHT),
            ),
        )
        assertNull(prs.max)
        assertEquals(8, prs.working!!.reps)
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
