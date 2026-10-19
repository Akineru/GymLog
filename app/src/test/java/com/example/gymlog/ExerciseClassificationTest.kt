package com.example.gymlog

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.gymlog.data.ExerciseCatalogImporter
import com.example.gymlog.data.ExerciseClassification
import com.example.gymlog.data.ExerciseClassificationMigrator
import com.example.gymlog.data.ExerciseEntity
import com.example.gymlog.data.ExerciseRepository
import com.example.gymlog.data.ExerciseSource
import com.example.gymlog.data.GymLogDatabase
import com.example.gymlog.data.PresetExercises
import com.example.gymlog.data.WeightMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExerciseClassificationTest {
    private lateinit var context: Context
    private lateinit var db: GymLogDatabase
    private lateinit var repository: ExerciseRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, GymLogDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = ExerciseRepository(db.exerciseDao(), db.exerciseMuscleDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun datasetContainsEveryActualBodyAndEquipmentCategory() = runBlocking {
        ExerciseCatalogImporter(context, db).importIfNeeded()
        val rows = db.exerciseDao().allExercises().filter { it.source == ExerciseSource.DATASET }
        assertEquals(1_324, rows.size)
        assertTrue(db.exerciseDao().allExercises().none { it.id.startsWith("preset_") })
        assertEquals(10, rows.mapNotNull { it.bodyCategoryId }.distinct().size)
        assertEquals(28, rows.mapNotNull { it.equipmentCategoryId }.distinct().size)
        assertTrue(rows.all { it.classificationStatus == ExerciseClassification.STATUS_DATASET })
        assertTrue(rows.all { it.classificationVersion == ExerciseClassification.VERSION })

        val json = JSONObject(
            context.assets.open(ExerciseSource.DATASET_ASSET)
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() },
        )
        val records = json.getJSONArray("records")
        val expectedBody = mutableMapOf<String, Int>()
        val expectedEquipment = mutableMapOf<String, Int>()
        for (index in 0 until records.length()) {
            val record = records.getJSONObject(index)
            val body = record.getString("bodyPartDetail")
            val equipment = record.getString("equipmentDetail")
            expectedBody[body] = expectedBody.getOrDefault(body, 0) + 1
            expectedEquipment[equipment] = expectedEquipment.getOrDefault(equipment, 0) + 1
        }
        val actualBody = rows.groupingBy { it.bodyPartDetail!! }.eachCount()
        val actualEquipment = rows.groupingBy { it.equipmentDetail!! }.eachCount()
        assertEquals(expectedBody, actualBody)
        assertEquals(expectedEquipment, actualEquipment)
        expectedBody.forEach { (raw, count) ->
            val category = ExerciseClassification.bodyForRaw(raw)
            assertNotNull("Unknown body category $raw", category)
            assertEquals(count, rows.count { it.bodyCategoryId == category!!.id })
        }
        expectedEquipment.forEach { (raw, count) ->
            val category = ExerciseClassification.equipmentForRaw(raw)
            assertNotNull("Unknown equipment category $raw", category)
            assertEquals(count, rows.count { it.equipmentCategoryId == category!!.id })
        }
    }

    @Test
    fun formalFiltersContainOnlyTheNewStandardCategories() {
        assertEquals(11, ExerciseClassification.bodyUi.size) // 全部 + 10
        assertEquals(29, ExerciseClassification.equipmentUi.size) // 全部 + 28
        assertTrue(ExerciseClassification.bodyUi.none { it.id.startsWith("body_legacy_") })
        assertTrue(ExerciseClassification.bodyUi.none { it.id == ExerciseClassification.UNKNOWN_BODY.id })
        assertTrue(
            ExerciseClassification.equipmentUi.none {
                it.id.startsWith("equipment_legacy_") ||
                    it.id == ExerciseClassification.UNKNOWN_EQUIPMENT.id
            },
        )
    }

    @Test
    fun legacyPresetsAndCustomRowsAreMigratedIdempotently() = runBlocking {
        db.exerciseDao().insertIgnore(PresetExercises.all(1L))
        db.exerciseDao().insertCustom(
            ExerciseEntity(
                id = "custom_old",
                name = "旧自定义保留",
                bodyPart = "BACK",
                equipmentType = "MACHINE",
                weightMode = "WEIGHTED",
                isCustom = true,
                aliases = "old_alias",
                createdAt = 2L,
                source = ExerciseSource.CUSTOM,
            ),
        )
        db.exerciseDao().insertCustom(
            ExerciseEntity(
                id = "custom_archived",
                name = "旧自定义归档",
                bodyPart = "LEGS",
                equipmentType = "FREE_WEIGHT",
                weightMode = "WEIGHTED",
                isCustom = true,
                aliases = null,
                createdAt = 3L,
                archived = true,
                source = ExerciseSource.CUSTOM,
            ),
        )
        val migrator = ExerciseClassificationMigrator(db)
        migrator.migrateAll()

        val presets = db.exerciseDao().allExercises().filter { it.id.startsWith("preset_") }
        assertEquals(89, presets.size)
        assertTrue(presets.all { it.bodyCategoryId != null && it.equipmentCategoryId != null })
        assertTrue(presets.all { it.classificationVersion == ExerciseClassification.VERSION })
        assertTrue(presets.all { it.classificationStatus == ExerciseClassification.STATUS_AMBIGUOUS })
        assertEquals("preset_barbell_bench_press", db.exerciseDao().getById("preset_barbell_bench_press")!!.id)
        assertEquals(
            ExerciseClassification.CHEST.id,
            db.exerciseDao().getById("preset_barbell_bench_press")!!.bodyCategoryId,
        )
        assertEquals(
            ExerciseClassification.LEGACY_FREE_WEIGHT.id,
            db.exerciseDao().getById("preset_barbell_bench_press")!!.equipmentCategoryId,
        )

        val custom = db.exerciseDao().getById("custom_old")!!
        assertEquals("旧自定义保留", custom.name)
        assertEquals("old_alias", custom.aliases)
        assertEquals("BACK", custom.bodyPart)
        assertEquals("MACHINE", custom.equipmentType)
        assertEquals(ExerciseClassification.BACK.id, custom.bodyCategoryId)
        assertEquals(ExerciseClassification.LEGACY_MACHINE.id, custom.equipmentCategoryId)
        assertEquals(ExerciseClassification.STATUS_AMBIGUOUS, custom.classificationStatus)
        assertTrue(custom.isCustom)
        assertFalse(custom.archived)
        val archived = db.exerciseDao().getById("custom_archived")!!
        assertTrue(archived.archived)
        assertEquals(ExerciseClassification.LEGACY_LEGS.id, archived.bodyCategoryId)

        val first = db.exerciseDao().allExercises().associateBy { it.id }
        migrator.migrateAll()
        val second = db.exerciseDao().allExercises().associateBy { it.id }
        first.keys.forEach { id ->
            assertEquals(first[id]!!.bodyCategoryId, second[id]!!.bodyCategoryId)
            assertEquals(first[id]!!.equipmentCategoryId, second[id]!!.equipmentCategoryId)
            assertEquals(first[id]!!.classificationStatus, second[id]!!.classificationStatus)
            assertEquals(first[id]!!.classificationVersion, second[id]!!.classificationVersion)
        }

        // A future human-confirmed mapping is represented by the same fields and
        // must not be overwritten by a repeat migration.
        db.exerciseDao().updateClassification(
            id = "custom_old",
            bodyCategoryId = ExerciseClassification.CHEST.id,
            equipmentCategoryId = ExerciseClassification.BARBELL.id,
            classificationStatus = ExerciseClassification.STATUS_MAPPED,
            classificationVersion = ExerciseClassification.VERSION,
        )
        migrator.migrateAll()
        val confirmed = db.exerciseDao().getById("custom_old")!!
        assertEquals(ExerciseClassification.CHEST.id, confirmed.bodyCategoryId)
        assertEquals(ExerciseClassification.BARBELL.id, confirmed.equipmentCategoryId)
        assertEquals(ExerciseClassification.STATUS_MAPPED, confirmed.classificationStatus)
    }

    @Test
    fun unknownDatasetCategoryIsPreservedAndMarkedUnmapped() = runBlocking {
        db.exerciseDao().insertIgnore(
            listOf(
                ExerciseEntity(
                    id = "ds_unknown",
                    name = "未知分类动作",
                    bodyPart = "OTHER",
                    equipmentType = "OTHER",
                    bodyPartDetail = "not-a-real-body-part",
                    equipmentDetail = "not-a-real-equipment",
                    weightMode = WeightMode.WEIGHTED.name,
                    isCustom = false,
                    aliases = null,
                    createdAt = 1L,
                    source = ExerciseSource.DATASET,
                    sourceId = "unknown",
                ),
            ),
        )
        ExerciseClassificationMigrator(db).migrateAll()
        val row = db.exerciseDao().getById("ds_unknown")!!
        assertEquals(ExerciseClassification.UNKNOWN_BODY.id, row.bodyCategoryId)
        assertEquals(ExerciseClassification.UNKNOWN_EQUIPMENT.id, row.equipmentCategoryId)
        assertEquals(ExerciseClassification.STATUS_UNMAPPED, row.classificationStatus)
        assertEquals("not-a-real-body-part", row.bodyPartDetail)
        assertEquals("not-a-real-equipment", row.equipmentDetail)
        assertEquals(1, repository.observeExercises("", null, null).first().size)
    }

    @Test
    fun categoryFiltersAndSearchComposeCorrectly() = runBlocking {
        insertClassified(
            id = "ds_legs_dumbbell",
            name = "腿部哑铃动作",
            body = ExerciseClassification.UPPER_LEGS,
            equipment = ExerciseClassification.DUMBBELL,
            sourceId = "c1",
        )
        insertClassified(
            id = "ds_legs_cable",
            name = "腿部绳索动作",
            body = ExerciseClassification.UPPER_LEGS,
            equipment = ExerciseClassification.CABLE,
            sourceId = "c2",
        )
        insertClassified(
            id = "ds_back_dumbbell",
            name = "背部哑铃动作",
            body = ExerciseClassification.BACK,
            equipment = ExerciseClassification.DUMBBELL,
            sourceId = "c3",
        )
        db.exerciseDao().insertIgnore(
            listOf(
                ExerciseEntity(
                    id = "preset_hidden",
                    name = "腿部隐藏预置",
                    bodyPart = "LEGS",
                    equipmentType = "FREE_WEIGHT",
                    weightMode = "WEIGHTED",
                    isCustom = false,
                    aliases = null,
                    createdAt = 1L,
                    bodyCategoryId = ExerciseClassification.LEGACY_LEGS.id,
                    equipmentCategoryId = ExerciseClassification.LEGACY_FREE_WEIGHT.id,
                    classificationStatus = ExerciseClassification.STATUS_AMBIGUOUS,
                    classificationVersion = ExerciseClassification.VERSION,
                    source = ExerciseSource.LEGACY,
                ),
            ),
        )

        val bodyOnly = repository.observeExercises(
            "",
            ExerciseClassification.UPPER_LEGS.id,
            null,
        ).first().map { it.id }.toSet()
        assertEquals(setOf("ds_legs_dumbbell", "ds_legs_cable"), bodyOnly)

        val equipmentOnly = repository.observeExercises(
            "",
            null,
            ExerciseClassification.DUMBBELL.id,
        ).first().map { it.id }.toSet()
        assertEquals(setOf("ds_legs_dumbbell", "ds_back_dumbbell"), equipmentOnly)

        val combined = repository.observeExercises(
            "",
            ExerciseClassification.UPPER_LEGS.id,
            ExerciseClassification.DUMBBELL.id,
        ).first().map { it.id }.toSet()
        assertEquals(setOf("ds_legs_dumbbell"), combined)

        val searchAndCategory = repository.observeExercises(
            "绳索",
            ExerciseClassification.UPPER_LEGS.id,
            null,
        ).first()
        // Text search intentionally searches the whole library, but the
        // classification filter remains active.
        assertEquals(listOf("ds_legs_cable"), searchAndCategory.map { it.id })

        val hidden = repository.observeExercises("隐藏预置", null, null).first()
        assertTrue(hidden.isEmpty())
    }

    private suspend fun insertClassified(
        id: String,
        name: String,
        body: ExerciseClassification.BodyCategory,
        equipment: ExerciseClassification.EquipmentCategory,
        sourceId: String,
    ) {
        db.exerciseDao().insertIgnore(
            listOf(
                ExerciseEntity(
                    id = id,
                    name = name,
                    bodyPart = ExerciseClassification.legacyBodyPartFor(body).name,
                    equipmentType = ExerciseClassification.legacyEquipmentFor(equipment).name,
                    bodyPartDetail = body.rawValue,
                    equipmentDetail = equipment.rawValue,
                    bodyCategoryId = body.id,
                    equipmentCategoryId = equipment.id,
                    classificationStatus = ExerciseClassification.STATUS_DATASET,
                    classificationVersion = ExerciseClassification.VERSION,
                    weightMode = WeightMode.WEIGHTED.name,
                    isCustom = false,
                    aliases = null,
                    createdAt = 1L,
                    source = ExerciseSource.DATASET,
                    sourceId = sourceId,
                ),
            ),
        )
    }
}
