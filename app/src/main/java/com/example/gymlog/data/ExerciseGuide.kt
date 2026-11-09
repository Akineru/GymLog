package com.example.gymlog.data

import org.json.JSONArray
import org.json.JSONException

/**
 * 动作要点：「怎么做」+「细节」。
 *
 * 内容**不存数据库**——它是由动作名、部位、器械、负重方式和数据集里已有的中文步骤
 * 推导出来的通用知识，不是用户的个人数据。详情页每次打开时现算：
 *
 * - 数据库与备份不会因此变大 1MB 级；
 * - 改规则后全库立即生效，不会出现新旧文案混在一起；
 * - 用户手动编辑过的 `howTo` / `cues` 永远优先，规则不覆盖。
 *
 * 生成不了内容时（自建动作且没有任何可参考信息）返回空，界面继续显示原来的空态。
 */
data class ExerciseGuide(
    val howTo: String,
    val cues: String,
) {
    val isBlank: Boolean get() = howTo.isEmpty() && cues.isEmpty()
}

object ExerciseGuideBuilder {

    private const val MAX_STEPS = 6

    /**
     * 数据集的步骤里常有这几句纯占位的话，删掉后剩下的才是动作本身。
     *
     * 「重复所需」是所有变体（重复次数 / 持续时间 / 时间和次数）的共同前缀，用短前缀一次盖住。
     */
    private val DROPPED_STEP_HINTS = listOf(
        "重复所需",
        "根据您的健身水平",
    )

    /** 数据集是机翻，几个器械名与器械商城的常用叫法对不上，这里统一。 */
    private val TERM_REPLACEMENTS = listOf(
        "电缆机" to "龙门架",
        "滑轮机" to "滑轮",
        "长凳" to "平凳",
    )

    /** 解析 `exercise_content.instructionStepsJson`（字符串数组）。 */
    fun parseSteps(json: String?): List<String> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(json)
            (0 until array.length()).mapNotNull { index ->
                array.optString(index).trim().takeIf { it.isNotEmpty() }
            }
        } catch (e: JSONException) {
            emptyList()
        }
    }

    fun build(exercise: ExerciseEntity, steps: List<String>): ExerciseGuide {
        val howTo = buildHowTo(steps)
        val cues = buildCues(exercise)
        return ExerciseGuide(howTo = howTo, cues = cues)
    }

    /** 步骤：清洗 → 去重 → 截断 → 编号。没有步骤时返回空串，交给界面显示空态。 */
    fun buildHowTo(steps: List<String>): String {
        val cleaned = LinkedHashSet<String>()
        for (raw in steps) {
            var step = raw.trim()
            if (step.isEmpty()) continue
            if (DROPPED_STEP_HINTS.any { it in step }) continue
            for ((from, to) in TERM_REPLACEMENTS) {
                step = step.replace(from, to)
            }
            if (step.isEmpty()) continue
            cleaned += step
            if (cleaned.size >= MAX_STEPS) break
        }
        if (cleaned.isEmpty()) return ""
        return cleaned.mapIndexed { index, step -> "${index + 1}. $step" }.joinToString("\n")
    }

    /**
     * 细节：四段结构。
     *
     * 识别出动作模式时用该模式的文案；识别不出时按部位给通用版，宁可短也不编造。
     */
    fun buildCues(exercise: ExerciseEntity): String {
        val movement = ExerciseGuideRules.movementOf(exercise.name)
        val fallback = ExerciseGuideRules.bodyFallback(exercise.bodyCategoryId)
        if (movement == null && fallback == null) return ""

        val drive = movement?.drive ?: fallback!!.first
        val posture = movement?.posture ?: fallback!!.second
        val errors = movement?.errors ?: "动作幅度缩水；用惯性代偿；重量超出控制能力。"
        val tempo = movement?.tempo ?: defaultTempo(exercise.weightMode)

        val postureText = listOfNotNull(
            posture,
            ExerciseGuideRules.equipmentNote(exercise.equipmentDetail),
        ).joinToString("")

        val errorText = listOfNotNull(
            errors,
            ExerciseGuideRules.equipmentError(exercise.equipmentDetail),
        ).joinToString("")

        return buildString {
            append("发力：").append(drive)
            append('\n')
            append("姿态与幅度：").append(postureText)
            append('\n')
            append("节奏与呼吸：").append(tempo)
            append('\n')
            append("常见错误：").append(errorText)
        }
    }

    private fun defaultTempo(weightMode: String): String {
        return when (weightMode) {
            WeightMode.BODYWEIGHT.name -> "全程控制速度，不靠惯性加速；发力时呼气，回程吸气。"
            else -> "向心 1–2 秒、离心 2–3 秒；发力时呼气，回程吸气。"
        }
    }
}
