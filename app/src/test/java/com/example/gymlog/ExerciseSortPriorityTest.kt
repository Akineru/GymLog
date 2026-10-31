package com.example.gymlog

import com.example.gymlog.data.ExerciseEntity
import com.example.gymlog.data.ExercisePriority
import com.example.gymlog.data.ExerciseUsageRow
import com.example.gymlog.data.sortedByTrainingCount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 动作库排序：优先级在最前，其余沿用「练得多 → 最近练 → 名称」。
 *
 * 关键约束：priority 默认 0 时排序结果必须和升级前**完全一致**，
 * 否则老用户升级后会看到动作顺序被打乱。
 */
class ExerciseSortPriorityTest {

    private fun exercise(id: String, priority: Int = 0) = ExerciseEntity(
        id = id,
        name = id,
        bodyPart = "CHEST",
        equipmentType = "BARBELL",
        weightMode = "WEIGHTED",
        isCustom = false,
        aliases = null,
        createdAt = 1L,
        priority = priority,
    )

    private fun usage(vararg pairs: Pair<String, Int>) = pairs.map { (id, count) ->
        ExerciseUsageRow(exerciseId = id, sessionCount = count, lastEpochDay = 0L)
    }

    @Test
    fun higherPrioritySortsFirstRegardlessOfUsage() {
        val list = listOf(exercise("a", 0), exercise("b", 50), exercise("c", 0))
        val sorted = list.sortedByTrainingCount(usage("a" to 30, "c" to 10))
        assertEquals(listOf("b", "a", "c"), sorted.map { it.id })
    }

    @Test
    fun defaultPriorityKeepsTheOriginalOrder() {
        val list = listOf(exercise("a"), exercise("b"), exercise("c"))
        val sorted = list.sortedByTrainingCount(usage("a" to 30, "c" to 10))
        // 训练次数降序；没有记录的按名称
        assertEquals(listOf("a", "c", "b"), sorted.map { it.id })
    }

    @Test
    fun equalPriorityFallsBackToUsageThenName() {
        val list = listOf(exercise("x", 10), exercise("y", 10), exercise("z", 10))
        val sorted = list.sortedByTrainingCount(usage("y" to 5))
        assertEquals(listOf("y", "x", "z"), sorted.map { it.id })
    }

    @Test
    fun priorityIsClampedToAllowedRange() {
        assertEquals(ExercisePriority.MIN, ExercisePriority.normalize(-5))
        assertEquals(ExercisePriority.MAX, ExercisePriority.normalize(999))
        // v21 起 MAX 收窄为 10
        assertEquals(10, ExercisePriority.MAX)
        // 旧范围 0–100 的值现在钳到 0–10（历史数据由迁移缩放，这里锁运行时行为）
        assertEquals(10, ExercisePriority.normalize(42))
        assertEquals(10, ExercisePriority.normalize(10))
        // 空输入视为「未填写」，由 UI 提示而不是悄悄当成 0
        assertNull(ExercisePriority.parse(""))
        assertNull(ExercisePriority.parse("abc"))
        assertEquals(7, ExercisePriority.parse(" 7 "))
    }
}
