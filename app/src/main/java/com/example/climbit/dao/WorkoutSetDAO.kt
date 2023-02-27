package com.example.climbit.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.climbit.model.WorkoutSet

@Dao
interface WorkoutSetDAO {
    @Query("SELECT * FROM workout_set WHERE workout_route_id = :routeID ORDER BY date_in DESC")
    fun getAll(routeID: Long): List<WorkoutSet>

    @Query("SELECT * FROM workout_set WHERE id = :id")
    fun get(id: Long): WorkoutSet

    @Insert
    fun insert(set: WorkoutSet): Long

    @Query("DELETE FROM workout_set WHERE id = :id")
    fun delete(id: Long)
}
