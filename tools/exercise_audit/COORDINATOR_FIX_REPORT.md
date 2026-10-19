# GymLog 共享动作目录协调器并发修复报告

> 阶段：并发协调修复。未做 UI 切换、未改默认可见性、未改数据库版本、未改备份格式、未改肌群资源或媒体。
>
> 结论：导入、备份恢复、清除/补种现在共用同一把 `ExerciseCatalogCoordinator` 锁；全量测试和 Debug 构建通过。

## 0. 基线

- Room：**13**
- BackupCodec：**2**
- 修复前：**42 个测试类、163 个测试、0 失败**
- 修复前 APK：27,961,716 B
- 源码备份：`C:\Users\75192\AndroidStudioProjects\GymLog-acceptance-fix-backup-20261018-150049.zip`
- 工作区没有 Git；真实用户数据库不可访问，未创建或修改真实 DB。

## 1. 实施前检查结论

### 1.1 原 Mutex 生命周期

`ExerciseCatalogImporter` 原来只有实例内 `private val mutex = Mutex()`：

- `importIfNeeded()` 获取它；
- `importIfNeeded()` 内的状态检查、JSON 解析、数据库事务、失败状态写入都在锁内；
- **但备份恢复和清除不使用这把锁。**

### 1.2 原构造关系

`GymLogApplication` 原来：

- 构造一个 `ExerciseCatalogImporter`；
- 构造一个 `BackupRepository`，只传入 importer；
- importer 的 Mutex 是私有实例字段，BackupRepository 无法使用。

### 1.3 原 `importSnapshot()` 顺序

1. 校验快照；
2. 导出 `previous` 快照；
3. 捕获 `previousDataset`；
4. 读取旧头像；
5. `replaceAll(snapshot)` + `finishImport()`；
6. 调用 importer 补数据集；
7. 写头像；
8. 失败时恢复旧快照和数据集状态。

第 2 到第 8 步**不在同一个共享锁内**，启动导入可以在这之间提交，导致 `previous` / `previousDataset` 捕获自不同时间点。

### 1.4 原 `clearLocalData()` 顺序

1. `clearAll()`；
2. 补预设；
3. 补数据集；
4. 重算 PR；
5. `dataEpoch++`。

原来既没有共享锁，也没有清除失败时的完整回滚；失败后数据已经清空。

### 1.5 调用点

- 启动：`GymLogApplication.onCreate()` → importer。
- 恢复：`BackupRepository.importSnapshot()` → importer。
- 清除：`BackupRepository.clearLocalData()` → importer。
- UI：`SettingsViewModel` / `DataTransferViewModel` 只调用 `BackupRepository`，不直接调用 importer。
- 只有 `GymLogApplication` 在生产代码中构造 `BackupRepository` 和 `ExerciseCatalogImporter`。

### 1.6 死锁风险

如果直接在公开入口外再取同一把非重入 Mutex，会自锁。因此实施时必须：

- 公开入口取锁；
- 锁内方法不重复取锁；
- 备份/清除使用锁内方法；
- 不在 `importIfNeededLocked()` 内调用 `importIfNeeded()`。

## 2. 实施内容

### 2.1 新增共享协调器

新增：

`app/src/main/java/com/example/gymlog/data/ExerciseCatalogCoordinator.kt`

- 内部只有一把 `Mutex`；
- `withLock` 是唯一的锁入口；
- 锁覆盖整个 catalog 生命周期，而不是单条 SQL。

### 2.2 Importer 改造

`ExerciseCatalogImporter`：

- 构造参数增加 `ExerciseCatalogCoordinator`；
- 删除私有 Mutex；
- `importIfNeeded()`：

```kotlin
coordinator.withLock { importIfNeededLocked(force) }
```

- 新增 `importIfNeededLocked(force)`：
  - 不带任何加锁；
  - 明确要求调用方已持有同一 coordinator 锁；
  - 只由 `BackupRepository` / 启动流程在锁内调用；
  - 不在内部再次获取锁。

### 2.3 BackupRepository 改造

`BackupRepository`：

- 构造参数增加共享 coordinator；
- 默认值从 `exerciseCatalogImporter?.coordinator` 派生，避免测试/手动构造时意外使用不同实例；
- App 中显式传入同一个 coordinator。

`importSnapshot()` 现在在 `coordinator.withLock` 内完成：

