# GymLog / 练有所迹 — 接手说明



本地离线的 Android 力量训练记录应用。单 Activity，Kotlin + Jetpack Compose + Room。没有账号、没有网络同步。界面中文；启动器英文名是 Traivol（`values-en/strings.xml` 的 `app_name`），中文名是「练有所迹」。



当前版本：`versionName` **1.6.2**，`versionCode` **22**。包名 `com.example.gymlog`。数据库文件 `gymlog.db`，Room version **22**（1.6.2 发版时是 21；之后新增 `exercises.originalName` 升到 22，**尚未发版**，versionCode 未动）。



> 状态：1.6.2 已发布。文末「1.6.2 发版」是最新口径；更早的功能章节保留为历史记录。硬约束仍然有效。



下一个实现者先读这份文档，再改代码。用户用简体中文沟通。版本说明写在 `ui/settings/ReleaseNotes.kt` 的 `ReleaseHistory.notes`，新版本插在列表最上面。关于页读安装包的 `versionName`。



## 硬约束



- 升级数据库只用 Room `Migration`。禁止 `fallbackToDestructiveMigration`，禁止为了改表而卸载或清空用户数据。

- 用户主动点的「清除本地数据」是唯一允许清空训练库相关表的路径，而且必须保留现在的两步确认。清除**不会**删除照片墙文件与 `photo_wall` 行（见「照片墙」）。

- 不要引入 Navigation Compose。导航是 `GymLogApp.kt` 里的内存状态 `Screen`。

- 不要改备份格式的已有字段含义。可以加可选字段，旧文件必须还能导入。当前 `BackupCodec.VERSION` 是 **2**。version 1 没有计时和头像键，仍然要能导入。`dayKinds` 是可选数组，旧文件没有该键当成空列表。照片墙**不进**备份 JSON。

- 数据集动作缩略图/GIF 版权归 Gym visual，按 180×180 授权随 APK 打包；备用示意图是 Bryl Lim / Everkinetic，CC BY-SA 4.0。不要重新生成，不要删掉动作库和关于页的署名字符串 `exercise_art_credit`。人体底图和肌肉遮罩是另一套素材，同样不要重画、不要改 `muscleId` 去凑文件名。

- 「正在训练通知」只是计时状态的只读消费方：**不得在通知/服务里另起计时**，否则会出现第二套时间基准；清理只允许发生在 `activeDraft` 被清空时（保存成功 / 舍弃 / 删除），保存失败不得清理。

- 页面里的业务文案（训练记录、容量、PR 等）不要随启动器改名一起改掉。

- 明确没做、先不要做的：记组复制、组间休息计时、估算 1RM、热身组。训练页的「训练计时器」是独立训练工具，入口在动作库下方；不要再把它当占位页删除。训练计划仍不要以占位页形式加回。



## 技术栈与编译



- Kotlin 2.2.10，AGP 9.4.1，Compose BOM 2026.02.01，Room 2.8.5 + KSP，minSdk 24，target/compile 37。开了 core library desugaring。

- JDK：`gradle.properties` 里 `org.gradle.java.home` 指向本机 Android Studio 的 JBR（OpenJDK 25）。Kotlin 在 JDK 25 上会回退到 JVM target 24，这是警告，不是失败。

- `gradle/gradle-daemon-jvm.properties` 的 toolchain 25 会尝试从网络下载 Temurin，这台机器上经常失败。不要为此改 Gradle 配置，除非用户要求。Android Studio 里直接编译即可。

- 命令行若 wrapper 又去下载 `gradle-9.6.0-bin.zip`，改用本机已经解压的 Gradle，并设置 `JAVA_HOME` 为上面的 JBR：

  `C:\Users\75192\.gradle\wrapper\dists\gradle-9.6.0-bin\42k10rwplmzkhuboz9kdazi7s\gradle-9.6.0\bin\gradle.bat`

- 单元测试：`:app:testDebugUnitTest`。Robolectric 4.17、sdk 34、内存数据库。`TrainingTrendScreen` 里 `Modifier.menuAnchor()` 的弃用警告是旧的，没让改就别动。



## 目录



```

app/src/main/java/com/example/gymlog/

  MainActivity.kt              主题入口

  GymLogApplication.kt         Room、各 Repository、启动时导入数据集并重算 PR（不再补旧预置）

  data/                        Entity、Dao、Migration、纯逻辑（含 CalendarDayColor、DayKind）

  data/ActiveWorkoutNotifier.kt / WorkoutNotificationService.kt / WorkoutNotificationReceiver.kt / WorkoutNotificationState.kt

                               「正在训练」系统通知（前台服务），见「正在训练通知」小节

  ui/GymLogApp.kt              导航、五个根页面、日历上的训练卡片与日类型

  ui/home/                     首页：周/月汇总、当天足迹；HomeTheme.kt 是首页材质 token

  ui/train/                    训练入口（动作库、训练记录）

  ui/session/                  编辑一次训练

  ui/exercise/                 动作库与示意图

  ui/body/                     身体数据（展示/编辑/历史）

  ui/muscle/                   人体底图、遮罩高亮、围度标注

  ui/photo/                    照片墙、详情、编辑器

  ui/stats/                    趋势图

  ui/progress/                 「趋势」入口

  ui/timer/                    训练计时器

  ui/settings/                 「我的」、设置、数据备份、关于、头像裁剪

  ui/calendar/                 月历展开、日类型选择器

  ui/theme/                    主题、部位颜色；CeramicSurface.kt = 全站卡片材质与页面背景

app/src/main/res/drawable-nodpi/   diagram_<preset_key>.png，以及 muscle_*_base

app/src/main/res/xml/photo_file_paths.xml   照片墙 FileProvider 路径

app/src/main/assets/muscle_masks/  front|back/<muscle_id>.png，1024×1536

app/src/test/                    逻辑与迁移测试，不覆盖界面导航

```



1.4 / 1.4.1 / 1.4.2 相关界面文件：



- `ui/settings/BackupDataScreen.kt`：导出/导入和导入确认、结果提示；文案说明照片墙不进备份。

- `ui/timer/TimerHistoryScreen.kt`：已保存计时列表，点条目进入 `SavedTimerScreen`；当前没有任何界面入口打开它（设置历史入口已删），路由保留兼容。

- `ui/body/BodyRecordScreen.kt` 的 `BodyRecordHistory` 是 `internal`，只在身体数据页内切换复用（设置侧入口已删）。

- `ui/photo/PhotoWallScreen.kt`、`PhotoEditorScreen.kt`、`PhotoEditorCanvas.kt`、`PhotoCanvasMath.kt`、`PhotoDialogs.kt`、`PhotoWallViewModel.kt`：照片墙与编辑器（2026-10-25 结构重构后的划分，见「照片墙」小节）。

- `ui/calendar/DayKindSelector.kt`：休息日 / 放纵日 / 偷懒日选择。

- `data/CalendarDayColor.kt`：小日历与展开月历共用的上色逻辑。



依赖方向：界面 ViewModel → Repository → Dao。重量、PR、趋势、日历排序/上色、首页汇总、计时、日类型纯逻辑放在 `data/` 里，测试打这些函数。



## 信息架构（当前）



冷启动在「首页」。底部栏只在五个根页面出现：



| 根页面 | 内容 | 再进去 |

| --- | --- | --- |

| 首页 | 按周或按月看训练时长、训练天数、总组数、容量；本周训练日；选中日按部位的足迹和已保存计时。顶栏是「首页」，齿轮进设置 | 开始训练会把日历选到今天并切到日历；也能打开当天训练、已保存计时、身体数据、趋势、日历 |

| 日历 | 月历（按部位容量或特殊日上色）、选中日期的训练卡片、日类型（休息/放纵/偷懒）；右下角无 FAB，顶栏新增训练入口已按需求删除，只剩「今天」和设置；训练计时器已移到训练页。顶栏标题用 `home_title`，跟界面语言走，现在是「练有所迹」。点标题直接进关于。桌面图标仍用 `app_name`，系统语言为英文时是 Traivol | 展开整月、编辑训练、选动作、已保存计时 |

| 训练 | 两个入口：开始训练、动作库；有活动训练时底部显示可恢复的缩小卡片 | 开始训练直接打开今天的新训练草稿；返回训练页会保留并继续计时；动作库是子页面，有返回 |

| 趋势 | 「训练足迹」「身体数据」 | 对应趋势页 |

| 我的 | 用户名、头像、可切换的统计、「身体数据」与「照片墙」入口；资料表单可改性别 | 身体数据、照片墙（网格→详情→编辑）。头像在这一页裁剪 |



五个根页面的齿轮都进同一套设置。设置里是外观、数据、关于、清除本地数据；数据下只有一个子页面：「数据备份」（导出/导入）。身体记录历史从身体数据页内进入，设置里原来的「历史记录」入口已删。关于再进版本更新。从日历标题打开的关于，返回回日历，不经过设置。



子页面隐藏底部栏。五个根页面用 `TabPane` 一直留在组合里：切 tab 不新建 ViewModel，日历选中日期、首页周月选择和列表滚动保留。子页面盖在上面。



`Screen` 要点：



- 冷启动是 `Screen.Home`。

- `SessionEditor` 带 `draftKey` 和 `returnTab`。新建记录的 key 是 `new-<uuid>`，编辑已有记录用 session id。ViewModel key 是 `"${draftKey}-$dataEpoch"`。日历上新建或编辑，保存后回日历并停在原来的日期。首页点进某次训练，`returnTab` 是首页。

- `PickExercise(returnTo)` 回到原来的 `SessionEditor`，草稿还在。选择器 ViewModel key 是 `picker-${draftKey}-$dataEpoch`，与训练 tab 的动作库 `library-$dataEpoch` 分开；新建训练不会继承之前的搜索/筛选。

- `CalendarExpanded` 返回日历。点某一天会选中它并回到日历。

- `Settings` / `BackupData` / `TimerHistory` / `About` / `ReleaseNotes` 带 `from: RootTab`。返回回到打开设置时的那个根页面，不在五个 tab 之间做返回栈。`About` 和 `ReleaseNotes` 还有 `openedFromSettings`。`SettingsHistory` 和 `BodyHistory` 两个路由已随设置历史入口删除。

- `TimerHistory` 页面和路由仍保留兼容，但没有任何界面入口（设置历史入口已删）；系统返回回 `from` 根页面。从日历当天已保存计时进入 `SavedTimer`，详情返回日历/原根页面。

- `BodyRecord` 和 `MuscleMap` 带 `returnTab`。首页和「我的」都能进身体数据。肌群图返回身体数据展示，再返回原来的根页面。

- `PhotoWall` → `PhotoDetail(id)` → `PhotoEditor`。详情/编辑返回照片墙；照片墙返回「我的」。归在 `RootTab.Mine`。

- `SavedTimer` 带 `returnTab` 和可选的 `returnToTimerHistory`。计时器页本身返回日历。

- 根页面上系统返回直接退出应用。子页面的系统返回与该页顶栏返回一致。

- 对话框、日期选择、下拉菜单、头像裁剪打开时，第一次返回只关这一层。页面级 `BackHandler` 在浮层打开时必须 `enabled = false`。导入确认在「数据备份」页里，打开时先关对话框。



导入或清除数据后 `BackupRepository.dataEpoch` 加一。身体数据、两条趋势、动作库、训练编辑、「我的」的 ViewModel 都把 `dataEpoch` 放进 key，从而丢掉过期草稿。「我的」的 key 是 `mine-$dataEpoch`。`DataTransferViewModel` 现在在数据备份页用固定 key `data-transfer`，不要加 `dataEpoch`，否则导入成功提示会丢。首页 ViewModel 只记周/月和选中日，不放 `dataEpoch`。计时器 ViewModel 是进程内单例，进行中的秒表不跟 `dataEpoch` 走；计时器页、计时记录页和已保存计时页共用它。照片墙 ViewModel 当前不绑 `dataEpoch`（清除/导入也不动照片墙）。



## 数据模型



`GymLogDatabase` version 19。迁移链：1→…→14→15→16→17→18→19，全部在 `GymLogMigrations.kt`，在 `GymLogApplication` 注册。



| 版本 | 变化 |

| --- | --- |

| 1→2 | 旧的「一天一条」`workout_records` 迁成动作 + 训练会话 + 逐组。补 `weightMode`、`exerciseOrder` |

| 2→3 | `body_profile`（生日）、`body_measurements`（身高、体重、体脂） |

| 3→4 | 动作上的做组 PR、极限 PR 列 |

| 4→5 | `exercises.archived`，默认 0 |

| 5→6 | 腰围 `waistCm`、臂围 `armCm`，可空 |

| 6→7 | `day_notes`：主键 `dateEpochDay`，一句 `message` |

| 7→8 | `exercise_muscles`：只存自定义动作的肌群映射。主键 `(exerciseId, muscleId)`，外键级联删除 |

| 8→9 | `body_profile` 重建，增加可空 `gender`。旧生日保留 |

| 9→10 | `timer_records`、`timer_laps`。计次外键级联删除 |

| 10→11 | `body_profile.displayName`、`avatarFileName`，都可空 |

| 11→12 | 围度列：左右臂、左右小臂、胸围、臀围、左右腿围、左右小腿。`waistCm` 和旧的 `armCm` 不动 |

| 12→13 | 动作库数据层：`exercises` 增加 `nameEn`、`source`、`sourceId`、`equipmentDetail`、`bodyPartDetail`；新增内容、媒体、外部来源映射、原始肌群词元、旧预置匹配候选和导入状态表 |

| 13→14 | 分类体系：`exercises` 增加 `bodyCategoryId`、`equipmentCategoryId`、`classificationStatus`、`classificationVersion` 和两个索引；旧数据由运行时 `ExerciseClassificationMigrator` 逐行补齐 |

| 14→15 | `day_kinds`：主键 `dateEpochDay`，`kind` 存枚举名（`REST` / `INDULGE` / `LAZY` 等） |

| 15→16 | `photo_wall`：照片墙元数据；原图/编辑图在应用私有目录，不在此表存 BLOB |

| 16→17 | 删除 `body_measurements.armCm`：重建表并保留其他身体数据；旧单侧臂围数据不再存在 |

| 17→18 | `workout_sessions.durationMillis`：训练总时长，老会话默认 0 |

| 18→19 | `exercise_durations`：每次训练每个动作的动作用时；外键关联 session/exercise，不参与当天总时长 |

| 19→20 | `exercises` 增加 `priority`（INTEGER NOT NULL DEFAULT 0）、`howTo`、`cues`（TEXT 可空）。只加列、不动旧数据 |

| 21→22 | `exercises` 增加 `originalName`（TEXT NOT NULL DEFAULT ''）。加列后在迁移里逐行按 `ExerciseNameCleaner` 回填：`source = EXERCISES_DATASET_ZH` 的按规则清洗，其余（预置 / 自建 / 旧数据）原样等于 `name`。`name` 等旧字段一个都不动 |



表：



- `workout_records`：遗留表，仍参与备份。新记录不再写这里。

- `exercises`：预设、自定义和新数据集动作。删除是 `archived = 1`，历史组还指着它。列表查询排除已归档。预设用 `INSERT OR IGNORE`，删过的预设不会被重新种回来。旧 ID 不变；新数据集动作是 `ds_0001` 这类 ID，`sourceId` 保留原始数字 ID，`source` 为 `LEGACY` / `CUSTOM` / `EXERCISES_DATASET_ZH`。`nameEn`、`equipmentDetail`、`bodyPartDetail` 可空。`bodyCategoryId` / `equipmentCategoryId` 是稳定分类 ID；`bodyPartDetail` / `equipmentDetail` 保留原始值；`classificationStatus` 为 `DATASET` / `MAPPED` / `AMBIGUOUS` / `UNMAPPED` / `UNMIGRATED`，`classificationVersion` 为逐行迁移版本。旧 `bodyPart` / `equipmentType` 仍保留，作为历史分组和兼容字段。

- `exercise_content`：动作的本地化名称、说明和分步说明；`(exerciseId, locale)` 为主键。当前导入 zh/en。

- `exercise_media`：缩略图/GIF 的源路径、本地路径、远程 URL、尺寸、署名、SHA-256、字节数和下载状态；不存 BLOB。阶段 F 起 1,324 张 180×180 JPG 和 1,324 个 GIF 已打包在 `assets/exercises/media/images|videos`，数据库仍按稳定 `ds_<datasetId>` 登记。

- `exercise_source_map`：外部数据源 `(source, sourceId)` 到本地动作 ID 的映射；当前 1,324 条数据集记录映射到 `ds_*`。旧预置不自动关联。

- `exercise_muscle_tokens`：保留数据集原始的 target/muscle_group/secondary 词元，以及 MAPPED/AMBIGUOUS/UNMAPPED 状态和候选 ID；未映射词元不会静默丢失。

- `exercise_preset_match_candidate`：89 个旧预置的显式匹配候选：23 个 `PENDING_CONFIRMATION`、1 个 `AMBIGUOUS`、65 个 `NO_MATCH`。确认前不自动关联。

- `exercise_import_state`：数据集导入版本和新增/跳过/待匹配/未映射统计。

- `workout_sessions`：一次训练，日期是 `dateEpochDay`；`durationMillis` 是训练总计时，老会话迁移后为 0。`createdAt` 继续表示创建时间，不表示开始/结束时间。

- `workout_sets`：一组。`weightMode` 存枚举名。`BODYWEIGHT` 的 `weightKg` 存 null。`BODYWEIGHT_PLUS` 只存增减量，可以是负数。新训练只保存已勾选完成的组；未完成组只存在于当前编辑草稿。

- `exercise_durations`：每个动作实例的动作用时。`sessionId`、`exerciseId`、`exerciseOrder`、`durationMillis`、`createdAt`；不进入当天总时长。动作用时从动作加入训练开始，到点击「完成动作」结束；再次点击完成后覆盖同一动作的用时。

- `body_profile`：固定一行。生日、性别（`MALE` / `FEMALE` / `OTHER`）、用户名（去空白后最多 24 字）、头像文件名。年龄由生日现算，不存时间戳。头像文件名只指向 `filesDir/profile/avatar.jpg`，字节不在库里。

- `body_measurements`：一条快照。身高、体重、体脂、腰围，以及 11→12 的左右臂围、左右小臂、胸围、臀围、左右腿围、左右小腿，全部可空。任一变化就插入新行；全空不插入。16→17 已物理删除旧的单侧臂围 `armCm`，当前代码没有这个字段，历史页也不再显示旧臂围。旧备份如果还带 `armCm`，导入时直接忽略。

- `day_notes`：没训练的日期上的一句话。空白保存即删除该行。选特殊日类型时可带默认文案（见「日历日类型」）。

- `day_kinds`：用户对某一天的显式标记。主键 `dateEpochDay`，`kind` 为 `DayKind` 枚举名。有训练的日子未存行时解析为训练日；库里一天最多一条。再点同一特殊标记可清除显式选择。

- `photo_wall`：照片墙元数据。`id`、`originalRelativePath`、`editedRelativePath`（可空）、`dateEpochDay`（用户确认的日历日，不是 UTC 瞬时）、`createdAt` / `updatedAt`、`note`、`width` / `height`。文件在 `filesDir/photo_wall/originals|edited/`，缩略图缓存在 `cacheDir/photo_wall_thumbs/`。路径必须经 `PhotoPaths.isSafe` 校验。

- `exercise_muscles`：自定义动作的主要/次要肌群；数据集动作只写入 `DatasetMuscleMapping` 中明确、非歧义的映射，泛化/未映射词元只进 `exercise_muscle_tokens`。旧 89 个预设仍以 `PresetMuscleMappings` 为准，不批量写入这张表。

- `timer_records`：已保存的训练计时。`durationMillis`、`dateEpochDay`、可空 `sessionId`（新记录为 null，旧记录可能仍指向训练）。删计时不改训练内容。

- `timer_laps`：该条计时的计次。`intervalMillis` 是这一段，`elapsedMillis` 是累计。



进行中的秒表不在 Room 里。它在 SharedPreferences `gymlog_timer`，由 `TrainingTimerStore` 读写。开机次数用 `Settings.Global.BOOT_COUNT`：次数变了，或 `elapsedRealtime` 倒退，就把 `RUNNING` 收成 `PAUSED`，并标 `recoveredAfterReboot`。这段状态不进备份，清除本地数据也不会擦掉它。



动作库与「添加训练」的选择页共用 `ExerciseBrowsePane`：**切换左侧部位分类（`BodyCategoryRail`）或器械筛选（`EquipmentCategoryBar`）后，右侧动作网格必须从顶部开始**，不能沿用上一组滚到的位置；**重复点当前已选中的那一项也回顶**。实现是在 `ExerciseLibraryScreen` 里用 `LaunchedEffect(state.bodyPart.id, state.equipmentType.id, topRequest) { gridState.scrollToItem(0) }`（页面级 `rememberLazyGridState` 提升），点击回调里把 `topRequest++`（`mutableIntStateOf`）——筛选没变时靠计数器重新触发。**不要**在点击回调里 `animateScrollToItem`：列表数据是异步刷新的，点击瞬间抢滚动会被随后的重建覆盖，现象就是「还停在原来滚下去的距离」（踩过）。也不要为了「点当前项」去注册 `onDoubleTap`：那会让每次单击都等双击判定才生效，切分类会明显变慢。分类栏手势只注册单击（`detectTapGestures(onTap)`）；回调仍经 `rememberUpdatedState` 再读、`pointerInput` 的 key 只留 `part.id`，避免状态重组取消进行中的手势。



