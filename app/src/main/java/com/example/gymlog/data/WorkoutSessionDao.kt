package com.example.gymlog.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

data class SessionWithSets(
    @Embedded val session: WorkoutSessionEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "sessionId",
    )
    val sets: List<WorkoutSetEntity>,
)

data class DatePartRow(
    val dateEpochDay: Long,
    val part: String,
    val volume: Double = 0.0,
)

data class DailyVolumeRow(
    val dateEpochDay: Long,
    val volume: Double,
)

data class SetVolumeRow(
    val dateEpochDay: Long,
    val part: String,
    val weightKg: Double?,
    val reps: Int?,
    val weightMode: String,
)

data class SetForPrRow(
    val exerciseId: String,
    val sessionId: String,
    val dateEpochDay: Long,
    val weightKg: Double?,
    val reps: Int?,
    val weightMode: String,
)

@Dao
abstract class WorkoutSessionDao {
    @Transaction
    @Query("SELECT * FROM workout_sessions ORDER BY dateEpochDay DESC, createdAt DESC")
    abstract fun observeAllWithSets(): Flow<List<SessionWithSets>>

    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE id = :id")
    abstract suspend fun getWithSets(id: String): SessionWithSets?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertSession(session: WorkoutSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertSets(sets: List<WorkoutSetEntity>)

    @Query("DELETE FROM workout_sets WHERE sessionId = :sessionId")
    abstract suspend fun deleteSetsForSession(sessionId: String)

    @Delete
    abstract suspend fun deleteSession(session: WorkoutSessionEntity)

    @Query("SELECT * FROM exercises WHERE id IN (:ids)")
    abstract suspend fun getExercisesByIds(ids: List<String>): List<ExerciseEntity>

    @Query("SELECT createdAt FROM workout_sessions WHERE id = :id")
    abstract suspend fun getCreatedAt(id: String): Long?

    @Query(
        """
        SELECT workout_sets.* FROM workout_sets
        INNER JOIN workout_sessions ON workout_sessions.id = workout_sets.sessionId
        WHERE workout_sets.exerciseId = :exerciseId
        ORDER BY workout_sessions.dateEpochDay DESC,
                 workout_sessions.createdAt DESC,
                 workout_sets.setIndex DESC
        LIMIT 1
        """,
    )
    abstract suspend fun latestSetForExercise(exerciseId: String): WorkoutSetEntity?

    @Query(
        """
        SELECT workout_sessions.dateEpochDay AS dateEpochDay,
               COALESCE(NULLIF(exercises.bodyCategoryId, ''), exercises.bodyPart) AS part,
               workout_sets.weightKg AS weightKg,
               workout_sets.reps AS reps,
               workout_sets.weightMode AS weightMode
        FROM workout_sessions
        INNER JOIN workout_sets ON workout_sets.sessionId = workout_sessions.id
        INNER JOIN exercises ON exercises.id = workout_sets.exerciseId
        WHERE workout_sessions.dateEpochDay BETWEEN :startEpochDay AND :endEpochDay
        """,
    )
    abstract fun observeSetsInRange(
        startEpochDay: Long,
        endEpochDay: Long,
    ): Flow<List<SetVolumeRow>>

    @Query(
        """
        SELECT workout_sets.exerciseId AS exerciseId,
               workout_sets.sessionId AS sessionId,
               workout_sessions.dateEpochDay AS dateEpochDay,
               workout_sets.weightKg AS weightKg,
               workout_sets.reps AS reps,
               workout_sets.weightMode AS weightMode
        FROM workout_sets
        INNER JOIN workout_sessions ON workout_sessions.id = workout_sets.sessionId
        WHERE workout_sets.exerciseId IN (:exerciseIds)
        """,
    )
    abstract suspend fun getSetsForPr(exerciseIds: List<String>): List<SetForPrRow>

    @Query(
        """
        SELECT workout_sets.exerciseId AS exerciseId,
               workout_sets.sessionId AS sessionId,
               workout_sessions.dateEpochDay AS dateEpochDay,
               workout_sets.weightKg AS weightKg,
               workout_sets.reps AS reps,
               workout_sets.weightMode AS weightMode
        FROM workout_sets
        INNER JOIN workout_sessions ON workout_sessions.id = workout_sets.sessionId
        """,
    )
    abstract suspend fun getAllSetsForPr(): List<SetForPrRow>

    @Query(
        """
        SELECT workout_sets.exerciseId AS exerciseId,
               workout_sets.sessionId AS sessionId,
               workout_sessions.dateEpochDay AS dateEpochDay,
               workout_sets.weightKg AS weightKg,
               workout_sets.reps AS reps,
               workout_sets.weightMode AS weightMode
        FROM workout_sets
        INNER JOIN workout_sessions ON workout_sessions.id = workout_sets.sessionId
        ORDER BY workout_sessions.dateEpochDay ASC
        """,
    )
    abstract fun observeAllSetsForPr(): Flow<List<SetForPrRow>>

    @Transaction
    open suspend fun replaceSession(session: WorkoutSessionEntity, sets: List<WorkoutSetEntity>) {
        upsertSession(session)
        deleteSetsForSession(session.id)
        if (sets.isNotEmpty()) {
            upsertSets(sets)
        }
    }
}
