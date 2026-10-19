package com.example.gymlog

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.gymlog.data.BodyMeasurementEntity
import com.example.gymlog.data.BodyPart
import com.example.gymlog.data.EquipmentType
import com.example.gymlog.data.ExerciseEntity
import com.example.gymlog.data.GymLogDatabase
import com.example.gymlog.data.WeightMode
import com.example.gymlog.data.WorkoutRepository
import com.example.gymlog.data.WorkoutSessionEntity
import com.example.gymlog.data.WorkoutSetEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DatePartQueryTest {
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
    fun mapsDistinctPartsByDateAndIgnoresOutsideRange() = runTest {
        val inRangeStart = LocalDate.of(2026, 10, 10)
        val inRangeEnd = LocalDate.of(2026, 10, 11)
        val beforeRange = LocalDate.of(2026, 10, 9)
        val afterRange = LocalDate.of(2026, 10, 12)

        db.exerciseDao().insertIgnore(
            listOf(
                exercise("bench", BodyPart.CHEST),
                exercise("fly", BodyPart.CHEST),
                exercise("row", BodyPart.BACK),
                exercise("squat", BodyPart.LEGS),
                exercise("curl", BodyPart.ARMS),
                exercise("raise", BodyPart.SHOULDERS),
            ),
        )

        insertSession(
            sessionId = "day10",
            date = inRangeStart,
            exerciseIds = listOf("bench", "fly", "row", "bench"),
        )
        insertSession(
            sessionId = "day11",
            date = inRangeEnd,
            exerciseIds = listOf("squat"),
        )
        insertSession(
            sessionId = "day9",
            date = beforeRange,
            exerciseIds = listOf("raise"),
        )
        insertSession(
            sessionId = "day12",
            date = afterRange,
            exerciseIds = listOf("curl"),
        )

        val result = repository.observeTrainedPartsByDate(inRangeStart, inRangeEnd).first()

        assertEquals(setOf(inRangeStart, inRangeEnd), result.keys)
        assertEquals(listOf(BodyPart.CHEST.name, BodyPart.BACK.name), result[inRangeStart])
        assertEquals(listOf(BodyPart.LEGS.name), result[inRangeEnd])
        assertEquals(2, result.getValue(inRangeStart).distinct().size)
        assertFalse(result.containsKey(beforeRange))
        assertFalse(result.containsKey(afterRange))
    }

    @Test
    fun ordersPartsByVolumeSoCalendarUsesDominantColor() = runTest {
        val day = LocalDate.of(2026, 10, 13)
        db.exerciseDao().insertIgnore(
            listOf(
                exercise("bench", BodyPart.CHEST),
                exercise("squat", BodyPart.LEGS),
            ),
        )
        insertSession(
            sessionId = "chest-small",
            date = day,
            exerciseIds = listOf("bench"),
            weightKg = 100.0,
            reps = 1,
        )
        insertSession(
            sessionId = "legs-large",
            date = day,
            exerciseIds = listOf("squat"),
            weightKg = 125.0,
            reps = 4,
        )

        val result = repository.observeTrainedPartsByDate(day, day).first()
        assertEquals(listOf(BodyPart.LEGS.name, BodyPart.CHEST.name), result[day])
    }

    @Test
    fun bodyweightVolumeUsesRecordedWeightThenAddsTheExtraLoad() = runTest {
        val day = LocalDate.of(2026, 10, 14)
        db.exerciseDao().insertIgnore(
            listOf(
                exercise("bench", BodyPart.CHEST),
                exercise("pull", BodyPart.BACK),
            ),
        )
        db.bodyRecordDao().insertMeasurement(
            BodyMeasurementEntity(
                id = "weight",
                recordedEpochDay = day.toEpochDay(),
                heightCm = null,
                weightKg = 70.0,
                bodyFatPercent = null,
                createdAt = 1L,
            ),
        )
        insertSession(
            sessionId = "chest",
            date = day,
            exerciseIds = listOf("bench"),
            weightKg = 20.0,
            reps = 1,
        )
        insertSession(
            sessionId = "pull",
            date = day,
            exerciseIds = listOf("pull"),
            weightKg = -10.0,
            reps = 8,
            mode = WeightMode.BODYWEIGHT_PLUS,
        )

        val result = repository.observeTrainedPartsByDate(day, day).first()
        assertEquals(listOf(BodyPart.BACK.name, BodyPart.CHEST.name), result[day])
    }

    private suspend fun insertSession(
        sessionId: String,
        date: LocalDate,
        exerciseIds: List<String>,
        weightKg: Double? = 60.0,
        reps: Int = 8,
        mode: WeightMode = WeightMode.WEIGHTED,
    ) {
        db.sessionDao().replaceSession(
            WorkoutSessionEntity(
                id = sessionId,
                dateEpochDay = date.toEpochDay(),
                note = null,
                createdAt = 1L,
            ),
            exerciseIds.mapIndexed { index, exerciseId ->
                WorkoutSetEntity(
                    id = "${sessionId}_set_$index",
                    sessionId = sessionId,
                    exerciseId = exerciseId,
                    exerciseOrder = index,
                    setIndex = 1,
                    weightMode = mode.name,
                    weightKg = weightKg,
                    reps = reps,
                    note = null,
                )
            },
        )
    }

    private fun exercise(id: String, part: BodyPart): ExerciseEntity {
        return ExerciseEntity(
            id = id,
            name = id,
            bodyPart = part.name,
            equipmentType = EquipmentType.FREE_WEIGHT.name,
            weightMode = WeightMode.WEIGHTED.name,
            isCustom = false,
            aliases = null,
            createdAt = 1L,
        )
    }
}