动作详情页（`ui/exercise/ExerciseDetailScreen.kt`）三页共用同一个 `ExerciseDetailViewModel`，动作 ID 由导航参数固定，切换 tab 不重新选动作，不存在串数据。顶部是等宽三段标签栏 + 下划线指示条，内容用 `HorizontalPager` 承载：可以点标签跳、也可以左右滑动切页；指示条的偏移用 `pagerState.currentPageOffsetFraction` 插值，拖动过程中跟着手指走，不是松手后才跳。纵向滚动由各页自己的 `verticalScroll`/`LazyColumn` 处理，与横滑不冲突。历史记录按 `exerciseId` 精确 JOIN（`ExerciseDao.observeHistoryForExercise`），不按名称匹配；容量走 `WeightLogic.effectiveKg × reps`，与统计页、日历配色同一口径；各组次数不一致或缺失时，摘要显示「4 组 · 10/10/8/7 次」而不是假装统一。每组序号用「第几组」的位置序号，不依赖 `setIndex` 的起始值（历史数据从 1 开始存）。



**别名、动作怎么做、动作细节这三处内容一律用中文**（专有名词里的字母如「T 杠」保留），不要在数据里写英文别名或英文说明：界面上这三处和动作名是并排展示的，混进英文会显得突兀。`PresetExercises` 的 89 条旧预置动作别名已全部改成中文说法（如「平板卧推, 平板杠铃卧推, 卧推」）。



动作的三个新字段：`priority` **由收藏功能统一管理，前台不显示也不可编辑**——收藏 = 10、取消 = 0，两者在 `ExerciseDao.setFavorite` 的同一条 UPDATE 里落库，`isFavorite`（v21 新增）独立记录收藏状态；0–10 是合法范围，0 等于维持原来的「练得多 → 最近练 → 名称」顺序（v20 及之前的 0–100 由 MIGRATION_20_21 缩放：1–10 不变、11–100 除十取整）；`howTo` / `cues` 是两段纯文本（怎么做 / 细节），空表示未维护；`aliases` 沿用已有列，多个别名用逗号分隔，读取走 `ExerciseAliases.parse`。这四项都通过详情页右上角「编辑资料」维护，写入只走 `ExerciseRepository.updateTrainingFields`，不碰名称、分类和肌群映射。



肌群目录在 `MuscleCatalog`（稳定 `muscleId`）。图用 `drawable-nodpi` 里的 `muscle_front_base` / `muscle_back_base`，再叠 `assets/muscle_masks/{front|back}/<muscle_id>.png`。底图和遮罩都是 1024×1536，必须画在同一个目标矩形里。坐标换算、frame 和点击反算统一在 `MuscleFigureGeometry`。遮罩按透明度命中；命中优先使用当前实际绘制的层，`quadriceps` 合并遮罩被绘制时整块命中 `quadriceps`，只绘制子头时命中子头，不再出现“画合并、点中某个子头”的错位。未训练的空白区域按最小遮罩命中，用于选中查看。深层肌（菱形肌、肱肌、腹内斜肌、腹横肌）对应遮罩是空的，不拿邻近肌肉代替；缝匠肌正面有独立遮罩。高亮只表示「这段时间的训练记录涉及」，不表示刺激量或恢复。训练主要/次要仍用红色系；用户选中用当前主题的 `primary`（蓝色）叠加，未训练的选中不会看成已训练。同一肌群当天只高亮一次；任一动作为主要则显示主要色。`MuscleAssetCache` 按面懒加载：身体数据页只加载正面，肌群图切到背面再加载背面；加载失败时保留底图并显示重试，身体数据与肌群图共用进程缓存。



预设动作 id 仍是 `preset_<key>`，旧 id 没有改。阶段 D 起不再自动补种旧预置；新安装只有数据集和用户自定义动作。旧预置行如已存在，仅保留给历史训练、PR、备份解析，不进入活动目录；不物理删除，避免破坏外键/历史。数据集动作是 `ds_<原始数字 ID>`，例如 `ds_0001`；`sourceId` 仍是 `0001`。数据集导入不自动关联旧预置，也不覆盖旧动作的分类、PR、归档状态或训练组。动作库和训练动作选择器只显示 `archived = 0`、非 `preset_*`、且 `source = EXERCISES_DATASET_ZH` 或 `isCustom = 1` 的动作。历史详情、PR、趋势和备份仍走全量查询。



标准分类在 `ExerciseClassification`：身体部位使用数据集的 `bodyPartDetail` 10 类（chest、back、shoulders、upper arms、lower arms、upper legs、lower legs、waist、cardio、neck），器械使用 `equipmentDetail` 28 类。稳定 ID 形如 `body_upper_arms` / `equipment_barbell`；原始值保留在 `bodyPartDetail` / `equipmentDetail`。旧 preset/custom 的 CHEST/BACK/SHOULDERS/ABS 映射到标准部位，ARMS/LEGS/OTHER 进入内部兼容分类；旧器械三类进入内部兼容分类，不按名称猜 28 类。旧 `bodyPart` / `equipmentType` 保留给历史分组。`ExerciseClassificationMigrator` 逐行补齐并写 `classificationVersion = 1`，重复运行不覆盖已有分类。正式筛选只展示 `bodyUi` 的 10 类 + 全部、`equipmentUi` 的 28 类 + 全部；兼容桶仅内部解析/历史展示。动作库、选择器、搜索/筛选、自定义动作、详情、训练趋势、首页/日历/历史展示优先使用新分类；极旧无分类行回退旧 broad 字段。



启动时数据集 `ExerciseCatalogImporter.importIfNeeded()`，然后 `rebuildAllPersonalRecords()`；不再补种旧预置。导入器、备份恢复和清除共用 `ExerciseCatalogCoordinator` 的一把 `Mutex`：`importIfNeeded()` 自己取锁；`BackupRepository.importSnapshot()` 和 `clearLocalData()` 取同一把锁并调用 `importIfNeededLocked()`，不能在锁内再调用公开入口。导入在后台读取 `assets/exercises/dataset_v1.json`，在事务中写入；重复执行幂等。整批导入失败会回滚，并写 `exercise_import_state.failed = 1`；下次启动重试，不把失败伪装成成功。`force = true` 只刷新来源元数据/内容/词元，保留媒体下载状态、非自动候选状态和已有数据集肌群关联。媒体源路径/校验值登记在 `exercise_media`；阶段 F 已将 1,324 张缩略图和 1,324 个 GIF 打包到 assets。



## 训练规则



三种重量模式，枚举名不要改：



- `WEIGHTED`：普通重量。不能为负。加减步长 2.5 kg，最低 0。空值点加号变成 2.5。

- `BODYWEIGHT`：自重。不填公斤。有效重量 = 当天及之前最近一条非空体重；没有、或 ≤ 0，按 0。未来的身体记录不影响更早的训练。同一天多条时，忽略体重为空的行，取日期和 `createdAt` 更晚且体重非空的那条。

- `BODYWEIGHT_PLUS`：界面文案「自重负重」。有效重量 = 上面的体重 + 填写的增减。负数表示辅助。普通重量、身高、体重、围度仍不能为负。



容量 = Σ 有效公斤 × 次数。有效公斤 ≤ 0 或次数 ≤ 0 的组容量为 0，但这些组本身仍算训练过，计入训练天数和部位。次数加减步长 1，最低 0，空值点加号变成 1。



展示已解析的重量时统一是「x kg」。调用方先算有效重量，不要在 PR 展示上再加一次体重。



### 组内重量 / 次数的手动输入



`SetRow` 的两个数值格（`VerticalNumberControl`）是 `BasicTextField`，点数值本身就能唤起键盘；上下箭头仍按原步长（重量 2.5 kg、次数 1）调节。两条路径写同一份状态（`normalKgText` / `extraKgText` / `repsText`，本来就是字符串），没有新增第二条数据通道。



- **逐字只过滤、不格式化**：`WeightLogic.sanitizeWeightInput(raw, signed)` 只保留数字与一个小数点，`BODYWEIGHT_PLUS` 额外允许开头一个负号；`sanitizeRepsInput` 只留数字、最多 4 位。必须保留「22.」这种中间态，否则逐字格式化会吃掉小数点，导致 22.5 永远打不出来，光标也会跳。

- **确认时才收敛**：失焦或键盘 `Done` 时走 `normalizeWeightInput` / `normalizeRepsInput`，解析不出来就清空，交给既有的「重量必填」校验（`storedWeightKg` 返回失败），**不写 0、不写 NaN/Infinity**。

- 输入过程中允许为空，不会因为中间态为空就回写成 0；`BODYWEIGHT` 模式下重量格 `enabled = false`，仍只显示当天体重，行为未变。

- 键盘：重量 `KeyboardType.Decimal`、次数 `KeyboardType.Number`，`ImeAction.Done` 收起键盘；聚焦 250ms 后调 `bringIntoView`，配合页面既有的 `imePadding()` 避免输入区被键盘遮挡。



动作默认模式若存的是 `BODYWEIGHT_PLUS`，新开一组时 `defaultModeForExercise` 仍落成 `WEIGHTED`。引体等预设保持 `BODYWEIGHT`，用户自己把这一组切到自重负重。



新建训练里第一次加入某个动作：优先抄本草稿里这个动作的最后一组；否则抄历史里该动作最新一次训练的最后一组（日期、会话创建时间、`setIndex` 都取最新）。从没练过则为空。组 id 必须是新的 UUID。同一条草稿里再加一组，抄上一组草稿。



训练记录卡片上的组是 2 列 × 3 行，最多 6 格。1–6 组显示真实数据；超过 6 组时前 5 组照常显示，第 6 格是 `…`。逻辑在 `sessionSetGridCells`。数据库中的组不截断，详情/编辑仍能看到全部。



## PR、日历、趋势、首页



PR 在写入前先把每组换成当天的有效重量。



- **极限 PR**：所有有效组里有效重量最大的那一组，**不看次数**，也不用任何公式推算；

  同为最重时取更晚的日期。纯自重组（有效重量 0）不参与，避免出现「极限 PR 0 kg」。

- **做组 PR**：次数达到「做组 PR 最低次数」（默认 6，可设 1–30）的有效组里，比重量再比次数再比日期。

  门槛存在 `TrainingPreferencesStore`（`gymlog_training_prefs` / `working_pr_min_reps`），

  计算时通过 `workingPrMinRepsProvider` 现读，改完立即生效。

- 两套候选规则集中在 `PersonalRecordLogic.isMaxCandidate / isWorkingCandidate`，

  仓库写入和趋势曲线（`TrendLogic.runningPrSeries`）共用，不允许各写一套。

- 「设置 → 偏好 → 刷新成绩」调用 `WorkoutRepository.rebuildAllPersonalRecords()`：

  先只读算出全部动作的新 PR，再在一个事务里写回，只写真正变化的动作，返回变化的动作数。

  不碰训练记录本身。App 启动时也会跑一次，用于把旧规则存下的 PR 迁移到新规则。

- 训练偏好页（`TrainingPreferencesScreen`）的版式约定：卡片里只放标题、控件和校验错误，

  **不放解释性灰字**——所有说明统一收进**卡片标题行右侧的小问号**（`Icons.Outlined.HelpOutline`，

  32dp 视觉尺寸）里的说明对话框；问号跟着卡片走，以后每张偏好卡各自带一个、只解释本卡内容，

  避免灰字越堆越多、卡片越来越长。

- 「刷新成绩」与步进器同排（`BoxWithConstraints` 量卡片内容宽度：≥ 292dp 并排、按钮 `weight(1f)`

  吃满剩余宽度；更窄则整行折到下一行）。按钮文字用 `labelMedium` 且 `maxLines = 1`，

  2 倍系统字体下也不会被截断。

- 只有更高才覆盖。相同成绩（重量和次数都相同）保留更晚的日期。

- 趋势里的 PR 曲线仍只在成绩提高时出现新点，相同成绩换到更晚日期不会新增点。

- 删除训练后，用剩下的组重算受影响动作的 PR。保存身体记录后重算全部 PR，因为自重有效重量变了。



日历上色在 `CalendarDayColorLogic`（`data/CalendarDayColor.kt`），小日历与展开月历共用同一套 `paintsFor`，不要各写一套。规则：



- 普通训练日：按**合并后的部位大类**容量最高者上色。大类六种：胸、背、肩、腿（上腿+下腿+旧 LEGS）、臂（上臂+前臂+旧 ARMS）、其他（腰/有氧/颈/旧 ABS/OTHER/未知等，色 `#90A4AE`）。并列时按枚举顺序：胸 → 背 → 肩 → 腿 → 臂 → 其他（与查询顺序无关）。

- 特殊日（`REST` / `INDULGE` / `LAZY`）盖过部位色：偷懒 `#FFEB3B`、放纵 `#EC407A`、休息 `#D8DDE3`。库中一天只有一种标记；若逻辑上同时出现多种，优先懒 → 放纵 → 休息。

- 没有组、也没有特殊标记的日子不上色。仅存 `TRAINING` 标记但无组，也不发明颜色。

- 容量在 Kotlin 里用有效重量算，不是 SQL 的 `SUM`。零容量但仍有组的类别仍占位，避免训练日无色。

- 日历页默认按「选中日所在周」显示，月历随 `LazyColumn` 一起滚动，不是 sticky header；卡片底部居中的小箭头可在整月和当周之间切换。



同一天的训练卡片，以及卡片里的动作，按部位分组后按容量从大到小排。部位组的顺序也是该部位当天容量大的在上面。容量相同则保持原来的先后。



「点击添加计划」用日历选中的日期，不是今天。首页的「开始训练」会先把日历选到今天，再切到日历，不直接打开编辑页。



## 日历日类型



枚举 `DayKind`：`TRAINING`、`REST`、`INDULGE`、`LAZY`。解析与点击切换在 `DayKindLogic`。



- 界面选择器（`DayKindSelector`）只提供休息日 / 放纵日 / 偷懒日；有组时训练日由数据推断，不必单独点「训练日」入库。

- 小日历不画图例；展开月历可有图例（两行）。格子上的短标记：休息 `Zzz`、放纵 `吃吃吃`、偷懒 `躺平`。

- 选中特殊日时，若当天备注为空，可用对应默认备注文案（`day_note_rest` / `day_note_indulge` / `day_note_lazy`）。

- `day_kinds` 进入备份 JSON 键 `dayKinds`（可选）；导入校验重复日与非法 `kind`。清除本地数据会清这张表。



趋势页可以点选某一天，看当天的点。纵轴公斤最小刻度 2.5，体脂和 BMI 是 0.5。



- 训练足迹：容量、做组 PR、极限 PR。先选部位和器械，再选动作。容量按该动作当天有效重量汇总，只画大于 0 的点。PR 曲线仍是「只有更高才出现新点」。图上的重量：非空且非 0 才画（辅助的负数要画出来，0 不画）。

- 训练足迹页面结构（2026-11-08 改版）：最上面一行动作选择条（2026-11-09 起：左侧 7dp 蓝色圆点 + **完整动作名 `name` 左对齐**，右侧是「部位」浅蓝胶囊 + 小箭头；名字不再居中，长名有整行可用，不需要再算对称留白），点开是底部面板（布局照动作库：上方搜索框，下面左侧部位竖排 `BodyCategoryRail`、右侧器械横排 `EquipmentCategoryBar` + 列表，面板高度 86%；列表也是**完整名 `name`** + 「部位 · 器械」小字）。候选规则在 `trendCandidates()`（纯函数，有单测）：**不输入时只列练过的**、按最近练过倒序取 20 条；**输入后连没练过的也搜**（匹配 `originalName`/`name`/`nameEn`/`aliases`），练过的排前面，没练过的在列表小字后加「未练过」。选中没练过的动作会切到空图（「还没有足够的数据画趋势。」）。下面是趋势卡：左上大字 = 当前指标最新值（`headlineSmall`，字重由全局 typography 统一给 Medium，页面内不再单独 `.copy(fontWeight=...)`）（点图上的点会切到那天），下面一行是「较上次 ±x%」或那天的日期，右上是指标开关（容量/做组 PR/极限 PR；2026-11-09 起选中项 = `primary` 62% 透明度的蓝胶囊 + 白字，未选是次级灰字，整条底色是 `surfaceVariant` 55%），在卡片上左右滑也能切，滑到头停住不循环。卡片下方是三格统计（训练次数 / 总容量 / 最好成绩）。
  - 已知取舍：选了没练过的动作后再打开面板（搜索框为空），列表里不会出现它——默认列表只有练过的。
  - `TrendChart` 为此加了三个可选参数：`chartHeight`、`showHeader`（关掉内置「大字 + 日期」表头）、`onShownChange`（回调当前显示的点，供页面自己画表头）。身体数据页仍用默认值，行为不变。

- 训练足迹下面的两块（2026-11-08 补，对应效果图「方向 3」）：三格统计之后是**练习频率卡**和**训练记录卡**，页面本身不滚动，最后一张卡 `weight(1f)` 吃掉剩余高度、卡内用 `LazyColumn` 滚动，图和统计始终留在屏幕上。
  - 练习频率：近 12 周每周一格（`weeklyActivity()`，纯函数有单测），按这周练过这个动作的**不同日期数**上色（0 天浅灰底 / 1 天 / 2 天 / 3 天以上最深），左下角是起始月份、右下角写「本周」；右上小字是「近 12 周 · 平均 N 天一次」（`averageGapDays()`，窗口内相邻训练日的平均间隔，练不到两次时改显示「近 N 周 · X 天」）。整张卡在该动作窗口内没练过时直接不显示。
  - 训练记录：一场训练一行，左列日期 + 星期（列宽 `68.dp * fontScale`，字号放大后不会和右边的「最好一组」挤在一起），中间是最好的一组「70 kg × 10」+「4 组 · 56 次」，右列当次容量 + 「容量」；只有最好那一组标「最佳」（并列时标最近一次），右上角「共 N 次」。没有记录时显示「还没有这个动作的训练记录。」。
  - 这两块的标题行共用 `SectionHeader`（左标题 14sp Medium、右侧 11sp 次级色），数字都开 `tnum`。
  - 记录卡滚动提示（2026-11-09 加）：`LazyColumn` 上下各盖一层卡片色渐隐——`canScrollBackward` 时顶部 16dp、`canScrollForward` 时底部 34dp，滚到头自动消失。渐隐色取 `surfaceContainerLowest`，与 `ceramicSurface` 的卡面色一致；别用纯白，深色模式会穿帮。

- 身体数据：体重、体脂、BMI。没有围度曲线。体重和体脂都要大于 0 才画。BMI 用到该日为止最近的正身高。



首页汇总在 `HomePeriod`。周从周一开始。训练天数是区间内有组的不同日期数，容量为 0 的组也算有组；当天足迹的部位顺序与日历相同，0 容量的部位也会显示，汇总容量显示为 0 kg。时长只加已保存计时，不加还在跑的秒表。



### 首页视觉体系（2026-11-08 第二轮精修）

首页不再用 `LiquidGlassCard` / `GlassBackdropBlobs`，材质只在 `ui/home/HomeTheme.kt` 里定义，页面不写字面值：

- **三层**：`homeBackground` 淡蓝灰底（`#EDF2FC`）+ 四角大范围色晕（左上 `#DFEAFE`、右上 `#EEF0FF`、右下 `#E8E5FA`、左下 `#E4F2F7`），半径 1.2–1.35 倍屏宽、中心压在屏幕外侧，中间档位把颜色压到 45% 再化到透明，所以看不到光斑和硬边；顶部还有一层 11% 白的天光→ `Modifier.ceramic(colors, CeramicStyle.Overview|Flat|Warm)` 不透明哑光陶瓷（三种白靠细腻度与色温区分，**不是**透明度）→ `Modifier.frost(...)` 磨砂玻璃只贴分段器、日期切换、小标签、底栏这类小面积控件。
- **投影**：只到卡片下沿 6–13dp，`shadow` 的 ambient/spot 都用中性 `rgba(32,43,64,…)`，不再有蓝紫投影；卡片轮廓靠 `rim` 浅灰 1dp 圈边 + 顶部白色釉面高光，不依赖阴影。
- **深浅色**：`homeColors()` 按 `LocalGymLogDarkTheme` 取 `LightHomeColors` / `DarkHomeColors`。深色下 `sheen` 必须用 `0x1FFFFFFF`，否则卡面顶部会出现一条发亮的灰带；`onBrand` 决定品牌按钮上的文字色。
- **磨砂在纯白卡上会"消失"**：浅色的 `frostFill` 是带淡蓝灰的雾面（`0xC6EEF2F9`）而不是纯白透明，`frostBorder` 是浅灰描边，否则分段器底槽和描边按钮在白卡上完全看不见（第一版实机就是这个毛病）。
- **训练时长排版**：`durationParts()` 把时长拆成「数字 + 单位」，小时为主、分钟次之；不足 1 分钟显示秒，和日期标题行的 `formatDurationChinese` 口径一致（否则会出现"0 分"旁边写着 12 秒）。
- **当天有记录**：新增「今日训练」汇总卡（时长 / 总组数 / 容量 + 单一训练时的「查看详情」），下面按部位分卡。`singleSessionId` 只在当天恰好一场训练时非空，多场时不显示详情入口，避免指向不明。
- **日历**：无分隔线，七列等宽；选中=实心蓝圆白字，今天=品牌蓝细环+蓝字，练过=圆下 4dp 蓝点（选中且练过用 55% 透明度），三者可叠加不误读；切换用 180ms 颜色过渡。

底部导航（`GymLogApp.kt` 的 `RootTabBar`）同时改成整条白色磨砂栏：56dp + `navigationBarsPadding()`，选中态是浅蓝 54×29 圆角底 + 蓝图标蓝文字，未选中用 `onSurfaceVariant`。**内容区底部预留从 `measuredBarHeight - 24dp` 改成按完整栏高让开**，否则首页最后一张卡会被导航栏压住（这是本次修复的 bug，其它根页面同样受益）。首页 `LazyColumn` 自身再留 20dp。

新增文案：`home_day_today`、`home_today_summary`、`home_view_detail`、`home_summary_duration`、`home_empty_timer_hint`。仅中文 `values/`，英文仍走回退。

### 全站材质统一（2026-11-08 第三轮）

首页那套三层材质已铺到全 app，旧的磨砂玻璃卡**整体移除**（不是逐页改）：

