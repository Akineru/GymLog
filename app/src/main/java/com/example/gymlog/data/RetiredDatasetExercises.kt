package com.example.gymlog.data

/**
 * 数据集去重（1.6.x）：同一个动作的视角版（`(side pov)` / `(后视图)`）、
 * `版本 2` / `版本 3` 变体、`（女）` 示范版、重复录入和纯翻译差异，
 * 已从 `assets/exercises/dataset_v1.json` 删除。
 *
 * 这里保留「被删数据集 id -> 保留数据集 id」对照，供三处共用：
 * - [MIGRATION_22_23]：把已导入到用户库里的旧行删掉，训练组和动作用时改挂到保留行。
 * - [MIGRATION_24_25]：后补的 4 条（`版本 3` 和 `（女）`）走这一支，对已经升到 24 的库再删一次。
 * - [GymLogSnapshot.withoutRetiredDatasetExercises]：恢复旧备份时同样过滤，避免重复动作又冒出来。
 *
 * 键值都是数据集原始 id（4 位补零，不带 `ds_` 前缀）。
 */
object RetiredDatasetExercises {

    val RETIRED_TO_KEEPER: Map<String, String> = mapOf(
        "1461" to "0043", // 杠铃 完全深蹲 (后视图)
        "1462" to "0043", // 杠铃 完全深蹲 (侧面视角)
        "0077" to "0078", // 杠铃 后弓步 版本 2
        "1371" to "0088", // 杠铃 坐姿 提踵（重复录入）
        "0097" to "0098", // 杠铃 侧 分腿深蹲 版本 2
        "0119" to "0120", // 杠铃 直立划船 版本 2
        "0125" to "0126", // 杠铃 手腕弯举 版本 2
        "0287" to "2137", // 哑铃 阿诺德推举 版本2
        "0296" to "1731", // 哑铃 窄距 推举（翻译差异）
        "1657" to "0298", // 哑铃 交叉 锤式弯举 版本 2
        "2136" to "0299", // 哑铃 古巴推举 版本 2
        "0304" to "0305", // 哑铃 下斜 耸肩 版本 2
        "0309" to "0310", // 哑铃 前平举 版本 2
        "0312" to "0313", // 哑铃 锤式弯举 版本 2
        "0317" to "0318", // 哑铃 上斜 弯举 版本 2
        "0342" to "0343", // 哑铃 仰卧 单臂 推举 版本 2
        "0360" to "0361", // 哑铃 单臂 肩推 版本 2
        "0395" to "0396", // 哑铃 坐姿 侧平举 版本 2
        "1680" to "0422", // 哑铃 站姿 单臂 弯举过上斜 凳
        "1765" to "0437", // 哑铃 直立划船 (后视图)
        "1628" to "0454", // 曲杠 蜘蛛弯举（重复录入）
        "3698" to "1471", // 尺蠖式 版本2
        "0497" to "0499", // 反向划船 版本 2
        "0513" to "0514", // 跳跃深蹲 版本 2
        "0576" to "0577", // 杠杆机 胸部推举（重复录入）
        "1439" to "0580", // 杠杆机 无握 耸肩 版本 2
        "1479" to "1299", // 杠杆机 上斜 胸部推举 版本 2
        "1614" to "0592", // 杠杆机 牧师凳弯举 版本 2
        "3760" to "1452", // 杠杆机 坐姿 卷腹 版本 2
        "0869" to "0603", // 杠杆机 肩推 版本 2
        "0656" to "0655", // 俯卧撑 (在稳定球)（重复录入）
        "0658" to "0659", // 俯卧撑 (墙) 版本 2
        "1766" to "0697", // 自我 辅助 反向腿弯举（重复录入）
        "1464" to "0739", // 雪橇机 45° 腿举 (后视图)
        "1463" to "0739", // 雪橇机 45° 腿举 (侧面视角)
        "1394" to "0763", // 史密斯 反向 提踵s（重复录入）
        "2371" to "0846", // 负重 俄罗斯转体 版本 2
        "0121" to "0120", // 杠铃 直立划船 版本 3
        "2318" to "0603", // 杠杆机 肩推 版本 3
        "2800" to "2799", // 杠铃 坐姿 交替 抬腿 （女）
        "2801" to "2802", // 扭转 抬腿 （女）
    )

    private fun localId(datasetId: String) = "ds_$datasetId"

    /** 被删除的本地动作 id（`ds_####`）。 */
    val retiredLocalIds: Set<String> = RETIRED_TO_KEEPER.keys.map(::localId).toSet()

    /** 被删 id -> 保留 id 的本地对照。 */
    val localKeeperOf: Map<String, String> =
        RETIRED_TO_KEEPER.entries.associate { (retired, keeper) -> localId(retired) to localId(keeper) }
}

/**
 * 恢复旧备份前把已下线的重复动作过滤掉：动作行丢掉，引用了它们的训练组和动作用时
 * 改挂到保留动作上，避免 `validate()` 报「指向不存在的动作」或 FK 失败。
 *
 * 保留动作不在快照里时（理论上只有手动删过才会），引用它的组一并丢掉。
 */
fun GymLogSnapshot.withoutRetiredDatasetExercises(): GymLogSnapshot {
    val keeperOf = RetiredDatasetExercises.localKeeperOf
    if (exercises.none { it.id in keeperOf }) return this
    val survivingIds = exercises.map { it.id }.filterNot { it in keeperOf }.toSet()

    val sets = sets.mapNotNull { set ->
        if (set.exerciseId !in keeperOf) return@mapNotNull set
        val keeper = keeperOf.getValue(set.exerciseId)
        if (keeper in survivingIds) set.copy(exerciseId = keeper) else null
    }
    val durations = exerciseDurations.mapNotNull { duration ->
        if (duration.exerciseId !in keeperOf) return@mapNotNull duration
        val keeper = keeperOf.getValue(duration.exerciseId)
        if (keeper in survivingIds) duration.copy(exerciseId = keeper) else null
    }
    return copy(
        exercises = exercises.filterNot { it.id in keeperOf },
        sets = sets,
        exerciseDurations = durations,
        exerciseMuscles = exerciseMuscles.filterNot { it.exerciseId in keeperOf },
    )
}
