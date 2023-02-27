package com.example.climbit.model

import androidx.room.Embedded
import androidx.room.Relation

data class WorkoutRouteWithSets(
    @Embedded
    val workoutRoute: WorkoutRoute,

    @Relation(
        parentColumn = "id",
        entityColumn = "workout_route_id",
    )
    val sets: List<WorkoutSet>,

    @Relation(
        parentColumn = "difficulty_id",
        entityColumn = "id"
    )
    val difficulty: Difficulty,
)
