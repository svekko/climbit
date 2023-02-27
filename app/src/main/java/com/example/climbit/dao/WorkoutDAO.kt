package com.example.climbit.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.climbit.model.Workout

@Dao
interface WorkoutDAO {
    @Query("SELECT * FROM workout ORDER BY date_started DESC LIMIT 20 OFFSET (20 * (:page - 1))")
    fun getAll(page: Int): List<Workout>

    @Query("SELECT * FROM workout WHERE id = :id")
    fun get(id: Long): Workout

    @Insert
    fun insert(workout: Workout): Long

    @Update
    fun update(workout: Workout)

    @Query("DELETE FROM workout WHERE id = :id")
    fun delete(id: Long)
}
