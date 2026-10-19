package com.example.gymlog

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.gymlog.data.MIGRATION_12_13
import com.example.gymlog.data.MIGRATION_13_14
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExerciseCatalogMigrationTest {
    @Test
    fun upgradeFromVersion12KeepsOldRowsAndAddsCatalogSchema() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "migrate-exercise-catalog.db"
        val file = context.getDatabasePath(name)
        file.parentFile?.mkdirs()
        if (file.exists()) file.delete()
        val raw = SQLiteDatabase.openOrCreateDatabase(file, null)
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
        raw.execSQL(
            """
            INSERT INTO exercises VALUES
            ('preset_barbell_bench_press', '杠铃卧推', 'CHEST', 'FREE_WEIGHT', 'WEIGHTED', 0, 'bench press', 100, 80.0, 5, 'WEIGHTED', 20000, 100.0, 1, 'WEIGHTED', 20001, 0),
            ('custom_1', '我的动作', 'BACK', 'MACHINE', 'BODYWEIGHT', 1, NULL, 101, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 1)
            """.trimIndent(),
        )
        raw.version = 12
        raw.close()

        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(name)
                .callback(object : SupportSQLiteOpenHelper.Callback(14) {
                    override fun onCreate(db: SupportSQLiteDatabase) = Unit

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                        assertEquals(12, oldVersion)
                        MIGRATION_12_13.migrate(db)
                        MIGRATION_13_14.migrate(db)
                    }
                })
                .build(),
        )
        val db = helper.writableDatabase
        db.query("SELECT COUNT(*) FROM exercises").use {
            assertTrue(it.moveToFirst())
            assertEquals(2, it.getInt(0))
        }
        db.query(
            "SELECT id, source, sourceId, nameEn, equipmentDetail, bodyPartDetail, archived, " +
                "workingPrWeightKg, maxPrWeightKg, bodyCategoryId, equipmentCategoryId, " +
                "classificationStatus, classificationVersion FROM exercises ORDER BY id",
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("custom_1", cursor.getString(0))
            assertEquals("CUSTOM", cursor.getString(1))
            assertNull(cursor.getString(2))
            assertNull(cursor.getString(3))
            assertNull(cursor.getString(4))
            assertNull(cursor.getString(5))
            assertEquals(1, cursor.getInt(6))
            assertTrue(cursor.isNull(7))
            assertTrue(cursor.isNull(8))
            assertTrue(cursor.isNull(9))
            assertTrue(cursor.isNull(10))
            assertEquals("UNMIGRATED", cursor.getString(11))
            assertEquals(0, cursor.getInt(12))
            assertTrue(cursor.moveToNext())
            assertEquals("preset_barbell_bench_press", cursor.getString(0))
            assertEquals("LEGACY", cursor.getString(1))
            assertNull(cursor.getString(2))
            assertEquals(0, cursor.getInt(6))
            assertEquals(80.0, cursor.getDouble(7), 0.001)
            assertEquals(100.0, cursor.getDouble(8), 0.001)
            assertTrue(cursor.isNull(9))
            assertTrue(cursor.isNull(10))
            assertEquals("UNMIGRATED", cursor.getString(11))
            assertEquals(0, cursor.getInt(12))
        }
        listOf(
            "exercise_content",
            "exercise_media",
            "exercise_source_map",
            "exercise_muscle_tokens",
            "exercise_preset_match_candidate",
            "exercise_import_state",
        ).forEach { table ->
            db.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name = ?", arrayOf(table)).use {
                assertTrue("missing $table", it.moveToFirst())
            }
        }
        db.close()
    }
}
