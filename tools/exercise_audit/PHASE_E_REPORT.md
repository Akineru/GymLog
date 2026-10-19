# GymLog 阶段 E 报告：媒体接入、三选项详情与训练记录三栏布局

> 媒体文件因许可限制未打包进 APK。代码、UI 和本地媒体读取路径已实现；无媒体时使用占位图和“暂无动作演示”。

## 0. 备份与限制

阶段 E 开始前创建：

`C:\Users\75192\AndroidStudioProjects\GymLog-phase-e-backup-20261018-205224.zip`  
大小：103,114,534 B

没有真实用户数据库；未在设备/模拟器上做视觉验收。

## 1. 媒体盘点与映射

审计详表：[PHASE_E_MEDIA_AUDIT.md](PHASE_E_MEDIA_AUDIT.md)

| 项目 | 结果 |
| --- | ---: |
| 缩略图 | 1,324 张，全部 180×180 JPG |
| GIF | 1,324 个，全部 180×180 GIF |
| 缺失文件 | 0 |
| 成功映射 | 1,324 / 1,324 |
| 媒体原始总大小 | 137,616,454 B（约 131.2 MiB） |

映射使用数据集稳定 `datasetId`：

`0001` → `ds_0001` → `exercise_media.exerciseId`

图片/GIF 分别登记，不按名称/排序/列表位置绑定。

## 2. 许可结论

**未将任何 Gym visual 图片或 GIF 打包进 APK。**

依据：

- 数据仓库 `LICENSE` 的 MIT **不覆盖** `images/` 和 `videos/`
- `NOTICE.md` 明确媒体 © Gym visual，仓库再分发基于单独许可
- “Cloning this repository does not grant you any license to the media”
- 本项目没有 Gym visual 的独立再分发许可

因此：

- 媒体未复制到 `assets/`
- APK 仍只有 3 个结构化 `assets/exercises/` 文件
- `exercise_media` 保留源路径、媒体 ID、校验值、`NOT_BUNDLED`
- UI 使用现有示意图作为缩略图占位
- 动作演示页显示“暂无动作演示”
- 只有 `filesDir/exercise_media` 下存在已获许可的本地文件时才读取

未来获得许可后：

- 缩略图约 8.5 MiB
- GIF 约 122.8 MiB
- 合计约 131.2 MiB
- 本阶段 APK 媒体体积增加：**0**

## 3. 修改文件

### 媒体数据/读取

- `app/src/main/java/com/example/gymlog/data/ExerciseCatalogDao.kt`
  - 新增 `observeAllMedia()` Flow
- `app/src/main/java/com/example/gymlog/data/ExerciseMediaRepository.kt`（新增）
  - 按稳定 `exerciseId` 映射
  - 分别解析缩略图和 GIF
  - 本地媒体根目录 `filesDir/exercise_media`
  - 不存在/损坏返回 null
- `app/src/main/java/com/example/gymlog/ui/exercise/ExerciseMediaUi.kt`（新增）
  - IO 线程解码缩略图
  - LruCache 缩略图缓存
  - GIF `Movie` 解码与 Compose 循环播放
  - 无 GIF/解码失败显示“暂无动作演示”
  - 缩略图缺失回退 `ExerciseDiagram`
- `GymLogApplication.kt`
  - 新增 `exerciseMediaRepository`

### 动作库/详情

- `ExerciseLibraryViewModel.kt`
  - 注入 media repository
  - UI state 增加 `media: Map<String, ExerciseMediaView>`
- `GymLogApp.kt`
  - 动作库和新增训练选择器传入 media repository
  - history 页面传入媒体映射
- `ExerciseLibraryScreen.kt`
  - 卡片缩略图替换为媒体缩略图/后备示意图
  - 详情新增三个固定选项：
    1. 动作演示
    2. 肌群（正）
    3. 肌群（反）
  - 默认“动作演示”
  - 无 GIF 空状态
  - 肌群正/反强制对应面
  - 保留动作名称、PR、主次肌群、确认、删除、编辑入口
- `MuscleFigure.kt`
  - 新增 `forcedFront`，详情页可固定正/反；原有内部切换不变
- `strings.xml`
  - 动作演示/肌群正/反/暂无演示

### 训练记录

- `GymLogViewModel.kt`
  - 暴露 `exerciseMedia` StateFlow
