package com.example.gymlog.ui.theme

import androidx.compose.ui.graphics.Color
import com.example.gymlog.data.BodyPart
import com.example.gymlog.data.ExerciseClassification

private val partColorByLabel = mapOf(
    "胸" to Color(0xFFE57373),
    "背" to Color(0xFF64B5F6),
    "肩" to Color(0xFFFFB74D),
    "腿" to Color(0xFF81C784),
    "手臂" to Color(0xFFBA68C8),
    "腹" to Color(0xFF90A4AE),
    "其他" to Color(0xFF90A4AE),
)

private val partColorByStorageName = mapOf(
    BodyPart.CHEST.name to partColorByLabel.getValue("胸"),
    BodyPart.BACK.name to partColorByLabel.getValue("背"),
    BodyPart.SHOULDERS.name to partColorByLabel.getValue("肩"),
    BodyPart.LEGS.name to partColorByLabel.getValue("腿"),
    BodyPart.ARMS.name to partColorByLabel.getValue("手臂"),
    BodyPart.ABS.name to partColorByLabel.getValue("其他"),
    BodyPart.OTHER.name to partColorByLabel.getValue("其他"),
    ExerciseClassification.CHEST.id to partColorByLabel.getValue("胸"),
    ExerciseClassification.BACK.id to partColorByLabel.getValue("背"),
    ExerciseClassification.SHOULDERS.id to partColorByLabel.getValue("肩"),
    ExerciseClassification.UPPER_ARMS.id to partColorByLabel.getValue("手臂"),
    ExerciseClassification.LOWER_ARMS.id to partColorByLabel.getValue("手臂"),
    ExerciseClassification.LEGACY_ARMS.id to partColorByLabel.getValue("手臂"),
    ExerciseClassification.UPPER_LEGS.id to partColorByLabel.getValue("腿"),
    ExerciseClassification.LOWER_LEGS.id to partColorByLabel.getValue("腿"),
    ExerciseClassification.LEGACY_LEGS.id to partColorByLabel.getValue("腿"),
    ExerciseClassification.WAIST.id to partColorByLabel.getValue("其他"),
    ExerciseClassification.CARDIO.id to partColorByLabel.getValue("其他"),
    ExerciseClassification.NECK.id to partColorByLabel.getValue("其他"),
    ExerciseClassification.LEGACY_BODY_OTHER.id to partColorByLabel.getValue("其他"),
    ExerciseClassification.UNKNOWN_BODY.id to partColorByLabel.getValue("其他"),
)

fun colorForPart(part: String): Color {
    return partColorByLabel[part]
        ?: partColorByStorageName[part]
        ?: Color(0xFF90A4AE)
}

fun orderedPartColors(parts: Collection<String>): List<Color> {
    val unique = parts.toSet()
    val known = if (unique.any { it.startsWith("body_") }) {
        ExerciseClassification.bodyUi.map { it.id }.filter { unique.contains(it) }
    } else {
        BodyPart.entries.map { it.name }.filter { unique.contains(it) }
            .ifEmpty { unique.filter { partColorByLabel.containsKey(it) } }
    }
    val rest = unique.filterNot { it in known }
    return (known + rest).map(::colorForPart)
}
