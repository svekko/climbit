package com.example.climbit.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.climbit.model.Grade

@Dao
interface GradeDAO {
    @Query("SELECT * FROM grade ORDER BY id")
    fun getAll(): List<Grade>

    @Query("SELECT * FROM grade WHERE id = :id")
    fun get(id: Long): Grade

    @Insert
    fun insert(difficulty: Grade): Long
}
