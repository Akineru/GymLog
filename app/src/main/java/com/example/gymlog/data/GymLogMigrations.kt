package com.example.gymlog.data

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_20_21 = object : Migration(20, 21) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 新增「收藏」字段；优先级从 0–100 收窄为 0–10。
        // 旧值转换规则：1–10 保持不变；11–100 按 十分之一 缩放（95→9、80→8、42→4、100→10），
        // 保留用户的相对排序意图；负值（不应存在）钳到 0。收藏状态独立记录，一律默认未收藏。
        val columns = db.query("PRAGMA table_info(`exercises`)").use { cursor ->
            val names = HashSet<String>()
            while (cursor.moveToNext()) names.add(cursor.getString(1))
            names
        }
        if ("isFavorite" !in columns) {
            db.execSQL(
                "ALTER TABLE `exercises` ADD COLUMN `isFavorite` INTEGER NOT NULL DEFAULT 0",
            )
        }
        db.execSQL(
            """
            UPDATE `exercises` SET `priority` = CASE
                WHEN `priority` < 0 THEN 0
                WHEN `priority` > 10 THEN `priority` * 10 / 100
                ELSE `priority`
            END
            """.trimIndent(),
        )
        // 极端兜底：万一缩放后仍越界（不可能，但迁移必须保证值域）
        db.execSQL("UPDATE `exercises` SET `priority` = 10 WHERE `priority` > 10")
        db.execSQL("UPDATE `exercises` SET `priority` = 0 WHERE `priority` < 0")
    }
}

val MIGRATION_19_20 = object : Migration(19, 20) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 动作新增「优先级 / 要点（怎么做、细节）」三个字段。
        // 全部带安全默认值，旧动作升级后仍在、顺序不变（priority 默认 0 时不会改变任何排序）。
        val columns = db.query("PRAGMA table_info(`exercises`)").use { cursor ->
            val names = HashSet<String>()
            while (cursor.moveToNext()) names.add(cursor.getString(1))
            names
        }
        if ("priority" !in columns) {
            db.execSQL(
                "ALTER TABLE `exercises` ADD COLUMN `priority` INTEGER NOT NULL DEFAULT 0",
            )
        }
        if ("howTo" !in columns) {
            db.execSQL("ALTER TABLE `exercises` ADD COLUMN `howTo` TEXT")
        }
        if ("cues" !in columns) {
            db.execSQL("ALTER TABLE `exercises` ADD COLUMN `cues` TEXT")
        }
    }
}

val MIGRATION_18_19 = object : Migration(18, 19) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `exercise_durations` (
                `id` TEXT NOT NULL,
                `sessionId` TEXT NOT NULL,
                `exerciseId` TEXT NOT NULL,
                `exerciseOrder` INTEGER NOT NULL,
                `durationMillis` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`sessionId`) REFERENCES `workout_sessions`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`exerciseId`) REFERENCES `exercises`(`id`)
                    ON UPDATE NO ACTION ON DELETE RESTRICT
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_exercise_durations_sessionId` " +
                "ON `exercise_durations` (`sessionId`)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_exercise_durations_exerciseId` " +
                "ON `exercise_durations` (`exerciseId`)",
        )
    }
}

val MIGRATION_17_18 = object : Migration(17, 18) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val hasColumn = db.query("PRAGMA table_info(`workout_sessions`)").use { cursor ->
            var found = false
            while (cursor.moveToNext()) {
                if (cursor.getString(1) == "durationMillis") {
                    found = true
                    break
                }
            }
            found
        }
        if (!hasColumn) {
            db.execSQL(
                "ALTER TABLE `workout_sessions` ADD COLUMN `durationMillis` INTEGER NOT NULL DEFAULT 0",
            )
        }
    }
}

