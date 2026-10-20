package com.example.gymlog

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.gymlog.data.GymLogDatabase
import com.example.gymlog.data.MIGRATION_16_17
import com.example.gymlog.data.MIGRATION_17_18
import com.example.gymlog.data.MIGRATION_18_19
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BodyMeasurementMigrationTest {
    @Test
    fun upgradeFrom16DropsLegacyArmColumnAndKeepsOtherMetrics() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "migrate-remove-legacy-arm.db"
        context.deleteDatabase(name)

        val created = Room.databaseBuilder(context, GymLogDatabase::class.java, name)
            .addMigrations(MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19)
            .build()
        val sqlite = created.openHelper.writableDatabase
        sqlite.execSQL("ALTER TABLE body_measurements ADD COLUMN armCm REAL")
        sqlite.execSQL(
            """
            INSERT INTO body_measurements (
                id, recordedEpochDay, heightCm, weightKg, bodyFatPercent,
                waistCm, armCm, armLeftCm, armRightCm, createdAt
            ) VALUES (
                'old', 20737, 177.0, 75.0, 17.0,
                82.0, 30.0, 31.0, 32.0, 5
            )
            """.trimIndent(),
        )
        sqlite.execSQL("PRAGMA user_version = 16")
        created.close()

        val upgraded = Room.databaseBuilder(context, GymLogDatabase::class.java, name)
            .addMigrations(MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19)
            .build()
        val row = upgraded.bodyRecordDao().listMeasurements().single()
        assertEquals(75.0, row.weightKg!!, 0.001)
        assertEquals(17.0, row.bodyFatPercent!!, 0.001)
        assertEquals(82.0, row.waistCm!!, 0.001)
        assertEquals(31.0, row.armLeftCm!!, 0.001)
        assertEquals(32.0, row.armRightCm!!, 0.001)

        upgraded.openHelper.readableDatabase.query(
            "PRAGMA table_info(`body_measurements`)",
        ).use { cursor ->
            val names = buildList {
                while (cursor.moveToNext()) add(cursor.getString(1))
            }
            assertFalse(names.contains("armCm"))
        }
        upgraded.close()
    }
}
