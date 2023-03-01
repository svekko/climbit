package com.example.climbit.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.climbit.dao.DifficultyDAO
import com.example.climbit.dao.WorkoutDAO
import com.example.climbit.dao.WorkoutRouteDAO
import com.example.climbit.dao.WorkoutSetDAO
import com.example.climbit.model.Difficulty
import com.example.climbit.model.Workout
import com.example.climbit.model.WorkoutRoute
import com.example.climbit.model.WorkoutSet

@Database(
    entities = [
        Workout::class,
        WorkoutRoute::class,
        WorkoutSet::class,
        Difficulty::class,
    ],
    version = 1,
)
@TypeConverters(Converters::class)
abstract class Database : RoomDatabase() {
    abstract fun workoutDAO(): WorkoutDAO
    abstract fun workoutRouteDAO(): WorkoutRouteDAO
    abstract fun workoutSetDAO(): WorkoutSetDAO
    abstract fun difficultyDAO(): DifficultyDAO

    fun afterBuild() {
        if (difficultyDAO().getAll().isEmpty()) {
            difficultyDAO().insert(Difficulty(1, "Beginner", "5ddf77"))
            difficultyDAO().insert(Difficulty(2, "Easy", "f0e570"))
            difficultyDAO().insert(Difficulty(3, "Intermediate", "e66b6b"))
            difficultyDAO().insert(Difficulty(4, "Advanced", "6baae6"))
            difficultyDAO().insert(Difficulty(5, "Expert", "dd8dec"))
            difficultyDAO().insert(Difficulty(6, "Elite", "a3a3a3"))
        }
    }
}
