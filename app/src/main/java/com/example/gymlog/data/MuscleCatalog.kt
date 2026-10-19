package com.example.gymlog.data

enum class MuscleRole {
    PRIMARY,
    SECONDARY,
}

enum class FigureSide {
    FRONT,
    BACK,
}

enum class MuscleRegion {
    CHEST,
    SHOULDERS,
    BACK,
    ARMS,
    CORE,
    GLUTES,
    LEGS,
    CALVES,
}

data class Muscle(
    val id: String,
    val nameZh: String,
    val nameEn: String,
    val region: MuscleRegion,
    val side: FigureSide?,
    val paintRegionIds: List<String>,
    val sortOrder: Int,
    val note: String,
) {
    val visualSupported: Boolean get() = paintRegionIds.isNotEmpty()
}

object MuscleCatalog {
    val all: List<Muscle> = listOf(
        muscle("pectoralis_major", "胸大肌", "Pectoralis major", MuscleRegion.CHEST, FigureSide.FRONT, 10, "胸前浅层", "pectoralis_major_l", "pectoralis_major_r"),
        muscle("deltoid_anterior", "三角肌前束", "Anterior deltoid", MuscleRegion.SHOULDERS, FigureSide.FRONT, 20, "肩前", "deltoid_anterior_l", "deltoid_anterior_r"),
        muscle("deltoid_lateral", "三角肌中束", "Lateral deltoid", MuscleRegion.SHOULDERS, FigureSide.FRONT, 21, "肩外侧", "deltoid_lateral_l", "deltoid_lateral_r"),
        muscle("deltoid_posterior", "三角肌后束", "Posterior deltoid", MuscleRegion.SHOULDERS, FigureSide.BACK, 22, "肩后", "deltoid_posterior_l", "deltoid_posterior_r"),
        muscle("latissimus_dorsi", "背阔肌", "Latissimus dorsi", MuscleRegion.BACK, FigureSide.BACK, 30, "背阔", "latissimus_dorsi_l", "latissimus_dorsi_r"),
        muscle("trapezius_upper", "斜方肌上部", "Upper trapezius", MuscleRegion.BACK, FigureSide.BACK, 31, "上斜方", "trapezius_upper"),
        muscle("trapezius_middle", "斜方肌中部", "Middle trapezius", MuscleRegion.BACK, FigureSide.BACK, 32, "中斜方", "trapezius_middle"),
        muscle("trapezius_lower", "斜方肌下部", "Lower trapezius", MuscleRegion.BACK, FigureSide.BACK, 33, "下斜方", "trapezius_lower"),
        muscle("teres_major", "大圆肌", "Teres major", MuscleRegion.BACK, FigureSide.BACK, 34, "腋后上方", "teres_major_l", "teres_major_r"),
        muscle("rhomboids", "菱形肌", "Rhomboids", MuscleRegion.BACK, null, 35, "位于斜方肌深层，正面和背面图上不单独绘制"),
        muscle("erector_spinae", "竖脊肌", "Erector spinae", MuscleRegion.BACK, FigureSide.BACK, 36, "腰背两侧", "erector_spinae"),
        muscle("biceps_brachii", "肱二头肌", "Biceps brachii", MuscleRegion.ARMS, FigureSide.FRONT, 40, "上臂前侧", "biceps_brachii_l", "biceps_brachii_r"),
        muscle("brachialis", "肱肌", "Brachialis", MuscleRegion.ARMS, null, 41, "位于肱二头肌深层，图上不单独绘制"),
        muscle("triceps_brachii", "肱三头肌", "Triceps brachii", MuscleRegion.ARMS, FigureSide.BACK, 42, "上臂后侧", "triceps_brachii_l", "triceps_brachii_r"),
        muscle("brachioradialis", "肱桡肌", "Brachioradialis", MuscleRegion.ARMS, FigureSide.FRONT, 43, "前臂桡侧", "brachioradialis_l", "brachioradialis_r"),
        muscle("forearm_flexors", "前臂屈肌群", "Forearm flexors", MuscleRegion.ARMS, FigureSide.FRONT, 44, "前臂掌侧", "forearm_flexors_l", "forearm_flexors_r"),
        muscle("forearm_extensors", "前臂伸肌群", "Forearm extensors", MuscleRegion.ARMS, FigureSide.BACK, 45, "前臂背侧", "forearm_extensors_l", "forearm_extensors_r"),
        muscle("rectus_abdominis", "腹直肌", "Rectus abdominis", MuscleRegion.CORE, FigureSide.FRONT, 50, "腹前正中", "rectus_abdominis"),
        muscle("external_oblique", "腹外斜肌", "External oblique", MuscleRegion.CORE, FigureSide.FRONT, 51, "腹外侧", "external_oblique_l", "external_oblique_r"),
        muscle("internal_oblique", "腹内斜肌", "Internal oblique", MuscleRegion.CORE, null, 52, "位于腹外斜肌深层，图上不单独绘制"),
        muscle("transversus_abdominis", "腹横肌", "Transversus abdominis", MuscleRegion.CORE, null, 53, "最深层腹肌，图上不绘制"),
        muscle("gluteus_maximus", "臀大肌", "Gluteus maximus", MuscleRegion.GLUTES, FigureSide.BACK, 60, "臀部浅层", "gluteus_maximus_l", "gluteus_maximus_r"),
        muscle("gluteus_medius", "臀中肌", "Gluteus medius", MuscleRegion.GLUTES, FigureSide.BACK, 61, "臀上部外侧", "gluteus_medius_l", "gluteus_medius_r"),
        muscle(
            "quadriceps",
            "股四头肌",
            "Quadriceps femoris",
            MuscleRegion.LEGS,
            FigureSide.FRONT,
            70,
            "整组股四头。高亮时只用合并遮罩，不再叠股直肌、股外侧肌和股内侧肌",
            "rectus_femoris_l",
            "rectus_femoris_r",
            "vastus_lateralis_l",
            "vastus_lateralis_r",
            "vastus_medialis_l",
            "vastus_medialis_r",
        ),
        muscle("rectus_femoris", "股直肌", "Rectus femoris", MuscleRegion.LEGS, FigureSide.FRONT, 71, "大腿前中", "rectus_femoris_l", "rectus_femoris_r"),
        muscle("vastus_lateralis", "股外侧肌", "Vastus lateralis", MuscleRegion.LEGS, FigureSide.FRONT, 72, "大腿前外侧", "vastus_lateralis_l", "vastus_lateralis_r"),
        muscle("vastus_medialis", "股内侧肌", "Vastus medialis", MuscleRegion.LEGS, FigureSide.FRONT, 73, "大腿前内侧下端", "vastus_medialis_l", "vastus_medialis_r"),
        muscle("hamstrings", "腘绳肌", "Hamstrings", MuscleRegion.LEGS, FigureSide.BACK, 74, "大腿后侧", "hamstrings_l", "hamstrings_r"),
        muscle("adductors", "内收肌群", "Adductors", MuscleRegion.LEGS, FigureSide.FRONT, 75, "大腿内侧", "adductors_l", "adductors_r"),
        muscle("sartorius", "缝匠肌", "Sartorius", MuscleRegion.LEGS, FigureSide.FRONT, 76, "正面有独立遮罩，背面不绘制", "sartorius_l", "sartorius_r"),
        muscle("gastrocnemius", "腓肠肌", "Gastrocnemius", MuscleRegion.CALVES, FigureSide.BACK, 80, "小腿后上部", "gastrocnemius_l", "gastrocnemius_r"),
        muscle("soleus", "比目鱼肌", "Soleus", MuscleRegion.CALVES, FigureSide.BACK, 81, "腓肠肌下方两侧可见部分", "soleus_l", "soleus_r"),
        muscle("tibialis_anterior", "胫骨前肌", "Tibialis anterior", MuscleRegion.CALVES, FigureSide.FRONT, 82, "小腿前侧", "tibialis_anterior_l", "tibialis_anterior_r"),
    )

    val byId: Map<String, Muscle> = all.associateBy { it.id }

    fun find(id: String): Muscle? = byId[id]

    fun specificMuscleForRegion(regionId: String): Muscle? {
        if (regionId == "quadriceps") return find("quadriceps")
        return all.filter { it.visualSupported && regionId in it.paintRegionIds && it.id != "quadriceps" }
            .minByOrNull { it.paintRegionIds.size }
    }
}

private fun muscle(
    id: String,
    nameZh: String,
    nameEn: String,
    region: MuscleRegion,
    side: FigureSide?,
    sortOrder: Int,
    note: String,
    vararg paintRegionIds: String,
): Muscle = Muscle(
    id = id,
    nameZh = nameZh,
    nameEn = nameEn,
    region = region,
    side = side,
    paintRegionIds = paintRegionIds.toList(),
    sortOrder = sortOrder,
    note = note,
)
