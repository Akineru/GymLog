# GymLog 动作库本地重构：第二阶段完成报告

> 状态：数据层与 Room 迁移已实现，测试和 Debug 构建通过；暂停，不进入 UI 重构。
>
> 本阶段未修改肌群 PNG/遮罩/manifest/MuscleMaskCatalog，未打包数据集图片/GIF。

## 1. 备份与基线

完整源码备份：

`C:\Users\75192\AndroidStudioProjects\GymLog-exercise-refactor-backup-20261018-143447.zip`

基线记录：[`baseline_before_phase2.json`](baseline_before_phase2.json)

- 数据库版本：修改前 **12**，修改后 **13**
- 备份格式：**2**（未升版本）
- 预置动作：**89 个**，旧 ID 列表已写入基线 JSON
- 预置肌群映射：89 组
- 稳定肌群：33 个
- 测试基线：**35 个测试类、145 个测试、0 失败**
- 当前工作区没有可访问的用户 `gymlog.db`；因此无法测量真实用户的 custom 动作、训练组和 PR 数量。`baseline_before_phase2.json` 明确记录 `accessibleUserDatabase=false`，没有编造这些数字。

## 2. 修改与新增文件

### 2.1 数据实体 / DAO / 迁移

- `app/src/main/java/com/example/gymlog/data/ExerciseEntity.kt`
- `app/src/main/java/com/example/gymlog/data/ExerciseSource.kt`
- `app/src/main/java/com/example/gymlog/data/ExerciseContentEntity.kt`
- `app/src/main/java/com/example/gymlog/data/ExerciseMediaEntity.kt`
- `app/src/main/java/com/example/gymlog/data/ExerciseSourceMapEntity.kt`
- `app/src/main/java/com/example/gymlog/data/ExerciseMuscleTokenEntity.kt`
- `app/src/main/java/com/example/gymlog/data/ExercisePresetMatchCandidateEntity.kt`
- `app/src/main/java/com/example/gymlog/data/ExerciseImportStateEntity.kt`
- `app/src/main/java/com/example/gymlog/data/ExerciseCatalogDao.kt`
- `app/src/main/java/com/example/gymlog/data/ExerciseDao.kt`
- `app/src/main/java/com/example/gymlog/data/ExerciseMuscleDao.kt`
- `app/src/main/java/com/example/gymlog/data/ExerciseRepository.kt`
- `app/src/main/java/com/example/gymlog/data/GymLogDatabase.kt`
- `app/src/main/java/com/example/gymlog/data/GymLogMigrations.kt`
- `app/src/main/java/com/example/gymlog/data/BackupCodec.kt`
- `app/src/main/java/com/example/gymlog/data/BackupDao.kt`
- `app/src/main/java/com/example/gymlog/data/BackupRepository.kt`
- `app/src/main/java/com/example/gymlog/GymLogApplication.kt`

### 2.2 导入与映射

- `app/src/main/java/com/example/gymlog/data/DatasetMuscleMapping.kt`
- `app/src/main/java/com/example/gymlog/data/ExerciseCatalogImporter.kt`

### 2.3 结构化资产（不含媒体）

- `app/src/main/assets/exercises/dataset_v1.json`（3,184,709 B）
- `app/src/main/assets/exercises/preset_match_candidates.json`（14,292 B）
- `app/src/main/assets/exercises/NOTICE.txt`

**没有**加入 1,324 个缩略图或 GIF；APK 的 `assets/exercises/` 只有上述 3 个文件。

### 2.4 测试

新增：

- `ExerciseCatalogMigrationTest.kt`
- `ExerciseCatalogImportTest.kt`
- `ExerciseCatalogBackupTest.kt`
- `ExerciseCatalogBackupRestoreTest.kt`

修改：

- `GirthSaveTest.kt`
- `ProfileMigrationTest.kt`
- `TimerMigrationTest.kt`

（这些旧迁移测试补上了 `MIGRATION_12_13`，使 Room 能完成到 v13 的迁移链。）

### 2.5 文档

- `airead.md`：数据库版本、12→13、动作来源/内容/媒体/词元表、启动导入、备份可选字段、清除/恢复补种说明。

## 3. Room 12→13 迁移

实现 `MIGRATION_12_13`，非破坏性：

### 3.1 `exercises` 新列

```text
nameEn TEXT
source TEXT NOT NULL DEFAULT 'LEGACY'
sourceId TEXT
equipmentDetail TEXT
bodyPartDetail TEXT
```

