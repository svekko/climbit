package com.example.climbit.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.climbit.model.Settings

@Dao
interface SettingsDAO {
    @Update
    fun update(v: Settings)

    @Query("SELECT * FROM settings")
    fun select(): Settings

    @Query("SELECT EXISTS(SELECT 1 FROM settings)")
    fun exists(): Boolean

    @Insert
    fun insert(v: Settings)
}
