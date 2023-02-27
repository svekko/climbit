package com.example.climbit.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.*

@Entity(
    tableName = "workout_set",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutRoute::class,
            childColumns = ["workout_route_id"],
            parentColumns = ["id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class WorkoutSet(
    @PrimaryKey(autoGenerate = true)
    val id: Long,

    @ColumnInfo(name = "date_in")
    val dateIn: Date,

    @ColumnInfo(name = "workout_route_id")
    val workoutRouteID: Long,

    @ColumnInfo(name = "finished")
    val finished: Boolean,
)
