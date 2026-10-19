package com.example.gymlog.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "exercises",
    indices = [
        Index(value = ["bodyPart"]),
        Index(value = ["equipmentType"]),
        Index(value = ["bodyCategoryId"]),
        Index(value = ["equipmentCategoryId"]),
        Index(value = ["name"]),
        Index(value = ["source", "sourceId"], unique = true),
    ],
)
data class ExerciseEntity(
    @PrimaryKey val id: String,
    val name: String,
    val bodyPart: String,
    val equipmentType: String,
    val weightMode: String,
    val isCustom: Boolean,
    val aliases: String?,
    val createdAt: Long,
    val workingPrWeightKg: Double? = null,
    val workingPrReps: Int? = null,
    val workingPrWeightMode: String? = null,
    val workingPrDateEpochDay: Long? = null,
    val maxPrWeightKg: Double? = null,
    val maxPrReps: Int? = null,
    val maxPrWeightMode: String? = null,
    val maxPrDateEpochDay: Long? = null,
    val archived: Boolean = false,
    val nameEn: String? = null,
    val source: String = ExerciseSource.LEGACY,
    val sourceId: String? = null,
    val equipmentDetail: String? = null,
    val bodyPartDetail: String? = null,
    val bodyCategoryId: String? = null,
    val equipmentCategoryId: String? = null,
    val classificationStatus: String = ExerciseClassification.STATUS_UNMIGRATED,
    val classificationVersion: Int = 0,
) {
    fun workingPr(): PersonalRecord? = toRecord(
        workingPrWeightKg,
        workingPrReps,
        workingPrWeightMode,
        workingPrDateEpochDay,
    )

    fun maxPr(): PersonalRecord? = toRecord(
        maxPrWeightKg,
        maxPrReps,
        maxPrWeightMode,
        maxPrDateEpochDay,
    )

    private fun toRecord(
        weightKg: Double?,
        reps: Int?,
        mode: String?,
        dateEpochDay: Long?,
    ): PersonalRecord? {
        if (reps == null || mode == null || dateEpochDay == null) return null
        return PersonalRecord(
            weightKg = weightKg,
            reps = reps,
            weightMode = mode,
            dateEpochDay = dateEpochDay,
        )
    }
}

fun List<ExerciseEntity>.sortedByTrainingCount(
    usage: List<ExerciseUsageRow>,
): List<ExerciseEntity> {
    val byId = usage.associateBy { it.exerciseId }
    return sortedWith(
        compareByDescending<ExerciseEntity> { byId[it.id]?.sessionCount ?: 0 }
            .thenByDescending { byId[it.id]?.lastEpochDay ?: Long.MIN_VALUE }
            .thenBy { it.name.lowercase() },
    )
}

fun ExerciseEntity.displayName(): String {
    val partLabel = ExerciseClassification.body(bodyCategoryId)?.labelZh
        ?: bodyPartDetail?.takeIf { it.isNotBlank() }
        ?: BodyPart.entries.find { it.name == bodyPart }?.labelZh
        ?: bodyPart
    return "$partLabel-$name"
}
