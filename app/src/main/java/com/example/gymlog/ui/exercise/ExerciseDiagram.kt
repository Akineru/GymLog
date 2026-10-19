package com.example.gymlog.ui.exercise

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.gymlog.R
import com.example.gymlog.data.BodyPart
import com.example.gymlog.data.ExerciseEntity
import com.example.gymlog.data.displayName

@Composable
fun ExerciseDiagram(
    exercise: ExerciseEntity,
    modifier: Modifier = Modifier.size(72.dp),
) {
    Image(
        painter = painterResource(diagramRes(exercise.id, exercise.bodyPart)),
        contentDescription = exercise.displayName(),
        modifier = modifier.clip(RoundedCornerShape(12.dp)),
        contentScale = ContentScale.Fit,
    )
}

internal fun hasSpecificDiagram(exerciseId: String): Boolean {
    return presetDiagramRes.containsKey(exerciseId.removePrefix("preset_"))
}

private fun diagramRes(exerciseId: String, bodyPart: String): Int {
    presetDiagramRes[exerciseId.removePrefix("preset_")]?.let { return it }
    return when (bodyPart) {
        BodyPart.CHEST.name -> R.drawable.diagram_barbell_bench_press
        BodyPart.SHOULDERS.name -> R.drawable.diagram_overhead_press
        BodyPart.BACK.name -> R.drawable.diagram_barbell_row
        BodyPart.LEGS.name -> R.drawable.diagram_barbell_squat
        BodyPart.ARMS.name -> R.drawable.diagram_dumbbell_curl
        BodyPart.ABS.name -> R.drawable.diagram_crunch
        else -> R.drawable.diagram_farmer_carry
    }
}

