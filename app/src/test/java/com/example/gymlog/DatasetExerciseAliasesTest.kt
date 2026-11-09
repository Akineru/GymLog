package com.example.gymlog

import com.example.gymlog.data.DatasetExerciseAliases
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DatasetExerciseAliasesTest {

    private fun aliases(name: String, equipment: String) =
        DatasetExerciseAliases.aliasesFor(name, equipment)

    @Test
    fun sledMachineLegPressIsCalledDaoDeng() {
        val aliases = aliases("雪橇机 45° 腿举", "sled machine")
        assertTrue("倒蹬", aliases.contains("倒蹬"))
        assertTrue("倒蹬机", aliases.contains("倒蹬机"))
        assertTrue("腿举机", aliases.contains("腿举机"))
    }

    @Test
    fun cableMovementsGetLongMenJia() {
        val aliases = aliases("绳索 上斜 飞鸟", "cable")
        assertTrue("龙门架", aliases.contains("龙门架"))
        assertTrue("夹胸", aliases.contains("夹胸"))
        assertTrue("龙门架夹胸", aliases.contains("龙门架夹胸"))
    }

    // 蝴蝶机只属于杠杆机的夹胸用法，不能挂到整类杠杆机上
    @Test
    fun butterflyMachineOnlyOnLeverageFly() {
        val fly = aliases("杠杆机 坐姿 飞鸟", "leverage machine")
        assertTrue(fly.contains("蝴蝶机"))
        assertTrue(fly.contains("夹胸机"))

        val legPress = aliases("杠杆机 水平 一 腿举", "leverage machine")
        assertTrue(legPress.contains("倒蹬机"))
        assertTrue(!legPress.contains("蝴蝶机"))
    }

    @Test
    fun stepperIsAlsoClimbingMachine() {
        val aliases = aliases("步行在踏步机", "stepmill machine")
        assertTrue(aliases.contains("登山机"))
    }

    @Test
    fun commonBodyweightNamesGetColloquialAliases() {
        assertTrue(aliases("空中蹬车", "body weight").contains("自行车卷腹"))
        assertTrue(aliases("登山者", "body weight").contains("登山跑"))
        assertTrue(aliases("杠铃 弓步", "barbell").contains("箭步蹲"))
        assertTrue(aliases("波比跳", "body weight").contains("立卧撑"))
    }

    @Test
    fun aliasesNeverRepeatWordsAlreadyInTheName() {
        val aliases = aliases("绳索 卧推", "cable")
        assertTrue(aliases.isNotEmpty())
        assertTrue(aliases.none { it.contains("绳索") || it == "卧推" })
    }

    // 拉伸、绕环这类动作没有通用叫法，不硬凑别名
    @Test
    fun stretchesAndMobilityGetNoAliases() {
        assertEquals(emptyList<String>(), aliases("45° 体侧屈", "body weight"))
        assertEquals(emptyList<String>(), aliases("脚踝 绕环", "body weight"))
    }

    @Test
    fun aliasCountIsCapped() {
        val aliases = aliases("杠杆机 坐姿 反向飞鸟", "leverage machine")
        assertTrue(aliases.size <= 6)
    }
}
