package com.example.gymlog

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.gymlog.data.BackupRepository
import com.example.gymlog.data.DatasetMappingStatus
import com.example.gymlog.data.DatasetMuscleMapping
import com.example.gymlog.data.ExerciseCatalogImporter
import com.example.gymlog.data.ExerciseEntity
import com.example.gymlog.data.ExerciseMuscleEntity
import com.example.gymlog.data.ExerciseRepository
import com.example.gymlog.data.ExerciseSource
import com.example.gymlog.data.GymLogDatabase
import com.example.gymlog.data.PresetExercises
import com.example.gymlog.data.ProfileAvatarStore
import com.example.gymlog.data.WorkoutRepository
import com.example.gymlog.data.WorkoutSessionEntity
import com.example.gymlog.data.WorkoutSetEntity
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExerciseCatalogImportTest {
    private lateinit var context: Context
    private lateinit var db: GymLogDatabase
    private lateinit var importer: ExerciseCatalogImporter

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, GymLogDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        importer = ExerciseCatalogImporter(context, db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun importsActualDatasetAndRegistersMediaWithoutBundlingFiles() = runBlocking {
        val stats = importer.importIfNeeded()

        assertEquals(1_324, stats.added)
        assertEquals(0, stats.linked)
        assertEquals(0, stats.skipped)
        assertEquals(24, stats.pendingMatches)
        assertEquals(2_476, stats.unmappedTokens)
        assertEquals(0, stats.failed)
        val state = db.exerciseCatalogDao().importState(ExerciseSource.DATASET)
        assertNotNull(state)
        assertEquals(1_324, state!!.recordCount)
        assertEquals(1_324, db.exerciseCatalogDao().exerciseCountBySource(ExerciseSource.DATASET))
        assertEquals(1_324, db.exerciseCatalogDao().sourceMapCount(ExerciseSource.DATASET))
        assertEquals(89, db.exerciseCatalogDao().presetCandidates().size)
        assertEquals(5_229, db.exerciseCatalogDao().totalMuscleTokens())
        assertEquals(2_051, db.exerciseMuscleDao().countAll())

        val first = db.exerciseDao().getById("ds_0001")
        assertNotNull(first)
        assertEquals("0001", first!!.sourceId)
        assertEquals(ExerciseSource.DATASET, first.source)
        assertEquals("waist", first.bodyPartDetail)
        assertEquals("body weight", first.equipmentDetail)
        assertEquals("3/4 sit-up", first.nameEn)
        assertEquals(2, db.exerciseCatalogDao().contentCount("ds_0001"))
        assertEquals(1, db.exerciseCatalogDao().mediaCount("ds_0001"))
        assertTrue(db.exerciseCatalogDao().muscleTokenCount("ds_0001") > 0)
        val media = db.exerciseCatalogDao().mediaForExercise("ds_0001")!!
        assertEquals("images/0001-2gPfomN.jpg", media.sourceImagePath)
        assertEquals("videos/0001-2gPfomN.gif", media.sourceGifPath)
        assertEquals(com.example.gymlog.data.ExerciseMediaStatus.NOT_BUNDLED, media.downloadState)
        assertTrue(db.exerciseCatalogDao().contentForExercise("ds_0001").any { it.locale == "zh" && !it.instructions.isNullOrBlank() })

        // A duplicate English name is kept as two distinct dataset rows.
        assertNotNull(db.exerciseDao().getById("ds_0088"))
        assertNotNull(db.exerciseDao().getById("ds_1371"))
        assertEquals("barbell seated calf raise", db.exerciseDao().getById("ds_0088")!!.nameEn)
        assertEquals("barbell seated calf raise", db.exerciseDao().getById("ds_1371")!!.nameEn)

        // An old preset row with the same semantic action is not modified or
        // auto-linked; the dataset row is separate.
        db.exerciseDao().insertIgnore(
            listOf(
                ExerciseEntity(
                    id = "preset_barbell_bench_press",
                    name = "杠铃卧推",
                    bodyPart = "CHEST",
                    equipmentType = "FREE_WEIGHT",
                    weightMode = "WEIGHTED",
                    isCustom = false,
                    aliases = "bench press",
                    createdAt = 1L,
                ),
            ),
        )
        assertNotNull(db.exerciseDao().getById("preset_barbell_bench_press"))
        assertNotNull(db.exerciseDao().getById("ds_0025"))
        assertEquals("preset_barbell_bench_press", db.exerciseDao().getById("preset_barbell_bench_press")!!.id)
    }

    @Test
    fun importIsIdempotentAndDoesNotDuplicateRows() = runBlocking {
        importer.importIfNeeded()
        val afterFirst = db.exerciseCatalogDao().exerciseCountBySource(ExerciseSource.DATASET)
        val second = importer.importIfNeeded()

        assertTrue(second.alreadyImported)
        assertEquals(afterFirst, db.exerciseCatalogDao().exerciseCountBySource(ExerciseSource.DATASET))
        assertEquals(1_324, db.exerciseCatalogDao().sourceMapCount(ExerciseSource.DATASET))
    }

    @Test
    fun importDoesNotTouchLegacyIdsArchivePrOrTrainingSets() = runBlocking {
        val presets = PresetExercises.all(1L)
        val archived = presets.first().copy(archived = true)
        db.exerciseDao().insertIgnore(presets.drop(1) + archived)
        db.exerciseDao().updatePersonalRecords(
            id = archived.id,
            workingPrWeightKg = 80.0,
            workingPrReps = 5,
            workingPrWeightMode = "WEIGHTED",
            workingPrDateEpochDay = 20_000,
            maxPrWeightKg = 100.0,
            maxPrReps = 1,
            maxPrWeightMode = "WEIGHTED",
            maxPrDateEpochDay = 20_001,
        )
        db.sessionDao().replaceSession(
            WorkoutSessionEntity(id = "s1", dateEpochDay = 20_000, note = null, createdAt = 1L),
            listOf(
                WorkoutSetEntity(
                    id = "set1",
                    sessionId = "s1",
                    exerciseId = archived.id,
                    exerciseOrder = 0,
                    setIndex = 0,
                    weightMode = "WEIGHTED",
                    weightKg = 50.0,
                    reps = 5,
                    note = null,
                ),
            ),
        )

        importer.importIfNeeded()

        val kept = db.exerciseDao().getById(archived.id)
        assertNotNull(kept)
        assertEquals(ExerciseSource.LEGACY, kept!!.source)
        assertTrue(kept.archived)
        assertEquals(80.0, kept.workingPrWeightKg!!, 0.0)
        assertEquals(100.0, kept.maxPrWeightKg!!, 0.0)
        assertEquals(1, db.backupDao().allSets().size)
        assertEquals(archived.id, db.backupDao().allSets().single().exerciseId)
        val legacyIds = db.exerciseDao().allIds().filter { it.startsWith("preset_") }
        assertEquals(89, legacyIds.size)
        assertTrue(legacyIds.all { db.exerciseDao().getById(it)?.source == ExerciseSource.LEGACY })
    }

    @Test
    fun importDoesNotOverwriteCustomExercise() = runBlocking {
        db.exerciseDao().insertCustom(
            ExerciseEntity(
                id = "custom_keep",
                name = "保留动作",
                bodyPart = "CHEST",
                equipmentType = "FREE_WEIGHT",
                weightMode = "WEIGHTED",
                isCustom = true,
                aliases = null,
                createdAt = 1L,
                source = ExerciseSource.CUSTOM,
            ),
        )
        importer.importIfNeeded()
        val row = db.exerciseDao().getById("custom_keep")
        assertEquals(ExerciseSource.CUSTOM, row?.source)
        assertEquals("保留动作", row?.name)
        assertEquals(1_324, db.exerciseCatalogDao().exerciseCountBySource(ExerciseSource.DATASET))
    }

    @Test
    fun rawMuscleTokensPreserveAmbiguousAndUnmappedValues() = runBlocking {
        importer.importIfNeeded()
        val tokens = db.exerciseCatalogDao().tokensForExercise("ds_0001")
        assertTrue(tokens.any { it.token == "abs" && it.mappingStatus == DatasetMappingStatus.MAPPED.name })
        assertTrue(DatasetMuscleMapping.rules.size == 50)
        assertTrue(DatasetMuscleMapping.rules.values.count { it.status == DatasetMappingStatus.MAPPED } == 23)
        assertTrue(DatasetMuscleMapping.rules.values.count { it.status == DatasetMappingStatus.AMBIGUOUS } == 12)
        assertTrue(DatasetMuscleMapping.rules.values.count { it.status == DatasetMappingStatus.UNMAPPED } == 15)
        assertEquals(0, DatasetMuscleMapping.mappedIds("shoulders").size)
        assertEquals(0, DatasetMuscleMapping.mappedIds("hip flexors").size)
        assertEquals(listOf("pectoralis_major"), DatasetMuscleMapping.mappedIds("pectorals"))
    }

    @Test
    fun concurrentImportsAreSingleFlightAndDoNotDuplicateRows() = runBlocking {
        val results = coroutineScope {
            listOf(
                async { importer.importIfNeeded() },
                async { importer.importIfNeeded() },
            ).awaitAll()
        }
        assertEquals(1, results.count { !it.alreadyImported })
        assertEquals(1_324, results.first { !it.alreadyImported }.added)
        assertTrue(results.any { it.alreadyImported })
        assertEquals(1_324, db.exerciseCatalogDao().exerciseCountBySource(ExerciseSource.DATASET))
        assertEquals(1_324, db.exerciseCatalogDao().sourceMapCount(ExerciseSource.DATASET))
        assertEquals(1_324, db.exerciseCatalogDao().importState(ExerciseSource.DATASET)!!.added)
        assertEquals(5_229, db.exerciseCatalogDao().totalMuscleTokens())
        assertEquals(2_051, db.exerciseMuscleDao().countAll())
    }

    @Test
    fun forceImportPreservesMediaConfirmedCandidateAndExistingLinks() = runBlocking {
        importer.importIfNeeded()
        val media = db.exerciseCatalogDao().mediaForExercise("ds_0001")!!.copy(
            localGifPath = "downloaded/0001.gif",
            localThumbnailPath = "thumbs/0001.webp",
            downloadState = com.example.gymlog.data.ExerciseMediaStatus.BUNDLED,
        )
        db.exerciseCatalogDao().insertMedia(listOf(media))
        db.exerciseMuscleDao().insertAll(
            listOf(ExerciseMuscleEntity("ds_0001", "biceps_brachii", "SECONDARY", 99)),
        )
        val candidate = db.exerciseCatalogDao().presetCandidates().first().copy(
            status = "CONFIRMED",
            datasetId = "confirmed-by-human",
        )
        db.exerciseCatalogDao().insertPresetCandidates(listOf(candidate))
        db.exerciseDao().archive("ds_0001")
        val beforeLinks = db.exerciseMuscleDao().countAll()

        importer.importIfNeeded(force = true)

        val restoredMedia = db.exerciseCatalogDao().mediaForExercise("ds_0001")!!
        assertEquals("downloaded/0001.gif", restoredMedia.localGifPath)
        assertEquals("thumbs/0001.webp", restoredMedia.localThumbnailPath)
        assertEquals(com.example.gymlog.data.ExerciseMediaStatus.BUNDLED, restoredMedia.downloadState)
        assertTrue(db.exerciseDao().getById("ds_0001")!!.archived)
        assertEquals(
            "CONFIRMED",
            db.exerciseCatalogDao().presetCandidates().first { it.presetExerciseId == candidate.presetExerciseId }.status,
        )
        assertTrue(
            db.exerciseMuscleDao().all().any {
                it.exerciseId == "ds_0001" && it.muscleId == "biceps_brachii"
            },
        )
        assertEquals(beforeLinks, db.exerciseMuscleDao().countAll())
        assertEquals(1_324, db.exerciseCatalogDao().exerciseCountBySource(ExerciseSource.DATASET))
        assertEquals(2_648, db.exerciseCatalogDao().allContents().size)
        assertEquals(1_324, db.exerciseCatalogDao().allMedia().size)
        assertEquals(5_229, db.exerciseCatalogDao().totalMuscleTokens())
        assertEquals(89, db.exerciseCatalogDao().presetCandidates().size)
    }

    @Test
    fun importAndClearRaceEndsInOneConsistentCatalog() = runBlocking {
        val repository = BackupRepository(
            db,
            db.backupDao(),
            ExerciseRepository(db.exerciseDao(), db.exerciseMuscleDao()),
            WorkoutRepository(db.sessionDao(), db.exerciseDao(), db.bodyRecordDao()),
            ProfileAvatarStore(context),
            exerciseCatalogImporter = importer,
        )
        coroutineScope {
            listOf(
                async { importer.importIfNeeded() },
                async { repository.clearLocalData() },
            ).awaitAll()
        }
        assertEquals(1_324, db.exerciseCatalogDao().exerciseCountBySource(ExerciseSource.DATASET))
        assertEquals(1_324, db.exerciseCatalogDao().sourceMapCount(ExerciseSource.DATASET))
        assertEquals(1_324, db.exerciseCatalogDao().importState(ExerciseSource.DATASET)!!.added)
        assertEquals(2_648, db.exerciseCatalogDao().allContents().size)
        assertEquals(1_324, db.exerciseCatalogDao().allMedia().size)
        assertEquals(5_229, db.exerciseCatalogDao().totalMuscleTokens())
        assertEquals(2_051, db.exerciseMuscleDao().countAll())
        assertEquals(89, db.exerciseCatalogDao().presetCandidates().size)
    }

    @Test
    fun parseFailureLeavesFailureStateAndRetrySucceeds() = runBlocking {
        val bad = ExerciseCatalogImporter(context, db, seedJsonOverride = "{not-json")
        var failed = false
        try {
            bad.importIfNeeded()
        } catch (_: Throwable) {
            failed = true
        }
        assertTrue(failed)
        val failedState = db.exerciseCatalogDao().importState(ExerciseSource.DATASET)
        assertNotNull(failedState)
        assertEquals(1, failedState!!.failed)
        assertEquals(0, db.exerciseCatalogDao().exerciseCountBySource(ExerciseSource.DATASET))

        val retry = importer.importIfNeeded()
        assertEquals(1_324, retry.added)
        assertEquals(0, db.exerciseCatalogDao().importState(ExerciseSource.DATASET)!!.failed)
        assertEquals(1_324, db.exerciseCatalogDao().exerciseCountBySource(ExerciseSource.DATASET))
    }

    @Test
    fun databaseFailureRollsBackAndRetrySucceeds() = runBlocking {
        db.exerciseDao().insertIgnore(
            listOf(
                ExerciseEntity(
                    id = "conflict",
                    name = "冲突",
                    bodyPart = "OTHER",
                    equipmentType = "OTHER",
                    weightMode = "WEIGHTED",
                    isCustom = false,
                    aliases = null,
                    createdAt = 1L,
                    source = ExerciseSource.DATASET,
                    sourceId = "0001",
                ),
            ),
        )
        var failed = false
        try {
            importer.importIfNeeded()
        } catch (_: Throwable) {
            failed = true
        }
        assertTrue(failed)
        assertEquals(1, db.exerciseCatalogDao().importState(ExerciseSource.DATASET)!!.failed)
        assertEquals(1, db.exerciseCatalogDao().exerciseCountBySource(ExerciseSource.DATASET))
        assertEquals(0, db.exerciseCatalogDao().contentCount("ds_0001"))

        db.openHelper.writableDatabase.execSQL("DELETE FROM exercises WHERE id = 'conflict'")
        val retry = importer.importIfNeeded()
        assertEquals(1_324, retry.added)
        assertEquals(0, db.exerciseCatalogDao().importState(ExerciseSource.DATASET)!!.failed)
        assertEquals(1_324, db.exerciseCatalogDao().exerciseCountBySource(ExerciseSource.DATASET))
    }

    @Test
    fun candidateListKeepsAllEightyNineLegacyPresets() = runBlocking {
        importer.importIfNeeded()
        val candidates = db.exerciseCatalogDao().presetCandidates()
        assertEquals(89, candidates.size)
        assertEquals(23, candidates.count { it.status == "PENDING_CONFIRMATION" })
        assertEquals(1, candidates.count { it.status == "AMBIGUOUS" })
        assertEquals(65, candidates.count { it.status == "NO_MATCH" })
        val pending = candidates.filter { it.status == "PENDING_CONFIRMATION" }
        assertTrue(pending.isNotEmpty())
        assertTrue(pending.all { it.datasetId != null })
        val ambiguous = candidates.single { it.status == "AMBIGUOUS" }
        assertEquals("", ambiguous.datasetId ?: "")
        assertEquals(2, ambiguous.candidateDatasetIds.split(",").size)
    }
}
