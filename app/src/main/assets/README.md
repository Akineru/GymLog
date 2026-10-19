# Traivol 训练肌群图素材包 v2

这个素材包包含灰白色正面/背面人体底图，以及与底图使用同一坐标系的独立肌肉遮罩，供 Cursor 接入现有 Android App。

## 素材清单
- `base/muscle_front_base.png`：正面人体底图，透明背景，1024×1536。
- `base/muscle_back_base.png`：背面人体底图，透明背景，1024×1536。
- `masks/front/<muscle_id>.png`：正面肌肉遮罩。
- `masks/back/<muscle_id>.png`：背面肌肉遮罩。
- 每个遮罩同时提供 `.svg` 矢量源文件。
- `muscle_manifest.json`：中文名称、英文名称、区域、正反面是否有可视遮罩，以及接入注意事项。
- `preview/`：透明背景预览和高亮示例。
- `docs/CURSOR_PROMPT_zh.txt`：可直接复制给 Cursor 的提示词。

## 遮罩如何工作
所有底图和遮罩均为 1024×1536 画布，坐标完全一致。遮罩中的白色不透明区域代表要着色的肌肉，其他部分透明。底图和遮罩必须以完全相同的目标矩形、缩放比例和偏移绘制，不能单独裁剪或缩放遮罩。

建议主要肌群使用红色、次要肌群使用浅红色，并把遮罩以约 0.35–0.5 的透明度覆盖到底图上，让原始肌肉纹理和轮廓仍然可见。

## 接入建议
- 将两个底图 PNG 复制到 `app/src/main/res/drawable-nodpi/`。
- 将 `masks/front/` 和 `masks/back/` 中的 PNG 保持目录结构，复制到 `app/src/main/assets/muscle_masks/front/` 与 `app/src/main/assets/muscle_masks/back/`，避免正反面同名资源冲突。SVG 文件留在素材包中作为可编辑源文件，不需要复制到 Android drawable 资源目录。
- Cursor 必须检查当前应用的真实 muscleId，并通过语义显式映射本包中的 `muscleIdHint`，不要为了匹配文件名而改数据库 ID。
- `quadriceps` 是由股直肌、股外侧肌、股内侧肌组成的合并遮罩。不要同时高亮这个合并遮罩及其三个子遮罩，否则颜色会叠加变深。

## 重要限制
这是一套用于 App 原型的自定义插画和手工校准遮罩，不是临床级解剖图谱。遮罩轮廓是人工近似，需要在模拟器中逐块检查。胸小肌、肱肌、菱形肌、腹内斜肌、腹横肌、缝匠肌等难以从当前浅层人体外观准确分离的肌肉，相关视图遮罩有意保持全透明；不要将它们错误映射到附近表层肌肉。`muscle_manifest.json` 标注了每个区域的实际可视状态。
