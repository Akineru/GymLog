package com.example.gymlog.data

/**
 * Stable category IDs for the dataset's own body-part and equipment values.
 *
 * The database stores both the stable id (used by queries and filters) and the
 * original detail value (shown in [ExerciseEntity.bodyPartDetail] /
 * [ExerciseEntity.equipmentDetail]). Display text must never be used as a key.
 */
object ExerciseClassification {
    const val VERSION = 1

    const val STATUS_UNMIGRATED = "UNMIGRATED"
    const val STATUS_DATASET = "DATASET"
    const val STATUS_MAPPED = "MAPPED"
    const val STATUS_AMBIGUOUS = "AMBIGUOUS"
    const val STATUS_UNMAPPED = "UNMAPPED"

    data class BodyCategory(
        val id: String,
        val rawValue: String?,
        val labelZh: String,
        val sortOrder: Int,
        val standard: Boolean = true,
    )

    data class EquipmentCategory(
        val id: String,
        val rawValue: String?,
        val labelZh: String,
        val sortOrder: Int,
        val standard: Boolean = true,
    )

    val ALL_BODY = BodyCategory("", null, "全部", -1, standard = false)
    val UNKNOWN_BODY = BodyCategory("body_unknown", null, "未分类", 10_000, standard = false)
    val LEGACY_ARMS = BodyCategory("body_legacy_arms", null, "手臂（旧分类）", 9_001, standard = false)
    val LEGACY_LEGS = BodyCategory("body_legacy_legs", null, "腿（旧分类）", 9_002, standard = false)
    val LEGACY_BODY_OTHER =
        BodyCategory("body_legacy_other", null, "其他（旧分类）", 9_003, standard = false)

    val CHEST = BodyCategory("body_chest", "chest", "胸部", 0)
    val BACK = BodyCategory("body_back", "back", "背部", 1)
    val SHOULDERS = BodyCategory("body_shoulders", "shoulders", "肩部", 2)
    val UPPER_ARMS = BodyCategory("body_upper_arms", "upper arms", "上臂", 3)
    val LOWER_ARMS = BodyCategory("body_lower_arms", "lower arms", "前臂", 4)
    val UPPER_LEGS = BodyCategory("body_upper_legs", "upper legs", "大腿", 5)
    val LOWER_LEGS = BodyCategory("body_lower_legs", "lower legs", "小腿", 6)
    val WAIST = BodyCategory("body_waist", "waist", "腰部", 7)
    val CARDIO = BodyCategory("body_cardio", "cardio", "有氧", 8)
    val NECK = BodyCategory("body_neck", "neck", "颈部", 9)

    val bodyStandard: List<BodyCategory> = listOf(
        CHEST, BACK, SHOULDERS, UPPER_ARMS, LOWER_ARMS,
        UPPER_LEGS, LOWER_LEGS, WAIST, CARDIO, NECK,
    )
    val bodyCompatibility: List<BodyCategory> = listOf(
        LEGACY_ARMS, LEGACY_LEGS, LEGACY_BODY_OTHER, UNKNOWN_BODY,
    )
    // Only the 10 dataset categories are formal filter options. Compatibility
    // buckets remain resolvable for history but are not shown in the active
    // catalog rail.
    val bodyUi: List<BodyCategory> = listOf(ALL_BODY) + bodyStandard
    val bodyById: Map<String, BodyCategory> =
        (bodyUi + bodyCompatibility).associateBy { it.id }
    val bodyByRaw: Map<String, BodyCategory> = bodyStandard.associateBy { it.rawValue!! }

    val ALL_EQUIPMENT = EquipmentCategory("", null, "全部", -1, standard = false)
    val UNKNOWN_EQUIPMENT = EquipmentCategory("equipment_unknown", null, "未分类", 10_000, standard = false)
    val LEGACY_FREE_WEIGHT =
        EquipmentCategory("equipment_legacy_free_weight", null, "自由器械（旧分类）", 9_001, standard = false)
    val LEGACY_MACHINE =
        EquipmentCategory("equipment_legacy_machine", null, "固定器械（旧分类）", 9_002, standard = false)
    val LEGACY_EQUIPMENT_OTHER =
        EquipmentCategory("equipment_legacy_other", null, "其他（旧分类）", 9_003, standard = false)

