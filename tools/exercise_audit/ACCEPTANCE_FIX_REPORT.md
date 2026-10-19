# GymLog 第二阶段验收修复报告

> 阶段：验收修复。未改 UI、未改肌群 PNG/遮罩/manifest/MuscleMaskCatalog、未添加 Gym visual 媒体。
>
> 结论：审查发现的三类问题已修复并补测；全量测试和 Debug 构建通过。媒体许可仍未确认。

## 0. 保护与基线

- 源码备份：`C:\Users\75192\AndroidStudioProjects\GymLog-acceptance-fix-backup-20261018-150049.zip`
- 基线记录：[`baseline_before_acceptance_fix.json`](baseline_before_acceptance_fix.json)
- Room：**13**
- BackupCodec：**VERSION 2**（未改）
- 旧预置：**89**
- 稳定肌群 ID：**33**
- 修复前测试：**39 个测试类、154 个测试、0 失败**
- 修复前 APK：27,961,716 B
- 工作区没有可访问的真实用户 `gymlog.db`；未创建、未修改真实数据库。

## 1. 修改文件

### 数据层

- `app/src/main/java/com/example/gymlog/data/ExerciseCatalogImporter.kt`
  - 单飞 `Mutex`
  - 失败状态 `failed = 1`
  - `force` 保留媒体本地状态、确认候选、已有数据集肌群关联
  - 可测试的 `seedJsonOverride`
- `app/src/main/java/com/example/gymlog/data/BackupRepository.kt`
  - 在恢复前完整捕获数据集表（内容、媒体、来源映射、词元、候选、存储肌群关联、导入状态）
  - 失败时在单事务中恢复旧快照 + 完整数据集状态
  - 恢复失败异常不再吞掉
  - `clearLocalData()` 不再用 `runCatching` 吞掉补种/重算失败
- `app/src/main/java/com/example/gymlog/data/ExerciseCatalogDao.kt`
  - 增加全量读取方法，供回滚快照使用
- `app/src/main/java/com/example/gymlog/data/ProfileAvatarStore.kt`
  - 改为可测试的 open 类/方法，用于头像写入失败注入；生产行为不变

### 测试

- `ExerciseCatalogImportTest.kt`
  - 并发两次导入
  - 导入 + `clearLocalData()` 竞争
  - JSON 解析失败 → 失败状态 → 重试
  - 数据库写入失败 → 事务回滚 → 重试
  - `force = true` 保留媒体/确认候选/已有数据集肌群关联
- `ExerciseCatalogBackupFailureTest.kt`（新增）
  - 数据集导入失败后完整回滚
  - 头像写入失败后完整回滚
  - 无孤立外键
  - 重试成功
- `ExerciseCatalogFullMigrationTest.kt`（新增）
  - 完整 v12 schema（动作、训练组、训练记录、肌群、PR、归档、计时、备注）
  - 执行 `MIGRATION_12_13`
  - 用 Room 实际打开并触发 schema 验证
  - 验证旧字段、训练组外键、肌群关联
  - 验证新表外键级联
- `ExerciseCatalogBackupCompatibilityTest.kt`（新增）
  - 旧 v1 快照恢复 + 数据集补种
  - v2 快照恢复
  - 恢复 → 清除 → 恢复

## 2. 修复详情

### 2.1 备份失败回滚

`BackupRepository.importSnapshot()` 原先的问题：

- `replaceAll(previous)` 只恢复 `exercises` 和自定义肌群关联；
- 数据集内容、媒体、来源映射、词元、匹配候选、导入状态、dataset 肌群关联会丢。

修复：

1. 导入前捕获完整 `DatasetState`：
   - `exercise_content`
   - `exercise_media`
   - `exercise_source_map`
   - `exercise_muscle_tokens`
   - `exercise_preset_match_candidate`
   - `exercise_import_state`
   - `exercise_muscles` 的**实际存储行**（不是快照里推导出的 preset links）
2. 失败时在单个 Room 事务中：
   - `backupDao.replaceAll(previous)` 恢复核心快照；
   - 恢复上述完整数据集状态；
   - 恢复实际存储的 `exercise_muscles`；
   - 恢复头像文件。
3. 回滚本身失败时把异常作为 suppressed 附加到原异常并重新抛出，不报告成功。
4. 失败后再次恢复使用同一完整状态，测试验证可成功。

### 2.2 导入并发单飞

`ExerciseCatalogImporter` 增加 `Mutex`：

- 整个 `importIfNeeded()` 在锁内：
  - 早期状态检查
  - JSON 读取/解析
  - 数据库事务
  - 失败状态写入
- 不是只锁解析或只锁事务。
- 启动导入、恢复补种、清除补种共用同一个 importer 实例，因此共用同一把锁。
- 锁内没有递归调用同一个 importer，无重入死锁。
- 并发测试：两个 `importIfNeeded()` 同时执行，只有一个真正 added=1324；另一个返回 `alreadyImported=true`；最终行数/内容/媒体/映射/词元/关联/候选一致。
- 导入 + `clearLocalData()` 竞争测试最终得到一份一致的 1,324 条目录，无重复。

### 2.3 `force = true` 语义

`force` 现在是“刷新来源元数据”，不是“重置用户状态”：

保留：

