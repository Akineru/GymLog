package com.example.gymlog.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class ExerciseUsageRow(
    val exerciseId: String,
    val sessionCount: Int,
    val lastEpochDay: Long,
)

/**
 * 某个动作在历史训练里的一组记录（已 JOIN 出所属训练场次的日期）。
 *
 * 只按 `exerciseId` 精确关联，不做名称模糊匹配：动作改名或存在同名动作时，
 * 记录都不会串到别的动作上。
 */
data class ExerciseHistorySetRow(
    val sessionId: String,
    val dateEpochDay: Long,
    val createdAt: Long,
    val setIndex: Int,
    val weightMode: String,
    val weightKg: Double?,
    val reps: Int?,
)

@Dao
interface ExerciseDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(exercises: List<ExerciseEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCustom(exercise: ExerciseEntity)

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM exercises WHERE name = :name COLLATE NOCASE")
    suspend fun countByName(name: String): Int

    @Query("SELECT id FROM exercises")
    suspend fun allIds(): List<String>

    @Query("SELECT * FROM exercises")
    suspend fun allExercises(): List<ExerciseEntity>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getById(id: String): ExerciseEntity?

    @Query(
        """
        SELECT * FROM exercises
        WHERE archived = 0
        ORDER BY priority DESC, isCustom DESC, name COLLATE NOCASE ASC
        """,
    )
    fun observeAll(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE id = :id")
    fun observeById(id: String): Flow<ExerciseEntity?>

    @Query("UPDATE exercises SET archived = 1 WHERE id = :id")
    suspend fun archive(id: String)

    /**
     * 只改「训练资料」类的四个字段，不碰名称、分类、肌群映射和资源路径，
     * 因此内置动作也能安全地补充别名、优先级和要点。
     */
    @Query(
        """
        UPDATE exercises SET
            aliases = :aliases,
            howTo = :howTo,
            cues = :cues
        WHERE id = :id
        """,
    )
    suspend fun updateTrainingFields(
        id: String,
        aliases: String?,
        howTo: String?,
        cues: String?,
    )

    /**
     * 收藏 / 取消收藏：状态与优先级在同一条 UPDATE 里落库，
     * 保证「isFavorite=true ⇔ priority=10」不会出现中间态。
     * 优先级由此统一管理，前台没有其他写 priority 的入口。
     */
    @Query(
        """
        UPDATE exercises SET
            isFavorite = :favorite,
            priority = CASE WHEN :favorite THEN 10 ELSE 0 END
        WHERE id = :id
        """,
    )
    suspend fun setFavorite(id: String, favorite: Boolean)

    /**
     * 当前动作的全部历史训练组。按日期倒序（同一天再按创建时间倒序），
     * 组内按 `setIndex` 升序保持训练时的顺序。
     */
    @Query(
        """
        SELECT workout_sets.sessionId AS sessionId,
               workout_sessions.dateEpochDay AS dateEpochDay,
               workout_sessions.createdAt AS createdAt,
               workout_sets.setIndex AS setIndex,
               workout_sets.weightMode AS weightMode,
               workout_sets.weightKg AS weightKg,
               workout_sets.reps AS reps
        FROM workout_sets
        INNER JOIN workout_sessions ON workout_sessions.id = workout_sets.sessionId
        WHERE workout_sets.exerciseId = :exerciseId
        ORDER BY workout_sessions.dateEpochDay DESC,
                 workout_sessions.createdAt DESC,
                 workout_sets.setIndex ASC
        """,
    )
    fun observeHistoryForExercise(exerciseId: String): Flow<List<ExerciseHistorySetRow>>

    @Query(
        """
        UPDATE exercises SET
            name = :name,
            nameEn = :nameEn,
            bodyPart = :bodyPart,
            equipmentType = :equipmentType,
            equipmentDetail = :equipmentDetail,
            bodyPartDetail = :bodyPartDetail,
            weightMode = :weightMode,
            source = :source,
            sourceId = :sourceId,
            bodyCategoryId = :bodyCategoryId,
            equipmentCategoryId = :equipmentCategoryId,
            classificationStatus = :classificationStatus,
            classificationVersion = :classificationVersion
        WHERE id = :id AND isCustom = 0 AND source = :source
        """,
    )
    suspend fun updateDatasetFields(
        id: String,
        name: String,
        nameEn: String?,
        bodyPart: String,
        equipmentType: String,
        equipmentDetail: String?,
        bodyPartDetail: String?,
        weightMode: String,
        source: String,
        sourceId: String?,
        bodyCategoryId: String?,
        equipmentCategoryId: String?,
        classificationStatus: String,
        classificationVersion: Int,
    )

    @Query(
        """
        UPDATE exercises SET
            bodyCategoryId = :bodyCategoryId,
            equipmentCategoryId = :equipmentCategoryId,
            classificationStatus = :classificationStatus,
            classificationVersion = :classificationVersion
        WHERE id = :id
        """,
    )
    suspend fun updateClassification(
        id: String,
        bodyCategoryId: String?,
        equipmentCategoryId: String?,
        classificationStatus: String,
        classificationVersion: Int,
    )

    @Query(
        """
        UPDATE exercises SET
            workingPrWeightKg = :workingPrWeightKg,
            workingPrReps = :workingPrReps,
            workingPrWeightMode = :workingPrWeightMode,
            workingPrDateEpochDay = :workingPrDateEpochDay,
            maxPrWeightKg = :maxPrWeightKg,
            maxPrReps = :maxPrReps,
            maxPrWeightMode = :maxPrWeightMode,
            maxPrDateEpochDay = :maxPrDateEpochDay
        WHERE id = :id
        """,
    )
    suspend fun updatePersonalRecords(
        id: String,
        workingPrWeightKg: Double?,
        workingPrReps: Int?,
        workingPrWeightMode: String?,
        workingPrDateEpochDay: Long?,
        maxPrWeightKg: Double?,
        maxPrReps: Int?,
        maxPrWeightMode: String?,
        maxPrDateEpochDay: Long?,
    )

    @Query(
        """
        SELECT * FROM exercises
        WHERE archived = 0
        AND (source = :datasetSource OR isCustom = 1)
        AND id NOT LIKE 'preset\_%' ESCAPE '\'
        AND (:query = ''
            OR name LIKE '%' || :query || '%' ESCAPE '\'
            OR IFNULL(nameEn, '') LIKE '%' || :query || '%' ESCAPE '\'
            OR IFNULL(aliases, '') LIKE '%' || :query || '%' ESCAPE '\')
        AND (:bodyCategoryId = '' OR bodyCategoryId = :bodyCategoryId)
        AND (:equipmentCategoryId = '' OR equipmentCategoryId = :equipmentCategoryId)
        ORDER BY name COLLATE NOCASE ASC
        """,
    )
    fun observeFiltered(
        query: String,
        bodyCategoryId: String,
        equipmentCategoryId: String,
        datasetSource: String,
    ): Flow<List<ExerciseEntity>>

    @Query(
        """
        SELECT workout_sets.exerciseId AS exerciseId,
               COUNT(DISTINCT workout_sets.sessionId) AS sessionCount,
               MAX(workout_sessions.dateEpochDay) AS lastEpochDay
        FROM workout_sets
        INNER JOIN workout_sessions ON workout_sessions.id = workout_sets.sessionId
        GROUP BY workout_sets.exerciseId
        """,
    )
    fun observeUsage(): Flow<List<ExerciseUsageRow>>
}
