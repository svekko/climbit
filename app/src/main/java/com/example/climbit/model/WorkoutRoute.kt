package com.example.climbit.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.*

@Entity(
    tableName = "workout_route",
    foreignKeys = [
        ForeignKey(
            entity = Workout::class,
            childColumns = ["workout_id"],
            parentColumns = ["id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Difficulty::class,
            childColumns = ["difficulty_id"],
            parentColumns = ["id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Grade::class,
            childColumns = ["grade_id"],
            parentColumns = ["id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class WorkoutRoute(
    @PrimaryKey(autoGenerate = true)
    val id: Long,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "date_in")
    val dateIn: Date,

    @ColumnInfo(name = "workout_id")
    val workoutID: Long,

    @ColumnInfo(name = "difficulty_id")
    val difficultyID: Long,

    @ColumnInfo(name = "grade_id")
    val gradeID: Long?,
)
