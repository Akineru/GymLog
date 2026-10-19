# GymLog 动作库本地重构：第一阶段审计报告

> 状态：只读审计完成，等待确认数据模型和迁移策略。  
> 本阶段未修改 `app/src`、Room 数据库、正式动作资源、训练记录、备份逻辑或肌群素材。

## 0. 结论摘要

- 数据集实际为 **1,324 条记录**，不是“约 1,324”；ID 唯一，媒体完整。
- 媒体总量 **131.24 MiB**：1,324 张 180×180 缩略图（8.46 MiB）+ 1,324 个 180×180 GIF（122.78 MiB）。
- 1,324 条记录都有图片和 GIF，引用无缺失、无损坏、无重复哈希、无多余文件。
- 数据字段齐全；10 种语言的说明和分步说明都存在。
- 中文名映射实际 **1,318 条**（按英文名索引），不是 README 写的 1,319；6 个英文名重复，涉及 12 条记录共用中文名。
- 数据集有 50 个肌群词元；按 33 个稳定肌群 ID 可候选映射 35 个，15 个没有直接对应 ID。
- 现有 89 个预置动作中，按现有别名/键名只能精确匹配 23 个、歧义 1 个、65 个需要人工/模糊匹配。
- 不能把数据集 ID 直接当作 GymLog 主键；不能用数据集中文名覆盖旧动作名；不能删除或重命名旧 `preset_*`。
- 不建议把 1,324 个 GIF 全部打进 APK：仅 GIF 就 122.78 MiB，当前 Debug APK 约 27.5 MB。
- 数据集代码/数据是 MIT；**媒体版权归 Gym visual**，180×180 分发并要求署名，不能仅凭 MIT 自由再分发。

完整机器可读审计：

- [`dataset_audit.json`](dataset_audit.json)
- [`mapping_audit.json`](mapping_audit.json)
- [`dataset_records.csv`](dataset_records.csv)
- [`dataset_media.csv`](dataset_media.csv)
- [`body_part_mapping.csv`](body_part_mapping.csv)
- [`equipment_mapping.csv`](equipment_mapping.csv)
- [`muscle_mapping_candidates.csv`](muscle_mapping_candidates.csv)
- [`preset_match_candidates.csv`](preset_match_candidates.csv)
- [`duplicate_names.csv`](duplicate_names.csv)

## 1. 数据集实际结构与数量

路径：

`D:\exercises-dataset-zh-master\exercises-dataset-zh-master`

```text
data/
  exercises.json             17,391,530 bytes，1,324 条
  exercises.schema.json      JSON Schema 2020-12
  name_zh.json               81,790 bytes，1,318 条英文名→中文名
  name_zh.js                 浏览器用中文名映射
images/                      1,324 个 .jpg，180×180
videos/                      1,324 个 .gif，180×180
```

实际记录：

- 记录数：**1,324**
- 唯一 ID：1,324
- 唯一英文名：**1,318**（6 个英文名重复）
- 唯一 `media_id`：1,324
- 记录 ID 范围：`0001`–`5201`，中间不连续；ID 为 4 位零填充字符串
- 每条记录字段：
  - `id`, `name`, `category`, `body_part`, `equipment`
  - `instructions`（10 语言字符串）
  - `instruction_steps`（10 语言字符串数组）
  - `muscle_group`, `secondary_muscles`, `target`
  - `image`, `gif_url`, `media_id`, `created_at`, `attribution`
- `category` 与 `body_part` 对全部 1,324 条完全相同。
- `created_at`：2026-03-18。
- `attribution`：1,324/1,324 条都有 `© Gym visual — https://gymvisual.com/`。

`image`/`gif_url` 命名规则：

```text
images/<4位ID>-<media_id>.jpg
videos/<4位ID>-<media_id>.gif
```

所有 1,324 条引用都与实际文件名、ID、`media_id` 一致。

## 2. 数据质量与媒体完整率

### 2.1 文件完整性

