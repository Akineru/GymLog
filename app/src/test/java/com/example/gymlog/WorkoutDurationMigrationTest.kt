package com.example.gymlog

import android.content.Context
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.gymlog.data.MIGRATION_17_18
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WorkoutDurationMigrationTest {
    @Test
    fun upgradeFrom17AddsDurationAndKeepsSession() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "migrate-workout-duration.db"
        context.deleteDatabase(name)
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(name)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(17) {
                        override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                            db.execSQL(
                                """
                                CREATE TABLE `workout_sessions` (
                                    `id` TEXT NOT NULL,
                                    `dateEpochDay` INTEGER NOT NULL,
                                    `note` TEXT,
                                    `createdAt` INTEGER NOT NULL,
                                    PRIMARY KEY(`id`)
                                )
                                """.trimIndent(),
                            )
                            db.execSQL(
                                "INSERT INTO workout_sessions (id, dateEpochDay, note, createdAt) " +
                                    "VALUES ('s1', 20737, '备注', 5)",
                            )
                        }

                        override fun onUpgrade(
                            db: androidx.sqlite.db.SupportSQLiteDatabase,
                            oldVersion: Int,
                            newVersion: Int,
                        ) = Unit
                    },
                )
                .build(),
        )
        val db = helper.writableDatabase
        MIGRATION_17_18.migrate(db)
        db.query(
            "SELECT id, dateEpochDay, note, createdAt, durationMillis FROM workout_sessions",
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("s1", cursor.getString(0))
            assertEquals(20_737L, cursor.getLong(1))
            assertEquals("备注", cursor.getString(2))
            assertEquals(5L, cursor.getLong(3))
            assertEquals(0L, cursor.getLong(4))
        }
        helper.close()
    }
}
