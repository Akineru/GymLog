package com.example.gymlog

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.gymlog.data.PersonalRecordLogic
import com.example.gymlog.data.TrainingPreferencesStore
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 「做组 PR 最低次数」的持久化与校验。
 *
 * 需求 F：没有历史配置时默认 6；改完重启仍是新值；非法/异常存储值不能崩，也不能进入计算。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TrainingPreferencesStoreTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        prefs().edit().clear().commit()
    }

    @Test
    fun defaultIsSix() {
        val store = TrainingPreferencesStore(context)
        assertEquals(6, store.workingPrMinRepsValue())
        assertEquals(6, store.workingPrMinReps.value)
        assertEquals(PersonalRecordLogic.DEFAULT_MIN_WORKING_REPS, store.workingPrMinRepsValue())
    }

    @Test
    fun settingIsPersistedAcrossStoreInstances() {
        TrainingPreferencesStore(context).setWorkingPrMinReps(4)
        // 模拟进程重启：新实例直接读 SharedPreferences
        val reopened = TrainingPreferencesStore(context)
        assertEquals(4, reopened.workingPrMinRepsValue())
        assertEquals(4, reopened.workingPrMinReps.value)
    }

    @Test
    fun outOfRangeValuesAreClamped() {
        val store = TrainingPreferencesStore(context)
        store.setWorkingPrMinReps(0)
        assertEquals(PersonalRecordLogic.MIN_WORKING_REPS, store.workingPrMinRepsValue())
        store.setWorkingPrMinReps(-5)
        assertEquals(PersonalRecordLogic.MIN_WORKING_REPS, store.workingPrMinRepsValue())
        store.setWorkingPrMinReps(999)
        assertEquals(PersonalRecordLogic.MAX_WORKING_REPS, store.workingPrMinRepsValue())
    }

    @Test
    fun corruptStoredValueFallsBackToDefault() {
        // 手工写入错误类型，读取时 getInt 会抛 ClassCastException，必须被兜住
        @Suppress("ApplySharedPref")
        prefs().edit().putString("working_pr_min_reps", "not-a-number").commit()

        val store = TrainingPreferencesStore(context)
        assertEquals(PersonalRecordLogic.DEFAULT_MIN_WORKING_REPS, store.workingPrMinRepsValue())
    }

    @Test
    fun storedOutOfRangeValueIsNormalizedOnRead() {
        @Suppress("ApplySharedPref")
        prefs().edit().putInt("working_pr_min_reps", 0).commit()

        assertEquals(1, TrainingPreferencesStore(context).workingPrMinRepsValue())
    }

    private fun prefs() =
        context.getSharedPreferences(TrainingPreferencesStore.PREFS_NAME, Context.MODE_PRIVATE)
}