1. `snapshot.validate()`；
2. `exportSnapshot()`；
3. `captureDatasetState()`；
4. 读取旧头像；
5. `replaceAll` + `finishImport`；
6. `importIfNeededLocked(force = false)`；
7. 写头像；
8. 失败时恢复旧快照 + 完整数据集状态 + 旧头像；
9. 成功后才 `dataEpoch++`。

### 2.4 clearLocalData 改造

`clearLocalData()` 现在在 `coordinator.withLock` 内：

1. 先 `exportSnapshot()` + `captureDatasetState()`；
2. `backupDao.clearAll()`；
3. `ensurePresetsSeeded()`；
4. `importIfNeededLocked(force = false)`；
5. `rebuildAllPersonalRecords()`；
6. 成功后才 `dataEpoch++`。

失败策略：

- 捕获异常；
- 在事务中恢复旧快照 + 完整数据集状态；
- 恢复失败作为 suppressed 附加；
- 原异常重新抛出；
- **不报告成功，不吞异常。**

说明：清除成功后会真正删除自定义动作、训练组、会话、旧 PR、计时等；失败时通过上述恢复路径保留旧状态。头像和进行中的秒表偏好本来就不由这条路径删除。

### 2.5 启动流程纳入协调锁

`GymLogApplication.onCreate()` 现在在 coordinator 锁内执行：

1. `ensurePresetsSeeded()`；
2. `importIfNeededLocked(force = false)`；
3. `rebuildAllPersonalRecords()`。

这样启动补种/导入/PR 重算不会与备份恢复或清除交错。

## 3. 协调器实例共享证据

生产代码中：

- `GymLogApplication.exerciseCatalogCoordinator` 只创建一次；
- `GymLogApplication.exerciseCatalogImporter` 注入该 coordinator；
- `GymLogApplication.backupRepository` 注入同一个 coordinator；
- `BackupRepository` 的默认 coordinator 也从同一个 importer 派生；
- 没有第二个生产 `BackupRepository` 或 `ExerciseCatalogImporter` 构造点。

调用链：

```text
GymLogApplication
  ├─ exerciseCatalogCoordinator
  ├─ exerciseCatalogImporter(coordinator)
  ├─ backupRepository(coordinator, importer)
  └─ onCreate
       └─ coordinator.withLock {
            ensurePresetsSeeded
            importer.importIfNeededLocked
            rebuildAllPersonalRecords
          }

BackupRepository.importSnapshot
  └─ coordinator.withLock {
       exportSnapshot
       captureDatasetState
       replaceAll
       finishImport
       importer.importIfNeededLocked
       applyAvatar
       rollback on failure
     }

BackupRepository.clearLocalData
  └─ coordinator.withLock {
       capture previous
       clearAll
       seed
       importer.importIfNeededLocked
       rebuild PR
       rollback on failure
     }
```

## 4. 锁获取/释放范围与死锁检查

### 锁范围

- 公开 `importIfNeeded()`：整次导入。
- `importSnapshot()`：快照导出、状态捕获、替换、补种、数据集导入、头像、回滚、成功计数。
- `clearLocalData()`：旧状态捕获、清空、补种、数据集导入、PR 重算、失败恢复、成功计数。
- 启动：预设补种 + 数据集导入 + PR 重算。

### 重入检查

- `importIfNeeded()` 是唯一在 importer 内取锁的入口。
- `importIfNeededLocked()` 不再取锁。
- `BackupRepository` 只在已经持有 coordinator 锁时调用 `importIfNeededLocked()`。
- `clearLocalData()` 同样只在锁内调用 locked 方法。
- 没有在锁内调用 `importIfNeeded()` 的生产路径。
- 新增测试使用 `CompletableDeferred` 控制交错，并设置 15 秒 `withTimeout`。

### 已知边界

- `importIfNeededLocked()` 是公开方法，供同模块的 BackupRepository/启动流程使用；错误调用（未持锁）不会被运行时强制检查。代码注释和调用点审计保证当前生产路径正确。
- `BackupRepository` 允许注入自定义 `finishImport`；如果未来在该 lambda 内调用公开 `importIfNeeded()`，会造成同锁重入死锁。当前默认 lambda 只补预设/PR，测试 lambda 只抛异常。

## 5. 新增/修改测试

新增：

`app/src/test/java/com/example/gymlog/ExerciseCatalogCoordinatorTest.kt`

4 个测试：

