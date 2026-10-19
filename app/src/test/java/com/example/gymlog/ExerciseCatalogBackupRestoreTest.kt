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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExerciseCatalogBackupRestoreTest {
    @Test
    fun restoringAnOldBackupKeepsCustomDataAndReseedsTheDataset() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, GymLogDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val importer = ExerciseCatalogImporter(context, db)
            val exercises = ExerciseRepository(db.exerciseDao(), db.exerciseMuscleDao())
            val workouts = WorkoutRepository(db.sessionDao(), db.exerciseDao(), db.bodyRecordDao())
            val repository = BackupRepository(
                db,
                db.backupDao(),
                exercises,
                workouts,
                ProfileAvatarStore(context),
                exerciseCatalogImporter = importer,
            )
            db.exerciseDao().insertIgnore(PresetExercises.all(1L))
            db.exerciseDao().insertCustom(
                ExerciseEntity(
                    id = "custom_keep",
                    name = "自定义保留",
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
                WorkoutSessionEntity(id = "s1", dateEpochDay = 20_000, note = "旧备份", createdAt = 3L),
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

            val encoded = BackupCodec.encode(repository.exportSnapshot(), exportedAt = 1L)
            repository.importSnapshot(BackupCodec.decode(encoded))

            assertNotNull(db.exerciseDao().getById("custom_keep"))
            assertEquals("自定义保留", db.exerciseDao().getById("custom_keep")!!.name)
            assertEquals(1, db.backupDao().allSets().size)
            assertEquals("custom_keep", db.backupDao().allSets().single().exerciseId)
            assertEquals(1_324, db.exerciseCatalogDao().exerciseCountBySource(ExerciseSource.DATASET))
            assertEquals(1_324, db.exerciseCatalogDao().sourceMapCount(ExerciseSource.DATASET))
            assertNotNull(db.exerciseDao().getById("preset_barbell_bench_press"))
            assertNotNull(db.exerciseDao().getById("ds_0001"))
            assertTrue(exercises.observeExercises("", null, null).first().none { it.id.startsWith("preset_") })

            repository.clearLocalData()
            assertNull(db.exerciseDao().getById("custom_keep"))
            assertNull(db.exerciseDao().getById("preset_barbell_bench_press"))
            assertNotNull(db.exerciseDao().getById("ds_0001"))
            assertTrue(exercises.observeExercises("", null, null).first().none { it.id.startsWith("preset_") })
            assertEquals(1_324, db.exerciseCatalogDao().exerciseCountBySource(ExerciseSource.DATASET))
        } finally {
            db.close()
        }
    }
}