- `ui/theme/CeramicSurface.kt`（新增，替代已删除的 `LiquidGlassCard.kt`）：
  - `Modifier.appMeshBackground()` —— 唯一的页面背景，在 `GymLogApp` 根节点画一次；淡蓝灰底 + 四角色晕 + 顶部天光。
  - `Modifier.ceramicSurface(shape)` —— 唯一的卡片材质，不透明陶瓷 + 釉面高光 + 1dp 浅灰圈边 + 5dp 内收投影；深浅色两套色值。
  - `LiquidGlassCard(...)` 名字保留（避免改 4 处调用点），渲染色已换成陶瓷。
  - `LiquidGlassColors.ink / inkMid / inkSoft / divider` 改成 `@Composable` getter，跟随深浅色；原来是一组写死的深色值，深色模式下会取到深色文字。
- `GymLogCard` 内部改调 `ceramicSurface()`，所以 60 处卡片调用点一行没动就全换了材质。
- `Theme.kt`：`background` 和 `surface` 设为 `Color.Transparent`，Scaffold 才不会把 mesh 盖住；`PageBackground` 的值改成 `#EDF2FC` 只作取色参考。**深色/浅色都一样**，新增页面不要再用 `colorScheme.background` 画不透明底。
- `GymLogApp`：根节点 `Box(...appMeshBackground())`；子页面遮罩 Box 也用 `appMeshBackground()`（原来是 `colorScheme.background`），否则透明后能看见下面的 tab 页。
- 训练「正在训练」迷你条（`ActiveWorkoutMiniBar`）从深色渐变改成白色陶瓷卡，跟随新材质。
- 删除的东西：`BackdropBlobs`、`GlassBackdropBlobs`、`glassSurface`，以及每张卡重绘模糊背景的实现（滚动时不再逐帧做高斯模糊）。

### 深色 hero 卡清理（2026-11-08 第四轮）

上一轮只换了 `GymLogCard` 覆盖不到的四个手写卡片，这次一并收掉：

- 训练保存页「本次训练」、身体记录页「当前身体」、计时页顶栏、已保存计时页顶栏：从 `#060B14 → #11263F → #234A73` 横渐变 + `#BBD0EC` 文字改成 `ceramicSurface` + 主题色（标签 `onSurfaceVariant`、数字 `onSurface`、分隔线 `outlineVariant`）。**代码里已无 `060B14/11263F/234A73/BBD0EC` 四个色值**。
- 身体记录卡的指标图标从 32dp 收到 24dp（实心图标在白卡上太大像一坨蓝），改 `primary`；变化提示色压深到 `#1F7A4D` / `#B2452F` / `#6B7688`（原来的浅绿/浅橙在白底只有 1.5 左右对比度）。
- 日历卡（`TrainingCalendarCard`）、身体图卡（`BodyFigureCard`）：M3 裸 `Card` → `ceramicSurface`，不再用 `dashboardCardColors()`。

对比度修复：
- 我的页性别图标 `#FFA9C8`/`#BBD0EC`（1.79/1.57）→ `#C2477A`/`primary`。
- 日历月视图星期标签 `#9EB5D0`（2.11）→ `onSurfaceVariant`；首页足迹的星期标签同步从 `inkTertiary` 提到 `inkSecondary`。

切换器统一成「浅蓝底 + 品牌蓝字」（和底栏选中态同一口径，深浅色都成立）：统计页「最近一周/最近一月」、我的页「时长/训练量/训练天数」。我的页柱状图（`MineWeeklyChartCard`）外面补了陶瓷卡，不再直接画在页面背景上。

顺手删掉 `GymLogApp.HomeTopBar`（早已无调用）。

仍然保留原样：日历的特殊日颜色来自用户自定义调色板，饱和度不受新材质影响。

已知未处理：月视图下三项统计的标签仍是「本周训练」（历史文案，`home_stat_days`），没跟周/月切换联动。

背景改深后重新测过对比度（实测像素，底色在 `#E5EDFC`–`#EAF1FA` 之间）：主文字 `#202B40` 12.0–12.5:1（白卡上 14.2），次级 `#7D8798` 约 3.1:1，辅助 `#A0A8B5` 约 2.1:1，品牌蓝 `#4380F5` 约 3.2:1。次级/辅助/品牌蓝都不到 WCAG AA（4.5:1，正文尺寸），这是原配色本身的取舍（"标签弱化"）。直接压在页面背景上的两处已经各提一档：顶栏副标题用次级色，日期标题行总时长的前缀用次级色、数值用主文字色。卡片内的弱化标签维持原样，要整体达 AA 需要把 `inkSecondary` / `inkTertiary` / 品牌蓝文字一并压深（约 `#6B7688` / `#8B95A5` / `#2F6BE0`），那是配色层面的决定，改之前先跟用户确认。

「我的」里的三个数可以分别切换，状态只在当次组合里：训练天数 / 训练年限，累计时长的自动单位 / 固定分钟，体脂率 / 体重。训练天数在这里不是「练过几天」，而是从第一次有组的训练日到今天的跨度（含两端）。年限是这个跨度除以 365、向上取到一位小数。时长不到 1 分钟显示秒，不到 1 小时显示分钟，否则显示一位小数的小时。



## 身体数据



「我的」入口文案是「身体数据」（`mine_body_title`）。展示页在下。底部「修改」进资料表单（生日、性别、身高、体重、体脂）。「我的」里的资料编辑（用户名/头像）也可改性别，经 `saveIdentity`。人体图上的围度另有编辑页：胸围、腰围、臀围，以及左右臂围、小臂、腿围、小腿。单侧旧臂围 `armCm` 已在 16→17 删除，不再提供输入，也不在历史记录里显示；腰围只在围度页维护。保存其他资料时，表单会原样保留已有的 `waistCm`，不清空。保存后回到展示，不直接回设置或首页。系统返回与顶栏一致：编辑中的表单或围度页先回展示，历史列表也先回展示，展示再回打开它的那个根页面。



身体记录历史复用 `BodyRecordHistory`，显示同一份 `body_measurements`；从身体数据展示页或编辑器里的「身体记录」卡进入，在本页切换，返回回到身体数据（设置侧入口和 `Screen.BodyHistory` 全屏路由已删）。历史卡片默认折叠，只显示记录时间；点卡片展开身高、体重、围度、BMI、体脂，缩起后不渲染详情。



展示页上的人体图可在「身体数据」和「训练肌群」之间切换：「身体数据」画围度，「训练肌群」画当前选中日的肌群高亮。也可以从「训练肌群图」入口进入。图按已保存的训练会话计算，可切当天、本周（周一起）、本月，以及只看主要或次要。宽屏下「身体数据」仍只显示单张正面图，围度标注和人体图共用同一个 `MuscleFigureGeometry` frame，不能各算一套矩形。返回回到身体数据展示页。



字段还有年龄和 BMI。BMI 由身高体重现算。围度没有单独趋势图。



## 照片墙



入口在「我的」。网格按日期浏览，可按月筛选；相册或相机添加（相机经 `FileProvider`，路径见 `@xml/photo_file_paths`）。详情可改日期、备注、删除；编辑支持裁剪、旋转、涂鸦、文字、马赛克、模糊（`PhotoStore` + `PhotoWallRepository`）。



- **不进** `BackupCodec` / 导出 JSON。数据备份页有说明（`photo_wall_backup_note`）。

- 导入训练备份、清除本地数据**都不会**删除 `photo_wall` 行或 `filesDir/photo_wall` 下的文件。

- 相对路径必须安全；展示优先 edited，否则 original。不要把用户照片扫进日志或交接输出。



### 编辑器代码结构（2026-10-25 重构，对外接口与行为不变）



`ui/photo/` 下的职责划分（本次只做结构拆分，公开签名和交互行为与重构前完全一致，未新增任何功能）：



- `PhotoWallViewModel.kt`：编排 + 状态；公开方法签名未动（GymLogApp.kt 零改动）。

- `PhotoWallScreen.kt`：照片墙网格 + `PhotoDetailScreen` 详情页。

- `PhotoEditorScreen.kt`：编辑器页面编排（顶栏、工具卡、日期卡、备注框、离开确认）。

- `PhotoEditorCanvas.kt`：画布组件，独占手势路由（点放文字 / 自由涂鸦 / 拖框裁剪·马赛克·模糊）与拖拽预览；回调收位图像素坐标。

- `PhotoCanvasMath.kt`：视图坐标 ↔ 位图像素的纯函数换算（`imagePoint` / `bitmapRect`，含 Fit letterbox 补偿），有单测。

- `PhotoDialogs.kt`：详情页与编辑器共用的 `NoteDialog` / `PhotoDateDialog` 及 epoch↔UTC 转换（internal，同包可见）。

- `BitmapHistory.kt`：撤销/重做全尺寸位图栈（默认 5 步，超出回收最旧的，push 清空 redo 分支），有单测；ViewModel 不再自己持有 undo/redo 双端队列。



重构动机：原 `PhotoEditorScreen.kt` 单文件 510 行，混放页面编排、手势、坐标数学和两个共用对话框；ViewModel 内联管理位图栈。重构后单文件最大约 380 行，纯逻辑可单测。后续若要做编辑功能增强（比例裁剪、调色、笔刷涂抹等），落点分别是 `PhotoEditorCanvas`（手势）、`PhotoCanvasMath`（换算）、`PhotoImage`（像素操作，本次未动）、`BitmapHistory`（历史）。



## 动作库



排序：练过的不同训练次数多的在前，然后更近的在前，然后名字。展示名是「部位-动作名」。自定义动作没有专属图时按部位用后备图。选择动作（`pickerMode = true`）不显示删除。



浏览：左侧部位，上方器械。名称搜索匹配中文名、英文名和别名，并保留当前部位/器械筛选；搜索不会把筛选清空。`ExerciseCatalogQuery.refinement` 留给以后的二级器械，现在没有对应字段，不能靠动作名猜测去缩小结果。



列表只写肌群名称；点进详情再画人体图。自定义动作可在新增和详情里编辑主要、次要肌群，同一块肌肉不能同时两种角色。没有映射就显示「未配置肌群」，不要猜。



数据集媒体已按项目方确认的授权打包；媒体版权归 Gym visual，必须保留 `© Gym visual — https://gymvisual.com/` 署名和 180×180 使用限制，并遵守 Gym visual Terms。授权记录在 `tools/exercise_audit/PHASE_F_AUTHORIZATION.md`，`assets/exercises/NOTICE.txt` 保留说明。`exercise_art_credit` 现在同时说明 Gym visual 缩略图/GIF 和 Bryl Lim / Everkinetic 备用示意图，不能删除。



## 正在训练通知（前台服务）



新建训练草稿开始计时后，系统通知栏挂一条**静音、低重要性**的持续通知，App 退到后台仍然在；点通知直接回到同一份草稿。



- 服务与状态：`ActiveWorkoutNotifier`（唯一出口，持有状态）+ `WorkoutNotificationService`（前台服务，只负责让通知活着）+ `WorkoutNotificationReceiver`（只接收「用户划掉通知」）。

- **通知不是第二套计时**。状态只存「发布瞬间的累计时长 + 单调时钟」，运行中交给系统 chronometer 走；暂停 / 改时长 / 进入保存页时重新投递并重置锚点。`SessionEditorUiState.totalTimerRunning` 是为此新增的只读字段，不改变 `totalElapsed()` 语义。

- 三种状态：`RUNNING`（标题「正在训练」，正文显示当前动作，秒数在标题右侧走）、`PAUSED`（编辑时长弹窗期间，正文写死「已暂停 · 已练 x分y秒」）、`FINISHING`（点「完成」进入保存页，标题「训练待保存」）。

- 只有 `sessionId == null` 的新草稿才挂通知；从日历/首页编辑已保存记录不会出现通知。

- **独立计时器同步（组歇 / 秒表）**：通知正文第二行显示独立计时器真实状态，与训练总计时**各归各**，绝不合并成一套时间。

  - 数据源：`GymLogApp` 已在用的同一份 `SessionEditorViewModel.clock`（`activeEditorClock`），ViewModel 不新增任何字段，计时逻辑一行未改。

  - 状态只存「快照显示值 + 单调时钟锚点」（`WorkoutNotificationClock`），渲染用与 `tickClock()` 相同的公式还原：倒计时 `base - (now - anchor)`、正计时 `base + (now - anchor)`。暂停/重置/±30s/切模式会推新锚点，所以它不是第二套计时。

  - 文案：`组歇 · 01:53`、`组歇 · 01:53 · 已暂停`、`组歇 · 00:00 · 已结束`、`秒表 · 00:42`、`秒表 · 00:42 · 已暂停`。**未启动 / 已重置整行不显示**，避免给出虚构的运行时间；进入保存页也不显示。

  - 判定依据是 `WorkoutClockUi.started`，**不再靠「显示值是否等于初始值」反推**——否则「开始后 100ms 内暂停」和「暂停时恰好停在初始时长」都会被误判成未启动。`started` 只是 UI 状态位，不参与任何计时计算：切模式 / 重置 / `setTimerDuration` 归 false，`toggleClock` 开跑置 true，暂停 / 继续 / ±30s 全部保留原值（走 `copy()`）；不进草稿快照、不进 Room、不进备份。

  - 测试：`WorkoutNotificationClockTest`（19 例，含「开始后立即暂停仍是已暂停」「暂停在初始值仍是已暂停」「重置后加时仍是未启动」「Chronometer 基准只跟单调时钟、不按刷新次数累加」）与 `WorkoutNotificationPostPolicyTest`（6 例，划掉后不被每秒刷新弹回）。

  - **渲染方式（1.5.3 修复，重要）**：整条通知改成自绘内容区`notification_workout_content.xml` + `DecoratedCustomViewStyle`，其中**两块时间都是系统 `Chronometer`**——标题右侧是训练总时长，第二行是独立计时器。两种模式都向上走（`countDown=false`、`base = now - 已走过`，倒计时的「已走过」= 设定时长 − 剩余）；暂停 / 到点改成静态文本（`chronometerSpec()` 返回 null）。

  - **为什么不再用「App 每秒 notify 一次文本」**：Android 15/16（尤其国产 ROM）在后台会冻结/限流普通进程，一旦 `delay(1000)` 的刷新协程停住，写死的文本就永久冻结，而系统 Chronometer 仍在走——表现就是「总时长在走、组歇/秒表停住、回前台时间又是对的」。改成系统渲染后，刷新任务被冻结也不影响数字，顺带把后台每秒一次的通知投递降为零（实测 15 秒 0 次投递，数字照走）。

  - 服务只保留两件事：状态变化时重投一次；倒计时运行时排一个**一次性**「到点」动作（`scheduleCountdownEnd`，到点后把文案换成「已结束」）。秒表、暂停、保存页都不排任务。

  - **倒计时改成向上走（1.5.3 定稿）**：`组歇 · 已歇 00:35 / 02:00`，不再是「倒数到 0」。

    原因是在真机（OnePlus PJZ110 / Android 16）上实测：切后台 2 秒就被 `OplusHansManager` 冻结整个进程，

    `OplusAlarmAdjustment` 把本应用所有闹钟（含 `setAlarmClock`）推迟 `+2d23h59m`，服务持 `PARTIAL_WAKE_LOCK` 也拦不住，

    协程 `delay` 用的 `uptimeMillis` 在深睡期间同样不走。也就是说「到点那一刻由 App 去改通知」这条路在本机物理上走不通，

    而向下的 `Chronometer` 越过零点没有下限，会一路走成 `-00:46`。

    改成向上走之后：数字只增不减，任何时刻都不会为负；也不需要零点唤醒，服务里不再有任何定时任务、闹钟或唤醒锁。

    超过目标时显示成 `01:04 / 00:30`，一眼能看出「已经超时」，但不会再出现负数。

  - 两种模式都走同一个绑定：`countDown=false`、`base = now - 已走过`。倒计时的「已走过」= 设定时长 − 剩余，

    与 `tickClock()` 用的是同一个锚点（`countdownElapsedTracksTheSameAnchorAsRemaining` 守住）。

  - 折叠态/无障碍文案用固定串（`组歇 · 已歇中 / 02:00`），**不含秒数**，所以 `contentKey` 不会每秒变化，

    也就不会出现「每秒重投 → Chronometer 视图重建 → 卡一秒再跳」。

  - 暂停 / 到点仍是静态文本：`已歇 00:35 / 02:00 · 已暂停`、`已歇 02:00 / 02:00 · 已到点`。

  - 去重分两层：`signature`（结构变化，会清「用户划掉」标记）与 `contentKey`（含渲染文本，只用于避免重复投递）。**计时器每秒文本变化不能清划掉标记**，否则用户划掉后会被每秒弹回来。

  - 独立计时器本来就不进草稿快照，所以进程被杀后它随 App 一起归零，通知恢复时也不会凭空造出一行时间。

- 清理只发生在 `activeDraft` 被清空时（保存成功、舍弃、删除）。保存失败（含照片保存失败）不会清理，草稿与通知都留着。

- 前台服务类型用 `specialUse`（`FOREGROUND_SERVICE_SPECIAL_USE` + `PROPERTY_SPECIAL_USE_FGS_SUBTYPE`）。**不用 `health`**：它要求 `ACTIVITY_RECOGNITION` / `BODY_SENSORS` / `HIGH_SAMPLING_RATE_SENSORS` 之一，而本 App 不读任何传感器。

- 渠道 `workout_active`（IMPORTANCE_LOW，无声音、无震动、无角标），通知 id `9420`；与独立计时器的 `timer_finished` / `9401` 完全隔离，互不覆盖。

- 进程被系统回收：通知会消失。重开 App 后按 `SessionDraftStore` 快照复原草稿并重建通知，时长用 `savedAtEpochMillis` 补齐。`START_STICKY` 只是尽力重启，不作为保证。

- 用户划掉通知（Android 14+）：不立即重发，等下一次真实状态变化（暂停/继续/换动作/进保存页）再显示。Android 13 上 `setOngoing(true)` 使其不可划掉。

- 通知权限被拒（Android 13+）：服务照常运行，但抽屉里看不到通知，只在「前台服务任务管理器」可见。训练中授予权限后，`MainActivity.onResume` 会调 `ActiveWorkoutNotifier.refresh()` 补投一次（用户主动划掉的不补）。

- **服务必须满足 5 秒契约**：只要是通过 `startForegroundService()` 拉起的，`onStartCommand` 里无论有没有有效训练，都必须在 5 秒内调用 `startForeground()`。「没有有效训练 → 直接 `stopSelf()`」会让系统抛 `ForegroundServiceDidNotStartInTimeException` 杀掉进程，所以走 `fulfillForegroundStart()`：先挂一个占位通知，立刻 `STOP_FOREGROUND_REMOVE` 撤下。



## 外观与日历配色（1.5.3）



- 设置页「偏好设置」中的外观改为普通入口，进入独立 `AppearanceSettingsScreen`：主题、使用壁纸配色、日历配色。

- `CalendarColorStore` 用 SharedPreferences `gymlog_calendar_colors` 保存模式和六部位自定义色，默认 `VIVID`；不进入备份 JSON。

- 三种模式：

  - `VIVID`：同色相最亮版本 `FF8A80 / 82B1FF / FFD740 / B9F6CA / EA80FC / CFD8DC`。

  - `SOFT`：当前熟悉配色 `E57373 / 64B5F6 / FFB74D / 81C784 / BA68C8 / 90A4AE`。

  - `CUSTOM`：六部位分别保存 ARGB；自定义页提供色相/饱和度圆盘、明度滑条、色值和恢复默认。

  - 自定义色号支持 `#RRGGBB` 或 `RRGGBB`；非法输入显示红色错误提示并禁用「应用颜色」。

- 选择「自定义」立即进入 `CalendarCustomColorsScreen`；颜色只影响日历/训练足迹的部位色，休息/放纵/偷懒固定色不变。

- 日期数字按背景 `luminance` 自动切换深色/白色，避免自定义深色看不清。

- 调色板通过 `LocalCalendarColorPalette` 从 `MainActivity` 提供，`CalendarMonthOverview`、`TrainingCalendarCard`、`TrainingCalendarDetailScreen` 和 `CalendarColorLegend` 共用；不再直接读枚举默认色。

- 测试：65 类 / 308 测试通过；模拟器验证鲜明（最亮）与柔和（当前）切换实际应用到日历；非法色号 `#GG0000` 显示错误且应用按钮禁用，合法 `FF8A80` 恢复可用。Debug APK：`GymLog-1.5.3-notification-settings.apk`，167,246,892 B，SHA-256 `DDD3913CF910F00081A25A5A645DC0E7D4ED5F4F9AAA99BF54DCE3DDCF98D6CC`。



## 通知设置（1.5.3）



- 设置页顶部大类改为「偏好设置」，包含外观卡和「通知」入口；外观卡内补「外观」小标题，不新增二级页。

- 设置页原「数据备份」改为「数据管理」；清除本地数据从设置首页移入数据管理页，仍保留原来的两步确认和清除逻辑。

- 设置页新增「通知」入口，进入独立 `NotificationSettingsScreen`；页面含「定时器到点通知」开关、系统通知权限状态和打开系统设置入口。

- 偏好存放在 `NotificationSettingsStore` 的 SharedPreferences `gymlog_notifications`，键 `countdown_alerts_enabled`，默认 **开启**；退出页面/重启应用保留。该偏好不属于备份 JSON，也不会进入 Room。

- 开关只控制独立倒计时的到点提醒，不影响训练总时长、进行中训练通知、训练保存或独立训练计时器。

- 开启：

  - 新的倒计时正常排 `setAlarmClock` + `setExactAndAllowWhileIdle` 双闹钟。

  - 如果倒计时正在运行，按当前剩余时间重新安排，不重新开始。

- 关闭：

  - 取消尚未触发的主/备闹钟。

  - 清除已经投递的 `timer_finished` 通知和震动。

  - 新的倒计时不再安排到点提醒。

  - `TimerAlarms.alert()` 和 `TimerAlarmReceiver` 还会再次检查偏好，拦截已排队的旧 PendingIntent。

- 自然归零仍保留系统闹钟，由 AlarmManager 负责后台触发；不能因为前台 `clockFinished` 收集器未运行就提前取消。

