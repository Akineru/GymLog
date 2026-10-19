# Phase C 分类审计与迁移基线

> 数据源：`app/src/main/assets/exercises/dataset_v1.json` 与 `PresetExercises.kt`。
> 时间：2026-10-18。真实用户数据库不可访问；custom 实机数量无法统计。

## 0. 备份与阶段 C 前基线

- 源码备份：`C:\Users\75192\AndroidStudioProjects\GymLog-phase-c-backup-20261018-174347.zip`
- 阶段 C 前：Room 13、BackupCodec 2
- 阶段 C 前测试：44 个测试类、178 个测试、0 失败
- 阶段 C 前 APK：27,436,049 B
- 数据集动作：1,324
- 旧 preset：89
- 旧 `bodyPart`：7 类；旧 `equipmentType`：3 类
- 稳定肌群 ID：33（未改）

## 1. 新数据集真实分类

### 1.1 身体部位：`bodyPartDetail`

实际 **10 类**：

| 稳定 ID | 原始值 | 显示 | 动作数 |
| --- | --- | --- | ---: |
| body_chest | chest | 胸部 | 163 |
| body_back | back | 背部 | 203 |
| body_shoulders | shoulders | 肩部 | 143 |
| body_upper_arms | upper arms | 上臂 | 292 |
| body_lower_arms | lower arms | 前臂 | 37 |
| body_upper_legs | upper legs | 大腿 | 227 |
| body_lower_legs | lower legs | 小腿 | 59 |
| body_waist | waist | 腰部 | 169 |
| body_cardio | cardio | 有氧 | 29 |
| body_neck | neck | 颈部 | 2 |
| **合计** | | | **1,324** |

数据集原始的旧 `bodyPart` 七分类仍保留在 `exercises.bodyPart`：

| 原始值 | 动作数 |
| --- | ---: |
| ARMS | 329 |
| LEGS | 286 |
| BACK | 203 |
| ABS | 169 |
| CHEST | 163 |
| SHOULDERS | 143 |
| OTHER | 31 |

### 1.2 器械：`equipmentDetail`

实际 **28 类**：

| 稳定 ID | 原始值 | 显示 | 动作数 |
| --- | --- | --- | ---: |
| equipment_body_weight | body weight | 自重 | 325 |
| equipment_barbell | barbell | 杠铃 | 154 |
| equipment_dumbbell | dumbbell | 哑铃 | 294 |
| equipment_cable | cable | 绳索 | 157 |
| equipment_leverage_machine | leverage machine | 杠杆器械 | 81 |
| equipment_smith_machine | smith machine | 史密斯机 | 48 |
| equipment_kettlebell | kettlebell | 壶铃 | 41 |
| equipment_band | band | 弹力带 | 54 |
| equipment_weighted | weighted | 负重 | 36 |
| equipment_stability_ball | stability ball | 健身球 | 28 |
| equipment_ez_barbell | ez barbell | EZ 杠铃 | 23 |
| equipment_assisted | assisted | 辅助 | 15 |
| equipment_sled_machine | sled machine | 雪橇机 | 15 |
| equipment_medicine_ball | medicine ball | 药球 | 13 |
| equipment_rope | rope | 绳索 | 10 |
| equipment_roller | roller | 滚轮 | 8 |
| equipment_resistance_band | resistance band | 阻力带 | 7 |
| equipment_bosu_ball | bosu ball | 波速球 | 3 |
| equipment_olympic_barbell | olympic barbell | 奥运杠铃 | 2 |
| equipment_wheel_roller | wheel roller | 健腹轮 | 2 |
| equipment_upper_body_ergometer | upper body ergometer | 上肢功率车 | 1 |
| equipment_skierg_machine | skierg machine | 滑雪机 | 1 |
| equipment_hammer | hammer | 锤 | 1 |
| equipment_stationary_bike | stationary bike | 固定自行车 | 1 |
| equipment_tire | tire | 轮胎 | 1 |
| equipment_trap_bar | trap bar | 六角杠铃 | 1 |
| equipment_elliptical_machine | elliptical machine | 椭圆机 | 1 |
| equipment_stepmill_machine | stepmill machine | 登山机 | 1 |
| **合计** | | | **1,324** |

