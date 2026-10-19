# GymLog 阶段 B：默认动作库切换报告

> 只切换默认目录可见性。没有删除、归档或迁移旧动作；没有改 UI 布局、数据库版本、备份格式、肌群资源或媒体。

## 0. 基线备份

开始前创建了新的源码备份，未覆盖旧备份：

`C:\Users\75192\AndroidStudioProjects\GymLog-phase-b-backup-20261018-173315.zip`

基线记录：

`tools/exercise_audit/baseline_before_phase_b.json`

阶段 B 前：

- Room：**13**
- BackupCodec：**2**
- 测试：**43 个测试类、167 个测试、0 失败**
- APK：**27,961,716 B**
- 数据集动作：**1,324**
- 旧 preset：**89**
- 稳定肌群 ID：**33**
- 真实用户数据库不可访问；custom 实际数量无法测量

## 1. 修改文件及原因

| 文件 | 原因 |
| --- | --- |
| `app/src/main/java/com/example/gymlog/data/ExerciseDao.kt` | 目录查询增加来源可见性和 `includeLegacy` 参数；archived 始终排除 |
| `app/src/main/java/com/example/gymlog/data/ExerciseRepository.kt` | `observeExercises()` 默认 `includeLegacy = false`，把来源过滤传入 DAO；`observeAll()` 不变 |
| `app/src/main/java/com/example/gymlog/data/ExerciseCatalogFilter.kt` | `ExerciseCatalogQuery` / `CatalogRepositoryArguments` 增加 `includeLegacy`，默认 false |
| `app/src/main/java/com/example/gymlog/ui/exercise/ExerciseLibraryViewModel.kt` | 把 `args.includeLegacy` 传给仓库；构造函数增加默认 false 的调试/回滚参数 |
| `app/src/test/java/com/example/gymlog/ExerciseCatalogFilterTest.kt` | 断言默认 false 和 true 的传递 |
| `app/src/test/java/com/example/gymlog/ExerciseCatalogVisibilityTest.kt` | 新增目录可见性、历史、PR、趋势、custom 回归测试 |
| `tools/exercise_audit/baseline_before_phase_b.json` | 记录阶段 B 前基线 |
| `airead.md` | 同步默认可见性和全量历史查询语义 |

未修改：

- `ExerciseLibraryScreen.kt`
- `SessionEditorViewModel.kt` / 选择器 UI
- `WorkoutRepository.kt`
- `TrainingTrendViewModel.kt`
- `ExerciseDiagram.kt`
- `GymLogDatabase.kt` / 迁移
- `BackupCodec.kt` / `BackupRepository.kt`
- 肌群资源、媒体资源

## 2. 查询路径

### 默认动作库

```text
ExerciseLibraryScreen
  → ExerciseLibraryViewModel.uiState
  → ExerciseRepository.observeExercises(..., includeLegacy = false)
  → ExerciseDao.observeFiltered(
        query, bodyPart, equipmentType,
        includeLegacy = false,
        datasetSource = EXERCISES_DATASET_ZH
     )
```

SQL 条件：

```sql
archived = 0
AND (
  :includeLegacy = 1
  OR source = :datasetSource
  OR isCustom = 1
)
AND 搜索/部位/器械条件
```

因此：

- dataset：显示。
- custom：显示。
- 旧 LEGACY `preset_*`：默认隐藏。
- 旧版本用户自定义行即使 `source = LEGACY`，只要 `isCustom = 1` 也显示。
- archived 的 dataset/custom/legacy 始终排除。
- `includeLegacy = true` 时才允许非归档 LEGACY 进入。

### 训练动作选择器

`GymLogApp` 的 `Screen.PickExercise` 和普通 `Screen.ExerciseLibrary` 都使用：

- 同一个 `ExerciseLibraryViewModel`
- 同一个 `ExerciseRepository.observeExercises`
- 同一个 DAO 查询

因此动作选择器和动作库使用完全相同的默认来源过滤，没有第二条查询路径。

### 搜索/部位/器械

- 文本搜索仍匹配 `name`、`nameEn`、`aliases`。
- 来源过滤在搜索条件之外始终生效；搜索旧 preset 名称/英文名/别名不会把它带回默认结果。
- 部位和器械筛选继续使用现有 `BodyPart` / `EquipmentType`。
- 文本搜索会清空部位/器械条件，但不会清空来源过滤。

### 全量历史路径

以下路径保持全量查询，没有接入 `observeFiltered`：

- `ExerciseRepository.observeAll()` → `ExerciseDao.observeAll()`
  - `TrainingTrendViewModel`
- `ExerciseDao.getById()` / `WorkoutSessionDao.getExercisesByIds()`
  - `WorkoutRepository.toDetails()`
  - 旧会话编辑/加载
- `WorkoutRepository.rebuildAllPersonalRecords()`
- `ExerciseEntity.workingPr()` / `maxPr()`
- `BackupRepository` / `BackupCodec`

## 3. 同名与展示

