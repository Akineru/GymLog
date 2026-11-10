package com.example.gymlog.data

/**
 * 从中文动作名里剥掉「器械 / 负重方式 / 姿态 / 泛部位词」，得到 [ExerciseEntity.originalName]。
 *
 * 数据集里的中文名是英文原名逐词机翻后用空格拼起来的（`杠铃 坐姿 窄距 卧推`），
 * 器械、负重这些信息本来就在 [ExerciseEntity.equipmentDetail]、[ExerciseEntity.weightMode] 里，
 * 名字里再带一遍只是重复。这里只清洗名字，不动任何其他字段。
 *
 * 口径（与用户确认过）：
 * - 删：器械、负重方式（辅助 / 负重 / 自重 / 固定）、姿态（坐姿 / 站姿 / 仰卧 / 俯卧 / 跪姿 / 悬垂 / 平卧）、
 *   泛部位（背部 / 胸部 / 肩 / 髋 / 腹部）；
 * - 留：上斜 / 下斜 / 窄距 / 宽距 / 反握 / 单臂 / 单腿 / 交替，以及肱二头肌、小腿、背阔肌这类**肌肉名**
 *   （它们是动作名的一部分，删掉只会变成「弯举」「拉伸」）；
 * - 格式：去掉词间空格、括注统一半角、删「版本N」「（男）（女）」、修 `直腿s` / `45в°` 这类翻译噪声。
 *
 * 只对数据集动作生效，预置动作和用户自建动作原样沿用 [ExerciseEntity.name]。
 */
object ExerciseNameCleaner {

    fun forExercise(name: String, source: String?): String =
        if (source == ExerciseSource.DATASET) clean(name) else name

    fun clean(name: String): String {
        OVERRIDES[name]?.let { return it }
        var result = cleanCore(name, keepGenericBody = false)
        // 删到只剩「内旋」「拉伸」「轻拍」这类词时含义会塌掉，把泛部位词补回来
        if (result.length <= 2 && (result in NEED_CONTEXT || result.length <= 1)) {
            val rescued = cleanCore(name, keepGenericBody = true)
            if (rescued.length > result.length) result = rescued
        }
        // 清洗后什么都不剩（`平衡板`、`战绳` 这种名字本身就是器械），原样保留
        return if (result.length < 2) name.replace(WHITESPACE, "") else result
    }

    private fun cleanCore(name: String, keepGenericBody: Boolean): String {
        val (main, groups) = splitGroups(normalize(name))
        val body = cleanTokens(main, keepGenericBody)
        val tail = groups
            .filter { it.isNotBlank() && it !in DROP_GROUPS && !VERSION_PATTERN.matches(it) }
            // 括注里的器械词要留着：`(带 臂托板)` 删成 `(带)` 就没有意义了
            .map { cleanTokens(it, keepGenericBody = true, keepEquipment = true, keepLoad = true) }
            .filter { it.isNotEmpty() }
            .joinToString("") { "($it)" }
        return body + tail
    }

    private fun cleanTokens(
        text: String,
        keepGenericBody: Boolean,
        keepEquipment: Boolean = false,
        keepLoad: Boolean = false,
    ): String {
        val kept = mutableListOf<String>()
        var skipNextNumber = false
        for (raw in normalize(text).split(' ')) {
            var token = TOKEN_FIX[raw] ?: raw
            for (tail in TAIL_STRIP) {
                if (token.length > tail.length && token.endsWith(tail)) {
                    token = token.dropLast(tail.length)
                }
            }
            if (skipNextNumber && token.isNotEmpty() && token.all { it.isDigit() }) {
                skipNextNumber = false
                continue
            }
            skipNextNumber = false
            if (token.isEmpty()) continue
            if (token == "版本") {
                skipNextNumber = true
                continue
            }
            if (token == "-" || token == "/" || token in DROP_TOKENS || VERSION_PATTERN.matches(token)) continue
            if (token in POSTURE) continue
            if (token in LOAD_WORDS && !keepLoad) continue
            if (token in EQUIPMENT && !keepEquipment) continue
            if (token in GENERIC_BODY && !keepGenericBody) continue
            if (TRAILING_LETTER.matches(token) && CJK.containsMatchIn(token) && !LATIN_RUN.containsMatchIn(token)) {
                token = token.dropLast(1)
            }
            if (kept.lastOrNull() == token) continue
            kept += token
        }
        return kept.joinToString("")
    }

    private fun normalize(text: String): String =
        text.replace('（', '(').replace('）', ')').replace(WHITESPACE, " ").trim()

    /** 拆成「主体」和括注内容；只处理一层括号，未闭合的按括注处理。 */
    private fun splitGroups(text: String): Pair<String, List<String>> {
        val main = StringBuilder()
        val groups = mutableListOf<String>()
        val current = StringBuilder()
        var depth = 0
        for (ch in text) {
            when (ch) {
                '(' -> {
                    depth++
                    if (depth > 1) current.append(ch)
                }
                ')' -> {
                    if (depth == 1) {
                        groups += current.toString()
                        current.clear()
                    } else if (depth > 1) {
                        current.append(ch)
                    }
                    if (depth > 0) depth--
                }
                else -> if (depth > 0) current.append(ch) else main.append(ch)
            }
        }
        if (current.isNotEmpty()) groups += current.toString()
        return main.toString() to groups
    }

