package com.example.climbit.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.climbit.model.WorkoutSet

@Dao
interface WorkoutSetDAO {
    @Query("SELECT * FROM workout_set WHERE workout_route_id = :routeID ORDER BY date_in DESC")
    fun getAll(routeID: Long): List<WorkoutSet>

    @Query(
        "SELECT workout_set.* FROM workout_set " +
                "JOIN workout_route " +
                "ON workout_route.id = workout_set.workout_route_id " +
                "AND workout_route.workout_id = :workoutID " +
                "ORDER BY workout_set.date_in DESC LIMIT 1"
    )
    fun getLastForWorkout(workoutID: Long): WorkoutSet?

    @Query("SELECT * FROM workout_set WHERE id = :id")
    fun get(id: Long): WorkoutSet

    @Insert
    fun insert(set: WorkoutSet): Long

    @Query("DELETE FROM workout_set WHERE id = :id")
    fun delete(id: Long)
}
