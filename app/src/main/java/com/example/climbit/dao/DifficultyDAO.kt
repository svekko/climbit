package com.example.climbit.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.climbit.model.Difficulty

@Dao
interface DifficultyDAO {
    @Query("SELECT * FROM difficulty ORDER BY id")
    fun getAll(): List<Difficulty>

    @Query("SELECT * FROM difficulty WHERE id = :id")
    fun get(id: Long): Difficulty

    @Insert
    fun insert(difficulty: Difficulty): Long
}
