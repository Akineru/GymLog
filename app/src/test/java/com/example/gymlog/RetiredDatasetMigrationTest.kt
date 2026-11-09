package com.example.gymlog

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.gymlog.data.MIGRATION_22_23
import com.example.gymlog.data.MIGRATION_23_24
import com.example.gymlog.data.MIGRATION_24_25
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 数据集去重迁移：已导入的重复动作删掉，训练组和动作用时改挂到保留动作，
 * 子表（内容、媒体、来源映射、肌群词元、自定义肌群）不留孤儿行。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RetiredDatasetMigrationTest {

    /** 造一个 v22 的库。[withSquatKeeper] 为 false 时模拟保留动作也不在库里的极端情况。 */
    private fun openAtVersion22(name: String, withSquatKeeper: Boolean = true): SupportSQLiteDatabase {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = context.getDatabasePath(name)
        file.parentFile?.mkdirs()
        if (file.exists()) file.delete()

        val raw = SQLiteDatabase.openOrCreateDatabase(file, null)
        raw.execSQL("CREATE TABLE exercises (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL)")
        raw.execSQL("CREATE TABLE workout_sets (id TEXT NOT NULL PRIMARY KEY, exerciseId TEXT NOT NULL)")
        raw.execSQL("CREATE TABLE exercise_durations (id TEXT NOT NULL PRIMARY KEY, exerciseId TEXT NOT NULL)")
        raw.execSQL("CREATE TABLE exercise_content (exerciseId TEXT NOT NULL, locale TEXT NOT NULL, PRIMARY KEY(exerciseId, locale))")
        raw.execSQL("CREATE TABLE exercise_media (exerciseId TEXT NOT NULL PRIMARY KEY, mediaId TEXT)")
        raw.execSQL("CREATE TABLE exercise_source_map (source TEXT NOT NULL, sourceId TEXT NOT NULL, localExerciseId TEXT NOT NULL)")
        raw.execSQL("CREATE TABLE exercise_muscle_tokens (exerciseId TEXT NOT NULL, bucket TEXT NOT NULL, token TEXT NOT NULL)")
        raw.execSQL("CREATE TABLE exercise_muscles (exerciseId TEXT NOT NULL, muscleId TEXT NOT NULL, role TEXT NOT NULL)")
        raw.execSQL(
            """
            INSERT INTO exercises (id, name) VALUES
            ('ds_0576', '杠杆机 胸部推举'),
            ('ds_0577', '杠杆机 胸部推举'),
            ('ds_1461', '杠铃 完全深蹲 (后视图)'),
            ('custom_1', '我的动作')
            """.trimIndent(),
        )
        if (withSquatKeeper) {
            raw.execSQL("INSERT INTO exercises (id, name) VALUES ('ds_0043', '杠铃 完全深蹲')")
        }
        raw.execSQL(
            """
            INSERT INTO workout_sets (id, exerciseId) VALUES
            ('set1', 'ds_0576'),
            ('set2', 'ds_0576'),
            ('set3', 'ds_1461')
            """.trimIndent(),
        )
        raw.execSQL("INSERT INTO exercise_durations (id, exerciseId) VALUES ('dur1', 'ds_0576')")
        raw.execSQL(
            """
            INSERT INTO exercise_content (exerciseId, locale) VALUES
            ('ds_0576', 'zh'), ('ds_0577', 'zh')
            """.trimIndent(),
        )
        raw.execSQL(
            """
            INSERT INTO exercise_media (exerciseId, mediaId) VALUES
            ('ds_0576', 'media-0576'), ('ds_0577', 'media-0577')
            """.trimIndent(),
        )
        raw.execSQL(
            """
            INSERT INTO exercise_source_map (source, sourceId, localExerciseId) VALUES
            ('dataset', '0576', 'ds_0576'), ('dataset', '0577', 'ds_0577')
            """.trimIndent(),
        )
        raw.execSQL(
            """
            INSERT INTO exercise_muscle_tokens (exerciseId, bucket, token) VALUES
            ('ds_0576', 'chest', '胸'), ('ds_0577', 'chest', '胸')
            """.trimIndent(),
        )
        raw.execSQL(
            """
            INSERT INTO exercise_muscles (exerciseId, muscleId, role) VALUES
            ('ds_0576', 'chest', 'primary'), ('ds_0577', 'chest', 'primary')
            """.trimIndent(),
        )
        raw.version = 22
        raw.close()

        return openMigrating(context, name, fromVersion = 22)
    }

    /** [fromVersion] 的库按真实路径逐级升级到当前版本。 */
    private fun openMigrating(
        context: Context,
        name: String,
        fromVersion: Int,
    ): SupportSQLiteDatabase {
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(name)
                .callback(object : SupportSQLiteOpenHelper.Callback(25) {
                    override fun onCreate(db: SupportSQLiteDatabase) = Unit

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                        assertEquals(fromVersion, oldVersion)
                        if (oldVersion < 23) MIGRATION_22_23.migrate(db)
                        MIGRATION_23_24.migrate(db)
                        MIGRATION_24_25.migrate(db)
                    }
                })
                .build(),
        )
        return helper.writableDatabase
    }

    private fun SupportSQLiteDatabase.count(sql: String, vararg args: Any): Int =
        query(sql, args).use { cursor ->
            assertTrue(cursor.moveToFirst())
            cursor.getInt(0)
        }

    @Test
    fun setsAndDurationsFollowTheKeptTwin() {
        val db = openAtVersion22("dedupe23.db")

        db.query("SELECT id FROM exercises ORDER BY id").use { cursor ->
            val ids = ArrayList<String>()
            while (cursor.moveToNext()) ids += cursor.getString(0)
            assertEquals(listOf("custom_1", "ds_0043", "ds_0577"), ids)
        }
        db.query("SELECT id, exerciseId FROM workout_sets ORDER BY id").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("set1", cursor.getString(0))
            assertEquals("ds_0577", cursor.getString(1))
            assertTrue(cursor.moveToNext())
            assertEquals("set2", cursor.getString(0))
            assertEquals("ds_0577", cursor.getString(1))
            assertTrue(cursor.moveToNext())
            assertEquals("set3", cursor.getString(0))
            assertEquals("ds_0043", cursor.getString(1))
        }
        db.query("SELECT exerciseId FROM exercise_durations").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("ds_0577", cursor.getString(0))
        }
        db.close()
    }

    @Test
    fun childRowsOfTheDeletedTwinAreGone() {
        val db = openAtVersion22("dedupe23c.db")

        listOf(
            "exercise_content" to "exerciseId",
            "exercise_media" to "exerciseId",
            "exercise_source_map" to "localExerciseId",
            "exercise_muscle_tokens" to "exerciseId",
            "exercise_muscles" to "exerciseId",
        ).forEach { (table, column) ->
            assertEquals("$table 里还留着已删动作的行", 0, db.count("SELECT count(*) FROM $table WHERE $column = 'ds_0576'"))
            assertEquals("$table 把保留动作的行也清掉了", 1, db.count("SELECT count(*) FROM $table WHERE $column = 'ds_0577'"))
        }
        db.close()
    }

    @Test
    fun missingKeeperLeavesTheRowAlone() {
        val db = openAtVersion22("dedupe23b.db", withSquatKeeper = false)

        assertEquals(1, db.count("SELECT count(*) FROM exercises WHERE id = 'ds_1461'"))
        assertEquals(1, db.count("SELECT count(*) FROM workout_sets WHERE id = 'set3' AND exerciseId = 'ds_1461'"))
        db.close()
    }

    /** 已经升到 23 的库（22 → 23 那版漏了子表清理）再升一级，孤儿行被清掉，业务数据不动。 */
    @Test
    fun version23WithOrphanChildRowsIsCleaned() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "dedupe24.db"
        val file = context.getDatabasePath(name)
        file.parentFile?.mkdirs()
        if (file.exists()) file.delete()

        val raw = SQLiteDatabase.openOrCreateDatabase(file, null)
        raw.execSQL("CREATE TABLE exercises (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL)")
        raw.execSQL("CREATE TABLE workout_sets (id TEXT NOT NULL PRIMARY KEY, exerciseId TEXT NOT NULL)")
        raw.execSQL("CREATE TABLE exercise_durations (id TEXT NOT NULL PRIMARY KEY, exerciseId TEXT NOT NULL)")
        raw.execSQL("CREATE TABLE exercise_content (exerciseId TEXT NOT NULL, locale TEXT NOT NULL, PRIMARY KEY(exerciseId, locale))")
        raw.execSQL("CREATE TABLE exercise_media (exerciseId TEXT NOT NULL PRIMARY KEY, mediaId TEXT)")
        raw.execSQL("CREATE TABLE exercise_source_map (source TEXT NOT NULL, sourceId TEXT NOT NULL, localExerciseId TEXT NOT NULL)")
        raw.execSQL("CREATE TABLE exercise_muscle_tokens (exerciseId TEXT NOT NULL, bucket TEXT NOT NULL, token TEXT NOT NULL)")
        raw.execSQL("CREATE TABLE exercise_muscles (exerciseId TEXT NOT NULL, muscleId TEXT NOT NULL, role TEXT NOT NULL)")
        raw.execSQL("INSERT INTO exercises (id, name) VALUES ('ds_0577', '杠杆机 胸部推举')")
        raw.execSQL("INSERT INTO workout_sets (id, exerciseId) VALUES ('set1', 'ds_0577')")
        // 22 → 23 之后残留的孤儿行
        raw.execSQL("INSERT INTO exercise_content (exerciseId, locale) VALUES ('ds_0576', 'zh')")
        raw.execSQL("INSERT INTO exercise_media (exerciseId, mediaId) VALUES ('ds_0576', 'media-0576')")
        raw.execSQL("INSERT INTO exercise_source_map (source, sourceId, localExerciseId) VALUES ('dataset', '0576', 'ds_0576')")
        raw.execSQL("INSERT INTO exercise_muscle_tokens (exerciseId, bucket, token) VALUES ('ds_0576', 'chest', '胸')")
        raw.execSQL("INSERT INTO exercise_muscles (exerciseId, muscleId, role) VALUES ('ds_0576', 'chest', 'primary')")
        raw.version = 23
        raw.close()

        val db = openMigrating(context, name, fromVersion = 23)

        listOf(
            "exercise_content" to "exerciseId",
            "exercise_media" to "exerciseId",
            "exercise_source_map" to "localExerciseId",
            "exercise_muscle_tokens" to "exerciseId",
            "exercise_muscles" to "exerciseId",
        ).forEach { (table, column) ->
            assertEquals("$table 的孤儿行没清干净", 0, db.count("SELECT count(*) FROM $table"))
        }
        assertEquals(1, db.count("SELECT count(*) FROM exercises"))
        assertEquals(1, db.count("SELECT count(*) FROM workout_sets"))
        db.close()
    }
}