数据集原始的旧 `equipmentType` 三分类仍保留在 `exercises.equipmentType`：

| 原始值 | 动作数 |
| --- | ---: |
| FREE_WEIGHT | 960 |
| MACHINE | 306 |
| OTHER | 58 |

## 2. 旧 preset 分类基线

89 个 `preset_*`：

| 旧 `bodyPart` | 数量 | 新处理 |
| --- | ---: | --- |
| CHEST | 14 | `body_chest`（标准） |
| SHOULDERS | 11 | `body_shoulders`（标准） |
| BACK | 15 | `body_back`（标准） |
| ABS | 12 | `body_waist`（标准） |
| ARMS | 14 | `body_legacy_arms`（兼容/待确认） |
| LEGS | 19 | `body_legacy_legs`（兼容/待确认） |
| OTHER | 4 | `body_legacy_other`（兼容/待确认） |

| 旧 `equipmentType` | 数量 | 新处理 |
| --- | ---: | --- |
| FREE_WEIGHT | 63 | `equipment_legacy_free_weight` |
| MACHINE | 23 | `equipment_legacy_machine` |
| OTHER | 3 | `equipment_legacy_other` |

旧 preset 迁移统计：

- 身体部位明确映射到标准分类：**52**
- 身体部位保留兼容分类，待确认：**37**
- 身体部位未映射：**0**
- 器械明确映射到 28 类中的某一类：**0**
- 器械保留旧三类兼容分类：**89**
- 完全达到“身体 + 器械都是新标准分类”：**0**
- 因器械为旧三类兼容而标记为 `AMBIGUOUS`：**89**
- 完全未映射：**0**

原因：旧 `equipmentType` 只有 FREE_WEIGHT/MACHINE/OTHER，不能在不猜测动作名称的前提下可靠拆成 28 类；旧 `bodyPart` 的 ARMS/LEGS 也不能可靠拆分上臂/前臂、大腿/小腿。因此不猜测、不编造，保留兼容分类并标记待确认。

## 3. Custom 动作

真实用户数据库不可访问，**实际 custom 数量无法测量**。迁移规则：

- 保留原 ID、名称、别名、PR、归档状态。
- 保留原 `bodyPart` / `equipmentType`。
- 旧 body CHEST/BACK/SHOULDERS/ABS 可映射标准；ARMS/LEGS/OTHER 进入兼容分类。
- 旧 equipment 三类进入兼容分类，不猜 28 类。
- 用户录入信息不被静默覆盖。
- 新建自定义动作直接选择新标准分类，写入稳定 ID 和 `MAPPED`。

## 4. 数据库结构变化

Room：**13 → 14**。

`MIGRATION_13_14` 增量添加：

- `exercises.bodyCategoryId TEXT`
- `exercises.equipmentCategoryId TEXT`
- `exercises.classificationStatus TEXT NOT NULL DEFAULT 'UNMIGRATED'`
- `exercises.classificationVersion INTEGER NOT NULL DEFAULT 0`
- 两个索引

保留：

- `bodyPart` / `equipmentType`（旧兼容/历史分组）
- `bodyPartDetail` / `equipmentDetail`（原始值）
- 所有旧 ID、PR、归档、训练组、肌群关联

`ExerciseClassificationMigrator`：

- 逐行、事务内补齐分类；
- `classificationVersion = 1` 后不重复覆盖；
- 已确认分类（version 1 + 两个 ID）不会被重复迁移改写；
- 失败时事务回滚，行保持旧版本，下次重试。

## 5. 风险与限制

1. 旧器械无法可靠细分；大量旧动作保留兼容分类，属于明确标记的存量债务。
2. custom 实机数量未知。
3. 历史首页/日历/训练编辑优先使用 `bodyCategoryId`；只有没有分类 ID 的极旧行才回退到旧 broad `bodyPart`。迁移后正常行使用新分类，回退不会丢失历史。
4. 数据集媒体仍未打包，媒体许可未解决。
5. 无真机/真实用户库验证。
