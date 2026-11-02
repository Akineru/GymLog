package com.example.gymlog.ui.muscle

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.example.gymlog.GymLogApplication
import com.example.gymlog.data.Gender

/** 人体图只有男女两版；未设置与 OTHER 一律落到 MALE。 */
internal enum class MuscleSex {
    MALE,
    FEMALE,
    ;

    companion object {
        fun from(gender: Gender?): MuscleSex = if (gender == Gender.FEMALE) FEMALE else MALE
    }
}

@Composable
internal fun rememberMuscleSex(): MuscleSex {
    val context = LocalContext.current
    val repo = remember { (context.applicationContext as GymLogApplication).bodyRecordRepository }
    val profile by repo.observeProfile().collectAsStateWithLifecycle(initialValue = null)
    return remember(profile?.gender) { MuscleSex.from(Gender.fromStored(profile?.gender)) }
}
