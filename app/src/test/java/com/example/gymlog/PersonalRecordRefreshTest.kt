package com.example.gymlog

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.gymlog.data.BodyPart
import com.example.gymlog.data.EquipmentType
import com.example.gymlog.data.ExerciseEntity
import com.example.gymlog.data.GymLogDatabase
import com.example.gymlog.data.SessionExerciseDraft
import com.example.gymlog.data.SessionSetDraft
import com.example.gymlog.data.WorkoutRepository
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * 「刷新成绩」走的那条路：`WorkoutRepository.rebuildAllPersonalRecords()`。
 *
 * 需求 E 的核心：读的是**当下**的设置、重复点不会重复写、不动训练记录本身。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PersonalRecordRefreshTest {
    private lateinit var db: GymLogDatabase
    private lateinit var repository: WorkoutRepository
    private var minReps = 6

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, GymLogDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = WorkoutRepository(
            sessionDao = db.sessionDao(),
            exerciseDao = db.exerciseDao(),
            bodyRecordDao = db.bodyRecordDao(),
            workingPrMinRepsProvider = { minReps },
            database = db,
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun refreshReadsTheLatestThreshold() = runTest {
        val bench = exercise("bench-refresh")
        db.exerciseDao().insertIgnore(listOf(bench))
        val day = LocalDate.of(2026, 10, 11).toEpochDay()
        // 40×8 达到 6 次门槛；60×4 与 50×3 只有在门槛 ≤4 时才可能参与
        repository.saveSession(
            sessionId = null,
            dateEpochDay = day,
            note = "",
            exercises = listOf(
                SessionExerciseDraft(
                    exercise = bench,
                    sets = listOf(
                        setDraft("a", "40", "8"),
                        setDraft("b", "60", "4"),
                        setDraft("c", "50", "3"),
                    ),
                ),
            ),
        ).getOrThrow()

        val atSix = db.exerciseDao().getById("bench-refresh")!!
        assertEquals(40.0, atSix.workingPrWeightKg!!, 0.0)
        assertEquals(8, atSix.workingPrReps)
        // 极限 PR 与门槛无关：60kg×4 就是最重有效单组
        assertEquals(60.0, atSix.maxPrWeightKg!!, 0.0)

        // 用户把门槛改成 4，然后点「刷新成绩」
        minReps = 4
        val updated = repository.rebuildAllPersonalRecords()
        assertTrue("应当至少更新一个动作", updated >= 1)

        val atFour = db.exerciseDao().getById("bench-refresh")!!
        assertEquals(60.0, atFour.workingPrWeightKg!!, 0.0)
        assertEquals(4, atFour.workingPrReps)
        assertEquals(60.0, atFour.maxPrWeightKg!!, 0.0)

        // 门槛改回 6 再刷新：4 次的 60kg 不再参与做组 PR
        minReps = 6
        repository.rebuildAllPersonalRecords()
        val backToSix = db.exerciseDao().getById("bench-refresh")!!
        assertEquals(40.0, backToSix.workingPrWeightKg!!, 0.0)
        assertEquals(8, backToSix.workingPrReps)
    }

    @Test
    fun refreshIsIdempotentAndLeavesTrainingDataUntouched() = runTest {
        val bench = exercise("bench-idem")
        db.exerciseDao().insertIgnore(listOf(bench))
        val day = LocalDate.of(2026, 10, 11).toEpochDay()
        repository.saveSession(
            sessionId = null,
            dateEpochDay = day,
            note = "原始记录",
            exercises = listOf(
                SessionExerciseDraft(
                    exercise = bench,
                    sets = listOf(
                        setDraft("x", "60", "3"),
                        setDraft("y", "50", "8"),
                    ),
                ),
            ),
        ).getOrThrow()

        val setsBefore = db.sessionDao().getAllSetsForPr()
        val prBefore = db.exerciseDao().getById("bench-idem")!!

        val second = repository.rebuildAllPersonalRecords()
        // 第一次刷新（保存时已经算过）不需要再改任何动作
        assertEquals(0, second)

        val setsAfter = db.sessionDao().getAllSetsForPr()
        val prAfter = db.exerciseDao().getById("bench-idem")!!
        assertEquals(setsBefore.size, setsAfter.size)
        setsBefore.zip(setsAfter).forEach { (before, after) ->
            assertEquals(before.sessionId, after.sessionId)
            assertEquals(before.weightKg, after.weightKg)
            assertEquals(before.reps, after.reps)
            assertEquals(before.weightMode, after.weightMode)
            assertEquals(before.dateEpochDay, after.dateEpochDay)
        }
        assertEquals(prBefore.maxPrWeightKg, prAfter.maxPrWeightKg)
        assertEquals(prBefore.workingPrWeightKg, prAfter.workingPrWeightKg)
        assertEquals(prBefore.workingPrDateEpochDay, prAfter.workingPrDateEpochDay)
    }

    @Test
    fun refreshRebuildsPrForExercisesWhoseRuleChanged() = runTest {
        val bench = exercise("bench-rule")
        db.exerciseDao().insertIgnore(listOf(bench))
        repository.saveSession(
            sessionId = null,
            dateEpochDay = LocalDate.of(2026, 10, 11).toEpochDay(),
            note = "",
            exercises = listOf(
                SessionExerciseDraft(
                    exercise = bench,
                    sets = listOf(setDraft("only", "70", "3")),
                ),
            ),
        ).getOrThrow()

        // 旧规则留下的结果：做组 PR 只统计 ≥2 次，所以这里有值；
        // 新规则默认门槛 6，3 次不够 ⇒ 刷新后做组 PR 应被清空，极限 PR 保留 70。
        db.exerciseDao().updatePersonalRecords(
            id = "bench-rule",
            workingPrWeightKg = 70.0,
            workingPrReps = 3,
            workingPrWeightMode = "WEIGHTED",
            workingPrDateEpochDay = 1L,
            maxPrWeightKg = 70.0,
            maxPrReps = 3,
            maxPrWeightMode = "WEIGHTED",
            maxPrDateEpochDay = 1L,
        )

        assertEquals(1, repository.rebuildAllPersonalRecords())

        val refreshed = db.exerciseDao().getById("bench-rule")!!
        assertEquals(null, refreshed.workingPrWeightKg)
        assertNotNull(refreshed.maxPrWeightKg)
        assertEquals(70.0, refreshed.maxPrWeightKg!!, 0.0)
        assertEquals(3, refreshed.maxPrReps)
    }

    private fun exercise(id: String) = ExerciseEntity(
        id = id,
        name = id,
        bodyPart = BodyPart.CHEST.name,
        equipmentType = EquipmentType.FREE_WEIGHT.name,
        weightMode = com.example.gymlog.data.WeightMode.WEIGHTED.name,
        isCustom = false,
        aliases = null,
        createdAt = 1L,
    )

    private fun setDraft(id: String, weight: String, reps: String) = SessionSetDraft(
        id = id,
        mode = com.example.gymlog.data.WeightMode.WEIGHTED,
        normalKgText = weight,
        extraKgText = "",
        repsText = reps,
    )
}