- 媒体 `localThumbnailPath` / `localGifPath` / `remoteGifUrl` / `downloadState`
- 非自动的候选状态（如未来 `CONFIRMED`）
- 已有数据集 `exercise_muscles` 关联（包括未来人工确认）
- 数据集动作 `archived` 状态、PR、旧动作、自定义动作

刷新：

- 数据集 action 的结构化字段（名称、英文名、部位、器械、weightMode、sourceId）
- `exercise_content`
- 媒体源路径/尺寸/署名/校验值
- `exercise_muscle_tokens`
- 自动候选状态（PENDING/AMBIGUOUS/NO_MATCH）

不刷新/不重建：

- 已有数据集肌群关联（避免覆盖人工确认；当前没有 UI 修改入口）
- 媒体下载状态（避免把已下载重置为 `NOT_BUNDLED`）

测试 `forceImportPreservesMediaConfirmedCandidateAndExistingLinks` 覆盖以上保留项。

### 2.4 失败统计

采用整批原子导入策略：

- 任一必需记录/数据库写入失败：整个数据集事务回滚。
- 在失败后另起事务写入 `ExerciseImportState(source=..., failed=1, recordCount=...)`。
- 异常向调用方抛出；App 记录 error。
- 下次调用：`previous.failed == 1`，不会走“已完成”捷径，会重试。
- 成功事务提交后写 `failed=0`，统计只反映最终提交结果。
- 失败状态不伪装成 `failed=0`。
- 失败状态没有单独的错误文本列；具体错误由抛出的异常和 App 日志携带。

测试覆盖：

- JSON 解析失败 → `failed=1` → 重试成功
- 数据库写入失败（`source/sourceId` 唯一冲突）→ 事务回滚 → `failed=1` → 重试成功
- 数据集导入失败 → 完整备份回滚
- 头像失败 → 完整备份回滚

### 2.5 完整 v12 迁移测试

新增 `ExerciseCatalogFullMigrationTest`：

- 手工创建完整 v12 schema，包括：
  - `exercises`（旧字段）
  - `workout_sessions`
  - `workout_sets`（外键）
  - `exercise_muscles`
  - `workout_records`
  - `body_profile`
  - `body_measurements`
  - `day_notes`
  - `timer_records` / `timer_laps`
  - 旧索引
- 写入旧预置、自定义、训练组、PR、归档、肌群关联、计时、备注。
- 执行 `MIGRATION_12_13`。
- 用 Room 打开触发 schema 校验。
- 验证：
  - 旧 ID/名称/部位/器械/weightMode/别名/PR/归档/自定义标记
  - `workout_sets` 外键
  - `exercise_muscles`
  - 旧记录/计时/备注
  - 新表初始为空
  - 新表插入后删除父动作，内容/媒体/来源映射/词元/肌群关联按外键级联删除

## 3. 测试结果

执行：

```text
:app:testDebugUnitTest --offline
:app:assembleDebug --offline
```

结果：

- **42 个测试类、163 个测试、0 失败、0 错误、0 跳过**
- Debug：**BUILD SUCCESSFUL**
- APK：`app/build/outputs/apk/debug/app-debug.apk`
- APK 大小：**27,961,716 B**
- APK 内 `assets/exercises/` 仍只有：
  - `NOTICE.txt`
  - `dataset_v1.json`
  - `preset_match_candidates.json`
- **没有** Gym visual 缩略图/GIF 被打包

## 4. 兼容性与保护

- `BackupCodec.VERSION` 仍为 2；旧 v1/v2 的字段含义未变。
- 新增字段继续保持可选。
- v1/v2 恢复 + 数据集补种测试通过。
- 恢复 → 清除 → 恢复测试通过。
- 旧 89 个预置、旧 PR、归档、训练组外键在测试中保留。
- 数据集动作仍只用 `ds_*`；旧 `preset_*`/`custom_*` 未改写。
- 23 个待确认候选、1 个歧义、65 个无匹配、12 个泛化词元、15 个未映射词元均未被自动关联。
- 33 个稳定肌群 ID 未改。
- 肌群 PNG/遮罩/manifest/MuscleMaskCatalog 未改。
- 没有添加任何 Gym visual 媒体；媒体许可仍未确认。

## 5. 剩余未解决 / 未验证

1. 23 个旧预置候选仍未人工确认，未关联旧 ID。
2. 1 个 `pull_up` 的 `pull-up`/`chin-up` 歧义仍未决策。
3. 65 个无精确匹配预置仍需人工/模糊匹配。
4. 12 个泛化肌群词元仍未确认规则；15 个词元仍无对应 ID。
5. 内容表仍只导入 zh/en；源数据还有 8 种语言。
6. 没有真实用户数据库；实际 custom/组/PR 数量未测量。
7. 首次启动导入耗时、首屏阻塞和真机 UI 未验证。
8. 没有独立测试“import 与 backup restore 同时执行”；已有 import 与 clear 竞争测试。
9. 失败状态没有单独错误文本列；具体异常在调用方/日志中。
10. 媒体下载、缓存、损坏降级、许可均未实现/确认。

## 6. 构建产物

`C:\Users\75192\AndroidStudioProjects\GymLog\app\build\outputs\apk\debug\app-debug.apk`
