package com.example.gymlog.data

/**
 * Legacy test/history fixture only. Production never seeds these rows.
 * Old rows remain readable for history when they already exist in a database.
 */
@Deprecated("Legacy preset fixture; never seed in production")
object PresetExercises {
    fun all(createdAt: Long): List<ExerciseEntity> = listOf(
        preset("barbell_bench_press", "杠铃卧推", BodyPart.CHEST, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "bench press, barbell bench, bench", createdAt),
        preset("dumbbell_bench_press", "哑铃卧推", BodyPart.CHEST, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "dumbbell bench, db bench", createdAt),
        preset("incline_bench_press", "上斜杠铃卧推", BodyPart.CHEST, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "incline bench, incline press", createdAt),
        preset("decline_bench_press", "下斜卧推", BodyPart.CHEST, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "decline bench, decline press", createdAt),
        preset("dumbbell_fly", "哑铃飞鸟", BodyPart.CHEST, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "fly, pec fly, dumbbell fly", createdAt),
        preset("cable_crossover", "绳索夹胸", BodyPart.CHEST, EquipmentType.MACHINE, WeightMode.WEIGHTED, "cable fly, crossover, cable crossover", createdAt),
        preset("chest_press_machine", "器械推胸", BodyPart.CHEST, EquipmentType.MACHINE, WeightMode.WEIGHTED, "chest press, machine chest press", createdAt),
        preset("pec_deck", "蝴蝶机夹胸", BodyPart.CHEST, EquipmentType.MACHINE, WeightMode.WEIGHTED, "pec deck, pec fly machine", createdAt),
        preset("push_up", "俯卧撑", BodyPart.CHEST, EquipmentType.FREE_WEIGHT, WeightMode.BODYWEIGHT, "push up, pushup, press up", createdAt),

        preset("overhead_press", "杠铃推举", BodyPart.SHOULDERS, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "ohp, overhead press, military press, shoulder press", createdAt),
        preset("dumbbell_shoulder_press", "哑铃推举", BodyPart.SHOULDERS, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "dumbbell press, db press, shoulder press", createdAt),
        preset("lateral_raise", "侧平举", BodyPart.SHOULDERS, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "lateral raise, side raise", createdAt),
        preset("front_raise", "前平举", BodyPart.SHOULDERS, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "front raise", createdAt),
        preset("rear_delt_fly", "反向飞鸟", BodyPart.SHOULDERS, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "reverse fly, rear delt, rear delt fly", createdAt),
        preset("face_pull", "面拉", BodyPart.SHOULDERS, EquipmentType.MACHINE, WeightMode.WEIGHTED, "face pull, facepull", createdAt),
        preset("arnold_press", "阿诺德推举", BodyPart.SHOULDERS, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "arnold press", createdAt),
        preset("shoulder_press_machine", "器械推肩", BodyPart.SHOULDERS, EquipmentType.MACHINE, WeightMode.WEIGHTED, "machine shoulder press", createdAt),

        preset("pull_up", "引体向上", BodyPart.BACK, EquipmentType.FREE_WEIGHT, WeightMode.BODYWEIGHT, "pull up, pullup, chin up", createdAt),
        preset("lat_pulldown", "高位下拉", BodyPart.BACK, EquipmentType.MACHINE, WeightMode.WEIGHTED, "lat pulldown, pulldown", createdAt),
        preset("barbell_row", "杠铃划船", BodyPart.BACK, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "barbell row, bent over row", createdAt),
        preset("dumbbell_row", "哑铃划船", BodyPart.BACK, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "dumbbell row, db row, one arm row", createdAt),
        preset("seated_row", "坐姿划船", BodyPart.BACK, EquipmentType.MACHINE, WeightMode.WEIGHTED, "seated row, cable row", createdAt),
        preset("deadlift", "硬拉", BodyPart.BACK, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "deadlift, conventional deadlift, dl", createdAt),
        preset("t_bar_row", "T杠划船", BodyPart.BACK, EquipmentType.MACHINE, WeightMode.WEIGHTED, "t bar row, t-bar row", createdAt),
        preset("inverted_row", "反向划船", BodyPart.BACK, EquipmentType.FREE_WEIGHT, WeightMode.BODYWEIGHT, "inverted row, australian pull up", createdAt),

        preset("barbell_squat", "杠铃深蹲", BodyPart.LEGS, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "squat, barbell squat, back squat", createdAt),
        preset("leg_press", "腿举", BodyPart.LEGS, EquipmentType.MACHINE, WeightMode.WEIGHTED, "leg press", createdAt),
        preset("lunge", "箭步蹲", BodyPart.LEGS, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "lunge, walking lunge", createdAt),
        preset("romanian_deadlift", "罗马尼亚硬拉", BodyPart.LEGS, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "rdl, romanian deadlift", createdAt),
        preset("leg_extension", "腿屈伸", BodyPart.LEGS, EquipmentType.MACHINE, WeightMode.WEIGHTED, "leg extension", createdAt),
        preset("leg_curl", "腿弯举", BodyPart.LEGS, EquipmentType.MACHINE, WeightMode.WEIGHTED, "leg curl, hamstring curl", createdAt),
        preset("calf_raise", "提踵", BodyPart.LEGS, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "calf raise, standing calf raise", createdAt),
        preset("hip_thrust", "臀桥", BodyPart.LEGS, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "hip thrust, glute bridge, glute bridge hip thrust", createdAt),
        preset("bulgarian_split_squat", "保加利亚分腿蹲", BodyPart.LEGS, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "bulgarian split squat, split squat", createdAt),
        preset("hip_abduction", "髋外展", BodyPart.LEGS, EquipmentType.MACHINE, WeightMode.WEIGHTED, "hip abduction, abductor", createdAt),

        preset("barbell_curl", "杠铃弯举", BodyPart.ARMS, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "barbell curl, bicep curl", createdAt),
        preset("dumbbell_curl", "哑铃弯举", BodyPart.ARMS, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "dumbbell curl, db curl", createdAt),
        preset("hammer_curl", "锤式弯举", BodyPart.ARMS, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "hammer curl", createdAt),
        preset("tricep_pushdown", "绳索下压", BodyPart.ARMS, EquipmentType.MACHINE, WeightMode.WEIGHTED, "tricep pushdown, pushdown, cable pushdown", createdAt),
        preset("skull_crusher", "颈后臂屈伸", BodyPart.ARMS, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "skull crusher, overhead extension, french press", createdAt),
        preset("dip", "双杠臂屈伸", BodyPart.ARMS, EquipmentType.FREE_WEIGHT, WeightMode.BODYWEIGHT, "dip, dips, tricep dip", createdAt),
        preset("close_grip_bench", "窄距卧推", BodyPart.ARMS, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "close grip bench, close-grip bench press", createdAt),

        preset("crunch", "卷腹", BodyPart.ABS, EquipmentType.FREE_WEIGHT, WeightMode.BODYWEIGHT, "crunch, crunches", createdAt),
        preset("sit_up", "仰卧起坐", BodyPart.ABS, EquipmentType.FREE_WEIGHT, WeightMode.BODYWEIGHT, "sit up, situp", createdAt),
        preset("plank", "平板支撑", BodyPart.ABS, EquipmentType.FREE_WEIGHT, WeightMode.BODYWEIGHT, "plank", createdAt),
        preset("hanging_leg_raise", "悬挂举腿", BodyPart.ABS, EquipmentType.FREE_WEIGHT, WeightMode.BODYWEIGHT, "hanging leg raise, hanging knee raise, hlr", createdAt),
        preset("russian_twist", "俄罗斯转体", BodyPart.ABS, EquipmentType.FREE_WEIGHT, WeightMode.BODYWEIGHT, "russian twist", createdAt),
        preset("mountain_climber", "登山跑", BodyPart.ABS, EquipmentType.FREE_WEIGHT, WeightMode.BODYWEIGHT, "mountain climber", createdAt),
        preset("cable_crunch", "绳索卷腹", BodyPart.ABS, EquipmentType.MACHINE, WeightMode.WEIGHTED, "cable crunch", createdAt),

        preset("weighted_push_up", "负重俯卧撑", BodyPart.CHEST, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "weighted push up, push up plus weight", createdAt),
        preset("incline_dumbbell_bench", "上斜哑铃卧推", BodyPart.CHEST, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "incline dumbbell press, incline db bench", createdAt),
        preset("decline_dumbbell_bench", "下斜哑铃卧推", BodyPart.CHEST, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "decline dumbbell press, decline db bench", createdAt),
        preset("incline_push_up", "上斜俯卧撑", BodyPart.CHEST, EquipmentType.FREE_WEIGHT, WeightMode.BODYWEIGHT, "incline push up, hands elevated push up", createdAt),
        preset("decline_push_up", "下斜俯卧撑", BodyPart.CHEST, EquipmentType.FREE_WEIGHT, WeightMode.BODYWEIGHT, "decline push up, feet elevated push up", createdAt),

        preset("seated_barbell_press", "坐姿杠铃推举", BodyPart.SHOULDERS, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "seated overhead press, seated barbell press", createdAt),
        preset("cable_lateral_raise", "绳索侧平举", BodyPart.SHOULDERS, EquipmentType.MACHINE, WeightMode.WEIGHTED, "cable lateral raise", createdAt),

        preset("assisted_pull_up", "辅助引体向上", BodyPart.BACK, EquipmentType.MACHINE, WeightMode.BODYWEIGHT_PLUS, "assisted pull up, band pull up", createdAt),
        preset("straight_arm_pulldown", "直臂下压", BodyPart.BACK, EquipmentType.MACHINE, WeightMode.WEIGHTED, "straight arm pulldown, straight-arm pulldown", createdAt),
        preset("chest_supported_row", "胸托划船", BodyPart.BACK, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "chest supported row, chest-supported row", createdAt),
        preset("landmine_row", "地雷管划船", BodyPart.BACK, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "landmine row, meadow row", createdAt),
        preset("sumo_deadlift", "相扑硬拉", BodyPart.BACK, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "sumo deadlift, sumo dl", createdAt),
        preset("stiff_leg_deadlift", "直腿硬拉", BodyPart.BACK, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "stiff leg deadlift, straight leg deadlift, sldl", createdAt),
        preset("barbell_shrug", "杠铃耸肩", BodyPart.BACK, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "shrug, barbell shrug", createdAt),

        preset("front_squat", "前蹲", BodyPart.LEGS, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "front squat", createdAt),
        preset("goblet_squat", "高脚杯深蹲", BodyPart.LEGS, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "goblet squat", createdAt),
        preset("hack_squat", "哈克深蹲", BodyPart.LEGS, EquipmentType.MACHINE, WeightMode.WEIGHTED, "hack squat", createdAt),
        preset("smith_squat", "史密斯深蹲", BodyPart.LEGS, EquipmentType.MACHINE, WeightMode.WEIGHTED, "smith squat, smith machine squat", createdAt),
        preset("step_up", "台阶上步", BodyPart.LEGS, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "step up, step-up", createdAt),
        preset("barbell_hip_thrust", "杠铃臀推", BodyPart.LEGS, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "barbell hip thrust", createdAt),
        preset("hip_adduction", "髋内收", BodyPart.LEGS, EquipmentType.MACHINE, WeightMode.WEIGHTED, "hip adduction, adductor machine", createdAt),
        preset("seated_calf_raise", "坐姿提踵", BodyPart.LEGS, EquipmentType.MACHINE, WeightMode.WEIGHTED, "seated calf raise", createdAt),
        preset("tibialis_raise", "胫骨前肌训练", BodyPart.LEGS, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "tibialis raise, toe raise", createdAt),

        preset("preacher_curl", "牧师凳弯举", BodyPart.ARMS, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "preacher curl", createdAt),
        preset("incline_dumbbell_curl", "斜板弯举", BodyPart.ARMS, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "incline curl, incline dumbbell curl", createdAt),
        preset("cable_curl", "绳索弯举", BodyPart.ARMS, EquipmentType.MACHINE, WeightMode.WEIGHTED, "cable curl", createdAt),
        preset("reverse_curl", "反向弯举", BodyPart.ARMS, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "reverse curl", createdAt),
        preset("overhead_tricep_extension", "过顶臂屈伸", BodyPart.ARMS, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "overhead tricep extension, french press", createdAt),
        preset("wrist_curl", "腕弯举", BodyPart.ARMS, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "wrist curl", createdAt),
        preset("reverse_wrist_curl", "反向腕弯举", BodyPart.ARMS, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "reverse wrist curl", createdAt),

        preset("reverse_crunch", "反向卷腹", BodyPart.ABS, EquipmentType.FREE_WEIGHT, WeightMode.BODYWEIGHT, "reverse crunch", createdAt),
        preset("side_plank", "侧平板支撑", BodyPart.ABS, EquipmentType.FREE_WEIGHT, WeightMode.BODYWEIGHT, "side plank", createdAt),
        preset("dead_bug", "死虫", BodyPart.ABS, EquipmentType.FREE_WEIGHT, WeightMode.BODYWEIGHT, "dead bug", createdAt),
        preset("bird_dog", "鸟狗", BodyPart.ABS, EquipmentType.FREE_WEIGHT, WeightMode.BODYWEIGHT, "bird dog", createdAt),
        preset("pallof_press", "帕洛夫推", BodyPart.ABS, EquipmentType.MACHINE, WeightMode.WEIGHTED, "pallof press, anti-rotation press", createdAt),

        preset("band_pull_apart", "弹力带拉开", BodyPart.SHOULDERS, EquipmentType.OTHER, WeightMode.WEIGHTED, "band pull apart, band pull-apart", createdAt),

        preset("farmer_carry", "农夫行走", BodyPart.OTHER, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "farmer walk, farmer carry, farmers walk", createdAt),
        preset("burpee", "波比跳", BodyPart.OTHER, EquipmentType.OTHER, WeightMode.BODYWEIGHT, "burpee, burpees", createdAt),
        preset("jumping_jack", "开合跳", BodyPart.OTHER, EquipmentType.OTHER, WeightMode.BODYWEIGHT, "jumping jack, jumping jacks", createdAt),
        preset("kettlebell_swing", "壶铃摆动", BodyPart.OTHER, EquipmentType.FREE_WEIGHT, WeightMode.WEIGHTED, "kettlebell swing, kb swing", createdAt),
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
