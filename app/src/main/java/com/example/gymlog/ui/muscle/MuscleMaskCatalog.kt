package com.example.gymlog.ui.muscle

import com.example.gymlog.data.MuscleCatalog
import com.example.gymlog.data.MuscleHighlight
import com.example.gymlog.data.MuscleRole

internal data class MuscleMaskSpec(
    val muscleId: String,
    val fileName: String,
    val front: Boolean,
    val back: Boolean,
)

internal data class MaskLayer(
    val muscleId: String,
    val role: MuscleRole,
)

/**
 * Explicit map from the app's existing muscle ids to the v2 mask pack.
 * [fileName] is the PNG stem under muscle_masks/front and muscle_masks/back.
 * A false side means that view's mask is empty or the manifest marks it invisible.
 */
internal object MuscleMaskCatalog {
    const val WIDTH = 1024
    const val HEIGHT = 1536
    const val OVERLAY_ALPHA = 0.42f

    private val quadHeads = setOf("rectus_femoris", "vastus_lateralis", "vastus_medialis")

    val specs: List<MuscleMaskSpec> = listOf(
        mask("pectoralis_major", front = true),
        mask("deltoid_anterior", front = true),
        mask("deltoid_lateral", fileName = "deltoid_middle", front = true),
        mask("deltoid_posterior", back = true),
        mask("trapezius_upper", back = true),
        mask("trapezius_middle", back = true),
        mask("trapezius_lower", back = true),
        mask("latissimus_dorsi", back = true),
        mask("teres_major", back = true),
        mask("rhomboids"),
        mask("erector_spinae", back = true),
        mask("biceps_brachii", front = true),
        mask("brachialis"),
        mask("triceps_brachii", back = true),
        mask("brachioradialis", front = true),
        mask("forearm_flexors", front = true),
        mask("forearm_extensors", front = true, back = true),
        mask("rectus_abdominis", front = true),
        mask("external_oblique", front = true),
        mask("internal_oblique"),
        mask("transversus_abdominis"),
        mask("gluteus_maximus", back = true),
        mask("gluteus_medius", back = true),
        mask("quadriceps", front = true),
        mask("rectus_femoris", front = true),
        mask("vastus_lateralis", front = true),
        mask("vastus_medialis", front = true),
        mask("hamstrings", back = true),
        mask("adductors", front = true),
        mask("sartorius", front = true),
        mask("gastrocnemius", back = true),
        mask("soleus", back = true),
        mask("tibialis_anterior", front = true),
    )

    private val byId = specs.associateBy { it.muscleId }

    fun spec(muscleId: String): MuscleMaskSpec? = byId[muscleId]

    fun visible(muscleId: String, front: Boolean): Boolean {
        val item = byId[muscleId] ?: return false
        return if (front) item.front else item.back
    }

    fun layers(highlights: List<MuscleHighlight>): List<MaskLayer> {
        val byMuscle = highlights.associateBy { it.muscleId }
        val drawQuad = byMuscle["quadriceps"] != null
        val layers = mutableListOf<MaskLayer>()
        if (drawQuad) {
            val involved = listOfNotNull(byMuscle["quadriceps"]) + quadHeads.mapNotNull { byMuscle[it] }
            val role = if (involved.any { it.role == MuscleRole.PRIMARY }) {
                MuscleRole.PRIMARY
            } else {
                MuscleRole.SECONDARY
            }
            layers += MaskLayer("quadriceps", role)
        }
        highlights.forEach { highlight ->
            if (highlight.muscleId == "quadriceps") return@forEach
            if (drawQuad && highlight.muscleId in quadHeads) return@forEach
            val item = byId[highlight.muscleId] ?: return@forEach
            if (!item.front && !item.back) return@forEach
            layers += MaskLayer(highlight.muscleId, highlight.role)
        }
        return layers.sortedBy { if (it.role == MuscleRole.SECONDARY) 0 else 1 }
    }

    fun regionId(muscleId: String, imageX: Int): String {
        if (muscleId == "quadriceps") return "quadriceps"
        val muscle = MuscleCatalog.find(muscleId) ?: return muscleId
        val sided = if (imageX < WIDTH / 2) "${muscleId}_l" else "${muscleId}_r"
        return when {
            sided in muscle.paintRegionIds -> sided
            muscleId in muscle.paintRegionIds -> muscleId
            else -> muscle.paintRegionIds.firstOrNull() ?: muscleId
        }
    }

    fun roleFor(regionId: String, highlights: List<MuscleHighlight>): MuscleRole? {
        val muscleId = regionId.removeSuffix("_l").removeSuffix("_r")
        val roles = mutableListOf<MuscleRole>()
        highlights.find { it.muscleId == muscleId }?.let { roles += it.role }
        if (muscleId in quadHeads) {
            highlights.find { it.muscleId == "quadriceps" }?.let { roles += it.role }
        }
        return when {
            roles.isEmpty() -> null
            roles.any { it == MuscleRole.PRIMARY } -> MuscleRole.PRIMARY
            else -> MuscleRole.SECONDARY
        }
    }

    private fun mask(
        muscleId: String,
        fileName: String = muscleId,
        front: Boolean = false,
        back: Boolean = false,
    ) = MuscleMaskSpec(muscleId, fileName, front, back)
}
