package com.example.gymlog

import com.example.gymlog.data.BodyPart
import com.example.gymlog.data.EquipmentType
import com.example.gymlog.data.ExerciseEntity
import com.example.gymlog.data.MuscleCatalog
import com.example.gymlog.data.MuscleHighlight
import com.example.gymlog.data.MuscleLink
import com.example.gymlog.data.MuscleMapLogic
import com.example.gymlog.data.MuscleRole
import com.example.gymlog.ui.muscle.MuscleMaskCatalog
import com.example.gymlog.data.PresetExercises
import com.example.gymlog.data.PresetMuscleMappings
import com.example.gymlog.data.SessionDetail
import com.example.gymlog.data.SessionExercise
import com.example.gymlog.data.WeightMode
import com.example.gymlog.data.WorkoutSessionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MuscleMapLogicTest {
    @Test
    fun deadliftAndRomanianDeadliftUseDifferentMuscles() {
        val deadlift = PresetMuscleMappings.links("preset_deadlift")
        val romanian = PresetMuscleMappings.links("preset_romanian_deadlift")
        assertTrue(deadlift.any { it.muscleId == "erector_spinae" && it.role == MuscleRole.PRIMARY })
        assertTrue(deadlift.any { it.muscleId == "quadriceps" && it.role == MuscleRole.SECONDARY })
        assertTrue(romanian.any { it.muscleId == "hamstrings" && it.role == MuscleRole.PRIMARY })
        assertTrue(romanian.any { it.muscleId == "gluteus_maximus" && it.role == MuscleRole.PRIMARY })
        assertTrue(romanian.none { it.muscleId == "quadriceps" })
        assertNotEquals(
            deadlift.map { it.muscleId to it.role }.toSet(),
            romanian.map { it.muscleId to it.role }.toSet(),
        )
    }

    @Test
    fun barbellCurlIncludesBiceps() {
        val links = PresetMuscleMappings.links("preset_barbell_curl")
        assertTrue(links.any { it.muscleId == "biceps_brachii" && it.role == MuscleRole.PRIMARY })
    }

    @Test
    fun sameDaySessionsMergeAndAMuscleIsHighlightedOnce() {
        val curl = exercise("preset_barbell_curl", "杠铃弯举")
        val hammer = exercise("preset_hammer_curl", "锤式弯举")
        val morning = session("s1", 20_000, curl)
        val evening = session("s2", 20_000, hammer)
        val highlights = MuscleMapLogic.highlightsFor(listOf(morning, evening), emptyList())
        val biceps = highlights.filter { it.muscleId == "biceps_brachii" }
        assertEquals(1, biceps.size)
        assertEquals(2, biceps.single().exercises.size)
    }

    @Test
    fun removingASessionDropsItsUniqueMuscle() {
        val raise = exercise("preset_seated_calf_raise", "坐姿提踵")
        val squat = exercise("preset_barbell_squat", "杠铃深蹲")
        val both = MuscleMapLogic.highlightsFor(
            listOf(session("s1", 20_000, raise), session("s2", 20_000, squat)),
            emptyList(),
        )
        val remaining = MuscleMapLogic.highlightsFor(listOf(session("s2", 20_000, squat)), emptyList())
        assertTrue(both.any { it.muscleId == "gastrocnemius" })
        assertTrue(remaining.none { it.muscleId == "gastrocnemius" })
        assertTrue(remaining.any { it.muscleId == "quadriceps" })
    }

    @Test
    fun customExerciseUsesStoredMusclesAndCanStayUnconfigured() {
        val stored = listOf(
            MuscleLink("custom_1", "biceps_brachii", MuscleRole.PRIMARY, 0),
            MuscleLink("custom_1", "brachialis", MuscleRole.SECONDARY, 1),
        )
        val links = MuscleMapLogic.linksFor("custom_1", isCustom = true, stored = stored)
        assertEquals(setOf("biceps_brachii", "brachialis"), links.map { it.muscleId }.toSet())
        assertTrue(MuscleMapLogic.linksFor("custom_2", isCustom = true, stored = emptyList()).isEmpty())
        assertTrue(
            MuscleMapLogic.linksFor("preset_barbell_curl", isCustom = false, stored = emptyList())
                .any { it.muscleId == "biceps_brachii" },
        )
    }

    @Test
    fun everyPresetHasAMappingAndEveryMappingHasAPreset() {
        val presets = PresetExercises.all(0L).map { it.id }.toSet()
        presets.forEach { id ->
            assertTrue(id, PresetMuscleMappings.links(id).isNotEmpty())
        }
        assertEquals(presets, PresetMuscleMappings.exerciseIds())
    }

    @Test
    fun figureMasksMatchExistingMuscles() {
        assertEquals(MuscleCatalog.all.map { it.id }.toSet(), MuscleMaskCatalog.specs.map { it.muscleId }.toSet())
        assertEquals(emptySet<String>(), MuscleCatalog.all.filter { !it.visualSupported }.map { it.id }.toSet())
        assertTrue(MuscleMaskCatalog.visible("pectoralis_major", front = true))
        assertFalse(MuscleMaskCatalog.visible("pectoralis_major", front = false))
        assertTrue(MuscleMaskCatalog.visible("latissimus_dorsi", front = false))
        assertTrue(MuscleMaskCatalog.visible("forearm", front = true))
        assertTrue(MuscleMaskCatalog.visible("forearm", front = false))
        assertTrue(MuscleMaskCatalog.visible("trapezius", front = true))
        assertTrue(MuscleMaskCatalog.visible("trapezius", front = false))
        val layers = MuscleMaskCatalog.layers(
            listOf(
                MuscleHighlight("quadriceps", MuscleRole.SECONDARY, emptyList()),
                MuscleHighlight("biceps_brachii", MuscleRole.SECONDARY, emptyList()),
                MuscleHighlight("rhomboids", MuscleRole.PRIMARY, emptyList()),
            ),
        )
        assertEquals(listOf("quadriceps", "biceps_brachii", "rhomboids"), layers.map { it.muscleId })
        assertEquals(MuscleRole.PRIMARY, layers.single { it.muscleId == "rhomboids" }.role)
        assertEquals("pectoralis_major_l", MuscleMaskCatalog.regionId("pectoralis_major", 400))
        assertEquals("pectoralis_major_r", MuscleMaskCatalog.regionId("pectoralis_major", 700))
        assertEquals("quadriceps_l", MuscleMaskCatalog.regionId("quadriceps", 400))
        assertEquals("trapezius", MuscleMaskCatalog.regionId("trapezius", 400))
        assertEquals("trapezius", MuscleCatalog.specificMuscleForRegion("trapezius")?.id)
        assertEquals("pectoralis_major", MuscleCatalog.specificMuscleForRegion("pectoralis_major_l")?.id)
    }

    @Test
    fun quadricepsRegionQueriesIncludePresetAndCustomExercises() {
        val squat = exercise("preset_barbell_squat", "杠铃深蹲")
        val legExtension = exercise("preset_leg_extension", "腿屈伸")
        val custom = ExerciseEntity(
            id = "custom_quad_only",
            name = "只映射股四头肌的自定义动作",
            bodyPart = BodyPart.LEGS.name,
            equipmentType = EquipmentType.FREE_WEIGHT.name,
            weightMode = WeightMode.WEIGHTED.name,
            isCustom = true,
            aliases = null,
            createdAt = 1L,
        )
        val stored = listOf(MuscleLink("custom_quad_only", "quadriceps", MuscleRole.PRIMARY, 0))
        val highlights = MuscleMapLogic.highlightsFor(
            listOf(
                session("s1", 20_000, squat),
                session("s2", 20_000, legExtension),
                session("s3", 20_000, custom),
            ),
            stored,
        )

        val composite = MuscleMapLogic.exercisesForRegion("quadriceps", highlights)
            .map { it.exerciseId }
            .toSet()
        val side = MuscleMapLogic.exercisesForRegion("quadriceps_l", highlights)
            .map { it.exerciseId }
            .toSet()

        assertTrue("preset_barbell_squat" in composite)
        assertTrue("preset_leg_extension" in composite)
        assertTrue("custom_quad_only" in composite)
        assertEquals(composite, side)
    }

    @Test
    fun duplicateAndUnknownLinksAreRejected() {
        val ids = setOf("custom_1")
        val duplicate = listOf(
            MuscleLink("custom_1", "biceps_brachii", MuscleRole.PRIMARY, 0),
            MuscleLink("custom_1", "biceps_brachii", MuscleRole.SECONDARY, 1),
        )
        assertTrue(fails { MuscleMapLogic.validateLinks(ids, duplicate) })
        assertTrue(fails { MuscleMapLogic.validateLinks(ids, listOf(MuscleLink("missing", "biceps_brachii", MuscleRole.PRIMARY, 0))) })
        assertTrue(fails { MuscleMapLogic.validateLinks(ids, listOf(MuscleLink("custom_1", "not_real", MuscleRole.PRIMARY, 0))) })
    }

    private fun fails(block: () -> Unit): Boolean {
        return try {
            block()
            false
        } catch (error: IllegalArgumentException) {
            true
        }
    }

    private fun session(id: String, day: Long, exercise: ExerciseEntity): SessionDetail {
        return SessionDetail(
            session = WorkoutSessionEntity(id = id, dateEpochDay = day, note = null, createdAt = 1L),
            exercises = listOf(SessionExercise(exercise, emptyList())),
        )
    }

    private fun exercise(id: String, name: String): ExerciseEntity {
        return ExerciseEntity(
            id = id,
            name = name,
            bodyPart = BodyPart.ARMS.name,
            equipmentType = EquipmentType.FREE_WEIGHT.name,
            weightMode = WeightMode.WEIGHTED.name,
            isCustom = false,
            aliases = null,
            createdAt = 1L,
        )
    }
}
