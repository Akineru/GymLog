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
import com.example.gymlog.data.WeightMode
import com.example.gymlog.data.WorkoutRepository
import com.example.gymlog.data.WorkoutSessionEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PersonalRecordUpdateTest {
    private lateinit var db: GymLogDatabase
    private lateinit var repository: WorkoutRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, GymLogDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = WorkoutRepository(db.sessionDao(), db.exerciseDao(), db.bodyRecordDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun savingSessionPersistsDuration() = runTest {
        val bench = exercise("bench-duration")
        db.exerciseDao().insertIgnore(listOf(bench))
        val saved = repository.saveSession(
            sessionId = null,
            dateEpochDay = LocalDate.of(2026, 10, 11).toEpochDay(),
            note = "计时",
            exercises = listOf(
                SessionExerciseDraft(
                    exercise = bench,
                    sets = listOf(setDraft("d1", "50", "10")),
                ),
            ),
            durationMillis = 123_456L,
        )
        val id = saved.getOrThrow()
        assertEquals(123_456L, repository.loadSession(id)?.session?.durationMillis)
    }

    @Test
    fun savingSetsUpdatesExerciseLibraryPrs() = runTest {
        val bench = exercise("bench")
        db.exerciseDao().insertIgnore(listOf(bench))

        val day = LocalDate.of(2026, 10, 11).toEpochDay()
        val saved = repository.saveSession(
            sessionId = null,
            dateEpochDay = day,
            note = "",
            exercises = listOf(
                SessionExerciseDraft(
                    exercise = bench,
                    sets = listOf(setDraft("m1", "100", "1")),
                ),
            ),
        )
        assertEquals(true, saved.isSuccess)

        val afterMax = db.exerciseDao().getById("bench")!!
        assertEquals(100.0, afterMax.maxPrWeightKg!!, 0.0)
        assertEquals(1, afterMax.maxPrReps)
        assertNull(afterMax.workingPrWeightKg)

        repository.saveSession(
            sessionId = null,
            dateEpochDay = day,
            note = "",
            exercises = listOf(
                SessionExerciseDraft(
                    exercise = bench,
                    sets = listOf(
                        setDraft("w1", "80", "8"),
                        setDraft("w2", "80", "8"),
                        setDraft("w3", "80", "7"),
                    ),
                ),
            ),
        )

        val afterBoth = db.exerciseDao().getById("bench")!!
        assertEquals(100.0, afterBoth.maxPrWeightKg!!, 0.0)
        assertEquals(80.0, afterBoth.workingPrWeightKg!!, 0.0)
        assertEquals(8, afterBoth.workingPrReps)

        repository.saveSession(
            sessionId = null,
            dateEpochDay = day,
            note = "",
            exercises = listOf(
                SessionExerciseDraft(
                    exercise = bench,
                    sets = listOf(setDraft("two", "90", "2")),
                ),
            ),
        )
        val afterTwoReps = db.exerciseDao().getById("bench")!!
        assertEquals(100.0, afterTwoReps.maxPrWeightKg!!, 0.0)
        assertEquals(90.0, afterTwoReps.workingPrWeightKg!!, 0.0)
        assertEquals(2, afterTwoReps.workingPrReps)

        repository.saveSession(
            sessionId = null,
            dateEpochDay = day,
            note = "",
            exercises = listOf(
                SessionExerciseDraft(
                    exercise = bench,
                    sets = listOf(
                        setDraft("low-work", "70", "6"),
                        setDraft("low-max", "90", "1"),
                    ),
                ),
            ),
        )
        val afterLower = db.exerciseDao().getById("bench")!!
        assertEquals(100.0, afterLower.maxPrWeightKg!!, 0.0)
        assertEquals(90.0, afterLower.workingPrWeightKg!!, 0.0)
        assertEquals(2, afterLower.workingPrReps)
    }

    @Test
    fun deletingTheRecordThatSetPrFallsBackToTheNextBest() = runTest {
        val bench = exercise("bench")
        db.exerciseDao().insertIgnore(listOf(bench))
        val day = LocalDate.of(2026, 10, 11).toEpochDay()
        val bestId = repository.saveSession(
            sessionId = null,
            dateEpochDay = day,
            note = "",
            exercises = listOf(
                SessionExerciseDraft(
                    exercise = bench,
                    sets = listOf(setDraft("best", "100", "1")),
                ),
            ),
        ).getOrThrow()
        repository.saveSession(
            sessionId = null,
            dateEpochDay = day,
            note = "",
            exercises = listOf(
                SessionExerciseDraft(
                    exercise = bench,
                    sets = listOf(setDraft("next", "80", "8")),
                ),
            ),
        )

        repository.deleteSession(
            WorkoutSessionEntity(
                id = bestId,
                dateEpochDay = day,
                note = null,
                createdAt = 1L,
            ),
        )

        val afterDelete = db.exerciseDao().getById("bench")!!
        assertNull(afterDelete.maxPrWeightKg)
        assertNull(afterDelete.maxPrReps)
        assertEquals(80.0, afterDelete.workingPrWeightKg!!, 0.0)
        assertEquals(8, afterDelete.workingPrReps)
    }

    @Test
    fun equalPrUsesTheLatestDateEvenWhenTheNewerSessionWasSavedEarlier() = runTest {
        val bench = exercise("bench")
        db.exerciseDao().insertIgnore(listOf(bench))
        val earlier = LocalDate.of(2026, 10, 5).toEpochDay()
        val later = LocalDate.of(2026, 10, 20).toEpochDay()

        repository.saveSession(
            sessionId = null,
            dateEpochDay = earlier,
            note = "",
            exercises = listOf(
                SessionExerciseDraft(
                    exercise = bench,
                    sets = listOf(setDraft("early", "100", "1")),
                ),
            ),
        )
        repository.saveSession(
            sessionId = null,
            dateEpochDay = later,
            note = "",
            exercises = listOf(
                SessionExerciseDraft(
                    exercise = bench,
                    sets = listOf(setDraft("late", "100", "1")),
                ),
            ),
        )

        val after = db.exerciseDao().getById("bench")!!
        assertEquals(later, after.maxPrDateEpochDay)
        assertEquals(100.0, after.maxPrWeightKg!!, 0.0)
    }

    @Test
    fun backdatedEqualPrDoesNotReplaceTheLatestDate() = runTest {
        val bench = exercise("bench")
        db.exerciseDao().insertIgnore(listOf(bench))
        val later = LocalDate.of(2026, 10, 20).toEpochDay()

        repository.saveSession(
            sessionId = null,
            dateEpochDay = later,
            note = "",
            exercises = listOf(
                SessionExerciseDraft(
                    exercise = bench,
                    sets = listOf(setDraft("late", "100", "1")),
                ),
            ),
        )
        repository.saveSession(
            sessionId = null,
            dateEpochDay = LocalDate.of(2026, 10, 5).toEpochDay(),
            note = "",
            exercises = listOf(
                SessionExerciseDraft(
                    exercise = bench,
                    sets = listOf(setDraft("backdated", "100", "1")),
                ),
            ),
        )

        assertEquals(later, db.exerciseDao().getById("bench")!!.maxPrDateEpochDay)
    }

    @Test
    fun latestSetForExerciseIsTheLastSetOfTheNewestSession() = runTest {
        val bench = exercise("bench")
        db.exerciseDao().insertIgnore(listOf(bench))
        repository.saveSession(
            sessionId = null,
            dateEpochDay = LocalDate.of(2026, 10, 1).toEpochDay(),
            note = "",
            exercises = listOf(
                SessionExerciseDraft(
                    exercise = bench,
                    sets = listOf(setDraft("old-1", "60", "5"), setDraft("old-2", "70", "6")),
                ),
            ),
        )
        repository.saveSession(
            sessionId = null,
            dateEpochDay = LocalDate.of(2026, 10, 11).toEpochDay(),
            note = "",
            exercises = listOf(
                SessionExerciseDraft(
                    exercise = bench,
                    sets = listOf(setDraft("new-1", "80", "8"), setDraft("new-2", "90", "3")),
                ),
            ),
        )

        val latest = repository.latestSetForExercise("bench")!!
        assertEquals(90.0, latest.weightKg!!, 0.0)
        assertEquals(3, latest.reps)
    }

    private fun setDraft(id: String, kg: String, reps: String): SessionSetDraft {
        return SessionSetDraft(
            id = id,
            mode = WeightMode.WEIGHTED,
            normalKgText = kg,
            extraKgText = "",
            repsText = reps,
        )
    }

    private fun exercise(id: String): ExerciseEntity {
        return ExerciseEntity(
            id = id,
            name = id,
            bodyPart = BodyPart.CHEST.name,
            equipmentType = EquipmentType.FREE_WEIGHT.name,
            weightMode = WeightMode.WEIGHTED.name,
            isCustom = false,
            aliases = null,
            createdAt = 1L,
        )
    }
}
