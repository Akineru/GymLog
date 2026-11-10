package com.example.gymlog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * 数据集名称的体检：源数据里不该再留「版本 2」这类抓取残留。
 *
 * [com.example.gymlog.data.ExerciseNameCleaner] 也会剥掉它，但源数据本身干净
 * 才不会出现「老库显示旧名字、重装才变」的差异。
 */
class DatasetNameSanityTest {
    private val raw by lazy {
        File("src/main/assets/exercises/dataset_v1.json").readText(Charsets.UTF_8)
    }

    private val records: List<String> by lazy {
        Regex("\"name\": \"([^\"]*)\"").findAll(raw).map { it.groupValues[1] }.toList()
    }

    @Test
    fun datasetHasNoVersionSuffixes() {
        val offenders = records.filter { Regex("版本\\s*\\d").containsMatchIn(it) }
        assertTrue("仍有版本后缀：$offenders", offenders.isEmpty())
    }

    @Test
    fun recordCountStillMatchesDeclaredSource() {
        val declared = Regex("\"sourceRecordCount\": (\\d+)").find(raw)!!.groupValues[1].toInt()
        assertEquals(declared, records.size)
    }
}
