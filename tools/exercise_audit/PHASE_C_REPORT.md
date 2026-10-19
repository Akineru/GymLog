# GymLog 阶段 C：统一身体部位与器械分类并迁移旧动作

> 已完成默认动作库的新分类切换、旧动作分类迁移、Room 13→14 增量迁移和查询/UI 接入。未删除动作、未按名称合并、未自动确认匹配候选、未改肌群资源/媒体。

## 0. 备份

阶段 C 开始前创建：

`C:\Users\75192\AndroidStudioProjects\GymLog-phase-c-backup-20261018-174347.zip`

大小：103,092,796 B

分类审计与完整分类表：[PHASE_C_AUDIT.md](PHASE_C_AUDIT.md)

## 1. 修改文件

### 分类核心

- `app/src/main/java/com/example/gymlog/data/ExerciseClassification.kt`（新增）
  - 10 个身体部位标准分类
  - 28 个器械标准分类
  - 旧分类兼容桶
  - 稳定 ID、原始值、中文显示名、状态和排序
- `app/src/main/java/com/example/gymlog/data/ExerciseClassificationMigrator.kt`（新增）
  - 逐行、事务内、幂等迁移
  - 不覆盖已确认分类
  - 保留原始字段和归档状态

### 数据层/迁移

- `ExerciseEntity.kt`
  - `bodyCategoryId`
  - `equipmentCategoryId`
  - `classificationStatus`
  - `classificationVersion`
  - 分类索引
- `ExerciseDao.kt`
  - 按分类 ID 查询
  - 分类更新
  - 全量分类读取
  - dataset 更新字段
- `ExerciseRepository.kt`
  - 目录查询使用分类 ID
  - 自定义动作使用新分类
  - 不改变 `observeAll()` 的全量语义
- `ExerciseCatalogFilter.kt`
  - 查询参数使用稳定分类 ID
  - 搜索不再清空分类选择
- `ExerciseCatalogImporter.kt`
  - 数据集行写入 10/28 分类和 `DATASET` 状态
  - 导入/已完成检查后运行分类迁移
- `PresetExercises.kt`
  - 旧预置写入兼容分类/`AMBIGUOUS`
- `LegacyRecordMigrator.kt`
  - 旧记录生成动作写入兼容分类
- `GymLogDatabase.kt`
  - Room version **14**
- `GymLogMigrations.kt`
  - `MIGRATION_13_14`，增量添加分类列/索引
- `BackupCodec.kt`
  - 分类字段可选序列化；BackupCodec 仍为 **2**
- `WorkoutSessionDao.kt`
  - 日期部位统计优先使用 `bodyCategoryId`，回退旧 `bodyPart`

### UI/查询

- `ExerciseCategoryFilters.kt`
  - `BodyCategoryRail`（全部 + 10 标准 + 兼容）
  - `EquipmentCategoryBar`（全部 + 28 标准 + 兼容，横向滚动）
- `ExerciseLibraryViewModel.kt`
  - 分类类型/ID
  - 自定义动作新分类
  - 默认 `ALL_BODY` / `ALL_EQUIPMENT`
- `ExerciseLibraryScreen.kt`
  - 动作库和选择器共用
  - 自定义动作选择 10/28 分类
  - 卡片/详情显示分类和待确认/未映射标记
- `TrainingTrendViewModel.kt` / `TrainingTrendScreen.kt`
  - 训练趋势按新分类筛选
  - 仍从全量动作解析历史
- `HomePeriod.kt` / `DayDisplayOrder.kt` / `PartColors.kt` / `GymLogApp.kt` / `BodyPart.kt`
  - 有分类 ID 时优先显示/分组新分类；旧数据回退 broad 兼容字段
- `SessionEditorViewModel.kt`
  - 编辑训练中的动作分组优先使用新分类
- `strings.xml`
  - 分类待确认/未映射文案；搜索横幅不再声称筛选暂停

### 测试

