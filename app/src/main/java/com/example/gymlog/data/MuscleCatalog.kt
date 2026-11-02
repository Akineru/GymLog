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
        muscle("trapezius", "斜方肌", "Trapezius", MuscleRegion.BACK, FigureSide.BACK, 30, "上背与颈后，左右连为一整块", "trapezius"),
        muscle("rhomboids", "菱形肌", "Rhomboids", MuscleRegion.BACK, FigureSide.BACK, 35, "肩胛骨之间", "rhomboids"),
        muscle("latissimus_dorsi", "背阔肌", "Latissimus dorsi", MuscleRegion.BACK, FigureSide.BACK, 40, "背阔", "latissimus_dorsi_l", "latissimus_dorsi_r"),
        muscle("erector_spinae", "竖脊肌", "Erector spinae", MuscleRegion.BACK, FigureSide.BACK, 42, "腰背两侧", "erector_spinae"),
        muscle("biceps_brachii", "肱二头肌", "Biceps brachii", MuscleRegion.ARMS, FigureSide.FRONT, 50, "上臂前侧", "biceps_brachii_l", "biceps_brachii_r"),
        muscle("triceps_brachii", "肱三头肌", "Triceps brachii", MuscleRegion.ARMS, FigureSide.BACK, 51, "上臂后侧", "triceps_brachii_l", "triceps_brachii_r"),
        muscle("forearm", "前臂肌群", "Forearm muscles", MuscleRegion.ARMS, FigureSide.FRONT, 52, "前臂屈伸肌群合并展示", "forearm"),
        muscle("rectus_abdominis", "腹直肌", "Rectus abdominis", MuscleRegion.CORE, FigureSide.FRONT, 60, "腹前正中", "rectus_abdominis"),
        muscle("external_oblique", "腹外斜肌", "External oblique", MuscleRegion.CORE, FigureSide.FRONT, 61, "腹外侧", "external_oblique_l", "external_oblique_r"),
        muscle("gluteus_maximus", "臀大肌", "Gluteus maximus", MuscleRegion.GLUTES, FigureSide.BACK, 70, "臀部浅层", "gluteus_maximus_l", "gluteus_maximus_r"),
        muscle("gluteus_medius", "臀中肌", "Gluteus medius", MuscleRegion.GLUTES, FigureSide.BACK, 71, "臀上部外侧", "gluteus_medius_l", "gluteus_medius_r"),
        muscle("quadriceps", "股四头肌", "Quadriceps femoris", MuscleRegion.LEGS, FigureSide.FRONT, 80, "大腿前侧整组", "quadriceps_l", "quadriceps_r"),
        muscle("hamstrings", "腘绳肌", "Hamstrings", MuscleRegion.LEGS, FigureSide.BACK, 81, "大腿后侧", "hamstrings_l", "hamstrings_r"),
        muscle("adductors", "内收肌群", "Adductors", MuscleRegion.LEGS, FigureSide.FRONT, 82, "大腿内侧", "adductors_l", "adductors_r"),
        muscle("gastrocnemius", "小腿三头肌", "Triceps surae", MuscleRegion.CALVES, FigureSide.BACK, 90, "小腿后侧整块", "gastrocnemius"),
    )

    val byId: Map<String, Muscle> = all.associateBy { it.id }

    fun find(id: String): Muscle? = byId[id]

    fun specificMuscleForRegion(regionId: String): Muscle? {
        return all.filter { it.visualSupported && regionId in it.paintRegionIds }
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
