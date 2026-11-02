package com.example.gymlog.data

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * 自动本地备份：把 [BackupRepository.exportSnapshot] 的 JSON 快照写到
 * files/auto_backups/，保留最近 [KEEP] 份滚动覆盖。
 * 冷启动节流触发（距上次自动备份不足 [MIN_INTERVAL_MS] 则跳过），
 * 也可从数据管理页手动立即备份/恢复。
 */
class AutoBackupManager(
    private val context: Context,
    private val backupRepository: BackupRepository,
) {
    data class AutoBackupEntry(
        val file: File,
        val exportedAt: Long,
        val sizeBytes: Long,
        val sessionCount: Int,
    )

    fun backupDir(): File = File(context.filesDir, DIR).apply { mkdirs() }

    /** 冷启动自动备份：距上次超过 6 小时才执行，失败静默记日志。 */
    suspend fun runAutoBackup() = withContext(Dispatchers.IO) {
        runCatching {
            val list = list()
            val last = list.maxOfOrNull { it.exportedAt } ?: 0L
            if (System.currentTimeMillis() - last < MIN_INTERVAL_MS) return@runCatching
            createBackup(auto = true)
        }.onFailure { error -> Log.w(TAG, "auto backup skipped/failed", error) }
    }

    /** 立即备份（无视节流），成功返回条目。 */
    suspend fun backupNow(): AutoBackupEntry = withContext(Dispatchers.IO) {
        createBackup(auto = false)
    }

    fun list(): List<AutoBackupEntry> {
        val files = backupDir().listFiles { f -> f.name.endsWith(".json") } ?: return emptyList()
        return files.mapNotNull { file ->
            runCatching {
                // 只读文件头提取 exportedAt，避免整读几 MB 的 JSON
                val head = file.inputStream().use { input ->
                    val buf = ByteArray(2048)
                    val read = input.read(buf)
                    if (read <= 0) "" else String(buf, 0, read, Charsets.UTF_8)
                }
                val match = EXPORTED_AT_REGEX.find(head)
                AutoBackupEntry(
                    file = file,
                    exportedAt = match?.groupValues?.get(1)?.toLongOrNull() ?: file.lastModified(),
                    sizeBytes = file.length(),
                    sessionCount = -1,
                )
            }.getOrNull()
        }.sortedByDescending { it.exportedAt }
    }

    /** 从自动备份恢复：走 importSnapshot（自带回滚与协调锁）。 */
    suspend fun restore(entry: AutoBackupEntry) = withContext(Dispatchers.IO) {
        val text = entry.file.readText()
        val snapshot = BackupCodec.decode(text)
        backupRepository.importSnapshot(snapshot)
    }

    private suspend fun createBackup(auto: Boolean): AutoBackupEntry {
        val text = BackupCodec.encode(
            snapshot = backupRepository.exportSnapshot(),
            exportedAt = System.currentTimeMillis(),
        )
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(backupDir(), "gymlog_${if (auto) "auto" else "manual"}_$stamp.json")
        file.writeText(text)
        // 滚动淘汰：只保留最近 KEEP 份
        list().drop(KEEP).forEach { stale -> stale.file.delete() }
        Log.i(TAG, "backup written: ${file.name} (${file.length()} bytes)")
        return AutoBackupEntry(file, System.currentTimeMillis(), file.length(), -1)
    }

    companion object {
        private const val TAG = "AutoBackup"
        private const val DIR = "auto_backups"
        private const val KEEP = 3
        private const val MIN_INTERVAL_MS = 6 * 60 * 60 * 1000L
        private val EXPORTED_AT_REGEX = Regex("\"exportedAt\"\\s*:\\s*(\\d+)")
    }
}
