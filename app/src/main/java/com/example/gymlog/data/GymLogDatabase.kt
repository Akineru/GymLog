package com.example.gymlog.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        WorkoutRecord::class,
        ExerciseEntity::class,
        WorkoutSessionEntity::class,
        WorkoutSetEntity::class,
        BodyProfileEntity::class,
        BodyMeasurementEntity::class,
        DayNoteEntity::class,
        DayKindEntity::class,
        ExerciseMuscleEntity::class,
        ExerciseContentEntity::class,
        ExerciseMediaEntity::class,
        ExerciseSourceMapEntity::class,
        ExerciseMuscleTokenEntity::class,
        ExercisePresetMatchCandidateEntity::class,
        ExerciseImportStateEntity::class,
        TimerRecordEntity::class,
        TimerLapEntity::class,
        PhotoWallEntity::class,
    ],
    version = 16,
    exportSchema = false,
)
abstract class GymLogDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun sessionDao(): WorkoutSessionDao
    abstract fun bodyRecordDao(): BodyRecordDao
    abstract fun backupDao(): BackupDao

    abstract fun dayNoteDao(): DayNoteDao

    abstract fun dayKindDao(): DayKindDao

    abstract fun exerciseMuscleDao(): ExerciseMuscleDao

    abstract fun exerciseCatalogDao(): ExerciseCatalogDao

    abstract fun timerRecordDao(): TimerRecordDao

    abstract fun photoWallDao(): PhotoWallDao
}
