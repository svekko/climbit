package com.example.climbit.model

data class WorkoutRouteBundle(
    val workout: Workout,
    val route: WorkoutRoute,
    var lastSet: WorkoutSet?,
    val difficulty: Difficulty,
    val grade: Grade?,
    val sets: List<WorkoutSet>,
)