    private val WHITESPACE = Regex("\\s+")
    private val VERSION_PATTERN = Regex("^版本\\s*\\d*$")
    private val TRAILING_LETTER = Regex(".+[a-z]")
    private val CJK = Regex("[\\u4e00-\\u9fff]")
    private val LATIN_RUN = Regex("[A-Za-z]{2,}")

    private val EQUIPMENT = setOf(
        "哑铃", "杠铃", "绳索", "绳索附件", "杠杆机", "史密斯", "史密斯机", "弹力带", "阻力带",
        "壶铃", "药球", "健身球", "健身球在", "稳定球", "博苏球", "波速球", "雪橇机", "曲杠",
        "杠铃片", "奥林匹克杠铃", "地雷管", "滑轮机", "战绳", "滚轮", "固定单车", "划船机",
        "椭圆机", "跑步机", "台阶机", "交叉训练机", "轮胎", "TRX", "悬吊", "滑轮", "器械",
        "机器", "双杠", "单杠", "凳", "椅子", "凳子", "船长椅", "椅", "臂托板", "毛巾",
        "平衡板", "跳箱", "墙", "哑铃s",
    )

    private val LOAD_WORDS = setOf("辅助", "负重", "自重", "固定", "阻力")

    private val POSTURE = setOf("坐姿", "站姿", "仰卧", "俯卧", "侧卧", "跪姿", "悬垂", "平卧", "伏地")

    private val GENERIC_BODY = setOf("背部", "胸部", "肩", "肩部", "髋", "髋部", "腹部")

    /** 粘在词尾的器材修饰，器械已单独入库。 */
    private val TAIL_STRIP = listOf(
        "在健身球", "在稳定球", "在波速球", "在博苏球", "带健身球", "带稳定球", "带绳索",
    )

    private val DROP_GROUPS = setOf("男", "女", "后视图", "侧面视角", "正面视角")
    private val DROP_TOKENS = setOf("男", "女")

    private val TOKEN_FIX = mapOf(
        "45в°" to "45°",
        "哑铃s" to "哑铃",
        "s股四头肌" to "股四头肌",
        "背部的" to "",
    )

    /** 删到这个词时含义会塌掉，需要把泛部位词补回来。 */
    private val NEED_CONTEXT = setOf(
        "内旋", "外旋", "旋前", "旋后", "上提", "侧屈", "屈", "拉伸", "伸展",
        "抬举", "轻拍", "传", "抛", "抱", "甩", "画圈",
    )

    /** 规则搞不定的个例：源数据翻译残缺、或清洗后只剩器械名。 */
    private val OVERRIDES = mapOf(
        // 「下压」单独拿出来太含糊，用户要求这条动作库里外都用全名
        "绳索 下压 (直臂)" to "绳索 下压 (直臂)",
        "哑铃过凳 中立 手腕弯举" to "过凳中立手腕弯举",
        "哑铃过凳 单臂 中立 手腕弯举" to "过凳单臂中立手腕弯举",
        "哑铃过凳 单臂 反手腕弯举" to "过凳单臂反手腕弯举",
        "哑铃过凳 单臂 手腕弯举" to "过凳单臂手腕弯举",
        "哑铃过凳 反向 手腕弯举" to "过凳反向手腕弯举",
        "哑铃过凳 手腕弯举" to "过凳手腕弯举",
        "辅助 俯卧 腘绳肌" to "腘绳肌拉伸",
        "杠铃 仰卧 背部的 头 肱三头肌 伸展" to "头后肱三头肌伸展",
        "自重蹲姿划船" to "蹲姿划船",
        "自重蹲姿划船（带毛巾）" to "蹲姿划船(带毛巾)",
        "弹力带 双腿 提踵 - (弹力带下方双 腿) 版本 2" to "双腿提踵",
        "弹力带 双腿 提踵 - (弹力带下方双 腿)" to "双腿提踵",
        "辅助 坐姿 胸肌 大肌 拉伸带稳定球" to "胸肌拉伸(稳定球)",
        "哑铃 交替 肱二头肌弯举带抬腿d在健身球" to "交替肱二头肌弯举带抬腿",
        "哑铃 坐姿 交替 肩" to "交替肩推",
        "单臂 靠 墙" to "单臂靠墙",
        "地雷管 180" to "地雷管180",
        "壶铃 8字" to "壶铃8字",
        "哑铃 仰卧 股骨" to "仰卧股骨",
        "侧 髋 (在双杠)" to "侧髋(在双杠)",
        "杠铃 宽 卧推" to "宽距卧推",
    )
}
