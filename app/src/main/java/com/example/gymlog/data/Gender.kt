package com.example.gymlog.data

enum class Gender {
    MALE,
    FEMALE,
    OTHER,
    ;

    companion object {
        fun fromStored(value: String?): Gender? = entries.find { it.name == value }
    }
}
