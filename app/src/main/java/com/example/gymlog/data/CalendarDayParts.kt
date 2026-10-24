package com.example.gymlog.data

/**
 * The "胸+腿" style body-part text shown under the date number on the expanded
 * calendar.
 *
 * Categories are the merged [CalendarColorCategory] buckets, so upper legs and
 * lower legs both read as 腿, and upper arms and lower arms both read as 手臂.
 * Waist, cardio, neck and anything unclassified read as 其他.
 *
 * Ordering is by that day's volume, highest first, matching the user's choice
 * to rank by how much was actually lifted. Equal volumes fall back to the
 * [CalendarColorCategory] ordinal so the text is stable between runs.
 */
object CalendarDayPartsLogic {
    /**
     * Cells are narrow; a folded tail becomes a trailing ellipsis.
     *
     * On the 720dp-wide detail grid a cell fits about five characters at the
     * 9sp label size ("背+胸+手臂" gets cut mid-word by the text ellipsis, so
     * the folding has to happen here instead). When more parts follow, one
     * character is reserved for the ellipsis.
     */
    const val MAX_VISIBLE_PARTS = 3
    private const val CHARS_WITH_MORE_PARTS = 4
    private const val CHARS_FOR_LAST_PART = 5

    fun labelsFor(
        sessions: List<SessionDetail>,
        measurements: List<BodyMeasurementEntity>,
        maxVisible: Int = MAX_VISIBLE_PARTS,
    ): Map<Long, String> {
        val limit = maxVisible.coerceAtLeast(1)
        val volumes = CalendarDayColorLogic.categoryVolumes(sessions, measurements)
        val labels = HashMap<Long, String>(volumes.size)
        for ((day, byCategory) in volumes) {
            val ordered = byCategory.entries
                .sortedWith(
                    compareByDescending<Map.Entry<CalendarColorCategory, Double>> { it.value }
                        .thenBy { it.key.ordinal },
                )
                .map { it.key.labelZh }
            if (ordered.isEmpty()) continue
            var text = ""
            var count = 0
            for ((index, label) in ordered.withIndex()) {
                val candidate = if (count == 0) label else "$text+$label"
                val nextCount = count + 1
                val charLimit = if (nextCount < ordered.size) {
                    CHARS_WITH_MORE_PARTS
                } else {
                    CHARS_FOR_LAST_PART
                }
                if (nextCount > limit || candidate.length > charLimit) break
                text = candidate
                count = nextCount
            }
            if (count == 0) continue
            labels[day] = if (count < ordered.size) "$text…" else text
        }
        return labels
    }
}