- 旧 preset 隐藏后，旧/新同名不会同时出现在默认目录。
- 数据集内部同名动作按 `id` 保留为独立记录。
- `ExerciseBrowsePane` 使用 `items(state.exercises, key = { it.id })`，没有按 `name` 分组或去重。
- 没有修改 `ExerciseLibraryScreen` 的布局，也没有增加英文名/来源标签；同名数据集动作仍可通过不同 ID 和现有器械副标题区分。
- 没有修改 `countByName()` 的自定义重名校验语义。
- 没有为未映射数据集动作猜测肌群；没有直接映射时继续显示未配置。
- `ExerciseDiagram` 后备图逻辑未改。

## 4. includeLegacy 回滚/调试开关

- `ExerciseCatalogQuery(includeLegacy = true)`
- `ExerciseLibraryViewModel(initialIncludeLegacy = true)`
- 默认 `false`。
- 第一版没有用户设置界面。
- 切换回 true 只改变查询结果，不修改数据库、ID、归档状态或历史关系。

## 5. 新增测试

`ExerciseCatalogVisibilityTest`（10 个）：

1. `defaultCatalogShowsDatasetAndCustomButHidesLegacyPresets`
   - 默认结果包含 dataset/new custom/旧版 custom；
   - 不包含非归档 legacy preset、archived dataset/custom/legacy；
   - 断言具体 ID 和来源。
2. `includeLegacyBringsBackOnlyNonArchivedLegacyRows`
   - `includeLegacy = true` 恢复非归档 legacy；
   - archived legacy 仍排除。
3. `archivedRowsAreAlwaysExcluded`
   - 对 false/true 两种模式都验证 archived 三类动作排除。
4. `searchMatchesNameNameEnAndAliasesWithoutRestoringHiddenLegacy`
   - name、nameEn、aliases 均匹配；
   - 搜索 legacy 三种字段默认仍为空；
   - includeLegacy 后才返回 legacy。
5. `bodyPartAndEquipmentFiltersDoNotBringBackHiddenLegacy`
   - CHEST/FREE_WEIGHT 过滤不返回 legacy。
6. `sameNameDatasetRowsRemainIndependentRecords`
   - 两条同名 dataset 的 ID 都返回，不合并。
7. `customAddEditMusclesAndArchiveStillWork`
   - 新增 custom、编辑肌群、归档；
   - 归档后默认目录排除，但 `getById` 仍可读。
8. `historicalPresetSetStillResolvesItsName`
   - 旧 preset 训练组通过 `WorkoutRepository.loadSession` 仍解析旧名称。
9. `oldPresetPersonalRecordStillReads`
   - 旧 preset 的 working/max PR 仍可读。
10. `trainingTrendStillSeesHistoricalLegacyExercise`
    - `TrainingTrendViewModel` 仍能看到旧 preset 历史并选中它。

`ExerciseCatalogFilterTest` 增加 1 个：

- `includeLegacyIsCarriedThroughRepositoryArguments`

## 6. 验收结果

执行：

```text
:app:testDebugUnitTest :app:assembleDebug --offline --no-daemon --console=plain
```

沿用本机可执行的 Gradle 9.6.0 发行版；`gradlew.bat` 的 daemon 挂起问题未复现为项目测试失败。

结果：

- **44 个测试类、178 个测试、0 失败、0 错误、0 跳过**
- 基线：43 个测试类、167 个测试
- 新增：1 个测试类、11 个测试
- **BUILD SUCCESSFUL**
- APK：`app/build/outputs/apk/debug/app-debug.apk`
- APK 大小：**27,436,049 B**
- APK 内 `assets/exercises/` 仍只有：
  - `NOTICE.txt`
  - `dataset_v1.json`
  - `preset_match_candidates.json`
- 没有 Gym visual 图片/GIF

APK 比阶段 B 基线小 525,667 B；没有新增/删除媒体资源，资源列表保持不变，属于构建输出差异，未发现功能资源变化。

## 7. 版本与数据数量

| 项目 | 值 |
| --- | --- |
| Room | **13** |
| BackupCodec | **2** |
| 数据集动作 | **1,324** |
| 旧 preset | **89** |
| 稳定肌群 ID | **33** |
| 真实 custom 数量 | **不可测量**（无真实用户 DB） |

## 8. 未验证项

1. 没有真实用户数据库，custom 实际数量和真实数据组合未验证。
2. 没有真机 UI/截图测试；只验证了查询与 ViewModel/历史逻辑。
3. `ExerciseLibraryViewModel` 的 `ensurePresetsSeeded()` 仍在协调器锁外；它是 `insertIgnore` 的旧预置补种，不会删除或覆盖数据。
4. 同名 dataset 动作在卡片上仍显示相同名称；本阶段没有增加来源/英文名标签，只保证 ID 独立、不合并。
5. `includeLegacy` 是代码级调试开关，没有普通用户设置。
6. 媒体许可、Gym visual 媒体、GIF/缩略图仍未解决。
7. 没有测试 Compose 视觉层；`ExerciseLibraryScreen` 未改布局。

## 9. 保护范围核对

- 旧 preset 未被删除、改名、归档或改成 `ds_*`。
- 没有按名称合并 dataset 动作。
- 没有自动确认匹配候选。
- 没有扩展不确定的肌群映射。
- Room v13、迁移和 BackupCodec v2 未改。
- 肌群 PNG、遮罩、manifest、`MuscleMaskCatalog` 未改。
- 没有接入 Gym visual 媒体或媒体加载器。
- 历史训练、PR、趋势、旧会话编辑、备份恢复仍走全量查询。
