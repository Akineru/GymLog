package com.example.gymlog

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.gymlog.data.GymLogDatabase
import com.example.gymlog.data.MIGRATION_15_16
import com.example.gymlog.data.PhotoPaths
import com.example.gymlog.data.PhotoWallEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PhotoWallMigrationTest {
    @Test
    fun upgradeFrom15AddsPhotoWallAndKeepsDayNotes() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "migrate-photo-wall.db"
        context.deleteDatabase(name)
        val created = Room.databaseBuilder(context, GymLogDatabase::class.java, name)
            .addMigrations(MIGRATION_15_16)
            .build()
        created.openHelper.writableDatabase.execSQL(
            "INSERT INTO day_notes (dateEpochDay, message) VALUES (7, '留下')",
        )
        created.openHelper.writableDatabase.execSQL("DROP TABLE photo_wall")
        created.openHelper.writableDatabase.execSQL("PRAGMA user_version = 15")
        created.close()

        val upgraded = Room.databaseBuilder(context, GymLogDatabase::class.java, name)
            .addMigrations(MIGRATION_15_16)
            .build()
        assertEquals("留下", upgraded.backupDao().allDayNotes().single().message)
        val id = "photo1"
        upgraded.photoWallDao().insert(
            PhotoWallEntity(
                id = id,
                originalRelativePath = PhotoPaths.original(id),
                editedRelativePath = null,
                dateEpochDay = 20_000,
                createdAt = 1,
                updatedAt = 1,
                note = "",
                width = 8,
                height = 8,
            ),
        )
        assertEquals(id, upgraded.photoWallDao().get(id)?.id)
        upgraded.close()
    }
}
