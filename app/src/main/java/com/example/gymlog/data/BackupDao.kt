package com.example.gymlog.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
abstract class BackupDao {
    @Query("SELECT * FROM exercises")
    abstract suspend fun allExercises(): List<ExerciseEntity>

    @Query("SELECT * FROM workout_sessions")
    abstract suspend fun allSessions(): List<WorkoutSessionEntity>

    @Query("SELECT * FROM workout_sets")
    abstract suspend fun allSets(): List<WorkoutSetEntity>

    @Query("SELECT * FROM exercise_durations")
    abstract suspend fun allExerciseDurations(): List<ExerciseDurationEntity>

    @Query("SELECT * FROM body_profile WHERE id = 1 LIMIT 1")
    abstract suspend fun profile(): BodyProfileEntity?

    @Query("SELECT * FROM body_measurements")
    abstract suspend fun allMeasurements(): List<BodyMeasurementEntity>

    @Query("SELECT * FROM workout_records")
    abstract suspend fun allLegacyRecords(): List<WorkoutRecord>

    @Query("SELECT * FROM day_notes")
    abstract suspend fun allDayNotes(): List<DayNoteEntity>

    @Query("SELECT * FROM day_kinds")
    abstract suspend fun allDayKinds(): List<DayKindEntity>

    @Query("SELECT * FROM exercise_muscles")
    abstract suspend fun allExerciseMuscles(): List<ExerciseMuscleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertExercises(rows: List<ExerciseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertSessions(rows: List<WorkoutSessionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertSets(rows: List<WorkoutSetEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertExerciseDurations(rows: List<ExerciseDurationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertProfile(profile: BodyProfileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertMeasurements(rows: List<BodyMeasurementEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertLegacy(rows: List<WorkoutRecord>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertDayNotes(rows: List<DayNoteEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertDayKinds(rows: List<DayKindEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertExerciseMuscles(rows: List<ExerciseMuscleEntity>)

    @Query("DELETE FROM workout_sets")
    abstract suspend fun clearSets()

    @Query("DELETE FROM exercise_durations")
    abstract suspend fun clearExerciseDurations()

    @Query("DELETE FROM workout_sessions")
    abstract suspend fun clearSessions()

    @Query("DELETE FROM exercises")
    abstract suspend fun clearExercises()

    @Query("DELETE FROM body_measurements")
    abstract suspend fun clearMeasurements()

    @Query("DELETE FROM body_profile")
    abstract suspend fun clearProfile()

    @Query("DELETE FROM workout_records")
    abstract suspend fun clearLegacy()

    @Query("DELETE FROM day_notes")
    abstract suspend fun clearDayNotes()

    @Query("DELETE FROM day_kinds")
    abstract suspend fun clearDayKinds()

    @Query("DELETE FROM exercise_muscles")
    abstract suspend fun clearExerciseMuscles()

    @Query("DELETE FROM exercise_muscle_tokens")
    abstract suspend fun clearExerciseMuscleTokens()

    @Query("DELETE FROM exercise_content")
    abstract suspend fun clearExerciseContent()

    @Query("DELETE FROM exercise_media")
    abstract suspend fun clearExerciseMedia()

    @Query("DELETE FROM exercise_source_map")
    abstract suspend fun clearExerciseSourceMap()

    @Query("DELETE FROM exercise_preset_match_candidate")
    abstract suspend fun clearExercisePresetCandidates()

    @Query("DELETE FROM exercise_import_state")
    abstract suspend fun clearExerciseImportState()

    @Query("SELECT * FROM timer_records")
    abstract suspend fun allTimerRecords(): List<TimerRecordEntity>

    @Query("SELECT * FROM timer_laps")
    abstract suspend fun allTimerLaps(): List<TimerLapEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertTimerRecords(rows: List<TimerRecordEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertTimerLaps(rows: List<TimerLapEntity>)

    @Query("DELETE FROM timer_laps")
    abstract suspend fun clearTimerLaps()

    @Query("DELETE FROM timer_records")
    abstract suspend fun clearTimerRecords()

    @Transaction
    open suspend fun clearAll() {
        clearExerciseDurations()
        clearSets()
        clearSessions()
        clearExerciseMuscles()
        clearExerciseMuscleTokens()
        clearExerciseContent()
        clearExerciseMedia()
        clearExerciseSourceMap()
        clearExercisePresetCandidates()
        clearExerciseImportState()
        clearExercises()
        clearMeasurements()
        clearProfile()
        clearLegacy()
        clearDayNotes()
        clearDayKinds()
        clearTimerLaps()
        clearTimerRecords()
    }

    @Transaction
    open suspend fun replaceAll(snapshot: GymLogSnapshot) {
        clearExerciseDurations()
        clearSets()
        clearSessions()
        clearExerciseMuscles()
        clearExerciseMuscleTokens()
        clearExerciseContent()
        clearExerciseMedia()
        clearExerciseSourceMap()
        clearExercisePresetCandidates()
        clearExerciseImportState()
        clearExercises()
        clearMeasurements()
        clearProfile()
        clearLegacy()
        clearDayNotes()
        clearDayKinds()
        if (snapshot.exercises.isNotEmpty()) insertExercises(snapshot.exercises)
        if (snapshot.sessions.isNotEmpty()) insertSessions(snapshot.sessions)
        if (snapshot.sets.isNotEmpty()) insertSets(snapshot.sets)
        if (snapshot.exerciseDurations.isNotEmpty()) {
            insertExerciseDurations(snapshot.exerciseDurations)
        }
        snapshot.profile?.let { insertProfile(it.copy(id = BodyProfileEntity.PROFILE_ID)) }
        if (snapshot.measurements.isNotEmpty()) insertMeasurements(snapshot.measurements)
        if (snapshot.legacyRecords.isNotEmpty()) insertLegacy(snapshot.legacyRecords)
        if (snapshot.dayNotes.isNotEmpty()) insertDayNotes(snapshot.dayNotes)
        if (snapshot.dayKinds.isNotEmpty()) insertDayKinds(snapshot.dayKinds)
        val customIds = snapshot.exercises.filter { it.isCustom }.map { it.id }.toSet()
        val customLinks = snapshot.exerciseMuscles.filter { it.exerciseId in customIds }
        if (customLinks.isNotEmpty()) {
            insertExerciseMuscles(customLinks.map { it.toEntity() })
        }
        val timerRecords = snapshot.timerRecords
        val timerLaps = snapshot.timerLaps
        if (timerRecords != null && timerLaps != null) {
            clearTimerLaps()
            clearTimerRecords()
            if (timerRecords.isNotEmpty()) insertTimerRecords(timerRecords)
            if (timerLaps.isNotEmpty()) insertTimerLaps(timerLaps)
        }
    }
}