- 旧行 `source` 默认 `LEGACY`；`isCustom = 1` 的旧行回填 `CUSTOM`。
- 增加 `(source, sourceId)` 唯一索引；旧行 `sourceId` 为 null，不影响。
- 不重建 `exercises`，不改旧动作 ID、PR、归档状态。
- 迁移没有解析 JSON，也没有导入 1,324 条数据。

### 3.2 新表

- `exercise_content`
- `exercise_media`
- `exercise_source_map`
- `exercise_muscle_tokens`
- `exercise_preset_match_candidate`
- `exercise_import_state`

均有明确主键；内容/媒体/来源映射/词元表通过外键级联到 `exercises`。迁移和 Room 期望 schema 的列名、类型、索引已对照检查。

### 3.3 注册

`GymLogApplication` 已把 `MIGRATION_12_13` 加入 Room builder；没有 `fallbackToDestructiveMigration`。

### 3.4 迁移测试

`ExerciseCatalogMigrationTest`：

- 手动创建 v12 `exercises` 表
- 保留一个旧预置和一个自定义动作（含归档/PR）
- 执行 `MIGRATION_12_13`
- 断言行数、旧 PR、归档、`source`/`sourceId`、自定义回填 `CUSTOM`、六张新表存在

## 4. 导入器

`ExerciseCatalogImporter`：

- `Dispatchers.IO` 读取 3.18 MB 结构化资产
- `Dispatchers.Default` 解析 JSON
- `database.withTransaction` 写入
- 幂等：重复执行不会增加重复动作
- 不自动解除归档，不覆盖自定义动作/旧动作
- 只写 `ds_*` 新动作，不写旧 `preset_*` 或 `custom_*`

### 4.1 实际导入结果

| 指标 | 结果 |
| --- | ---: |
| 新增动作 | 1,324 |
| 关联旧预置 | 0（按规则不自动关联） |
| 跳过 | 0 |
| 待匹配 | 24（23 PENDING_CONFIRMATION + 1 AMBIGUOUS） |
| 未映射词元出现次数 | 2,476 |
| 失败 | 0 |
| `exercise_source_map` | 1,324 |
| `exercise_content` | 2,648（zh + en） |
| `exercise_media` | 1,324，全部 `NOT_BUNDLED` |
| `exercise_muscle_tokens` | 5,229 |
| `exercise_muscles` 直接映射 | 2,051 |
| 旧预置候选 | 89（23 / 1 / 65） |

“未映射词元 2,476”是 token 出现次数，不是唯一词元数；唯一词元仍是 50 个。

### 4.2 数据集 ID / 旧 ID

- 新动作 ID：`ds_0001` … 与数据集原始 `0001` 对应
- `sourceId` 保存原始数字 ID
- `source` = `EXERCISES_DATASET_ZH`
- 旧 `preset_*` / `custom_*` 完全保留

### 4.3 匹配策略

- 23 个唯一候选只进 `exercise_preset_match_candidate`，状态 `PENDING_CONFIRMATION`
- `pull_up` 的 `pull-up`/`chin-up` 歧义进 `AMBIGUOUS`
- 65 个无精确匹配进 `NO_MATCH`
- 没有按名称合并，也没有自动把数据集动作挂到旧预置
- 1,324 条全部以 `ds_*` 独立存在（包括与旧预置同名的动作），便于后续人工确认后再决定关联/隐藏

### 4.4 肌群映射

`DatasetMuscleMapping` 显式列出 50 个词元：

- 23 个 `MAPPED`：直接映射到现有稳定 ID
- 12 个 `AMBIGUOUS`：保留候选 ID，但**不写入** `exercise_muscles`
- 15 个 `UNMAPPED`：保留原词元和状态

因此：

- 未映射/歧义词元不会静默丢失
- 不会因为 `shoulders`、`forearms`、`calves`、`traps` 等泛化词元自动点亮多块具体肌肉
- 4 个无独立遮罩的稳定 ID（`rhomboids`、`brachialis`、`internal_oblique`、`transversus_abdominis`）含义不变

## 5. 旧数据保护证据

自动测试覆盖：

1. **旧 89 个 preset ID 全保留**：导入后计数仍为 89，`source` 仍为 `LEGACY`。
2. **旧归档状态保留**：导入不会解除 `archived`。
3. **旧 PR 保留**：旧预置的 working/max PR 在导入后数值不变。
4. **训练组外键保留**：引用旧预置的 `workout_sets.exerciseId` 仍存在，组数不变。
5. **自定义动作保留**：`custom_keep` 的名称/source 不被覆盖。
6. **同名不合并**：旧 `preset_barbell_bench_press` 与 `ds_0025` 同时存在；6 组同名数据集动作均按 ID 分开（如 `ds_0088`/`ds_1371`）。
7. **未映射词元保留**：`exercise_muscle_tokens` 中保留 `shoulders`/`hip flexors` 等原始词元及状态。

