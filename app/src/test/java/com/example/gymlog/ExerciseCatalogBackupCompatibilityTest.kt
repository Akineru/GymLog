package com.example.gymlog

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.gymlog.data.BackupCodec
import com.example.gymlog.data.BackupRepository
import com.example.gymlog.data.ExerciseCatalogImporter
import com.example.gymlog.data.ExerciseEntity
import com.example.gymlog.data.ExerciseRepository
import com.example.gymlog.data.ExerciseSource
import com.example.gymlog.data.GymLogDatabase
import com.example.gymlog.data.PresetExercises
import com.example.gymlog.data.ProfileAvatarStore
import com.example.gymlog.data.WorkoutRepository
import com.example.gymlog.data.WorkoutSessionEntity
import com.example.gymlog.data.WorkoutSetEntity
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExerciseCatalogBackupCompatibilityTest {
    @Test
    fun v1AndV2BackupsBothReseedTheDatasetAndClearCanBeFollowedByRestore() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, GymLogDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val importer = ExerciseCatalogImporter(context, db)
            val repository = BackupRepository(
                db,
                db.backupDao(),
                ExerciseRepository(db.exerciseDao(), db.exerciseMuscleDao()),
                WorkoutRepository(db.sessionDao(), db.exerciseDao(), db.bodyRecordDao()),
                ProfileAvatarStore(context),
                exerciseCatalogImporter = importer,
            )
            db.exerciseDao().insertIgnore(PresetExercises.all(1L))
            db.exerciseDao().insertCustom(
                ExerciseEntity(
                    id = "custom_keep",
                    name = "旧备份保留",
                    bodyPart = "CHEST",
                    equipmentType = "FREE_WEIGHT",
                    weightMode = "WEIGHTED",
                    isCustom = true,
                    aliases = null,
                    createdAt = 2L,
                    source = ExerciseSource.CUSTOM,
                ),
            )
            db.sessionDao().replaceSession(
                WorkoutSessionEntity(id = "s1", dateEpochDay = 20_000, note = "旧", createdAt = 3L),
                listOf(
                    WorkoutSetEntity(
                        id = "set1",
                        sessionId = "s1",
                        exerciseId = "custom_keep",
                        exerciseOrder = 0,
                        setIndex = 0,
                        weightMode = "WEIGHTED",
                        weightKg = 20.0,
                        reps = 5,
                        note = null,
                    ),
                ),
            )
            val v2 = JSONObject(BackupCodec.encode(repository.exportSnapshot(), exportedAt = 1L))
            val v1 = JSONObject(v2.toString()).apply {
                put("version", 1)
                remove("timerRecords")
                remove("timerLaps")
                val exercises = getJSONArray("exercises")
                for (index in 0 until exercises.length()) {
                    val exercise = exercises.getJSONObject(index)
                    listOf(
                        "nameEn", "source", "sourceId", "equipmentDetail", "bodyPartDetail",
                        "bodyCategoryId", "equipmentCategoryId",
                        "classificationStatus", "classificationVersion",
                    ).forEach { exercise.remove(it) }
                }
            }

            repository.importSnapshot(BackupCodec.decode(v1.toString()))
            assertNotNull(db.exerciseDao().getById("custom_keep"))
            assertEquals(1, db.backupDao().allSets().size)
            assertEquals(1_324, db.exerciseCatalogDao().exerciseCountBySource(ExerciseSource.DATASET))
            assertNotNull(db.exerciseDao().getById("preset_barbell_bench_press"))
            assertNotNull(db.exerciseDao().getById("ds_0001"))

            repository.clearLocalData()
            repository.importSnapshot(BackupCodec.decode(v2.toString()))
            assertNotNull(db.exerciseDao().getById("custom_keep"))
            assertEquals(1, db.backupDao().allSets().size)
            assertEquals(1_324, db.exerciseCatalogDao().exerciseCountBySource(ExerciseSource.DATASET))
            assertNotNull(db.exerciseDao().getById("ds_0001"))
        } finally {
            db.close()
        }
    }
}
