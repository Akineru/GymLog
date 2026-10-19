# 阶段 E 媒体盘点与许可审计

数据源：

`D:\exercises-dataset-zh-master\exercises-dataset-zh-master`

## 1. 文件盘点

| 项目 | 结果 |
| --- | ---: |
| JSON 记录 | 1,324 |
| 缩略图引用 | 1,324 |
| 缩略图文件 | 1,324 |
| 缺失缩略图 | 0 |
| GIF 引用 | 1,324 |
| GIF 文件 | 1,324 |
| 缺失 GIF | 0 |
| 缩略图格式 | 全部 JPG |
| 缩略图尺寸 | 全部 180×180 |
| 缩略图总大小 | 8,875,057 B |
| GIF 格式 | 全部 GIF |
| GIF 尺寸 | 全部 180×180 |
| GIF 总大小 | 128,741,397 B |
| GIF 帧数 | 12–54 帧；常见 12 帧 |
| 媒体总大小 | 137,616,454 B |

源文件路径在 `data/exercises.json` 中逐条指向：

- `image`：`images/0001-....jpg`
- `gif_url`：`videos/0001-....gif`

## 2. 动作 ID 映射

使用数据集稳定的数字 `id`：

- `datasetId = 0001` → `ds_0001`
- 现有 `ExerciseCatalogImporter` 已按 `datasetId` 写入 `exercise_media.exerciseId`
- 图片和 GIF 分别登记为 `sourceImagePath` / `sourceGifPath`
- `mediaId` 保持源数据
- **不按名称映射，不按列表位置映射，不合并同名动作**

审计结果：1,324 条记录全部能按 `datasetId` 对应到 `ds_<id>`，文件路径 1,324/1,324 存在。

## 3. 许可核查

根目录 `LICENSE` 明确：

- MIT 只覆盖代码、工具、数据结构和说明文字
- **不覆盖 `images/` 和 `videos/`**
- 媒体 © Gym visual，仓库中的再分发是基于 Gym visual 的单独许可
- 使用和再使用受 Gym visual Terms & Conditions 约束
- “Cloning this repository does not grant you any license to the media; obtain your own from Gym visual.”

`NOTICE.md` 同样说明：

- 媒体为 Gym visual 所有
- 仓库仅按权利人许可在 180×180 下分发
- 在自己的项目中使用需要自行审查条款并向 Gym visual 获取许可

## 4. 接入结论

**不把 1,324 张图片或 1,324 个 GIF 复制进 APK。**

原因：

- 当前项目没有 Gym visual 的独立再分发许可。
- 媒体许可不覆盖本 APK。
- 全量媒体约 131.2 MiB，也会显著增加 APK。
- 用户要求不得擅自打包整套媒体。

因此：

- APK 不包含 Gym visual 图片/GIF
- 现有 `exercise_media` 记录仍登记来源、媒体 ID、校验值和 `NOT_BUNDLED`
- UI 对无媒体动作显示现有示意图占位
- 动作演示页显示“暂无动作演示”
- 只有在 `filesDir/exercise_media` 下存在**已获许可**的本地文件时，才读取和显示
- 不提供自动从数据集目录复制到 APK 的路径；避免误打包

## 5. 潜在存储影响

若未来取得许可并选择本地部署：

- 仅缩略图：约 8.5 MiB
- GIF：约 122.8 MiB
- 两者合计：约 131.2 MiB

本阶段未复制任何媒体文件，APK 媒体体积增加为 **0**。

## 6. 未完成

- 未获得 Gym visual 的独立许可，未打包媒体。
- 未在真机验证 GIF 播放性能和内存释放。
- 未对 1,324 个 GIF 做逐帧解码性能测试。
