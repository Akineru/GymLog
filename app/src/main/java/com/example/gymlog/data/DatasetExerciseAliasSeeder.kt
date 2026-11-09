package com.example.gymlog.data

import androidx.room.withTransaction

/**
 * 给数据集动作补上「本地叫法」别名（规则见 [DatasetExerciseAliases]）。
 *
 * 只填 **别名还是空的** 数据集行：
 * - 用户自己写过别名的行一律不动；
 * - 预置动作和自建动作不碰；
 * - 重复执行是空操作，所以每次导入/启动都可以调，旧库和从旧备份恢复的库都能补上。
 *
 * 已知取舍：用户把别名**删空**后，下次启动会被重新补回规则别名。
 * 要彻底清掉需要另加一列标记，为这点收益不值当。
 */
class DatasetExerciseAliasSeeder(
    private val database: GymLogDatabase,
) {
    suspend fun seedAll() {
        val dao = database.exerciseDao()
        val pending = dao.allExercises().filter { row ->
            row.source == ExerciseSource.DATASET && row.aliases.isNullOrBlank()
        }
        if (pending.isEmpty()) return
        database.withTransaction {
            pending.forEach { row ->
                val joined = ExerciseAliases.join(
                    DatasetExerciseAliases.aliasesFor(row.name, row.equipmentDetail),
                )
                if (joined != null) dao.fillAliasesIfBlank(row.id, joined)
            }
        }
    }
}
