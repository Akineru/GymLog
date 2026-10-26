package com.example.gymlog

import com.example.gymlog.data.BodyMeasurementEntity
import com.example.gymlog.data.WeightLogic
import com.example.gymlog.data.WeightMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WeightLogicTest {
    @Test
    fun bodyweightStoresNullNotZero() {
        val stored = WeightLogic.storedWeightKg(WeightMode.BODYWEIGHT, "0", "0")
        assertTrue(stored.isSuccess)
        assertNull(stored.getOrNull())
        assertEquals("0 kg", WeightLogic.displaySet(stored.getOrNull()))
    }

    @Test
    fun extraLoadIsAppliedToRecordedBodyWeight() {
        val stored = WeightLogic.storedWeightKg(WeightMode.BODYWEIGHT_PLUS, "70", "10")
        assertEquals(10.0, stored.getOrNull()!!, 0.0)
        val assisted = WeightLogic.storedWeightKg(WeightMode.BODYWEIGHT_PLUS, "", "-10")
        assertEquals(-10.0, assisted.getOrNull()!!, 0.0)
        assertEquals(70.0, WeightLogic.effectiveKg(WeightMode.BODYWEIGHT, null, 70.0), 0.0)
        assertEquals(0.0, WeightLogic.effectiveKg(WeightMode.BODYWEIGHT, null, 0.0), 0.0)
        assertEquals(80.0, WeightLogic.effectiveKg(WeightMode.BODYWEIGHT_PLUS, stored.getOrNull(), 70.0), 0.0)
        assertEquals(60.0, WeightLogic.effectiveKg(WeightMode.BODYWEIGHT_PLUS, assisted.getOrNull(), 70.0), 0.0)
        assertEquals(10.0, WeightLogic.effectiveKg(WeightMode.BODYWEIGHT_PLUS, 10.0, 0.0), 0.0)
        assertEquals(22.5, WeightLogic.effectiveKg(WeightMode.WEIGHTED, 22.5, 70.0), 0.0)
        assertEquals("80 kg", WeightLogic.displaySet(80.0))
        assertEquals("60 kg", WeightLogic.displaySet(60.0))
        assertEquals(480.0, WeightLogic.setVolume(60.0, 8), 0.0)
        assertTrue(WeightLogic.storedWeightKg(WeightMode.WEIGHTED, "-10", "").isFailure)
    }

    @Test
    fun bodyWeightUsesLatestRecordOnOrBeforeTheDay() {
        val rows = listOf(
            measurement(day = 10, weight = 60.0, createdAt = 1),
            measurement(day = 20, weight = null, createdAt = 2),
            measurement(day = 20, weight = 70.0, createdAt = 3),
            measurement(day = 30, weight = 80.0, createdAt = 4),
        )
        assertEquals(0.0, WeightLogic.bodyWeightKg(emptyList(), 20), 0.0)
        assertEquals(60.0, WeightLogic.bodyWeightKg(rows, 15), 0.0)
        assertEquals(70.0, WeightLogic.bodyWeightKg(rows, 20), 0.0)
        assertEquals(70.0, WeightLogic.bodyWeightKg(rows, 25), 0.0)
        assertEquals(80.0, WeightLogic.bodyWeightKg(rows, 30), 0.0)
    }

    @Test
    fun ordinaryWeightUsesNormalFieldNotExtra() {
        val stored = WeightLogic.storedWeightKg(WeightMode.WEIGHTED, "22.5", "99")
        assertEquals(22.5, stored.getOrNull()!!, 0.0)
        assertEquals("22.5 kg", WeightLogic.displaySet(stored.getOrNull()))
    }

    @Test
    fun stepRepsChangesByOne() {
        assertEquals("9", WeightLogic.stepReps("8", true))
        assertEquals("7", WeightLogic.stepReps("8", false))
        assertEquals("1", WeightLogic.stepReps("", true))
        assertEquals("0", WeightLogic.stepReps("0", false))
        assertEquals("0", WeightLogic.stepReps("", false))
    }

    @Test
    fun stepUsesTwoPointFive() {
        assertEquals("22.5", WeightLogic.stepKg("20", true))
        assertEquals("17.5", WeightLogic.stepKg("20", false))
        assertEquals("2.5", WeightLogic.stepKg("", true))
        assertEquals("0", WeightLogic.stepKg("1", false))
        assertEquals("-2.5", WeightLogic.stepSignedKg("0", false))
        assertEquals("-10", WeightLogic.stepSignedKg("-12.5", true))
        assertEquals("0", WeightLogic.stepKg("-5", false))
    }

    @Test
    fun acceptsManualDecimal() {
        assertEquals(21.25, WeightLogic.parseKg("21.25")!!, 0.0)
        assertEquals(21.25, WeightLogic.parseKg("21,25")!!, 0.0)
        assertNull(WeightLogic.parseKg("abc"))
        assertNull(WeightLogic.parseKg("-1"))
    }

    @Test
    fun volumeIsWeightTimesRepsTimesSets() {
        val oneSet = WeightLogic.setVolume(62.5, 8)
        assertEquals(500.0, oneSet, 0.0)
        val fourSets = WeightLogic.totalVolume(
            listOf(
                62.5 to 8,
                62.5 to 8,
                62.5 to 8,
                62.5 to 8,
            ),
        )
        assertEquals(2000.0, fourSets, 0.0)
        assertEquals("2000 kg", WeightLogic.displayVolume(fourSets))
        assertEquals(0.0, WeightLogic.setVolume(null, 8), 0.0)
    }

    private fun measurement(day: Long, weight: Double?, createdAt: Long): BodyMeasurementEntity {
        return BodyMeasurementEntity(
            id = "$day-$createdAt",
            recordedEpochDay = day,
            heightCm = null,
            weightKg = weight,
            bodyFatPercent = null,
            createdAt = createdAt,
        )
    }

    @Test
    fun missingWeightFailsInsteadOfSilentDrop() {
        assertTrue(WeightLogic.storedWeightKg(WeightMode.WEIGHTED, "", "").isFailure)
        assertTrue(WeightLogic.storedWeightKg(WeightMode.BODYWEIGHT_PLUS, "20", "").isFailure)
    }

    // ---- 手动输入：逐字过滤（不格式化，避免光标跳动） ----

    @Test
    fun weightInputKeepsIntegersAndDecimals() {
        assertEquals("20", WeightLogic.sanitizeWeightInput("20", signed = false))
        assertEquals("22.5", WeightLogic.sanitizeWeightInput("22.5", signed = false))
        assertEquals("7.5", WeightLogic.sanitizeWeightInput("7.5", signed = false))
        assertEquals("42.5", WeightLogic.sanitizeWeightInput("42.5", signed = false))
    }

    @Test
    fun weightInputDoesNotReformatWhileTyping() {
        // 正在输入小数：末尾的小数点必须保留，否则用户永远打不出「22.5」
        assertEquals("22.", WeightLogic.sanitizeWeightInput("22.", signed = false))
        // 暂时为空也要保留，不能立刻回写成 0
        assertEquals("", WeightLogic.sanitizeWeightInput("", signed = false))
        assertEquals("0", WeightLogic.sanitizeWeightInput("0", signed = false))
    }

    @Test
    fun weightInputRejectsIllegalCharactersAndExtraDots() {
        assertEquals("20", WeightLogic.sanitizeWeightInput("2a0", signed = false))
        assertEquals("22.5", WeightLogic.sanitizeWeightInput("22..5", signed = false))
        assertEquals("0.5", WeightLogic.sanitizeWeightInput(".5", signed = false))
        assertEquals("", WeightLogic.sanitizeWeightInput("abc", signed = false))
        assertEquals("3.25", WeightLogic.sanitizeWeightInput("3,25", signed = false))
    }

    @Test
    fun weightInputRespectsSignOnlyForExtraLoad() {
        // 普通重量不允许负数
        assertEquals("5", WeightLogic.sanitizeWeightInput("-5", signed = false))
        // 负重（自重 + 额外）允许负数，用于辅助
        assertEquals("-5", WeightLogic.sanitizeWeightInput("-5", signed = true))
        assertEquals("-10.5", WeightLogic.sanitizeWeightInput("-10.5", signed = true))
    }

    @Test
    fun weightInputCapsLengthToStayInRange() {
        assertEquals("1234", WeightLogic.sanitizeWeightInput("12345", signed = false))
        assertEquals("22.56", WeightLogic.sanitizeWeightInput("22.567", signed = false))
    }

    @Test
    fun repsInputAllowsMultiDigitAndStripsNonDigits() {
        assertEquals("12", WeightLogic.sanitizeRepsInput("12"))
        assertEquals("100", WeightLogic.sanitizeRepsInput("100"))
        assertEquals("12", WeightLogic.sanitizeRepsInput("1a2"))
        assertEquals("", WeightLogic.sanitizeRepsInput(""))
        assertEquals("1234", WeightLogic.sanitizeRepsInput("12345"))
    }

    // ---- 确认时（失焦 / 键盘完成）才做的规范化 ----

    @Test
    fun weightNormalizationHappensOnConfirm() {
        assertEquals("22", WeightLogic.normalizeWeightInput("22.", signed = false))
        assertEquals("22.5", WeightLogic.normalizeWeightInput("22.50", signed = false))
        assertEquals("22.5", WeightLogic.normalizeWeightInput("22,5", signed = false))
        // 清空后保持为空，交给既有的「重量必填」校验
        assertEquals("", WeightLogic.normalizeWeightInput("", signed = false))
        // 负数在普通重量下不合法，清空而不是写 0
        assertEquals("", WeightLogic.normalizeWeightInput("-10", signed = false))
        assertEquals("-10", WeightLogic.normalizeWeightInput("-10", signed = true))
        // 非法文本不会变成 NaN / Infinity
        assertEquals("", WeightLogic.normalizeWeightInput("NaN", signed = false))
        assertEquals("", WeightLogic.normalizeWeightInput("Infinity", signed = false))
    }

    @Test
    fun repsNormalizationHappensOnConfirm() {
        assertEquals("7", WeightLogic.normalizeRepsInput("007"))
        assertEquals("0", WeightLogic.normalizeRepsInput("0"))
        assertEquals("", WeightLogic.normalizeRepsInput(""))
        assertEquals("", WeightLogic.normalizeRepsInput("abc"))
    }

    @Test
    fun manualInputFeedsTheExistingStorageRules() {
        // 手动输入的值走的是同一套 storedWeightKg 规则，没有新增第二条路径
        val typed = WeightLogic.normalizeWeightInput("42.5", signed = false)
        assertEquals(42.5, WeightLogic.storedWeightKg(WeightMode.WEIGHTED, typed, "").getOrNull()!!, 0.0)
        val extra = WeightLogic.normalizeWeightInput("-7.5", signed = true)
        assertEquals(-7.5, WeightLogic.storedWeightKg(WeightMode.BODYWEIGHT_PLUS, "", extra).getOrNull()!!, 0.0)
        // 空值仍然是失败，不会静默写成 0
        assertTrue(
            WeightLogic.storedWeightKg(
                WeightMode.WEIGHTED,
                WeightLogic.normalizeWeightInput("", signed = false),
                "",
            ).isFailure,
        )
    }
}