- 主/备闹钟共享一次性 token，Receiver 只在同 token 的 3 秒内去重；新倒计时有新 token，不会被旧去重记录误伤。

- **提前到点（1.5.3 修复）**：备用闹钟 `setExactAndAllowWhileIdle` 在 ColorOS 上会被按批处理窗口**提前**投递，实测 03:00 的组歇在 02:31 就弹了「计时结束」——因为 `TimerAlarmReceiver` 原来是「收到即提醒」，没有任何到点校验。

  - 修法：排闹钟时把目标时刻记进 Intent（`EXTRA_ALERT_AT`，`SystemClock.elapsedRealtime()` 基准，与倒计时同一时钟），接收器先用 `TimerAlertGate.evaluate(at, now, retried)` 复核：

    - 明显提前（剩余 > 1 秒容差且未补排过）→ **不提醒**，按真实剩余时间补排一次（`retried=true`）；

    - 已到点 / 容差内 / 晚到 / 补排后仍提前 / 没有目标时刻（老 PendingIntent）→ 照常提醒，绝不吞掉。

  - 补排走同一个 `scheduleCountdown`，会先 `cancel()` 掉原来的主/备闹钟，所以不会与真正的到点重复。

  - `TimerAlertGateTest` 6 个用例锁死这套判定（提前/准点/容差内/晚到/二次提前/缺目标时刻）。

- 进程重建时 `GymLogApplication.onCreate` 会取消上一进程遗留的独立计时闹钟；独立倒计时本来就不跨进程恢复。

- 打开开关时弹窗提示检查通知权限、应用耗电管理、睡眠待机优化；不自动请求权限，不声称已检查/修复厂商设置。

- 「检查系统设置」优先打开 `ACTION_APP_NOTIFICATION_SETTINGS`，失败回退 `ACTION_APPLICATION_DETAILS_SETTINGS`；两者都失败时显示手动提示，不崩溃。Android 没有可靠的通用厂商耗电管理入口，因此不伪造跳转。

- 系统通知权限状态用 `NotificationManagerCompat.areNotificationsEnabled()` 读取；它不能代表精确闹钟能力或厂商后台限制。

- 验证：65 类 / 308 测试通过；模拟器验证关闭后新倒计时无闹钟、运行中关闭取消闹钟、运行中重新开启按剩余 13 秒恢复并在到点发出通知、开启引导弹窗和系统通知设置跳转。Debug APK：`GymLog-1.5.3-notification-settings.apk`，167,246,892 B，SHA-256 `DDD3913CF910F00081A25A5A645DC0E7D4ED5F4F9AAA99BF54DCE3DDCF98D6CC`。

- 修改前备份：`C:\Users\75192\AndroidStudioProjects\GymLog-backups\notification-settings-20261027-225749`（含 `HEAD.txt`、`status.txt`、`working.diff`、`untracked.txt` 和工作区源码副本）。

- 一加 ColorOS 若把「应用耗电管理」限制后台，仍会把系统闹钟整体推迟；应用只能提示用户检查，不能绕过。



## 计时器



训练页「训练计时器」可以开始、暂停、继续。计次记的是从上一个计次点到现在的间隔，以及当时的累计时间。可以直接改时、分、秒：分和秒必须在 0–59，时不超过 999。改完如果还在跑，起点改成现在的 `elapsedRealtime`，累计改成新值。计时器页当前只有当前计时和计次；设置 → 历史记录不再提供计时入口，已保存计时从日历当天记录进入。



操作栏四个控件在同一行：开始/暂停/继续（播放/暂停图标）、计次（旗帜图标）、重置（刷新图标）、训练结束（文字按钮）。已保存记录从 `TimerHistoryScreen` 打开 `SavedTimerScreen`，返回时回计时记录列表。



保存时写入 `timer_records` / `timer_laps`，日期用开始计时时记下的 `recordDateEpochDay`。当前版本只记日期，不再关联具体训练；`timer_records.sessionId` 保留给旧记录和备份兼容，新记录写 null。删除已保存计时只删这两张表。



## 训练记录重构（工作区进度）



从「训练」页的「开始训练」进入 `Screen.SessionEditor(sessionId = null, returnTab = Train)`，新建草稿的 ViewModel 初始化时立即启动训练总计时。顶部居中显示总时长，右侧是独立计时器图标和最右侧「完成」按钮。点击总时长打开时/分/秒滚轮，小时 00–23；编辑期间总计时暂停，确认后从新时间继续。训练保存时把总时长写入 `timer_records`（id/sessionId = 训练 id），日历不再提供手动训练计时器；旧计时记录保留并继续计入当天总时长。



- 总计时使用 `SystemClock.elapsedRealtime()` 的起止时间差，不依赖 Compose 重组次数；ViewModel 在旋转、打开动作选择器、打开独立计时器时都不重建。应用短暂进入后台不会把后台时间丢掉；进程被杀后的恢复尚未实现。

- 独立计时器/秒表也在 `SessionEditorViewModel` 中，使用单调时钟；打开/关闭底部面板不会重置。训练总计时与独立计时器的状态完全分开。定时器默认 120 秒，±30 秒，归零停止并通过 `VIBRATE` 触发一次震动；秒表可开始/暂停/重置。计时器 BottomSheet 使用 `skipPartiallyExpanded = true`，并加 `verticalScroll` + `navigationBarsPadding`，打开即完整显示，不需要手动上拖。点击定时器圆形时间打开分/秒滚轮（上限 59:59）；编辑时暂停，确认后保持暂停。

- 动作组 `DraftSetUi.completed` 初始 false；勾选后组背景变绿，头像/总容量/组数只统计已完成组。完成状态由 UI 直接重算，不做增量累加。

- 保存时只把已完成组写入 `workout_sets`；未完成组不落库。新组继承上一组的重量/次数，完成状态不继承；已有训练加载时视为已完成，避免改变历史容量。

- 「完成」进入同一路由内的保存页：顶部统计横向显示时长、总容量、组数、部位；照片选择器把图片经 `PhotoWallRepository` 私有存储保存到照片墙（当前不建立 sessionId 外键，照片墙仍不进训练备份）；备注、日期可编辑；保存失败保留页面和照片，删除/舍弃需要确认。

- 点击「完成动作」会记录该动作从加入训练到结束的用时（同一动作再次完成后覆盖），并收起卡片；动作时间独立、可重叠，不进入当天总时长。动作用时展示在动作卡和历史训练详情。

- 新增 `durationMillis` 字段，迁移 17→18，仅 additive。旧会话默认 0。

- 训练页顶部统计固定为总容量、动作数、组数；动作数统计已添加动作，容量/组数只统计已完成组。

- 组编辑为横向一行，表头为「组 / 负重模式 / 重量 / 次数 / 完成」：组号用普通数字 1、2、3，负重模式点开下拉选择，重量和次数各自上 + 下 −（2.5kg / 1次），右侧完成勾选。重量/次数格是只读数字展示，不使用可横向滚动的输入框，避免窄屏数字拖动隐藏；数值只能通过上下按钮调整。完成整行绿色，新增组继承上一组重量/次数但不继承完成状态；长按完成勾可删除该组（可删除最后一组）。

- 每个动作卡底部「添加一组」右侧是「完成动作」：只把该动作卡收起为类似日历训练记录的摘要小卡，不保存、不结束计时、不丢弃数据；点击摘要卡展开。顶部「完成」才进入保存页。折叠 ID 存在 SessionEditorViewModel 的 `collapsedExerciseIds`，去选动作再回来仍保持折叠；只有点击卡片才展开。

- 卡片底部操作行是「添加一组 ｜ 居中向上箭头 ｜ 完成动作」：箭头（`KeyboardArrowUp`，`training_collapse_exercise`）手动收起卡片，走的是和「点击摘要卡」同一个 `toggleExerciseCollapsed`；收起后的摘要卡右端有一个 18dp 的向下箭头（`KeyboardArrowDown`，纯装饰、无 contentDescription），提示点卡片可以再展开。

- 动作卡头三行依次是部位、动作名、容量；训练页/保存页的统计名加粗、数值不加粗。

- 选择自重模式时，重量栏显示当前日期可用的身体记录体重并禁用加减；没有体重记录时弹出设置体重的对话框，保存后自动带出。体重只读。

- 从训练页退出编辑器会保留 `activeDraft`，训练页底部显示缩小卡片；其他根页面不显示。点击卡片回到同一个 `SessionEditor`，总计时继续；保存/删除后清掉 activeDraft。当前 activeDraft 只保存在内存，进程被杀后不会恢复。

- 当前保存仍然只写已完成组；未完成组不会落库（审计已标为待确认设计，不要静默改变规则）。



## 动作媒体与训练记录布局



数据集媒体随 APK 发布在 `assets/exercises/media/images|videos`，1,324 张 JPG + 1,324 个 GIF，按 `ds_<datasetId>` 稳定关联。`ExerciseMediaRepository` 优先读取 `filesDir/exercise_media` 下已获许可的本地覆盖文件，否则从 assets 直接读取；没有文件/解码失败时缩略图回退 `ExerciseDiagram`，动作演示显示“暂无动作演示”。缩略图在 IO 线程解码并用 LruCache 缓存；GIF 用 `Movie` 在演示页可见时循环，切页/关闭后协程结束，不在后台播放。不做首次全量复制，避免额外 137MB 磁盘占用。



动作详情默认“动作演示”，固定三个选项：动作演示、正面、反面。`MuscleFigure(forcedFront = ...)` 用于固定正反面，原有肌群缓存/高亮不变。



训练记录按每个动作一行三栏：左为部位/名称/容量，中为缩略图，右为 2 列 × 3 行组数据。实际显示每组“重量 × 次数”；1–5 组原样，6 组显示第 6 组，超过 6 组显示前 5 组 + 省略号；数据库中的全部组仍保留，详情/编辑可看。

历史/日历页那三栏的宽度跟会话编辑页（`CollapsedExerciseCard`）对齐：缩略图 48dp + 左右各 8dp，组数区固定 124dp，名字列 `weight(1f)`。360dp 宽的手机（如 1440×3168/密度 640）上卡片内可用 312dp，名字列能分到 124dp 约 7 个汉字，长名字换行而不是被挤成一行两个字。改宽度前先按 `312 - 64 - 124` 算一遍，别再把名字列挤没。



## 备份与清除



`BackupCodec`：`format = gymlog`，`VERSION = 2`。JSON 含 exercises、sessions、sets、profile、measurements、legacyRecords、dayNotes、**dayKinds**、exerciseMuscles、timerRecords、timerLaps、**exerciseDurations**，以及头像 JPEG。**不含**照片墙。旧备份 measurement 里的 `armCm` 会被忽略，新导出不再写它。session 的 `durationMillis` 和 `exerciseDurations` 是可选字段，旧文件缺省为 0/空。`dayNotes`、`dayKinds`、`exerciseMuscles` 用 `optJSONArray`，没有这个键的旧文件当成空列表。围度、性别、用户名同样缺键则为空。动作的可选 `nameEn` / `source` / `sourceId` / `equipmentDetail` / `bodyPartDetail` 只在有值时写入；旧备份缺这些键时 `source` 回退为 `LEGACY`。分类字段 `bodyCategoryId` / `equipmentCategoryId` / `classificationStatus` / `classificationVersion` 也是可选键，旧备份缺省为 `UNMIGRATED` / 0，恢复后由分类迁移补齐。内容、媒体、来源映射和原始肌群词元不进备份，恢复后由导入器按本地数据集补种；自定义动作和其肌群映射仍按原规则恢复。



设置里导入/导出的界面现在是 **设置 → 数据 → 数据备份**（`BackupDataScreen`），不再直接放在设置首页。确认对话框和失败提示都在这个子页面内。



version 2 必须带 `timerRecords`、`timerLaps` 和 `avatarJpeg`（可以是 JSON null，表示没有头像）。version 1 没有这些键：计时保持 `null`，头像是 `Unspecified`。导入时 `null` 计时不会清掉库里已有的计时；`Unspecified` 不会动磁盘上的头像文件。不要把这两件事改成「旧文件也要清空」。



头像是 Base64 JPEG。校验：2MB 以内，宽高 1–4096，文件头是 JPEG。裁剪输出 512×512。文件只存在 `filesDir/profile/avatar.jpg`。



导入前 `validate()`：重复 id、组的外键、重复肌群关系、未知肌群、指向不存在的动作、非法性别、重复或非法日类型、计时和计次对不上，必须先失败，失败则不替换。



导入整库替换会清掉并重写 `day_kinds`（快照里没有则为空）。照片墙仍不受导入影响。



导出时，库里的自定义映射会写进 JSON；当前库里存在的预设动作，其映射也从代码写入 JSON。导入时只把 `isCustom` 动作的映射写回 `exercise_muscles`，预设继续以代码为准。



导入是整库替换，不是合并。确认框文案用 `data_import_confirm_message`。取消或文件读不了，当前数据不动。成功写入后再补数据集种子、重算 PR、`dataEpoch++`；不再补旧预设。如果补种或头像写入失败，会在一个事务中恢复旧快照**以及完整的数据集表**（内容、媒体、来源映射、原始词元、匹配候选、导入状态和 `exercise_muscles` 存储关联），再恢复旧头像；失败不会被吞掉。文件走系统选择器，没有存储权限。建议文件名 `gymlog-yyyy-MM-dd.json`。读取上限 20MB。



外观存在 SharedPreferences `gymlog_appearance`（主题、是否用壁纸配色），不进备份。进行中的秒表在 `gymlog_timer`，也不进备份。Android 12 以下没有壁纸配色开关。



清除本地数据：设置里两步确认，然后在协调器锁内 `clearAll`、补数据集种子、重算 PR、`dataEpoch++`；不再补旧预设。清除前会保留旧快照和完整数据集状态；补种/重算失败时按同一恢复路径还原，再向调用方抛出，不再静默报告成功。清表顺序是组、会话、肌群映射、动作、动作内容/媒体/来源映射/原始肌群词元、匹配候选、导入状态、身体记录、资料、遗留记录、日期备注、**日类型**、计次、计时记录。**不清** `photo_wall`。自定义动作和它们的肌群映射不会回来。已保存计时会删掉。头像文件、照片墙文件和进行中的秒表偏好不会被这条路径删掉；资料行没了，界面就不再引用那张头像 jpg。



## 外观与图标



`MainActivity` 读 `AppearanceStore`：跟随系统 / 浅色 / 深色，以及是否 `dynamicColor`。关掉壁纸配色后使用 A 蓝品牌色（`#2563EB`），日历部位色更稳。卡片样式集中在 `ui/theme/DashboardCards.kt` 和 `ui/theme/GymLogComponents.kt`。



启动器名中文「练有所迹」、英文 Traivol。自适应图标在 `drawable/ic_launcher_foreground.xml` 与 `ic_launcher_background.xml`，`mipmap-anydpi-v26` 引用它们，`mipmap-anydpi` 是 API 24–25 的 layer-list。单色层只用前景的透明度。用户换过好几版图标；桌面若还是旧图，是启动器缓存，需要卸掉再装。



## 进程死亡草稿快照（JSON）



- `SessionDraftStore` 位于 `app/src/main/java/com/example/gymlog/data/SessionDraftStore.kt`，写入 `filesDir/session_draft.json`，`schemaVersion=1`。

- 快照包含：draftKey、sessionId、dateEpochDay、note、totalElapsedMillis、timerRunning、savedAtEpochMillis、finishing、finishedDurationMillis、photoId，以及动作/组的 id、负重模式、重量文本、次数、完成状态、动作计时。

- 写入采用临时文件 + `fsync` + rename；每 350ms 防抖，并在训练中每 5 秒检查点；保存/舍弃/删除时清空。不是 Room 表，不进备份格式。

- 恢复：Activity SavedState 恢复 activeDraft/screen；ViewModel 冷启动读取快照，通过 `WorkoutRepository.findExercise` 重新取动作，按 `savedAtEpochMillis` 补齐进程死亡期间的计时；照片只保存 photoId，再异步解码。

- 实际进程测试：创建动作、2 组、40kg×10 完成，快照落盘；`adb shell am kill` 杀进程后重新启动，仍进入训练编辑页，容量 400kg、动作/组/完成状态保留，计时从约 10 秒恢复为 26 秒；保存后快照文件删除。

- 该轮 APK 大小 166,335,409 B，版本 1.5 / versionCode 10（历史记录；当前版本见文末 1.5.2）。

- 未修改 Room schema、DAO 表、数据库迁移、备份 JSON 或训练保存规则。



## 保存训练页视觉重构



- 顶部摘要改为深蓝渐变「本次训练」卡：日期右上、时长主信息、分割线、总容量/组数/部位三列。

- 照片区压缩为 108dp 虚线感浅色选择区，含图标、添加文案和提示；已有照片仍显示 160dp 预览和更换/移除。

- 备注区增加「备注 / 选填」标题，输入框使用 16dp 圆角，提示「今天练得怎么样？」和更具体的占位文案。

- 日期改为白卡行：日历图标、训练日期、日期值、右箭头，点击逻辑不变。

- 保存按钮文案改为「保存训练」；取消锻炼保留。

- 增加 `training_save_action`、`training_save_optional`、`training_photo_section`、`training_photo_hint`、`training_note_prompt`、`training_note_hint` 字符串。

- 保存/照片/日期/备注逻辑未改；后续已在 1.5.1/1.5.2 构建中编译验证。



## 动作完成后收起卡变绿



- 动作卡点击「完成动作」后，`completedAtMillis` 写入，卡片收起并整卡使用与单组完成一致的绿色反馈：浅色 `#E6F4EA` 背景 + `#81C784` 边框，深色 `#1B3A2A` 背景 + `#A5D6A7` 绿色标签。

- 仅收起的已完成动作变绿；手动收起未完成动作保持白卡。



## 字体：系统细黑体 Light



- 全局 `Typography` 改用 Android 系统 `sans-serif-light` Typeface，所有层级字重降为 `FontWeight.Normal`，保留原有字号/行高/tabular 数字。

- UI 层 10 个文件里原有的 `Bold/SemiBold/ExtraBold/Medium` 覆盖全部改为 `Normal`，避免局部又变回粗黑体。

- 保存页四项摘要的值改为 Normal，不再额外 SemiBold；身体卡数值同步改为 Normal。

- 不改布局、不打包第三方字体，安装包体积不变。



## A 清爽蓝主题：首页 + 训练



- 以用户提供的 `app-redesign.png` 为配色基准，但按要求只改配色，不恢复参考图中的「最近训练」，也不改底部活动迷你卡结构。

- 主题恢复 A 蓝：主色 `#2563EB`、浅蓝 `#E8F0FF`、页面底色 `#F3F6FA`、正文 `#0B1220`、次要文字 `#667085`、分割线 `#E1E7F0`、行动按钮橙色 `#FF6B35`；深色主题恢复深蓝灰。

- 首页概览改为深海军蓝渐变数据卡：`#060B14 → #11263F → #234A73` 横向渐变，白色大时长、浅蓝日期/说明、橙色开始训练。

- 三项统计去掉左侧色条和白色小卡，直接排在深色渐变卡内：浅蓝标签、白色数值，容量项保留点击 `kg ↔ t` 切换；空状态卡恢复白底，只保留记录身体、查看趋势。

- 训练页保留浅蓝「今天练什么？」、白色动作库、橙色开始训练；底部活动迷你卡改为与首页一致的深蓝横向渐变 `#060B14 → #11263F → #234A73`。

- 部位色、日历日期底色、照片画笔恢复 A 蓝/原色；`ActionOrange` 恢复橙色。

- 首页和训练页未改结构；其余页面仅继承主题色。测试：55 类、221 测试通过，0 失败。

- 按新要求去掉日期下方的「本周/本月」文字；日期范围本身仍可点击跳回当前周期。

- 首页深色概览卡按效果图收紧：顶部 12dp、底部 14dp，内部间距 9dp；时长从 `displaySmall` 改为 `headlineMedium`，日期左右箭头 32dp；标题「本周/本月训练时长」上移。

- 周/月模式改为 76×32dp 双段切换条：深色半透明轨道、蓝色选中块用 `animateDpAsState` + `tween(220ms)` 左右滑动，点击周/月仍切换统计区间。

- 动作库/选择器第一批统一：动作卡改为 `GymLogCard` 白卡 + 细边框、圆角图片、`titleSmall` 名称；分类栏和搜索继续沿用蓝色主题。趋势、我的、设置、身体、照片、计时器仍主要是旧布局，仅继承主题色，后续逐页重排。

- 目前没有与这套 A 配色对应的交付 APK；`GymLog-1.5-green-preview.apk` 是上一版青绿预览，不应作为本版验收包。临时模拟器安装仅用于查看效果。



## 日历月视图：A 方向第二版



- 新增 `CalendarMonthOverview`：深蓝横向渐变「本月训练」摘要，显示训练次数、总组数、总容量 `t` 和月份日期范围；数据由当月已保存训练现算。摘要内的「本月训练」用 `titleSmall`，日期范围用 `labelLarge`，三项统计标签用 `bodyMedium`，数值保持 `titleLarge`。

- 白色月历卡顶部居中显示月份标题，左右切换按钮在标题两侧；首页日期圆圈与整月详情色块使用同一套部位色（胸 `#E57373`、背 `#64B5F6`、肩 `#FFB74D`、腿 `#81C784`、手臂 `#BA68C8`、其他 `#90A4AE`），休息日灰、偷懒日黄、放纵日粉，选中日深色实心；不再显示数字下方小点，也不再显示外部图例。

- 选中日期行显示日期 + `未训练/已训练` 标签（点击弹出日类型选择），下方保留部位、当天总时长和训练卡片。

- 空日期卡改为白底、图标 + 标题 + 文案 + 右箭头，点击编辑当天备注。

- 右下角 FAB 移除，新增训练入口改到顶栏 `+`；月份标题仍可点击进入整月详情页。

- 测试：55 类、221 测试通过；临时模拟器已验收摘要、圆圈颜色、选中日期、空状态和迷你卡共存。未生成交付 APK。



