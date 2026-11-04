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
            versionName = "1.6.1",
            date = "2026年11月4日",
            sections = listOf(
                ReleaseSection(
                    title = "肌群训练热力图",
                    items = listOf(
                        "统计页新增肌群训练热力图，可在最近一周和最近一月之间切换。",
                        "练过的肌肉显示红色，练得越勤颜色越深；点一块肌肉可查看课次、组数和动作。",
                        "右上角问号说明颜色深浅的含义。",
                    ),
                ),
            ),
        ),
        ReleaseNote(
            versionName = "1.6.0",
            date = "2026年11月2日",
            sections = listOf(
                ReleaseSection(
                    title = "肌群图全新素材",
                    items = listOf(
                        "肌群图彻底重制：男/女双版本，共 19 块肌群，遮罩贴合身体轮廓。",
                        "肌群图新增淡色轮廓展示，无训练数据时也能看清肌肉分块。",
                    ),
                ),
                ReleaseSection(
                    title = "训练肌群标注",
                    items = listOf(
                        "身体记录页可显示当天练过肌肉的引线标注，点击肌肉可查看名称。",
                    ),
                ),
                ReleaseSection(
                    title = "日历与细节",
                    items = listOf(
                        "日历月份支持左右滑动切换。",
                        "日历展开页显示每日训练容量。",
                        "界面文案精简，收藏星标位置优化。",
                    ),
                ),
                ReleaseSection(
                    title = "数据安全",
                    items = listOf(
                        "新增自动本地备份：冷启动自动保留最近 3 份快照，数据管理页可一键恢复。",
                    ),
                ),
            ),
        ),
        ReleaseNote(
            versionName = "1.5.6",
            date = "2026年11月1日",
            sections = listOf(
                ReleaseSection(
                    title = "动作详情页改版",
                    items = listOf(
                        "总览顶部改为通宽动作横幅，动作名与分类一目了然。",
                        "新增按周统计：累计组数、累计容量、最近训练，一眼看到你和这个动作的关系。",
                        "肌群图与肌群信息左右并排，要点页为空时可以一键去补充。",
                    ),
                ),
                ReleaseSection(
                    title = "我的页统计图表",
                    items = listOf(
                        "新增按周柱状图，支持最近1个月、3个月、1年和全期切换，并记住上次选择。",
                        "时长、训练量、训练天数三个指标左右滑动切换，时长支持小时/分钟，训练量支持 KG/T。",
                        "点击柱子显示该周具体数值，训练量超过四位数自动显示为 15k 这样的格式。",
                    ),
                ),
                ReleaseSection(
                    title = "日历",
                    items = listOf(
                        "月份网格支持左右滑动切换，切换带滑动动画。",
                        "「今天」按钮和点击其他月日期会自动滑到对应月份。",
                    ),
                ),
                ReleaseSection(
                    title = "界面优化",
                    items = listOf(
                        "各主要页面的解释性小字统一收拢，入口只保留图标和标题，更清爽。",
                        "动作库筛选摘要不再显示重复的「全部」，只显示选中的分类和数量。",
                        "我的页新增数据快捷入口：身体数据、照片墙、数据管理、关于。",
                        "设置页同组选项合并展示，「关于」独立分组。",
                        "首页与我的页的训练天数口径更清晰：本周训练 / 累计训练。",
                        "收藏星标更容易点中，点亮时有轻弹动效。",
                    ),
                ),
            ),
        ),
        ReleaseNote(
            versionName = "1.5.5",
            date = "2026年10月31日",
            sections = listOf(
                ReleaseSection(
                    title = "动作详情页",
                    items = listOf(
                        "点击动作卡片进入详情页，总览、历史记录、要点三个页面左右滑动切换。",
                        "总览里有动作 GIF、主要与次要肌群、可切换正反面的肌群图和 PR 记录。",
                        "历史记录按日期列出每次训练的容量和每组明细；要点页可查看动作怎么做和细节。",
                        "右上角菜单可以编辑别名与要点，自定义动作还能编辑肌群映射。",
                    ),
                ),
                ReleaseSection(
                    title = "收藏",
                    items = listOf(
                        "动作卡片右上角有星标，点一下收藏、再点取消，收藏的固定排在最前面。",
                        "收藏状态会保存，也包含在备份里。",
                    ),
                ),
                ReleaseSection(
                    title = "别名搜索",
                    items = listOf(
                        "动作支持多个别名，动作库和添加训练都能用别名搜到，不用记正式名称。",
                        "编辑资料时可以实时看到将保存的别名。",
                    ),
                ),
                ReleaseSection(
                    title = "交互优化",
                    items = listOf(
                        "双击左侧部位分类，或点一下器械筛选（自重、杠铃、哑铃等），列表自动回到顶部。",
                        "PR 的日期改为 2026.10.09 这样的格式并移到行尾。",
                    ),
                ),
            ),
        ),
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