    private data class EqSeed(val id: String, val raw: String, val label: String, val order: Int)
    private val equipmentSeeds = listOf(
        EqSeed("equipment_body_weight", "body weight", "自重", 0),
        EqSeed("equipment_barbell", "barbell", "杠铃", 1),
        EqSeed("equipment_dumbbell", "dumbbell", "哑铃", 2),
        EqSeed("equipment_cable", "cable", "绳索", 3),
        EqSeed("equipment_leverage_machine", "leverage machine", "杠杆器械", 4),
        EqSeed("equipment_smith_machine", "smith machine", "史密斯机", 5),
        EqSeed("equipment_kettlebell", "kettlebell", "壶铃", 6),
        EqSeed("equipment_band", "band", "弹力带", 7),
        EqSeed("equipment_weighted", "weighted", "负重", 8),
        EqSeed("equipment_stability_ball", "stability ball", "健身球", 9),
        EqSeed("equipment_ez_barbell", "ez barbell", "EZ 杠铃", 10),
        EqSeed("equipment_assisted", "assisted", "辅助", 11),
        EqSeed("equipment_sled_machine", "sled machine", "雪橇机", 12),
        EqSeed("equipment_medicine_ball", "medicine ball", "药球", 13),
        EqSeed("equipment_rope", "rope", "绳索", 14),
        EqSeed("equipment_roller", "roller", "滚轮", 15),
        EqSeed("equipment_resistance_band", "resistance band", "阻力带", 16),
        EqSeed("equipment_bosu_ball", "bosu ball", "波速球", 17),
        EqSeed("equipment_olympic_barbell", "olympic barbell", "奥运杠铃", 18),
        EqSeed("equipment_wheel_roller", "wheel roller", "健腹轮", 19),
        EqSeed("equipment_upper_body_ergometer", "upper body ergometer", "上肢功率车", 20),
        EqSeed("equipment_skierg_machine", "skierg machine", "滑雪机", 21),
        EqSeed("equipment_hammer", "hammer", "锤", 22),
        EqSeed("equipment_stationary_bike", "stationary bike", "固定自行车", 23),
        EqSeed("equipment_tire", "tire", "轮胎", 24),
        EqSeed("equipment_trap_bar", "trap bar", "六角杠铃", 25),
        EqSeed("equipment_elliptical_machine", "elliptical machine", "椭圆机", 26),
        EqSeed("equipment_stepmill_machine", "stepmill machine", "登山机", 27),
    )

    val equipmentStandard: List<EquipmentCategory> = equipmentSeeds.map {
        EquipmentCategory(it.id, it.raw, it.label, it.order)
    }
    val BODY_WEIGHT = equipmentStandard.first { it.id == "equipment_body_weight" }
    val BARBELL = equipmentStandard.first { it.id == "equipment_barbell" }
    val DUMBBELL = equipmentStandard.first { it.id == "equipment_dumbbell" }
    val CABLE = equipmentStandard.first { it.id == "equipment_cable" }
    val LEVERAGE_MACHINE = equipmentStandard.first { it.id == "equipment_leverage_machine" }
    val SMITH_MACHINE = equipmentStandard.first { it.id == "equipment_smith_machine" }

    val equipmentCompatibility: List<EquipmentCategory> = listOf(
        LEGACY_FREE_WEIGHT, LEGACY_MACHINE, LEGACY_EQUIPMENT_OTHER, UNKNOWN_EQUIPMENT,
    )
    // Only the 28 dataset categories are formal filter options.
    val equipmentUi: List<EquipmentCategory> =
        listOf(ALL_EQUIPMENT) + equipmentStandard
    val equipmentById: Map<String, EquipmentCategory> =
        (equipmentUi + equipmentCompatibility).associateBy { it.id }
    val equipmentByRaw: Map<String, EquipmentCategory> =
        equipmentStandard.associateBy { it.rawValue!! }

    fun body(id: String?): BodyCategory? = id?.let { bodyById[it] }
    fun equipment(id: String?): EquipmentCategory? = id?.let { equipmentById[it] }

    fun bodyForRaw(raw: String?): BodyCategory? =
        raw?.trim()?.lowercase()?.let { bodyByRaw[it] }

    fun equipmentForRaw(raw: String?): EquipmentCategory? =
        raw?.trim()?.lowercase()?.let { equipmentByRaw[it] }

    /** Legacy broad body-part mapping. Ambiguous broad values get compatibility buckets. */
    fun bodyForLegacy(oldBodyPart: String): BodyCategory {
        return when (oldBodyPart) {
            BodyPart.CHEST.name -> CHEST
            BodyPart.BACK.name -> BACK
            BodyPart.SHOULDERS.name -> SHOULDERS
            BodyPart.ABS.name -> WAIST
            BodyPart.ARMS.name -> LEGACY_ARMS
            BodyPart.LEGS.name -> LEGACY_LEGS
            else -> LEGACY_BODY_OTHER
        }
    }