## 训练页深蓝主卡



- 「今天练什么？」从浅蓝 `primaryContainer` 卡改为与首页一致的深海军蓝横向渐变：`#060B14 → #11263F → #234A73`，20dp 圆角。

- 标题改用浅蓝 `#BBD0EC`，说明文字用 `#D6E4F5`，橙色「开始训练」按钮保留。

- 动作库、训练计时器入口不变。

- 验证：`:app:installDebug` 成功；模拟器检查浅色训练页主卡。



## UI 批次 C12：动作详情与肌群编辑



- 动作详情弹窗改为 24dp 圆角，标题用 `titleLarge`；动作演示、PR、肌群和删除按钮逻辑未改。

- 肌群编辑弹窗改为 24dp 圆角，标题用 `titleLarge`；主要/次要肌群选择逻辑未改。

- 验证：`:app:installDebug` 成功；模拟器检查动作详情弹窗。



## UI 批次 C13：零散弹窗



- 照片提示、添加照片、选择月份、照片备注、日类型、日备注统一 24dp 圆角和标题层级。

- 日备注、照片备注输入框统一 14dp 圆角。

- 逻辑均未改。



## UI 批次 C14：改名、清理与深层回归



- 底部导航和页面标题「趋势」改为「统计」；首页入口文案改为「查看统计」。

- 设置 → 历史记录说明从「查看计时与身体历史」改为「查看身体记录历史」。

- 删除 `SettingsScreen.kt` 中已不再使用的旧 `ProfileCard` / `StatsCard` / `StatItem`。

- `ReleaseHistoryTest` 测试名改为 `newestNoteIsVersionOnePointFiveTwo`，断言仍是 1.5.2。

- 深色 + 360dp + 130% 检查统计、训练趋势、设置、备份、保存训练页。

- 修复保存页训练照片选择框在深色下仍是白底的问题，改用 `surfaceVariant` 半透明底；文案改用 `training_add_photo` 资源。

- 回归后恢复模拟器 density 440、font_scale 1.0、浅色模式。



## UI 批次 C11：日期选择器



- 四处 `DatePickerDialog` 统一为 24dp 圆角：身体资料生日、训练日期、肌群图日期、照片日期。

- 选择、取消、日期换算和确认逻辑未改。

- 日期选择器内部文案跟随系统语言；模拟器是英文系统，所以显示 `Select date` / `November 2000`，中文手机上会显示中文。这是系统控件行为，不在 App 内硬编码语言。

- 验证：`:app:installDebug` 成功；模拟器检查身体资料生日选择器。未生成交付 APK。



## UI 批次 C10：时长滚轮与训练时钟



- 训练时长滚轮 `DurationWheelDialog`：24dp 圆角、`titleLarge` 标题；时/分/秒滚轮中间选中行增加浅蓝圆角高亮，选中值使用主色，标签保留在下方。

- 训练时钟底部面板 `WorkoutClockSheet`：顶部圆角改为 24dp，内部计时圆、分段、±30 秒和开始/重置按钮逻辑不变。

- 验证：`:app:installDebug` 成功；模拟器检查训练时长滚轮。训练时钟面板已编译，未单独截图。未生成交付 APK。



## UI 批次 C9：确认弹窗统一



- 关键确认弹窗统一为 24dp 圆角：照片删除、训练草稿丢弃/已有训练删除、动作删除、活跃训练取消、清空数据两步确认、备份导入/结果、计时器编辑/重置、已保存计时删除、照片编辑放弃。

- 危险操作按钮统一为错误色：照片删除、动作删除、训练取消、清空数据、已保存计时删除、照片编辑放弃。

- 照片删除弹窗补标题「删除照片」，正文保留确认说明。

- 确认/取消逻辑、两步清空流程、备份导入流程均未改。

- 验证：`:app:installDebug` 成功；模拟器检查照片删除确认弹窗。其余弹窗已编译，未逐个截图。未生成交付 APK。



## UI 批次 C8：深色 / 360dp / 130% 字体回归



- 深色模式检查首页、日历、训练、我的、动作库和训练编辑页，整体可用。

- 修复日历选中日期：`MonthDayCircle` 改为主题主色圆底 + `onPrimary` 数字，避免深色或高字体下数字与圆底对比不足。

- 360dp（density 480）+ 130% 字体检查首页、日历、我的、训练编辑页：无横向溢出；我的资料卡三项统计、首页数据卡和日历月统计均正常。

- 已知可接受现象：我的页「身体数据」说明在 360dp/130% 下折成两行。

- 回归后已恢复模拟器：density 440、font_scale 1.0、浅色模式。

- 验证：`:app:installDebug` 成功；未生成交付 APK。



## UI 批次 C7：弹窗与表单统一



- 个人信息编辑弹窗：24dp 圆角、`titleLarge` 标题、头像居中、14dp 圆角用户名输入框、性别小标题、头像操作并排居中；头像选择和移除仍沿用原逻辑。

- 添加自定义动作弹窗：24dp 圆角、标题层级、动作名 14dp 圆角输入框，部位/器械小标题使用次要色。

- 体重输入弹窗：24dp 圆角、标题层级、体重输入框 14dp 圆角并占满宽度。

- 验证：`:app:installDebug` 成功；模拟器检查个人信息弹窗。添加动作、体重弹窗已编译，未逐个截图。未生成交付 APK。



## UI 批次 C6：照片详情与编辑器



- 照片编辑器画布改为深色 16dp 圆角背景，图片 `ContentScale.Fit` 居中。

- 工具区收进白色 `GymLogCard`：马赛克/模糊/涂鸦/文字/裁剪 Chip、旋转按钮、文字/模糊操作；涂鸦调色改为圆形色块，选中有边框。

- 照片日期改为白卡行：日历图标、照片日期、日期值、右箭头；备注输入框改 16dp 圆角。

- 照片详情保持大图 + 日期/备注白卡 + 橙色编辑按钮 + 修改日期/备注 + 红色删除。

- 验证：在模拟器相册放入测试图，完整走「照片墙 → 从相册选择 → 编辑照片 → 保存 → 照片详情」，`installDebug` 成功。未生成交付 APK。



## UI 批次 C5：肌群图



- 控制区从灰色 Surface + 裸 Chip 收成一张 `GymLogCard`：日期/训练块数、范围（当天/本周/本月）、角色（全部/主要/次要）、图例和说明同卡显示。

- 人体图放进独立白色 `GymLogCard`，正面/背面切换保留；无训练或存在未支持肌肉时，状态文案放入白卡。

- 肌群详情卡继续使用 `GymLogCard`；日期切换、范围/角色筛选、点击肌肉和调色逻辑未改。

- 验证：`:app:installDebug` 成功；模拟器检查控制卡、图例、人体卡和筛选 Chip。未生成交付 APK。



## UI 批次 C4：动作库



- 动作库搜索框改为 16dp 圆角；新增动作按钮改为橙色圆形 `+`。

- 左侧部位栏改为白底细边框卡片，选中项使用实心主蓝 + 白字；器械筛选保留横向 Chip。

- 数量标题改为 `titleSmall` + 次要色；动作栅格卡和详情弹窗结构保留。

- 动作详情弹窗沿用 `AlertDialog`：名称、部位/器械、做组 PR/极限 PR、动作演示/正面/反面、署名和主要/次要肌群。

- 搜索、筛选、自定义动作、删除和选择逻辑均未改。

- 验证：`:app:installDebug` 成功；模拟器检查动作库搜索/筛选、卡片栅格和动作详情弹窗。未生成交付 APK。



## UI 批次 C3：我的合并卡



- 「我的」页个人资料与三项统计合并为一张深蓝渐变 `ProfileStatsCard`：上半头像/姓名/性别/年龄/箭头，分隔线，下半三列统计。

- 三列统计使用白色数值、浅蓝标签、半透明分隔线；训练天数/年限、时长单位、体脂/体重仍可点击切换。

- 下方「身体数据」「照片墙」入口卡不变。

- 验证：`:app:installDebug` 成功；模拟器检查资料区、统计对齐、分隔线和入口卡。未生成交付 APK。



## UI 批次 C3：身体资料与围度编辑



- 身体资料编辑页 `BodyRecordEditor`：改为「基础资料」+「身体数据」两张白卡；生日改为可点击行（日历图标、日期、年龄、箭头），性别 Chip 放入基础资料卡；身高/体重/体脂使用 14dp 圆角输入框，BMI 作为说明行；底部橙色「保存资料」，不再使用顶栏保存。

- 围度编辑页 `GirthEditor`：按「手臂 / 躯干 / 腿部」三张白卡分组，左右围度并排，单位 cm；底部橙色「保存围度」。

- 保存、生日选择、字段校验、历史入口逻辑均未改。

- 验证：`:app:installDebug` 成功，模拟器检查两个编辑页的生日行、性别 Chip、数值字段、分组、BMI 和保存按钮。未生成交付 APK。



## UI 批次 C2：照片



- 照片墙：空状态改为白色 `GymLogCard`（图标、标题、说明）；月份分组显示「yyyy年M月 · N 张」；照片圆角加大；新增照片 FAB 改橙色。

- 照片详情：图片圆角加大；日期和备注移入 `GymLogCard`；编辑改为橙色主按钮，修改日期/备注改为并排文字按钮，删除保留红色。

- 照片编辑器：工具 Chip 从单行 `Row` 改为 `FlowRow`，小屏自动换行，避免 5 个工具横向溢出；绘制、马赛克、裁剪、旋转、日期和备注逻辑不变。

- 测试：55 类、221 测试通过，0 失败；模拟器验收照片墙空状态。当前没有照片数据，详情和编辑器未做运行截图，代码已编译。

- 未生成交付 APK。



## 活动训练入口改为底部迷你卡



- 训练页不再显示占满宽度的大活动卡；无活动训练时显示「今天练什么？」+ 动作库，有活动训练时隐藏「开始训练」主卡，只保留动作库和底部迷你卡。

- 活动训练改为根页面底部的深色迷你卡，位于底部导航上方：绿色状态点、`锻炼 · 时长`、动作名、取消入口、右箭头。

- 迷你卡在首页、日历、训练、趋势、我的五个根标签切换时保持显示；点击任意位置继续训练，垃圾桶弹出取消确认。

- 内容区底部 padding 使用迷你卡 + 导航栏的总高度，避免遮挡。

- 进入训练编辑页或保存页时不显示迷你卡，避免和编辑界面重复。

- 编译通过；`:app:testDebugUnitTest` 55 类、221 测试通过。

- 已用临时 `assembleDebug` + `adb install` 在模拟器验收：训练页无大活动卡和「开始训练」主卡、显示迷你卡；首页和日历切换时迷你卡保持；点击迷你卡恢复编辑页；垃圾桶弹出「取消这次锻炼」确认；确认后迷你卡消失、快照清理。未生成交付 APK。

- 首页和日历的当天多条计时记录都改为汇总为一条「训练总时长」，避免同一个日期重复显示多行。



## 第三阶段：日历页



- 修改前备份：`C:\Users\75192\AndroidStudioProjects\GymLog-backups\ui-phase3-calendar-20261022104231`。

- 顶部改为「日历」标题 + 「今天」+ 设置入口；不再沿用首页的「练有所述」标题。

- 选中日期区域改为白色 `GymLogCard`：日期、周几、部位容量、当天计时、日类型 Chip 放在同一张卡内。

- 空日期改为白色空状态卡，图标 + 标题 + 当天文案 + 「写一句」入口；仍可编辑当天备注。

- 训练记录卡改用 `GymLogCard`，保留动作行、照片、组数据和点击编辑。

- 右下角新增记录按钮改为橙色 `ActionOrange`。

- 未改：日期查询、选中日期逻辑、月切换、日历上色、特殊日、训练记录排序、编辑/保存、图片/遮罩。

- 验证：`:app:compileDebugKotlin` 成功；`:app:testDebugUnitTest` 55 类、221 测试，0 失败/错误/跳过。

- 日历已用临时 Debug 安装在模拟器验收：顶部「日历/今天」、选中日期白卡、部位容量、聚合后的当天总时长、日类型、空状态、橙色新增按钮、记录卡正常；当天多条计时记录已改为汇总显示，不再重复两行「训练总时长」。

- 未生成交付 APK；现有 `GymLog-1.5-ui-phase2.apk` 不含本轮日历和迷你卡改动。

- 整月日历详情页改为白色 `GymLogCard` 内的紧凑月历：顶部月份切换、固定 72dp 周行、日期数字 + 部位底色/特殊日标记；动作名和备注不再塞进格子，避免六周网格被撑高。图例移到卡片下方。已临时安装到模拟器验证，221 测试通过。



## 日历格子显示训练部位



整月日历详情页（`ui/calendar/TrainingCalendarDetailScreen.kt`）每个日期格子在数字下方增加一行「训练部位」文字，例如 `胸+腿`。只改这个页面，首页小月历（`CalendarMonthOverview` / `TrainingCalendarCard`）不动。



- 数据来源：当天真实保存的训练记录，**没有组数的动作不算**（与 `paintsFor` 的着色口径一致）。

- 归并口径直接复用 `CalendarColorCategory`：大腿+小腿→`腿`，上臂+前臂→`手臂`，腰/有氧/颈/未分类→`其他`。标签写在枚举的 `labelZh` 上（胸/背/肩/腿/手臂/其他），不要另建一套文案。

- 排序：按当天该大类的**容量**从高到低，容量相同按 `CalendarColorCategory` ordinal（胸、背、肩、腿、手臂、其他）兜底，保证两次渲染结果一致。

- 超过 3 个部位只显示前 3 个并加省略号，例如 `胸+背+肩…`；上限常量是 `CalendarDayPartsLogic.MAX_VISIBLE_PARTS`。另外格子实测只装得下约 5 个字符，所以折叠按字符预算做：还有后续部位时限 4 字符、末位限 5 字符，保证省略号不会切在「手臂」这种两字词中间（`胸+背+肩+腿+手臂` 实际显示 `胸+背…`）。

- 日类型标记（休息/放纵/偷懒）与部位文字**同时显示**，标记在上、部位在下。格高仍是 72dp，三行（数字 28dp + 两行 11sp）放得下。

- 逻辑在 `data/CalendarDayParts.kt`。体积聚合抽成 `CalendarDayColorLogic.categoryVolumes()`，与 `paintsFor()` 共用，保证格子的底色和文字不会算出两套结果。

- 验证：`:app:testDebugUnitTest` 56 类、233 测试，0 失败（新增 `CalendarDayPartsTest` 12 个）。已用 `:app:installDebug` 装 Debug 包在 Pixel_3a_API_33 模拟器验收，未出交付 APK；验收日类型+部位同显时曾临时往 `day_kinds` 写入一行 (20734,'REST')，验证完已删除，数据已还原。



## 迷你卡只显示当前动作



底部活动训练迷你卡（`ui/train/ActiveWorkoutMiniBar`）第二行从「列出草稿里全部动作名」改为**只显示当前动作**。



- 当前动作 = 草稿动作列表里第一个 `completedAtMillis == null` 的动作（`ui/train/ActiveWorkoutLabel.kt` 的 `currentExerciseLabel()`）。做完一个点「完成动作」，迷你卡自动切到下一个。

- 「完成」以「完成动作」按钮为准；把组全勾完但不点按钮，动作仍是当前动作（与编辑器收起变绿口径一致）。

- 没有可显示的动作（还没加动作，或全部已完成）→ 显示 `training_active_next_hint`「开始新的动作吧」（`ActiveWorkoutUi.currentExerciseName` 为 null 时）。绿点、第一行「锻炼 · 时长」、垃圾桶、点击继续都不变。

- 验证：`:app:testDebugUnitTest` 57 类、239 测试，0 失败（新增 `ActiveWorkoutLabelTest` 6 个）；Debug 包在模拟器验收了三种状态：全部完成→提示语、加一个未完成动作→显示「上臂-哑铃 交替 肱二头肌弯举」、删除后回到提示语。未出交付 APK。



## 迷你卡在定时器运行时显示倒计时



训练进行中（底部迷你卡可见）且训练内的定时器（`WorkoutClock`，倒计时/秒表，组间休息用）正在跑时，迷你卡**第一行右侧**显示该定时器的时间，第一行左侧仍是「锻炼 · 总时长」，第二行（当前动作名）不变。



- `WorkoutClockUi` 的 `running == true` 才显示；暂停、停止或倒计时归零后第一行恢复成「锻炼 · 总时长」，不残留定时器时间。

- 时间格式用 `WorkoutClockLogic.formatClock(displayMillis)`（与训练内定时器面板一致，输出 `MM:SS` / `HH:MM:SS`），前面带一个定时器小图标。

- 数据来源：`GymLogApp.kt` 里 `activeEditorViewModel` 与编辑器共用同一个 `SessionEditorViewModel` 实例（key 都是 `${draftKey}-$dataEpoch`），所以迷你卡直接 `activeEditorViewModel?.clock?.collectAsStateWithLifecycle()` 就能读到正在跑的定时器；回到根页面不会丢草稿也不会丢定时器状态。

- `ActiveWorkoutUi` 增加可空字段 `timerText: String?`（默认 null）；`ActiveWorkoutMiniBar` 第一行由单个 `Text` 改成 `Row`，左侧时长 `weight(1f)`、右侧定时器时间。

- 验证：Debug 包在 Pixel_3a_API_33 模拟器验收——从迷你卡进入编辑器、开定时器（02:00 倒计时）回到根页面，迷你卡第一行显示「锻炼 · 09时31分25秒 01:14」（时长 + 倒计时并排）；暂停定时器后回到根页面，第一行只剩「锻炼 · 09时32分26秒」，定时器时间消失。未出交付 APK；验收用的是模拟器上已有的用户草稿，结束后已暂停定时器并保留原草稿。



## 计时器后台/锁屏震动提醒



倒计时到点后，应用在后台或锁屏时也要能震动。原来的实现是「协程 `delay` 到点 → 调 `Vibrator.vibrate()`」，前台可以、后台不行。原因有两条，缺一不可：



1. 协程随界面走，进程退到后台被系统挂起后 `delay` 不再准时（甚至不再执行）。

2. `Vibrator.vibrate()` 官方定位就是「前台才有效」，后台调用会被忽略。



改法：把「到点」这件事交给系统闹钟，把「提醒」这件事交给通知。



- `data/TimerAlarmScheduler.kt`：`scheduleCountdown(remainingMillis)` 同时注册两条精确闹钟：主路径 `AlarmManager.setAlarmClock()`（RTC_WAKEUP，可唤醒 CPU，且会在状态栏显示闹钟图标），备用路径 `setExactAndAllowWhileIdle()`（部分 ROM 只延迟前者，不一定延迟后者）。`cancel()` 两条都取消。请求码 `REQUEST_ALARM=9401`、`REQUEST_SHOW=9402`、`REQUEST_FALLBACK=9404`。

- `data/TimerAlarmReceiver.kt`：静态注册的 `BroadcastReceiver`，收 `ACTION_TIMER_FINISHED`（`com.example.gymlog.action.TIMER_FINISHED`），`exported=false`。到点后调 `TimerAlerts.alert()`；主/备闹钟可能同时到达，Receiver 用单调时钟做 3 秒去重，避免双提醒。

- **自然归零时不能 cancel 系统闹钟**：`SessionEditorViewModel.tickClock()` 只把 UI 状态置零并发出 `clockFinished`，让 AlarmManager 自己触发；否则后台时钟协程先跑完时会把唯一可靠的系统提醒取消掉，表现就是「到点没有任何系统提示」。

- `data/TimerAlerts.kt`：发一条通知（渠道 `timer_finished`、id 9401、`CATEGORY_ALARM`、`PRIORITY_MAX`、震动 `[0,600,400,600]`）；通知权限被拒时退回直接调 `Vibrator`。**后台震动只能靠通知渠道带出来，直接调 Vibrator 在后台无效。**

- `AndroidManifest.xml`：`POST_NOTIFICATIONS`、`USE_EXACT_ALARM` 两个权限；注册 receiver。`USE_EXACT_ALARM` 是系统授予、不可撤销，比 `SCHEDULE_EXACT_ALARM` 省掉「用户去设置里手动授权」这一步。

- `MainActivity.kt`：`RequestNotificationPermissionIfNeeded()` 在 API 33+ 首次进入时申请通知权限。

- `ui/session/SessionEditorViewModel.kt`：构造参数多了一个 `timerAlarmScheduler: TimerAlarmScheduler?`（factory 同步加）。开始/调整倒计时时 `scheduleCountdown()`；暂停、重置、切换计时模式、结束训练时 `cancel()`。**三处 `SessionEditorViewModel.factory(...)` 调用点都要传，漏一个就会静默失效。**

- `ui/session/SessionEditorScreen.kt`：前台到点仍是 `viewModel.clockFinished.collect { TimerAlerts.alert(context) }`。本地 `vibrateOnce()` 已删除，Vibrator 的 import 一并清掉。



验证：Debug 包装到 Pixel_3a_API_33 模拟器。`adb shell dumpsys alarm` 看到 `RTC_WAKEUP ... com.example.gymlog.action.TIMER_FINISHED` 且时间为精确值；按 Home 退后台等 65 秒后 `dumpsys notification` 能看到已发出的通知，渠道 `timer_finished`、重要性 4、`mVibration=[0, 600, 400, 600]`、`mVibrationEnabled=true`。未出交付 APK。



已知边界（未做，需要时再说）：



- **一加/ColorOS 会整体调整应用闹钟**。2026-10-27 在 OnePlus PJZ110 / Android 16 上实测：主 `setAlarmClock` 和备用 `setExactAndAllowWhileIdle` 都被 `OplusAlarmAdjustment` 推到 `+2d23h59m`，即使应用已加入 DeviceIdle 白名单、前台服务和通知都正常。此时代码无法从应用侧绕过，必须在系统设置里关闭「睡眠待机优化」/把应用设为「无限制」并允许后台活动。普通 Android 和模拟器已验证能准时弹出 `timer_finished` 通知。

