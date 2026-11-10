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
            versionName = "1.6.4",
            date = "2026年11月10日",
            sections = listOf(
                ReleaseSection(
                    title = "训练页",
                    items = listOf(
                        "肌群训练热力图从统计页搬到训练页，就在「今天练什么？」下面。",
                        "热力图新增「恢复」：黄色是还在恢复期，绿色是已恢复，灰色是 14 天以上没练。",
                    ),
                ),
                ReleaseSection(
                    title = "界面与修复",
                    items = listOf(
                        "首页「训练容量」数字太长时，右边的 KG 会被裁掉，已修好。",
                        "颜色说明弹窗改成色点加分行说明，不再是一段密密麻麻的文字。",
                        "新增自定义动作：器械和肌群改成一行点开选择，一屏就能填完。",
                        "清掉动作名里的「版本 2」这类后缀。",
                    ),
                ),
            ),
        ),
        ReleaseNote(
            versionName = "1.6.3",
            date = "2026年11月10日",
            sections = listOf(
                ReleaseSection(
                    title = "动作库瘦身与更名",
                    items = listOf(
                        "清掉 41 个重复动作（同一动作的侧面视角版、版本 2 / 版本 3、女版示范），动作库从 1324 条减到 1283 条。",
                        "用到的动作若被合并，训练记录会自动改挂到保留下来的那一条，历史记录不丢。",
                        "动作名不再带器械前缀：列表里显示「45°腿举」，不再是「雪橇机 45° 腿举」；搜索仍按原名匹配。",
                        "顺手修了 3 条名字里的乱码。",
                    ),
                ),
                ReleaseSection(
                    title = "搜得到本地叫法",
                    items = listOf(
                        "给 633 个动作补了别名，共 1025 条：倒蹬、龙门架、蝴蝶机、夹胸、高位下拉、登山机、史密夫机……",
                        "搜索框输「倒蹬」也能找到腿举，「龙门架」能列出所有绳索动作。",
                        "动作详情里的别名改成一排小胶囊，不再挤成一行字。",
                    ),
                ),
                ReleaseSection(
                    title = "动作要点：怎么做 + 细节",
                    items = listOf(
                        "每个动作新增「动作怎么做」编号步骤，术语按本地叫法统一（电缆机→龙门架、长凳→平凳）。",
                        "新增「动作细节」四段：发力方式、姿态与幅度、节奏与呼吸、常见错误。",
                        "绳索、器械、弹力带、自由重量各自追加一句器械特有的提示。",
                        "点右上角「编辑资料」可以改写成自己的版本；留空就继续显示自动整理的。",
                    ),
                ),
                ReleaseSection(
                    title = "动作详情页改版",
                    items = listOf(
                        "演示动图改成浅色名片，名字和部位 / 器械标签排在图片下方，不再压黑遮罩。",
                        "个人最好成绩改成两栏并排，成绩放大、日期弱化。",
                        "练到的肌群收进同一张卡：左边人形图，右边主要 / 次要肌群。",
                        "编辑弹窗重做：别名变成一张张卡片，点右上角 × 删除，点虚线 ＋ 现填一个，回车可以连着加好几个。",
                    ),
                ),
                ReleaseSection(
                    title = "训练与记录",
                    items = listOf(
                        "历史 / 日历页的动作名一列加宽到 5 个汉字以上，长的换行，不再被挤成两个字。",
                        "「锻炼中」悬浮条真正浮起来了，内容从卡片背后穿过、不再被硬截一条；点右侧箭头可收起成右下角小胶囊。",
                    ),
                ),
                ReleaseSection(
                    title = "修复与其他",
                    items = listOf(
                        "修复休息倒计时归零后不重置就直接改时长会叠加的问题（设 2 分钟跑完再 +30 秒 ×4，原来会变成 4 分钟）。",
                        "设置页「偏好设置」改名「通用」，「训练偏好」改名「偏好」。",
                        "删掉若干解释性的小字，界面更干净。",
                    ),
                ),
            ),
        ),
        ReleaseNote(
            versionName = "1.6.2",
            date = "2026年11月8日",
            sections = listOf(
                ReleaseSection(
                    title = "首页全新改版",
                    items = listOf(
                        "本周训练时长做成一眼可见的大数字，左右切换周和月。",
                        "训练足迹改成一行七天：选中的日子蓝色实心圆，今天蓝色细圈，练过的日子下面一个小蓝点。",
                        "当天练过会多出一张训练汇总卡，时长、组数、容量一眼看到。",
                        "没训练时给的是鼓励而不是提醒，两个入口照旧：记录身体、查看统计。",
                    ),
                ),
                ReleaseSection(
                    title = "材质与配色焕新",
                    items = listOf(
                        "卡片换成哑光陶瓷质感：柔和阴影、细描边，文字对比度比之前的磨砂玻璃更清楚。",
                        "页面背景改成淡蓝紫渐变，白色卡片和背景的层次更分明。",
                        "周月切换、日期箭头、底部导航用磨砂玻璃点缀，不再整块发虚。",
                        "品牌色统一为蓝，全应用一套色板。",
                    ),
                ),
                ReleaseSection(
                    title = "底部导航与训练细节",
                    items = listOf(
                        "底部导航重做：图标配文字，选中项浅蓝底高亮，页面内容不再被导航栏压住。",
                        "动作卡底部新增向上箭头，可把当前动作收起成摘要，点一下再展开。",
                        "全部页面材质统一，切换页面不再有明显的风格断层。",
                    ),
                ),
            ),
        ),
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
