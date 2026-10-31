package com.example.gymlog

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.gymlog.data.MIGRATION_20_21
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * v20 → v21 迁移：加 isFavorite 列 + 优先级收窄到 0–10。
 *
 * 转换规则：1–10 保持；11–100 按十分之一缩放（95→9、80→8、42→4、100→10），
 * 保留相对排序意图；负值钳 0。收藏状态独立记录，全部默认未收藏（不把旧高优先级误判为收藏）。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Migration20To21Test {

    private fun openLegacyDatabase(): SupportSQLiteDatabase {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(null)
            .callback(object : SupportSQLiteOpenHelper.Callback(20) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE `exercises` (
                            `id` TEXT NOT NULL,
                            `name` TEXT NOT NULL,
                            `priority` INTEGER NOT NULL DEFAULT 0,
                            `howTo` TEXT,
                            `cues` TEXT,
                            PRIMARY KEY(`id`)
                        )
                        """.trimIndent(),
                    )
                    // 旧值覆盖：0（默认）、1–10（本来合法）、42/80/95/100（旧测试与用户配置）、-5（异常）
                    val rows = listOf(
                        "a" to 0, "b" to 1, "c" to 5, "d" to 10,
                        "e" to 42, "f" to 80, "g" to 95, "h" to 100, "i" to -5,
                    )
                    for ((id, priority) in rows) {
                        db.execSQL(
                            "INSERT INTO `exercises` (`id`, `name`, `priority`) VALUES (?, ?, ?)",
                            arrayOf(id, "动作$id", priority),
                        )
                    }
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            })
            .build()
        return FrameworkSQLiteOpenHelperFactory().create(config).writableDatabase
    }

    @Test
    fun addsFavoriteColumnAndClampsPriority() {
        val db = openLegacyDatabase()
        MIGRATION_20_21.migrate(db)

        val columns = db.query("PRAGMA table_info(`exercises`)").use { cursor ->
            buildSet {
                while (cursor.moveToNext()) add(cursor.getString(1))
            }
        }
        assertTrue("缺少 isFavorite 列", "isFavorite" in columns)

        val expected = mapOf(
            "a" to 0L, "b" to 1L, "c" to 5L, "d" to 10L,
            "e" to 4L, "f" to 8L, "g" to 9L, "h" to 10L, "i" to 0L,
        )
        db.query("SELECT id, priority, isFavorite FROM `exercises`").use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getString(0)
                assertEquals("动作 $id 的优先级转换", expected[id], cursor.getLong(1))
                assertEquals("动作 $id 默认不收藏", 0L, cursor.getLong(2))
            }
        }
    }

    @Test
    fun allRowsSurviveAndStayInRange() {
        val db = openLegacyDatabase()
        MIGRATION_20_21.migrate(db)
        db.query("SELECT COUNT(*), MIN(priority), MAX(priority), SUM(isFavorite) FROM `exercises`").use { cursor ->
            cursor.moveToNext()
            assertEquals(9L, cursor.getLong(0))
            assertTrue(cursor.getLong(1) >= 0)
            assertTrue(cursor.getLong(2) <= 10)
            assertEquals("没有动作被自动标成收藏", 0L, cursor.getLong(3))
        }
    }
}