- `ExerciseClassificationTest.kt`（新增，4 个）
- `ExerciseCatalogVisibilityTest.kt`（更新到新分类）
- `ExerciseCatalogFilterTest.kt`（更新）
- 迁移测试补 `MIGRATION_13_14`
- `ExerciseCatalogBackupTest.kt`（分类字段往返/旧 JSON 默认）
- `ExerciseCatalogBackupCompatibilityTest.kt`（旧 v1 移除分类字段）
- `ExerciseClassificationTest` 覆盖完整分类、旧 preset/custom、幂等、未知分类、组合筛选

## 2. 新分类体系

### 身体部位 10 类

原始 `bodyPartDetail` 值：

`chest`、`back`、`shoulders`、`upper arms`、`lower arms`、`upper legs`、`lower legs`、`waist`、`cardio`、`neck`

稳定 ID 如 `body_chest`、`body_upper_arms`。原始值继续保存在 `bodyPartDetail`。

动作数：

| 分类 | 动作数 |
| --- | ---: |
| 上臂 | 292 |
| 大腿 | 227 |
| 背部 | 203 |
| 腰部 | 169 |
| 胸部 | 163 |
| 肩部 | 143 |
| 小腿 | 59 |
| 前臂 | 37 |
| 有氧 | 29 |
| 颈部 | 2 |
| 合计 | **1,324** |

### 器械 28 类

从 `equipmentDetail` 原样生成稳定 ID，如 `equipment_barbell`、`equipment_body_weight`、`equipment_smith_machine`。原始值继续保存在 `equipmentDetail`。

完整 28 类及动作数见 [PHASE_C_AUDIT.md](PHASE_C_AUDIT.md)。合计 **1,324**。

## 3. 旧动作迁移统计

### 89 个旧 preset

| 项目 | 数量 |
| --- | ---: |
| 保留数量 | **89** |
| 身体部位自动映射到标准分类 | **52** |
| 身体部位保留兼容分类 | **37** |
| 身体部位未映射 | **0** |
| 器械自动映射到 28 类之一 | **0** |
| 器械保留旧三类兼容分类 | **89** |
| 身体 + 器械都达到标准分类 | **0** |
| 标记 `AMBIGUOUS` | **89** |
| 完全未映射 | **0** |

旧动作的 ID、名称、PR、归档状态和训练组引用均未改。

### Custom

真实用户数据库不可访问，**实际 custom 数量无法统计**。

迁移规则：

- 原 ID、名称、别名、PR、归档状态保留。
- `bodyPart` / `equipmentType` 原值保留。
- 旧 body CHEST/BACK/SHOULDERS/ABS 可映射标准；ARMS/LEGS/OTHER 进入兼容分类。
- 旧 equipment FREE_WEIGHT/MACHINE/OTHER 进入兼容分类。
- 新自定义动作直接选择新标准分类，状态 `MAPPED`，版本 1。
- 不静默覆盖用户名称/别名/分类原文。

测试中的旧 custom 和归档 custom 均保留。

### 数据集

1,324 条全部写入：

- `classificationStatus = DATASET`
- `classificationVersion = 1`
- 10/28 分类 ID 全部可追溯

### 无法可靠映射

- 旧 ARMS 无法可靠拆上臂/前臂 → 兼容 `body_legacy_arms`
- 旧 LEGS 无法可靠拆大腿/小腿 → 兼容 `body_legacy_legs`
- 旧 OTHER → 兼容 `body_legacy_other`
- 旧 FREE_WEIGHT/MACHINE/OTHER 无法可靠拆 28 类 → 三个器械兼容分类
- 未知原始值 → `*_unknown` + `UNMAPPED`，保留原始 detail

没有按名称猜测分类，没有编造器械，没有合并动作。

## 4. 数据库、迁移与备份

- Room：**13 → 14**
- `MIGRATION_13_14` 增量添加：
  - `bodyCategoryId`
  - `equipmentCategoryId`
  - `classificationStatus`，默认 `UNMIGRATED`
  - `classificationVersion`，默认 0
  - 两个索引