| 检查 | 结果 |
| --- | --- |
| 缩略图数量 | 1,324 |
| GIF 数量 | 1,324 |
| 缩略图格式/尺寸 | 全部 .jpg / 180×180 |
| GIF 格式/尺寸 | 全部 .gif / 180×180 |
| 引用缺失图片 | 0 |
| 引用缺失 GIF | 0 |
| 未被引用的图片 | 0 |
| 未被引用的 GIF | 0 |
| 解码失败 | 0 |
| 重复图片哈希 | 0 |
| 重复 GIF 哈希 | 0 |
| 文件名与 `id-media_id` 不一致 | 0 |

### 2.2 体积

| 类型 | 数量 | 总大小 | 平均值 | 最小/最大 |
| --- | ---: | ---: | ---: | ---: |
| 缩略图 | 1,324 | 8,875,057 B（8.46 MiB） | 6,703.2 B | 3,324 / 11,118 B |
| GIF | 1,324 | 128,741,397 B（122.78 MiB） | 97,236.7 B | 46,144 / 232,578 B |
| 媒体合计 | 2,648 | 137,616,454 B（131.24 MiB） | — | — |
| JSON | 1 | 17,391,530 B（16.59 MiB） | — | — |

GIF 帧数：**12–54 帧**，平均 **12.99 帧**；其中 1,206 个为 12 帧。

### 2.3 缺失字段

按 Schema 的 required 字段检查：**无缺失**。  
10 种语言的 `instructions` 和 `instruction_steps` 都是 1,324/1,324 条齐全，没有空数组或空字符串。

### 2.4 中文名

- `name_zh.json` 是 **英文名 → 中文名**，不是 `id → 中文名`。
- 实际 1,318 条，覆盖 1,318 个唯一英文名；按名字查 **无缺失**。
- 1,324 条记录中，有 1,318 个唯一名字，6 个名字重复；因此 12 条记录的中文名存在歧义。
- 有 1 个重复中文值：`哑铃 窄距 推举`。
- README 的“1,319 个中文动作名”与实际 1,318 条不一致，差 1。
- 数据集中文翻译质量不统一（例如 `all fours squad stretch` 被译为“四肢 s股四头肌 拉伸”）；不能把中文名当作稳定主键。

### 2.5 重复英文名

| 英文名 | 数据集 ID | 中文名 | 媒体 ID |
| --- | --- | --- | --- |
| barbell seated calf raise | 0088 / 1371 | 杠铃 坐姿 提踵 | ktsFQAZ / ipvgBnC |
| ez barbell spider curl | 0454 / 1628 | 曲杠 蜘蛛弯举 | Ye5Qxb0 / 2kattbR |
| lever chest press | 0576 / 0577 | 杠杆机 胸部推举 | DOoWcnA / T0yTjgW |
| push-up (on stability ball) | 0655 / 0656 | 俯卧撑 (在稳定球) | Y4BRNQF / tgryw5Y |
| self assisted inverse leg curl | 0697 / 1766 | 自我 辅助 反向腿弯举 | GwYwElT / ZSY3MsL |
| smith reverse calf raises | 0763 / 1394 | 史密斯 反向 提踵s | ywaNfuh / Lsqrgh4 |

这 6 组必须按 **数据集 ID** 分开，不能按名称合并；中文显示需要补充器械/动作细节来区分。

## 3. 数据集分类

### 3.1 `body_part` / `category`

`body_part` 和 `category` 完全相同。实际分布：

| 数据集值 | 数量 | 候选 `BodyPart` |
| --- | ---: | --- |
| upper arms | 292 | ARMS |
| upper legs | 227 | LEGS |
| back | 203 | BACK |
| waist | 169 | ABS |
| chest | 163 | CHEST |
| shoulders | 143 | SHOULDERS |
| lower legs | 59 | LEGS |
| lower arms | 37 | ARMS |
| cardio | 29 | OTHER |
| neck | 2 | OTHER |

如果保持现有 7 个 `BodyPart`，`cardio` 和 `neck` 只能落到 `OTHER`，会丢失筛选语义；建议新增 `bodyPartDetail` 字段，暂不扩展枚举，或者单独讨论是否新增 `CARDIO`/`NECK`。

### 3.2 `equipment`

数据集有 **28 种**器械值，现有 `EquipmentType` 只有 3 个枚举。候选粗映射：

