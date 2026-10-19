# GymLog 阶段 D：移除旧预置动作与旧分类正式入口

> 阶段 D 完成活动目录切换：旧 `preset_*` 不再进入活动动作库，旧兼容分类不再作为正式筛选选项，自动补种旧预置的代码路径已删除。旧预置行本身保留给历史/PR/备份解析，未做物理删除。

## 0. 备份与限制

实施完成后生成的恢复包：

`C:\Users\75192\AndroidStudioProjects\GymLog-phase-d-recovery-20261018-203815.zip`  
大小：103,110,596 B

最近一次更早的源码备份：

`C:\Users\75192\AndroidStudioProjects\GymLog-phase-c-backup-20261018-174347.zip`

**需要明确说明**：阶段 D 的代码改动前没有单独生成一份 pre-change 备份；上述 recovery 包是实施后的当前源码。没有真实用户数据库，无法做数据库级恢复演练。阶段 C 的审计/备份可用于了解改动前数据层状态，但不能替代阶段 D pre-change 源码包。

## 1. 修改文件

### 活动目录/查询

- `ExerciseDao.kt`
  - 活动查询只保留 `archived = 0`、数据源或自定义动作
  - 增加 `id NOT LIKE 'preset\_%'` 的身份排除，不按名称删除
  - 删除 `includeLegacy` 参数
- `ExerciseRepository.kt`
  - `observeExercises` 删除 `includeLegacy`
  - 删除 `ensurePresetsSeeded()` 生产入口
- `ExerciseCatalogFilter.kt`
  - 删除 `includeLegacy` 字段/参数
- `ExerciseLibraryViewModel.kt`
  - 删除 `initialIncludeLegacy`
  - 删除初始化时补种旧预置
- `ExerciseLibraryScreen.kt`
  - 动作库和选择器继续共用同一查询
- `ExerciseClassification.kt`
  - `bodyUi` 只保留“全部 + 10 标准分类”
  - `equipmentUi` 只保留“全部 + 28 标准分类”
  - 兼容桶仍保留在内部 `bodyById` / `equipmentById`，仅服务历史解析，不再作为正式筛选
- `TrainingTrendViewModel.kt`
  - 旧兼容分类不再作为趋势的正式默认选中项；无标准分类时回退“全部”
- `ExerciseCategoryFilters.kt`
  - 身体部位栏 11 项、器械栏 29 项；不再显示兼容桶

### 补种/启动/备份/清除

- `GymLogApplication.kt`
  - 启动不再 `ensurePresetsSeeded()`
- `BackupRepository.kt`
  - 导入成功后的 `finishImport` 不再补旧预置
  - `clearLocalData()` 不再补旧预置
- `PresetExercises.kt`
  - 标记为 deprecated 的测试/历史 fixture
  - 生产代码不再调用
- `ExerciseRepository.kt`
  - 已删除 `ensurePresetsSeeded()`

### 分类/历史兼容

- `ExerciseClassification.kt`
  - 正式 UI 分类收缩为 10/28
  - 兼容分类仅内部解析
- `HomePeriod.kt`、`DayDisplayOrder.kt`、`WorkoutSessionDao.kt`、`PartColors.kt`、`GymLogApp.kt`、`SessionEditorViewModel.kt`
  - 优先使用新分类；没有分类字段的极旧行回退旧 broad 字段
- `TrainingTrendViewModel.kt`
  - 标准分类不存在时使用“全部”，不把兼容桶当正式分类

### 测试

- `ExerciseCatalogVisibilityTest.kt`
  - 删除 includeLegacy 测试
  - 增加/保留旧预置不进入活动目录、搜索/筛选、归档、历史解析验证
- `ExerciseCatalogFilterTest.kt`
  - 删除 includeLegacy 测试
  - 新分类过滤参数
- `ExerciseClassificationTest.kt`
  - 增加“正式分类只有 10/28”测试
  - 新安装导入后没有旧预置
- `ExerciseCatalogBackupRestoreTest.kt`
  - 恢复旧备份后旧预置仍可 `getById`，但活动目录不显示
  - 清除后旧预置不补种
- `BackupRoundTripTest.kt`
  - 清除/导入后不再断言旧预置被补种
- 迁移、备份 v1/v2、协调器并发测试继续通过

## 2. 旧预置的处理方式

### 结论：停用 + 保留历史行，未物理删除

- 89 个旧 `preset_*` 行如已存在于用户数据库，保留在 `exercises`。
- 活动目录查询排除：
  - `id LIKE 'preset_%'`
  - 旧 `source = LEGACY` 且非 custom 的行
- 因此旧预置：
  - 不出现在动作库
  - 不出现在新增训练选择器
  - 不参与正式分类筛选
  - 不能通过 `includeLegacy` 打开（该开关已删除）
