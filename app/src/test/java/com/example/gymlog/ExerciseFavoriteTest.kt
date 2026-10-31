package com.example.gymlog

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.gymlog.data.ExerciseEntity
import com.example.gymlog.data.ExerciseRepository
import com.example.gymlog.data.ExerciseSource
import com.example.gymlog.data.GymLogDatabase
import com.example.gymlog.data.WeightMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 收藏 ↔ 优先级联动 + 列表排序。
 *
 * 收藏与优先级在 DAO 的同一条 UPDATE 中落库（收藏=10 / 取消=0），
 * 动作库和「添加训练」共用 [ExerciseRepository.observeExercises]，这里锁定这一层的规则。
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExerciseFavoriteTest {
    private lateinit var db: GymLogDatabase
    private lateinit var repository: ExerciseRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, GymLogDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = ExerciseRepository(db.exerciseDao(), db.exerciseMuscleDao())
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    private suspend fun exercise(id: String, name: String) {
        db.exerciseDao().insertIgnore(
            listOf(
            ExerciseEntity(
                id = id,
                name = name,
                bodyPart = "BACK",
                equipmentType = "FREE_WEIGHT",
                weightMode = WeightMode.WEIGHTED.name,
                isCustom = false,
                aliases = null,
                createdAt = 1L,
                source = ExerciseSource.DATASET,
                sourceId = id,
                bodyCategoryId = "back",
                equipmentCategoryId = "free_weight",
            ),
            ),
        )
    }

    @Test
    fun `favorite sets priority 10, unfavorite resets to 0`() = runTest {
        exercise("a", "动作甲")
        repository.setFavorite("a", true)
        var row = db.exerciseDao().observeAll().first().single { it.id == "a" }
        assertTrue(row.isFavorite)
        assertEquals(10, row.priority)

        repository.setFavorite("a", false)
        row = db.exerciseDao().observeAll().first().single { it.id == "a" }
        assertFalse(row.isFavorite)
        assertEquals(0, row.priority)
    }

    @Test
    fun `favorites sort before non favorites and order stays stable`() = runTest {
        exercise("a", "动作甲")
        exercise("b", "动作乙")
        exercise("c", "动作丙")
        repository.setFavorite("c", true)

        // 名称排序按 Unicode：丙(4E19) < 乙(4E59) < 甲(7532)，收藏的 c 固定第一
        val order = repository.observeExercises("", null, null).first().map { it.id }
        assertEquals(listOf("c", "b", "a"), order)
    }

    @Test
    fun `toggling favorite updates list ordering immediately`() = runTest {
        exercise("a", "动作甲")
        exercise("b", "动作乙")
        // 未收藏时按名称序：乙(4E59) < 甲(7532)
        assertEquals(listOf("b", "a"), repository.observeExercises("", null, null).first().map { it.id })

        repository.setFavorite("a", true)
        assertEquals(listOf("a", "b"), repository.observeExercises("", null, null).first().map { it.id })

        repository.setFavorite("a", false)
        assertEquals(listOf("b", "a"), repository.observeExercises("", null, null).first().map { it.id })
    }

    @Test
    fun `rapid toggles converge to consistent state`() = runTest {
        exercise("a", "动作甲")
        repeat(5) { repository.setFavorite("a", true) }
        repeat(3) { repository.setFavorite("a", false) }
        val row = db.exerciseDao().observeAll().first().single { it.id == "a" }
        assertFalse(row.isFavorite)
        assertEquals(0, row.priority)
    }

    @Test
    fun `search results keep priority ordering`() = runTest {
        exercise("a", "划船甲")
        exercise("b", "划船乙")
        repository.setFavorite("b", true)
        val hits = repository.observeExercises("划船", null, null).first().map { it.id }
        assertEquals(listOf("b", "a"), hits)
    }
}
