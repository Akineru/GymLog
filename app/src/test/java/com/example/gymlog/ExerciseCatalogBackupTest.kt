package com.example.gymlog

import com.example.gymlog.data.AvatarImageBackup
import com.example.gymlog.data.BackupCodec
import com.example.gymlog.data.ExerciseEntity
import com.example.gymlog.data.ExerciseSource
import com.example.gymlog.data.GymLogSnapshot
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExerciseCatalogBackupTest {
    @Test
    fun newCatalogFieldsRoundTripAndOldJsonUsesLegacyDefaults() {
        val source = ExerciseEntity(
            id = "ds_0001",
            name = "3/4 仰卧起坐",
            bodyPart = "ABS",
            equipmentType = "FREE_WEIGHT",
            weightMode = "BODYWEIGHT",
            isCustom = false,
            aliases = null,
            createdAt = 1L,
            archived = true,
            nameEn = "3/4 sit-up",
            source = ExerciseSource.DATASET,
            sourceId = "0001",
            equipmentDetail = "body weight",
            bodyPartDetail = "waist",
            bodyCategoryId = "body_waist",
            equipmentCategoryId = "equipment_body_weight",
            classificationStatus = "DATASET",
            classificationVersion = 1,
        )
        val snapshot = GymLogSnapshot(
            exercises = listOf(source),
            sessions = emptyList(),
            sets = emptyList(),
            profile = null,
            measurements = emptyList(),
            legacyRecords = emptyList(),
            timerRecords = emptyList(),
            timerLaps = emptyList(),
            avatarImage = AvatarImageBackup.None,
        )
        val encoded = BackupCodec.encode(snapshot, exportedAt = 1L)
        val decoded = BackupCodec.decode(encoded).exercises.single()
        assertEquals("3/4 sit-up", decoded.nameEn)
        assertEquals(ExerciseSource.DATASET, decoded.source)
        assertEquals("0001", decoded.sourceId)
        assertEquals("body weight", decoded.equipmentDetail)
        assertEquals("waist", decoded.bodyPartDetail)
        assertEquals("body_waist", decoded.bodyCategoryId)
        assertEquals("equipment_body_weight", decoded.equipmentCategoryId)
        assertEquals("DATASET", decoded.classificationStatus)
        assertEquals(1, decoded.classificationVersion)
        assertEquals(true, decoded.archived)

        val oldJson = JSONObject(encoded)
        oldJson.getJSONArray("exercises").getJSONObject(0).apply {
            remove("nameEn")
            remove("source")
            remove("sourceId")
            remove("equipmentDetail")
            remove("bodyPartDetail")
            remove("bodyCategoryId")
            remove("equipmentCategoryId")
            remove("classificationStatus")
            remove("classificationVersion")
        }
        val old = BackupCodec.decode(oldJson.toString()).exercises.single()
        assertNull(old.nameEn)
        assertEquals(ExerciseSource.LEGACY, old.source)
        assertNull(old.sourceId)
        assertNull(old.equipmentDetail)
        assertNull(old.bodyPartDetail)
        assertNull(old.bodyCategoryId)
        assertNull(old.equipmentCategoryId)
        assertEquals("UNMIGRATED", old.classificationStatus)
        assertEquals(0, old.classificationVersion)

        val custom = source.copy(id = "custom_old", isCustom = true, source = ExerciseSource.CUSTOM, sourceId = null)
        val oldCustomJson = JSONObject(encoded)
        oldCustomJson.getJSONArray("exercises").put(0, JSONObject().apply {
            put("id", custom.id)
            put("name", custom.name)
            put("bodyPart", custom.bodyPart)
            put("equipmentType", custom.equipmentType)
            put("weightMode", custom.weightMode)
            put("isCustom", true)
            put("aliases", JSONObject.NULL)
            put("createdAt", custom.createdAt)
            put("archived", false)
        })
        val oldCustom = BackupCodec.decode(oldCustomJson.toString()).exercises.single()
        assertEquals(ExerciseSource.CUSTOM, oldCustom.source)
    }
}
