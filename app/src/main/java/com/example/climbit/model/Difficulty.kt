package com.example.climbit.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Difficulty(
    @PrimaryKey()
    val id: Long,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "hex_color")
    val hexColor: String,
) {
    override fun toString(): String {
        return name
    }
}
