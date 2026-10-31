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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 别名编辑 + 别名搜索。
 *
 * 动作库和「添加训练」用的是同一个 [ExerciseRepository.observeExercises]，
 * 所以这里锁定的是**两个入口共用的那一层**：别名保存后能立刻搜到、改掉后旧别名失效、
 * 通配符按字面处理。只要这一层成立，两个页面就不会再出现「一个搜得到一个搜不到」。
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExerciseAliasSearchTest {
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

    private fun exercise(
        id: String,
        name: String,
        nameEn: String? = null,
        aliases: String? = null,
    ) = ExerciseEntity(
        id = id,
        name = name,
        bodyPart = "BACK",
        equipmentType = "FREE_WEIGHT",
        weightMode = WeightMode.WEIGHTED.name,
        isCustom = false,
        aliases = aliases,
        createdAt = 1L,
        nameEn = nameEn,
        source = ExerciseSource.DATASET,
        sourceId = id,
        bodyCategoryId = "back",
        equipmentCategoryId = "free_weight",
    )

    private suspend fun visibleIds(query: String): List<String> =
        repository.observeExercises(query, null, null).first().map { it.id }

    // ---- 测试 A：编辑并保存别名 ----

    @Test
    fun savedAliasesBecomeSearchableImmediately() = runTest {
        db.exerciseDao().insertIgnore(listOf(exercise("ds_row", "杠铃划船")))
        // 保存前：搜别名什么都搜不到
        assertTrue(visibleIds("俯身").isEmpty())

        repository.updateTrainingFields(
            exerciseId = "ds_row",
            aliases = "杠铃划船， 俯身杠铃划船 , 杠铃划船;  Barbell Row ",
            howTo = null,
            cues = null,
        )

        // 保存后：中文别名、英文别名都能立刻命中，且不需要重启
        assertTrue(visibleIds("俯身").contains("ds_row"))
        assertTrue(visibleIds("Barbell Row").contains("ds_row"))
        assertTrue(visibleIds("barbell row").contains("ds_row"))

        val stored = db.exerciseDao().getById("ds_row")!!.aliases
        assertEquals("杠铃划船,俯身杠铃划船,Barbell Row", stored)
    }

    @Test
    fun editingAliasesReplacesTheOldOnes() = runTest {
        db.exerciseDao().insertIgnore(listOf(exercise("ds_row", "杠铃划船", aliases = "俯身划船")))
        assertTrue(visibleIds("俯身划船").contains("ds_row"))

        repository.updateTrainingFields("ds_row", "杠铃划船变式", null, null)

        // 旧别名不再命中，新别名命中
        assertTrue(visibleIds("俯身划船").isEmpty())
        assertTrue(visibleIds("杠铃划船变式").contains("ds_row"))
    }

    @Test
    fun clearingAliasesKeepsNameSearchWorking() = runTest {
        db.exerciseDao().insertIgnore(listOf(exercise("ds_row", "杠铃划船", aliases = "俯身划船")))

        repository.updateTrainingFields("ds_row", "   ", null, null)

        val cleared = db.exerciseDao().getById("ds_row")!!
        assertEquals(null, cleared.aliases)
        // 空查询仍然列全；按正式名称仍然搜得到
        assertTrue(visibleIds("").contains("ds_row"))
        assertTrue(visibleIds("杠铃划船").contains("ds_row"))
        assertTrue(visibleIds("俯身划船").isEmpty())
    }

    // ---- 测试 B：别名规范化（存储层） ----

    @Test
    fun savingNormalizesSeparatorsSpacesAndDuplicates() = runTest {
        db.exerciseDao().insertIgnore(listOf(exercise("ds_row", "杠铃划船")))
        repository.updateTrainingFields(
            "ds_row",
            "  杠铃划船 ， 俯身杠铃划船、杠铃划船； Barbell Row ,, ",
            null,
            null,
        )
        assertEquals(
            "杠铃划船,俯身杠铃划船,Barbell Row",
            db.exerciseDao().getById("ds_row")!!.aliases,
        )
    }

    // ---- 测试 C/D：搜索匹配 ----

    @Test
    fun searchMatchesNameEnglishNameAndAlias() = runTest {
        db.exerciseDao().insertIgnore(
            listOf(
                exercise("ds_row", "杠铃划船", nameEn = "Barbell Row", aliases = "俯身划船"),
                exercise("ds_squat", "杠铃深蹲", nameEn = "Back Squat", aliases = "背蹲"),
            ),
        )
        assertEquals(listOf("ds_row"), visibleIds("杠铃划船"))
        assertEquals(listOf("ds_row"), visibleIds("Barbell"))
        assertEquals(listOf("ds_row"), visibleIds("俯身划船"))
        assertEquals(listOf("ds_squat"), visibleIds("背蹲"))
    }

    @Test
    fun asciiSearchIsCaseInsensitive() = runTest {
        db.exerciseDao().insertIgnore(
            listOf(exercise("ds_row", "杠铃划船", nameEn = "Barbell Row")),
        )
        assertEquals(listOf("ds_row"), visibleIds("barbell row"))
        assertEquals(listOf("ds_row"), visibleIds("BARBELL ROW"))
    }

    @Test
    fun emptyQueryReturnsEverything() = runTest {
        db.exerciseDao().insertIgnore(
            listOf(
                exercise("a", "动作一"),
                exercise("b", "动作二", aliases = "别名二"),
            ),
        )
        assertEquals(listOf("a", "b"), visibleIds("").sorted())
    }

    // ---- 测试 E：通配符必须按字面处理 ----

    @Test
    fun wildcardsInQueryMatchLiterally() = runTest {
        db.exerciseDao().insertIgnore(
            listOf(
                exercise("ds_pct", "卧推 100% 强度"),
                exercise("ds_plain", "杠铃深蹲"),
            ),
        )
        // 未转义时 "%" 会变成 LIKE '%%%'，把两条都捞出来；转义后只匹配名称里真有 % 的那条
        assertEquals(listOf("ds_pct"), visibleIds("%"))
        assertTrue(visibleIds("_").isEmpty())
    }

    @Test
    fun escapeLikePatternEscapesBackslashFirst() {
        assertEquals("a\\%b", ExerciseRepository.escapeLikePattern("a%b"))
        assertEquals("a\\_b", ExerciseRepository.escapeLikePattern("a_b"))
        assertEquals("a\\\\b", ExerciseRepository.escapeLikePattern("a\\b"))
        assertEquals("杠铃划船", ExerciseRepository.escapeLikePattern("杠铃划船"))
    }

    // ---- 不会因为搜别名而新增动作 ----

    @Test
    fun searchingByAliasDoesNotCreateDuplicateExercise() = runTest {
        db.exerciseDao().insertIgnore(listOf(exercise("ds_row", "杠铃划船", aliases = "俯身划船")))
        val before = db.exerciseDao().count()

        repeat(3) { visibleIds("俯身划船") }

        assertEquals(before, db.exerciseDao().count())
        assertEquals(listOf("ds_row"), visibleIds("俯身划船"))
    }
}
