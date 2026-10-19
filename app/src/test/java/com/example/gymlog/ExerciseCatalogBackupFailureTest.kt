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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExerciseCatalogBackupFailureTest {
    private class FailingImporter(
        context: Context,
        db: GymLogDatabase,
    ) : ExerciseCatalogImporter(context, db) {
        override suspend fun importIfNeeded(force: Boolean): ImportStats {
            throw IllegalStateException("INJECTED_IMPORT")
        }

        override suspend fun importIfNeededLocked(force: Boolean): ImportStats {
            throw IllegalStateException("INJECTED_IMPORT")
        }
    }

    private class FailingAvatarStore(context: Context) : ProfileAvatarStore(context) {
        override fun delete() {
            throw IllegalStateException("INJECTED_AVATAR")
        }
    }

    @Test
    fun failedRestoreRollsBackDatasetStateAndRetrySucceeds() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, GymLogDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val realImporter = ExerciseCatalogImporter(context, db)
            val exercises = ExerciseRepository(db.exerciseDao(), db.exerciseMuscleDao())
            val workouts = WorkoutRepository(db.sessionDao(), db.exerciseDao(), db.bodyRecordDao())
            val avatar = ProfileAvatarStore(context)
            val repository = BackupRepository(
                db,
                db.backupDao(),
                exercises,
                workouts,
                avatar,
                exerciseCatalogImporter = realImporter,
            )
            db.exerciseDao().insertIgnore(PresetExercises.all(1L))
            db.exerciseDao().insertCustom(
                ExerciseEntity(
                    id = "custom_keep",
                    name = "失败回滚保留",
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
            db.exerciseDao().updatePersonalRecords(
                id = "preset_deadlift",
                workingPrWeightKg = 100.0,
                workingPrReps = 5,
                workingPrWeightMode = "WEIGHTED",
                workingPrDateEpochDay = 20_000,
                maxPrWeightKg = 140.0,
                maxPrReps = 1,
                maxPrWeightMode = "WEIGHTED",
                maxPrDateEpochDay = 20_001,
            )
            db.exerciseDao().archive("preset_deadlift")
            realImporter.importIfNeeded()

            val before = datasetCounts(db)
            val beforeExercises = db.exerciseDao().allIds().toSet()
            val beforeLinks = db.exerciseMuscleDao().countAll()
            val encoded = BackupCodec.encode(repository.exportSnapshot(), exportedAt = 1L)

            val failing = BackupRepository(
                db,
                db.backupDao(),
                exercises,
                workouts,
                avatar,
                exerciseCatalogImporter = FailingImporter(context, db),
            )
            var failed = false
            try {
                failing.importSnapshot(BackupCodec.decode(encoded))
            } catch (error: IllegalStateException) {
                assertEquals("INJECTED_IMPORT", error.message)
                failed = true
            }
            assertTrue(failed)
            assertEquals(before, datasetCounts(db))
            assertEquals(beforeLinks, db.exerciseMuscleDao().countAll())
            assertEquals(beforeExercises, db.exerciseDao().allIds().toSet())
            db.openHelper.readableDatabase.query("PRAGMA foreign_key_check").use {
                assertFalse("orphan foreign key after rollback", it.moveToFirst())
            }
            assertNotNull(db.exerciseDao().getById("custom_keep"))
            assertEquals(1, db.backupDao().allSets().size)
            val deadlift = db.exerciseDao().getById("preset_deadlift")!!
            assertTrue(deadlift.archived)
            assertEquals(100.0, deadlift.workingPrWeightKg!!, 0.0)
            assertEquals(140.0, deadlift.maxPrWeightKg!!, 0.0)

            repository.importSnapshot(BackupCodec.decode(encoded))
            assertEquals(1_324, db.exerciseCatalogDao().exerciseCountBySource(ExerciseSource.DATASET))
            assertEquals(before, datasetCounts(db))
            assertNotNull(db.exerciseDao().getById("custom_keep"))
        } finally {
            db.close()
        }
    }

    @Test
    fun avatarFailureAlsoRollsBackTheDatasetState() = runBlocking {
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
                    id = "custom_avatar",
                    name = "头像失败保留",
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
                WorkoutSessionEntity(id = "s1", dateEpochDay = 20_000, note = null, createdAt = 3L),
                listOf(
                    WorkoutSetEntity(
                        id = "set1",
                        sessionId = "s1",
                        exerciseId = "custom_avatar",
                        exerciseOrder = 0,
                        setIndex = 0,
                        weightMode = "WEIGHTED",
                        weightKg = 20.0,
                        reps = 5,
                        note = null,
                    ),
                ),
            )
            importer.importIfNeeded()
            val before = datasetCounts(db)
            val encoded = BackupCodec.encode(repository.exportSnapshot(), exportedAt = 1L)
            val failing = BackupRepository(
                db,
                db.backupDao(),
                exercises,
                workouts,
                FailingAvatarStore(context),
                exerciseCatalogImporter = importer,
            )
            var failed = false
            try {
                failing.importSnapshot(BackupCodec.decode(encoded))
            } catch (error: IllegalStateException) {
                assertEquals("INJECTED_AVATAR", error.message)
                failed = true
            }
            assertTrue(failed)
            assertEquals(before, datasetCounts(db))
            assertNotNull(db.exerciseDao().getById("custom_avatar"))
            assertEquals(1, db.backupDao().allSets().size)
            db.openHelper.readableDatabase.query("PRAGMA foreign_key_check").use {
                assertFalse(it.moveToFirst())
            }
        } finally {
            db.close()
        }
    }

    private suspend fun datasetCounts(db: GymLogDatabase): List<Int> = listOf(
        db.exerciseCatalogDao().exerciseCountBySource(ExerciseSource.DATASET),
        db.exerciseCatalogDao().allContents().size,
        db.exerciseCatalogDao().allMedia().size,
        db.exerciseCatalogDao().allSourceMaps().size,
        db.exerciseCatalogDao().allMuscleTokens().size,
        db.exerciseCatalogDao().presetCandidates().size,
        db.exerciseCatalogDao().importState(ExerciseSource.DATASET)?.failed ?: -1,
    )
}