- `GymLogApp.kt` `SessionCard`
  - 每个动作一行三栏：
    - 左：部位 / 动作名 / 容量
    - 中：缩略图或占位图
    - 右：2 列 × 3 行组数据
  - 右侧固定宽度，左侧 `weight(1f)`，长名称最多两行省略
  - 组数据按“重量 × 次数”，使用历史 `weightKg`/`reps`/`weightMode`
  - 1–5 组显示实际组
  - 6 组显示真实第 6 组
  - 7 组及以上显示前 5 组 + 第 6 格省略号
  - 不修改数据库中的任何组
- `SessionSetGridTest.kt`
  - 更新为 1/2/5/6/7/12 组

### 测试

- `ExerciseMediaRepositoryTest.kt`（新增）
  - 缺文件 → 占位状态
  - 本地缩略图解析/解码
  - 缩略图与 GIF 独立
  - 损坏 GIF 不崩溃，返回无演示
- `SessionSetGridTest.kt`
  - 1、2、5、6、7、12 组边界

## 4. Room / 数据兼容

- Room：**14**，未新增迁移
- 复用现有 `exercise_media` 字段：
  - `sourceImagePath`
  - `sourceGifPath`
  - `localThumbnailPath`
  - `localGifPath`
  - `downloadState`
  - 校验值和署名
- 未新增表、未新增列、未 destructive migration
- 未修改肌群 PNG/遮罩/manifest/MuscleMaskCatalog
- 未修改 33 个稳定肌群 ID
- 备份/恢复未改；媒体元数据本来就不在备份中，恢复后无文件时仍可运行

## 5. 实现行为

### 动作库

- 有本地缩略图：显示缩略图
- 无缩略图/文件缺失/解码失败：显示现有 `ExerciseDiagram` 后备图
- 图片解码在 `Dispatchers.IO`
- LruCache 缓存 32 张缩略图
- 连续滚动不会在主线程重复解码

### 动作详情

- 三个选项固定顺序
- 默认动作演示
- 有 GIF：循环播放
- 无 GIF/损坏：显示“暂无动作演示”
- GIF 仅在演示页 Composable 存活时运行；切换肌群页或关闭弹窗后协程取消
- 肌群正/反强制对应面，原有 `MuscleFigure` 缓存逻辑不变

### 训练记录

- 每个动作一张三栏记录
- 组数据来自历史记录，不使用当前动作默认值
- 第 6 组边界正确
- 超过 6 组只在第 6 格显示省略号，不丢弃数据库组
- 详情/编辑页仍可查看全部组

## 6. 测试与构建

执行：

```text
:app:testDebugUnitTest :app:assembleDebug --offline --no-daemon --console=plain
```

本机 `gradlew.bat` daemon 启动挂起；使用本机 Gradle 9.6.0。

结果：

- **46 个测试类、186 个测试**
- **0 失败、0 错误、0 跳过**
- **BUILD SUCCESSFUL**
- APK：`app/build/outputs/apk/debug/app-debug.apk`
- APK 大小：**27,628,290 B**
- Room：**14**
- `assets/exercises/` 仍只有 3 个结构化文件
- 未打包 Gym visual 图片/GIF

关键测试：

- `missingFilesBecomePlaceholderStatesInsteadOfPaths`
- `localLicensedFilesAreResolvedRelativeToTheAppMediaDirectory`
- `thumbnailAndGifAreIndependent`
- `gifDecodeFailureIsReportedAsNoGifInsteadOfCrashing`
- `SessionSetGridTest` 的 1/2/5/6/7/12 组边界

## 7. 未完成/未验证

1. **没有实际打包任何缩略图或 GIF**，因为许可未确认；因此动作库当前显示后备示意图，演示页显示“暂无动作演示”。这是有意的合规选择。
2. 没有在真机/模拟器上验证 GIF 播放、内存占用、滚动帧率和触控体验。
3. 没有 Compose UI 截图测试；三栏布局的长名称/大数值/小屏适配只做了代码约束，未做视觉验收。
4. 没有用 1,324 个真实 GIF 做逐帧性能测试；`Movie` 方案适合 180×180 GIF，但未做设备基线。
5. 媒体许可需要用户/项目方从 Gym visual 获取独立许可后，才能把文件放入本地媒体目录或 APK。
6. 没有真实用户数据库验证旧历史/自定义动作的媒体映射；按 ID 映射设计不依赖名称。
7. 媒体元数据仍不进入备份；这是现有设计，未改。