## 6. 备份与恢复

`BackupCodec.VERSION` 仍为 **2**，未改变已有字段含义。

新增可选字段：

- `nameEn`
- `source`
- `sourceId`
- `equipmentDetail`
- `bodyPartDetail`

兼容性：

- 旧 v1/v2 JSON 没有这些键：`source` 对旧预置回退 `LEGACY`；旧自定义回退 `CUSTOM`；其余为 null。
- 旧备份仍可导入；已有 `BackupRoundTripTest` v1/v2 用例全部通过。
- 内容/媒体/来源映射/原始词元不进备份，恢复后由导入器补种；自定义动作和自定义肌群映射仍按原规则恢复。
- 恢复旧备份后：
  - 旧预置/自定义/训练组/PR 保留
  - 导入器补 1,324 条数据集
  - `clearLocalData()` 后再补预置和数据集
- `ExerciseCatalogBackupRestoreTest` 验证：旧快照恢复 → 自定义动作/训练组保留 → `ds_0001` 和旧预置都在 → 清除本地数据后自定义消失、旧预置和数据集重新出现。

## 7. 测试与构建

最终命令：

```text
:app:testDebugUnitTest :app:assembleDebug --offline
```

结果：

- **BUILD SUCCESSFUL**
- **39 个测试类、154 个测试、0 失败、0 错误、0 跳过**
- 基线 145 个测试；新增 9 个
- 新增测试：
  - `ExerciseCatalogMigrationTest`：1
  - `ExerciseCatalogImportTest`：6
  - `ExerciseCatalogBackupTest`：1
  - `ExerciseCatalogBackupRestoreTest`：1
- APK：`app/build/outputs/apk/debug/app-debug.apk`
  - 大小：**27,961,716 B**
  - 修改前约 27,532,898 B；增加约 **419 KB**
  - 没有媒体文件，只有 3.18 MB JSON 经 APK 压缩后的增量

旧迁移测试已补 `MIGRATION_12_13`；没有删除旧测试。

## 8. 未完成 / 待确认

### 8.1 匹配

- 23 个 `PENDING_CONFIRMATION` 候选尚未人工确认，因此没有关联旧预置。
- 1 个 `AMBIGUOUS`：`pull_up` 对 `pull-up`/`chin-up`。
- 65 个 `NO_MATCH` 需要人工/模糊匹配；本轮不猜。
- 12 个泛化词元保持 `AMBIGUOUS`，不自动映射多块肌肉。
- 15 个词元保持 `UNMAPPED`，没有新增肌群 ID。

### 8.2 媒体

- 没有加入任何图片/GIF。
- `exercise_media` 只登记源路径、尺寸、署名、SHA-256 和 `NOT_BUNDLED`。
- Gym visual 媒体许可尚未确认；`NOTICE.txt` 记录了来源与署名要求。
- 缩略图/GIF 的 UI 加载、下载、缓存和损坏降级尚未实现。

### 8.3 UI

- 列表、详情、筛选、缩略图、GIF 播放均未重构。
- 当前动作库会看到新增的 `ds_*` 动作，使用原有按部位后备示意图；这是数据层阶段的可接受中间状态。
- 旧预置与 `ds_*` 同名并存会出现在列表中，需在 UI 阶段决定隐藏/标记/关联策略。

### 8.4 数据

- 当前工作区没有用户数据库，无法测量真实 custom/组/PR 数量；已记录基线限制。
- 内容表本轮只导入 zh/en；源数据还有 8 种语言，后续可扩展。
- 导入失败整体回滚，下一次启动重试；本轮没有 UI 重试入口。

## 9. 媒体许可状态

**未确认，未打包。**

- 数据集代码/数据结构/中英说明：MIT。
- 媒体（缩略图/GIF）：© Gym visual，180×180，要求保留署名；其条款不由 MIT 覆盖。
- 当前 APK 不含任何 Gym visual 图片/GIF。
- 正式打包或公开分发前必须确认 Gym visual 许可和 `https://gymvisual.com/` 署名要求。

---

本阶段完成数据层、迁移、导入、备份兼容和测试；暂停，等待确认匹配规则、媒体许可和 UI 阶段。