val MIGRATION_16_17 = object : Migration(16, 17) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Drop the legacy single-arm measurement entirely, including its data.
        db.execSQL("DROP TABLE IF EXISTS `body_measurements_new`")
        db.execSQL(
            """
            CREATE TABLE `body_measurements_new` (
                `id` TEXT NOT NULL,
                `recordedEpochDay` INTEGER NOT NULL,
                `heightCm` REAL,
                `weightKg` REAL,
                `bodyFatPercent` REAL,
                `waistCm` REAL,
                `armLeftCm` REAL,
                `armRightCm` REAL,
                `forearmLeftCm` REAL,
                `forearmRightCm` REAL,
                `chestCm` REAL,
                `hipCm` REAL,
                `thighLeftCm` REAL,
                `thighRightCm` REAL,
                `calfLeftCm` REAL,
                `calfRightCm` REAL,
                `createdAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO `body_measurements_new` (
                `id`, `recordedEpochDay`, `heightCm`, `weightKg`, `bodyFatPercent`,
                `waistCm`, `armLeftCm`, `armRightCm`, `forearmLeftCm`, `forearmRightCm`,
                `chestCm`, `hipCm`, `thighLeftCm`, `thighRightCm`, `calfLeftCm`,
                `calfRightCm`, `createdAt`
            )
            SELECT
                `id`, `recordedEpochDay`, `heightCm`, `weightKg`, `bodyFatPercent`,
                `waistCm`, `armLeftCm`, `armRightCm`, `forearmLeftCm`, `forearmRightCm`,
                `chestCm`, `hipCm`, `thighLeftCm`, `thighRightCm`, `calfLeftCm`,
                `calfRightCm`, `createdAt`
            FROM `body_measurements`
            """.trimIndent(),
        )
        db.execSQL("DROP TABLE `body_measurements`")
        db.execSQL("ALTER TABLE `body_measurements_new` RENAME TO `body_measurements`")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_body_measurements_recordedEpochDay` " +
                "ON `body_measurements` (`recordedEpochDay`)",
        )
    }
}

val MIGRATION_15_16 = object : Migration(15, 16) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `photo_wall` (
                `id` TEXT NOT NULL,
                `originalRelativePath` TEXT NOT NULL,
                `editedRelativePath` TEXT,
                `dateEpochDay` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                `note` TEXT NOT NULL,
                `width` INTEGER NOT NULL,
                `height` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_photo_wall_dateEpochDay` ON `photo_wall` (`dateEpochDay`)",
        )
    }
}

