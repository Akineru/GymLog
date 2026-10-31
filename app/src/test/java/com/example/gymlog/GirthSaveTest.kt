package com.example.gymlog

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.gymlog.data.BodyGirths
import com.example.gymlog.data.BodyMeasurementEntity
import com.example.gymlog.data.BodyRecordRepository
import com.example.gymlog.data.GymLogDatabase
import com.example.gymlog.data.MIGRATION_11_12
import com.example.gymlog.data.MIGRATION_12_13
import com.example.gymlog.data.MIGRATION_13_14
import com.example.gymlog.data.MIGRATION_14_15
import com.example.gymlog.data.MIGRATION_15_16
import com.example.gymlog.data.MIGRATION_16_17
import com.example.gymlog.data.MIGRATION_17_18
import com.example.gymlog.data.MIGRATION_18_19
import com.example.gymlog.data.MIGRATION_19_20
import com.example.gymlog.data.MIGRATION_20_21
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GirthSaveTest {
    @Test
    fun savingGirthsKeepsWeightAndOtherMetrics() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, GymLogDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        db.bodyRecordDao().insertMeasurement(
            BodyMeasurementEntity(
                id = "old",
                recordedEpochDay = 20_000,
                heightCm = 177.0,
                weightKg = 74.0,
                bodyFatPercent = 17.2,
                waistCm = 80.0,
                createdAt = 1,
            ),
        )
        val repository = BodyRecordRepository(db.bodyRecordDao())
        repository.saveGirths(
            BodyGirths(armLeftCm = 32.5, armRightCm = 33.0, chestCm = 98.0, waistCm = 80.0),
        )
        val latest = repository.observeMeasurements().first().first()
        assertEquals(74.0, latest.weightKg!!, 0.001)
        assertEquals(17.2, latest.bodyFatPercent!!, 0.001)
        assertEquals(177.0, latest.heightCm!!, 0.001)
        assertEquals(32.5, latest.armLeftCm!!, 0.001)
        assertEquals(33.0, latest.armRightCm!!, 0.001)
        assertEquals(98.0, latest.chestCm!!, 0.001)
        assertNull(latest.hipCm)
        assertEquals("old", db.bodyRecordDao().listMeasurements().first { it.id == "old" }.id)
        db.close()
    }

    @Test
    fun blankGirthSaveDoesNotInsertARow() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, GymLogDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        BodyRecordRepository(db.bodyRecordDao()).saveGirths(BodyGirths())
        assertEquals(0, db.bodyRecordDao().listMeasurements().size)
        db.close()
    }

    @Test
    fun upgradeFromVersion11LeavesNewGirthsEmpty() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "migrate-girth.db"
        context.deleteDatabase(name)
        val created = Room.databaseBuilder(context, GymLogDatabase::class.java, name)
            .addMigrations(MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20, MIGRATION_20_21)
            .build()
        created.bodyRecordDao().insertMeasurement(
            BodyMeasurementEntity(
                id = "kept",
                recordedEpochDay = 20_737,
                heightCm = 177.0,
                weightKg = 75.0,
                bodyFatPercent = 17.0,
                waistCm = 82.0,
                createdAt = 5,
            ),
        )
        val sqlite = created.openHelper.writableDatabase
        rewindMeasurementsToVersion11(sqlite)
        sqlite.execSQL("PRAGMA user_version = 11")
        created.close()

        val upgraded = Room.databaseBuilder(context, GymLogDatabase::class.java, name)
            .addMigrations(MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20, MIGRATION_20_21)
            .build()
        val row = upgraded.bodyRecordDao().listMeasurements().single()
        assertEquals(75.0, row.weightKg!!, 0.001)
        assertEquals(17.0, row.bodyFatPercent!!, 0.001)
        assertEquals(82.0, row.waistCm!!, 0.001)
        assertNull(row.armLeftCm)
        assertNull(row.armRightCm)
        assertNull(row.chestCm)
        assertNull(row.hipCm)
        assertNull(row.thighLeftCm)
        assertNull(row.calfRightCm)
        upgraded.close()
    }
}
