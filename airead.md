# GymLog / 练有所迹 — 接手说明

本地离线的 Android 力量训练记录应用。单 Activity，Kotlin + Jetpack Compose + Room。没有账号、没有网络同步。界面中文；启动器英文名是 Traivol（`values-en/strings.xml` 的 `app_name`），中文名是「练有所迹」。

当前版本：`versionName` 1.5，`versionCode` 10。包名 `com.example.gymlog`。数据库文件 `gymlog.db`，Room version **19**。

下一个实现者先读这份文档，再改代码。用户用简体中文沟通。版本说明写在 `ui/settings/ReleaseNotes.kt` 的 `ReleaseHistory.notes`，新版本插在列表最上面。关于页读安装包的 `versionName`。

## 硬约束

- 升级数据库只用 Room `Migration`。禁止 `fallbackToDestructiveMigration`，禁止为了改表而卸载或清空用户数据。
- 用户主动点的「清除本地数据」是唯一允许清空训练库相关表的路径，而且必须保留现在的两步确认。清除**不会**删除照片墙文件与 `photo_wall` 行（见「照片墙」）。
- 不要引入 Navigation Compose。导航是 `GymLogApp.kt` 里的内存状态 `Screen`。
- 不要改备份格式的已有字段含义。可以加可选字段，旧文件必须还能导入。当前 `BackupCodec.VERSION` 是 **2**。version 1 没有计时和头像键，仍然要能导入。`dayKinds` 是可选数组，旧文件没有该键当成空列表。照片墙**不进**备份 JSON。
- 数据集动作缩略图/GIF 版权归 Gym visual，按 180×180 授权随 APK 打包；备用示意图是 Bryl Lim / Everkinetic，CC BY-SA 4.0。不要重新生成，不要删掉动作库和关于页的署名字符串 `exercise_art_credit`。人体底图和肌肉遮罩是另一套素材，同样不要重画、不要改 `muscleId` 去凑文件名。
- 页面里的业务文案（训练记录、容量、PR 等）不要随启动器改名一起改掉。
- 明确没做、先不要做的：记组复制、组间休息计时、估算 1RM、热身组。已经做的「训练计时器」是整段训练的秒表，不是组间休息。训练计划、训练工具入口已删除，不要再以占位页形式加回。

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
  ui/GymLogApp.kt              导航、五个根页面、日历上的训练卡片与日类型
  ui/home/                     首页：周/月汇总、当天足迹
  ui/train/                    训练入口（动作库、训练记录）
  ui/session/                  编辑一次训练
  ui/exercise/                 动作库与示意图
  ui/body/                     身体数据（展示/编辑/历史）
  ui/muscle/                   人体底图、遮罩高亮、围度标注
  ui/photo/                    照片墙、详情、编辑器
  ui/stats/                    趋势图
  ui/progress/                 「趋势」入口
  ui/timer/                    训练计时器
  ui/settings/                 「我的」、设置、数据备份、历史记录、关于、头像裁剪
  ui/calendar/                 月历展开、日类型选择器
  ui/theme/                    主题、部位颜色、卡片样式
