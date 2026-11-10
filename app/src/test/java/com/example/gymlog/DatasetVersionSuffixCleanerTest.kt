package com.example.gymlog

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.gymlog.data.DatasetVersionSuffixCleaner
import com.example.gymlog.data.ExerciseEntity
import com.example.gymlog.data.ExerciseSource
import com.example.gymlog.data.GymLogDatabase
import com.example.gymlog.data.WeightMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 老库里抓取残留的「版本 2」要能就地清掉：不能要求用户重装。
 * 只删后缀，名字里的器械、括号、空格一个都不许动。
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DatasetVersionSuffixCleanerTest {
    private lateinit var db: GymLogDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, GymLogDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun row(id: String, name: String, originalName: String, source: String) = ExerciseEntity(
        id = id,
        name = name,
        bodyPart = "上肢",
        equipmentType = "CABLE",
        weightMode = WeightMode.WEIGHTED.name,
        isCustom = source != ExerciseSource.DATASET,
        aliases = null,
        createdAt = 0L,
        originalName = originalName,
        source = source,
        sourceId = id,
    )

    @Test
    fun stripsSuffixAndKeepsEverythingElse() = runTest {
        val dao = db.exerciseDao()
        dao.insertIgnore(
            listOf(
                row("ds_1", "绳索 下压 (直臂) 版本 2", "下压(直臂)", ExerciseSource.DATASET),
                row("ds_2", "哑铃 交替 肱二头肌弯举 版本 2", "交替肱二头肌弯举", ExerciseSource.DATASET),
            ),
        )

        DatasetVersionSuffixCleaner(db).cleanAll()

        // 器械词和括号保留，只少掉「版本 2」
        assertEquals("绳索 下压 (直臂)", dao.getById("ds_1")!!.name)
        // 动作库和详情页用同一个名字，两处一致
        assertEquals("绳索 下压 (直臂)", dao.getById("ds_1")!!.originalName)
        assertEquals("哑铃 交替 肱二头肌弯举", dao.getById("ds_2")!!.name)
    }

    @Test
    fun leavesCleanRowsAndCustomRowsUntouched() = runTest {
        val dao = db.exerciseDao()
        dao.insertIgnore(
            listOf(
                row("ds_3", "绳索 坐姿划船", "坐姿划船", ExerciseSource.DATASET),
                row("custom_1", "我的 版本 2 练法", "我的 版本 2 练法", ExerciseSource.CUSTOM),
            ),
        )

        DatasetVersionSuffixCleaner(db).cleanAll()

        assertEquals("绳索 坐姿划船", dao.getById("ds_3")!!.name)
        assertEquals("坐姿划船", dao.getById("ds_3")!!.originalName)
        assertEquals("我的 版本 2 练法", dao.getById("custom_1")!!.name)
    }

    @Test
    fun repeatRunIsNoOp() = runTest {
        val dao = db.exerciseDao()
        dao.insertIgnore(
            listOf(row("ds_4", "侧桥 版本 2", "侧桥", ExerciseSource.DATASET)),
        )
        val cleaner = DatasetVersionSuffixCleaner(db)

        cleaner.cleanAll()
        val after = dao.getById("ds_4")!!
        cleaner.cleanAll()

        assertEquals(after, dao.getById("ds_4")!!)
        assertEquals("侧桥", after.name)
    }
}
