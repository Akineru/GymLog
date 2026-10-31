package com.example.gymlog.data

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

data class PerformedSet(
    val sessionId: String,
    val dateEpochDay: Long,
    val weightKg: Double?,
    val reps: Int,
    val weightMode: String,
)

data class PersonalRecord(
    val weightKg: Double?,
    val reps: Int,
    val weightMode: String,
    val dateEpochDay: Long,
)

data class ExercisePersonalRecords(
    val working: PersonalRecord?,
    val max: PersonalRecord?,
)

/**
 * 两类 PR 的唯一计算入口。
 *
 * - **极限 PR（max）**：所有有效训练组里 [有效重量][effectiveWeight] 最大的那一组，
 *   **不看次数**，也不用 Epley 之类的公式推算。所以 `60kg × 3` 会赢过 `50kg × 8`。
 * - **做组 PR（working）**：次数达到 [minWorkingReps] 的有效组里，重量最大者；
 *   重量相同再看次数，再相同取更晚的日期。默认门槛 6 次，用户可在「训练偏好」里改。
 *
 * 有效组沿用项目既有规则：`reps > 0`（未完成组的 reps 为空，在仓库层就被过滤掉）。
 * 重量用 [WeightLogic.effectiveKg] 已换算好的「有效重量」，各重量模式的定义保持不变。
 */
object PersonalRecordLogic {
    /** 做组 PR 的默认最低次数。 */
    const val DEFAULT_MIN_WORKING_REPS = 6

    /**
     * 允许的取值区间。
     *
     * 下限 1：0 次或负数没有意义（连「做了一个」都不算）。
     * 上限 30：单组做到 30 次以上已属耐力项目，再往上调门槛只会让做组 PR 长期为空；
     * 留出 1..30 足够覆盖项目里所有常见做组场景，也避免把明显误输入（如 999）当成有效设置。
     */
    const val MIN_WORKING_REPS = 1
    const val MAX_WORKING_REPS = 30

    fun fromSessions(
        sets: List<PerformedSet>,
        minWorkingReps: Int = DEFAULT_MIN_WORKING_REPS,
    ): ExercisePersonalRecords {
        val threshold = normalizeMinWorkingReps(minWorkingReps)
        var working: PersonalRecord? = null
        var max: PersonalRecord? = null
        sets.forEach { set ->
            if (set.reps <= 0) return@forEach
            val candidate = PersonalRecord(
                weightKg = set.weightKg,
                reps = set.reps,
                weightMode = set.weightMode,
                dateEpochDay = set.dateEpochDay,
            )
            if (isMaxCandidate(set)) {
                max = keepHigher(max, candidate, PrKind.MAX, preferLatestOnTie = true)
            }
            if (isWorkingCandidate(set, threshold)) {
                working = keepHigher(working, candidate, PrKind.WORKING, preferLatestOnTie = true)
            }
        }
        return ExercisePersonalRecords(working = working, max = max)
    }

    /**
     * 极限 PR 的候选：有效组 + 有效重量大于 0。
     *
     * 纯自重组（自重模式下重量记为 0）在「重量」维度上没有意义，
     * 让它们参与会出现「极限 PR 0 kg」，所以排除；自重模式的做组 PR 仍按次数比较，不受影响。
     */
    fun isMaxCandidate(set: PerformedSet): Boolean =
        set.reps > 0 && effectiveWeight(set.weightKg) > 0.0

    /** 做组 PR 的候选：次数达到当前门槛。 */
    fun isWorkingCandidate(set: PerformedSet, minWorkingReps: Int): Boolean =
        set.reps >= normalizeMinWorkingReps(minWorkingReps)

    /** 把任意输入收敛到合法区间；越界/异常值都落到最近的合法边界。 */
    fun normalizeMinWorkingReps(value: Int): Int = value.coerceIn(MIN_WORKING_REPS, MAX_WORKING_REPS)

    /** 只含成绩，不含日期；日期由 UI 单独取 [formatDate] 右对齐展示。 */
    fun displayWorking(record: PersonalRecord): String {
        return "${WeightLogic.displaySet(record.weightKg)}×${record.reps}"
    }

    fun displayMax(record: PersonalRecord): String {
        return WeightLogic.displaySet(record.weightKg)
    }

    /** 2026.10.09 这样的纯数字日期，PR 行右对齐用。 */
    fun formatDate(dateEpochDay: Long): String {
        return LocalDate.ofEpochDay(dateEpochDay).format(dateFormatter)
    }

    /** 两类 PR 的比较维度不同：极限只比重量，做组还要比次数。 */
    internal enum class PrKind { WORKING, MAX }

    /**
     * Stored PRs prefer the latest date when weight（和次数）都相等。
     * The trend chart leaves [preferLatestOnTie] false so an equal performance
     * does not create a new point.
     */
    internal fun keepHigher(
        current: PersonalRecord?,
        candidate: PersonalRecord?,
        kind: PrKind = PrKind.WORKING,
        preferLatestOnTie: Boolean = false,
    ): PersonalRecord? {
        if (candidate == null) return current
        if (current == null) return candidate
        val cmp = compareRecords(candidate, current, kind, preferLatestOnTie)
        return when {
            cmp > 0 -> candidate
            cmp < 0 -> current
            else -> current
        }
    }

    internal fun compareRecords(
        a: PersonalRecord,
        b: PersonalRecord,
        kind: PrKind = PrKind.WORKING,
        preferLatestOnTie: Boolean = false,
    ): Int {
        val weightCmp = effectiveWeight(a.weightKg).compareTo(effectiveWeight(b.weightKg))
        if (weightCmp != 0) return weightCmp
        if (kind == PrKind.WORKING) {
            val repsCmp = a.reps.compareTo(b.reps)
            if (repsCmp != 0) return repsCmp
        }
        return if (preferLatestOnTie) a.dateEpochDay.compareTo(b.dateEpochDay) else 0
    }

    private fun effectiveWeight(weightKg: Double?): Double = weightKg ?: 0.0

    private val dateFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy.MM.dd", Locale.CHINA)
}
