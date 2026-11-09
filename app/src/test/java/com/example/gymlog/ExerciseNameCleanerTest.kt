package com.example.gymlog

import com.example.gymlog.data.ExerciseNameCleaner
import com.example.gymlog.data.ExerciseSource
import org.junit.Assert.assertEquals
import org.junit.Test

class ExerciseNameCleanerTest {

    private fun clean(name: String) = ExerciseNameCleaner.clean(name)

    @Test
    fun removesEquipment() {
        assertEquals("卧推", clean("杠铃 卧推"))
        assertEquals("弯举", clean("绳索 弯举"))
        assertEquals("腿举", clean("杠杆机 腿举"))
        assertEquals("耸肩", clean("史密斯 耸肩"))
    }

    @Test
    fun removesLoadMode() {
        assertEquals("提膝", clean("辅助 悬垂 提膝"))
        assertEquals("引体向上", clean("负重 引体向上"))
        assertEquals("弯举", clean("负重 站姿 弯举"))
    }

    @Test
    fun removesPosture() {
        assertEquals("交替锤式弯举", clean("哑铃 交替 坐姿 锤式弯举"))
        assertEquals("过头推举", clean("杠铃 坐姿 过头推举"))
        assertEquals("飞鸟", clean("绳索 仰卧 飞鸟"))
    }

    @Test
    fun removesGenericBodyWordsButKeepsMuscleNames() {
        assertEquals("耸肩", clean("史密斯 背部 耸肩"))
        assertEquals("窄距下拉", clean("弹力带 固定 背部 窄距 下拉"))
        // 肌肉名是动作名的一部分，不能删
        assertEquals("交替肱二头肌弯举", clean("杠铃 交替 肱二头肌弯举"))
        assertEquals("小腿拉伸", clean("辅助 仰卧 小腿拉伸"))
        assertEquals("后三角肌划船", clean("史密斯 后三角肌划船"))
    }

    @Test
    fun keepsVariationWords() {
        assertEquals("上斜窄距卧推", clean("杠铃 上斜 窄距 卧推"))
        assertEquals("单臂划船", clean("哑铃 单臂 划船"))
        assertEquals("侧平举", clean("哑铃 坐姿 侧平举"))
    }

    @Test
    fun collapsesSpacesBetweenChineseWords() {
        assertEquals("交替触踵", clean("交替 触踵"))
        assertEquals("3/4仰卧起坐", clean("3/4 仰卧起坐"))
    }

    @Test
    fun keepsParentheticalDetailButDropsVersionAndGender() {
        assertEquals("肱三头肌伸展(带毛巾)", clean("辅助 站姿 肱三头肌伸展 (带 毛巾)"))
        assertEquals("肱二头肌弯举(带臂托板)", clean("杠铃 肱二头肌弯举 (带 臂托板)"))
        assertEquals("阿诺德推举", clean("哑铃 阿诺德推举 版本 2"))
        assertEquals("交叉锤式弯举", clean("哑铃 交叉 锤式弯举 版本2"))
        assertEquals("手臂过顶完全仰卧起坐", clean("手臂过顶 完全 仰卧起坐 （男）"))
        assertEquals("俯卧撑(墙)", clean("俯卧撑 (墙) 版本 2"))
    }

    @Test
    fun fixesTranslationNoise() {
        assertEquals("摆臂直腿", clean("摆臂 悬垂 直腿s"))
        assertEquals("45°体侧屈", clean("45° 体侧屈"))
        assertEquals("四肢股四头肌拉伸", clean("四肢 s股四头肌 拉伸"))
    }

    @Test
    fun restoresBodyWordWhenResultBecomesAmbiguous() {
        assertEquals("肩内旋", clean("绳索 坐姿 肩 内旋"))
        assertEquals("髋上提", clean("弹力带 髋 上提"))
        assertEquals("背部拉伸", clean("滚轮 背部 拉伸"))
    }

    @Test
    fun keepsNameWhenNothingWouldBeLeft() {
        assertEquals("战绳", clean("战绳"))
        assertEquals("平衡板", clean("平衡板"))
    }

    @Test
    fun nonDatasetExercisesKeepTheirName() {
        assertEquals("弹力带拉开", ExerciseNameCleaner.forExercise("弹力带拉开", ExerciseSource.LEGACY))
        assertEquals("我的动作", ExerciseNameCleaner.forExercise("我的动作", ExerciseSource.CUSTOM))
        assertEquals(
            "卧推",
            ExerciseNameCleaner.forExercise("杠铃 卧推", ExerciseSource.DATASET),
        )
    }
}
