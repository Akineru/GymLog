package com.example.gymlog.data

import android.content.Context
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Imports the structured exercise dataset into the local catalog.
 *
 * This deliberately does not copy media files. It registers media paths,
 * checksums and the NOT_BUNDLED state so a later media phase can decide what to
 * package or download. It never links a dataset row to a legacy preset: all
 * preset candidates stay in exercise_preset_match_candidate for human review.
 */
open class ExerciseCatalogImporter(
    private val context: Context,
    private val database: GymLogDatabase,
    internal val coordinator: ExerciseCatalogCoordinator = ExerciseCatalogCoordinator(),
    private val seedJsonOverride: String? = null,
) {
    data class ImportStats(
        val source: String,
        val catalogVersion: Int,
        val added: Int,
        val linked: Int,
        val skipped: Int,
        val pendingMatches: Int,
        val unmappedTokens: Int,
        val failed: Int,
        val alreadyImported: Boolean = false,
    )

    open suspend fun importIfNeeded(force: Boolean = false): ImportStats =
        coordinator.withLock { importIfNeededLocked(force) }

    /**
     * Catalog import that MUST be called only while the caller already holds
     * [coordinator]'s lock (for example from BackupRepository). It never
     * acquires the same non-reentrant mutex again.
     */
    open suspend fun importIfNeededLocked(force: Boolean): ImportStats {
        val catalogDao = database.exerciseCatalogDao()
        val previous = catalogDao.importState(ExerciseSource.DATASET)
        if (!force && previous != null &&
            previous.failed == 0 &&
            previous.catalogVersion == ExerciseSource.DATASET_CATALOG_VERSION &&
            catalogDao.sourceMapCount(ExerciseSource.DATASET) >= previous.recordCount &&
            catalogDao.exerciseCountBySource(ExerciseSource.DATASET) >= previous.recordCount
        ) {
            // Rows restored from an older backup may still need classification
            // even when the dataset itself is already complete.
            ExerciseClassificationMigrator(database).migrateAll()
            return previous.toStats(alreadyImported = true)
        }

        var parsedRecordCount = 0
        try {
            val seedJson = seedJsonOverride ?: withContext(Dispatchers.IO) {
                context.assets.open(ExerciseSource.DATASET_ASSET).bufferedReader(Charsets.UTF_8).use { it.readText() }
            }
            val presetJson = withContext(Dispatchers.IO) {
                context.assets.open(ExerciseSource.PRESET_MATCH_ASSET).bufferedReader(Charsets.UTF_8).use { it.readText() }
            }
            val seed = withContext(Dispatchers.Default) { parseSeed(seedJson) }
            parsedRecordCount = seed.records.size
            val presetCandidates = withContext(Dispatchers.Default) { parsePresetCandidates(presetJson) }

            val stats = database.withTransaction {
                val exerciseDao = database.exerciseDao()
                val muscleDao = database.exerciseMuscleDao()
                val catalog = database.exerciseCatalogDao()
                val existingIds = exerciseDao.allIds().toSet()
                val existingMedia = catalog.allMedia().associateBy { it.exerciseId }
                val existingCandidates = catalog.presetCandidates().associateBy { it.presetExerciseId }
                val existingLinks = muscleDao.all().groupBy { it.exerciseId }
                val newExercises = ArrayList<ExerciseEntity>(seed.records.size)
                val contents = ArrayList<ExerciseContentEntity>(seed.records.size * 2)
                val media = ArrayList<ExerciseMediaEntity>(seed.records.size)
                val sourceMaps = ArrayList<ExerciseSourceMapEntity>(seed.records.size)
                val tokens = ArrayList<ExerciseMuscleTokenEntity>()
                val links = ArrayList<MuscleLink>()
                var added = 0
                var skipped = 0
                var unmappedTokens = 0

                seed.records.forEach { record ->
                    val localId = "ds_${record.datasetId}"
                    val existing = existingIds.contains(localId)
                    if (existing) {
                        // Never overwrite a custom row or a row from another source.
                        val current = exerciseDao.getById(localId)
                        if (current != null && current.source != ExerciseSource.DATASET) {
                            skipped++
                            return@forEach
                        }
                        skipped++
                    } else {
                        newExercises += record.toEntity(localId)
                        added++
                    }

                    sourceMaps += ExerciseSourceMapEntity(
                        source = ExerciseSource.DATASET,
                        sourceId = record.datasetId,
                        localExerciseId = localId,
                        matchType = "DATASET_ONLY",
                        confidence = 1.0,
                    )
                    contents += ExerciseContentEntity(
                        exerciseId = localId,
                        locale = "zh",
                        displayName = record.name,
                        instructions = record.instructionsZh,
                        instructionStepsJson = record.stepsZh,
                    )
                    contents += ExerciseContentEntity(
                        exerciseId = localId,
                        locale = "en",
                        displayName = record.nameEn,
                        instructions = record.instructionsEn,
                        instructionStepsJson = record.stepsEn,
                    )
                    val oldMedia = existingMedia[localId]
                    media += ExerciseMediaEntity(
                        exerciseId = localId,
                        mediaId = record.mediaId,
                        sourceImagePath = record.image,
                        sourceGifPath = record.gif,
                        localThumbnailPath = oldMedia?.localThumbnailPath,
                        localGifPath = oldMedia?.localGifPath,
                        remoteGifUrl = oldMedia?.remoteGifUrl,
                        width = 180,
                        height = 180,
                        attribution = record.attribution,
                        thumbnailSha256 = record.imageSha256,
                        gifSha256 = record.gifSha256,
                        thumbnailBytes = record.imageBytes,
                        gifBytes = record.gifBytes,
                        downloadState = oldMedia?.downloadState ?: ExerciseMediaStatus.NOT_BUNDLED,
                    )

                    catalog.deleteMuscleTokens(localId)
                    val tokenRows = ArrayList<ExerciseMuscleTokenEntity>()
                    val roleById = LinkedHashMap<String, MuscleRole>()
                    fun addToken(bucket: String, token: String, order: Int) {
                        if (token.isBlank()) return
                        if (tokenRows.any { it.bucket == bucket && it.token == token }) return
                        val rule = DatasetMuscleMapping.rule(token)
                        tokenRows += ExerciseMuscleTokenEntity(
                            exerciseId = localId,
                            bucket = bucket,
                            token = token,
                            sortOrder = order,
                            mappingStatus = rule.status.name,
                            candidateMuscleIds = rule.muscleIds.takeIf { it.isNotEmpty() }?.joinToString(","),
                        )
                        if (rule.status != DatasetMappingStatus.MAPPED) {
                            unmappedTokens++
                            return
                        }
                        rule.muscleIds.forEach { muscleId ->
                            val existingRole = roleById[muscleId]
                            if (existingRole == null || bucket == "TARGET") {
                                roleById[muscleId] = if (bucket == "TARGET") MuscleRole.PRIMARY else MuscleRole.SECONDARY
                            }
                        }
                    }

                    addToken("TARGET", record.target, 0)
                    addToken("MUSCLE_GROUP", record.muscleGroup, 1)
                    record.secondaryMuscles.forEachIndexed { index, token ->
                        addToken("SECONDARY", token, index + 2)
                    }
                    tokens += tokenRows
                    // Preserve existing dataset muscle links (including a future
                    // confirmed/manual mapping) on a re-import. Only rows without
                    // links are populated from the current automatic rules.
                    val preserveLinks = existing && !existingLinks[localId].isNullOrEmpty()
                    if (!preserveLinks) {
                        muscleDao.deleteForExercise(localId)
                        roleById.entries.forEachIndexed { index, entry ->
                            links += MuscleLink(
                                exerciseId = localId,
                                muscleId = entry.key,
                                role = entry.value,
                                sortOrder = index,
                            )
                        }
                    }
                    catalog.deleteContent(localId)
                    catalog.deleteMedia(localId)
                    catalog.deleteSourceMapForExercise(localId)
                }

                if (newExercises.isNotEmpty()) exerciseDao.insertIgnore(newExercises)
                // Update only catalog-owned fields; PR, archive and custom flags are
                // intentionally not touched for an already-imported row.
                seed.records.forEach { record ->
                    val localId = "ds_${record.datasetId}"
                    if (existingIds.contains(localId)) {
                        exerciseDao.updateDatasetFields(
                            id = localId,
                            name = record.name,
                            nameEn = record.nameEn,
                            bodyPart = record.bodyPart,
                            equipmentType = record.equipmentType,
                            equipmentDetail = record.equipmentDetail,
                            bodyPartDetail = record.bodyPartDetail,
                            weightMode = record.weightMode,
                            source = ExerciseSource.DATASET,
                            sourceId = record.datasetId,
                            bodyCategoryId = record.bodyCategory().id,
                            equipmentCategoryId = record.equipmentCategory().id,
                            classificationStatus = record.classificationStatus(),
                            classificationVersion = ExerciseClassification.VERSION,
                        )
                    }
                }
                if (contents.isNotEmpty()) catalog.insertContents(contents)
                if (media.isNotEmpty()) catalog.insertMedia(media)
                if (sourceMaps.isNotEmpty()) catalog.insertSourceMaps(sourceMaps)
                if (tokens.isNotEmpty()) catalog.insertMuscleTokens(tokens)
                if (links.isNotEmpty()) muscleDao.insertAll(links.map { it.toEntity() })

                // Preserve a human-confirmed candidate status. Only the automatic
                // PENDING/AMBIGUOUS/NO_MATCH states are refreshed from the asset.
                val automaticStatuses = setOf("PENDING_CONFIRMATION", "AMBIGUOUS", "NO_MATCH")
                val mergedCandidates = presetCandidates.map { incoming ->
                    val old = existingCandidates[incoming.presetExerciseId]
                    if (old != null && old.status !in automaticStatuses) old else incoming
                }
                if (mergedCandidates.isNotEmpty()) catalog.insertPresetCandidates(mergedCandidates)

                val pending = mergedCandidates.count {
                    it.status == "PENDING_CONFIRMATION" || it.status == "AMBIGUOUS"
                }
                val state = ExerciseImportStateEntity(
                    source = ExerciseSource.DATASET,
                    catalogVersion = ExerciseSource.DATASET_CATALOG_VERSION,
                    importedAt = System.currentTimeMillis(),
                    recordCount = seed.records.size,
                    added = added,
                    linked = 0,
                    skipped = skipped,
                    pendingMatches = pending,
                    unmappedTokens = unmappedTokens,
                    failed = 0,
                )
                catalog.putImportState(state)
                state.toStats()
            }
            ExerciseClassificationMigrator(database).migrateAll()
            return stats
        } catch (error: Throwable) {
            // The dataset transaction has rolled back. Persist a separate failure
            // marker so the next call retries instead of trusting stale counts.
            runCatching {
                database.withTransaction {
                    catalogDao.putImportState(
                        ExerciseImportStateEntity(
                            source = ExerciseSource.DATASET,
                            catalogVersion = ExerciseSource.DATASET_CATALOG_VERSION,
                            importedAt = System.currentTimeMillis(),
                            recordCount = parsedRecordCount,
                            added = 0,
                            linked = 0,
                            skipped = 0,
                            pendingMatches = 0,
                            unmappedTokens = 0,
                            failed = 1,
                        ),
                    )
                }
            }.onFailure { rollbackError -> error.addSuppressed(rollbackError) }
            throw error
        }
    }

    private fun ExerciseImportStateEntity.toStats(alreadyImported: Boolean = false) = ImportStats(
        source = source,
        catalogVersion = catalogVersion,
        added = added,
        linked = linked,
        skipped = skipped,
        pendingMatches = pendingMatches,
        unmappedTokens = unmappedTokens,
        failed = failed,
        alreadyImported = alreadyImported,
    )

    private data class Seed(
        val records: List<SeedRecord>,
    )

    private data class SeedRecord(
        val datasetId: String,
        val name: String,
        val nameEn: String,
        val bodyPart: String,
        val bodyPartDetail: String,
        val equipmentType: String,
        val equipmentDetail: String,
        val weightMode: String,
        val target: String,
        val muscleGroup: String,
        val secondaryMuscles: List<String>,
        val image: String,
        val gif: String,
        val mediaId: String,
        val attribution: String?,
        val imageBytes: Long?,
        val gifBytes: Long?,
        val imageSha256: String?,
        val gifSha256: String?,
        val instructionsZh: String?,
        val instructionsEn: String?,
        val stepsZh: String,
        val stepsEn: String,
        val createdAtEpochMillis: Long,
    ) {
        fun toEntity(localId: String) = ExerciseEntity(
            id = localId,
            name = name,
            bodyPart = bodyPart,
            equipmentType = equipmentType,
            weightMode = weightMode,
            isCustom = false,
            aliases = null,
            createdAt = createdAtEpochMillis,
            archived = false,
            nameEn = nameEn,
            source = ExerciseSource.DATASET,
            sourceId = datasetId,
            equipmentDetail = equipmentDetail,
            bodyPartDetail = bodyPartDetail,
            bodyCategoryId = bodyCategory().id,
            equipmentCategoryId = equipmentCategory().id,
            classificationStatus = classificationStatus(),
            classificationVersion = ExerciseClassification.VERSION,
        )

        fun classificationStatus(): String {
            val knownBody = ExerciseClassification.bodyForRaw(bodyPartDetail) != null
            val knownEquipment = ExerciseClassification.equipmentForRaw(equipmentDetail) != null
            return if (knownBody && knownEquipment) {
                ExerciseClassification.STATUS_DATASET
            } else {
                ExerciseClassification.STATUS_UNMAPPED
            }
        }

        fun bodyCategory(): ExerciseClassification.BodyCategory =
            ExerciseClassification.bodyForRaw(bodyPartDetail)
                ?: ExerciseClassification.UNKNOWN_BODY

        fun equipmentCategory(): ExerciseClassification.EquipmentCategory =
            ExerciseClassification.equipmentForRaw(equipmentDetail)
                ?: ExerciseClassification.UNKNOWN_EQUIPMENT
    }

    private fun parseSeed(text: String): Seed {
        val root = JSONObject(text)
        val array = root.getJSONArray("records")
        val records = ArrayList<SeedRecord>(array.length())
        for (index in 0 until array.length()) {
            val row = array.getJSONObject(index)
            records += SeedRecord(
                datasetId = row.getString("datasetId"),
                name = row.getString("name"),
                nameEn = row.getString("nameEn"),
                bodyPart = row.getString("bodyPart"),
                bodyPartDetail = row.getString("bodyPartDetail"),
                equipmentType = row.getString("equipmentType"),
                equipmentDetail = row.getString("equipmentDetail"),
                weightMode = row.getString("weightMode"),
                target = row.getString("target"),
                muscleGroup = row.getString("muscleGroup"),
                secondaryMuscles = row.getJSONArray("secondaryMuscles").strings(),
                image = row.getString("image"),
                gif = row.getString("gif"),
                mediaId = row.getString("mediaId"),
                attribution = row.optNullableString("attribution"),
                imageBytes = row.optLongOrNull("imageBytes"),
                gifBytes = row.optLongOrNull("gifBytes"),
                imageSha256 = row.optNullableString("imageSha256"),
                gifSha256 = row.optNullableString("gifSha256"),
                instructionsZh = row.optNullableString("instructionsZh"),
                instructionsEn = row.optNullableString("instructionsEn"),
                stepsZh = row.optJSONArray("instructionStepsZh")?.toString() ?: "[]",
                stepsEn = row.optJSONArray("instructionStepsEn")?.toString() ?: "[]",
                createdAtEpochMillis = row.optLong("createdAtEpochMillis"),
            )
        }
        return Seed(records)
    }

    private fun parsePresetCandidates(text: String): List<ExercisePresetMatchCandidateEntity> {
        val rows = JSONObject(text).getJSONArray("candidates")
        return (0 until rows.length()).map { index ->
            val row = rows.getJSONObject(index)
            ExercisePresetMatchCandidateEntity(
                presetExerciseId = row.getString("presetExerciseId"),
                nameZh = row.getString("nameZh"),
                datasetId = row.optNullableString("datasetId"),
                candidateDatasetIds = row.optJSONArray("candidateDatasetIds")?.strings()?.joinToString(",") ?: "",
                status = row.getString("status"),
                evidence = row.opt("evidence")?.toString(),
            )
        }
    }

    private fun JSONArray.strings(): List<String> = (0 until length()).map { getString(it) }

    private fun JSONObject.optNullableString(key: String): String? {
        if (!has(key) || isNull(key)) return null
        return getString(key).takeIf { it.isNotBlank() }
    }

    private fun JSONObject.optLongOrNull(key: String): Long? {
        if (!has(key) || isNull(key)) return null
        return getLong(key)
    }
}
