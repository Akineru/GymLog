package com.example.gymlog

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.gymlog.data.BackupCodec
import com.example.gymlog.data.BackupRepository
import com.example.gymlog.data.ExerciseCatalogCoordinator
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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExerciseCatalogCoordinatorTest {
    private class BlockingImporter(
        context: Context,
        db: GymLogDatabase,
        coordinator: ExerciseCatalogCoordinator,
        private val started: CompletableDeferred<Unit>,
        private val release: CompletableDeferred<Unit>,
    ) : ExerciseCatalogImporter(context, db, coordinator) {
        override suspend fun importIfNeededLocked(force: Boolean): ImportStats {
            started.complete(Unit)
            release.await()
            return super.importIfNeededLocked(force)
        }
    }

    private class FailingImporter(
        context: Context,
        db: GymLogDatabase,
        coordinator: ExerciseCatalogCoordinator,
    ) : ExerciseCatalogImporter(context, db, coordinator) {
        override suspend fun importIfNeededLocked(force: Boolean): ImportStats {
            throw IllegalStateException("INJECTED_IMPORT")
        }
    }

    @Test
    fun concurrentImportsAndClearUseOneCoordinator() = runBlocking {
        withTimeout(15_000) {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val db = Room.inMemoryDatabaseBuilder(context, GymLogDatabase::class.java)
                .allowMainThreadQueries()
                .build()
            try {
                val coordinator = ExerciseCatalogCoordinator()
                val importer = ExerciseCatalogImporter(context, db, coordinator)
                val repository = BackupRepository(
                    db,
                    db.backupDao(),
                    ExerciseRepository(db.exerciseDao(), db.exerciseMuscleDao()),
                    WorkoutRepository(db.sessionDao(), db.exerciseDao(), db.bodyRecordDao()),
                    ProfileAvatarStore(context),
                    exerciseCatalogImporter = importer,
                    coordinator = coordinator,
                )
                val result = coroutineScope {
                    val first = async { importer.importIfNeeded() }
                    val second = async { importer.importIfNeeded() }
                    val clear = async { repository.clearLocalData() }
                    Triple(first.await(), second.await(), clear.await())
                }
                assertEquals(1, listOf(result.first, result.second).count { !it.alreadyImported })
                assertEquals(1_324, db.exerciseCatalogDao().exerciseCountBySource(ExerciseSource.DATASET))
                assertEquals(1_324, db.exerciseCatalogDao().sourceMapCount(ExerciseSource.DATASET))
                assertEquals(2_648, db.exerciseCatalogDao().allContents().size)
                assertEquals(1_324, db.exerciseCatalogDao().allMedia().size)
                assertEquals(5_229, db.exerciseCatalogDao().totalMuscleTokens())
                assertEquals(2_051, db.exerciseMuscleDao().countAll())
                assertEquals(89, db.exerciseCatalogDao().presetCandidates().size)
                val state = db.exerciseCatalogDao().importState(ExerciseSource.DATASET)
                assertEquals(0, state?.failed)
            } finally {
                db.close()
            }
        }
    }

    @Test
    fun clearFailureRestoresPreviousCatalogInsteadOfReportingSuccess() = runBlocking {
        withTimeout(15_000) {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val db = Room.inMemoryDatabaseBuilder(context, GymLogDatabase::class.java)
                .allowMainThreadQueries()
                .build()
            try {
                val coordinator = ExerciseCatalogCoordinator()
                val real = ExerciseCatalogImporter(context, db, coordinator)
                val exercises = ExerciseRepository(db.exerciseDao(), db.exerciseMuscleDao())
                val workouts = WorkoutRepository(db.sessionDao(), db.exerciseDao(), db.bodyRecordDao())
                db.exerciseDao().insertIgnore(PresetExercises.all(1L))
                db.exerciseDao().insertCustom(
                    ExerciseEntity(
                        id = "custom_clear",
                        name = "清除失败保留",
                        bodyPart = "CHEST",
                        equipmentType = "FREE_WEIGHT",
                        weightMode = "WEIGHTED",
                        isCustom = true,
                        aliases = null,
                        createdAt = 1L,
                        source = ExerciseSource.CUSTOM,
                    ),
                )
                real.importIfNeeded()
                val before = db.exerciseCatalogDao().exerciseCountBySource(ExerciseSource.DATASET)
                val failing = BackupRepository(
                    db,
                    db.backupDao(),
                    exercises,
                    workouts,
                    ProfileAvatarStore(context),
                    exerciseCatalogImporter = FailingImporter(context, db, coordinator),
                    coordinator = coordinator,
                )
                var failed = false
                try {
                    failing.clearLocalData()
                } catch (_: IllegalStateException) {
                    failed = true
                }
                assertTrue(failed)
                assertEquals(before, db.exerciseCatalogDao().exerciseCountBySource(ExerciseSource.DATASET))
                assertNotNull(db.exerciseDao().getById("custom_clear"))
                assertEquals(1_324, db.exerciseCatalogDao().exerciseCountBySource(ExerciseSource.DATASET))
                db.openHelper.readableDatabase.query("PRAGMA foreign_key_check").use {
                    assertFalse(it.moveToFirst())
                }
            } finally {
                db.close()
            }
        }
    }

    @Test
    fun importAndRestoreSerializeAndEndInOneConsistentCatalog() = runBlocking {
        withTimeout(15_000) {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val db = Room.inMemoryDatabaseBuilder(context, GymLogDatabase::class.java)
                .allowMainThreadQueries()
                .build()
            try {
                val coordinator = ExerciseCatalogCoordinator()
                val started = CompletableDeferred<Unit>()
                val release = CompletableDeferred<Unit>()
                val blocking = BlockingImporter(context, db, coordinator, started, release)
                val real = ExerciseCatalogImporter(context, db, coordinator)
                val repository = BackupRepository(
                    db,
                    db.backupDao(),
                    ExerciseRepository(db.exerciseDao(), db.exerciseMuscleDao()),
                    WorkoutRepository(db.sessionDao(), db.exerciseDao(), db.bodyRecordDao()),
                    ProfileAvatarStore(context),
                    exerciseCatalogImporter = real,
                    coordinator = coordinator,
                )
                db.exerciseDao().insertIgnore(PresetExercises.all(1L))
                db.exerciseDao().insertCustom(
                    ExerciseEntity(
                        id = "custom_keep",
                        name = "并发保留",
                        bodyPart = "CHEST",
                        equipmentType = "FREE_WEIGHT",
                        weightMode = "WEIGHTED",
                        isCustom = true,
                        aliases = null,
                        createdAt = 1L,
                        source = ExerciseSource.CUSTOM,
                    ),
                )
                db.sessionDao().replaceSession(
                    WorkoutSessionEntity(id = "s1", dateEpochDay = 20_000, note = null, createdAt = 1L),
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
                val importJob = async { blocking.importIfNeeded() }
                started.await()
                val restoreJob = async { repository.importSnapshot(BackupCodec.decode(encoded)) }
                release.complete(Unit)
                importJob.await()
                restoreJob.await()

                assertEquals(1_324, db.exerciseCatalogDao().exerciseCountBySource(ExerciseSource.DATASET))
                assertEquals(1_324, db.exerciseCatalogDao().sourceMapCount(ExerciseSource.DATASET))
                assertEquals(2_648, db.exerciseCatalogDao().allContents().size)
                assertEquals(1_324, db.exerciseCatalogDao().allMedia().size)
                assertEquals(5_229, db.exerciseCatalogDao().totalMuscleTokens())
                assertEquals(2_051, db.exerciseMuscleDao().countAll())
                assertEquals(0, db.exerciseCatalogDao().importState(ExerciseSource.DATASET)?.failed)
                assertNotNull(db.exerciseDao().getById("custom_keep"))
                assertEquals(1, db.backupDao().allSets().size)
                db.openHelper.readableDatabase.query("PRAGMA foreign_key_check").use {
                    assertFalse(it.moveToFirst())
                }
            } finally {
                db.close()
            }
        }
    }

    @Test
    fun failedRestoreDoesNotOverwriteConcurrentImport() = runBlocking {
        withTimeout(15_000) {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val db = Room.inMemoryDatabaseBuilder(context, GymLogDatabase::class.java)
                .allowMainThreadQueries()
                .build()
            try {
                val coordinator = ExerciseCatalogCoordinator()
                val started = CompletableDeferred<Unit>()
                val release = CompletableDeferred<Unit>()
                val blocking = BlockingImporter(context, db, coordinator, started, release)
                val failing = FailingImporter(context, db, coordinator)
                val repository = BackupRepository(
                    db,
                    db.backupDao(),
                    ExerciseRepository(db.exerciseDao(), db.exerciseMuscleDao()),
                    WorkoutRepository(db.sessionDao(), db.exerciseDao(), db.bodyRecordDao()),
                    ProfileAvatarStore(context),
                    exerciseCatalogImporter = failing,
                    coordinator = coordinator,
                )
                db.exerciseDao().insertIgnore(PresetExercises.all(1L))
                val encoded = BackupCodec.encode(repository.exportSnapshot(), exportedAt = 1L)
                val importJob = async { blocking.importIfNeeded() }
                started.await()
                val restoreJob = async {
                    runCatching { repository.importSnapshot(BackupCodec.decode(encoded)) }
                }
                release.complete(Unit)
                importJob.await()
                val failed = restoreJob.await()
                assertTrue(failed.isFailure)
                assertEquals(1_324, db.exerciseCatalogDao().exerciseCountBySource(ExerciseSource.DATASET))
                assertEquals(1_324, db.exerciseCatalogDao().sourceMapCount(ExerciseSource.DATASET))
                assertEquals(2_648, db.exerciseCatalogDao().allContents().size)
                assertEquals(1_324, db.exerciseCatalogDao().allMedia().size)
                assertEquals(5_229, db.exerciseCatalogDao().totalMuscleTokens())
                assertEquals(2_051, db.exerciseMuscleDao().countAll())
                assertEquals(0, db.exerciseCatalogDao().importState(ExerciseSource.DATASET)?.failed)
            } finally {
                db.close()
            }
        }
    }
}
