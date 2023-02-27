package com.example.climbit.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(
    tableName = "workout",
)
data class Workout(
    @PrimaryKey(autoGenerate = true)
    val id: Long,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "date_started")
    val dateStarted: Date,

    @ColumnInfo(name = "date_finished")
    var dateFinished: Date?,
)
