package com.example.gymlog

import androidx.sqlite.db.SupportSQLiteDatabase

fun rewindMeasurementsToVersion11(sqlite: SupportSQLiteDatabase) {
    sqlite.execSQL(
        """
        CREATE TABLE `body_measurements_v11` (
            `id` TEXT NOT NULL,
            `recordedEpochDay` INTEGER NOT NULL,
            `heightCm` REAL,
            `weightKg` REAL,
            `bodyFatPercent` REAL,
            `waistCm` REAL,
            `armCm` REAL,
            `createdAt` INTEGER NOT NULL,
            PRIMARY KEY(`id`)
        )
        """.trimIndent(),
    )
    sqlite.execSQL(
        """
        INSERT INTO `body_measurements_v11` (
            `id`, `recordedEpochDay`, `heightCm`, `weightKg`, `bodyFatPercent`,
            `waistCm`, `armCm`, `createdAt`
        )
        SELECT `id`, `recordedEpochDay`, `heightCm`, `weightKg`, `bodyFatPercent`,
            `waistCm`, NULL AS `armCm`, `createdAt`
        FROM `body_measurements`
        """.trimIndent(),
    )
    sqlite.execSQL("DROP TABLE `body_measurements`")
    sqlite.execSQL("ALTER TABLE `body_measurements_v11` RENAME TO `body_measurements`")
    sqlite.execSQL(
        """
        CREATE INDEX IF NOT EXISTS `index_body_measurements_recordedEpochDay`
        ON `body_measurements` (`recordedEpochDay`)
        """.trimIndent(),
    )
}
