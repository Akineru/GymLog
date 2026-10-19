package com.example.gymlog.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseCatalogDao {
    @Query("SELECT COUNT(*) FROM exercise_source_map WHERE source = :source")
    suspend fun sourceMapCount(source: String): Int

    @Query("SELECT COUNT(*) FROM exercises WHERE source = :source")
    suspend fun exerciseCountBySource(source: String): Int

    @Query("SELECT * FROM exercise_import_state WHERE source = :source LIMIT 1")
    suspend fun importState(source: String): ExerciseImportStateEntity?

    @Query("SELECT COUNT(*) FROM exercise_muscle_tokens WHERE exerciseId = :exerciseId")
    suspend fun muscleTokenCount(exerciseId: String): Int

    @Query("SELECT COUNT(*) FROM exercise_content WHERE exerciseId = :exerciseId")
    suspend fun contentCount(exerciseId: String): Int

    @Query("SELECT COUNT(*) FROM exercise_media WHERE exerciseId = :exerciseId")
    suspend fun mediaCount(exerciseId: String): Int

    @Query("SELECT * FROM exercise_muscle_tokens WHERE exerciseId = :exerciseId ORDER BY bucket, sortOrder")
    suspend fun tokensForExercise(exerciseId: String): List<ExerciseMuscleTokenEntity>

    @Query("SELECT * FROM exercise_media WHERE exerciseId = :exerciseId LIMIT 1")
    suspend fun mediaForExercise(exerciseId: String): ExerciseMediaEntity?

    @Query("SELECT * FROM exercise_media")
    suspend fun allMedia(): List<ExerciseMediaEntity>

    @Query("SELECT * FROM exercise_media")
    fun observeAllMedia(): Flow<List<ExerciseMediaEntity>>

    @Query("SELECT * FROM exercise_content")
    suspend fun allContents(): List<ExerciseContentEntity>

    @Query("SELECT * FROM exercise_source_map")
    suspend fun allSourceMaps(): List<ExerciseSourceMapEntity>

    @Query("SELECT * FROM exercise_muscle_tokens")
    suspend fun allMuscleTokens(): List<ExerciseMuscleTokenEntity>

    @Query("SELECT * FROM exercise_content WHERE exerciseId = :exerciseId ORDER BY locale")
    suspend fun contentForExercise(exerciseId: String): List<ExerciseContentEntity>

    @Query("SELECT COUNT(*) FROM exercise_source_map")
    suspend fun totalSourceMaps(): Int

    @Query("SELECT COUNT(*) FROM exercise_muscle_tokens")
    suspend fun totalMuscleTokens(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContents(rows: List<ExerciseContentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedia(rows: List<ExerciseMediaEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSourceMaps(rows: List<ExerciseSourceMapEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMuscleTokens(rows: List<ExerciseMuscleTokenEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPresetCandidates(rows: List<ExercisePresetMatchCandidateEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putImportState(state: ExerciseImportStateEntity)

    @Query("DELETE FROM exercise_muscle_tokens WHERE exerciseId = :exerciseId")
    suspend fun deleteMuscleTokens(exerciseId: String)

    @Query("DELETE FROM exercise_content WHERE exerciseId = :exerciseId")
    suspend fun deleteContent(exerciseId: String)

    @Query("DELETE FROM exercise_media WHERE exerciseId = :exerciseId")
    suspend fun deleteMedia(exerciseId: String)

    @Query("DELETE FROM exercise_source_map WHERE localExerciseId = :exerciseId")
    suspend fun deleteSourceMapForExercise(exerciseId: String)

    @Query("DELETE FROM exercise_preset_match_candidate")
    suspend fun clearPresetCandidates()

    @Query("DELETE FROM exercise_import_state")
    suspend fun clearImportState()

    @Query("SELECT * FROM exercise_preset_match_candidate ORDER BY presetExerciseId")
    suspend fun presetCandidates(): List<ExercisePresetMatchCandidateEntity>
}
