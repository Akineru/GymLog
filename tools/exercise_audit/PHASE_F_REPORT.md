# GymLog 阶段 F 报告：正式接入授权媒体

> 阶段 F 将 Gym visual 的 1,324 张缩略图和 1,324 个 GIF 作为 APK assets 接入。代码/UI 沿用阶段 E，不重做。

## 0. 备份

阶段 F 开始前创建：

`C:\Users\75192\AndroidStudioProjects\GymLog-phase-f-backup-20261018-211054.zip`  
大小：103,125,814 B

## 1. 媒体重新扫描

原始目录：

`D:\exercises-dataset-zh-master\exercises-dataset-zh-master`

| 项目 | 结果 |
| --- | ---: |
| JSON 记录 | 1,324 |
| 缩略图文件 | **1,324** JPG |
| GIF 文件 | **1,324** GIF |
| 缩略图尺寸 | 全部 180×180 |
| GIF 尺寸 | 全部 180×180 |
| 缩略图总大小 | 8,875,057 B |
| GIF 总大小 | 128,741,397 B |
| 媒体总大小 | **137,616,454 B** |
| 缺失文件 | **0** |
| 重复 ID | **0** |
| SHA-256 不一致 | **0** |
| 成功映射 | **1,324 / 1,324** |

### 关于动作 ID

数据集的 `id` **不是连续 0001–1324**：

- 1,324 个唯一 ID
- 最小 `0001`
- 最大 `5201`
- 中间有空洞

应用不重新编号，也不按位置绑定。映射规则仍是：

```text
datasetId 0001 → ds_0001
datasetId 5201 → ds_5201
```

媒体按原始 `datasetId` 对应的 `ds_<datasetId>` 登记。用户提到的“ds_0001 到 ds_1324”是数量表述，不是实际连续 ID 范围。

## 2. 授权落实

- 项目方在阶段 F 请求中确认已获得 Gym visual 图片与 GIF 授权，包含随 APK 分发。
- 工作区和数据集目录中没有发现单独的签名授权文件。
- 已新增记录：`tools/exercise_audit/PHASE_F_AUTHORIZATION.md`
- 已更新 `app/src/main/assets/exercises/NOTICE.txt`：
  - `© Gym visual — https://gymvisual.com/`
  - 180×180 限制
  - Gym visual Terms & Conditions 链接
  - 项目方 2026-10-18 授权确认
  - 媒体随 APK 分发说明
- 若项目方有单独签署文件，应保存在项目记录中。

## 3. 资源存放方式

媒体放入：

```text
app/src/main/assets/exercises/media/images/<original image path>
app/src/main/assets/exercises/media/videos/<original gif path>
```

例如：

```text
assets/exercises/media/images/0001-2gPfomN.jpg
assets/exercises/media/videos/0001-2gPfomN.gif
```

- 保留原始相对路径，不按排序位置绑定。
- 没有复制到 `filesDir` 做首次启动全量复制；应用直接从 AssetManager 读取，避免额外 137MB 磁盘占用。
- `ExerciseMediaRepository` 先检查 `filesDir/exercise_media` 的本地覆盖文件，再检查 assets。
- 图片/GIF 分别登记，不把 GIF 当静态缩略图。
- 现有 `exercise_media` 字段足够，未新增表/字段。

## 4. APK 实际资源

APK：

`app/build/outputs/apk/debug/app-debug.apk`

实际检查 APK zip：

| 项目 | 结果 |
| --- | ---: |
| APK 内缩略图条目 | **1,324** |
| APK 内 GIF 条目 | **1,324** |
| APK 内媒体未压缩总大小 | 137,616,454 B |
| APK 内媒体压缩后总大小 | 137,616,454 B |
| APK 总大小 | **165,536,753 B** |
| 阶段 E APK | 27,628,290 B |
| 增加量 | **137,908,463 B** |
| 增加比例 | **+499.16%** |
| 倍数 | **5.99×** |

增加量高于媒体总量约 292 KB，来自代码/资源变化和 zip 结构；媒体本身未做有损压缩或格式替换。

**分发风险**：165.5 MB 超过 Google Play 常见 150 MB base APK 上限。若走 Play，应使用 App Bundle/asset delivery 或 Play Asset Delivery；本阶段仅验证 Debug APK，未改分发方案。

## 5. 应用读取

- `ExerciseMediaRepository`：
  - 稳定 `exerciseId` → `exercise_media`
  - 本地覆盖文件优先
  - 否则 `assets/exercises/media/images|videos`
  - 资源存在性通过 AssetManager 目录索引确认
- `ExerciseMediaUi`：
  - 缩略图在 `Dispatchers.IO` 从 assets/file 解码
  - LruCache 缓存
  - GIF 从 assets/file 解码为 `Movie`
  - 无文件/损坏显示占位图或“暂无动作演示”
- 动作库、动作详情、训练记录三栏布局沿用阶段 E 的 UI。
- 未做首次复制，失败不会留下“复制成功”的伪状态。
- GIF 只在动作演示页 Composable 存活时循环；切页/关闭后协程取消。

## 6. Room、迁移和备份

- Room：**14**
- **没有新增 Room schema 或 migration**
- BackupCodec：**2**
- 媒体不是数据库 BLOB，未加入备份；备份恢复不依赖媒体文件存在
- 肌群 PNG、遮罩、manifest、MuscleMaskCatalog、33 个稳定肌群 ID 未改
- 历史/PR/趋势/自定义动作/旧动作兼容逻辑未改

## 7. 测试与构建

执行：

```text
:app:testDebugUnitTest :app:assembleDebug --offline --no-daemon --console=plain
```

结果：

- **46 个测试类、187 个测试**
- **0 失败、0 错误、0 跳过**
- **BUILD SUCCESSFUL**
- APK：**165,536,753 B**
- APK 内实际缩略图/GIF：**1,324 / 1,324**

新增/更新测试：

- `ExerciseMediaRepositoryTest`
  - 缺失文件 → 占位状态
  - 本地许可覆盖文件解析与缩略图解码
  - 打包 asset 路径与稳定 ID 映射
  - 缩略图/GIF 独立
  - 损坏 GIF → 无演示且不崩溃
- `SessionSetGridTest`
  - 1、2、5、6、7、12 组边界

## 8. 未验证

1. **未在真机/模拟器上安装运行**；未验证图片显示、GIF 播放、内存、滚动帧率。
2. **Robolectric 没有真实 `android.graphics.Movie` 解码器**，测试只验证打包 GIF 路径和 GIF89a 文件头；真实 GIF 播放未验证。
3. 没有用 1,324 个真实 GIF 做逐帧解码/内存测试。
4. 没有 Compose 截图测试；训练记录三栏布局的长名称/大数值/小屏未做视觉验收。
5. 没有验证 165.5 MB APK 在低存储设备上的安装/更新体验。
6. 未验证 Google Play 150 MB 限制下的分发方案。
7. 没有真实用户数据库；媒体映射按 ID 设计，未在真实旧库上运行。

## 9. 结论

- 媒体授权已按项目方确认落实。
- 1,324 张缩略图和 1,324 个 GIF 已实际打包进 APK，并由稳定动作 ID 读取。
- 阶段 E 的三选项、缩略图和三栏训练记录布局无需重做。
- Room 仍为 14，无新增迁移，用户数据/历史/PR/肌群资源未改。
