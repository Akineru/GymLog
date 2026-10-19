package com.example.gymlog.data

enum class GirthSlot {
    ARM,
    CHEST,
    FOREARM,
    WAIST,
    HIP,
    THIGH,
    CALF,
    ;

    val onRight: Boolean
        get() = this == CHEST || this == WAIST || this == HIP || this == THIGH
}

object GirthCopy {
    fun pairValue(left: Double?, right: Double?, missing: String = MISSING): String {
        return "左 ${number(left, missing)} / 右 ${number(right, missing)} cm"
    }

    fun singleValue(value: Double?, missing: String = MISSING): String {
        return "${number(value, missing)} cm"
    }

    fun number(value: Double?, missing: String = MISSING): String {
        return value?.let(WeightLogic::formatKg) ?: missing
    }

    fun valueLines(slot: GirthSlot, girths: BodyGirths, missing: String = MISSING): String {
        return when (slot) {
            GirthSlot.ARM -> pairValue(girths.armLeftCm, girths.armRightCm, missing)
            GirthSlot.FOREARM -> pairValue(girths.forearmLeftCm, girths.forearmRightCm, missing)
            GirthSlot.CHEST -> singleValue(girths.chestCm, missing)
            GirthSlot.WAIST -> singleValue(girths.waistCm, missing)
            GirthSlot.HIP -> singleValue(girths.hipCm, missing)
            GirthSlot.THIGH -> pairValue(girths.thighLeftCm, girths.thighRightCm, missing)
            GirthSlot.CALF -> pairValue(girths.calfLeftCm, girths.calfRightCm, missing)
        }
    }

    private const val MISSING = "--"
}
