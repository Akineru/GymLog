package com.example.gymlog.data

enum class DatasetMappingStatus {
    MAPPED,
    AMBIGUOUS,
    UNMAPPED,
}

data class DatasetMuscleRule(
    val status: DatasetMappingStatus,
    val muscleIds: List<String> = emptyList(),
    val note: String = "",
)

/**
 * Explicit, auditable mapping for the 50 muscle tokens in the external
 * exercise dataset. Only unambiguous tokens are inserted into exercise_muscles.
 * Generic tokens keep their candidate ids for later human confirmation but do
 * not paint several specific muscles automatically.
 */
object DatasetMuscleMapping {
    private fun mapped(id: String) = DatasetMuscleRule(DatasetMappingStatus.MAPPED, listOf(id))
    private fun ambiguous(vararg ids: String) =
        DatasetMuscleRule(DatasetMappingStatus.AMBIGUOUS, ids.toList(), "generic token; needs a human rule")
    private fun unmapped(note: String) =
        DatasetMuscleRule(DatasetMappingStatus.UNMAPPED, emptyList(), note)

    val rules: Map<String, DatasetMuscleRule> = mapOf(
        "pectorals" to mapped("pectoralis_major"),
        "chest" to mapped("pectoralis_major"),
        "upper chest" to mapped("pectoralis_major"),
        "biceps" to mapped("biceps_brachii"),
        "triceps" to mapped("triceps_brachii"),
        "brachialis" to mapped("brachialis"),
        "wrist flexors" to mapped("forearm_flexors"),
        "wrist extensors" to mapped("forearm_extensors"),
        "lats" to mapped("latissimus_dorsi"),
        "latissimus dorsi" to mapped("latissimus_dorsi"),
        "rhomboids" to mapped("rhomboids"),
        "spine" to mapped("erector_spinae"),
        "lower back" to mapped("erector_spinae"),
        "abs" to mapped("rectus_abdominis"),
        "abdominals" to mapped("rectus_abdominis"),
        "lower abs" to mapped("rectus_abdominis"),
        "obliques" to mapped("external_oblique"),
        "rear deltoids" to mapped("deltoid_posterior"),
        "quadriceps" to mapped("quadriceps"),
        "quads" to mapped("quadriceps"),
        "hamstrings" to mapped("hamstrings"),
        "adductors" to mapped("adductors"),
        "soleus" to mapped("soleus"),

        "shoulders" to ambiguous("deltoid_anterior", "deltoid_lateral", "deltoid_posterior"),
        "delts" to ambiguous("deltoid_anterior", "deltoid_lateral", "deltoid_posterior"),
        "deltoids" to ambiguous("deltoid_anterior", "deltoid_lateral", "deltoid_posterior"),
        "forearms" to ambiguous("forearm_flexors", "forearm_extensors"),
        "calves" to ambiguous("gastrocnemius", "soleus"),
        "trapezius" to ambiguous("trapezius_upper", "trapezius_middle", "trapezius_lower"),
        "traps" to ambiguous("trapezius_upper", "trapezius_middle", "trapezius_lower"),
        "upper back" to ambiguous("trapezius_middle", "rhomboids", "latissimus_dorsi"),
        "glutes" to ambiguous("gluteus_maximus", "gluteus_medius"),
        "core" to ambiguous("rectus_abdominis", "external_oblique"),
        "inner thighs" to ambiguous("adductors"),
        "shins" to ambiguous("tibialis_anterior"),

        "hip flexors" to unmapped("no stable GymLog id"),
        "cardiovascular system" to unmapped("not a skeletal muscle"),
        "ankles" to unmapped("no stable GymLog id"),
        "back" to unmapped("too generic; do not guess"),
        "rotator cuff" to unmapped("no stable GymLog id"),
        "feet" to unmapped("no stable GymLog id"),
        "serratus anterior" to unmapped("no stable GymLog id"),
        "abductors" to unmapped("not equivalent to gluteus medius"),
        "ankle stabilizers" to unmapped("no stable GymLog id"),
        "wrists" to unmapped("no stable GymLog id"),
        "hands" to unmapped("no stable GymLog id"),
        "levator scapulae" to unmapped("no stable GymLog id"),
        "sternocleidomastoid" to unmapped("no stable GymLog id"),
        "groin" to unmapped("do not automatically equate to adductors"),
        "grip muscles" to unmapped("do not automatically equate to forearm flexors"),
    )

    fun rule(token: String): DatasetMuscleRule =
        rules[token] ?: DatasetMuscleRule(
            DatasetMappingStatus.UNMAPPED,
            emptyList(),
            "token missing from the explicit mapping table",
        )

    fun mappedIds(token: String): List<String> {
        val rule = rule(token)
        return if (rule.status == DatasetMappingStatus.MAPPED) rule.muscleIds else emptyList()
    }
}
