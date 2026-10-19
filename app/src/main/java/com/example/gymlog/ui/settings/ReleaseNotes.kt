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
            versionName = "1.4",
            date = "2026年10月18日",
            sections = listOf(
                ReleaseSection(
                    title = "动作库升级",
                    items = listOf(
                        "动作库切换为 1,324 个数据集动作，使用 10 种身体部位和 28 种器械分类。",
                        "动作库新增动作缩略图；动作详情新增「动作演示 / 正面 / 反面」三个选项，动作演示会循环播放 GIF。",
                        "训练记录改为三栏布局：左侧动作信息、中间缩略图、右侧每组的重量 × 次数。",
                        "旧预置动作不再出现在活动动作库，也不会被自动补种；历史训练、PR 和旧动作仍可正常解析。",
                        "搜索支持动作名称、英文名和别名，并保留身体部位与器械筛选。",
                    ),
                ),
                ReleaseSection(
                    title = "体验优化",
                    items = listOf(
                        "无图或 GIF 损坏时使用占位图或「暂无动作演示」，不会卡住或崩溃。",
                        "动作库图片在后台解码并缓存，训练记录的组数据保持单行显示。",
                        "旧自定义动作保留 ID、名称、PR、归档和肌群关联。",
                    ),
                ),
                ReleaseSection(
                    title = "版权说明",
                    items = listOf(
                        "动作缩略图和 GIF：© Gym visual — https://gymvisual.com/，按授权在 180×180 分辨率下使用。",
                    ),
                ),
            ),
        ),
        ReleaseNote(
            versionName = "1.3.1",
            date = "2026年10月17日",
            sections = listOf(
                ReleaseSection(
                    title = "新增功能",
                    items = listOf(
                        "底部增加「首页」。可以按周或按月查看训练时长、训练天数、总组数和容量，并按部位看本周训练足迹。",
                        "首页可以直接开始训练、记录身体、查看趋势或打开日历。",
                        "选择头像时可以拖动并双指缩放后裁剪。头像会写入数据备份。",
                    ),
                ),
                ReleaseSection(
                    title = "体验优化",
                    items = listOf(
                        "「我的」里的统计可以切换：训练天数和训练年限、累计时长的单位、体脂率和体重。训练天数按从第一次训练到今天的跨度计算。",
                        "训练卡片最多显示 9 格。超过 9 组时，前 8 组照常显示，第 9 格显示省略号。",
                        "人体图素材会缓存，反复打开时更顺畅。",
                    ),
                ),
            ),
        ),
        ReleaseNote(
            versionName = "1.3",
            date = "2026年10月17日",
            sections = listOf(
                ReleaseSection(
                    title = "新增功能",
                    items = listOf(
                        "「我的」可以设置用户名和头像，并显示训练天数、累计训练时长和最新体脂率。",
                        "身体记录可以在人体图上填写围度：胸围、腰围、臀围，以及左右臂围、小臂、腿围和小腿。同一张图可以切换查看训练肌群。",
                        "用户名和围度会写入数据备份。",
                    ),
                ),
                ReleaseSection(
                    title = "体验优化",
                    items = listOf(
                        "「趋势」中的入口改名为「训练足迹」和「身体数据」。训练足迹可以按身体部位查看。",
                        "趋势图可以点选某一天，查看当天的数据。",
                        "动作库左侧按部位选择，上方按器械筛选。",
                        "卡片和页面配色按同一套样式调整。",
                    ),
                ),
                ReleaseSection(
                    title = "问题修复",
                    items = listOf(
                        "计时器会根据开机次数判断手机是否重启，避免把关机期间的时间算进计时。",
                    ),
                ),
            ),
        ),
        ReleaseNote(
            versionName = "1.2.1",
            date = "2026年10月16日",
            sections = listOf(
                ReleaseSection(
                    title = "新增功能",
                    items = listOf(
                        "日历页可以打开训练计时器。支持开始、暂停、继续、计次，也可以直接改时间。",
                        "计时可以单独保存，或关联到当天训练。当天会显示训练总时长，点开可以查看已保存的计次。删除计时不会改动训练内容。",
                        "手机重新启动后，正在进行的计时会暂停，核对时间后可以继续。",
                    ),
                ),
                ReleaseSection(
                    title = "体验优化",
                    items = listOf(
                        "「训练」页改为入口：动作库和训练记录可以直接进入。训练计划和训练工具仍显示为开发中。",
                        "动作库可以按部位和器械浏览。输入名称搜索时，会在全部动作里查找。",
                        "点日历页左上角的应用名可以直接打开关于。",
                    ),
                ),
            ),
        ),
        ReleaseNote(
            versionName = "1.2",
            date = "2026年10月15日",
            sections = listOf(
                ReleaseSection(
                    title = "新增功能",
                    items = listOf(
                        "底部增加「日历」。打开应用先看到日历和当天的训练，日历可以展开成整月。",
                        "「训练」页改为动作库。动作可以设置主要肌群和次要肌群，内置动作已带好对应关系。",
                        "身体记录中可以打开训练肌群图，按当天、本周或本月查看正面和背面。点一块肌肉能看到相关动作。红色只表示这次训练涉及这块肌肉。",
                        "个人资料可以填写性别：男、女或其他。",
                        "训练趋势增加容量曲线。",
                    ),
                ),
                ReleaseSection(
                    title = "体验优化",
                    items = listOf(
                        "「进步」改名为「趋势」。",
                        "同一天的记录按部位容量排列，容量高的动作排在前面，日期旁会显示各部位容量。",
                        "导出和导入改到设置里。备份会带上性别和动作肌群。",
                    ),
                ),
            ),
        ),
        ReleaseNote(
            versionName = "1.1",
            date = "2026年10月13日",
            sections = listOf(
                ReleaseSection(
                    title = "新增功能",
                    items = listOf(
                        "底部分为「训练」「进步」「我的」。训练趋势和身体趋势放在进步里，动作库、身体记录和数据备份放在我的里。",
                        "设置里可以切换外观：跟随系统、浅色或深色。Android 12 及以上可以选择是否使用壁纸配色，选择会保留到下次打开。",
                        "没有训练的日期可以点开，写下一句自己的话，并按这一天保存。",
                        "设置里可以分两步确认后清除本地数据。清空后内置动作会恢复，自定义动作不会保留。",
                        "关于页可以查看各版本的更新记录。",
                    ),
                ),
                ReleaseSection(
                    title = "体验优化",
                    items = listOf(
                        "中文名称改为「练有所迹」，英文名称改为 Traivol。",
                        "更换为黑底白标的 Traivol 启动器图标。",
                    ),
                ),
            ),
        ),
    )
}
