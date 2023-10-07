package com.example.climbit.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "settings",
)
data class Settings(
    @PrimaryKey(autoGenerate = true)
    val id: Long,

    @ColumnInfo(name = "profile_image_base64")
    var profileImageBase64: String?
)
