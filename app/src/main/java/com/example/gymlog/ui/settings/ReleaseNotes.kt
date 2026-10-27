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
            versionName = "1.5.4",
            date = "2026年10月28日",
            sections = listOf(
                ReleaseSection(
                    title = "外观",
                    items = listOf(
                        "外观改为独立设置页，新增日历配色。",
                        "日历配色支持鲜明、柔和和自定义；自定义可逐部位调色并输入色号。",
                    ),
                ),
                ReleaseSection(
                    title = "设置",
                    items = listOf(
                        "新增通知设置，可关闭定时器到点通知；开启时提示检查通知权限与耗电管理。",
                        "数据备份更名为数据管理，清除本地数据移入其中。",
                    ),
                ),
            ),
        ),
        ReleaseNote(
            versionName = "1.5.3",
            date = "2026年10月27日",
            sections = listOf(
                ReleaseSection(
                    title = "bug修复",
                    items = listOf(
                        "每组的重量、次数可以直接点进去手动输入，重量支持 42.5 这样的小数。",
                        "上下箭头照旧可用，重量步长 2.5 kg、次数步长 1，两种方式改的是同一个值。",
                    ),
                ),
                ReleaseSection(
                    title = "训练通知",
                    items = listOf(
                        "开始训练后通知栏常驻一条「正在训练」，退回桌面也能看到已练时长。",
                        "通知里同步显示组歇与秒表的状态和剩余时间，暂停、归零都会跟着变。",
                        "点通知直接回到同一场训练，动作和已经勾选的组都还在。",
                    ),
                ),
            ),
        ),
        ReleaseNote(
            versionName = "1.5.2",
            date = "2026年10月24日",
            sections = listOf(
                ReleaseSection(
                    title = "训练记录",
                    items = listOf(
                        "日历展开月历每个日期显示当天训练部位，例如「胸+腿」。",
                        "底部训练迷你卡只显示当前动作，全部完成后提示「开始新的动作吧」。",
                        "训练迷你卡在定时器运行时，第一行右侧同步显示倒计时/秒表时间。",
                    ),
                ),
                ReleaseSection(
                    title = "计时器",
                    items = listOf(
                        "倒计时在后台或锁屏到点也能震动提醒，不再错过组间休息。",
                        "取消训练时会同时停止本次训练的倒计时、秒表，并清除到点提醒。",
                    ),
                ),
            ),
        ),
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
