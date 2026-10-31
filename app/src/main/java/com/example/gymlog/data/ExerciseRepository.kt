package com.example.gymlog.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.UUID

class ExerciseRepository(
    private val exerciseDao: ExerciseDao,
    private val muscleDao: ExerciseMuscleDao,
) {
    fun observeAll(): Flow<List<ExerciseEntity>> = exerciseDao.observeAll()

    /** 详情页用：跟随数据库变化，动作被删除/归档后自动变成 null。 */
    fun observeById(exerciseId: String): Flow<ExerciseEntity?> =
        exerciseDao.observeById(exerciseId)

    /** 详情页「历史记录」用：只取当前动作的组，按日期倒序。 */
    fun observeHistory(exerciseId: String): Flow<List<ExerciseHistorySetRow>> =
        exerciseDao.observeHistoryForExercise(exerciseId)

    /**
     * 保存「训练资料」四个字段（别名 / 优先级 / 怎么做 / 细节）。
     *
     * 不动名称、分类、肌群和资源，因此内置动作也能安全编辑这些内容。
     * 空文本统一写成 null，避免出现「空串」与「未维护」两种表示。
     */
    suspend fun updateTrainingFields(
        exerciseId: String,
        aliases: String?,
        howTo: String?,
        cues: String?,
    ) {
        exerciseDao.updateTrainingFields(
            id = exerciseId,
            aliases = ExerciseAliases.normalize(aliases),
            howTo = howTo.normalizeText(),
            cues = cues.normalizeText(),
        )
    }

    /**
     * 收藏 / 取消收藏的唯一入口：isFavorite 与 priority 在 DAO 的同一条 UPDATE 中联动
     * （收藏 = 10，取消 = 0），两个页面（动作库 / 详情页菜单）都走这里，不会出现两套逻辑。
     */
    suspend fun setFavorite(exerciseId: String, favorite: Boolean) {
        exerciseDao.setFavorite(id = exerciseId, favorite = favorite)
    }

    /** 去掉首尾空白；全空时写 null，避免「空串」与「未维护」两种表示并存。 */
    private fun String?.normalizeText(): String? =
        this?.trim()?.takeIf { it.isNotEmpty() }

    fun observeExercises(
        query: String,
        bodyCategoryId: String?,
        equipmentCategoryId: String?,
    ): Flow<List<ExerciseEntity>> {
        return combine(
            exerciseDao.observeFiltered(
                // 转义 LIKE 通配符：用户输入「%」或「_」时应按字面匹配，
                // 否则会变成「命中全部」，搜索结果看起来像坏掉了。
                query = escapeLikePattern(query.trim()),
                bodyCategoryId = bodyCategoryId.orEmpty(),
                equipmentCategoryId = equipmentCategoryId.orEmpty(),
                datasetSource = ExerciseSource.DATASET,
            ),
            exerciseDao.observeUsage(),
        ) { exercises, usage ->
            exercises.sortedByTrainingCount(usage)
        }
    }

    fun observeMuscleLinks(): Flow<List<MuscleLink>> {
        return muscleDao.observeAll().map { rows -> rows.map { it.toLink() } }
    }

    suspend fun addCustomExercise(
        name: String,
        bodyCategory: ExerciseClassification.BodyCategory,
        equipmentCategory: ExerciseClassification.EquipmentCategory,
        weightMode: WeightMode,
        aliases: String?,
        muscles: List<MuscleLink> = emptyList(),
    ): Result<Unit> {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            return Result.failure(IllegalArgumentException("EMPTY_NAME"))
        }
        if (!bodyCategory.standard || !equipmentCategory.standard) {
            return Result.failure(IllegalArgumentException("INVALID_CATEGORY"))
        }
        if (exerciseDao.countByName(trimmedName) > 0) {
            return Result.failure(IllegalStateException("DUPLICATE_NAME"))
        }
        val id = "custom_${UUID.randomUUID()}"
        val trimmedAliases = aliases?.trim().orEmpty().ifEmpty { null }
        val links = muscles.mapIndexed { index, link -> link.copy(exerciseId = id, sortOrder = index) }
        MuscleMapLogic.validateLinks(setOf(id), links)
        exerciseDao.insertCustom(
            ExerciseEntity(
                id = id,
                name = trimmedName,
                // Keep the legacy broad fields for historical grouping/fallbacks,
                // but the new stable classification ids are authoritative.
                bodyPart = ExerciseClassification.legacyBodyPartFor(bodyCategory).name,
                equipmentType = ExerciseClassification.legacyEquipmentFor(equipmentCategory).name,
                bodyPartDetail = bodyCategory.rawValue,
                equipmentDetail = equipmentCategory.rawValue,
                bodyCategoryId = bodyCategory.id,
                equipmentCategoryId = equipmentCategory.id,
                classificationStatus = ExerciseClassification.STATUS_MAPPED,
                classificationVersion = ExerciseClassification.VERSION,
                weightMode = weightMode.name,
                isCustom = true,
                aliases = trimmedAliases,
                createdAt = System.currentTimeMillis(),
                source = ExerciseSource.CUSTOM,
            ),
        )
        if (links.isNotEmpty()) muscleDao.insertAll(links.map { it.toEntity() })
        return Result.success(Unit)
    }

    suspend fun replaceCustomMuscles(exerciseId: String, muscles: List<MuscleLink>) {
        val exercise = exerciseDao.getById(exerciseId)
        if (exercise == null || !exercise.isCustom) return
        val links = muscles.mapIndexed { index, link -> link.copy(exerciseId = exerciseId, sortOrder = index) }
        MuscleMapLogic.validateLinks(setOf(exerciseId), links)
        muscleDao.deleteForExercise(exerciseId)
        if (links.isNotEmpty()) muscleDao.insertAll(links.map { it.toEntity() })
    }

    suspend fun deleteExercise(id: String) {
        exerciseDao.archive(id)
    }

    companion object {
        /**
         * 把 LIKE 的通配符转成字面量。
         *
         * 反斜杠必须最先转义，否则会把后面新加的转义符再转一遍。
         * 动作库与「添加训练」共用这一个入口，两边的搜索语义必须完全一致。
         */
        fun escapeLikePattern(value: String): String =
            value
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_")
    }
}
