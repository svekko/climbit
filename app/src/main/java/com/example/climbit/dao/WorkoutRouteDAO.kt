package com.example.climbit.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.example.climbit.model.WorkoutRoute
import com.example.climbit.model.WorkoutRouteWithSets

@Dao
interface WorkoutRouteDAO {
    @Transaction
    @Query("SELECT * FROM workout_route WHERE workout_id = :workoutID ORDER BY date_in DESC")
    fun getRoutesAndSets(workoutID: Long): List<WorkoutRouteWithSets>

    @Query("SELECT * FROM workout_route WHERE id = :id")
    fun get(id: Long): WorkoutRoute

    @Insert
    fun insert(workoutRoute: WorkoutRoute): Long

    @Query("DELETE FROM workout_route WHERE id = :id")
    fun delete(id: Long)
}