- 未使用 destructive migration，未清库。
- 分类数据由 `ExerciseClassificationMigrator` 在导入/启动/恢复/清除后逐行补齐，`classificationVersion = 1` 后不重复覆盖。
- 分类失败时事务回滚；已有动作/历史/归档不被部分覆盖。
- BackupCodec 仍为 **2**；新分类字段可选写入，旧 v1/v2 缺失时回退 `UNMIGRATED`/0，恢复后由迁移补齐。
- 旧备份恢复测试和 v12→v13→v14 迁移测试通过。

## 5. UI 行为

- 动作库和新增训练动作选择器共用同一查询。
- 默认 `全部身体部位 + 全部器械`。
- 身体部位栏 15 项（全部 + 10 标准 + 4 兼容/未知），可纵向滚动。
- 器械栏 33 项（全部 + 28 标准 + 4 兼容/未知），横向滚动。
- 搜索匹配 name/nameEn/aliases，并保留当前分类筛选。
- 部位 + 器械组合筛选同时生效。
- 自定义动作新增使用新 10/28 分类。
- 动作卡片/详情显示新分类；兼容分类显示“分类待确认”，未知显示“分类未映射”。
- 历史详情、PR、趋势仍走全量动作查询，旧 ID 可解析。
- 旧 preset 继续按阶段 B 默认隐藏，includeLegacy 调试开关仍保留。

## 6. 测试

执行（本机 Gradle 9.6.0，因 `gradlew.bat` daemon 启动挂起）：

```text
:app:testDebugUnitTest :app:assembleDebug --offline --no-daemon --console=plain
```

结果：

- **45 个测试类、182 个测试**
- 失败：**0**
- 错误：**0**
- 跳过：**0**
- **BUILD SUCCESSFUL**

新增/关键测试：

- `datasetContainsEveryActualBodyAndEquipmentCategory`
  - 1,324 条；10 个 body、28 个 equipment；逐类计数与原始 JSON 一致；全部 `DATASET`。
- `legacyPresetsAndCustomRowsAreMigratedIdempotently`
  - 89 个 preset 保留；
  - 旧 custom 名称/别名/原始字段/归档保留；
  - 全部 preset/custom 分类非空；
  - 二次迁移不改变分类；
  - 已确认映射不被覆盖。
- `unknownDatasetCategoryIsPreservedAndMarkedUnmapped`
  - 未知原始值不丢失、不崩溃，标记 `UNMAPPED`。
- `categoryFiltersAndSearchComposeCorrectly`
  - 身体、器械、组合、搜索 + 分类、隐藏 preset。
- 既有迁移、备份 v1/v2、协调器并发、历史/PR/趋势、custom 新增/编辑/归档测试全部通过。

## 7. 构建

APK：

`app/build/outputs/apk/debug/app-debug.apk`

大小：**28,048,897 B**

APK 内 `assets/exercises/` 仍只有：

- `NOTICE.txt`
- `dataset_v1.json`
- `preset_match_candidates.json`

没有 Gym visual 图片/GIF。

## 8. 未完成与风险

1. 真实用户数据库不可访问，实际 custom 数量、实际旧分类组合、真实归档数量未验证。
2. 没有真机/Compose 截图测试；10/28 分类 UI 的滚动和触控未做视觉验收。
3. 旧器械 89 个 preset 只能进入兼容分类；这是不猜测名称的必然结果，需要后续人工确认。
4. 旧 ARMS/LEGS 无法可靠拆成上臂/前臂、大腿/小腿；保留兼容分类。
5. 首页/日历/训练编辑/历史分组已优先使用新分类 ID；只有分类字段为空的极旧/未迁移行回退到旧 broad 字段。新分类贯穿动作库、选择器、搜索/筛选、自定义动作、详情、训练趋势和历史展示。
6. 分类迁移失败注入测试没有单独做；迁移使用 Room 事务，代码路径有回滚，但未模拟磁盘/SQL 失败。
7. 媒体许可和 Gym visual 媒体仍未解决。
8. 训练趋势在旧数据没有分类时依赖迁移补齐；未在真实用户旧库上运行。