- `FREE_WEIGHT` 960 条
- `MACHINE` 306 条
- `OTHER` 57 条
- 未映射：`tire` 1 条

完整逐项表见 [`equipment_mapping.csv`](equipment_mapping.csv)。  
必须新增 `equipmentDetail` 字符串保存原始器械，否则“杠铃/哑铃/绳索/史密斯机”等二级筛选会丢失。

### 3.3 `weightMode`

数据集没有重量模式字段；以下是**派生候选**，不是数据集事实：

- `BODYWEIGHT`：340 条（body weight、assisted）
- `WEIGHTED`：948 条
- `BODYWEIGHT_PLUS`：36 条（weighted）

必须在导入规则中写明派生逻辑；`assisted` 不应误判为普通重量，`weighted` 不应误判为普通器械重量。

## 4. 肌群词元与 33 个稳定 ID

数据集共有 **50 个肌群词元**（target + muscle_group + secondary_muscles 去重）。  
候选映射结果：

- 35 个可映射到现有稳定 ID
- 15 个没有直接对应 ID
- 1,283/1,324 条记录的 `target` 可映射
- 1,324/1,324 条至少有一个可映射词元
- 41 条记录的 `target` 无法映射（29 cardiovascular system、5 serratus anterior、5 abductors、2 levator scapulae）

### 4.1 可映射词元

| 数据集词元 | 次数 | 候选稳定 ID | 备注 |
| --- | ---: | --- | --- |
| shoulders | 591 | deltoid_anterior / deltoid_lateral / deltoid_posterior | 泛化，需人工决定是否三块都标 |
| triceps | 570 | triceps_brachii | 直接 |
| biceps | 509 | biceps_brachii | 直接 |
| forearms | 479 | forearm_flexors / forearm_extensors | 泛化，不可只取一块 |
| hamstrings | 444 | hamstrings | 直接 |
| glutes | 351 | gluteus_maximus | 泛化；数据集没有单独 gluteus medius 词元 |
| quadriceps | 271 | quadriceps | 父级复合 |
| calves | 217 | gastrocnemius / soleus | 泛化 |
| abs | 169 | rectus_abdominis | 直接 |
| pectorals | 158 | pectoralis_major | 直接 |
| chest | 144 | pectoralis_major | 泛化 |
| delts | 143 | 三个 deltoid | 泛化 |
| obliques | 139 | external_oblique | 直接 |
| upper back | 126 | trapezius_middle | 不精确；可能是斜方肌/菱形肌/背阔肌 |
| core | 101 | rectus_abdominis | 泛化 |
| lats | 83 | latissimus_dorsi | 直接 |
| trapezius | 83 | 三个 trapezius | 泛化 |
| traps | 80 | 三个 trapezius | 泛化 |
| lower back | 77 | erector_spinae | 候选 |
| rhomboids | 56 | rhomboids | 稳定 ID 存在，但当前无独立遮罩 |
| deltoids | 52 | 三个 deltoid | 泛化 |
| quads | 44 | quadriceps | 直接 |
| rear deltoids | 20 | deltoid_posterior | 直接 |
| spine | 19 | erector_spinae | 候选 |
| brachialis | 14 | brachialis | 稳定 ID 存在，无独立遮罩 |
| soleus | 8 | soleus | 直接 |
| latissimus dorsi | 7 | latissimus_dorsi | 直接 |
| adductors | 6 | adductors | 直接 |
| upper chest | 3 | pectoralis_major | 候选 |
| wrist flexors | 4 | forearm_flexors | 候选 |
| wrist extensors | 3 | forearm_extensors | 候选 |
| abdominals | 4 | rectus_abdominis | 候选 |
| lower abs | 1 | rectus_abdominis | 泛化 |
| inner thighs | 1 | adductors | 候选 |
| shins | 1 | tibialis_anterior | 候选 |

完整 50 行见 [`muscle_mapping_candidates.csv`](muscle_mapping_candidates.csv)。

### 4.2 无直接对应的词元

