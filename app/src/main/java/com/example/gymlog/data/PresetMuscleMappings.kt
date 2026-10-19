package com.example.gymlog.data

data class MuscleLink(
    val exerciseId: String,
    val muscleId: String,
    val role: MuscleRole,
    val sortOrder: Int = 0,
)

object PresetMuscleMappings {
    fun links(exerciseId: String): List<MuscleLink> {
        val raw = table[exerciseId].orEmpty()
        return raw.mapIndexed { index, (muscleId, role) ->
            MuscleLink(exerciseId = exerciseId, muscleId = muscleId, role = role, sortOrder = index)
        }
    }

    fun exerciseIds(): Set<String> = table.keys

    private val table: Map<String, List<Pair<String, MuscleRole>>> = mapOf(
        p("barbell_bench_press", pri("pectoralis_major"), sec("deltoid_anterior", "triceps_brachii")),
        p("dumbbell_bench_press", pri("pectoralis_major"), sec("deltoid_anterior", "triceps_brachii")),
        p("incline_bench_press", pri("pectoralis_major", "deltoid_anterior"), sec("triceps_brachii")),
        p("decline_bench_press", pri("pectoralis_major"), sec("triceps_brachii")),
        p("dumbbell_fly", pri("pectoralis_major"), sec("deltoid_anterior")),
        p("cable_crossover", pri("pectoralis_major"), sec("deltoid_anterior")),
        p("chest_press_machine", pri("pectoralis_major"), sec("deltoid_anterior", "triceps_brachii")),
        p("pec_deck", pri("pectoralis_major")),
        p("push_up", pri("pectoralis_major"), sec("triceps_brachii", "deltoid_anterior")),
        p("incline_dumbbell_bench", pri("pectoralis_major", "deltoid_anterior"), sec("triceps_brachii")),
        p("decline_dumbbell_bench", pri("pectoralis_major"), sec("triceps_brachii")),
        p("incline_push_up", pri("pectoralis_major"), sec("triceps_brachii")),
        p("decline_push_up", pri("pectoralis_major", "deltoid_anterior"), sec("triceps_brachii")),
        p("weighted_push_up", pri("pectoralis_major"), sec("triceps_brachii", "deltoid_anterior")),

        p("overhead_press", pri("deltoid_anterior", "deltoid_lateral"), sec("triceps_brachii", "trapezius_upper")),
        p("seated_barbell_press", pri("deltoid_anterior", "deltoid_lateral"), sec("triceps_brachii")),
        p("dumbbell_shoulder_press", pri("deltoid_anterior", "deltoid_lateral"), sec("triceps_brachii")),
        p("shoulder_press_machine", pri("deltoid_anterior", "deltoid_lateral"), sec("triceps_brachii")),
        p("lateral_raise", pri("deltoid_lateral")),
        p("cable_lateral_raise", pri("deltoid_lateral")),
        p("front_raise", pri("deltoid_anterior")),
        p("rear_delt_fly", pri("deltoid_posterior"), sec("trapezius_middle", "rhomboids")),
        p("face_pull", pri("deltoid_posterior", "trapezius_middle"), sec("rhomboids")),
        p("arnold_press", pri("deltoid_anterior", "deltoid_lateral"), sec("triceps_brachii")),

        p("pull_up", pri("latissimus_dorsi"), sec("biceps_brachii", "teres_major", "trapezius_lower")),
        p("assisted_pull_up", pri("latissimus_dorsi"), sec("biceps_brachii", "teres_major")),
        p("lat_pulldown", pri("latissimus_dorsi"), sec("biceps_brachii", "teres_major")),
        p("straight_arm_pulldown", pri("latissimus_dorsi"), sec("teres_major")),
        p("barbell_row", pri("latissimus_dorsi", "trapezius_middle"), sec("rhomboids", "deltoid_posterior", "biceps_brachii")),
        p("dumbbell_row", pri("latissimus_dorsi"), sec("deltoid_posterior", "biceps_brachii")),
        p("chest_supported_row", pri("latissimus_dorsi", "trapezius_middle"), sec("rhomboids", "deltoid_posterior")),
        p("seated_row", pri("latissimus_dorsi", "trapezius_middle"), sec("biceps_brachii", "deltoid_posterior")),
        p("t_bar_row", pri("latissimus_dorsi", "trapezius_middle"), sec("deltoid_posterior", "biceps_brachii")),
        p("inverted_row", pri("latissimus_dorsi", "trapezius_middle"), sec("biceps_brachii", "deltoid_posterior")),
        p("landmine_row", pri("latissimus_dorsi"), sec("deltoid_posterior", "biceps_brachii")),
        p("deadlift", pri("erector_spinae", "gluteus_maximus", "hamstrings"), sec("quadriceps", "trapezius_upper", "latissimus_dorsi", "forearm_flexors")),
        p("sumo_deadlift", pri("gluteus_maximus", "adductors", "quadriceps"), sec("hamstrings", "erector_spinae", "trapezius_upper")),
        p("romanian_deadlift", pri("hamstrings", "gluteus_maximus"), sec("erector_spinae")),
        p("stiff_leg_deadlift", pri("hamstrings"), sec("gluteus_maximus", "erector_spinae")),
        p("barbell_shrug", pri("trapezius_upper")),

        p("barbell_squat", pri("quadriceps", "gluteus_maximus"), sec("hamstrings", "erector_spinae")),
        p("front_squat", pri("quadriceps"), sec("gluteus_maximus", "rectus_abdominis")),
        p("goblet_squat", pri("quadriceps", "gluteus_maximus"), sec("rectus_abdominis")),
        p("hack_squat", pri("quadriceps"), sec("gluteus_maximus")),
        p("smith_squat", pri("quadriceps", "gluteus_maximus"), sec("erector_spinae")),
        p("leg_press", pri("quadriceps", "gluteus_maximus"), sec("hamstrings")),
        p("leg_extension", pri("quadriceps", "rectus_femoris", "vastus_lateralis", "vastus_medialis")),
        p("leg_curl", pri("hamstrings")),
        p("lunge", pri("quadriceps", "gluteus_maximus"), sec("hamstrings")),
        p("step_up", pri("quadriceps", "gluteus_maximus"), sec("hamstrings")),
        p("bulgarian_split_squat", pri("quadriceps", "gluteus_maximus"), sec("hamstrings")),
        p("hip_thrust", pri("gluteus_maximus"), sec("hamstrings")),
        p("barbell_hip_thrust", pri("gluteus_maximus"), sec("hamstrings")),
        p("hip_abduction", pri("gluteus_medius")),
        p("hip_adduction", pri("adductors")),
        p("calf_raise", pri("gastrocnemius"), sec("soleus")),
        p("seated_calf_raise", pri("soleus"), sec("gastrocnemius")),
        p("tibialis_raise", pri("tibialis_anterior")),

        p("barbell_curl", pri("biceps_brachii"), sec("brachialis")),
        p("dumbbell_curl", pri("biceps_brachii"), sec("brachialis")),
        p("hammer_curl", pri("brachioradialis", "brachialis"), sec("biceps_brachii")),
        p("preacher_curl", pri("biceps_brachii"), sec("brachialis")),
        p("incline_dumbbell_curl", pri("biceps_brachii"), sec("brachialis")),
        p("cable_curl", pri("biceps_brachii"), sec("brachialis")),
        p("reverse_curl", pri("brachioradialis"), sec("biceps_brachii", "forearm_extensors")),
        p("close_grip_bench", pri("triceps_brachii"), sec("pectoralis_major", "deltoid_anterior")),
        p("dip", pri("triceps_brachii", "pectoralis_major"), sec("deltoid_anterior")),
        p("tricep_pushdown", pri("triceps_brachii")),
        p("overhead_tricep_extension", pri("triceps_brachii")),
        p("skull_crusher", pri("triceps_brachii")),
        p("wrist_curl", pri("forearm_flexors")),
        p("reverse_wrist_curl", pri("forearm_extensors")),

        p("crunch", pri("rectus_abdominis")),
        p("reverse_crunch", pri("rectus_abdominis")),
        p("sit_up", pri("rectus_abdominis"), sec("external_oblique")),
        p("hanging_leg_raise", pri("rectus_abdominis"), sec("external_oblique")),
        p("plank", pri("rectus_abdominis", "transversus_abdominis"), sec("external_oblique", "erector_spinae")),
        p("side_plank", pri("external_oblique"), sec("internal_oblique", "gluteus_medius")),
        p("dead_bug", pri("rectus_abdominis"), sec("transversus_abdominis")),
        p("bird_dog", pri("erector_spinae"), sec("gluteus_maximus", "rectus_abdominis")),
        p("russian_twist", pri("external_oblique"), sec("rectus_abdominis")),
        p("mountain_climber", pri("rectus_abdominis"), sec("quadriceps")),
        p("cable_crunch", pri("rectus_abdominis")),
        p("pallof_press", pri("external_oblique", "internal_oblique"), sec("transversus_abdominis")),

        p("farmer_carry", pri("forearm_flexors", "trapezius_upper"), sec("erector_spinae")),
        p("burpee", pri("pectoralis_major", "quadriceps"), sec("deltoid_anterior")),
        p("jumping_jack", pri("deltoid_lateral"), sec("gastrocnemius")),
        p("kettlebell_swing", pri("gluteus_maximus", "hamstrings"), sec("erector_spinae")),
        p("band_pull_apart", pri("deltoid_posterior", "trapezius_middle"), sec("rhomboids")),
    )

    private fun p(id: String, vararg groups: List<Pair<String, MuscleRole>>): Pair<String, List<Pair<String, MuscleRole>>> {
        return "preset_$id" to groups.flatMap { it }
    }

    private fun pri(vararg ids: String) = ids.map { it to MuscleRole.PRIMARY }
    private fun sec(vararg ids: String) = ids.map { it to MuscleRole.SECONDARY }
}
