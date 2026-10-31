package com.example.gymlog

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.gymlog.data.MIGRATION_19_20
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * v19 → v20 迁移：只加列，不动数据。
 *
 * 这是本次最容易伤到用户数据的一步，所以单独锁住三件事：
 * 1. 三个新列确实加上了；
 * 2. 旧动作一条不少；
 * 3. `priority` 的默认值是 0（默认排序不变），两个要点列为 NULL（未维护）。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Migration19To20Test {

    private fun openLegacyDatabase(): SupportSQLiteDatabase {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(null)
            .callback(object : SupportSQLiteOpenHelper.Callback(19) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    // 只建迁移需要的最小列，避免依赖历史迁移链的全部细节
                    db.execSQL(
                        """
                        CREATE TABLE `exercises` (
                            `id` TEXT NOT NULL,
                            `name` TEXT NOT NULL,
                            `aliases` TEXT,
                            PRIMARY KEY(`id`)
                        )
                        """.trimIndent(),
                    )
                    db.execSQL("INSERT INTO `exercises` (`id`, `name`) VALUES ('a', '杠铃卧推')")
                    db.execSQL("INSERT INTO `exercises` (`id`, `name`) VALUES ('b', '深蹲')")
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            })
            .build()
        return FrameworkSQLiteOpenHelperFactory().create(config).writableDatabase
    }

    @Test
    fun addsNewColumnsWithSafeDefaults() {
        val db = openLegacyDatabase()
        MIGRATION_19_20.migrate(db)

        val columns = db.query("PRAGMA table_info(`exercises`)").use { cursor ->
            buildSet {
                while (cursor.moveToNext()) add(cursor.getString(1))
            }
        }
        assertTrue("缺少 priority 列", "priority" in columns)
        assertTrue("缺少 howTo 列", "howTo" in columns)
        assertTrue("缺少 cues 列", "cues" in columns)

        // 旧数据仍在，且默认值与「未维护」语义一致
        val ids = mutableListOf<String>()
        val names = mutableListOf<String>()
        db.query("SELECT id, name, priority, howTo, cues FROM `exercises` ORDER BY id").use { cursor ->
            while (cursor.moveToNext()) {
                ids += cursor.getString(0)
                names += cursor.getString(1)
                assertEquals(0, cursor.getInt(2))
                assertTrue(cursor.isNull(3))
                assertTrue(cursor.isNull(4))
            }
        }
        assertEquals(listOf("a", "b"), ids)
        assertEquals(listOf("杠铃卧推", "深蹲"), names)
    }

    @Test
    fun migrationIsIdempotentWhenColumnsAlreadyExist() {
        val db = openLegacyDatabase()
        MIGRATION_19_20.migrate(db)
        // 再跑一次不应抛异常（PRAGMA 守卫），也不会清空数据
        MIGRATION_19_20.migrate(db)
        db.query("SELECT COUNT(*) FROM `exercises`").use { cursor ->
            cursor.moveToFirst()
            assertEquals(2, cursor.getInt(0))
        }
    }
}