| 词元 | 次数 | 处理建议 |
| --- | ---: | --- |
| hip flexors | 143 | 无稳定 ID；不要硬套到股四头肌 |
| cardiovascular system | 29 | 不属于骨骼肌；`OTHER`/cardio |
| ankles | 22 | 无稳定 ID |
| back | 11 | 过泛；不要猜 |
| rotator cuff | 10 | 无稳定 ID |
| feet | 8 | 无稳定 ID |
| serratus anterior | 5 | 无稳定 ID |
| abductors | 5 | 不要自动等同臀中肌 |
| ankle stabilizers | 5 | 无稳定 ID |
| wrists | 4 | 无稳定 ID |
| hands | 3 | 无稳定 ID |
| levator scapulae | 2 | 无稳定 ID |
| sternocleidomastoid | 2 | 无稳定 ID |
| groin | 1 | 不宜自动等同内收肌 |
| grip muscles | 1 | 不宜自动等同前臂屈肌 |

这些不能被静默丢弃或强行映射；需要在导入结果中保留 `unmapped` 记录，并在详情里显示“未配置肌群”或只显示部分映射。

## 5. 与现有项目对照

### 5.1 当前动作数据模型

`ExerciseEntity`（`app/src/main/java/com/example/gymlog/data/ExerciseEntity.kt`）：

- `id: String` 主键
- `name`, `bodyPart`, `equipmentType`, `weightMode`
- `isCustom`, `aliases`, `createdAt`, `archived`
- 做组 PR / 极限 PR 的 8 个字段

其他：

- `BodyPart`：7 个：CHEST、SHOULDERS、BACK、LEGS、ARMS、ABS、OTHER
- `EquipmentType`：3 个：FREE_WEIGHT、MACHINE、OTHER
- `WeightMode`：3 个：WEIGHTED、BODYWEIGHT、BODYWEIGHT_PLUS
- 预置动作：**89 个**（`PresetExercises`）
- 预置肌群映射：**89 个**（`PresetMuscleMappings`）
- 稳定肌群：**33 个**（`MuscleCatalog`）
- Room：**version 12**
- 备份格式：`BackupCodec.VERSION = 2`

### 5.2 训练记录如何引用动作

- `workout_sets.exerciseId` 是 **字符串外键**，指向 `exercises.id`，`onDelete = RESTRICT`。
- `workout_sessions` 只存会话日期/备注；组通过 `exerciseId` 关联动作。
- `ExerciseEntity` 的 PR 字段由训练组重建；`WorkoutRepository.rebuildAllPersonalRecords()` 依赖动作/组。
- 日历、容量、趋势通过 `exercises.bodyPart` 和 `workout_sets.exerciseId` 统计。
- 遗留 `workout_records` 只存 `exerciseName`，是旧表，仍参与备份；新记录不写它。
- 所以：**改动作 ID、删除动作、合并同名动作、改已有动作的 `bodyPart`，都会影响历史训练/统计/PR/备份。**

### 5.3 现有 ID 的使用点

- `ExerciseEntity.id` 主键，`preset_<key>` / `custom_<uuid>`。
- `WorkoutSetEntity.exerciseId` 外键。
- `ExerciseMuscleEntity.exerciseId` 外键。
- `ExerciseDiagram` 用 `id.removePrefix("preset_")` 找 89 张示意图。
- `PresetMuscleMappings` 以 `preset_<key>` 为键。
- `BackupCodec` 导出 `exercises.id`、`sets.exerciseId`、`exerciseMuscles.exerciseId`。
- `ExerciseDao.observeUsage()` 按 `workout_sets.exerciseId` 统计。
- `ExerciseDao.getById()`、`archive()`、`updatePersonalRecords()` 都按 ID。
- `GymLogSnapshot.validate()` 要求每个 set 的 `exerciseId` 存在于 exercises。

### 5.4 旧预置与数据集的精确匹配

按现有 `PresetExercises` 的中文名、英文别名、键名做精确/规范化匹配：

- 唯一匹配：**23 个**
- 歧义：**1 个**：`pull_up`（引体向上）可匹配 `0652`（pull-up）和 `1326`（chin-up）
- 无精确匹配：**65 个**

