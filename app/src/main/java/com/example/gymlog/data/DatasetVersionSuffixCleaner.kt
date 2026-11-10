package com.example.gymlog.data

import androidx.room.withTransaction

/**
 * 清掉数据集里抓取残留的「版本 2」后缀。
 *
 * 源数据 `dataset_v1.json` 已经改干净了，但已入库的行不会跟着变（导入时同一 id 直接跳过），
 * 老用户只能重装才看得到。所以每次导入结束后按名字扫一遍：只动名字里带「版本 + 数字」的行，
 * 把后缀删掉，其余 1200 多条一个字都不碰。
 *
 * [ExerciseNameCleaner] 也有一条「版本」规则，但它同时会删器械词；这里只负责删后缀，
 * 显示名再交给 cleaner 按同一口径算一次，保证动作库和详情页一致。
 *
 * 幂等：名字里没有版本号的行不会被读到。
 */
class DatasetVersionSuffixCleaner(
    private val database: GymLogDatabase,
) {
    suspend fun cleanAll() {
        val dao = database.exerciseDao()
        val pending = dao.allExercises()
            .filter { it.source == ExerciseSource.DATASET && VERSION_SUFFIX.containsMatchIn(it.name) }
            .mapNotNull { row ->
                val name = row.name.replace(VERSION_SUFFIX, "")
                val original = ExerciseNameCleaner.clean(name)
                if (row.name == name && row.originalName == original) {
                    null
                } else {
                    Triple(row.id, name, original)
                }
            }
        if (pending.isEmpty()) return
        database.withTransaction {
            pending.forEach { (id, name, original) -> dao.updateName(id, name, original) }
        }
    }

    private companion object {
        /** 「版本 2」「版本2」「版本 3」；数字是必需项，避免误伤正常名字里的「版本」二字。 */
        val VERSION_SUFFIX = Regex("\\s*版本\\s*\\d+")
    }
}
