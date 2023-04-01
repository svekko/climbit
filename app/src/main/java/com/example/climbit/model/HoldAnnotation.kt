package com.example.climbit.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "hold_annotation",
)
data class HoldAnnotation(
    @PrimaryKey(autoGenerate = true)
    val id: Long,

    @ColumnInfo(name = "file_name")
    val fileName: String,

    @ColumnInfo(name = "radius")
    val radius: Float,

    @ColumnInfo(name = "stroke_width")
    val strokeWidth: Float,

    @ColumnInfo(name = "x")
    val x: Float,

    @ColumnInfo(name = "y")
    val y: Float,
)
