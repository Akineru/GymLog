package com.example.gymlog.ui.muscle

import com.example.gymlog.data.MuscleCatalog
import com.example.gymlog.data.MuscleHighlight
import com.example.gymlog.data.MuscleRole

internal data class MuscleMaskSpec(
    val muscleId: String,
    val front: Boolean,
    val back: Boolean,
)

internal data class MaskLayer(
    val muscleId: String,
    val role: MuscleRole,
)

/**
 * v3 mask pack: file name == muscle id, one PNG per muscle per shown side.
 * A false side means the mask has no shape in that view.
 */
internal object MuscleMaskCatalog {
    const val WIDTH = 1024
    const val HEIGHT = 1536
    const val OVERLAY_ALPHA = 0.42f

    val specs: List<MuscleMaskSpec> = listOf(
        mask("pectoralis_major", front = true),
        mask("deltoid_anterior", front = true),
        mask("deltoid_lateral", front = true, back = true),
        mask("deltoid_posterior", back = true),
        mask("trapezius", front = true, back = true),
        mask("rhomboids", back = true),
        mask("latissimus_dorsi", back = true),
        mask("erector_spinae", back = true),
        mask("biceps_brachii", front = true),
        mask("triceps_brachii", front = true, back = true),
        mask("forearm", front = true, back = true),
        mask("rectus_abdominis", front = true),
        mask("external_oblique", front = true, back = true),
        mask("gluteus_maximus", back = true),
        mask("gluteus_medius", front = true, back = true),
        mask("quadriceps", front = true),
        mask("hamstrings", back = true),
        mask("adductors", front = true, back = true),
        mask("gastrocnemius", front = true, back = true),
    )

    private val byId = specs.associateBy { it.muscleId }

    fun spec(muscleId: String): MuscleMaskSpec? = byId[muscleId]

    fun visible(muscleId: String, front: Boolean): Boolean {
        val item = byId[muscleId] ?: return false
        return if (front) item.front else item.back
    }

    fun layers(highlights: List<MuscleHighlight>): List<MaskLayer> {
        return highlights.mapNotNull { highlight ->
            val item = byId[highlight.muscleId] ?: return@mapNotNull null
            if (!item.front && !item.back) return@mapNotNull null
            MaskLayer(highlight.muscleId, highlight.role)
        }.sortedBy { if (it.role == MuscleRole.SECONDARY) 0 else 1 }
    }

    fun regionId(muscleId: String, imageX: Int): String {
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
        return highlights.find { it.muscleId == muscleId }?.role
    }

    private fun mask(
        muscleId: String,
        front: Boolean = false,
        back: Boolean = false,
    ) = MuscleMaskSpec(muscleId, front, back)
}
