package com.example.gymlog.data

import android.content.Context
import android.provider.Settings
import org.json.JSONArray
import org.json.JSONObject

class TrainingTimerStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun currentBootCount(): Int {
        return runCatching {
            Settings.Global.getInt(appContext.contentResolver, Settings.Global.BOOT_COUNT)
        }.getOrDefault(-1)
    }

    fun savedBootCount(): Int {
        val raw = prefs.getString(KEY_STATE, null) ?: return -1
        return runCatching { JSONObject(raw).optInt("bootCount", -1) }.getOrDefault(-1)
    }

    fun read(): TimerState {
        val raw = prefs.getString(KEY_STATE, null) ?: return TimerState()
        return runCatching { decode(raw) }.getOrDefault(TimerState())
    }

    fun write(state: TimerState) {
        prefs.edit().putString(KEY_STATE, encode(state)).commit()
    }

    private fun encode(state: TimerState): String {
        val root = JSONObject()
        root.put("phase", state.phase.name)
        root.put("accumulatedMillis", state.accumulatedMillis)
        root.put("bootCount", currentBootCount())
        root.put("lapBaselineMillis", state.lapBaselineMillis)
        if (state.segmentStartedAtElapsed != null) {
            root.put("segmentStartedAtElapsed", state.segmentStartedAtElapsed)
        }
        val laps = JSONArray()
        state.laps.forEach { lap ->
            laps.put(
                JSONObject()
                    .put("index", lap.index)
                    .put("intervalMillis", lap.intervalMillis)
                    .put("elapsedMillis", lap.elapsedMillis),
            )
        }
        root.put("laps", laps)
        if (state.recordDateEpochDay != null) {
            root.put("recordDateEpochDay", state.recordDateEpochDay)
        }
        return root.toString()
    }

    private fun decode(raw: String): TimerState {
        val root = JSONObject(raw)
        val phase = TimerPhase.entries.find { it.name == root.optString("phase") } ?: TimerPhase.IDLE
        val laps = ArrayList<TimerLap>()
        val array = root.optJSONArray("laps") ?: JSONArray()
        for (index in 0 until array.length()) {
            val item = array.getJSONObject(index)
            laps += TimerLap(
                index = item.getInt("index"),
                intervalMillis = item.getLong("intervalMillis"),
                elapsedMillis = item.getLong("elapsedMillis"),
            )
        }
        return TimerState(
            phase = phase,
            accumulatedMillis = root.optLong("accumulatedMillis").coerceAtLeast(0),
            segmentStartedAtElapsed = if (root.has("segmentStartedAtElapsed") && !root.isNull("segmentStartedAtElapsed")) {
                root.getLong("segmentStartedAtElapsed")
            } else {
                null
            },
            laps = laps,
            lapBaselineMillis = root.optLong("lapBaselineMillis").coerceAtLeast(0),
            recordDateEpochDay = if (root.has("recordDateEpochDay") && !root.isNull("recordDateEpochDay")) {
                root.getLong("recordDateEpochDay")
            } else {
                null
            },
        )
    }

    companion object {
        private const val PREFS_NAME = "gymlog_timer"
        private const val KEY_STATE = "state"
    }
}
