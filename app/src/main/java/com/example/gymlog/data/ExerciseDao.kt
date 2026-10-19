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
        ORDER BY isCustom DESC, name COLLATE NOCASE ASC
        """,
    )
    fun observeAll(): Flow<List<ExerciseEntity>>

    @Query("UPDATE exercises SET archived = 1 WHERE id = :id")
    suspend fun archive(id: String)

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
            OR name LIKE '%' || :query || '%' COLLATE NOCASE
            OR IFNULL(nameEn, '') LIKE '%' || :query || '%' COLLATE NOCASE
            OR IFNULL(aliases, '') LIKE '%' || :query || '%' COLLATE NOCASE)
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
