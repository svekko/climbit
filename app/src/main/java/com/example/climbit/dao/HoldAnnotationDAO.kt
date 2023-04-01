package com.example.climbit.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.climbit.model.HoldAnnotation

@Dao
interface HoldAnnotationDAO {
    @Query("SELECT * FROM hold_annotation WHERE file_name = :fileName")
    fun getAll(fileName: String): List<HoldAnnotation>

    @Insert
    fun insert(ann: HoldAnnotation)

    @Query("DELETE FROM hold_annotation WHERE id = :id")
    fun delete(id: Long)
}
