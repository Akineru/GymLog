package com.example.gymlog

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.gymlog.data.ExerciseSource
import com.example.gymlog.data.GymLogDatabase
import com.example.gymlog.data.MIGRATION_12_13
import com.example.gymlog.data.MIGRATION_13_14
import com.example.gymlog.data.MIGRATION_14_15
import com.example.gymlog.data.MIGRATION_15_16
import com.example.gymlog.data.MIGRATION_16_17
import com.example.gymlog.data.MIGRATION_17_18
import com.example.gymlog.data.MIGRATION_18_19
import com.example.gymlog.data.MIGRATION_19_20
import com.example.gymlog.data.MIGRATION_20_21
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExerciseCatalogFullMigrationTest {
    @Test
    fun fullV12SchemaMigratesAndOpensThroughRoom() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "migrate-exercise-full.db"
        context.deleteDatabase(name)
        val file = context.getDatabasePath(name)
        file.parentFile?.mkdirs()
        val raw = SQLiteDatabase.openOrCreateDatabase(file, null)
        raw.execSQL("PRAGMA foreign_keys = ON")
        raw.execSQL(
            """
            CREATE TABLE workout_records (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                dateEpochDay INTEGER NOT NULL,
                exerciseName TEXT NOT NULL,
                weightKg REAL NOT NULL,
                reps INTEGER NOT NULL,
                sets INTEGER NOT NULL,
                note TEXT NOT NULL
            )
            """.trimIndent(),
        )
        raw.execSQL(
            """
            CREATE TABLE exercises (
                id TEXT NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                bodyPart TEXT NOT NULL,
                equipmentType TEXT NOT NULL,
                weightMode TEXT NOT NULL,
                isCustom INTEGER NOT NULL,
                aliases TEXT,
                createdAt INTEGER NOT NULL,
                workingPrWeightKg REAL,
                workingPrReps INTEGER,
                workingPrWeightMode TEXT,
                workingPrDateEpochDay INTEGER,
                maxPrWeightKg REAL,
                maxPrReps INTEGER,
                maxPrWeightMode TEXT,
                maxPrDateEpochDay INTEGER,
                archived INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        raw.execSQL("CREATE INDEX index_exercises_bodyPart ON exercises (bodyPart)")
        raw.execSQL("CREATE INDEX index_exercises_equipmentType ON exercises (equipmentType)")
        raw.execSQL("CREATE INDEX index_exercises_name ON exercises (name)")
        raw.execSQL(
            """
            CREATE TABLE workout_sessions (
                id TEXT NOT NULL PRIMARY KEY,
                dateEpochDay INTEGER NOT NULL,
                note TEXT,
                createdAt INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        raw.execSQL("CREATE INDEX index_workout_sessions_dateEpochDay ON workout_sessions (dateEpochDay)")
        raw.execSQL(
            """
            CREATE TABLE workout_sets (
                id TEXT NOT NULL PRIMARY KEY,
                sessionId TEXT NOT NULL,
                exerciseId TEXT NOT NULL,
                exerciseOrder INTEGER NOT NULL,
                setIndex INTEGER NOT NULL,
                weightMode TEXT NOT NULL,
                weightKg REAL,
                reps INTEGER,
                note TEXT,
                FOREIGN KEY(sessionId) REFERENCES workout_sessions(id) ON DELETE CASCADE,
                FOREIGN KEY(exerciseId) REFERENCES exercises(id) ON DELETE RESTRICT
            )
            """.trimIndent(),
        )
        raw.execSQL("CREATE INDEX index_workout_sets_sessionId ON workout_sets (sessionId)")
        raw.execSQL("CREATE INDEX index_workout_sets_exerciseId ON workout_sets (exerciseId)")
        raw.execSQL(
            """
            CREATE TABLE body_profile (
                id INTEGER NOT NULL PRIMARY KEY,
                birthdayEpochDay INTEGER,
                gender TEXT,
                displayName TEXT,
                avatarFileName TEXT
            )
            """.trimIndent(),
        )
        raw.execSQL(
            """
            CREATE TABLE body_measurements (
                id TEXT NOT NULL PRIMARY KEY,
                recordedEpochDay INTEGER NOT NULL,
                heightCm REAL,
                weightKg REAL,
                bodyFatPercent REAL,
                waistCm REAL,
                armCm REAL,
                armLeftCm REAL,
                armRightCm REAL,
                forearmLeftCm REAL,
                forearmRightCm REAL,
                chestCm REAL,
                hipCm REAL,
                thighLeftCm REAL,
                thighRightCm REAL,
                calfLeftCm REAL,
                calfRightCm REAL,
                createdAt INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        raw.execSQL("CREATE INDEX index_body_measurements_recordedEpochDay ON body_measurements (recordedEpochDay)")
        raw.execSQL(
            """
            CREATE TABLE day_notes (
                dateEpochDay INTEGER NOT NULL PRIMARY KEY,
                message TEXT NOT NULL
            )
            """.trimIndent(),
        )
        raw.execSQL(
            """
            CREATE TABLE exercise_muscles (
                exerciseId TEXT NOT NULL,
                muscleId TEXT NOT NULL,
                role TEXT NOT NULL,
                sortOrder INTEGER NOT NULL,
                PRIMARY KEY(exerciseId, muscleId),
                FOREIGN KEY(exerciseId) REFERENCES exercises(id) ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        raw.execSQL("CREATE INDEX index_exercise_muscles_exerciseId ON exercise_muscles (exerciseId)")
        raw.execSQL(
            """
            CREATE TABLE timer_records (
                id TEXT NOT NULL PRIMARY KEY,
                durationMillis INTEGER NOT NULL,
                dateEpochDay INTEGER NOT NULL,
                sessionId TEXT,
                createdAt INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        raw.execSQL(
            """
            CREATE TABLE timer_laps (
                id TEXT NOT NULL PRIMARY KEY,
                recordId TEXT NOT NULL,
                lapIndex INTEGER NOT NULL,
                intervalMillis INTEGER NOT NULL,
                elapsedMillis INTEGER NOT NULL,
                FOREIGN KEY(recordId) REFERENCES timer_records(id) ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        raw.execSQL("CREATE INDEX index_timer_laps_recordId ON timer_laps (recordId)")
        raw.execSQL(
            """
            INSERT INTO exercises VALUES
            ('preset_barbell_bench_press', '杠铃卧推', 'CHEST', 'FREE_WEIGHT', 'WEIGHTED', 0,
             'bench press', 100, 80.0, 5, 'WEIGHTED', 20000, 100.0, 1, 'WEIGHTED', 20001, 1),
            ('custom_keep', '自定义', 'BACK', 'MACHINE', 'BODYWEIGHT', 1,
             NULL, 101, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0)
            """.trimIndent(),
        )
        raw.execSQL(
            """
            INSERT INTO workout_sessions VALUES ('s1', 20000, '备注', 3)
            """.trimIndent(),
        )
        raw.execSQL(
            """
            INSERT INTO workout_sets VALUES ('set1', 's1', 'custom_keep', 0, 0, 'BODYWEIGHT', NULL, 10, NULL)
            """.trimIndent(),
        )
        raw.execSQL(
            """
            INSERT INTO exercise_muscles VALUES ('custom_keep', 'biceps_brachii', 'PRIMARY', 0)
            """.trimIndent(),
        )
        raw.execSQL(
            """
            INSERT INTO workout_records (dateEpochDay, exerciseName, weightKg, reps, sets, note)
            VALUES (20000, '旧记录', 50.0, 5, 3, 'legacy')
            """.trimIndent(),
        )
        raw.execSQL(
            """
            INSERT INTO timer_records VALUES ('timer1', 60000, 20000, 's1', 4)
            """.trimIndent(),
        )
        raw.execSQL("INSERT INTO timer_laps VALUES ('lap1', 'timer1', 0, 60000, 60000)")
        raw.execSQL("INSERT INTO day_notes VALUES (20000, '休息')")
        raw.version = 12
        raw.close()

        val db = Room.databaseBuilder(context, GymLogDatabase::class.java, name)
            .addMigrations(MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20, MIGRATION_20_21)
            .build()
        try {
            val preset = db.exerciseDao().getById("preset_barbell_bench_press")!!
            assertEquals("杠铃卧推", preset.name)
            assertEquals("CHEST", preset.bodyPart)
            assertEquals("FREE_WEIGHT", preset.equipmentType)
            assertEquals("WEIGHTED", preset.weightMode)
            assertEquals("bench press", preset.aliases)
            assertEquals(100L, preset.createdAt)
            assertEquals(80.0, preset.workingPrWeightKg!!, 0.0)
            assertEquals(100.0, preset.maxPrWeightKg!!, 0.0)
            assertEquals(ExerciseSource.LEGACY, preset.source)
            assertTrue(preset.archived)
            val custom = db.exerciseDao().getById("custom_keep")!!
            assertEquals(ExerciseSource.CUSTOM, custom.source)
            assertEquals("BACK", custom.bodyPart)
            assertEquals("MACHINE", custom.equipmentType)
            assertEquals("BODYWEIGHT", custom.weightMode)
            assertTrue(custom.isCustom)
            assertEquals(101L, custom.createdAt)
            assertEquals("s1", db.sessionDao().getWithSets("s1")!!.session.id)
            assertEquals("custom_keep", db.sessionDao().getWithSets("s1")!!.sets.single().exerciseId)
            assertEquals("biceps_brachii", db.exerciseMuscleDao().all().single().muscleId)
            assertEquals(1, db.backupDao().allLegacyRecords().size)
            assertEquals(1, db.backupDao().allTimerRecords().size)
            assertEquals(1, db.backupDao().allTimerLaps().size)
            assertEquals("休息", db.backupDao().allDayNotes().single().message)
            assertNull(db.exerciseCatalogDao().importState(ExerciseSource.DATASET))
            assertEquals(0, db.exerciseCatalogDao().allContents().size)
            assertEquals(0, db.exerciseCatalogDao().allMedia().size)
            assertEquals(0, db.exerciseCatalogDao().allSourceMaps().size)
            assertTrue(db.exerciseCatalogDao().allMuscleTokens().isEmpty())

            db.openHelper.writableDatabase.execSQL(
                "INSERT INTO exercise_content VALUES ('custom_keep', 'zh', '自定义', '说明', '[]')",
            )
            db.openHelper.writableDatabase.execSQL(
                "INSERT INTO exercise_media (exerciseId, width, height, downloadState) " +
                    "VALUES ('custom_keep', 180, 180, 'NOT_BUNDLED')",
            )
            db.openHelper.writableDatabase.execSQL(
                "INSERT INTO exercise_source_map VALUES ('test', 'custom_keep', 'custom_keep', 'TEST', 1.0)",
            )
            db.openHelper.writableDatabase.execSQL(
                "INSERT INTO exercise_muscle_tokens VALUES ('custom_keep', 'TARGET', 'test', 0, 'MAPPED', 'biceps_brachii')",
            )
            db.openHelper.writableDatabase.execSQL("DELETE FROM workout_sets WHERE exerciseId = 'custom_keep'")
            db.openHelper.writableDatabase.execSQL("DELETE FROM exercises WHERE id = 'custom_keep'")
            assertTrue(db.exerciseCatalogDao().allContents().isEmpty())
            assertTrue(db.exerciseCatalogDao().allMedia().isEmpty())
            assertTrue(db.exerciseCatalogDao().allSourceMaps().isEmpty())
            assertTrue(db.exerciseCatalogDao().allMuscleTokens().isEmpty())
            assertTrue(db.exerciseMuscleDao().all().isEmpty())
        } finally {
            db.close()
        }
    }
}