private val presetDiagramRes: Map<String, Int> = mapOf(
    "barbell_bench_press" to R.drawable.diagram_barbell_bench_press,
    "dumbbell_bench_press" to R.drawable.diagram_dumbbell_bench_press,
    "incline_bench_press" to R.drawable.diagram_incline_bench_press,
    "decline_bench_press" to R.drawable.diagram_decline_bench_press,
    "dumbbell_fly" to R.drawable.diagram_dumbbell_fly,
    "cable_crossover" to R.drawable.diagram_cable_crossover,
    "chest_press_machine" to R.drawable.diagram_chest_press_machine,
    "pec_deck" to R.drawable.diagram_pec_deck,
    "push_up" to R.drawable.diagram_push_up,
    "overhead_press" to R.drawable.diagram_overhead_press,
    "dumbbell_shoulder_press" to R.drawable.diagram_dumbbell_shoulder_press,
    "lateral_raise" to R.drawable.diagram_lateral_raise,
    "front_raise" to R.drawable.diagram_front_raise,
    "rear_delt_fly" to R.drawable.diagram_rear_delt_fly,
    "face_pull" to R.drawable.diagram_face_pull,
    "arnold_press" to R.drawable.diagram_arnold_press,
    "shoulder_press_machine" to R.drawable.diagram_shoulder_press_machine,
    "pull_up" to R.drawable.diagram_pull_up,
    "lat_pulldown" to R.drawable.diagram_lat_pulldown,
    "barbell_row" to R.drawable.diagram_barbell_row,
    "dumbbell_row" to R.drawable.diagram_dumbbell_row,
    "seated_row" to R.drawable.diagram_seated_row,
    "deadlift" to R.drawable.diagram_deadlift,
    "t_bar_row" to R.drawable.diagram_t_bar_row,
    "inverted_row" to R.drawable.diagram_inverted_row,
    "barbell_squat" to R.drawable.diagram_barbell_squat,
    "leg_press" to R.drawable.diagram_leg_press,
    "lunge" to R.drawable.diagram_lunge,
    "romanian_deadlift" to R.drawable.diagram_romanian_deadlift,
    "leg_extension" to R.drawable.diagram_leg_extension,
    "leg_curl" to R.drawable.diagram_leg_curl,
    "calf_raise" to R.drawable.diagram_calf_raise,
    "hip_thrust" to R.drawable.diagram_hip_thrust,
    "bulgarian_split_squat" to R.drawable.diagram_bulgarian_split_squat,
    "hip_abduction" to R.drawable.diagram_hip_abduction,
    "barbell_curl" to R.drawable.diagram_barbell_curl,
    "dumbbell_curl" to R.drawable.diagram_dumbbell_curl,
    "hammer_curl" to R.drawable.diagram_hammer_curl,
    "tricep_pushdown" to R.drawable.diagram_tricep_pushdown,
    "skull_crusher" to R.drawable.diagram_skull_crusher,
    "dip" to R.drawable.diagram_dip,
    "close_grip_bench" to R.drawable.diagram_close_grip_bench,
    "crunch" to R.drawable.diagram_crunch,
    "sit_up" to R.drawable.diagram_sit_up,
    "plank" to R.drawable.diagram_plank,
    "hanging_leg_raise" to R.drawable.diagram_hanging_leg_raise,
    "russian_twist" to R.drawable.diagram_russian_twist,
    "mountain_climber" to R.drawable.diagram_mountain_climber,
    "cable_crunch" to R.drawable.diagram_cable_crunch,
    "weighted_push_up" to R.drawable.diagram_push_up,
    "incline_dumbbell_bench" to R.drawable.diagram_incline_bench_press,
    "decline_dumbbell_bench" to R.drawable.diagram_decline_bench_press,
    "incline_push_up" to R.drawable.diagram_push_up,
    "decline_push_up" to R.drawable.diagram_push_up,
    "seated_barbell_press" to R.drawable.diagram_overhead_press,
    "cable_lateral_raise" to R.drawable.diagram_lateral_raise,
    "assisted_pull_up" to R.drawable.diagram_pull_up,
    "straight_arm_pulldown" to R.drawable.diagram_lat_pulldown,
    "chest_supported_row" to R.drawable.diagram_dumbbell_row,
    "landmine_row" to R.drawable.diagram_barbell_row,
    "sumo_deadlift" to R.drawable.diagram_deadlift,
    "stiff_leg_deadlift" to R.drawable.diagram_romanian_deadlift,
    "barbell_shrug" to R.drawable.diagram_deadlift,
    "front_squat" to R.drawable.diagram_barbell_squat,
    "goblet_squat" to R.drawable.diagram_barbell_squat,
    "hack_squat" to R.drawable.diagram_leg_press,
    "smith_squat" to R.drawable.diagram_barbell_squat,
    "step_up" to R.drawable.diagram_lunge,
    "barbell_hip_thrust" to R.drawable.diagram_hip_thrust,
    "hip_adduction" to R.drawable.diagram_hip_abduction,
    "seated_calf_raise" to R.drawable.diagram_calf_raise,
    "tibialis_raise" to R.drawable.diagram_calf_raise,
    "preacher_curl" to R.drawable.diagram_barbell_curl,
    "incline_dumbbell_curl" to R.drawable.diagram_dumbbell_curl,
    "cable_curl" to R.drawable.diagram_barbell_curl,
    "reverse_curl" to R.drawable.diagram_barbell_curl,
    "overhead_tricep_extension" to R.drawable.diagram_skull_crusher,
    "wrist_curl" to R.drawable.diagram_barbell_curl,
    "reverse_wrist_curl" to R.drawable.diagram_barbell_curl,
    "reverse_crunch" to R.drawable.diagram_crunch,
    "side_plank" to R.drawable.diagram_plank,
    "dead_bug" to R.drawable.diagram_crunch,
    "bird_dog" to R.drawable.diagram_plank,
    "pallof_press" to R.drawable.diagram_cable_crunch,
    "band_pull_apart" to R.drawable.diagram_rear_delt_fly,
    "farmer_carry" to R.drawable.diagram_farmer_carry,
    "burpee" to R.drawable.diagram_burpee,
    "jumping_jack" to R.drawable.diagram_jumping_jack,
    "kettlebell_swing" to R.drawable.diagram_kettlebell_swing,
)