- **重启后闹钟会丢**。`AlarmManager` 的闹钟不跨开机。要做开机恢复得再加 `BOOT_COMPLETED` 的 receiver，把没跑完的倒计时重新注册，并且倒计时目标时间得先落盘。

- **国产 ROM**（小米/华为/OPPO/vivo 等）默认限制后台唤醒，即使代码正确也可能不响。只能引导用户去「电池优化 / 自启动 / 锁定后台」里把应用加白名单，代码里加不了。

- **Doze 深度待机**下 `setAlarmClock` 是少数还能准点的方式，但如果用户手动开了省电/飞行模式仍会不准。

- 应用被用户从最近任务划掉后，`BroadcastReceiver` 仍能收到闹钟（静态注册），但进程若已被杀且没有落盘数据，恢复不了剩余时间。



## 保存页删除重复的训练照片添加入口



保存页「训练照片」区块原来有两处功能完全一样的添加入口：① 大虚线框（点框内任意位置都触发选图），② 框下方一个独立的「添加照片」文字按钮。两者都调 `onPickPhoto`，属重复。



- 删掉 ② 那个独立按钮（`SessionEditorScreen.kt` 照片 Card 内原来的 `Row` 分支：未选照片时显示 `training_add_photo`、已选照片时显示 `training_change_photo` + `training_remove_photo`）。

- 保留：未选照片时只有大虚线框（框内含图标 + 「添加训练照片」+ 提示语，点击即选图）；已选照片时显示图片，并在下方保留「更换照片」+「移除」两个按钮（这两个是必需的，避免误删已选图）。

- 验证：Debug 包含在 Pixel_3a_API_33 模拟器验收，进入用户草稿的保存页，照片区块只出现一个「添加训练照片」字样，原重复的「添加照片」按钮已消失；验收用草稿保留未动。未出交付 APK。



## 日历页删除顶栏新增训练入口



日历根页面顶栏（`ui/GymLogApp.kt` 的 `CalendarTopBar`，只被 `HistoryScreen` 使用）原来有三个操作：「今天」文字按钮、`+` 新增训练图标按钮（content-desc 用 `add_record`「新增训练」）、设置齿轮。按需求删掉 `+`。



- 删掉的内容：`CalendarTopBar` 里的 `IconButton { Icons.Filled.Add }`；`CalendarTopBar` 和 `HistoryScreen` 的 `onAdd` 参数及 `HistoryScreen` 调用处传参（原来点 `+` 会用 `selectedDate` 打开 `Screen.SessionEditor(sessionId = null, dateEpochDay = selectedDate.toEpochDay())`）；`Icons.Filled.Add` 的 import；`strings.xml` 的 `add_record`（全项目仅此一处引用）。

- 保留：「今天」和设置齿轮不动；新增训练仍可从首页「开始训练」和「训练」页进入。展开整月页（`TrainingCalendarDetailScreen`）本来就没有新增按钮，不用动。

- 验证：Debug 包含在 Pixel_3a_API_33 模拟器验收，日历页顶栏只剩「日历 / 今天 / 设置」，全页面已无「新增训练」字样；编译通过。未出交付 APK。



## 头像操作收敛到头像查看/编辑页



「选择头像」「移除头像」原来直接放在「编辑个人信息」弹窗里（`ui/settings/SettingsScreen.kt` 的 `ProfileEditorDialog`）。按需求改为：外层界面（「我的」页资料卡、编辑弹窗本身）只显示头像本身，两个操作按钮收进点击头像后进入的「头像」页。



- `ProfileEditorDialog`：删掉头像下方的「选择头像/移除头像」`Row`；头像预览包了一层 `clickable`（`showAvatarEditor = true`），点头像打开 `AvatarEditorDialog`。pending 状态仍由本弹窗持有：`pendingBytes` / `clearAvatar`，保存时经 `onSave(name, gender, pendingBytes, clearAvatar)` 交给 `MineViewModel.saveProfile`，链路未变。

- 新增 `AvatarEditorDialog`（`SettingsScreen.kt`）：「头像」标题 + 180dp 大图预览 + 「选择头像」（`mine_pick_avatar`）+「移除头像」（仅 `hasAvatar` 时显示）+「完成/取消」。系统选图 → `decodeAvatarForCrop` → `AvatarCropDialog` 裁剪的整条链路（picker launcher、`cropSource`）从 `ProfileEditorDialog` 搬到这个弹窗里，自包含。

- 状态流转（验收过的路径）：

  - 头像页「移除头像」→ 回调把父弹窗 `pendingBytes=null, clearAvatar=true` → 预览变占位图、移除按钮隐藏；再「选择头像」→ `pendingBytes` 有值、`clearAvatar=false`，按钮恢复。

  - 裁剪页「使用」→ `onCropped(bytes)` 更新父弹窗 pending 状态并关裁剪页，头像页预览即时刷新（父状态是参数传入，重组即更新）。

  - 头像页「完成/取消」都只是关掉本页，不回退已做的 pending 修改；最终「取消」在编辑弹窗，弹窗离开组合后 `remember` 状态全部丢弃，不落库。

  - 编辑弹窗「保存」→ `MineViewModel.saveProfile` 写 `filesDir/profile/avatar.jpg` + `saveIdentity` → `observeProfile()` 回流 → `MineUiState.avatarPath` 更新，「我的」页头像自动刷新（验收时文件 08:22 被重写，页面即时显示新头像）。

- 新增字符串 `mine_avatar_title`「头像」、`mine_avatar_done`「完成」；`mine_pick_avatar` / `mine_remove_avatar` 搬到新弹窗继续用。`values-en/strings.xml` 本来就没有这批 mine_ 字符串，未加。

- 验证：Debug 包含在 Pixel_3a_API_33 模拟器走完整链路——「我的」页无按钮；编辑弹窗无按钮、点头像进头像页；移除头像后按钮隐藏；选图→裁剪→使用→头像页预览刷新；保存后「我的」页头像与 `files/profile/avatar.jpg` 同步更新；编辑弹窗取消后资料原样。编译通过，未出交付 APK。

- **验收注意事项**：完整链路会把模拟器上用户的真实头像覆盖掉。本次验收后已用设备里 2026-10-20 的备份（`/sdcard/Download/gymlog-2026-10-20.json` 的 `profile.avatarJpeg`）按字节还原（MD5 `fc403744efb98243001b7c17d62858bd`，43956 字节，与备份解码结果完全一致）。以后验收这条链路，先备份头像或验收后还原。



## 日历选中日期：部位名与日期并排并加粗



日历根页面选中日期卡（`ui/GymLogApp.kt` `HistoryScreen` 的 `selected-date-header` item）原来日期、「已训练/未训练」徽章在第一行，部位名（`partLine`，由 `daySessions.partVolumes` 归并）单独在第二行。按需求把部位名挪到日期同一行并加粗。



- 结构：外层 `Row`（日期+部位 包在内层 `Modifier.weight(1f)` 的 `Row` 里，徽章保持最右不变）；内层日期 `Text` 不再挂 `weight(1f)`（由内层 Row 整体占位，徽章才能继续贴右边），部位名 `Text` 加 `padding(start = 8.dp)`。

- 部位名样式：`titleMedium` + `fontWeight = FontWeight.Bold` + `primary` 色不变；日期 `titleLarge` 格式/字号/颜色完全未动。

- 防溢出：日期和部位名都加 `maxLines = 1` + `TextOverflow.Ellipsis`。内层 Row 被 `weight(1f)` 限定了宽度，部位名过长时先被截断，不会把日期挤没或顶掉徽章；`partLine` 的计算逻辑（归并、去「部」后缀、`+` 连接）没有改。

- 验证：Debug 包含在 Pixel_3a_API_33 模拟器验收——选 10月13日，「2026年10月13日」与「背+上臂」在同一行（y 区间重叠、水平相距 8dp）、加粗显示，「已训练」徽章仍在最右，三者不重叠；选无训练的 10月27日只有日期和徽章，位置与改前一致。编译通过，未出交付 APK。



## 历史需求（都已落地）



按用户提出的顺序。后面的条目会覆盖前面的界面结构，以「信息架构」一节为准。



1. 动作库、一次训练多动作、每组单独记。三种重量模式。

2. 日历、按选中日期过滤、空日期提示。会话卡片按部位着色。同部位排在一起。容量按组、按次训练汇总。

3. 身体记录：生日算年龄，身高体重体脂可选，自动时间，BMI。入口曾经在设置齿轮里。

4. 修复主线程 Room，以及保存后 ViewModel 被复用导致立刻弹回首页（`exitAfterSave` 用 SharedFlow）。

5. 腰围、臂围，规则同身高体重。迁移 5→6，不丢数据。

6. 导入导出全部本地数据。导入前确认，替换全部。

7. 次数加减，一次一步。

8. 「自重加负重」改名为「自重负重」，仅该模式允许负数。负的附加重量不计入容量。PR 仍是数字更大更好（-10 好于 -20）。

9. 自重按个人记录里的体重算，没有记录按 0。自重负重在该体重上增减。

10. 新建记录时，添加动作的第一组沿用上一组的重量和次数。

11. 系统返回接到页面上，而不是直接关掉 Activity。浮层先关。编辑页不弹「放弃更改」。

12. 启动器名称曾经改为「训迹」，后来改为「练有所迹」/ Traivol。图标改为黑底白标。

13. 信息架构：底部三个入口。设置不再当总目录。导出导入当时留在「我的」。

14. 之后又加了：每个根页面的齿轮进外观设置；没训练的日期可以写一句话（迁移 6→7，并进入备份）；两步确认清除本地数据；关于与版本更新。

15. 底部改为四个：日历、训练、进步、我的。原来的训练页改名为日历，顶栏用应用名。动作库成为「训练」这一页。导入导出移进设置。

16. 1.2：动作带主要/次要肌群（迁移 7→8）。身体记录可开训练肌群图。资料可填性别（迁移 8→9）。训练趋势增加容量。底部「进步」改名为「趋势」。备份带上性别和肌群。

17. 1.2.1：日历上的训练计时器，可保存、可关联当天训练（迁移 9→10）。关机或重启后进行中的计时暂停。「训练」改成入口，动作库和训练记录可进，计划和工具仍是占位。动作库按部位和器械浏览，搜索时查全部。点日历标题进关于。

18. 1.3：「我的」有用户名和头像（迁移 10→11）。身体记录在人体图上记左右围度（迁移 11→12）。用户名、围度和头像进入备份，备份版本升到 2。趋势入口改名为「训练足迹」「身体数据」，训练足迹可按部位看。趋势图可点选某一天。计时器用开机次数判断重启。

19. 1.3.1：底部增加「首页」，冷启动改到首页。头像可以拖动、双指缩放后裁剪。「我的」的三项统计可以切换。训练卡片最多 9 格（1.4 起改为 2×3、最多 6 格）。人体图素材按进程缓存。

20. 1.4：动作库切换为 1,324 个数据集动作和 10/28 分类；旧预置退出活动目录；接入 1,324 张缩略图和 1,324 个 GIF；动作详情改为「动作演示 / 正面 / 反面」；训练记录改三栏并显示每组重量 × 次数。计时器页不再显示已保存计时，改由设置 → 数据 → 历史记录进入；数据下新增「数据备份」和「历史记录」两个子页面。计时操作改为图标并与训练结束同排。媒体按 Gym visual 授权打包。

21. 1.4.1：日历日类型（休息/放纵/偷懒，迁移 14→15，进备份 `dayKinds`）；日历上色改为六大类 + 特殊日盖过（`CalendarDayColorLogic`）；「我的」增加照片墙（迁移 15→16，私有存储，不进备份、清除训练数据不删）；「我的」身体入口改称「身体数据」；资料编辑可改性别；设置历史记录（计时/身体）在 1.4 已有，1.4.1 版本说明再次列出。

22. 1.4.2：物理删除旧的单侧臂围 `armCm` 及其数据（迁移 16→17），身体历史不再显示旧臂围；旧备份里的 `armCm` 导入时忽略。

23. 1.5：训练总时长自动计时并写入日历计时记录，日历手动计时入口移除但旧记录保留；训练时长/计时器支持滚轮编辑；动作用时写入 `exercise_durations`（Room 19）；动作卡支持完成动作收起、组横向编辑、自重组自动体重、保存页照片/备注/日期。

24. 1.5.2（开发中）：日历展开月历格子显示训练部位文字；底部迷你卡只显示当前动作；计时器倒计时在后台/锁屏到点也能震动提醒（改走 AlarmManager + 通知，详见「计时器后台/锁屏震动提醒」）。三项均无数据库迁移，Room 仍是 19。

26. 1.5.3：组内重量/次数改为可点击手动输入（`BasicTextField` + 逐字过滤、确认时收敛，上下箭头与步长不变）；活动训练通知上线（前台服务 `specialUse`，后台常驻、点通知回同一草稿、正文同步组歇/秒表状态）。无数据库迁移，Room 仍是 19。

25. 设置里删除「历史记录」入口：连同只有一个身体记录行的中间页 `SettingsHistoryScreen.kt`、`Screen.SettingsHistory` / `Screen.BodyHistory` 两个路由（含 ScreenSaver 的编解码）、`AppSettingsScreen` 的 `onOpenHistory` 参数和 `settings_history` / `body_history` 字符串一并删除；`Icons.Filled.History` 的 import 随之移除。身体记录历史只剩身体数据页内两个入口（展示页和编辑器里的「身体记录」卡）。`TimerHistory` 的返回从「历史记录菜单」改为 `from` 根页面。无数据库迁移，单测数量不变。



## 改代码时容易踩的点



- `displaySet` 只有一个重量参数。传入的必须已经是有效公斤。

- 保存身体记录会重算 PR。只改展示、不改重量含义时不要顺手重算。

- 动作库用 `library-$dataEpoch`；选动作器用 `picker-${draftKey}-$dataEpoch`。不要把两者合并，否则新建训练会继承上一次的搜索和筛选。

- 导入/导出只放在 `BackupDataScreen`；设置首页只保留「数据备份」入口（「历史记录」入口已删，身体记录历史走身体数据页内进入）。`TimerHistory` 路由保留但无入口，不要再把已保存计时塞回计时器页。

- 根页面的 `Scaffold` 在底部栏留白之内，并 `consumeWindowInsets` 掉导航栏底部，避免和底部栏叠两层 inset。子页面在这层外面，自己处理 inset。

- 隐藏中的根页面用 0 尺寸布局留在组合里，避免点到后面一页，同时留下滚动状态。

- 英文资源目前只有 `app_name`。不要把整份中文 `strings.xml` 翻成英文，除非用户要求。

- 清除数据和导入 version 1 都不会删除 `filesDir/profile/avatar.jpg`，也不会重置 `gymlog_timer`。已保存计时只在 version 2 或清除数据时整表替换。

- 人体图的底图和遮罩必须同一矩形绘制。不要单独缩放遮罩，不要把 `quadriceps` 和它的三块成分一起涂亮。

- `armCm` 已在 16→17 删除。不要把它加回实体、备份导出或历史 UI；旧备份里的 `armCm` 直接忽略。左右臂围/小臂围继续使用 `armLeftCm` / `armRightCm` / `forearmLeftCm` / `forearmRightCm`。

- 日历格子颜色必须走 `CalendarDayColorLogic`；特殊日盖过部位色。不要再按旧的单 `BodyPart.rank` 独自算一套颜色给小日历或展开月历。

- `dayKinds` 是可选备份字段；缺键=空。不要把照片墙塞进备份，也不要在清除/导入训练数据时删照片文件。

- 照片墙相对路径必须经 `PhotoPaths.isSafe`；FileProvider 只用于相机临时文件。不要把用户照片内容写进日志或交接文档。

- 肌群训练高亮用红色系，用户选中用主题 `primary`；不要把选中画成训练色。点击命中优先使用 `MuscleMaskCatalog.layers` 的绘制顺序。1.6.0 起肌群素材已彻底替换：19 条目录（`MuscleCatalog`），男/女双版素材在 `assets/muscle_masks/{male,female}/{front,back}/`，加载层按性别拆分（`MuscleSex` + `MuscleAssetLoader`）。旧 33 肌群 id 已全部废弃，`PresetMuscleMappings` / `DatasetMuscleMapping` 已改写为新 id；不要再引用 `trapezius_upper`、`soleus`、`forearm_flexors` 等旧 id，也不要恢复 `AnatomySvg.kt`、`muscle_manifest.json` 和 `unsupported` 降级 UI。

- 肌群加载按性别：`MuscleAssetLoader` 实例经 `MuscleAssetCache.forSex(sex)` 获取，`MuscleFigure` 必须传 `sex`；遮罩文件名 = muscleId，无侧肌（斜方/菱形/竖脊/腹直/前臂/小腿三头）用整块编号。

- 长驻协程闭包捕获旧状态是重复踩过的坑（日历滑回当月标题不更新、动作分类双击失效）。`LaunchedEffect` 里读外部状态一律 `rememberUpdatedState`。

- 直接用 adb 改 app 数据库是高危操作：拉取和推回必须包含 `gymlog.db` + `-wal` + `-shm` 三件套，先 force-stop。只换主库、删 WAL 会丢掉未 checkpoint 的近期训练记录（2026-11-02 已因此丢过一次数据）。app 现有自动备份兜底（见「自动本地备份」）。

- 与 1.4.1 / 1.4.2 / 训练重构相关的单测包括：`DayKindMigrationTest`、`DayKindLogicTest`、`CalendarDayColorTest`、`CalendarDayPartsTest`、`PhotoWallMigrationTest`、`PhotoImageTest`、`BodyMeasurementMigrationTest`、`WorkoutDurationMigrationTest`、`WorkoutClockLogicTest`、`ActiveWorkoutLabelTest`、`ReleaseHistoryTest`（最新版应跟随最新发版，当前 1.6.1）。



## 1.5.1 发版



- `versionCode = 11`，`versionName = 1.5.1`。

- 设置 → 历史记录移除「计时记录」入口，只保留「身体记录」；计时器统一放在训练页「训练计时器」。

- 版本更新记录精简为 1.5.1 与 1.5 两条；1.5.1 只保留界面焕新、交互优化两组共 5 条。

- `ReleaseHistoryTest` 最新版本断言同步改为 `1.5.1`。

- Release 构建命令：`gradle :app:assembleRelease --no-daemon --max-workers=1`，已成功。

- 第一次 release 开启 `optimization` 后 R8 把 Kotlin 集合类混淆坏，启动即 `IllegalAccessError`；已关闭 release optimization，重新构建并签名。

- 单测：清理死机写坏的 `apk-for-local-test.ap_` 测试资源副本后，`:app:testDebugUnitTest --max-workers=1` 通过，55 类 / 221 测试，0 失败。

- 产物：`GymLog-1.5.1.apk`，156,292,348 B，SHA-256 `E10EA8AB46D4808E6E89DCC3F025FD97AB88B102A061BF8C4D25E9B03A01EBFD`。

- 已在模拟器 `install -r` + 冷启动验证，首页正常打开，无 FATAL EXCEPTION。

- 使用 `~/.android/debug.keystore`（Android Debug 证书）签名，和此前预览 APK 的签名一致，可覆盖安装；这不是正式发布证书。



## 1.5.2（已出可试安装包）



- ersionCode = 12，ersionName = 1.5.2（2026-10-24 自 1.5.1 升上来，无数据库迁移）。

- 包含四项功能：日历展开月历格子显示训练部位；底部迷你卡只显示当前动作；计时器后台/锁屏到点震动（见「计时器后台/锁屏震动提醒」）；训练迷你卡在定时器运行时第一行右侧同步显示倒计时/秒表时间（见「迷你卡在定时器运行时显示倒计时」）。另含一处 UI 修正：保存页删除重复的「训练照片」添加入口（保留大虚线框 + 已选图时的「更换/移除」，见「保存页删除重复的训练照片添加入口」）。

- ReleaseNotes.kt 顶部新增 1.5.2 更新记录；ReleaseHistoryTest 首条断言同步改为 1.5.2。

- 构建：gradle :app:assembleDebug（debug 证书自动签名，可直接 db install 或拷到手机安装，用于试用）。最终单测：57 类 / 239 测试，0 失败、0 错误。

- 产物：仓库根目录 GymLog-1.5.2.apk，166,734,143 B，SHA-256 9942D152B8583ABCC862820740A4C43F989BFEC04963719698380F342AB30E7B。

- apt dump badging：com.example.gymlog，versionCode 12，versionName 1.5.2，label「练有所迹」。

- 签名：Android Debug 证书（CN=Android Debug，证书 SHA-256 8dfbabfe94e907e2ebe5c0069ae31a868da7109219e564a7edbe04a01514b633）；可覆盖旧预览包，但不是正式商店签名。

- 已在模拟器 db install -r + 冷启动验证，无 FATAL EXCEPTION。



## 1.5.4 发版



- ersionCode = 14，ersionName = 1.5.4（2026-10-28，自 1.5.3 升上来，无数据库迁移）。

- 1.5.3 的训练记录分组改名为「bug修复」，保留重量/次数手动输入内容。

- 外观改为独立设置页；新增日历配色：鲜明（最亮色）、柔和（当前色）、自定义（六部位 HSV 调色盘）。

- 自定义配色支持 #RRGGBB / RRGGBB 色号输入；非法输入提示并禁用应用。

- 新增通知设置：可关闭定时器到点通知；关闭取消主备闹钟和已投递提醒，重新开启按剩余时间恢复；开启时提示检查通知权限、耗电管理和睡眠待机优化。

- 数据备份更名为数据管理，清除本地数据移入其中，保留两步确认。

- 测试：65 类 / 308 测试，0 失败、0 错误。

- Debug APK：GymLog-1.5.4.apk，166,199,844 B，SHA-256 494FF0FD40EAB626867A7087CB12E9B8A405EE5C1A23D8750A4D5F2590BC16D5。

- apt dump badging：versionCode 14，versionName 1.5.4，label「练有所迹」；Android Debug 证书签名，不是正式商店签名。

- 模拟器 install -r 后冷启动无 FATAL EXCEPTION。



