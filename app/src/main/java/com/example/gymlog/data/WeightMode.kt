package com.example.gymlog.data

enum class WeightMode(val labelZh: String) {
    WEIGHTED("普通重量"),
    BODYWEIGHT("自重"),
    BODYWEIGHT_PLUS("自重负重");

    companion object {
        fun fromStorage(value: String): WeightMode {
            return entries.find { it.name == value } ?: WEIGHTED
        }
    }
}
