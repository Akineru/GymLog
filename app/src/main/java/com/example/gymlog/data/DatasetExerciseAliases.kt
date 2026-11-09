package com.example.gymlog.data

/**
 * 数据集动作的「本地叫法」别名，用于补上搜不到的常用词
 * （`倒蹬`、`龙门架`、`蝴蝶机`、`夹胸`、`登山机` 这些数据集里一个都没有）。
 *
 * 三条规则，按顺序拼：
 * 1. **动作族**：名字里出现某个关键词就加同义叫法（`飞鸟` → `夹胸`、`腿举` → `倒蹬`）；
 * 2. **器械**：只收「本地叫法和数据集用词不一样」的器械（`cable` → `龙门架`），
 *    所以 `dumbbell` / `barbell` 这种同名的不写，避免全是噪声；
 * 3. **器械 × 动作**：同一个器械的不同用法叫法不同（`杠杆机 坐姿 飞鸟` 才是 `蝴蝶机`，
 *    `雪橇机 腿举` 才是 `倒蹬机`），所以只能组合着加。
 *
 * 只做「追加」：名字里已经有的词不重复加；每条最多 [MAX_ALIASES] 个。
 * 生成结果只喂给 [ExerciseEntity.aliases]（搜索已经在用这个字段），不碰 name / originalName。
 */
object DatasetExerciseAliases {
    private const val MAX_ALIASES = 6

    fun aliasesFor(name: String, equipmentDetail: String?): List<String> {
        val compact = name.replace(" ", "")
        val candidates = ArrayList<String>()

        MOVEMENTS.forEach { rule ->
            if (compact.contains(rule.keyword) && rule.exclude.none { compact.contains(it) }) {
                candidates += rule.aliases
            }
        }
        EQUIPMENT[equipmentDetail?.lowercase()]?.let(candidates::addAll)
        COMBOS.forEach { combo ->
            if (combo.equipment == equipmentDetail?.lowercase() && compact.contains(combo.keyword)) {
                candidates += combo.aliases
            }
        }

        val seen = HashSet<String>()
        return candidates.asSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !compact.contains(it) }
            .filter { seen.add(it) }
            .take(MAX_ALIASES)
            .toList()
    }

    private class MovementRule(
        val keyword: String,
        val aliases: List<String>,
        /** 名字里出现这些词时规则不成立（上斜卧推不是平板卧推、罗马尼亚硬拉不是传统硬拉）。 */
        val exclude: List<String> = emptyList(),
    )

    private class EquipmentCombo(
        val equipment: String,
        val keyword: String,
        val aliases: List<String>,
    )

    private val MOVEMENTS = listOf(
        MovementRule("飞鸟", listOf("夹胸")),
        MovementRule("腿举", listOf("倒蹬")),
        MovementRule("下拉", listOf("高位下拉")),
        MovementRule("引体", listOf("引体向上")),
        MovementRule("臀推", listOf("臀冲")),
        MovementRule("臀冲", listOf("臀推")),
        MovementRule("挺身", listOf("山羊挺身")),
        MovementRule("平板", listOf("平板支撑")),
        MovementRule("弓步", listOf("箭步蹲", "箭步")),
        MovementRule("波比", listOf("立卧撑")),
        MovementRule("空中蹬车", listOf("自行车卷腹")),
        MovementRule("登山者", listOf("登山跑")),
        // 三大项和常见上肢动作的本地叫法
        MovementRule("卧推", listOf("平板卧推"), exclude = listOf("上斜", "下斜", "平板", "地面")),
        MovementRule("硬拉", listOf("传统硬拉"), exclude = listOf("罗马尼亚", "直腿", "相扑", "单腿", "六角")),
        MovementRule("深蹲", listOf("后蹲"), exclude = listOf("前蹲", "哈克", "相扑", "跳", "单腿", "保加利亚")),
        MovementRule("弯举", listOf("二头弯举"), exclude = listOf("肱二头肌", "手腕", "反握")),
        MovementRule("下压", listOf("三头下压"), exclude = listOf("肱三头肌")),
        MovementRule("侧平举", listOf("侧举")),
        MovementRule("推举", listOf("肩推"), exclude = listOf("胸部", "卧推", "腿")),
        MovementRule("提踵", listOf("小腿提踵"), exclude = listOf("小腿")),
    )

    /** 只写「本地叫法 ≠ 数据集用词」的器械；哑铃/杠铃/壶铃这些同名的不写。 */
    private val EQUIPMENT = mapOf(
        "cable" to listOf("龙门架", "钢索"),
        "band" to listOf("阻力带"),
        "resistance band" to listOf("弹力带"),
        "smith machine" to listOf("史密夫机"),
        "stability ball" to listOf("瑞士球", "健身球"),
        "bosu ball" to listOf("波苏球"),
        "ez barbell" to listOf("EZ杆", "曲杆"),
        "trap bar" to listOf("六角杠"),
        "stepmill machine" to listOf("登山机", "爬楼机"),
        "stationary bike" to listOf("动感单车"),
        "elliptical machine" to listOf("交叉训练机"),
        "upper body ergometer" to listOf("手摇车"),
        "skierg machine" to listOf("滑雪机"),
    )

    private val COMBOS = listOf(
        // 杠杆机只有夹胸用法才叫蝴蝶机
        EquipmentCombo("leverage machine", "飞鸟", listOf("蝴蝶机", "夹胸机")),
        EquipmentCombo("leverage machine", "腿举", listOf("倒蹬机", "腿举机")),
        EquipmentCombo("leverage machine", "胸部推举", listOf("推胸机", "坐姿推胸")),
        // 雪橇机的腿举就是倒蹬；哈克深蹲另算
        EquipmentCombo("sled machine", "腿举", listOf("倒蹬机", "腿举机")),
        EquipmentCombo("sled machine", "哈克", listOf("哈克机")),
        EquipmentCombo("cable", "飞鸟", listOf("龙门架夹胸")),
        EquipmentCombo("cable", "下拉", listOf("龙门架下拉")),
    )
}