无匹配的主要原因是数据集名称带器械前缀和后缀（例如 `barbell incline bench press` vs 现有别名 `incline bench`），并不代表数据集没有该动作。不能据此自动删除或重命名旧动作；需要人工/模糊匹配表。

`pull_up` 的歧义说明：`pull-up` 与 `chin-up` 在数据集是两条动作，不能按中文同名合并。

### 5.5 同名/重复风险

- 数据集内部 6 组英文同名（见 2.5）；必须按数据集 ID 分开。
- 数据集 1,318 个唯一英文名，对应 1,318 条中文名；12 条记录中文名有歧义。
- 现有 89 个预置动作与新数据集按名称存在潜在重叠；精确匹配只能确认 23 个，其他需要逐条确认。
- 现有 `ExerciseDao.countByName()` 是 `COLLATE NOCASE` 的简单同名检查；导入新数据不能直接走 `addCustomExercise()`，否则 6 组同名和大量中文重复会被拒绝或产生歧义。

## 6. 建议的新本地数据模型

核心原则：**旧 ID 永远不改；新数据只新增或链接。**

### 6.1 `ExerciseEntity`（保留原表，增加可选字段）

```text
id                  String   PK   // preset_..., custom_..., ds_...
name                String        // 旧动作保持旧中文名；新动作用中文名
nameEn              String?       // 新增，英文名
source              String        // LEGACY / DATASET / CUSTOM
sourceId            String?       // 数据集 "0001"；旧预置为 null 或 id
bodyPart            String        // 继续使用现有 7 类
equipmentType       String        // 继续使用现有 3 类
equipmentDetail     String?       // 新增，原始 equipment 字符串
weightMode          String
isCustom            Boolean
aliases             String?
createdAt           Long
archived            Boolean
PR 字段保持不变
```

### 6.2 内容表

```text
exercise_content(
  exerciseId TEXT NOT NULL,
  locale TEXT NOT NULL,          // zh, en; 其他语言可选
  displayName TEXT,
  instructions TEXT,
  instructionStepsJson TEXT,
  PRIMARY KEY(exerciseId, locale),
  FOREIGN KEY(exerciseId) REFERENCES exercises(id) ON DELETE CASCADE
)
```

这样列表查询不会把所有语言的长文本一起读出来；详情页按需取 `zh`，缺失回退 `en`。

### 6.3 媒体表

```text
exercise_media(
  exerciseId TEXT NOT NULL,
  mediaId TEXT,
  thumbnailPath TEXT?,           // assets 相对路径
  gifPath TEXT?,                 // assets 或 filesDir 相对路径
  remoteGifUrl TEXT?,            // 若采用按需下载
  width INTEGER,
  height INTEGER,
  attribution TEXT,
  sha256 TEXT?,
  downloadState TEXT,            // bundled/downloaded/missing
  PRIMARY KEY(exerciseId),
  FOREIGN KEY(exerciseId) REFERENCES exercises(id) ON DELETE CASCADE
)
```

GIF 不存 BLOB，只存路径/状态。

### 6.4 外部动作映射表

```text
exercise_source_map(
  source TEXT NOT NULL,          // exercises-dataset-zh
  sourceId TEXT NOT NULL,        // 0001
  localExerciseId TEXT NOT NULL, // 旧 preset_... 或新 ds_...
  matchType TEXT,                // exact/manual/fuzzy
  confidence REAL,
  PRIMARY KEY(source, sourceId),
  FOREIGN KEY(localExerciseId) REFERENCES exercises(id) ON DELETE CASCADE
)
```

用途：

- 23 个可精确匹配的旧预置：把数据集内容/媒体挂到旧 `preset_*`，不新建重复行。
- 65 个待确认：先人工匹配，确认后再写入映射。
- 真正的新动作：插入 `ds_0001` 等新 ID，再写映射。

### 6.5 肌群关系

继续使用现有 `exercise_muscles`：

- `exerciseId` + `muscleId` + `role` + `sortOrder`
- 33 个稳定 ID 不变
- 自定义动作的映射继续存表
- 数据集动作的映射可由导入器写入，或在代码中维护；必须在备份恢复后重新补齐

不建议把 `quadriceps` 改成三个子头，也不建议删除 `rhomboids`、`brachialis`、`internal_oblique`、`transversus_abdominis`。