1. `concurrentImportsAndClearUseOneCoordinator`
   - 两个并发 `importIfNeeded()` + 一个并发 `clearLocalData()`；
   - 断言只有一次有效导入，最终 1,324 条；
   - 验证内容、媒体、来源映射、词元、候选、肌群关联、导入状态。
2. `importAndRestoreSerializeAndEndInOneConsistentCatalog`
   - `BlockingImporter` 先持锁；
   - 恢复任务在锁外等待；
   - 释放后用 `CompletableDeferred` 让导入和恢复按确定顺序完成；
   - 验证最终目录、old custom/训练组、无孤立外键。
3. `clearFailureRestoresPreviousCatalogInsteadOfReportingSuccess`
   - 清除后数据集补种注入失败；
   - 断言异常抛出、旧 1,324 条数据与 custom 保留、无孤立外键。
4. `failedRestoreDoesNotOverwriteConcurrentImport`
   - 导入持锁，失败恢复等待；
   - 导入完成后恢复失败并回滚；
   - 断言另一个操作的有效导入结果没有被覆盖，计数和导入状态一致。

修改：

- `ExerciseCatalogBackupFailureTest` 的 `FailingImporter` 同时覆盖 `importIfNeededLocked()`，确保失败注入仍作用于备份恢复路径。
- `GymLogApplication` 启动流程增加共享锁。
- `airead.md` 同步协调器、清除失败恢复语义。

## 6. 验收结果

实际构建环境说明：`gradlew.bat` 在本机启动单次 daemon 时挂起，未进入编译；改用同一 Gradle 9.6.0 发行版的本地 `gradle.bat`，设置 `JAVA_HOME=D:\Android studio\jbr` 后执行：

```text
:app:testDebugUnitTest :app:assembleDebug --offline --no-daemon --console=plain
```

结果：

- **43 个测试类、167 个测试、0 失败、0 错误、0 跳过**
- 新增：4 个协调器测试（163 → 167）
- **BUILD SUCCESSFUL**
- APK：`app/build/outputs/apk/debug/app-debug.apk`
- APK 大小：**27,961,716 B**
- APK 内 `assets/exercises/` 仍只有：
  - `NOTICE.txt`
  - `dataset_v1.json`
  - `preset_match_candidates.json`
- **没有** Gym visual 图片/GIF

## 7. 与基线差异

| 项目 | 修复前 | 修复后 |
| --- | --- | --- |
| 测试类 | 42 | **43** |
| 测试数 | 163 | **167** |
| 失败 | 0 | **0** |
| Room | 13 | **13** |
| BackupCodec | 2 | **2** |
| APK | 27,961,716 B | **27,961,716 B** |
| 数据集动作 | 1,324 | **1,324** |
| 旧预置 | 89 | **89** |
| 稳定肌群 ID | 33 | **33** |
| Gym visual 媒体 | 无 | **无** |

## 8. 保护范围核对

未修改：

- `ExerciseLibraryScreen` / `ExerciseLibraryViewModel` 的 UI 行为；
- 动作库默认可见性；
- `GymLogDatabase` 版本与迁移；
- `BackupCodec` 格式；
- 肌群 PNG、遮罩、manifest、`MuscleMaskCatalog`；
- 媒体资源和媒体加载逻辑；
- 旧 preset / dataset / custom ID 规则；
- 历史训练、PR、趋势解析逻辑。

未删除或重新生成真实用户数据。当前没有真实用户数据库可供验证。

## 9. 未验证项

1. 没有真实用户数据库或真机运行；并发修复只在 Robolectric/内存 Room 中验证。
2. 没有验证 Android 真机上的文件头像写入时序、低内存和进程被杀场景。
3. `ExerciseLibraryViewModel` 仍会调用 `ensurePresetsSeeded()`；它是 `insertIgnore` 的旧预置补种，不在协调锁内，但不会覆盖或删除数据集/custom。生产启动路径已在锁内。
4. `importIfNeededLocked()` 未持锁调用属于代码约定，不是运行时强制。
5. `clearLocalData()` 的“清除成功”仍会真正删除用户数据；失败时已实现恢复，但恢复本身失败只能附加 suppressed，不能保证物理上 100% 恢复。
6. 媒体许可、Gym visual 媒体、GIF/缩略图仍未解决。
7. `gradlew.bat` 的 daemon 启动问题属于本机环境，未作为项目修复内容。
