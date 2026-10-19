package com.example.gymlog.data

data class MuscleExerciseRef(
    val exerciseId: String,
    val name: String,
    val sessionId: String,
    val role: MuscleRole,
)

data class MuscleHighlight(
    val muscleId: String,
    val role: MuscleRole,
    val exercises: List<MuscleExerciseRef>,
)

object MuscleMapLogic {
    fun linksFor(
        exerciseId: String,
        isCustom: Boolean,
        stored: List<MuscleLink>,
    ): List<MuscleLink> {
        val own = stored.filter { it.exerciseId == exerciseId }
        if (isCustom || own.isNotEmpty()) return own.distinctBy { it.muscleId }
        return PresetMuscleMappings.links(exerciseId)
    }

    fun highlightsFor(
        sessions: List<SessionDetail>,
        storedLinks: List<MuscleLink>,
    ): List<MuscleHighlight> {
        val grouped = linkedRefs(sessions, storedLinks).groupBy { it.muscleId }
        return grouped.map { (muscleId, refs) ->
            val role = if (refs.any { it.role == MuscleRole.PRIMARY }) MuscleRole.PRIMARY else MuscleRole.SECONDARY
            MuscleHighlight(
                muscleId = muscleId,
                role = role,
                exercises = refs.distinctBy { it.exerciseId to it.sessionId }.map {
                MuscleExerciseRef(
                    exerciseId = it.exerciseId,
                    name = it.name,
                    sessionId = it.sessionId,
                    role = it.role,
                )
            },
            )
        }.sortedBy { MuscleCatalog.find(it.muscleId)?.sortOrder ?: Int.MAX_VALUE }
    }

    /**
     * Current product behavior is asymmetric: a `quadriceps` query returns
     * exercises explicitly mapped to the composite, while a child-head query
     * also includes the composite because `quadriceps.paintRegionIds` contains
     * the child region ids. Keep this behavior until a product rule is confirmed.
     */
    fun exercisesForRegion(
        regionId: String,
        highlights: List<MuscleHighlight>,
    ): List<MuscleExerciseRef> {
        val muscleIds = if (regionId == "quadriceps") {
            setOf("quadriceps")
        } else {
            MuscleCatalog.all
                .filter { regionId in it.paintRegionIds }
                .map { it.id }
                .toSet()
        }
        return highlights
            .filter { it.muscleId in muscleIds }
            .flatMap { it.exercises }
            .distinctBy { it.exerciseId to it.sessionId }
    }

    fun validateLinks(exerciseIds: Set<String>, links: List<MuscleLink>) {
        val pairs = links.map { it.exerciseId to it.muscleId }
        require(pairs.size == pairs.toSet().size) { "DUPLICATE_MUSCLE_LINK" }
        links.forEach { link ->
            require(link.exerciseId in exerciseIds) { "MISSING_EXERCISE" }
            require(link.muscleId in MuscleCatalog.byId) { "UNKNOWN_MUSCLE" }
        }
    }

    private fun linkedRefs(
        sessions: List<SessionDetail>,
        storedLinks: List<MuscleLink>,
    ): List<TaggedRef> {
        return sessions.flatMap { detail ->
            detail.exercises.flatMap { item ->
                linksFor(item.exercise.id, item.exercise.isCustom, storedLinks).map { link ->
                    TaggedRef(
                        muscleId = link.muscleId,
                        exerciseId = item.exercise.id,
                        name = item.exercise.name,
                        sessionId = detail.session.id,
                        role = link.role,
                    )
                }
            }
        }
    }

    private data class TaggedRef(
        val muscleId: String,
        val exerciseId: String,
        val name: String,
        val sessionId: String,
        val role: MuscleRole,
    )
}
