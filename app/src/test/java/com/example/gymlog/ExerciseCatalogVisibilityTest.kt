package com.example.gymlog

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.gymlog.data.BodyRecordRepository
import com.example.gymlog.data.ExerciseClassification
import com.example.gymlog.data.ExerciseClassificationMigrator
import com.example.gymlog.data.ExerciseEntity
import com.example.gymlog.data.ExerciseRepository
import com.example.gymlog.data.ExerciseSource
import com.example.gymlog.data.GymLogDatabase
import com.example.gymlog.data.MuscleLink
import com.example.gymlog.data.MuscleRole
import com.example.gymlog.data.WorkoutRepository
import com.example.gymlog.data.WorkoutSessionEntity
import com.example.gymlog.data.WorkoutSetEntity
import com.example.gymlog.data.WeightMode
import com.example.gymlog.ui.stats.TrainingTrendViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * UI catalog visibility only. Historical queries are deliberately tested with
 * all()/getById()/loadSession() and must not inherit the directory filter.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExerciseCatalogVisibilityTest {
    private lateinit var db: GymLogDatabase
    private lateinit var repository: ExerciseRepository
    private lateinit var workouts: WorkoutRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, GymLogDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = ExerciseRepository(db.exerciseDao(), db.exerciseMuscleDao())
        workouts = WorkoutRepository(db.sessionDao(), db.exerciseDao(), db.bodyRecordDao())
        runBlocking {
            insertFixtures()
            ExerciseClassificationMigrator(db).migrateAll()
        }
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    @Test
    fun defaultCatalogShowsDatasetAndCustomButHidesLegacyPresets() = runTest {
        val visible = repository.observeExercises("", null, null).first()

        assertTrue(visible.any { it.id == "ds_bench" && it.source == ExerciseSource.DATASET })
        assertTrue(visible.any { it.id == "custom_new" && it.isCustom })
        assertTrue(visible.any { it.id == "custom_legacy" && it.isCustom })
        assertFalse(visible.any { it.id == "preset_bench" })
        assertFalse(visible.any { it.id == "preset_archived" })
        assertFalse(visible.any { it.id == "ds_archived" })
        assertFalse(visible.any { it.id == "custom_archived" })
        assertEquals(
            listOf("custom_legacy", "custom_new", "ds_bench", "ds_bench_dup", "ds_same_1", "ds_same_2"),
            visible.map { it.id }.sorted(),
        )
    }

    @Test
    fun archivedRowsAreAlwaysExcluded() = runTest {
        val visible = repository.observeExercises("", null, null).first()
        assertFalse(visible.any { it.id == "ds_archived" })
        assertFalse(visible.any { it.id == "custom_archived" })
        assertFalse(visible.any { it.id == "preset_archived" })
    }

    @Test
    fun searchMatchesNameNameEnAndAliasesWithoutRestoringHiddenLegacy() = runTest {
        val byName = repository.observeExercises("Dataset Only", null, null).first()
        assertTrue(byName.any { it.id == "ds_bench" })

        val byEnglishName = repository.observeExercises("Dataset English", null, null).first()
        assertTrue(byEnglishName.any { it.id == "ds_bench" })

        val byAlias = repository.observeExercises("dataset_alias", null, null).first()
        assertTrue(byAlias.any { it.id == "ds_bench" })

        val hiddenByName = repository.observeExercises("Legacy Only", null, null).first()
        assertTrue(hiddenByName.isEmpty())
        val hiddenByEnglish = repository.observeExercises("Legacy English", null, null).first()
        assertTrue(hiddenByEnglish.isEmpty())
        val hiddenByAlias = repository.observeExercises("legacy_alias", null, null).first()
        assertTrue(hiddenByAlias.isEmpty())

        val stillHidden = repository.observeExercises("legacy_alias", null, null).first()
        assertTrue(stillHidden.isEmpty())
    }

    @Test
    fun bodyPartAndEquipmentFiltersDoNotBringBackHiddenLegacy() = runTest {
        val visible = repository.observeExercises(
            "",
            ExerciseClassification.CHEST.id,
            ExerciseClassification.BARBELL.id,
        ).first()

        assertTrue(visible.any { it.id == "ds_bench" })
        assertFalse(visible.any { it.id == "preset_bench" })
        assertFalse(visible.any { it.id == "ds_archived" })
    }

    @Test
    fun sameNameDatasetRowsRemainIndependentRecords() = runTest {
        val visible = repository.observeExercises("Same Name", null, null).first()

        assertEquals(setOf("ds_same_1", "ds_same_2"), visible.map { it.id }.toSet())
        assertEquals(
            listOf("ds_same_1", "ds_same_2"),
            visible.map { it.id }.sorted(),
        )
        assertTrue(visible.all { it.source == ExerciseSource.DATASET })
    }

    @Test
    fun customAddEditMusclesAndArchiveStillWork() = runTest {
        val added = repository.addCustomExercise(
            name = "New Custom Move",
            bodyCategory = ExerciseClassification.BACK,
            equipmentCategory = ExerciseClassification.CABLE,
            weightMode = WeightMode.WEIGHTED,
            aliases = "new_custom_alias",
            muscles = listOf(
                MuscleLink(
                    exerciseId = "",
                    muscleId = "biceps_brachii",
                    role = MuscleRole.PRIMARY,
                    sortOrder = 0,
                ),
            ),
        )
        assertTrue(added.isSuccess)

        val custom = repository.observeExercises("New Custom Move", null, null).first().single()
        assertEquals(ExerciseSource.CUSTOM, custom.source)
        assertTrue(custom.isCustom)
        assertEquals(
            listOf("biceps_brachii"),
            db.exerciseMuscleDao().all().filter { it.exerciseId == custom.id }.map { it.muscleId },
        )

        repository.replaceCustomMuscles(
            custom.id,
            listOf(
                MuscleLink(
                    exerciseId = custom.id,
                    muscleId = "pectoralis_major",
                    role = MuscleRole.SECONDARY,
                    sortOrder = 0,
                ),
            ),
        )
        assertEquals(
            listOf("pectoralis_major"),
            db.exerciseMuscleDao().all().filter { it.exerciseId == custom.id }.map { it.muscleId },
        )

        repository.deleteExercise(custom.id)
        assertTrue(repository.observeExercises("New Custom Move", null, null).first().isEmpty())
        val archived = db.exerciseDao().getById(custom.id)
        assertNotNull(archived)
        assertTrue(archived!!.archived)
    }

    @Test
    fun historicalPresetSetStillResolvesItsName() = runTest {
        val detail = workouts.loadSession("session_history")
        assertNotNull(detail)
        val exercise = detail!!.exercises.single().exercise
        assertEquals("preset_bench", exercise.id)
        assertEquals("杠铃卧推", exercise.name)
        assertEquals(ExerciseSource.LEGACY, exercise.source)
    }

    @Test
    fun oldPresetPersonalRecordStillReads() = runTest {
        val preset = db.exerciseDao().getById("preset_bench")
        assertNotNull(preset)
        assertEquals(100.0, preset!!.workingPrWeightKg!!, 0.0)
        assertEquals(5, preset.workingPrReps)
        assertEquals(120.0, preset.maxPrWeightKg!!, 0.0)
        assertNotNull(preset.workingPr())
        assertNotNull(preset.maxPr())
    }

    @Test
    fun trainingTrendStillSeesHistoricalLegacyExercise() = runBlocking {
        val viewModel = TrainingTrendViewModel(
            workoutRepository = workouts,
            exerciseRepository = repository,
            bodyRecordRepository = BodyRecordRepository(db.bodyRecordDao()),
        )
        val state = withTimeout(5_000) {
            viewModel.uiState.first { current ->
                current.exercises.any { it.id == "preset_bench" }
            }
        }
        assertTrue(state.exercises.any { it.id == "preset_bench" })
        assertEquals("preset_bench", state.selectedExerciseId)
    }

    private suspend fun insertFixtures() {
        db.exerciseDao().insertIgnore(
            listOf(
                exercise(
                    id = "preset_bench",
                    name = "杠铃卧推",
                    nameEn = "Legacy English",
                    aliases = "legacy_alias",
                    source = ExerciseSource.LEGACY,
                    isCustom = false,
                    workingPrWeightKg = 100.0,
                    workingPrReps = 5,
                    workingPrWeightMode = WeightMode.WEIGHTED.name,
                    workingPrDateEpochDay = 20_000,
                    maxPrWeightKg = 120.0,
                    maxPrReps = 3,
                    maxPrWeightMode = WeightMode.WEIGHTED.name,
                    maxPrDateEpochDay = 20_010,
                ),
                exercise(
                    id = "ds_bench",
                    name = "Dataset Only",
                    nameEn = "Dataset English",
                    aliases = "dataset_alias",
                    source = ExerciseSource.DATASET,
                    sourceId = "0001",
                ),
                exercise(
                    id = "ds_bench_dup",
                    name = "Dataset Only",
                    source = ExerciseSource.DATASET,
                    sourceId = "0002",
                    bodyPart = "CHEST",
                    equipmentType = "FREE_WEIGHT",
                ),
                exercise(
                    id = "ds_same_1",
                    name = "Same Name",
                    source = ExerciseSource.DATASET,
                    sourceId = "0003",
                ),
                exercise(
                    id = "ds_same_2",
                    name = "Same Name",
                    source = ExerciseSource.DATASET,
                    sourceId = "0004",
                ),
                exercise(
                    id = "custom_new",
                    name = "Custom Only",
                    source = ExerciseSource.CUSTOM,
                    sourceId = null,
                    isCustom = true,
                    muscleLinks = false,
                ),
                // Old user-created rows kept source=LEGACY but isCustom=1.
                exercise(
                    id = "custom_legacy",
                    name = "Old Custom",
                    source = ExerciseSource.LEGACY,
                    sourceId = null,
                    isCustom = true,
                    muscleLinks = false,
                ),
                exercise(
                    id = "ds_archived",
                    name = "Archived Dataset",
                    source = ExerciseSource.DATASET,
                    sourceId = "0005",
                    archived = true,
                ),
                exercise(
                    id = "custom_archived",
                    name = "Archived Custom",
                    source = ExerciseSource.CUSTOM,
                    sourceId = null,
                    isCustom = true,
                    archived = true,
                    muscleLinks = false,
                ),
                exercise(
                    id = "preset_archived",
                    name = "Archived Preset",
                    source = ExerciseSource.LEGACY,
                    sourceId = null,
                    isCustom = false,
                    archived = true,
                ),
            ),
        )
        db.sessionDao().replaceSession(
            WorkoutSessionEntity(
                id = "session_history",
                dateEpochDay = 20_000,
                note = null,
                createdAt = 1L,
            ),
            listOf(
                WorkoutSetEntity(
                    id = "set_history",
                    sessionId = "session_history",
                    exerciseId = "preset_bench",
                    exerciseOrder = 0,
                    setIndex = 0,
                    weightMode = WeightMode.WEIGHTED.name,
                    weightKg = 100.0,
                    reps = 5,
                    note = null,
                ),
            ),
        )
    }

    private fun exercise(
        id: String,
        name: String,
        nameEn: String? = null,
        aliases: String? = null,
        source: String,
        sourceId: String? = null,
        isCustom: Boolean = false,
        archived: Boolean = false,
        bodyPart: String = "CHEST",
        equipmentType: String = "FREE_WEIGHT",
        workingPrWeightKg: Double? = null,
        workingPrReps: Int? = null,
        workingPrWeightMode: String? = null,
        workingPrDateEpochDay: Long? = null,
        maxPrWeightKg: Double? = null,
        maxPrReps: Int? = null,
        maxPrWeightMode: String? = null,
        maxPrDateEpochDay: Long? = null,
        muscleLinks: Boolean = true,
    ): ExerciseEntity {
        // muscleLinks is kept as a call-site marker; fixtures intentionally do
        // not add guessed muscle mappings for dataset/legacy rows.
        require(!muscleLinks || !isCustom)
        return ExerciseEntity(
            id = id,
            name = name,
            bodyPart = bodyPart,
            equipmentType = equipmentType,
            weightMode = WeightMode.WEIGHTED.name,
            isCustom = isCustom,
            aliases = aliases,
            createdAt = 1L,
            workingPrWeightKg = workingPrWeightKg,
            workingPrReps = workingPrReps,
            workingPrWeightMode = workingPrWeightMode,
            workingPrDateEpochDay = workingPrDateEpochDay,
            maxPrWeightKg = maxPrWeightKg,
            maxPrReps = maxPrReps,
            maxPrWeightMode = maxPrWeightMode,
            maxPrDateEpochDay = maxPrDateEpochDay,
            archived = archived,
            nameEn = nameEn,
            source = source,
            sourceId = sourceId,
            bodyPartDetail = if (source == ExerciseSource.DATASET) "chest" else null,
            equipmentDetail = if (source == ExerciseSource.DATASET) "barbell" else null,
        )
    }
}
