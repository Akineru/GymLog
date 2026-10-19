package com.example.gymlog.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "body_profile")
data class BodyProfileEntity(
    @PrimaryKey val id: Int = PROFILE_ID,
    val birthdayEpochDay: Long? = null,
    val gender: String? = null,
    val displayName: String? = null,
    val avatarFileName: String? = null,
) {
    companion object {
        const val PROFILE_ID = 1
    }
}
