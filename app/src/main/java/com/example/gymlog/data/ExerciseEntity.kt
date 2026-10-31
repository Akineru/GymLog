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
    /**
     * 排序优先级，**数值越大越靠前**，合法范围 0–10（见 [ExercisePriority]）。
     *
     * 由收藏功能自动管理：收藏 = 10，取消收藏 = 0，前台不提供手动编辑入口。
     * 只影响动作库 / 添加训练列表的排序，不参与训练页、统计页等其他排序。
     */
    val priority: Int = 0,
    /** 收藏状态。与 [priority] 联动：收藏时 priority 固定 10，取消收藏恢复 0。 */
    val isFavorite: Boolean = false,
    /** 要点：动作怎么做。空表示尚未维护。 */
    val howTo: String? = null,
    /** 要点：动作细节 / 注意点。空表示尚未维护。 */
    val cues: String? = null,
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

/**
 * 动作优先级的合法范围与含义。
 *
 * **数值越大越靠前**，0 是默认值（默认不动 = 沿用原来的「练得多 / 最近练 / 名称」顺序）。
 * 取 0~100：手机数字键盘打不出负号，限制在非负区间可以避免「输入了却存不进去」，
 * 100 档也足够把常用动作钉到前面。
 */
object ExercisePriority {
    const val MIN = 0
    const val MAX = 10
    const val DEFAULT = 0

    fun normalize(value: Int): Int = value.coerceIn(MIN, MAX)

    fun parse(text: String): Int? = text.trim().toIntOrNull()?.let(::normalize)
}

fun List<ExerciseEntity>.sortedByTrainingCount(    usage: List<ExerciseUsageRow>,
): List<ExerciseEntity> {
    val byId = usage.associateBy { it.exerciseId }
    return sortedWith(
        // 人工设定的优先级排在最前（越大越靠前），其余沿用原来的「练得多 / 最近练 / 名称」规则。
        // priority 默认 0 时这一层不产生任何影响，升级后顺序与之前一致。
        compareByDescending<ExerciseEntity> { it.priority }
            .thenByDescending { byId[it.id]?.sessionCount ?: 0 }
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
