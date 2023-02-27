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
            difficultyDAO().insert(Difficulty(1, "Beginner", "1ee317"))
            difficultyDAO().insert(Difficulty(2, "Easy", "e3d217"))
            difficultyDAO().insert(Difficulty(3, "Intermediate", "e31717"))
            difficultyDAO().insert(Difficulty(4, "Advanced", "1736e3"))
            difficultyDAO().insert(Difficulty(5, "Expert", "c817e3"))
            difficultyDAO().insert(Difficulty(6, "Elite", "ffffff"))
        }
    }
}