    /**
     * Legacy equipment was only FREE_WEIGHT/MACHINE/OTHER. It cannot be safely
     * narrowed to one of the 28 dataset values without user input.
     */
    fun equipmentForLegacy(oldEquipmentType: String): EquipmentCategory {
        return when (oldEquipmentType) {
            EquipmentType.FREE_WEIGHT.name -> LEGACY_FREE_WEIGHT
            EquipmentType.MACHINE.name -> LEGACY_MACHINE
            else -> LEGACY_EQUIPMENT_OTHER
        }
    }

    fun bodyLabel(id: String?, fallbackRaw: String? = null): String {
        return body(id)?.labelZh ?: fallbackRaw?.takeIf { it.isNotBlank() } ?: "未分类"
    }

    fun equipmentLabel(id: String?, fallbackRaw: String? = null): String {
        return equipment(id)?.labelZh ?: fallbackRaw?.takeIf { it.isNotBlank() } ?: "未分类"
    }

    fun legacyBodyPartFor(category: BodyCategory): BodyPart {
        return when (category.id) {
            CHEST.id -> BodyPart.CHEST
            BACK.id -> BodyPart.BACK
            SHOULDERS.id -> BodyPart.SHOULDERS
            WAIST.id -> BodyPart.ABS
            UPPER_ARMS.id, LOWER_ARMS.id, LEGACY_ARMS.id -> BodyPart.ARMS
            UPPER_LEGS.id, LOWER_LEGS.id, LEGACY_LEGS.id -> BodyPart.LEGS
            else -> BodyPart.OTHER
        }
    }

    fun legacyEquipmentFor(category: EquipmentCategory): EquipmentType {
        return when (category.id) {
            LEGACY_FREE_WEIGHT.id -> EquipmentType.FREE_WEIGHT
            LEGACY_MACHINE.id -> EquipmentType.MACHINE
            LEGACY_EQUIPMENT_OTHER.id, UNKNOWN_EQUIPMENT.id -> EquipmentType.OTHER
            "equipment_body_weight", "equipment_barbell", "equipment_dumbbell",
            "equipment_kettlebell", "equipment_band", "equipment_weighted",
            "equipment_stability_ball", "equipment_ez_barbell", "equipment_medicine_ball",
            "equipment_resistance_band", "equipment_bosu_ball", "equipment_roller",
            "equipment_olympic_barbell", "equipment_wheel_roller", "equipment_hammer",
            "equipment_trap_bar", "equipment_tire",
            -> EquipmentType.FREE_WEIGHT
            "equipment_cable", "equipment_leverage_machine", "equipment_smith_machine",
            "equipment_assisted", "equipment_sled_machine", "equipment_rope",
            "equipment_upper_body_ergometer", "equipment_skierg_machine",
            "equipment_stationary_bike", "equipment_elliptical_machine",
            "equipment_stepmill_machine",
            -> EquipmentType.MACHINE
            else -> EquipmentType.OTHER
        }
    }

    fun statusFor(body: BodyCategory?, equipment: EquipmentCategory?): String {
        if (body == null || equipment == null) return STATUS_UNMAPPED
        return if (body.standard && equipment.standard) STATUS_MAPPED else STATUS_AMBIGUOUS
    }

    fun bodyCategoryForExercise(exercise: ExerciseEntity): BodyCategory {
        body(exercise.bodyCategoryId)?.let { return it }
        bodyForRaw(exercise.bodyPartDetail)?.let { return it }
        return bodyForLegacy(exercise.bodyPart)
    }

    fun equipmentCategoryForExercise(exercise: ExerciseEntity): EquipmentCategory {
        equipment(exercise.equipmentCategoryId)?.let { return it }
        equipmentForRaw(exercise.equipmentDetail)?.let { return it }
        return equipmentForLegacy(exercise.equipmentType)
    }

    fun isCompatibilityBody(id: String?): Boolean =
        body(id)?.standard == false && id?.isNotBlank() == true

    fun isCompatibilityEquipment(id: String?): Boolean =
        equipment(id)?.standard == false && id?.isNotBlank() == true

    /** Part key used by session/home/calendar ordering while new data rolls out. */
    fun exerciseGroupingKey(exercise: ExerciseEntity): String =
        exercise.bodyCategoryId?.takeIf { it.isNotBlank() } ?: exercise.bodyPart

    fun partLabel(part: String): String =
        body(part)?.labelZh
            ?: BodyPart.entries.find { it.name == part }?.labelZh
            ?: part

    fun partSortOrder(part: String): Int =
        body(part)?.sortOrder ?: BodyPart.rank(part)
}
