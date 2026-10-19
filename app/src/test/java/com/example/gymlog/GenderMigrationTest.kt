package com.example.gymlog

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.gymlog.data.MIGRATION_8_9
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GenderMigrationTest {
    @Test
    fun upgradeKeepsBirthdayAndAddsGender() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "migrate-gender.db"
        val file = context.getDatabasePath(name)
        file.parentFile?.mkdirs()
        if (file.exists()) file.delete()
        val raw = SQLiteDatabase.openOrCreateDatabase(file, null)
        raw.execSQL(
            """
            CREATE TABLE body_profile (
                id INTEGER NOT NULL PRIMARY KEY,
                birthdayEpochDay INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        raw.execSQL("INSERT INTO body_profile (id, birthdayEpochDay) VALUES (1, 10000)")
        raw.version = 8
        raw.close()

        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(name)
                .callback(object : SupportSQLiteOpenHelper.Callback(9) {
                    override fun onCreate(db: SupportSQLiteDatabase) = Unit

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                        assertEquals(8, oldVersion)
                        MIGRATION_8_9.migrate(db)
                    }
                })
                .build(),
        )
        val db = helper.writableDatabase
        val profile = db.query("SELECT birthdayEpochDay, gender FROM body_profile")
        assertTrue(profile.moveToFirst())
        assertEquals(10_000L, profile.getLong(0))
        assertTrue(profile.isNull(1))
        profile.close()
        db.close()
    }
}