app/src/main/res/drawable-nodpi/   diagram_<preset_key>.png，以及 muscle_*_base
app/src/main/res/xml/photo_file_paths.xml   照片墙 FileProvider 路径
app/src/main/assets/muscle_masks/  front|back/<muscle_id>.png，1024×1536
app/src/test/                    逻辑与迁移测试，不覆盖界面导航
```

1.4 / 1.4.1 / 1.4.2 相关界面文件：

- `ui/settings/BackupDataScreen.kt`：导出/导入和导入确认、结果提示；文案说明照片墙不进备份。
- `ui/settings/SettingsHistoryScreen.kt`：设置 → 历史记录菜单（计时记录、身体记录）。
- `ui/timer/TimerHistoryScreen.kt`：已保存计时列表，点条目进入 `SavedTimerScreen`。
- `ui/body/BodyRecordScreen.kt` 的 `BodyRecordHistory` 是 `internal`，供设置里的身体记录复用。
- `ui/photo/PhotoWallScreen.kt`、`PhotoEditor` 相关、`PhotoWallViewModel.kt`：照片墙。
- `ui/calendar/DayKindSelector.kt`：休息日 / 放纵日 / 偷懒日选择。
- `data/CalendarDayColor.kt`：小日历与展开月历共用的上色逻辑。

依赖方向：界面 ViewModel → Repository → Dao。重量、PR、趋势、日历排序/上色、首页汇总、计时、日类型纯逻辑放在 `data/` 里，测试打这些函数。

## 信息架构（当前）

冷启动在「首页」。底部栏只在五个根页面出现：

| 根页面 | 内容 | 再进去 |
| --- | --- | --- |
| 首页 | 按周或按月看训练时长、训练天数、总组数、容量；本周训练日；选中日按部位的足迹和已保存计时。顶栏是「首页」，齿轮进设置 | 开始训练会把日历选到今天并切到日历；也能打开当天训练、已保存计时、身体数据、趋势、日历 |
| 日历 | 月历（按部位容量或特殊日上色）、选中日期的训练卡片、日类型（休息/放纵/偷懒）、右下角加号、训练计时入口。顶栏标题用 `home_title`，跟界面语言走，现在是「练有所迹」。点标题直接进关于。桌面图标仍用 `app_name`，系统语言为英文时是 Traivol | 展开整月、编辑训练、选动作、计时器、已保存计时 |
| 训练 | 两个入口：开始训练、动作库；有活动训练时底部显示可恢复的缩小卡片 | 开始训练直接打开今天的新训练草稿；返回训练页会保留并继续计时；动作库是子页面，有返回 |
| 趋势 | 「训练足迹」「身体数据」 | 对应趋势页 |
| 我的 | 用户名、头像、可切换的统计、「身体数据」与「照片墙」入口；资料表单可改性别 | 身体数据、照片墙（网格→详情→编辑）。头像在这一页裁剪 |

五个根页面的齿轮都进同一套设置。设置里是外观、数据、关于、清除本地数据；数据下有两个子页面：「数据备份」（导出/导入）和「历史记录」（计时记录、身体记录）。关于再进版本更新。从日历标题打开的关于，返回回日历，不经过设置。

子页面隐藏底部栏。五个根页面用 `TabPane` 一直留在组合里：切 tab 不新建 ViewModel，日历选中日期、首页周月选择和列表滚动保留。子页面盖在上面。

`Screen` 要点：

- 冷启动是 `Screen.Home`。
- `SessionEditor` 带 `draftKey` 和 `returnTab`。新建记录的 key 是 `new-<uuid>`，编辑已有记录用 session id。ViewModel key 是 `"${draftKey}-$dataEpoch"`。日历上新建或编辑，保存后回日历并停在原来的日期。首页点进某次训练，`returnTab` 是首页。
- `PickExercise(returnTo)` 回到原来的 `SessionEditor`，草稿还在。选择器 ViewModel key 是 `picker-${draftKey}-$dataEpoch`，与训练 tab 的动作库 `library-$dataEpoch` 分开；新建训练不会继承之前的搜索/筛选。
- `CalendarExpanded` 返回日历。点某一天会选中它并回到日历。
- `Settings` / `BackupData` / `SettingsHistory` / `TimerHistory` / `BodyHistory` / `About` / `ReleaseNotes` 带 `from: RootTab`。返回回到打开设置时的那个根页面，不在五个 tab 之间做返回栈。`About` 和 `ReleaseNotes` 还有 `openedFromSettings`。
- `TimerHistory` 进入 `SavedTimer` 时带 `returnToTimerHistory = true`，详情返回计时记录列表；普通 `SavedTimer` 仍返回日历/原根页面。
- `BodyRecord` 和 `MuscleMap` 带 `returnTab`。首页和「我的」都能进身体数据。肌群图返回身体数据展示，再返回原来的根页面。
- `PhotoWall` → `PhotoDetail(id)` → `PhotoEditor`。详情/编辑返回照片墙；照片墙返回「我的」。归在 `RootTab.Mine`。
- `SavedTimer` 带 `returnTab` 和可选的 `returnToTimerHistory`。计时器页本身返回日历。
- 根页面上系统返回直接退出应用。子页面的系统返回与该页顶栏返回一致。
- 对话框、日期选择、下拉菜单、头像裁剪打开时，第一次返回只关这一层。页面级 `BackHandler` 在浮层打开时必须 `enabled = false`。导入确认在「数据备份」页里，打开时先关对话框。

导入或清除数据后 `BackupRepository.dataEpoch` 加一。身体数据、两条趋势、动作库、训练编辑、「我的」的 ViewModel 都把 `dataEpoch` 放进 key，从而丢掉过期草稿。「我的」的 key 是 `mine-$dataEpoch`。`DataTransferViewModel` 现在在数据备份页用固定 key `data-transfer`，不要加 `dataEpoch`，否则导入成功提示会丢。首页 ViewModel 只记周/月和选中日，不放 `dataEpoch`。计时器 ViewModel 是进程内单例，进行中的秒表不跟 `dataEpoch` 走；计时记录页和设置历史页共用它。照片墙 ViewModel 当前不绑 `dataEpoch`（清除/导入也不动照片墙）。

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

动作默认模式若存的是 `BODYWEIGHT_PLUS`，新开一组时 `defaultModeForExercise` 仍落成 `WEIGHTED`。引体等预设保持 `BODYWEIGHT`，用户自己把这一组切到自重负重。

新建训练里第一次加入某个动作：优先抄本草稿里这个动作的最后一组；否则抄历史里该动作最新一次训练的最后一组（日期、会话创建时间、`setIndex` 都取最新）。从没练过则为空。组 id 必须是新的 UUID。同一条草稿里再加一组，抄上一组草稿。

训练记录卡片上的组是 2 列 × 3 行，最多 6 格。1–6 组显示真实数据；超过 6 组时前 5 组照常显示，第 6 格是 `…`。逻辑在 `sessionSetGridCells`。数据库中的组不截断，详情/编辑仍能看到全部。

## PR、日历、趋势、首页

PR 在写入前先把每组换成当天的有效重量。

- 1 次：极限 PR，只比重量，再比日期。
- 2 次及以上：做组 PR，比重量再比次数。
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
- 身体数据：体重、体脂、BMI。没有围度曲线。体重和体脂都要大于 0 才画。BMI 用到该日为止最近的正身高。

首页汇总在 `HomePeriod`。周从周一开始。训练天数是区间内有组的不同日期数，容量为 0 的组也算有组；当天足迹的部位顺序与日历相同，0 容量的部位也会显示，汇总容量显示为 0 kg。时长只加已保存计时，不加还在跑的秒表。

「我的」里的三个数可以分别切换，状态只在当次组合里：训练天数 / 训练年限，累计时长的自动单位 / 固定分钟，体脂率 / 体重。训练天数在这里不是「练过几天」，而是从第一次有组的训练日到今天的跨度（含两端）。年限是这个跨度除以 365、向上取到一位小数。时长不到 1 分钟显示秒，不到 1 小时显示分钟，否则显示一位小数的小时。

## 身体数据

「我的」入口文案是「身体数据」（`mine_body_title`）。展示页在下。底部「修改」进资料表单（生日、性别、身高、体重、体脂）。「我的」里的资料编辑（用户名/头像）也可改性别，经 `saveIdentity`。人体图上的围度另有编辑页：胸围、腰围、臀围，以及左右臂围、小臂、腿围、小腿。单侧旧臂围 `armCm` 已在 16→17 删除，不再提供输入，也不在历史记录里显示；腰围只在围度页维护。保存其他资料时，表单会原样保留已有的 `waistCm`，不清空。保存后回到展示，不直接回设置或首页。系统返回与顶栏一致：编辑中的表单或围度页先回展示，历史列表也先回展示，展示再回打开它的那个根页面。

设置 → 数据 → 历史记录 → 身体记录复用 `BodyRecordHistory`，显示同一份 `body_measurements`；从设置进入时返回历史记录菜单。历史卡片默认折叠，只显示记录时间；点卡片展开身高、体重、围度、BMI、体脂，缩起后不渲染详情。

展示页上的人体图可在「身体数据」和「训练肌群」之间切换：「身体数据」画围度，「训练肌群」画当前选中日的肌群高亮。也可以从「训练肌群图」入口进入。图按已保存的训练会话计算，可切当天、本周（周一起）、本月，以及只看主要或次要。宽屏下「身体数据」仍只显示单张正面图，围度标注和人体图共用同一个 `MuscleFigureGeometry` frame，不能各算一套矩形。返回回到身体数据展示页。

字段还有年龄和 BMI。BMI 由身高体重现算。围度没有单独趋势图。

## 照片墙

入口在「我的」。网格按日期浏览，可按月筛选；相册或相机添加（相机经 `FileProvider`，路径见 `@xml/photo_file_paths`）。详情可改日期、备注、删除；编辑支持裁剪、旋转、涂鸦、文字、马赛克、模糊（`PhotoStore` + `PhotoWallRepository`）。

- **不进** `BackupCodec` / 导出 JSON。数据备份页有说明（`photo_wall_backup_note`）。
- 导入训练备份、清除本地数据**都不会**删除 `photo_wall` 行或 `filesDir/photo_wall` 下的文件。
- 相对路径必须安全；展示优先 edited，否则 original。不要把用户照片扫进日志或交接输出。

## 动作库

排序：练过的不同训练次数多的在前，然后更近的在前，然后名字。展示名是「部位-动作名」。自定义动作没有专属图时按部位用后备图。选择动作（`pickerMode = true`）不显示删除。

浏览：左侧部位，上方器械。名称搜索匹配中文名、英文名和别名，并保留当前部位/器械筛选；搜索不会把筛选清空。`ExerciseCatalogQuery.refinement` 留给以后的二级器械，现在没有对应字段，不能靠动作名猜测去缩小结果。

列表只写肌群名称；点进详情再画人体图。自定义动作可在新增和详情里编辑主要、次要肌群，同一块肌肉不能同时两种角色。没有映射就显示「未配置肌群」，不要猜。

数据集媒体已按项目方确认的授权打包；媒体版权归 Gym visual，必须保留 `© Gym visual — https://gymvisual.com/` 署名和 180×180 使用限制，并遵守 Gym visual Terms。授权记录在 `tools/exercise_audit/PHASE_F_AUTHORIZATION.md`，`assets/exercises/NOTICE.txt` 保留说明。`exercise_art_credit` 现在同时说明 Gym visual 缩略图/GIF 和 Bryl Lim / Everkinetic 备用示意图，不能删除。

