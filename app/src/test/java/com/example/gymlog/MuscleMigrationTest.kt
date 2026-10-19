package com.example.gymlog

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.gymlog.data.MIGRATION_7_8
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MuscleMigrationTest {
    @Test
    fun upgradeFromVersion7KeepsExistingSession() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "migrate-muscles.db"
        val file = context.getDatabasePath(name)
        file.parentFile?.mkdirs()
        if (file.exists()) file.delete()
        val raw = SQLiteDatabase.openOrCreateDatabase(file, null)
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
        raw.execSQL(
            "INSERT INTO workout_sessions (id, dateEpochDay, note, createdAt) VALUES ('keep-me', 20000, '备注', 3)",
        )
        raw.version = 7
        raw.close()

        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(name)
                .callback(object : SupportSQLiteOpenHelper.Callback(8) {
                    override fun onCreate(db: SupportSQLiteDatabase) = Unit

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                        assertEquals(7, oldVersion)
                        MIGRATION_7_8.migrate(db)
                    }
                })
                .build(),
        )
        val db = helper.writableDatabase
        val session = db.query("SELECT id, note FROM workout_sessions")
        assertTrue(session.moveToFirst())
        assertEquals("keep-me", session.getString(0))
        assertEquals("备注", session.getString(1))
        session.close()
        val table = db.query(
            "SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'exercise_muscles'",
        )
        assertTrue(table.moveToFirst())
        table.close()
        db.close()
    }
}
