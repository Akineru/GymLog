package com.example.gymlog

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.gymlog.data.DayKind
import com.example.gymlog.data.DayKindEntity
import com.example.gymlog.data.GymLogDatabase
import com.example.gymlog.data.MIGRATION_14_15
import com.example.gymlog.data.MIGRATION_15_16
import com.example.gymlog.data.MIGRATION_16_17
import com.example.gymlog.data.MIGRATION_17_18
import com.example.gymlog.data.MIGRATION_18_19
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DayKindMigrationTest {
    @Test
    fun upgradeFromVersion14AddsDayKindsWithoutTouchingNotes() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "migrate-day-kind.db"
        context.deleteDatabase(name)
        val created = Room.databaseBuilder(context, GymLogDatabase::class.java, name)
            .addMigrations(MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19)
            .build()
        created.openHelper.writableDatabase.execSQL(
            "INSERT INTO day_notes (dateEpochDay, message) VALUES (20, '记下')",
        )
        created.openHelper.writableDatabase.execSQL("DROP TABLE day_kinds")
        created.openHelper.writableDatabase.execSQL("PRAGMA user_version = 14")
        created.close()

        val upgraded = Room.databaseBuilder(context, GymLogDatabase::class.java, name)
            .addMigrations(MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19)
            .build()
        assertEquals("记下", upgraded.backupDao().allDayNotes().single().message)
        upgraded.dayKindDao().upsert(DayKindEntity(dateEpochDay = 20, kind = DayKind.REST.name))
        assertEquals(DayKind.REST.name, upgraded.backupDao().allDayKinds().single().kind)
        upgraded.close()
    }
}
