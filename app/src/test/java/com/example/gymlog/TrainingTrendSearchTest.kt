package com.example.gymlog

import com.example.gymlog.data.ExerciseEntity
import com.example.gymlog.ui.stats.matches
import com.example.gymlog.ui.stats.trendCandidates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 训练足迹选择面板的搜索匹配：原名 / 旧名 / 英文 / 别名都要能命中。 */
class TrainingTrendSearchTest {

    private fun exercise(
        name: String,
        originalName: String = "",
        nameEn: String? = null,
        aliases: String? = null,
    ) = ExerciseEntity(
        id = "id-$name",
        name = name,
        bodyPart = "chest",
        equipmentType = "barbell",
        weightMode = "WEIGHTED",
        isCustom = false,
        aliases = aliases,
        createdAt = 0,
        nameEn = nameEn,
        originalName = originalName,
    )

    @Test
    fun matchesCleanedOriginalName() {
        val entity = exercise(name = "杠铃 上斜 窄距 卧推", originalName = "上斜窄距卧推")
        assertTrue(entity.matches("窄距"))
        assertTrue(entity.matches("卧推"))
        assertFalse(entity.matches("深蹲"))
    }

    @Test
    fun matchesRawChineseName() {
        val entity = exercise(name = "绳索 站姿 提踵", originalName = "提踵")
        assertTrue(entity.matches("站姿"))
    }

    @Test
    fun matchesEnglishNameIgnoringCase() {
        val entity = exercise(name = "卧推", originalName = "卧推", nameEn = "Barbell Bench Press")
        assertTrue(entity.matches("bench"))
        assertTrue(entity.matches("BENCH"))
    }

    @Test
    fun matchesAlias() {
        val entity = exercise(name = "卧推", originalName = "卧推", aliases = "平板卧推,胸推")
        assertTrue(entity.matches("胸推"))
    }

    @Test
    fun emptyQueryMatchesEverything() {
        assertTrue(exercise(name = "深蹲").matches("   "))
    }

    // ---- 候选列表：默认只给练过的，搜索才带出没练过的 ----

    private val bench = exercise(name = "杠铃 卧推", originalName = "卧推")
    private val squat = exercise(name = "杠铃 深蹲", originalName = "深蹲")
    private val fly = exercise(name = "哑铃 飞鸟", originalName = "飞鸟")
    private val all = listOf(bench, squat, fly)

    /** 只练过卧推和飞鸟；飞鸟比卧推更近。 */
    private val trained = mapOf(bench.id to 100L, fly.id to 200L)

    @Test
    fun defaultListHasOnlyTrainedExercisesByRecency() {
        val result = trendCandidates(all, trained, "")
        assertEquals(listOf(fly.id, bench.id), result.map { it.id })
    }

    @Test
    fun searchCanReachUntrainedExercise() {
        val result = trendCandidates(all, trained, "深蹲")
        assertEquals(listOf(squat.id), result.map { it.id })
    }

    @Test
    fun searchListsTrainedBeforeUntrained() {
        val a = exercise(name = "动作甲", originalName = "甲动作")
        val b = exercise(name = "动作乙", originalName = "乙动作")
        val c = exercise(name = "动作丙", originalName = "丙动作")
        val result = trendCandidates(listOf(a, b, c), mapOf(a.id to 1L, b.id to 2L), "动作")
        assertEquals(3, result.size)
        assertEquals(setOf(a.id, b.id), result.take(2).map { it.id }.toSet())
        assertEquals(c.id, result.last().id)
    }

    @Test
    fun defaultListIsLimited() {
        val result = trendCandidates(all, trained, "", limit = 1)
        assertEquals(listOf(fly.id), result.map { it.id })
    }
}
