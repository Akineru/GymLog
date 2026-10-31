package com.example.gymlog.data

/**
 * Legacy test/history fixture only. Production never seeds these rows.
 * Old rows remain readable for history when they already exist in a database.
 */
@Deprecated("Legacy preset fixture; never seed in production")
object PresetExercises {
    fun all(createdAt: Long): List<ExerciseEntity> = listOf(
        preset("barbell_bench_press", "杠铃卧推", BodyPart.CHEST, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "平板卧推, 平板杠铃卧推, 卧推", createdAt),
        preset("dumbbell_bench_press", "哑铃卧推", BodyPart.CHEST, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "哑铃卧推, 平板哑铃卧推", createdAt),
        preset("incline_bench_press", "上斜杠铃卧推", BodyPart.CHEST, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "上斜卧推, 上斜杠铃卧推", createdAt),
        preset("decline_bench_press", "下斜卧推", BodyPart.CHEST, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "下斜卧推, 下斜杠铃卧推", createdAt),
        preset("dumbbell_fly", "哑铃飞鸟", BodyPart.CHEST, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "哑铃飞鸟, 平板飞鸟, 飞鸟", createdAt),
        preset("cable_crossover", "绳索夹胸", BodyPart.CHEST, EquipmentType.MACHINE, WeightMode.WEIGHTED, "绳索夹胸, 龙门架夹胸, 十字夹胸", createdAt),
        preset("chest_press_machine", "器械推胸", BodyPart.CHEST, EquipmentType.MACHINE, WeightMode.WEIGHTED, "坐姿推胸, 器械推胸", createdAt),
        preset("pec_deck", "蝴蝶机夹胸", BodyPart.CHEST, EquipmentType.MACHINE, WeightMode.WEIGHTED, "蝴蝶机夹胸, 蝴蝶机, 夹胸器", createdAt),
        preset("push_up", "俯卧撑", BodyPart.CHEST, EquipmentType.FREE_WEIGHT, WeightMode.BODYWEIGHT, "俯卧撑, 掌上压", createdAt),

        preset("overhead_press", "杠铃推举", BodyPart.SHOULDERS, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "站姿杠铃推举, 军式推举, 推举", createdAt),
        preset("dumbbell_shoulder_press", "哑铃推举", BodyPart.SHOULDERS, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "哑铃肩推, 哑铃推举", createdAt),
        preset("lateral_raise", "侧平举", BodyPart.SHOULDERS, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "哑铃侧平举, 侧平举", createdAt),
        preset("front_raise", "前平举", BodyPart.SHOULDERS, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "哑铃前平举, 前平举", createdAt),
        preset("rear_delt_fly", "反向飞鸟", BodyPart.SHOULDERS, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "俯身飞鸟, 后束飞鸟, 反向飞鸟", createdAt),
        preset("face_pull", "面拉", BodyPart.SHOULDERS, EquipmentType.MACHINE, WeightMode.WEIGHTED, "绳索面拉, 面拉", createdAt),
        preset("arnold_press", "阿诺德推举", BodyPart.SHOULDERS, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "阿诺德推举", createdAt),
        preset("shoulder_press_machine", "器械推肩", BodyPart.SHOULDERS, EquipmentType.MACHINE, WeightMode.WEIGHTED, "器械肩推, 推肩器械", createdAt),

        preset("pull_up", "引体向上", BodyPart.BACK, EquipmentType.FREE_WEIGHT, WeightMode.BODYWEIGHT, "引体向上, 正手引体", createdAt),
        preset("lat_pulldown", "高位下拉", BodyPart.BACK, EquipmentType.MACHINE, WeightMode.WEIGHTED, "高位下拉, 下拉", createdAt),
        preset("barbell_row", "杠铃划船", BodyPart.BACK, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "杠铃划船, 俯身杠铃划船", createdAt),
        preset("dumbbell_row", "哑铃划船", BodyPart.BACK, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "哑铃划船, 单臂哑铃划船", createdAt),
        preset("seated_row", "坐姿划船", BodyPart.BACK, EquipmentType.MACHINE, WeightMode.WEIGHTED, "坐姿划船, 绳索划船", createdAt),
        preset("deadlift", "硬拉", BodyPart.BACK, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "硬拉, 传统硬拉, 杠铃硬拉", createdAt),
        preset("t_bar_row", "T杠划船", BodyPart.BACK, EquipmentType.MACHINE, WeightMode.WEIGHTED, "T杠划船, T杆划船", createdAt),
        preset("inverted_row", "反向划船", BodyPart.BACK, EquipmentType.FREE_WEIGHT, WeightMode.BODYWEIGHT, "反向划船, 澳式引体", createdAt),

        preset("barbell_squat", "杠铃深蹲", BodyPart.LEGS, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "杠铃深蹲, 背蹲, 深蹲", createdAt),
        preset("leg_press", "腿举", BodyPart.LEGS, EquipmentType.MACHINE, WeightMode.WEIGHTED, "腿举, 倒蹬", createdAt),
        preset("lunge", "箭步蹲", BodyPart.LEGS, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "箭步蹲, 弓步蹲, 行走箭步蹲", createdAt),
        preset("romanian_deadlift", "罗马尼亚硬拉", BodyPart.LEGS, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "罗马硬拉, 罗马尼亚硬拉", createdAt),
        preset("leg_extension", "腿屈伸", BodyPart.LEGS, EquipmentType.MACHINE, WeightMode.WEIGHTED, "坐姿腿屈伸, 腿屈伸", createdAt),
        preset("leg_curl", "腿弯举", BodyPart.LEGS, EquipmentType.MACHINE, WeightMode.WEIGHTED, "俯卧腿弯举, 腿弯举", createdAt),
        preset("calf_raise", "提踵", BodyPart.LEGS, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "站姿提踵, 提踵", createdAt),
        preset("hip_thrust", "臀桥", BodyPart.LEGS, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "臀桥, 臀推", createdAt),
        preset("bulgarian_split_squat", "保加利亚分腿蹲", BodyPart.LEGS, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "保加利亚蹲, 分腿蹲", createdAt),
        preset("hip_abduction", "髋外展", BodyPart.LEGS, EquipmentType.MACHINE, WeightMode.WEIGHTED, "髋外展, 坐姿髋外展", createdAt),

        preset("barbell_curl", "杠铃弯举", BodyPart.ARMS, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "杠铃弯举, 二头弯举", createdAt),
        preset("dumbbell_curl", "哑铃弯举", BodyPart.ARMS, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "哑铃弯举", createdAt),
        preset("hammer_curl", "锤式弯举", BodyPart.ARMS, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "锤式弯举", createdAt),
        preset("tricep_pushdown", "绳索下压", BodyPart.ARMS, EquipmentType.MACHINE, WeightMode.WEIGHTED, "绳索下压, 三头下压", createdAt),
        preset("skull_crusher", "颈后臂屈伸", BodyPart.ARMS, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "仰卧臂屈伸, 碎颅式", createdAt),
        preset("dip", "双杠臂屈伸", BodyPart.ARMS, EquipmentType.FREE_WEIGHT, WeightMode.BODYWEIGHT, "双杠臂屈伸, 双杠撑", createdAt),
        preset("close_grip_bench", "窄距卧推", BodyPart.ARMS, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "窄距卧推, 窄握卧推", createdAt),

        preset("crunch", "卷腹", BodyPart.ABS, EquipmentType.FREE_WEIGHT, WeightMode.BODYWEIGHT, "卷腹", createdAt),
        preset("sit_up", "仰卧起坐", BodyPart.ABS, EquipmentType.FREE_WEIGHT, WeightMode.BODYWEIGHT, "仰卧起坐", createdAt),
        preset("plank", "平板支撑", BodyPart.ABS, EquipmentType.FREE_WEIGHT, WeightMode.BODYWEIGHT, "平板支撑", createdAt),
        preset("hanging_leg_raise", "悬挂举腿", BodyPart.ABS, EquipmentType.FREE_WEIGHT, WeightMode.BODYWEIGHT, "悬挂举腿, 悬垂举腿", createdAt),
        preset("russian_twist", "俄罗斯转体", BodyPart.ABS, EquipmentType.FREE_WEIGHT, WeightMode.BODYWEIGHT, "俄罗斯转体", createdAt),
        preset("mountain_climber", "登山跑", BodyPart.ABS, EquipmentType.FREE_WEIGHT, WeightMode.BODYWEIGHT, "登山跑, 登山者", createdAt),
        preset("cable_crunch", "绳索卷腹", BodyPart.ABS, EquipmentType.MACHINE, WeightMode.WEIGHTED, "绳索卷腹, 跪姿卷腹", createdAt),

        preset("weighted_push_up", "负重俯卧撑", BodyPart.CHEST, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "负重俯卧撑", createdAt),
        preset("incline_dumbbell_bench", "上斜哑铃卧推", BodyPart.CHEST, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "上斜哑铃卧推, 上斜哑铃推胸", createdAt),
        preset("decline_dumbbell_bench", "下斜哑铃卧推", BodyPart.CHEST, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "下斜哑铃卧推", createdAt),
        preset("incline_push_up", "上斜俯卧撑", BodyPart.CHEST, EquipmentType.FREE_WEIGHT, WeightMode.BODYWEIGHT, "上斜俯卧撑, 高位俯卧撑", createdAt),
        preset("decline_push_up", "下斜俯卧撑", BodyPart.CHEST, EquipmentType.FREE_WEIGHT, WeightMode.BODYWEIGHT, "下斜俯卧撑, 脚抬高俯卧撑", createdAt),

        preset("seated_barbell_press", "坐姿杠铃推举", BodyPart.SHOULDERS, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "坐姿杠铃推举, 坐姿肩推", createdAt),
        preset("cable_lateral_raise", "绳索侧平举", BodyPart.SHOULDERS, EquipmentType.MACHINE, WeightMode.WEIGHTED, "绳索侧平举", createdAt),

        preset("assisted_pull_up", "辅助引体向上", BodyPart.BACK, EquipmentType.MACHINE, WeightMode.BODYWEIGHT_PLUS, "辅助引体向上, 弹力带引体", createdAt),
        preset("straight_arm_pulldown", "直臂下压", BodyPart.BACK, EquipmentType.MACHINE, WeightMode.WEIGHTED, "直臂下压, 直臂下拉", createdAt),
        preset("chest_supported_row", "胸托划船", BodyPart.BACK, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "胸托划船, 上斜凳划船", createdAt),
        preset("landmine_row", "地雷管划船", BodyPart.BACK, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "地雷管划船, 杠铃炮筒划船", createdAt),
        preset("sumo_deadlift", "相扑硬拉", BodyPart.BACK, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "相扑硬拉, 宽站距硬拉", createdAt),
        preset("stiff_leg_deadlift", "直腿硬拉", BodyPart.BACK, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "直腿硬拉", createdAt),
        preset("barbell_shrug", "杠铃耸肩", BodyPart.BACK, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "杠铃耸肩, 耸肩", createdAt),

        preset("front_squat", "前蹲", BodyPart.LEGS, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "前蹲, 颈前深蹲", createdAt),
        preset("goblet_squat", "高脚杯深蹲", BodyPart.LEGS, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "高脚杯深蹲, 哑铃深蹲", createdAt),
        preset("hack_squat", "哈克深蹲", BodyPart.LEGS, EquipmentType.MACHINE, WeightMode.WEIGHTED, "哈克深蹲, 哈克机", createdAt),
        preset("smith_squat", "史密斯深蹲", BodyPart.LEGS, EquipmentType.MACHINE, WeightMode.WEIGHTED, "史密斯深蹲", createdAt),
        preset("step_up", "台阶上步", BodyPart.LEGS, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "台阶上步, 登阶", createdAt),
        preset("barbell_hip_thrust", "杠铃臀推", BodyPart.LEGS, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "杠铃臀推, 杠铃臀桥", createdAt),
        preset("hip_adduction", "髋内收", BodyPart.LEGS, EquipmentType.MACHINE, WeightMode.WEIGHTED, "髋内收, 坐姿髋内收", createdAt),
        preset("seated_calf_raise", "坐姿提踵", BodyPart.LEGS, EquipmentType.MACHINE, WeightMode.WEIGHTED, "坐姿提踵", createdAt),
        preset("tibialis_raise", "胫骨前肌训练", BodyPart.LEGS, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "胫骨前肌训练, 勾脚尖", createdAt),

        preset("preacher_curl", "牧师凳弯举", BodyPart.ARMS, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "牧师凳弯举, 斜托弯举", createdAt),
        preset("incline_dumbbell_curl", "斜板弯举", BodyPart.ARMS, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "斜板弯举, 上斜哑铃弯举", createdAt),
        preset("cable_curl", "绳索弯举", BodyPart.ARMS, EquipmentType.MACHINE, WeightMode.WEIGHTED, "绳索弯举, 龙门架弯举", createdAt),
        preset("reverse_curl", "反向弯举", BodyPart.ARMS, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "反向弯举, 反握弯举", createdAt),
        preset("overhead_tricep_extension", "过顶臂屈伸", BodyPart.ARMS, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "过顶臂屈伸, 颈后臂屈伸", createdAt),
        preset("wrist_curl", "腕弯举", BodyPart.ARMS, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "腕弯举, 正握腕弯举", createdAt),
        preset("reverse_wrist_curl", "反向腕弯举", BodyPart.ARMS, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "反向腕弯举, 反握腕弯举", createdAt),

        preset("reverse_crunch", "反向卷腹", BodyPart.ABS, EquipmentType.FREE_WEIGHT, WeightMode.BODYWEIGHT, "反向卷腹", createdAt),
        preset("side_plank", "侧平板支撑", BodyPart.ABS, EquipmentType.FREE_WEIGHT, WeightMode.BODYWEIGHT, "侧平板支撑, 侧桥", createdAt),
        preset("dead_bug", "死虫", BodyPart.ABS, EquipmentType.FREE_WEIGHT, WeightMode.BODYWEIGHT, "死虫式, 死虫", createdAt),
        preset("bird_dog", "鸟狗", BodyPart.ABS, EquipmentType.FREE_WEIGHT, WeightMode.BODYWEIGHT, "鸟狗式, 鸟狗", createdAt),
        preset("pallof_press", "帕洛夫推", BodyPart.ABS, EquipmentType.MACHINE, WeightMode.WEIGHTED, "帕洛夫推, 抗旋转推", createdAt),

        preset("band_pull_apart", "弹力带拉开", BodyPart.SHOULDERS, EquipmentType.OTHER, WeightMode.WEIGHTED, "弹力带拉开, 弹力带外旋", createdAt),

        preset("farmer_carry", "农夫行走", BodyPart.OTHER, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "农夫行走, 农夫提", createdAt),
        preset("burpee", "波比跳", BodyPart.OTHER, EquipmentType.OTHER, WeightMode.BODYWEIGHT, "波比跳, 立卧撑跳", createdAt),
        preset("jumping_jack", "开合跳", BodyPart.OTHER, EquipmentType.OTHER, WeightMode.BODYWEIGHT, "开合跳", createdAt),
        preset("kettlebell_swing", "壶铃摆动", BodyPart.OTHER, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "壶铃摆动, 壶铃甩", createdAt),
    )

    private fun preset(
        id: String,
        name: String,
        bodyPart: BodyPart,
        equipmentType: EquipmentType,
        weightMode: WeightMode,
        aliases: String,
        createdAt: Long,
    ): ExerciseEntity {
        val bodyCategory = ExerciseClassification.bodyForLegacy(bodyPart.name)
        val equipmentCategory = ExerciseClassification.equipmentForLegacy(equipmentType.name)
        return ExerciseEntity(
            id = "preset_$id",
            name = name,
            bodyPart = bodyPart.name,
            equipmentType = equipmentType.name,
            weightMode = weightMode.name,
            isCustom = false,
            aliases = aliases,
            createdAt = createdAt,
            bodyCategoryId = bodyCategory.id,
            equipmentCategoryId = equipmentCategory.id,
            classificationStatus = ExerciseClassification.STATUS_AMBIGUOUS,
            classificationVersion = ExerciseClassification.VERSION,
        )
    }
}