val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `day_kinds` (
                `dateEpochDay` INTEGER NOT NULL,
                `kind` TEXT NOT NULL,
                PRIMARY KEY(`dateEpochDay`)
            )
            """.trimIndent(),
        )
    }
}

val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        fun hasColumn(table: String, column: String): Boolean {
            db.query("PRAGMA table_info(`$table`)").use { cursor ->
                while (cursor.moveToNext()) {
                    if (cursor.getString(1) == column) return true
                }
            }
            return false
        }

        if (!hasColumn("exercises", "bodyCategoryId")) {
            db.execSQL("ALTER TABLE `exercises` ADD COLUMN `bodyCategoryId` TEXT")
        }
        if (!hasColumn("exercises", "equipmentCategoryId")) {
            db.execSQL("ALTER TABLE `exercises` ADD COLUMN `equipmentCategoryId` TEXT")
        }
        if (!hasColumn("exercises", "classificationStatus")) {
            db.execSQL(
                "ALTER TABLE `exercises` ADD COLUMN `classificationStatus` TEXT NOT NULL " +
                    "DEFAULT 'UNMIGRATED'",
            )
        }
        if (!hasColumn("exercises", "classificationVersion")) {
            db.execSQL(
                "ALTER TABLE `exercises` ADD COLUMN `classificationVersion` INTEGER NOT NULL DEFAULT 0",
            )
        }
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_exercises_bodyCategoryId` " +
                "ON `exercises` (`bodyCategoryId`)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_exercises_equipmentCategoryId` " +
                "ON `exercises` (`equipmentCategoryId`)",
        )
    }
}

val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        fun hasColumn(table: String, column: String): Boolean {
            db.query("PRAGMA table_info(`$table`)").use { cursor ->
                while (cursor.moveToNext()) {
                    if (cursor.getString(1) == column) return true
                }
            }
            return false
        }

        // Idempotent column additions keep migration fixtures that already
        // contain the v13 columns (created by a newer Room schema) safe.
        if (!hasColumn("exercises", "nameEn")) {
            db.execSQL("ALTER TABLE `exercises` ADD COLUMN `nameEn` TEXT")
        }
        if (!hasColumn("exercises", "source")) {
            db.execSQL("ALTER TABLE `exercises` ADD COLUMN `source` TEXT NOT NULL DEFAULT 'LEGACY'")
        }
        if (!hasColumn("exercises", "sourceId")) {
            db.execSQL("ALTER TABLE `exercises` ADD COLUMN `sourceId` TEXT")
        }
        if (!hasColumn("exercises", "equipmentDetail")) {
            db.execSQL("ALTER TABLE `exercises` ADD COLUMN `equipmentDetail` TEXT")
        }
        if (!hasColumn("exercises", "bodyPartDetail")) {
            db.execSQL("ALTER TABLE `exercises` ADD COLUMN `bodyPartDetail` TEXT")
        }
        db.execSQL("UPDATE `exercises` SET `source` = 'CUSTOM' WHERE `isCustom` = 1")
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_exercises_source_sourceId` " +
                "ON `exercises` (`source`, `sourceId`)",
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `exercise_content` (
                `exerciseId` TEXT NOT NULL,
                `locale` TEXT NOT NULL,
                `displayName` TEXT,
                `instructions` TEXT,
                `instructionStepsJson` TEXT,
                PRIMARY KEY(`exerciseId`, `locale`),
                FOREIGN KEY(`exerciseId`) REFERENCES `exercises`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_exercise_content_exerciseId` " +
                "ON `exercise_content` (`exerciseId`)",
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `exercise_media` (
                `exerciseId` TEXT NOT NULL,
                `mediaId` TEXT,
                `sourceImagePath` TEXT,
                `sourceGifPath` TEXT,
                `localThumbnailPath` TEXT,
                `localGifPath` TEXT,
                `remoteGifUrl` TEXT,
                `width` INTEGER NOT NULL,
                `height` INTEGER NOT NULL,
                `attribution` TEXT,
                `thumbnailSha256` TEXT,
                `gifSha256` TEXT,
                `thumbnailBytes` INTEGER,
                `gifBytes` INTEGER,
                `downloadState` TEXT NOT NULL,
                PRIMARY KEY(`exerciseId`),
                FOREIGN KEY(`exerciseId`) REFERENCES `exercises`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_exercise_media_exerciseId` " +
                "ON `exercise_media` (`exerciseId`)",
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `exercise_source_map` (
                `source` TEXT NOT NULL,
                `sourceId` TEXT NOT NULL,
                `localExerciseId` TEXT NOT NULL,
                `matchType` TEXT NOT NULL,
                `confidence` REAL NOT NULL,
                PRIMARY KEY(`source`, `sourceId`),
                FOREIGN KEY(`localExerciseId`) REFERENCES `exercises`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_exercise_source_map_localExerciseId` " +
                "ON `exercise_source_map` (`localExerciseId`)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_exercise_source_map_source_localExerciseId` " +
                "ON `exercise_source_map` (`source`, `localExerciseId`)",
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `exercise_muscle_tokens` (
                `exerciseId` TEXT NOT NULL,
                `bucket` TEXT NOT NULL,
                `token` TEXT NOT NULL,
                `sortOrder` INTEGER NOT NULL,
                `mappingStatus` TEXT NOT NULL,
                `candidateMuscleIds` TEXT,
                PRIMARY KEY(`exerciseId`, `bucket`, `token`),
                FOREIGN KEY(`exerciseId`) REFERENCES `exercises`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_exercise_muscle_tokens_exerciseId` " +
                "ON `exercise_muscle_tokens` (`exerciseId`)",
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `exercise_preset_match_candidate` (
                `presetExerciseId` TEXT NOT NULL,
                `nameZh` TEXT NOT NULL,
                `datasetId` TEXT,
                `candidateDatasetIds` TEXT NOT NULL,
                `status` TEXT NOT NULL,
                `evidence` TEXT,
                PRIMARY KEY(`presetExerciseId`)
            )
            """.trimIndent(),
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `exercise_import_state` (
                `source` TEXT NOT NULL,
                `catalogVersion` INTEGER NOT NULL,
                `importedAt` INTEGER NOT NULL,
                `recordCount` INTEGER NOT NULL,
                `added` INTEGER NOT NULL,
                `linked` INTEGER NOT NULL,
                `skipped` INTEGER NOT NULL,
                `pendingMatches` INTEGER NOT NULL,
                `unmappedTokens` INTEGER NOT NULL,
                `failed` INTEGER NOT NULL,
                PRIMARY KEY(`source`)
            )
            """.trimIndent(),
        )
    }
}

val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        listOf(
            "armLeftCm",
            "armRightCm",
            "forearmLeftCm",
            "forearmRightCm",
            "chestCm",
            "hipCm",
            "thighLeftCm",
            "thighRightCm",
            "calfLeftCm",
            "calfRightCm",
        ).forEach { column ->
            db.execSQL("ALTER TABLE `body_measurements` ADD COLUMN `$column` REAL")
        }
    }
}

val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `body_profile` ADD COLUMN `displayName` TEXT")
        db.execSQL("ALTER TABLE `body_profile` ADD COLUMN `avatarFileName` TEXT")
    }
}

val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `timer_records` (
                `id` TEXT NOT NULL,
                `durationMillis` INTEGER NOT NULL,
                `dateEpochDay` INTEGER NOT NULL,
                `sessionId` TEXT,
                `createdAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `timer_laps` (
                `id` TEXT NOT NULL,
                `recordId` TEXT NOT NULL,
                `lapIndex` INTEGER NOT NULL,
                `intervalMillis` INTEGER NOT NULL,
                `elapsedMillis` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`recordId`) REFERENCES `timer_records`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_timer_laps_recordId` ON `timer_laps` (`recordId`)",
        )
    }
}

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `body_profile_new` (
                `id` INTEGER NOT NULL,
                `birthdayEpochDay` INTEGER,
                `gender` TEXT,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO `body_profile_new` (`id`, `birthdayEpochDay`, `gender`)
            SELECT `id`, `birthdayEpochDay`, NULL FROM `body_profile`
            """.trimIndent(),
        )
        db.execSQL("DROP TABLE `body_profile`")
        db.execSQL("ALTER TABLE `body_profile_new` RENAME TO `body_profile`")
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `exercise_muscles` (
                `exerciseId` TEXT NOT NULL,
                `muscleId` TEXT NOT NULL,
                `role` TEXT NOT NULL,
                `sortOrder` INTEGER NOT NULL,
                PRIMARY KEY(`exerciseId`, `muscleId`),
                FOREIGN KEY(`exerciseId`) REFERENCES `exercises`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_exercise_muscles_exerciseId` ON `exercise_muscles` (`exerciseId`)",
        )
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `day_notes` (
                `dateEpochDay` INTEGER NOT NULL,
                `message` TEXT NOT NULL,
                PRIMARY KEY(`dateEpochDay`)
            )
            """.trimIndent(),
        )
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE body_measurements ADD COLUMN waistCm REAL")
        db.execSQL("ALTER TABLE body_measurements ADD COLUMN armCm REAL")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE exercises ADD COLUMN archived INTEGER NOT NULL DEFAULT 0",
        )
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE exercises ADD COLUMN workingPrWeightKg REAL")
        db.execSQL("ALTER TABLE exercises ADD COLUMN workingPrReps INTEGER")
        db.execSQL("ALTER TABLE exercises ADD COLUMN workingPrWeightMode TEXT")
        db.execSQL("ALTER TABLE exercises ADD COLUMN workingPrDateEpochDay INTEGER")
        db.execSQL("ALTER TABLE exercises ADD COLUMN maxPrWeightKg REAL")
        db.execSQL("ALTER TABLE exercises ADD COLUMN maxPrReps INTEGER")
        db.execSQL("ALTER TABLE exercises ADD COLUMN maxPrWeightMode TEXT")
        db.execSQL("ALTER TABLE exercises ADD COLUMN maxPrDateEpochDay INTEGER")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `body_profile` (
                `id` INTEGER NOT NULL,
                `birthdayEpochDay` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `body_measurements` (
                `id` TEXT NOT NULL,
                `recordedEpochDay` INTEGER NOT NULL,
                `heightCm` REAL,
                `weightKg` REAL,
                `bodyFatPercent` REAL,
                `createdAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_body_measurements_recordedEpochDay` ON `body_measurements` (`recordedEpochDay`)",
        )
    }
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE workout_sets ADD COLUMN weightMode TEXT NOT NULL DEFAULT 'WEIGHTED'",
        )
        db.execSQL(
            "ALTER TABLE workout_sets ADD COLUMN exerciseOrder INTEGER NOT NULL DEFAULT 0",
        )

        val records = db.query(
            "SELECT id, dateEpochDay, exerciseName, weightKg, reps, sets, note FROM workout_records",
        )
        val createdAt = System.currentTimeMillis()
        while (records.moveToNext()) {
            val recordId = records.getLong(0)
            val dateEpochDay = records.getLong(1)
            val exerciseName = records.getString(2).orEmpty()
            val weightKg = records.getDouble(3)
            val reps = records.getInt(4)
            val sets = records.getInt(5)
            val note = records.getString(6).orEmpty()

            val existingExerciseId = findExerciseIdByName(db, exerciseName)
            val converted = LegacyRecordMigrator.convert(
                recordId = recordId,
                dateEpochDay = dateEpochDay,
                exerciseName = exerciseName.ifBlank { "未命名动作" },
                weightKg = weightKg,
                reps = reps,
                sets = sets,
                note = note,
                existingExerciseId = existingExerciseId,
                createdAt = createdAt,
            )

            converted.newExercise?.let { exercise ->
                val values = ContentValues().apply {
                    put("id", exercise.id)
                    put("name", exercise.name)
                    put("bodyPart", exercise.bodyPart)
                    put("equipmentType", exercise.equipmentType)
                    put("weightMode", exercise.weightMode)
                    put("isCustom", if (exercise.isCustom) 1 else 0)
                    put("aliases", exercise.aliases)
                    put("createdAt", exercise.createdAt)
                }
                db.insert("exercises", SQLiteDatabase.CONFLICT_IGNORE, values)
            }

            val sessionValues = ContentValues().apply {
                put("id", converted.session.id)
                put("dateEpochDay", converted.session.dateEpochDay)
                put("note", converted.session.note)
                put("createdAt", converted.session.createdAt)
            }
            db.insert("workout_sessions", SQLiteDatabase.CONFLICT_REPLACE, sessionValues)

            db.execSQL(
                "DELETE FROM workout_sets WHERE sessionId = ?",
                arrayOf(converted.session.id),
            )
            converted.sets.forEach { set ->
                val setValues = ContentValues().apply {
                    put("id", set.id)
                    put("sessionId", set.sessionId)
                    put("exerciseId", set.exerciseId)
                    put("exerciseOrder", set.exerciseOrder)
                    put("setIndex", set.setIndex)
                    put("weightMode", set.weightMode)
                    put("weightKg", set.weightKg)
                    put("reps", set.reps)
                    put("note", set.note)
                }
                db.insert("workout_sets", SQLiteDatabase.CONFLICT_REPLACE, setValues)
            }
        }
        records.close()
    }

    private fun findExerciseIdByName(db: SupportSQLiteDatabase, name: String): String? {
        if (name.isBlank()) return null
        val cursor = db.query(
            "SELECT id FROM exercises WHERE name = ? COLLATE NOCASE LIMIT 1",
            arrayOf(name),
        )
        val id = if (cursor.moveToFirst()) cursor.getString(0) else null
        cursor.close()
        return id
    }
}
