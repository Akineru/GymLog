package com.example.gymlog.data

object WeightLogic {
    const val STEP_KG = 2.5

    fun parseKg(text: String): Double? {
        val value = parseSignedKg(text) ?: return null
        if (value < 0.0) return null
        return value
    }

    fun parseSignedKg(text: String): Double? {
        val normalized = text.trim().replace(',', '.')
        if (normalized.isEmpty()) return null
        val value = normalized.toDoubleOrNull() ?: return null
        if (value.isNaN() || value.isInfinite()) return null
        return value
    }

    fun formatKg(value: Double): String {
        val rounded = Math.round(value * 1000.0) / 1000.0
        return if (rounded % 1.0 == 0.0) {
            rounded.toLong().toString()
        } else {
            rounded.toString()
        }
    }

    fun stepKg(currentText: String, increase: Boolean): String {
        val current = parseKg(currentText) ?: 0.0
        val next = if (increase) current + STEP_KG else current - STEP_KG
        return formatKg(maxOf(0.0, next))
    }

    fun stepSignedKg(currentText: String, increase: Boolean): String {
        val current = parseSignedKg(currentText) ?: 0.0
        val next = if (increase) current + STEP_KG else current - STEP_KG
        return formatKg(next)
    }

    fun stepReps(currentText: String, increase: Boolean): String {
        val current = currentText.trim().toIntOrNull()?.coerceAtLeast(0) ?: 0
        val next = if (increase) current + 1 else current - 1
        return maxOf(0, next).toString()
    }

    /**
     * 重量手动输入的**逐字过滤**：只保留数字与一个小数点，负重模式额外允许开头一个负号。
     *
     * 刻意不做任何格式化（不改写、不补零、不四舍五入），这样输入「22.」时不会被吃掉小数点，
     * 光标也不会因为数值被重排而跳动。规范化放在失焦 / 确认时由 [normalizeWeightInput] 做。
     */
    fun sanitizeWeightInput(raw: String, signed: Boolean): String {
        val builder = StringBuilder()
        var dot = false
        var intDigits = 0
        var fracDigits = 0
        raw.forEachIndexed { index, ch ->
            when {
                ch.isDigit() -> {
                    if (dot) {
                        if (fracDigits < 2) {
                            builder.append(ch)
                            fracDigits++
                        }
                    } else if (intDigits < 4) {
                        builder.append(ch)
                        intDigits++
                    }
                }
                (ch == '.' || ch == ',') && !dot -> {
                    dot = true
                    // 允许直接打「.5」，补前导 0 更稳
                    if (intDigits == 0) {
                        builder.append('0')
                        intDigits++
                    }
                    builder.append('.')
                }
                ch == '-' && signed && index == 0 && builder.isEmpty() -> builder.append('-')
            }
        }
        return builder.toString()
    }

    /** 次数手动输入的逐字过滤：只保留数字，最多 4 位（足够 100 次以上）。 */
    fun sanitizeRepsInput(raw: String): String = raw.filter { it.isDigit() }.take(4)

    /**
     * 确认（失焦 / 键盘完成）时才做的规范化：把编辑中的字符串收敛成合法数值。
     * 解析不出来就清空，交给既有的「重量必填」校验提示，不写入 0。
     */
    fun normalizeWeightInput(text: String, signed: Boolean): String {
        val parsed = if (signed) parseSignedKg(text) else parseKg(text)
        return if (parsed == null) "" else formatKg(parsed)
    }

    fun normalizeRepsInput(text: String): String {
        val value = text.trim().toIntOrNull() ?: return ""
        return value.coerceAtLeast(0).toString()
    }

    fun storedWeightKg(
        mode: WeightMode,
        normalText: String,
        extraText: String,
    ): Result<Double?> {
        return when (mode) {
            WeightMode.BODYWEIGHT -> Result.success(null)
            WeightMode.WEIGHTED -> {
                val value = parseKg(normalText)
                    ?: return Result.failure(IllegalArgumentException("WEIGHT_REQUIRED"))
                Result.success(value)
            }
            WeightMode.BODYWEIGHT_PLUS -> {
                val value = parseSignedKg(extraText)
                    ?: return Result.failure(IllegalArgumentException("EXTRA_WEIGHT_REQUIRED"))
                Result.success(value)
            }
        }
    }

    fun bodyWeightKg(measurements: List<BodyMeasurementEntity>, onEpochDay: Long): Double {
        val recorded = measurements
            .filter { it.recordedEpochDay <= onEpochDay && it.weightKg != null }
            .maxWithOrNull(compareBy<BodyMeasurementEntity>({ it.recordedEpochDay }, { it.createdAt }))
            ?.weightKg
        return recorded?.takeIf { it > 0.0 } ?: 0.0
    }

    fun effectiveKg(mode: WeightMode, storedKg: Double?, bodyWeightKg: Double): Double {
        return when (mode) {
            WeightMode.WEIGHTED -> storedKg ?: 0.0
            WeightMode.BODYWEIGHT -> bodyWeightKg
            WeightMode.BODYWEIGHT_PLUS -> bodyWeightKg + (storedKg ?: 0.0)
        }
    }

    fun setVolume(weightKg: Double?, reps: Int?): Double {
        val weight = weightKg ?: 0.0
        val count = reps ?: 0
        if (weight <= 0.0 || count <= 0) return 0.0
        return weight * count
    }

    fun totalVolume(sets: List<Pair<Double?, Int?>>): Double {
        return sets.sumOf { setVolume(it.first, it.second) }
    }

    fun displayVolume(volume: Double): String {
        return "${formatKg(volume)} kg"
    }

    fun displaySet(weightKg: Double?): String {
        return "${formatKg(weightKg ?: 0.0)} kg"
    }

    fun defaultModeForExercise(exercise: ExerciseEntity): WeightMode {
        val stored = WeightMode.fromStorage(exercise.weightMode)
        return if (stored == WeightMode.BODYWEIGHT_PLUS) WeightMode.WEIGHTED else stored
    }
}
