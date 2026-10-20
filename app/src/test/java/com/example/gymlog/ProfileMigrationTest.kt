package com.example.gymlog

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.gymlog.data.BodyProfileEntity
import com.example.gymlog.data.Gender
import com.example.gymlog.data.GymLogDatabase
import com.example.gymlog.data.MIGRATION_10_11
import com.example.gymlog.data.MIGRATION_11_12
import com.example.gymlog.data.MIGRATION_12_13
import com.example.gymlog.data.MIGRATION_13_14
import com.example.gymlog.data.MIGRATION_14_15
import com.example.gymlog.data.MIGRATION_15_16
import com.example.gymlog.data.MIGRATION_16_17
import com.example.gymlog.data.MIGRATION_17_18
import com.example.gymlog.data.MIGRATION_18_19
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProfileMigrationTest {
    @Test
    fun upgradeFromVersion10KeepsBirthdayAndGender() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "migrate-profile.db"
        context.deleteDatabase(name)
        val created = Room.databaseBuilder(context, GymLogDatabase::class.java, name)
            .addMigrations(MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19)
            .build()
        created.bodyRecordDao().upsertProfile(
            BodyProfileEntity(birthdayEpochDay = 10_000, gender = Gender.MALE.name),
        )
        val sqlite = created.openHelper.writableDatabase
        sqlite.execSQL(
            """
            CREATE TABLE `body_profile_v10` (
                `id` INTEGER NOT NULL,
                `birthdayEpochDay` INTEGER,
                `gender` TEXT,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
        sqlite.execSQL(
            """
            INSERT INTO `body_profile_v10` (`id`, `birthdayEpochDay`, `gender`)
            SELECT `id`, `birthdayEpochDay`, `gender` FROM `body_profile`
            """.trimIndent(),
        )
        sqlite.execSQL("DROP TABLE `body_profile`")
        sqlite.execSQL("ALTER TABLE `body_profile_v10` RENAME TO `body_profile`")
        rewindMeasurementsToVersion11(sqlite)
        sqlite.execSQL("PRAGMA user_version = 10")
        created.close()

        val upgraded = Room.databaseBuilder(context, GymLogDatabase::class.java, name)
            .addMigrations(MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19)
            .build()
        val profile = upgraded.bodyRecordDao().getProfile()
        assertEquals(10_000L, profile?.birthdayEpochDay)
        assertEquals(Gender.MALE.name, profile?.gender)
        assertNull(profile?.displayName)
        assertNull(profile?.avatarFileName)
        upgraded.close()
    }
}
