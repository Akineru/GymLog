package com.example.gymlog.ui.settings

data class ReleaseSection(
    val title: String,
    val items: List<String>,
)

data class ReleaseNote(
    val versionName: String,
    val date: String,
    val sections: List<ReleaseSection>,
)

/**
 * Newest version first. Append a new [ReleaseNote] at the top when shipping the next version.
 */
object ReleaseHistory {
    val notes: List<ReleaseNote> = listOf(
        ReleaseNote(
            versionName = "1.5.1",
            date = "2026年10月23日",
            sections = listOf(
                ReleaseSection(
                    title = "界面焕新",
                    items = listOf(
                        "首页、训练、日历、趋势、我的、设置、身体、计时和照片统一为 A 蓝视觉。",
                        "日历改为本月训练摘要 + 部位色圆圈，月份标题居中并支持左右切换。",
                        "保存页重排训练摘要、照片、备注和日期；全局切换细黑体 Light。",
                    ),
                ),
                ReleaseSection(
                    title = "交互优化",
                    items = listOf(
                        "周/月切换支持滑动动画；完成动作后收起卡整卡变绿。",
                        "人体测量标签移到人体外侧；历史记录移除计时入口，计时器统一放在训练工具。",
                    ),
                ),
            ),
        ),
        ReleaseNote(
            versionName = "1.5",
            date = "2026年10月21日",
            sections = listOf(
                ReleaseSection(
                    title = "训练与记录",
                    items = listOf(
                        "新增训练计时器、秒表和动作完成计时。",
                        "支持训练照片、备注和日期编辑；数据库升级到 Room 19。",
                    ),
                ),
            ),
        ),
    )
}
