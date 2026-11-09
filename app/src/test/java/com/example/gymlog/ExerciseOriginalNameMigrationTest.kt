package com.example.gymlog

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.gymlog.data.MIGRATION_21_22
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExerciseOriginalNameMigrationTest {

    @Test
    fun upgradeFromVersion21AddsOriginalNameAndBackfillsDatasetRows() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "migrate-original-name.db"
        val file = context.getDatabasePath(name)
        file.parentFile?.mkdirs()
        if (file.exists()) file.delete()

        val raw = SQLiteDatabase.openOrCreateDatabase(file, null)
        raw.execSQL(
            """
            CREATE TABLE exercises (
                id TEXT NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                source TEXT NOT NULL DEFAULT 'LEGACY'
            )
            """.trimIndent(),
        )
        raw.execSQL(
            """
            INSERT INTO exercises (id, name, source) VALUES
            ('ex_1', '杠铃 坐姿 窄距 卧推', 'EXERCISES_DATASET_ZH'),
            ('preset_band_pull_apart', '弹力带拉开', 'LEGACY'),
            ('custom_1', '我的动作', 'CUSTOM')
            """.trimIndent(),
        )
        raw.version = 21
        raw.close()

        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(name)
                .callback(object : SupportSQLiteOpenHelper.Callback(22) {
                    override fun onCreate(db: SupportSQLiteDatabase) = Unit

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                        assertEquals(21, oldVersion)
                        MIGRATION_21_22.migrate(db)
                    }
                })
                .build(),
        )
        val db = helper.writableDatabase
        db.query("SELECT id, originalName FROM exercises ORDER BY id").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("custom_1", cursor.getString(0))
            assertEquals("我的动作", cursor.getString(1))
            assertTrue(cursor.moveToNext())
            assertEquals("ex_1", cursor.getString(0))
            assertEquals("窄距卧推", cursor.getString(1))
            assertTrue(cursor.moveToNext())
            assertEquals("preset_band_pull_apart", cursor.getString(0))
            assertEquals("弹力带拉开", cursor.getString(1))
        }
        db.close()
    }
}
