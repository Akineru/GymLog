package com.example.gymlog.data

/**
 * 动作别名的读写约定。
 *
 * 存储沿用 `exercises.aliases` 这个已有的 TEXT 字段，多个别名用**逗号分隔**：
 * `"杠铃卧推,平板卧推，bench press"`。
 *
 * 为什么不做独立关联表：
 * - 现有搜索（[ExerciseDao.observeFiltered]）就是用 `aliases LIKE '%q%'` 匹配的，
 *   逗号分隔天然支持子串搜索；
 * - 现有备份（[BackupCodec]）已经带着这个字段，改结构会让旧备份需要额外兼容；
 * - 别名数量少（通常 1~4 个），没有独立查询需求。
 *
 * 写入统一走 [join]，读取统一走 [parse]，避免各处自己 split 造成分隔符不一致。
 */
object ExerciseAliases {
    /** 中英文逗号、顿号、分号都当作分隔符：用户手输时不会在意用哪个。 */
    private val separators = charArrayOf(',', '，', '、', ';', '；')

    private const val MAX_ALIAS_COUNT = 20
    private const val MAX_ALIAS_LENGTH = 40

    /** 解析成去重后的别名列表；空白、重复项会被丢掉，顺序按输入顺序保留。 */
    fun parse(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        val seen = HashSet<String>()
        val result = ArrayList<String>()
        raw.split(*separators).forEach { part ->
            val trimmed = part.trim().take(MAX_ALIAS_LENGTH)
            if (trimmed.isEmpty()) return@forEach
            // 去重按忽略大小写，避免「Bench / bench」出现两次
            if (!seen.add(trimmed.lowercase())) return@forEach
            if (result.size >= MAX_ALIAS_COUNT) return@forEach
            result += trimmed
        }
        return result
    }

    /** 反向写回存储；没有有效别名时返回 null，保持「无别名」与「空字符串」同一种表示。 */
    fun join(aliases: List<String>): String? {
        val normalized = parse(aliases.joinToString(","))
        return normalized.takeIf { it.isNotEmpty() }?.joinToString(",")
    }

    /** 把用户输入的一串文本规范化成存储格式。 */
    fun normalize(raw: String?): String? = join(parse(raw))

    /** 展示用：解析后重新用中文顿号连接，更适合阅读。 */
    fun display(raw: String?): String = parse(raw).joinToString("、")
}