## 计时器

日历页可以开始、暂停、继续。计次记的是从上一个计次点到现在的间隔，以及当时的累计时间。可以直接改时、分、秒：分和秒必须在 0–59，时不超过 999。改完如果还在跑，起点改成现在的 `elapsedRealtime`，累计改成新值。计时器页当前只有当前计时和计次，不再显示“已保存的计时”；已保存记录统一从设置 → 数据 → 历史记录 → 计时记录进入。

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
- 组编辑为横向一行，表头为「组 / 负重模式 / 重量 / 次数 / 完成」：组号用普通数字 1、2、3，负重模式点开下拉选择，重量和次数各自上 + 下 −（2.5kg / 1次），右侧完成勾选。完成整行绿色，新增组继承上一组重量/次数但不继承完成状态；长按完成勾可删除该组（可删除最后一组）。
- 每个动作卡底部「添加一组」右侧是「完成动作」：只把该动作卡收起为类似日历训练记录的摘要小卡，不保存、不结束计时、不丢弃数据；点击摘要卡展开。顶部「完成」才进入保存页。折叠 ID 存在 SessionEditorViewModel 的 `collapsedExerciseIds`，去选动作再回来仍保持折叠；只有点击卡片才展开。
- 动作卡头三行依次是部位、动作名、容量；训练页/保存页的统计名加粗、数值不加粗。
- 选择自重模式时，重量栏显示当前日期可用的身体记录体重并禁用加减；没有体重记录时弹出设置体重的对话框，保存后自动带出。体重只读。
- 从训练页退出编辑器会保留 `activeDraft`，训练页底部显示缩小卡片；其他根页面不显示。点击卡片回到同一个 `SessionEditor`，总计时继续；保存/删除后清掉 activeDraft。当前 activeDraft 只保存在内存，进程被杀后不会恢复。
- 当前保存仍然只写已完成组；未完成组不会落库（审计已标为待确认设计，不要静默改变规则）。

