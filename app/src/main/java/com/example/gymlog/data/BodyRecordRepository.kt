package com.example.gymlog.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import java.time.LocalDate
import java.util.UUID

class BodyRecordRepository(
    private val dao: BodyRecordDao,
) {
    fun observeProfile(): Flow<BodyProfileEntity?> = dao.observeProfile().flowOn(Dispatchers.IO)

    fun observeMeasurements(): Flow<List<BodyMeasurementEntity>> =
        dao.observeMeasurements().flowOn(Dispatchers.IO)

    suspend fun saveIdentity(
        displayName: String?,
        gender: String?,
        avatarFileName: String?,
        clearAvatar: Boolean,
    ) {
        val existing = dao.getProfile()
        val name = displayName?.trim()?.take(NAME_LIMIT)?.ifEmpty { null }
        val avatar = when {
            clearAvatar -> null
            avatarFileName != null -> avatarFileName
            else -> existing?.avatarFileName
        }
        if (existing?.birthdayEpochDay == null && gender == null && name == null && avatar == null) {
            dao.clearProfile()
            return
        }
        dao.upsertProfile(
            BodyProfileEntity(
                birthdayEpochDay = existing?.birthdayEpochDay,
                gender = gender,
                displayName = name,
                avatarFileName = avatar,
            ),
        )
    }

    suspend fun saveSnapshot(
        birthdayEpochDay: Long?,
        gender: String?,
        heightCm: Double?,
        weightKg: Double?,
        bodyFatPercent: Double?,
        waistCm: Double?,
        armCm: Double?,
        recordMeasurement: Boolean,
    ): Result<Unit> {
        val existing = dao.getProfile()
        val name = existing?.displayName
        val avatar = existing?.avatarFileName
        if (birthdayEpochDay != null || gender != null || !name.isNullOrBlank() || avatar != null) {
            dao.upsertProfile(
                BodyProfileEntity(
                    birthdayEpochDay = birthdayEpochDay,
                    gender = gender,
                    displayName = name,
                    avatarFileName = avatar,
                ),
            )
        } else {
            dao.clearProfile()
        }
        if (recordMeasurement) {
            val latest = latestMeasurement()
            dao.insertMeasurement(
                measurementFrom(
                    heightCm = heightCm,
                    weightKg = weightKg,
                    bodyFatPercent = bodyFatPercent,
                    waistCm = waistCm,
                    armCm = armCm,
                    girths = latest?.girths()?.copy(waistCm = waistCm) ?: BodyGirths(waistCm = waistCm),
                ),
            )
        }
        return Result.success(Unit)
    }

    suspend fun saveGirths(girths: BodyGirths) {
        val latest = latestMeasurement()
        if (!girths.changedFrom(latest)) return
        dao.insertMeasurement(
            measurementFrom(
                heightCm = latest?.heightCm,
                weightKg = latest?.weightKg,
                bodyFatPercent = latest?.bodyFatPercent,
                waistCm = girths.waistCm,
                armCm = latest?.armCm,
                girths = girths,
            ),
        )
    }

    private suspend fun latestMeasurement(): BodyMeasurementEntity? {
        return dao.listMeasurements().maxWithOrNull(
            compareBy<BodyMeasurementEntity> { it.recordedEpochDay }.thenBy { it.createdAt },
        )
    }

    private fun measurementFrom(
        heightCm: Double?,
        weightKg: Double?,
        bodyFatPercent: Double?,
        waistCm: Double?,
        armCm: Double?,
        girths: BodyGirths,
    ): BodyMeasurementEntity {
        return BodyMeasurementEntity(
            id = UUID.randomUUID().toString(),
            recordedEpochDay = LocalDate.now().toEpochDay(),
            heightCm = heightCm,
            weightKg = weightKg,
            bodyFatPercent = bodyFatPercent,
            waistCm = waistCm,
            armCm = armCm,
            armLeftCm = girths.armLeftCm,
            armRightCm = girths.armRightCm,
            forearmLeftCm = girths.forearmLeftCm,
            forearmRightCm = girths.forearmRightCm,
            chestCm = girths.chestCm,
            hipCm = girths.hipCm,
            thighLeftCm = girths.thighLeftCm,
            thighRightCm = girths.thighRightCm,
            calfLeftCm = girths.calfLeftCm,
            calfRightCm = girths.calfRightCm,
            createdAt = System.currentTimeMillis(),
        )
    }

    private companion object {
        const val NAME_LIMIT = 24
    }
}
