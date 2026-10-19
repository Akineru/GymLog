package com.example.gymlog

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.gymlog.data.GymLogDatabase
import com.example.gymlog.data.MIGRATION_10_11
import com.example.gymlog.data.MIGRATION_11_12
import com.example.gymlog.data.MIGRATION_12_13
import com.example.gymlog.data.MIGRATION_13_14
import com.example.gymlog.data.MIGRATION_14_15
import com.example.gymlog.data.MIGRATION_15_16
import com.example.gymlog.data.MIGRATION_9_10
import com.example.gymlog.data.WorkoutSessionEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TimerMigrationTest {
    @Test
    fun upgradeFromVersion9KeepsExistingSessionAndAddsTimerTables() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "migrate-timer.db"
        context.getDatabasePath(name).parentFile?.mkdirs()
        context.deleteDatabase(name)
        val created = Room.databaseBuilder(context, GymLogDatabase::class.java, name)
            .addMigrations(MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16)
            .build()
        created.sessionDao().upsertSession(
            WorkoutSessionEntity(id = "keep-me", dateEpochDay = 20_000, note = "备注", createdAt = 3),
        )
        val sqlite = created.openHelper.writableDatabase
        sqlite.execSQL("DROP TABLE IF EXISTS timer_laps")
        sqlite.execSQL("DROP TABLE IF EXISTS timer_records")
        sqlite.execSQL(
            """
            CREATE TABLE `body_profile_v9` (
                `id` INTEGER NOT NULL,
                `birthdayEpochDay` INTEGER,
                `gender` TEXT,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
        sqlite.execSQL(
            """
            INSERT INTO `body_profile_v9` (`id`, `birthdayEpochDay`, `gender`)
            SELECT `id`, `birthdayEpochDay`, `gender` FROM `body_profile`
            """.trimIndent(),
        )
        sqlite.execSQL("DROP TABLE `body_profile`")
        sqlite.execSQL("ALTER TABLE `body_profile_v9` RENAME TO `body_profile`")
        rewindMeasurementsToVersion11(sqlite)
        sqlite.execSQL("PRAGMA user_version = 9")
        created.close()

        val upgraded = Room.databaseBuilder(context, GymLogDatabase::class.java, name)
            .addMigrations(MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16)
            .build()
        val session = upgraded.sessionDao().getWithSets("keep-me")
        assertEquals("keep-me", session?.session?.id)
        assertEquals("备注", session?.session?.note)
        val table = upgraded.openHelper.readableDatabase.query(
            "SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'timer_records'",
        )
        assertTrue(table.moveToFirst())
        table.close()
        upgraded.close()
    }
}
