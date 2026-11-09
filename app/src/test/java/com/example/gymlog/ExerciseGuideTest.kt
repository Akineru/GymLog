package com.example.gymlog

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.gymlog.data.ExerciseClassification
import com.example.gymlog.data.ExerciseEntity
import com.example.gymlog.data.ExerciseGuideBuilder
import com.example.gymlog.data.ExerciseGuideRules
import com.example.gymlog.data.ExerciseSource
import com.example.gymlog.data.WeightMode
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 「动作怎么做 / 动作细节」的生成规则。
 *
 * 这两块内容不进数据库，全靠规则推导，所以规则本身必须被测试盖住：
 * 步骤清洗错了会显示数据集里的机翻废话，动作模式认错了会给出别的动作的要领。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExerciseGuideTest {

    private fun exercise(
        name: String,
        bodyCategoryId: String? = null,
        equipmentDetail: String? = null,
        weightMode: String = WeightMode.WEIGHTED.name,
    ) = ExerciseEntity(
        id = "test",
        name = name,
        bodyPart = "CHEST",
        equipmentType = "FREE_WEIGHT",
        weightMode = weightMode,
        isCustom = false,
        aliases = null,
        createdAt = 0,
        equipmentDetail = equipmentDetail,
        bodyCategoryId = bodyCategoryId,
        source = ExerciseSource.DATASET,
    )

    // ---- 步骤清洗 ----

    @Test
    fun howToDropsFillerStepsAndNumbersThem() {
        val howTo = ExerciseGuideBuilder.buildHowTo(
            listOf(
                "平躺在长凳上，双脚平放在地上。",
                "将杠铃慢慢降低到胸部。",
                "重复所需的重复次数。",
            ),
        )
        assertEquals(
            "1. 平躺在平凳上，双脚平放在地上。\n2. 将杠铃慢慢降低到胸部。",
            howTo,
        )
    }

    @Test
    fun howToUnifiesMachineVocabulary() {
        val howTo = ExerciseGuideBuilder.buildHowTo(listOf("将电缆机调整至较低位置并安装手柄。"))
        assertEquals("1. 将龙门架调整至较低位置并安装手柄。", howTo)
    }

    @Test
    fun howToKeepsAtMostSixStepsAndRemovesDuplicates() {
        val steps = (1..9).map { "第 $it 步。" } + "第 1 步。"
        val howTo = ExerciseGuideBuilder.buildHowTo(steps)
        assertEquals(6, howTo.lines().size)
        assertTrue(howTo.startsWith("1. 第 1 步。"))
        assertTrue(howTo.endsWith("6. 第 6 步。"))
    }

    @Test
    fun howToIsEmptyWithoutSteps() {
        assertEquals("", ExerciseGuideBuilder.buildHowTo(emptyList()))
    }

    @Test
    fun parseStepsIgnoresBrokenJson() {
        assertEquals(emptyList<String>(), ExerciseGuideBuilder.parseSteps(null))
        assertEquals(emptyList<String>(), ExerciseGuideBuilder.parseSteps("not json"))
        assertEquals(
            listOf("第一步", "第二步"),
            ExerciseGuideBuilder.parseSteps("""["第一步","","第二步"]"""),
        )
    }

    // ---- 动作细节 ----

    @Test
    fun benchPressGetsFourStructuredSections() {
        val cues = ExerciseGuideBuilder.buildCues(
            exercise("杠铃 卧推", ExerciseClassification.CHEST.id, "barbell"),
        )
        assertTrue(cues.contains("发力：胸大肌主导"))
        assertTrue(cues.contains("姿态与幅度：仰卧平凳"))
        assertTrue(cues.contains("节奏与呼吸：向心 1–2 秒"))
        assertTrue(cues.contains("常见错误：肘部外展接近 90°"))
        // 自由重量追加的器械提示接在姿态段里
        assertTrue(cues.contains("重量以能控制全程为准"))
    }

    @Test
    fun cableFlyMentionsPulleySetup() {
        val cues = ExerciseGuideBuilder.buildCues(
            exercise("绳索 上斜 飞鸟", ExerciseClassification.CHEST.id, "cable"),
        )
        assertTrue(cues.contains("肘角固定不变"))
        assertTrue(cues.contains("滑轮高度"))
    }

    @Test
    fun bodyweightExerciseUsesBodyweightTempo() {
        val cues = ExerciseGuideBuilder.buildCues(
            exercise("引体向上", ExerciseClassification.BACK.id, "body weight", WeightMode.BODYWEIGHT.name),
        )
        assertTrue(cues.contains("节奏与呼吸：全程控制速度"))
    }

    @Test
    fun unknownPatternFallsBackToBodyPart() {
        val cues = ExerciseGuideBuilder.buildCues(
            exercise("咒术师", ExerciseClassification.UPPER_LEGS.id, "body weight"),
        )
        assertTrue(cues.contains("臀腿主导"))
        assertTrue(cues.contains("常见错误："))
    }

    @Test
    fun nothingKnownProducesBlankCues() {
        val cues = ExerciseGuideBuilder.buildCues(exercise("我的动作"))
        assertTrue(cues.isEmpty())
    }

    /**
     * 分类覆盖率兜底：数据集 1283 个动作里，识别不出动作模式的不能超过 10%。
     *
     * 这条断言不是为了「凑数字」，而是防止以后改关键词时把大批动作挤到兜底分支，
     * 让它们只剩「保持呼吸、别甩」这种没用的通用话。
     */
    @Test
    fun mostDatasetExercisesMatchAMovementPattern() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val json = context.assets.open(ExerciseSource.DATASET_ASSET).bufferedReader().use { it.readText() }
        val records = JSONObject(json).getJSONArray("records")
        var total = 0
        var matched = 0
        repeat(records.length()) { index ->
            val name = records.getJSONObject(index).optString("name")
            if (name.isNotBlank()) {
                total++
                if (ExerciseGuideRules.movementOf(name) != null) matched++
            }
        }
        assertTrue("数据集为空，检查 assets 是否被打包", total > 1000)
        val ratio = matched.toDouble() / total
        assertTrue("动作模式识别率过低：$matched/$total", ratio >= 0.9)
    }

    @Test
    fun everyMovementPatternHasAllFourParts() {
        ExerciseGuideRules.movements.forEach { movement ->
            assertFalse("${movement.key} 的发力文案为空", movement.drive.isBlank())
            assertFalse("${movement.key} 的姿态文案为空", movement.posture.isBlank())
            assertFalse("${movement.key} 的错误文案为空", movement.errors.isBlank())
        }
    }
}
