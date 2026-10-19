# 肌群遮罩重绘 — 第一步盘点基线

本目录是第三阶段“完整重绘 33 个肌群遮罩”的审计/预览工作区。**正式资源没有被修改。**

## 备份

- 完整项目备份：`C:\Users\75192\AndroidStudioProjects\GymLog-full-backup-20261018-132219.zip`
- 独立肌群资源备份：`C:\Users\75192\AndroidStudioProjects\GymLog-muscle-assets-backup-20261018-132210.zip`

独立备份包含 66 张遮罩 PNG、2 张底图、66 个 SVG 源文件、`muscle_manifest.json` 和 `README.md`。

## 实际资源路径（已从代码与文件核实）

- 正面底图：`app/src/main/res/drawable-nodpi/muscle_front_base.png`
- 背面底图：`app/src/main/res/drawable-nodpi/muscle_back_base.png`
- 正面遮罩：`app/src/main/assets/muscle_masks/front/*.png`
- 背面遮罩：`app/src/main/assets/muscle_masks/back/*.png`
- 清单：`app/src/main/assets/muscle_manifest.json`

应用运行时通过 `MuscleMaskCatalog` 的显式映射读取 `muscle_masks/<front|back>/<fileName>.png`；`deltoid_lateral` 使用 `deltoid_middle.png`。底图和全部遮罩均为 **1024×1536**。

## 盘点结果

- 33 个稳定肌群 ID。
- 正面 33 个 PNG、背面 33 个 PNG（共 66 个遮罩 PNG）；每面另有 33 个 SVG 源文件。
- 正面实际可见 16 个遮罩，背面实际可见 14 个遮罩；`forearm_extensors` 正背面都有。
- 4 个肌群当前无独立遮罩：`rhomboids`、`brachialis`、`internal_oblique`、`transversus_abdominis`。
- `sartorius` 正面有独立遮罩，背面为空；`deltoid_lateral` 的素材文件名是 `deltoid_middle.png`。
- 所有既有 PNG 的 alpha 阈值统计、bbox、SHA-256 和状态在 `inventory.json` / `inventory.csv` 中。

## 机器可读清单

- `inventory.json`：完整结构，含路径、底图哈希、文件列表、每个肌群的正背面文件、状态、像素数、bbox、父/子关系、别名和审核备注。
- `inventory.csv`：便于表格查看的扁平版本。

字段说明：

- `frontStatus` / `backStatus`：`drawn`、`empty_on_this_side`、`no_independent_mask`。
- `overallStatus`：`drawn` 或 `no_independent_mask`。
- `parent` / `children` / `relation`：`quadriceps` 与 `rectus_femoris`、`vastus_lateralis`、`vastus_medialis` 的合并/子头关系。
- `assetAlias`：`deltoid_lateral` → `deltoid_middle`。
- `reviewStatus`：当前全部为 `pending_human_review`。

## 基线预览

`baseline/`：

- `front_all_masks.png` / `back_all_masks.png`：每面 33 个肌群的编号接触表；空遮罩显示 EMPTY。
- `front_partition.png` / `back_partition.png`：底图上叠加当面前全部可见遮罩的彩色分区总览，带编号和图例。
- 编号与 `inventory.json` 的 `index` 一致（`#01`–`#33`）。

`previews/front/<id>.png`、`previews/back/<id>.png`：

- 每个肌群单独一张 512×768 预览；红色仅用于区分遮罩，不代表训练状态。
- 空侧只显示底图和 `EMPTY` 标记。

## 候选重绘集（未写入正式资源）

第三阶段候选集生成在独立目录：

`C:\Users\75192\AndroidStudioProjects\GymLog-muscle-redraw-final3-20261018-1420`

该目录包含 66 张候选 PNG、全量单独预览、前后对比、彩色分区和质量报告。正式
`app/src/main/assets/muscle_masks` 未被覆盖；候选集等待人工验收后再决定是否替换。

## 注意

- 预览使用辅助红色/彩色，不是应用内的最终训练/选中配色。
- 现有 `tools/muscle_mask_audit/` 的像素审计仍可参考；本目录的基线是“重绘前”的对照。
- 重绘产物应先写入独立临时目录，经过人工验收后才替换正式资源。
