package com.example.gymlog

import com.example.gymlog.data.ExerciseAliases
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 别名的读写约定。
 *
 * 存储是一个逗号分隔的字符串（沿用 `exercises.aliases`），所以解析必须能容错：
 * 中文逗号、顿号、分号、多余空格、重复项都不应该产生「脏别名」。
 */
class ExerciseAliasesTest {

    @Test
    fun parseSupportsMixedSeparators() {
        assertEquals(
            listOf("杠铃卧推", "平板卧推", "bench press"),
            ExerciseAliases.parse("杠铃卧推, 平板卧推，bench press"),
        )
        assertEquals(
            listOf("硬拉", "deadlift"),
            ExerciseAliases.parse("硬拉、deadlift；"),
        )
    }

    @Test
    fun parseDropsBlankAndDuplicateEntries() {
        assertEquals(listOf("卧推"), ExerciseAliases.parse(" 卧推 ,, ，  ,卧推"))
        // 去重忽略大小写：Bench 与 bench 视为同一个
        assertEquals(listOf("Bench"), ExerciseAliases.parse("Bench,bench, BENCH"))
    }

    @Test
    fun emptyAliasesNormalizeToNull() {
        assertNull(ExerciseAliases.normalize(null))
        assertNull(ExerciseAliases.normalize(""))
        assertNull(ExerciseAliases.normalize("   ,  ，、 "))
        // 没有别名时不显示任何内容，也不会拼出空字符串
        assertEquals("", ExerciseAliases.display(null))
    }

    @Test
    fun joinRoundTripsThroughParse() {
        val raw = "坐姿划船, seated row"
        val stored = ExerciseAliases.normalize(raw)
        assertEquals("坐姿划船,seated row", stored)
        assertEquals(listOf("坐姿划船", "seated row"), ExerciseAliases.parse(stored))
    }

    @Test
    fun displayUsesChineseDelimiter() {
        assertEquals("卧推、平板卧推", ExerciseAliases.display("卧推,平板卧推"))
    }
}