- 历史详情、PR、趋势、旧会话编辑、备份仍可通过 `getById()` / `getExercisesByIds()` / 全量查询解析旧 ID。

### 为什么没有物理删除

- 没有真实用户数据库，无法证明某个旧预置没有任何训练组、PR、归档或备份引用。
- 物理删除会造成外键/历史解析和旧备份恢复风险。
- 阶段 D 选择最小安全方案：**停用活动目录入口，保留解析记录**。
- 没有按名称删除任何动作；没有删除 `custom_*`；没有改 ID；没有合并同名动作。

### 实际数量

| 项目 | 数量 |
| --- | ---: |
| 旧预置总行数（代码 fixture 基线） | 89 |
| 物理删除 | **0** |
| 活动目录中显示 | **0** |
| 保留用于历史/备份解析 | **89** |
| 新安装自动补种 | **0** |

真实用户库中的旧预置/自定义/历史引用数量不可访问；没有做真实数据删除。

## 3. 旧分类体系移除

- 正式身体部位筛选：**全部 + 10 个标准分类**，不再有：
  - 旧七分类
  - `body_legacy_arms`
  - `body_legacy_legs`
  - `body_legacy_other`
  - `body_unknown`
- 正式器械筛选：**全部 + 28 个标准分类**，不再有：
  - 自由器械/固定器械/其他
  - `equipment_legacy_free_weight`
  - `equipment_legacy_machine`
  - `equipment_legacy_other`
  - `equipment_unknown`
- 兼容分类仍存在于 `ExerciseClassification` 的内部映射/历史显示中，但不进入 `bodyUi` / `equipmentUi`，不作为筛选选项。
- 旧 `bodyPart` / `equipmentType` 字段保留，用于历史分组、旧备份和兼容回退；不覆盖成展示名称。
- 数据集 `bodyPartDetail` / `equipmentDetail` 原始值保留。

## 4. 用户自定义动作和历史保护

- 所有 `custom_*` 保留。
- 自定义动作名称、别名、PR、归档、肌群关联不变。
- 旧 custom 的兼容分类仅内部标记；活动目录在“全部”中显示，卡片显示“分类待确认”，不会被伪装成标准分类。
- 历史训练组外键、PR、趋势、旧会话加载和备份恢复不变。
- 旧备份恢复后，旧预置即使回到数据库，也不会重新进入活动目录。

## 5. 数据库与备份

- Room：**14**（阶段 D 未新增 schema 版本）
- 未新增/未执行 destructive migration
- 旧预置的停用是查询层 + 身份过滤，不是物理 DELETE
- BackupCodec：**2**
- 旧备份 v1/v2 恢复兼容
- 恢复后：

  - 旧预置可 `getById` 用于历史解析
  - 活动目录查询排除旧预置
  - 导入/清除不再补种旧预置

## 6. 测试与构建

执行：

```text
:app:testDebugUnitTest :app:assembleDebug --offline --no-daemon --console=plain
```

本机 `gradlew.bat` daemon 启动挂起；使用本机 Gradle 9.6.0 完成。

结果：

- **45 个测试类、181 个测试**
- **0 失败、0 错误、0 跳过**
- **BUILD SUCCESSFUL**
- APK：`app/build/outputs/apk/debug/app-debug.apk`
- APK 大小：**27,453,261 B**
- Room：**14**
- BackupCodec：**2**
- 数据集动作：**1,324**
- 活跃旧预置：**0**
- 物理删除旧预置：**0**
- 稳定肌群 ID：**33**（未改）
- APK 内 `assets/exercises/` 仍只有 3 个结构化文件，没有 Gym visual 图片/GIF

关键新增/更新验证：

- 新安装导入后没有 `preset_*`
- 正式分类列表只有 10/28 + 全部
- 旧预置不出现在活动目录/搜索/筛选
- 旧备份恢复后旧预置仍可解析但不活动
- 清除后不补种旧预置
- custom 新增/编辑/归档和同名记录保留
- 历史训练/PR/趋势仍能解析旧 ID
- v12→v13→v14 迁移、旧 v1/v2 备份、协调器并发继续通过

## 7. 未完成/未验证

1. **没有真正 pre-change 阶段 D 备份**，只有实施后的 recovery 包；这是本阶段流程缺陷。
2. 没有真实用户数据库，无法确认 89 条旧预置的实际历史/PR/归档引用；因此全部保留、全部停用。
3. 没有物理清理旧预置；这是有意的安全选择，不是遗漏。
4. 旧 custom 如果仍是兼容分类，活动目录“全部”可见但无法按标准分类筛选；显示“分类待确认”。
5. 真机/Compose 视觉验收未做；分类栏 11/29 项的滚动和触摸未实测。
6. 分类迁移失败注入未单独测试；Room 事务路径已存在。
7. 媒体许可和 Gym visual 媒体仍未解决。