## 肌群素材彻底替换（1.6.0）



- 素材：4 张 1024×1536 底座（男女×正背，`drawable-nodpi/muscle_*_base_{male,female}.png`）+ 54 张遮罩（`assets/muscle_masks/{male,female}/{front,back}/`，文件名 = muscleId）。旧的 135 个遮罩/底座文件、`AnatomySvg.kt`、`muscle_manifest.json` 已删除。

- `MuscleCatalog` 收为 19 条：斜方肌、菱形肌、竖脊肌、腹直肌、前臂肌群、小腿三头肌为左右连块的整块编号，其余按 `_l/_r` 分侧。用户明确不做旧 id 兼容、不考虑旧备份导入。

- 加载层按性别：`MuscleSex` 枚举 + `rememberMuscleSex()`（读身体资料性别），`MuscleAssetCache.forSex(sex)` 每性别独立 loader；`MuscleFigure` 加 `sex` 参数，全部 4 个调用点已传。

- 围度锚点改用 `forearm` / `gastrocnemius` 遮罩。`unsupported`（深层肌不绘制）降级 UI 全链路删除。

- 预置数据改写：`PresetMuscleMappings` / `DatasetMuscleMapping` 中旧 id 按合并映射改写（三头斜方→trapezius、前臂三组→forearm、rectus_femoris/vastus_*→quadriceps、soleus→gastrocnemius），仅训练胫骨前肌的预置动作 `tibialis_raise` 已删除；导入器对同动作肌群链接去重。

- 回归测试：`MuscleResourceTest` 断言遮罩 100% 落在底图内（容差 8px，实测 male back/gastrocnemius 4px）。

- 肌群图空态和高亮态都淡画全部遮罩轮廓（16% 透明度），纯剪影问题已修。



## 我的页统计图表（1.6.0）



- `MineChartCard`：名片与 2×2 快捷入口之间的按周柱状图，Canvas 自绘无三方库。

- 右上范围：最近 1 个月 / 3 个月 / 1 年 / 全期，持久化在 `MineChartPrefsStore`（SharedPreferences），重启恢复上次选择；全期超 52 周自动抽稀。

- 三指标 HorizontalPager + 底部 chips 双向联动；头部大数字居中、范围靠左、单位靠右（Box 叠加布局，uiautomator 实测像素级居中）。

- 单位：时长小时（小数，90 分钟=1.5 小时）/ 分钟；训练量 KG（≥1 万压缩成 15k）/ T；天数无单位切换。

- 点击柱子顶部气泡显示该周数值（纯数字无单位），其他柱变淡；切页/换范围清空选中。



## 日历滑切与每日容量（1.6.0）



- `CalendarMonthOverview` 网格放进 HorizontalPager（1600 页，1970-2103）：手势、‹ › 按钮、「今天」、跨月点击全部走滑动动画并双向联动月份状态。

- 坑：pager 的 snapshotFlow 闭包捕获旧 month，滑回同一月份不触发更新；用 `rememberUpdatedState` 修。

- 日历展开页（`TrainingCalendarDetailScreen`）训练日格子第三行小字显示当日容量（`formatDayVolume`：无单位，≥1 万压缩成 k）；休息日和跨月格不显示。数据源 `dayVolumes` 在 `GymLogApp` 用 `partVolumes` 按日汇总。



## 去解释小字（1.6.0）



- 设置首页五行副标题、我的页 2×2 快捷网格副标题、统计页分组小标题与两卡副标题、训练页入口卡副标题与 hero 副标题全部删除；子页面内部的功能说明和风险提示保留。

- 我的页 2×2 网格：身体数据 / 照片墙 / 数据管理 / 关于，数据管理与关于有 Mine 直达入口（`MineScreen` 加 `onOpenBackup` / `onOpenAbout`）。



## 自动本地备份（1.6.0）



- `AutoBackupManager`：冷启动 `GymLogApplication.onCreate` 异步触发，距上次超过 6 小时才备份；JSON 快照复用 `BackupCodec`，写到 `files/auto_backups/`，保留最近 3 份滚动淘汰。照片墙图片不进快照。

- 数据管理页新增「自动备份」卡片：显示上次备份时间、「立即备份到本机」、备份条目列表（时间+大小）、点条目从备份恢复（红色确认框，走 `importSnapshot` 自带回滚）。

- 坑：`list()` 解析快照头不能截断 JSON（JSONObject 解析半截必抛异常），用正则从文件头 2KB 提取 `exportedAt`。



## 1.6.0 发版



- versionCode = 19，versionName = 1.6.0（2026-11-02；先以 17 发过一次，加入统计图表、日历、备份后归并重发为 19，tag `v1.6.0` 强移到最终提交）。

- 内容：肌群素材彻底替换、我的页统计图表、日历滑切与每日容量、去解释小字、自动本地备份。

- ReleaseNotes.kt 顶部为 1.6.0（5 节），`ReleaseHistoryTest` 首条断言为 1.6.0。

- GitHub Release：`v1.6.0`，附件 GymLog-1.6.0.apk（debug 签名，163.8MB）。发版流程：版本号 → 应用内公告 → 测试 → commit+tag → `gh release create/upload` → adb 装实机（无线调试 `adb-cdbf33ff-dx3Km2._adb-tls-connect._tcp`）。

- 动作库数据另有一份手工补全：全量 1418 个动作的主要/次要肌群已按 19 肌群目录补齐（含 TARGET 歧义大类按动作名细分），并已用 `muscle_sync.py` 写回模拟器与实机数据库。注意这是 DB 直改，重新导入预置目录会回退；要长期生效需把映射固化进导入器数据。



## 1.6.1 发版



- versionCode = 20，versionName = 1.6.1（2026-11-04；提交 `9f92587`，tag `v1.6.1`）。无数据库迁移，Room 仍是 21。

- 统计页（`ProgressScreen`）新增卡片「肌群训练热力图」。默认最近 7 个本地自然日（含今天），可切换最近 30 个本地自然日（含今天）。只计已保存训练的组。同一课次同一肌群只计一次：有主练记 1，仅次练记 0.5，跨课次累加成分数。色带：0 灰，≤0.5 低，≤1.5 一般，≤3.5 中，≤6.5 高，其余峰值。正背面 `MuscleFigure` 共用一份 muscleId 颜色表；`heatColors` 为可选参数。训练页肌群高亮仍用原来的红色系，不要和热力色带混用。

- 点一块肌肉可看课次数、动作数、组数、最近日期和动作列表。问号在卡片右上角（不是底部），对话框说明颜色含义。

- 逻辑在 `data/MuscleHeatLogic.kt`；UI 在 `ui/progress/MuscleHeatmapCard.kt` 与 `MuscleHeatViewModel.kt`。

- ReleaseNotes.kt 顶部为 1.6.1（2026年11月4日，节「肌群训练热力图」），`ReleaseHistoryTest` 首条断言为 1.6.1。

- GitHub Release：https://github.com/Akineru/GymLog/releases/tag/v1.6.1 。


## 1.6.2 发版

- versionCode = 22，versionName = 1.6.2（2026-11-08；tag `v1.6.2`，master 已合并 `liquid-glass-home` 全部提交）。无数据库迁移，Room 仍是 21。
- **哑光陶瓷 + 淡蓝紫背景（本版最大改动，接手必读）**：
  - 核心：`ui/theme/CeramicSurface.kt`。`Modifier.appMeshBackground()` 是全 app 唯一的页面背景（淡蓝灰 `#EDF2FC` + 四角色晕 + 顶部天光），在 `GymLogApp` 根节点画一次；`Modifier.ceramicSurface(shape)` 是全 app 唯一的卡片材质（不透明陶瓷 + 釉面高光 + 1dp 浅灰圈边 + 内收投影，深浅色两套色值）。
  - `GymLogCard` 内部改调 `ceramicSurface()`，60 处调用点零改动；`LiquidGlassCard` 函数名保留但渲染色已换成陶瓷。旧的 `LiquidGlassCard.kt`、`glassSurface`、`BackdropBlobs`、`GlassBackdropBlobs`、逐卡重绘模糊已删除——**滚动时不再逐帧高斯模糊**。
  - `Theme.kt` 的 `background` / `surface` 设为 `Color.Transparent`，Scaffold 才不会盖住 mesh；子页面遮罩也要用 `appMeshBackground()`，不要再用 `colorScheme.background`。
  - 首页视觉体系（`ui/home/HomeTheme.kt` 材质 token + `HomeScreen.kt` 重写）：时长大数字、周/月分段器、七列足迹（选中/今天/练过三态）、今日训练汇总卡、无记录空态。详见「首页视觉体系」小节。
  - 动作卡底部新增居中向上箭头手动收起（`KeyboardArrowUp` / `training_collapse_exercise`），摘要卡右端一个 18dp 向下箭头提示可展开。

- **底部导航**（`GymLogApp.kt` `RootTabBar`）：整条白色磨砂栏，56dp + `navigationBarsPadding()`，图标配文字，选中态浅蓝 54×29 圆角底 + 蓝图标蓝文字。内容区底部按**完整栏高**让开（旧实现是 `- 24dp`，会导致最后一卡被压住）。**滑动切页手势已按用户要求删除**，只保留点按。