## 7. 稳定 ID 与旧动作兼容策略

### 7.1 新 ID

- 旧预置：`preset_<key>` 保持不变
- 自定义：`custom_<uuid>` 保持不变
- 新导入动作：`ds_0001`、`ds_0002`……（`sourceId` 存 `0001`）

不要直接用 `0001` 作为 `ExerciseEntity.id`：虽然目前不与旧 ID 冲突，但加前缀能避免未来外部源/导入批次冲突，也便于备份识别。

### 7.2 匹配顺序

1. 用现有 89 个预置的中文名、别名、键名与数据集做人工/规范化匹配；
2. 唯一匹配 → 写 `exercise_source_map`，把内容/媒体挂到旧 ID；
3. 歧义（如 pull-up/chin-up）→ 人工选择；
4. 无匹配 → 检查是否真为新动作；确认后插入 `ds_*`；
5. 绝不按英文名或中文名直接合并 6 组重复名。

### 7.3 旧字段不可变

- 不改旧动作的 `name`、`bodyPart`、`equipmentType`、`weightMode`；
- 不删除旧预置，不把旧预置归档；
- 不改变旧动作的 PR 字段；
- 新数据集的 `equipmentDetail` 只加在新字段，不回填旧字段。

## 8. Room 数据库迁移方案

当前 version 12。建议 **12→13**，只加不删：

```sql
ALTER TABLE exercises ADD COLUMN nameEn TEXT;
ALTER TABLE exercises ADD COLUMN source TEXT NOT NULL DEFAULT 'LEGACY';
ALTER TABLE exercises ADD COLUMN sourceId TEXT;
ALTER TABLE exercises ADD COLUMN equipmentDetail TEXT;

CREATE TABLE exercise_content (...);
CREATE TABLE exercise_media (...);
CREATE TABLE exercise_source_map (...);
CREATE INDEX ...;
```

迁移注意：

- 旧行全部回填 `source='LEGACY'`；`sourceId` 留空或等于 id；
- 不移动/不重建 `workout_sets`，不改 FK；
- `exercise_muscles` 不动；
- 不在迁移中插入 1,324 条数据；种子在后台事务中执行；
- 必须补 `MIGRATION_12_13` 到 `GymLogApplication`；
- 不能使用 `fallbackToDestructiveMigration`；
- 迁移后先跑旧数据完整性检查，再导入新数据。

种子策略：

- `ensureCatalogSeeded()`：按 `source + sourceId` 幂等插入/更新静态内容；
- 旧 `ensurePresetsSeeded()` 继续保留，避免旧 89 个动作消失；
- 不在主线程解析 17 MB JSON；放 `Dispatchers.IO`，分批/事务插入；
- 增加 `catalogVersion` 或 `SharedPreferences` 版本，避免每次启动重复导入 1,324 条；
- 已归档动作不自动恢复；自定义动作不覆盖。

## 9. 备份与恢复兼容

当前 `BackupCodec.VERSION = 2`：

- `exercises`、`sessions`、`sets`、`exerciseMuscles` 等已有字段含义不能改。
- 新增 `nameEn`/`source`/`sourceId`/`equipmentDetail` 作为**可选字段**：旧备份没有这些键时使用默认值；旧版本 GymLog 读到新备份时忽略未知键。
- 不建议把 1,324 条完整多语言说明和 GIF 二进制塞进备份；备份仍只存动作核心行、组、PR 和自定义肌群映射。
- 内容/媒体表不导出；导入后由 `ensureCatalogSeeded()` 重新补齐。
- `BackupDao.replaceAll()` 会先清空 `exercises`；导入后必须调用 `ensureCatalogSeeded()`，否则旧备份只有 89 个预置，新的 1,324 条不会出现。
- `exerciseMuscles`：
  - 自定义映射继续导出/导入；
  - 预置/数据集映射从代码或种子重建；
  - 不要因为恢复备份把自定义映射覆盖掉。
