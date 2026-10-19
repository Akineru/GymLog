package com.example.gymlog.data

/**
 * Stable source labels stored in `exercises.source`.
 *
 * LEGACY is the default for every row created before version 13, including
 * user-created rows from old versions. New custom rows explicitly use CUSTOM.
 */
object ExerciseSource {
    const val LEGACY = "LEGACY"
    const val CUSTOM = "CUSTOM"
    const val DATASET = "EXERCISES_DATASET_ZH"

    const val DATASET_CATALOG_VERSION = 1
    const val DATASET_ASSET = "exercises/dataset_v1.json"
    const val PRESET_MATCH_ASSET = "exercises/preset_match_candidates.json"
}

object ExerciseMediaStatus {
    const val NOT_BUNDLED = "NOT_BUNDLED"
    const val BUNDLED = "BUNDLED"
    const val DOWNLOADED = "DOWNLOADED"
    const val MISSING = "MISSING"
    const val CORRUPT = "CORRUPT"
}