- **品牌色**：`ActionOrange(#FF6B35)` 已更名为 **`BrandAccent`(#3B7DF8 钴蓝)**，辅色 `BrandAccentSoft`(#6B99FA)；主题 `primary` 是 `BrandBlue`，首页 brand 是 `#4380F5`，三者都是蓝但不同值，别混用。

- **字体**：OPPO Sans 3.0（官方免费商用）打包 `res/font/opposans_{light,regular,medium}.ttf`；`Type.kt` 字重分层（2026-11-09 起）：display*/headline*/title* → **Medium**，body*/label* → Regular。旧的全局 `sans-serif-light` 已废。代码里少量 SemiBold/Bold 会落到最近 Medium。**注意 OPPO Sans 只打包了 Light / Regular / Medium 三档，Medium 是最粗可选。**

- **解释小字清理**：全 app 静态解释性 bodySmall 文案已删（外观/日历配色/通知设置/关于/数据管理共 15 处 UI + 12 条字符串）。保留：输入框 supportingText、空态、对话框警示、`HelpParagraph` 帮助段落。不要再把这类说明小字加回来。

- `ReleaseNotes.kt` 顶部为 1.6.2（三节：首页全新改版 / 材质与配色焕新 / 底部导航与训练细节），已按最终形态覆盖旧文案；`ReleaseHistoryTest` 首条断言为 1.6.2。
- 构建：debug 包已在模拟器（API 37）与实机（OnePlus PJZ110 无线调试 `adb-cdbf33ff-dx3Km2._adb-tls-connect._tcp`）验收；GitHub Release v1.6.2 未上传 APK 附件，仅 tag。无线调试配对一次后长期有效，端口会变（`adb mdns services` 可发现）。

## 数据集去重（已随 1.6.3 发版）

动作数据集里混进了同一个动作的多个版本：`(back pov)` / `(侧面视角)` 视角版、`版本 2` 变体、重复录入、纯翻译差异（`直腿硬拉` vs `直腿 硬拉`）。已裁掉 **37 条**，`dataset_v1.json` 从 1324 条降到 **1287 条**。

- **数据集**：`dataset_v1.json` 删 37 条，`sourceRecordCount` 同步为 1287；对应 72 个 jpg/gif（3.9 MB）已从 `assets/exercises/media/` 删除。顺手修了 3 条名字乱码（`45в°` 里的西里尔 `в` → `°`：0738 / 0739 / 0740）。`0740 腿宽推举` 是宽站距、属于另一个动作，**没有删**；116 条中文名里的多余空格也没动。
- **对照表**：`data/RetiredDatasetExercises.kt` 的 `RETIRED_TO_KEEPER`，37 组「被删 id → 保留 id」，迁移和备份导入共用这一份。
- **Room 22 → 23**（`MIGRATION_22_23`）：`workout_sets` / `exercise_durations` 对 `exercises` 是 RESTRICT，必须先 `UPDATE` 改挂到保留动作再 `DELETE` 动作行；其余子表 CASCADE 自动清。保留动作不在库里的极端情况**不删**，避免孤儿训练组。PR 不用手写，启动时 `rebuildAllPersonalRecords()` 按新归属重算。
- **备份导入**：`GymLogSnapshot.withoutRetiredDatasetExercises()` 在 `importSnapshot` 里先过滤（早于 `validate()`），旧备份恢复后这 37 条不会重新冒出来，引用它们的组和动作用时会改挂到保留动作。
- **单测**：`RetiredDatasetMigrationTest`（改挂 + 缺 keeper 不删）、`RetiredDatasetBackupTest`（过滤 + 无 keeper 丢弃组）。原有测试里硬编码的 `1_324` 全部改成 `1_287`，各迁移测试补上 `MIGRATION_22_23`。
- **坑**：Robolectric 的临时目录路径会把「测试类名 + 方法名」拼进去，方法名太长会超过 Windows 260 字符上限，报 `SQLiteCantOpenDatabaseException`。迁移测试的方法名和库名要短。
- 本次**不动** `versionName` / `versionCode`，只出 debug 包验收。

### 补丁：子表孤儿行（Room 23 → 24）

22 → 23 那版迁移**漏了子表清理**。`exercise_content` / `exercise_media` / `exercise_source_map` / `exercise_muscle_tokens` / `exercise_muscles` 虽然声明了 `ON DELETE CASCADE`，但**迁移期间 Room 不开启外键约束**，级联不触发，删掉 `exercises` 行后这五张表留下了孤儿行（实测手机和模拟器各 37 / 74 / 37 / 150 / 91 行）。

- `GymLogMigrations.kt` 新增 `purgeOrphanExerciseReferences()`：按「子表里找不到对应 exercises 行」删一遍，五张表都清；`MIGRATION_22_23` 末尾调用。
- 新增 `MIGRATION_23_24`（`GymLogDatabase` version = 24）：对已经升到 23 的库补一次同样的清理。数据集去重的实质内容只在 23，所以 24 除了清理不动业务数据。
- 教训：**迁移里不能指望 CASCADE**。以后凡是删 `exercises` 行，都要自己清子表，或者显式 `PRAGMA foreign_keys = ON`（Room 迁移里不建议改）。
- 单测 `RetiredDatasetMigrationTest` 扩到 4 个：改挂、子表不留孤儿、缺 keeper 不删、v23 带孤儿库升 24 被清干净。全量 411 个用例通过。
- 两台设备实测：`user_version = 24`，孤儿合计 0，动作 1381，训练组/会话数不变，2026-10-19 仍是 4 组 2495 kg。
- 仍带「版本 2/3」字样的 16 条中，只有 `0121 杠铃 直立划船 版本 3`（孪生 0120）和 `2318 杠杆机 肩推 版本 3`（孪生 0603）是漏掉的重复 —— 上一轮正则只处理了 `v. 2`，没处理 `v. 3`；另有 `2800 / 2801` 两条「（女）」示范版。这 4 条已确认全删（见下一条）。

### 后补 4 条（Room 24 → 25）

- `dataset_v1.json` 再删 4 条：`0121`、`2318`（`版本 3`）与 `2800`、`2801`（`（女）` 示范版），保留 `0120`、`0603`、`2799`、`2802`。记录数 **1287 → 1283**，媒体再删 8 个文件。
- `RETIRED_TO_KEEPER` 扩到 41 组；`GymLogMigrations.kt` 把原来的循环抽成 `retireDatasetExercises()`，`MIGRATION_22_23` 和新增的 `MIGRATION_24_25` 共用（幂等）。已升到 24 的设备靠 25 再删一次，`GymLogDatabase` version = 25。
- 两台设备实测：`user_version = 25`，动作 1381 → **1377**，孤儿 0，训练组/会话数不变，2026-10-19 仍 4 组 2495 kg；动作库界面显示 1287 个动作。
- 单测 411 个通过；数据集统计常量同步为 1283 / 2418 / 2566 / 5063 / 1970。

### 界面显示动作名：用 originalName（2026-11-09）

动作名有两套：`name` 是数据集原名（`雪橇机 45° 腿举` 这种带器械/负重/姿态的），`originalName` 是剥掉这些之后的动作本体（`45°腿举`）。**给人看的标题一律用 `ExerciseEntity.originalOrName()`**（`originalName` 为空才回退 `name`），搜索、排序、去重仍然用 `name`。

- 已经用 `originalOrName()` 的：首页本周训练足迹（`HomePeriod`）、历史/今日训练列表、动作库详情标题、会话编辑页动作卡片（`SessionEditorScreen` 两处：折叠态 702 行、展开态 823 行，2026-11-09 改）。
- 历史训练详情页（`GymLogApp.kt` 的训练卡片）本来就用 `originalOrName()`。
- 仍用 `name` 的地方（统计/热图/肌群图/趋势里的动作名）**是有意的**：那些列表是按 `name` 分组和去重的，换成 `originalName` 会让同名不同器械的动作并成一行。改之前先确认分组键。

### 悬浮条（2026-11-09）

「锻炼中」`ActiveWorkoutMiniBar` 改成真正浮起来：圆角 16 → **20dp**，离底栏 **12dp**（`GymLogApp.kt` 的 `padding`：左右 16 / 上 8 / 下 12），投影用 `ceramicSurface(floatingElevation = 12.dp)`。

`ceramicSurface` 新增可选参数 `floatingElevation: Dp? = null`：为空时沿用原来的贴地投影（亮色 5dp、暗色 0dp、`0x14202B40`）；传值时用该高度投影，阴影色换成 `0x4A1F2B3F`，暗色主题也给投影。**默认值不变，其他卡片不受影响。** 需要新的悬浮卡片时传这个参数，别另写一套 shadow。

### 「锻炼中」悬浮条：内容穿透 + 收起（2026-11-09 二次调整）

上一次只改了间距和投影，问题没解决：内容区底部留的是「底栏 + 悬浮条」的总高度，所以内容被硬截在卡片上沿，看着像贴在底部的条块。

- **内容穿透**：`GymLogApp.kt` 里 `measuredBarHeight` 改成**只量 RootTabBar**（`.onSizeChanged` 从外层 Column 挪到底栏上）。内容区只给底栏让高度，悬浮条不再占位，内容从卡片背后穿过去，卡片上下都能看见内容。已知取舍：滚到最底时最后一行会被卡片盖住一截（用户已确认接受）。
- **收起/展开**：`ActiveWorkoutMiniBar` 新增 `collapsed` / `onToggleCollapse`。展开态右侧箭头（`KeyboardArrowDown`）点一下收起；收起态变成右下角圆角胶囊，只显示绿点 + 时长，点一下展开。切换用 `Crossfade`（180ms）。收起状态在 `GymLogApp` 用 `rememberSaveable` 保存，切页不自动展开；重启 App 草稿丢失时自然复位。
- **点击语义变了**：进训练不再是右侧箭头，而是**点卡片主体**；垃圾桶（取消训练）和收起箭头各自响应，不触发进入。
- 新增字符串 `training_collapse` / `training_expand`。
- 组件拆分：`ExpandedActiveWorkoutCard` / `CollapsedActiveWorkoutPill` 两个私有 composable，`ActiveWorkoutMiniBar` 只做状态切换。

### 历史/日历页动作行：名字列变宽（2026-11-09 第三次调整）

用户在实机上看到动作名被挤成一行两个字（`引体 / 向上`），容量和动作用时都被截成 `容…` / `动作用`。

- 原因：手机是 360dp 宽（1440×3168、密度 640），卡片内可用宽度只有 312dp；原来是「名字列 weight(1f) + 缩略图定宽列 112dp + 组数区 164dp」，名字列只剩 **36dp**，约 2 个字。
- 改法：`GymLogApp.kt` 的 `SessionExerciseRow` 改成跟训练详情页一样的比例——缩略图 48dp + 左右各 8dp（去掉定宽居中列），组数区 **164 → 124dp**；名字列仍是 `weight(1f)`，实际分到 124dp（约 7 个汉字），超长换行、`maxLines = 2` 不变。删掉 `SESSION_THUMBNAIL_COLUMN_WIDTH`，新增 `SESSION_SET_GRID_WIDTH = 124.dp`，`SESSION_THUMBNAIL_SIZE` 64 → 48dp。
- 组数区收到 124dp 后每列 62dp，`73.2 kg×13` 这类最长串约 55dp，仍放得下；如果以后出现更长的（三档小数 + 四位数重量）先量一遍再加宽。
- 只改了历史/日历页，训练详情页本来就是这个比例，没动。
- 手机实测：`哈克深蹲`、`髋外展`、`45度一腿举` 都一行显示完整，容量和动作用时不再被截。

### 设置页文案改名（2026-11-09）

- 设置页顶部大类「偏好设置」→ **通用**（`preferences_title`），组内仍是外观 / 偏好 / 通知三个入口。
- 行标题与二级页标题「训练偏好」→ **偏好**（`training_preferences_title`），说明弹窗「关于训练偏好」→ **关于偏好**（`training_preferences_help_title`）。
- 键名没动（仍叫 `training_preferences_*`），只改文案；`values-en` 同步为 Preferences / About preferences。

### 修复：休息倒计时归零后再调时长会叠加（2026-11-09）

现象：休息 2 分钟跑到 0，不按「重置」，直接按 4 次「+30 秒」再开始，通知显示「已歇 02:54 / 04:00」——总时长按 2+2 叠加了。

- 根因在 `WorkoutClockLogic.adjustTimer`：它把 delta 同时加到 `durationMillis`（总时长）和 `displayMillis`（剩余）上。到点后剩余是 0、总时长还留着上一次的 120000，于是每次 +30 秒都在旧总时长上继续累加。
- 修法：`displayMillis <= 0` 时视为**重新配置**，总时长和剩余都从 0 开始算 delta（+4 次就是 2:00 / 2:00）；其它情况（未启动时 display == duration、跑到一半）保持原逻辑不动。
- `SessionEditorViewModel.adjustTimer` 同时把 `started` 在这一刻复位成 `false`，避免通知里出现「已歇 00:30 / 00:30 · 已暂停」这种没歇过的播报；下一次点「开始」才置位。
- 单测：`WorkoutClockLogicTest` 新增两例（归零后累加、跑一半加时），全量 413 个通过。
- 语义边界：跑到一半按 − 减到 0 之后再加，同样按「重新配置」处理；未启动时按 ± 仍然两边同步变。

### 动作别名：常见叫法 + 器械别名（2026-11-09）

需求：库里搜不到「倒蹬」「龙门架」「蝴蝶机」「夹胸」「登山机」这些本地叫法，给动作补上别名。

- **存储**：还是 `exercises.aliases`（逗号分隔，`ExerciseAliases` 读写）。搜索本来就走 `name / nameEn / aliases` 三路 LIKE，所以填进去立刻能搜到；动作详情页也会显示「别名：…」。**没有新增表、没有 Room 迁移。**
- **规则**（`data/DatasetExerciseAliases.kt`）：三张表按顺序拼，只追加、不覆盖名字里已有的词，每条最多 6 个：
  1. 动作族（关键词 → 别称）：飞鸟→夹胸、腿举→倒蹬、下拉→高位下拉、引体→引体向上、臀推⇄臀冲、挺身→山羊挺身、平板→平板支撑、弓步→箭步蹲、波比→立卧撑、空中蹬车→自行车卷腹、登山者→登山跑、卧推→平板卧推、硬拉→传统硬拉、深蹲→后蹲、弯举→二头弯举、下压→三头下压、侧平举→侧举、推举→肩推、提踵→小腿提踵。带 `exclude`：上斜/下斜卧推不算平板卧推，罗马尼亚/直腿硬拉不算传统硬拉，前蹲/哈克不算后蹲。
  2. 器械（本地叫法 ≠ 数据集用词才写）：cable→龙门架/钢索、band→阻力带、resistance band→弹力带、smith→史密夫机、stability ball→瑞士球/健身球、bosu→波苏球、ez barbell→EZ杆/曲杆、trap bar→六角杠、stepmill→登山机/爬楼机、elliptical→交叉训练机等。哑铃/杠铃/壶铃这种同名的**故意不写**，否则全是噪声。
  3. 器械 × 动作：杠杆机只在夹胸用法才叫蝴蝶机（`杠杆机 坐姿 飞鸟`），腿举才叫倒蹬机；雪橇机腿举=倒蹬、哈克=哈克机；cable+飞鸟=龙门架夹胸。
- **落库**（`data/DatasetExerciseAliasSeeder.kt`）：只填「数据集动作 + 别名还是空」的行，用户写过的别名一律不动，重复执行是空操作。挂在 `ExerciseCatalogImporter` 的两处（已导入的早退分支 / 导入成功后），所以首次安装、强制刷新、从旧备份恢复后都会补。
- **实测覆盖**：1283 个数据集动作里 **633 个**拿到别名，共 **1025 条**。手机库验证：`倒蹬` 8 条、`龙门架` 157 条（全部绳索动作）、`夹胸` 38、`高位下拉` 27、`平板卧推` 18、`蝴蝶机` 3、`传统硬拉` 7、`登山机` 1。
- **取舍**：剩下 650 条是哑铃/杠铃变式和拉伸/绕环/走路类，别名只会和名字重复或纯噪声，故意留空。另外用户把别名**删空**后，下次启动会被补回规则别名；要彻底清掉得另加标记列，暂不做。
- 单测：`DatasetExerciseAliasesTest` 8 例 + `ExerciseCatalogImportTest.importFillsColloquialAliasesButKeepsUserOnes`（验证落库 + 不覆盖用户别名）。全量 422 通过。

### 动作要点：怎么做 + 动作细节（2026-11-09）

需求：给动作库每个动作补「动作怎么做」和「动作细节」（发力方式、身体姿态、动作幅度、节奏呼吸、常见错误纠正）。

- **不落库**（用户拍板）：内容是「按动作模式和内联数据集步骤推导的通用知识」，不是个人数据。详情页每次打开现算 → 数据库和备份都不变大（否则 1283 条约 1.2 MB），改规则全库立即生效，用户手写的 `howTo`/`cues` 永远优先。
  - 界面上：`exercise.howTo?.trim()?.ifEmpty { guide.howTo }`，两个字段都为空时底部显示「内容按动作模式自动整理…」小字；编辑框 placeholder 写「留空则显示自动整理的内容」。
- **「怎么做」来源**：`exercise_content.instructionStepsZh`（数据集自带，1283 条全有，平均 5.8 步，之前库里躺着但界面从没显示过）。清洗规则在 `ExerciseGuideBuilder.buildHowTo`：删掉「重复所需…」「根据您的健身水平…」这类占位句（删完仍每个动作 ≥3 步）、`电缆机→龙门架`、`滑轮机→滑轮`、`长凳→平凳`、去重、最多 6 条、编号输出。
- **「细节」来源**：`data/ExerciseGuideRules.kt` 的 40 个动作模式（卧推/俯卧撑/飞鸟/划船/硬拉/深蹲/腿举/弯举/卷腹/平板支撑/拉伸/有氧…），每个给 `drive`（发力）+ `posture`（姿态与幅度）+ `errors`（常见错误）+ 可选 `tempo`（覆盖默认节奏）。四段拼成：
  `发力：…` / `姿态与幅度：…` / `节奏与呼吸：…` / `常见错误：…`
  - 器械再追加一句：绳索（滑轮高度/配重砸底）、器械类（座椅对准转轴）、弹力带（固定点/回弹）、自由重量（重量以能控制为准）；附带 1–2 条器械特有的常见错误。
  - 默认节奏按 `weightMode`：负重「向心 1–2 秒、离心 2–3 秒」，自重「全程控制速度」；发力呼气、回程吸气。
  - **识别不出模式**（约 4%，46 条，多为「咒术师」「伦敦桥」这类笼统名）→ 按 `bodyCategoryId` 给部位级通用版（胸/背/肩/上臂/前臂/大腿/小腿/腰/有氧），宁可短也不编造。
- **关键词匹配**：动作名去空格后做包含匹配，按列表顺序先具体后笼统；`exclude` 用来挡误伤（「腿伸展」不算拉伸、「内收肌拉伸」不算髋内收、「地面拉伸」不算面拉）。改关键词后 `ExerciseGuideTest.mostDatasetExercisesMatchAMovementPattern` 会守住识别率 ≥90%。
- 数据通路：`ExerciseDao.observeInstructionSteps`（跨表查 `exercise_content`，避免给 `ExerciseRepository` 多加 DAO 构造参数牵动十几处测试）→ `ExerciseRepository.observeInstructionSteps` → `ExerciseDetailViewModel.instructionSteps`（独立 StateFlow，不进 uiState 的 5 路 combine）→ `ExerciseDetailScreen` 里 `remember(exercise, steps)` 算一次。
- 测试：`ExerciseGuideTest` 12 例（步骤清洗/术语统一/上限截断/坏 JSON/卧推四段/绳索器械提示/自重节奏/部位兜底/无信息为空/数据集识别率 ≥90%/每个模式三段文案非空）。全量 434 通过。
- 模拟器实测：`杠铃 卧推`、`绳索 下压（直臂）`、`前后 步` 三页排版正常；`前后 步` 原本被归到「有氧」给成跑步要领，已改归「弓步」。

### 去掉要点页底部「自动整理」说明（2026-11-09）

- 要点页内容区下方原来有一行灰字「内容按动作模式自动整理，可在右上角「编辑资料」里改成自己的版本。」用户要求删掉。
- 删了 `CuesTab` 里的 `autoGenerated` 判断与那段 `Text`，同步移除 `exercise_detail_cues_auto` 两个语言串。内容生成逻辑、用户编辑优先的规则都没动。
- 编辑框 placeholder「留空则显示自动整理的内容」保留（`exercise_detail_edit_auto_hint`）。

### 动作详情总览页 + 编辑弹窗改版（2026-11-09）

用户觉得详情页「很粗糙」，指定 grill-me + impeccable。结论：不新增功能，只重排现有元素，沿用既有哑光陶瓷语言。

**总览页**（`ExerciseDetailScreen.kt`）
- 头部：原来是 280dp 通栏 GIF + 底部黑色渐变压白字。问题是动图本身白底、留边用 surfaceVariant → 两侧露出两道灰条，黑遮罩又把下半截压脏。改成 `GymLogCard(shape = panel)` 名片：动图 `height(196.dp)`、`containerColor = surfaceContainerLowest`（白底留边不可见），名字（titleMedium 19sp SemiBold，深色）和部位/器械胶囊排在图片下方；`BannerChip` 从「黑底白字」改成「淡主色底 + 主色字」。
- `ExerciseGifPlayer` 新增可选参数 `containerColor`（默认 surfaceVariant，行为不变），只有头部传白。
- 别名：从「别名：A、B、C」一行文字改成 `FlowRow` 里的描边胶囊 `AliasChip`，超长自然换行。
- PR：`PrRow`（左标签 + 右侧成绩和日期挤一行）换成 `PrColumn` + 两栏并排（`StatDivider(height = 44.dp)` 分隔），标签小字在上、成绩放大、日期最淡。
- 肌群：原来人形图和右侧文字是裸的 Row，右侧还顶着重复的「训练肌群图」标题。改成整块 `GymLogCard`：左人形图 `boundedHeight 200.dp`（weight 0.5），右 `MuscleRoleBlock`（标签在上、肌肉名 bodyLarge 放大，次要肌群用浅色）。删掉 `muscle_map_title` 的使用。
- 节奏：内容区 padding 从「四周 16dp + spacedBy(12dp)」改成 `padding(horizontal = 16.dp)` + 显式 Spacer，卡片之间统一 12dp、页尾 20dp，头部名片与胶囊之间 8dp。

**编辑弹窗**
- 从 `AlertDialog`（三个重边框 OutlinedTextField + 居中按钮）改成自定义 `Dialog`（`usePlatformDefaultWidth = false`）+ Surface(panel 圆角)：只有标题，不再有副标题（原「只影响你自己看到的版本」小字已按用户要求删除，`exercise_detail_edit_subtitle` 资源一并删掉）；输入项用新组件 `EditField`（标签在框外、淡底无边框 TextField、圆角 12dp）；底部按钮右对齐，保存用填充主色按钮。
- 文案：别名下面那句「N 个别名，用逗号、顿号或分号分隔」后来整条删掉，见下节「别名编辑改成卡片」。
- 取向：详情页各处不要再加这类解释性小字，用户明确不喜欢。

- 单测 434 全通过；模拟器实测总览页与弹窗，手机已装 23:19 包（弹窗副标题已删）。

## 别名编辑改成卡片

编辑弹窗里的别名从「逗号分隔文本框」改成卡片列表（用户给了效果图确认后实施）。

- `AliasEditor`：标签 + 淡底容器（`surfaceVariant` 35% 透明、圆角 12dp、内边距 10dp），里面是 `FlowRow`（间距 8dp）。
- `AliasCard`：高 34dp、圆角 11dp、白底 + 1dp `outlineVariant` 描边、文字 13sp 单行、超长 `widthIn(max = 190.dp)` + 省略号；右上角是 8dp 细线叉（`CrossIcon`），颜色 `onSurfaceVariant` 60%。
- `AliasAddTile`：虚线「＋」（`drawBehind` + `PathEffect.dashPathEffect`，注意描边要向内缩 0.75dp，否则被 `clip` 切掉一半），常态 40×34dp，没有别名时 `fillMaxWidth()` 占满整行；淡主色底 6%。
- `AliasDraftField`：132×34dp、1.5dp 主色描边的 `BasicTextField`（单行、自动聚焦、光标主色），占位「新别名」。
- 交互：回车（IME Done）= 收成卡片并保持输入态，可以连着加几个；点别处失焦 = 收成卡片并收起输入框；空白与重复（忽略大小写）不入列。
- **踩过的坑**：`Modifier.clickable` 会被系统补到 48dp 热区，26dp 的叉因此压到相邻卡片的「＋」上（uiautomator dump 看到的 bounds 就是 48dp）。叉和＋都改成 `pointerInput + detectTapGestures`，无障碍用 `semantics { role = Role.Button; onClick { …; true } }` 自己补。
- 保存仍走 `ExerciseAliases.join(list)`，存储还是逗号分隔的原字段，备份与旧数据不受影响；进弹窗时用 `ExerciseAliases.parse` 初始化。
- 资源：删 `exercise_detail_edit_aliases_hint / _empty / _count`；新增 `exercise_detail_edit_alias_add`（添加别名，也是虚线按钮的无障碍标签）、`_alias_hint`（新别名占位）、`_alias_delete`（删除别名）。
- 弹窗 Surface 加了 `imePadding()`，键盘弹出时整体上移，不挡输入框。
- 验证：模拟器实测点＋→输入→回车连加两张卡、点×删除、点空白收起、保存后详情页三枚别名胶囊正常；单测 434 通过；手机与模拟器均已安装（23:59）。
  说明：用 `adb input keyevent 66` 走的是硬件回车，系统键盘不会自动收起；软键盘 Done 按钮的收起行为未单独验证，属于注入方式差异，与弹窗逻辑无关。

## 1.6.3 发版（2026-11-10）

- versionCode = **23**，versionName = **1.6.3**，tag `v1.6.3`。Room **22 → 25**（三次迁移），数据集 **1283 条**。
- **数据库迁移（必读）**：`MIGRATION_22_23` 删 41 条重复数据集动作并改挂训练组，`MIGRATION_23_24` 补清五张子表的孤儿行，`MIGRATION_24_25` 再退休 4 条（`版本 3` 与女版示范）。三份都调 `retireDatasetExercises()`，幂等。**迁移里不依赖 CASCADE**（Room 迁移期间外键不生效），删 `exercises` 行必须自己清子表。对照表 `data/RetiredDatasetExercises.kt` 的 `RETIRED_TO_KEEPER` 是迁移与备份导入共用的唯一来源；保留动作不在库里的极端情况不删行，避免孤儿训练组。
- **动作名两套字段**：`name` 是数据集原名（带器械/负重/姿态），`originalName` 是剥掉这些的动作本体。给人看的标题一律 `originalOrName()`；搜索、排序、分组仍用 `name`（换掉会把同名不同器械的动作并成一行）。`ExerciseNameCleaner` 负责生成 `originalName`。
- **别名**：`DatasetExerciseAliases`（三张规则表：动作族 / 器械 / 器械×动作）+ `DatasetExerciseAliasSeeder`（只填「数据集动作 + 别名空」的行，不覆盖用户值，幂等），挂在 `ExerciseCatalogImporter` 两处。当前 633/1283 有别名、共 1025 条。用户把别名删空后下次启动会被补回，属已知取舍。
- **动作要点**：`ExerciseGuideBuilder.buildHowTo`（清洗 `exercise_content.instructionStepsZh`）+ `buildCues`（`ExerciseGuideRules` 的 40 个动作模式，四段：发力 / 姿态与幅度 / 节奏与呼吸 / 常见错误，器械再追加一句）。**运行时生成、不落库**，用户手写的 `howTo` / `cues` 优先；数据通路是 `ExerciseDao.observeInstructionSteps`（跨表查）→ `ExerciseDetailViewModel.instructionSteps` 独立 StateFlow（不进 5 路 combine）。改关键词后 `ExerciseGuideTest` 会守住识别率 ≥90%。
- **动作详情页**：头部 `GymLogCard(shape = panel)` 名片（GIF 196dp、白底 `containerColor`），别名 `FlowRow` 胶囊，PR 两栏，肌群整卡左图右文；编辑弹窗是自定义 `Dialog(usePlatformDefaultWidth = false)` + `EditField`，别名的卡片编辑器见上一节（`clickable` 会补 48dp 热区，叉和加号用 `pointerInput`）。
- **口径**：详情页不再加解释性小字（用户明确不喜欢）；历史/日历动作行是「名字 weight(1f) + 48dp 缩略图 + 124dp 组数」。
- `ReleaseNotes.kt` 顶部为 1.6.3（六节）；`ReleaseHistoryTest` 首条断言为 1.6.3。
- 构建与发布：仓库**没有 signingConfig**，`assembleRelease` 产出的是未签名包，实际安装/公开分发用 `:app:assembleDebug` 的 debug 包（`GymLog-1.6.3.apk`，约 165 MB，附在 GitHub Release `v1.6.3` 下）。发版脚本顺序：改版本号 → ReleaseNotes + 断言 → README 版本 → 本文件 → `testDebugUnitTest` → `assembleDebug` → 提交 + tag + push → `gh release create` → `adb install -r`。

## 1.6.3 的 Release 正文

与 `ReleaseNotes.kt` 六节同源（动作库瘦身与更名 / 搜得到本地叫法 / 动作要点 / 动作详情页改版 / 训练与记录 / 修复与其他），末尾补一行版本与迁移信息。

## 肌群热力图从统计页挪到训练页（2026-11-10）

用户要求把训练热力图放到训练页、「今天练什么？」卡片下面。

- `ui/GymLogApp.kt`：`MuscleHeatViewModel` 从 `RootTab.Progress` 的 `TabPane` 移到 `RootTab.Train` 的 `TabPane`，作为 `heatState` / `onHeatRange` 传给 `TrainingHomeScreen`；`ProgressScreen` 不再接这两个参数。
- `ui/train/TrainingHomeScreen.kt`：LazyColumn 里新增 `item(key = "muscle-heat")`，位置在 `start`（「今天练什么？」+ 开始训练）之后、「动作库」之前。**开练时 `start` 卡会隐藏（`hasActiveWorkout`），热力图会顶到第一位**——这是刻意的降级顺序，不要再加别的判断。
- `ui/progress/ProgressScreen.kt`：删掉热力图 item，统计页现在只剩「训练足迹」「身体数据」两个入口行。
- 组件本身（`ui/progress/MuscleHeatmapCard.kt`）没改，仍是 `internal`：同模块跨包可用，`ui.train` 直接引用即可。
- 验证：模拟器实测顺序为「今天练什么？→ 肌群训练热力图 → 动作库 → 训练计时器」；点肌肉弹出底部详情（臀大肌 / 1 课次 / 1 动作 / 1 组 / 最近一次 11月8日 / 杠铃 硬拉·主要肌群）；周/月切换后日期区间与颜色同步变化。

## 肌群热力图恢复分段（2026-11-10）

热力图分段从「最近一周 / 最近一月」扩成三段，第三段是**恢复**：同一组人体图按恢复状态着色，点肌肉看恢复信息。

- **判定**（`data/MuscleHeatLogic.kt`）：新增 `MuscleHeatRange.RECOVERY`、`MuscleRecoveryState{RECOVERING, RECOVERED, STALE}`、`MuscleRecoveryInfo(state, lastDateEpochDay, expectedRecoveryEpochDay)`。
  - 窗口写死：小肌群（`SMALL_MUSCLES` 9 个：三角肌三束、肱二/三头、前臂、腹直肌、腹外斜肌、小腿三头肌）2 天；其余 3 天。**不在这张表里的 id（含未知）一律按大肌群**——一开始写成「大肌群白名单」导致未知 id 落到小肌群，有单测守着。
  - 最近一次只以次要角色出现 → 窗口 / 2（整数除法，3/2 得 1 天）；既有主练又有次要按主练算。
  - `days = today - lastDate`：`< window` 恢复中，`<= STALE_DAYS(14)` 已恢复，`> 14` 久未训练；从没练过的不进表、图上不着色。
  - 入参必须是**全量 touches**，不能先按周 / 月窗口过滤。
- **配色**（`MuscleHeatmapCard.recoveryColor`）：恢复中 **浅绿** `#A5D6A7`（暗色 `#C8E6C9`）、已恢复 **深绿** `#2E7D32`（暗色 `#66BB6A`）、久未训练 灰 `#7C8698`（暗色 `#AEB6C4`，alpha 0.85）。
  - **踩坑**：灰最初用 0.45 alpha，在人体图上和未着色底几乎分不出来，必须 0.85 才能看出「久未训练」和「从没练过」的区别。
  - 用户明确选的是「恢复中浅绿 / 已恢复深绿」（不是文档原稿的红/绿/灰），红只留给周月的热力档位，三种着色不同屏。
- **动作列表口径**（与文档不同的地方）：恢复模式下 `stats` 不再用 `period` 区间聚合，而走新增的 `MuscleHeatLogic.latestSessionStats(touches)`——每块肌肉只保留**最近一次训练那天**的记录再 summarize。否则 `period` 是「今天..今天」，详情里的动作只剩今天练的，和「上次训练 11月8日」自相矛盾。
- **卡面**：分段器第三段「恢复」；日期行换成「按最近一次训练估算 · 截至 M月d日」；问号对话框在恢复段换成三色说明（`muscle_heat_help_recovery_body`，写明「按训练时间推算的参考，不是生理测量」）。标题仍是「肌群训练热力图」（用户要求不动）。
- **底部详情**：恢复模式把「课次 / 动作数 / 组数」三格换成 `RecoverySummary`——状态圆点 + 状态名、上次训练、预计恢复（预计恢复 = 上次 + 窗口，会早于今天，属于「早就过了」的正常显示）；下面的「训练动作」列表保留。
- **英文资源没加**：`values-en/strings.xml` 里连 `muscle_heat_*` 都没有（英文只覆盖应用名/通知等），不为这一次单开一半翻译。
- 单测：`MuscleHeatLogicTest` 新增 7 例（大肌群第 3 天边界、次要减半、主次混合按主练、STALE 第 14/15 天边界、从未练过不入表、未知 id 按大肌群、`latestSessionStats` 只留最近一天）。全量 441 通过。
- 模拟器实测：三段可切换；恢复段呈现浅绿（臀大肌）/ 深绿（腘绳肌、竖脊肌、肱二头）/ 灰（背阔肌、斜方肌、小腿）/ 无色（未练过）；点臀大肌显示「恢复中 · 上次训练 11月8日 · 预计恢复 11月11日 · 杠铃 硬拉·主要肌群」，点背阔肌显示「久未训练 · 10月20日」；切回最近一周红热力与日期区间都复原。