- `GymLogSnapshot.validate()` 的 `MISSING_EXERCISE` 逻辑不能放宽；导入前要确保 1,324 条种子在清空/替换后能补回。
- 继续支持 version 1/2 旧备份；不要提高版本号除非确实增加不可选字段。
- 清除本地数据后：清表 → 补旧 89 + 新 1,324 → 重建 PR；`dataEpoch++`，现有页面 ViewModel 逻辑不变。

## 10. 缩略图与 GIF 的本地存储方案

### 10.1 体积结论

| 方案 | 增量 | 评价 |
| --- | ---: | --- |
| 全部 1,324 缩略图 | +8.46 MiB | 可接受，建议全部内置/优化 |
| 全部 1,324 GIF | +122.78 MiB | 不建议进基础 APK |
| 89 个旧预置 GIF | 约 +8.3 MiB（按平均 97 KB 估算） | 可接受的折中 |
| 缩略图 + 89 GIF | 约 +17 MiB | 当前 27.5 MB APK 约到 45 MB |

全部媒体加进 APK 会让安装包超过 150 MB，且 GIF 已被压缩，APK 再压缩收益有限。

### 10.2 推荐方案

**推荐：全部缩略图内置；GIF 走可选下载包/远程，详情页按需加载。**

```text
assets/exercises/thumbs/<id>.webp      // 全部 1,324，优先 WebP
filesDir/exercises/gifs/<id>.gif       // 下载/导入后存在，不打包进基础 APK
```

如果产品要求完全离线：

- 只内置当前 89 个预置动作的 GIF；其余先显示缩略图 + “下载/暂无动画”；
- 后续通过可选媒体包补齐；
- 不要把 1,324 个 GIF 全部塞进基础 APK。

### 10.3 加载策略

- 列表：只加载 180×180 缩略图；`LazyVerticalGrid` 只解码可见项；内存/磁盘缓存限制。
- 详情：用户打开动作详情后才加载 GIF；一次只解码一个动画；关闭详情释放。
- 不把 GIF 放进列表，不在主线程解码，不用 Room BLOB。
- GIF 若放 `assets`，应使用不压缩方式（`noCompress "gif"`）以支持流式读取；若下载到 `filesDir`，用本地路径。
- 缩略图全量 1,324 张按 ARGB_8888 约 164 MiB，不能全部常驻；只缓存可见/最近使用。
- 一个 12 帧 GIF 约 1.5 MiB，54 帧约 6.7 MiB；单详情页可接受，但不要预加载多个。

## 11. 现有分类差异与迁移风险

| 项目 | 现有 | 数据集 | 风险 |
| --- | --- | --- | --- |
| 部位 | 7 类 | 10 个值 | `cardio`/`neck` 只能落 OTHER；需 `bodyPartDetail` 或扩展枚举 |
| 器械 | 3 类 | 28 种 | 必须加 `equipmentDetail`，否则二级筛选丢失 |
| 重量模式 | 3 类，手工/预设 | 无字段 | 340/948/36 是派生值，需人工确认 |
| 肌群 | 33 稳定 ID | 50 词元 | 15 个无对应；不能强行映射 |
| 物品 ID | `preset_`/`custom_` | 4 位数字 | 不能直接替换；需 `ds_` + 映射表 |
| 名称 | 中文为主 | 英文 + 机器中文 | 中文质量不一；6 组同名 |
| 媒体 | 89 张示意图 | 1,324 缩略图 + GIF | 体积、版权、加载方式都需重构 |
| 说明 | 无 | 10 语言长文本 | 需新内容表和详情 UI；不能塞进列表行 |

历史记录风险：

- 旧 `preset_*` 被删除/重命名 → `workout_sets` 外键失败或历史丢失。
- 旧动作 `bodyPart` 被改 → 日历/容量/趋势历史颜色和统计改变。
- 旧动作 ID 被数据集 ID 替换 → 备份 `MISSING_EXERCISE`、PR 丢失、组指向不存在动作。
- 同名合并 → 两个不同动作的训练历史混在一起。
- 把 1,324 条全部当作“预置”重建 → `ensurePresetsSeeded()` 可能覆盖/忽略旧归档状态。

## 12. 建议的分阶段实施计划