## 动作媒体与训练记录布局

数据集媒体随 APK 发布在 `assets/exercises/media/images|videos`，1,324 张 JPG + 1,324 个 GIF，按 `ds_<datasetId>` 稳定关联。`ExerciseMediaRepository` 优先读取 `filesDir/exercise_media` 下已获许可的本地覆盖文件，否则从 assets 直接读取；没有文件/解码失败时缩略图回退 `ExerciseDiagram`，动作演示显示“暂无动作演示”。缩略图在 IO 线程解码并用 LruCache 缓存；GIF 用 `Movie` 在演示页可见时循环，切页/关闭后协程结束，不在后台播放。不做首次全量复制，避免额外 137MB 磁盘占用。

动作详情默认“动作演示”，固定三个选项：动作演示、正面、反面。`MuscleFigure(forcedFront = ...)` 用于固定正反面，原有肌群缓存/高亮不变。

训练记录按每个动作一行三栏：左为部位/名称/容量，中为缩略图，右为 2 列 × 3 行组数据。实际显示每组“重量 × 次数”；1–5 组原样，6 组显示第 6 组，超过 6 组显示前 5 组 + 省略号；数据库中的全部组仍保留，详情/编辑可看。

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

`MainActivity` 读 `AppearanceStore`：跟随系统 / 浅色 / 深色，以及是否 `dynamicColor`。关掉壁纸配色后用应用自己的紫灰色，日历部位色更稳。卡片样式集中在 `ui/theme/DashboardCards.kt`。

