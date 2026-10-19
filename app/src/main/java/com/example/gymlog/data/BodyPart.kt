package com.example.gymlog.data

enum class BodyPart(val labelZh: String) {
    CHEST("胸"),
    SHOULDERS("肩"),
    BACK("背"),
    LEGS("腿"),
    ARMS("手臂"),
    ABS("腹"),
    OTHER("其他");

    companion object {
        fun rank(stored: String): Int {
            val index = entries.indexOfFirst { it.name == stored }
            return if (index >= 0) index else Int.MAX_VALUE
        }
    }
}

fun <T> List<T>.sortedGroupingBodyPart(bodyPartOf: (T) -> String): List<T> {
    return mapIndexed { index, item ->
        Triple(ExerciseClassification.partSortOrder(bodyPartOf(item)), index, item)
    }
        .sortedWith(compareBy({ it.first }, { it.second }))
        .map { it.third }
}