1. **审计与决策（本阶段）**  
   确认 15 个未映射肌群词元、65 个待匹配预置、6 组同名、4 个无遮罩稳定 ID 的处理规则。

2. **数据模型与迁移**  
   12→13 迁移；新增字段/内容/媒体/映射表；旧 89 + 自定义 + 历史/PR/备份验证。

3. **导入器与种子**  
   裁剪 JSON（只保留 zh/en 或 zh + fallback）；后台事务；1,324 条幂等导入；映射表；导入报告。

4. **媒体层**  
   缩略图内置/优化；GIF 下载包或子集；媒体完整性校验；按需加载。

5. **UI 重构**  
   列表缩略图；详情说明/GIF；二级器械筛选；未映射肌群提示；保留旧动作/自定义编辑。

6. **回归与验收**  
   迁移、备份、训练历史、PR、统计、搜索、性能、APK 体积、设备测试。

## 13. 测试与验收标准

### 13.1 自动测试

- 数据集导入：1,324 条；ID 唯一；1,318 唯一英文名；6 组同名按 ID 分开；媒体引用 100%。
- 中文映射：1,318 条覆盖全部唯一英文名；12 条重复名记录有明确歧义标记。
- 分类映射：10 个 body_part 全部有目标；28 个 equipment 全部有粗分类/详情；`tire` 明确落 OTHER。
- 肌群映射：35 个映射词元；15 个 unmapped 不静默丢失；1,283 条 target 映射；41 条 target-unmapped 有标记。
- 旧预置：89 个 ID 全部存在；旧 sets 全部可解析；PR 不变；归档状态不变。
- 迁移 12→13：旧 89 + 自定义 + sets + 肌群映射 + PR + 备份字段全部保留。
- 备份：version 1/2 旧文件可导入；新备份 roundtrip；`MISSING_EXERCISE` 不出现。
- 媒体：缩略图尺寸 180×180；缺失回退；GIF 只在详情加载；不主线程解码。
- 性能：1,324 条列表可滚动；搜索/筛选可接受；启动种子不阻塞 UI。

### 13.2 验收标准

- 旧动作 ID、训练记录、PR、日历、容量、时长、备份全部不回归。
- 33 个稳定肌群 ID 不新增/不删除/不改名；无遮罩 ID 仍按现状处理。
- 1,324 条导入数量与数据集实际一致；不把 1,318 当 1,324。
- 65 个待匹配预置、6 组同名、15 个未映射词元都有明确人工结论。
- APK 体积在预算内；GIF 不全部进基础包。
- 数据集媒体署名可见，许可范围已确认。

## 14. 需要你确认的事项

1. **15 个无对应肌群词元**是否接受“未配置/仅部分高亮”，还是需要新增稳定肌群 ID？  
   建议：不新增、不猜，先明确 unmapped。
2. **`shoulders`、`forearms`、`calves`、`traps`、`glutes`** 这类泛化词元：三块全标、只标一块，还是保持“未配置”？  
   建议：先人工建映射表，不要自动三块全标。
3. **65 个无精确匹配的旧预置**：是否允许做人工/模糊匹配表？  
   不能直接按名称自动导入，否则会重复或错配。
4. **6 组同名数据集动作**：是否允许在列表中显示同名，用器械/详情区分？  
   建议：按数据集 ID 保留，不合并。
5. **4 个无独立遮罩的稳定 ID**（`rhomboids`、`brachialis`、`internal_oblique`、`transversus_abdominis`）：数据集有词元，是否仍显示“未单独标出”？  
   建议：保持现状。
6. **媒体策略**：全部缩略图内置 + GIF 可选下载，还是只内置 89/精选 GIF？  
   不能全量 GIF 进基础 APK。
7. **`bodyPart` 是否扩展 `CARDIO`/`NECK`**，还是保留 7 类并把 cardio/neck 放 OTHER？
8. **许可**：Gym visual 媒体是否允许 GymLog 以当前方式分发/打包？必须按 `NOTICE.md` 保留署名并确认 180×180 限制。

---

**本阶段只做审计与方案设计，未修改项目源码、数据库、正式动作资源或肌群素材。** 等待确认上述决策后，再进入数据模型和迁移实现。
