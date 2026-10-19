package com.example.gymlog.data

data class BodyGirths(
    val armLeftCm: Double? = null,
    val armRightCm: Double? = null,
    val forearmLeftCm: Double? = null,
    val forearmRightCm: Double? = null,
    val chestCm: Double? = null,
    val waistCm: Double? = null,
    val hipCm: Double? = null,
    val thighLeftCm: Double? = null,
    val thighRightCm: Double? = null,
    val calfLeftCm: Double? = null,
    val calfRightCm: Double? = null,
) {
    fun isEmpty(): Boolean {
        return armLeftCm == null && armRightCm == null &&
            forearmLeftCm == null && forearmRightCm == null &&
            chestCm == null && waistCm == null && hipCm == null &&
            thighLeftCm == null && thighRightCm == null &&
            calfLeftCm == null && calfRightCm == null
    }

    fun changedFrom(previous: BodyMeasurementEntity?): Boolean {
        if (previous == null) return !isEmpty()
        return !same(previous.armLeftCm, armLeftCm) ||
            !same(previous.armRightCm, armRightCm) ||
            !same(previous.forearmLeftCm, forearmLeftCm) ||
            !same(previous.forearmRightCm, forearmRightCm) ||
            !same(previous.chestCm, chestCm) ||
            !same(previous.waistCm, waistCm) ||
            !same(previous.hipCm, hipCm) ||
            !same(previous.thighLeftCm, thighLeftCm) ||
            !same(previous.thighRightCm, thighRightCm) ||
            !same(previous.calfLeftCm, calfLeftCm) ||
            !same(previous.calfRightCm, calfRightCm)
    }

    private fun same(left: Double?, right: Double?): Boolean {
        if (left == null && right == null) return true
        if (left == null || right == null) return false
        return kotlin.math.abs(left - right) < 0.0001
    }
}

fun BodyMeasurementEntity.girths(): BodyGirths {
    return BodyGirths(
        armLeftCm = armLeftCm,
        armRightCm = armRightCm,
        forearmLeftCm = forearmLeftCm,
        forearmRightCm = forearmRightCm,
        chestCm = chestCm,
        waistCm = waistCm,
        hipCm = hipCm,
        thighLeftCm = thighLeftCm,
        thighRightCm = thighRightCm,
        calfLeftCm = calfLeftCm,
        calfRightCm = calfRightCm,
    )
}