启动器名中文「练有所迹」、英文 Traivol。自适应图标在 `drawable/ic_launcher_foreground.xml` 与 `ic_launcher_background.xml`，`mipmap-anydpi-v26` 引用它们，`mipmap-anydpi` 是 API 24–25 的 layer-list。单色层只用前景的透明度。用户换过好几版图标；桌面若还是旧图，是启动器缓存，需要卸掉再装。

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

## 改代码时容易踩的点

- `displaySet` 只有一个重量参数。传入的必须已经是有效公斤。
- 保存身体记录会重算 PR。只改展示、不改重量含义时不要顺手重算。
- 动作库用 `library-$dataEpoch`；选动作器用 `picker-${draftKey}-$dataEpoch`。不要把两者合并，否则新建训练会继承上一次的搜索和筛选。
- 导入/导出只放在 `BackupDataScreen`；设置首页只保留「数据备份」「历史记录」入口。历史记录的计时列表和身体列表分别是 `TimerHistory` / `BodyHistory`，不要再把已保存计时塞回计时器页。
- 根页面的 `Scaffold` 在底部栏留白之内，并 `consumeWindowInsets` 掉导航栏底部，避免和底部栏叠两层 inset。子页面在这层外面，自己处理 inset。
- 隐藏中的根页面用 0 尺寸布局留在组合里，避免点到后面一页，同时留下滚动状态。
- 英文资源目前只有 `app_name`。不要把整份中文 `strings.xml` 翻成英文，除非用户要求。
- 清除数据和导入 version 1 都不会删除 `filesDir/profile/avatar.jpg`，也不会重置 `gymlog_timer`。已保存计时只在 version 2 或清除数据时整表替换。
- 人体图的底图和遮罩必须同一矩形绘制。不要单独缩放遮罩，不要把 `quadriceps` 和它的三块成分一起涂亮。
- `armCm` 已在 16→17 删除。不要把它加回实体、备份导出或历史 UI；旧备份里的 `armCm` 直接忽略。左右臂围/小臂围继续使用 `armLeftCm` / `armRightCm` / `forearmLeftCm` / `forearmRightCm`。
- 日历格子颜色必须走 `CalendarDayColorLogic`；特殊日盖过部位色。不要再按旧的单 `BodyPart.rank` 独自算一套颜色给小日历或展开月历。
- `dayKinds` 是可选备份字段；缺键=空。不要把照片墙塞进备份，也不要在清除/导入训练数据时删照片文件。
- 照片墙相对路径必须经 `PhotoPaths.isSafe`；FileProvider 只用于相机临时文件。不要把用户照片内容写进日志或交接文档。
- 肌群训练高亮用红色系，用户选中用主题 `primary`；不要把选中画成训练色。点击命中优先使用 `MuscleMaskCatalog.layers` 的绘制顺序；`quadriceps` 合并层和子头不能各用一套优先级。
- `MuscleAssetCache` 按面懒加载，失败时保留底图并提供重试；遮罩解码、BitSet 和锚点计算必须继续在 `Dispatchers.Default`，不要移回主线程。
- 与 1.4.1 / 1.4.2 / 训练重构相关的单测包括：`DayKindMigrationTest`、`DayKindLogicTest`、`CalendarDayColorTest`、`PhotoWallMigrationTest`、`PhotoImageTest`、`BodyMeasurementMigrationTest`、`WorkoutDurationMigrationTest`、`WorkoutClockLogicTest`、`ReleaseHistoryTest`（最新版应为 1.5）。
