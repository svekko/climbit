package com.example.climbit.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.climbit.dao.*
import com.example.climbit.model.*

@Database(
    entities = [
        Workout::class,
        WorkoutRoute::class,
        WorkoutSet::class,
        Difficulty::class,
        Grade::class,
        HoldAnnotation::class,
    ],
    version = 3,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
    ]
)
@TypeConverters(Converters::class)
abstract class Database : RoomDatabase() {
    companion object {
        const val DIFF_BEGINNER: Long = 1
        const val DIFF_EASY: Long = 2
        const val DIFF_INTERMEDIATE: Long = 3
        const val DIFF_ADVANCED: Long = 4
        const val DIFF_EXPERT: Long = 5
        const val DIFF_ELITE: Long = 6
    }

    abstract fun workoutDAO(): WorkoutDAO
    abstract fun workoutRouteDAO(): WorkoutRouteDAO
    abstract fun workoutSetDAO(): WorkoutSetDAO
    abstract fun difficultyDAO(): DifficultyDAO
    abstract fun gradeDAO(): GradeDAO
    abstract fun holdAnnotationDAO(): HoldAnnotationDAO

    fun afterBuild() {
        if (difficultyDAO().getAll().isEmpty()) {
            difficultyDAO().insert(Difficulty(DIFF_BEGINNER, "Beginner", "5ddf77"))
            difficultyDAO().insert(Difficulty(DIFF_EASY, "Easy", "f0e570"))
            difficultyDAO().insert(Difficulty(DIFF_INTERMEDIATE, "Intermediate", "e66b6b"))
            difficultyDAO().insert(Difficulty(DIFF_ADVANCED, "Advanced", "6baae6"))
            difficultyDAO().insert(Difficulty(DIFF_EXPERT, "Expert", "dd8dec"))
            difficultyDAO().insert(Difficulty(DIFF_ELITE, "Elite", "a3a3a3"))
        }

        if (gradeDAO().getAll().isEmpty()) {
            gradeDAO().insert(Grade(1, DIFF_BEGINNER, "3"))
            gradeDAO().insert(Grade(2, DIFF_BEGINNER, "4"))
            gradeDAO().insert(Grade(3, DIFF_BEGINNER, "5"))
            gradeDAO().insert(Grade(4, DIFF_EASY, "5+"))
            gradeDAO().insert(Grade(5, DIFF_EASY, "6A"))
            gradeDAO().insert(Grade(6, DIFF_EASY, "6A+"))
            gradeDAO().insert(Grade(7, DIFF_INTERMEDIATE, "6B"))
            gradeDAO().insert(Grade(8, DIFF_INTERMEDIATE, "6B+"))
            gradeDAO().insert(Grade(9, DIFF_INTERMEDIATE, "6C"))
            gradeDAO().insert(Grade(10, DIFF_ADVANCED, "6C+"))
            gradeDAO().insert(Grade(11, DIFF_ADVANCED, "7A"))
            gradeDAO().insert(Grade(12, DIFF_ADVANCED, "7A+"))
            gradeDAO().insert(Grade(13, DIFF_EXPERT, "7B"))
            gradeDAO().insert(Grade(14, DIFF_EXPERT, "7B+"))
            gradeDAO().insert(Grade(15, DIFF_EXPERT, "7C"))
            gradeDAO().insert(Grade(16, DIFF_ELITE, "7C+"))
            gradeDAO().insert(Grade(17, DIFF_ELITE, "8A"))
            gradeDAO().insert(Grade(18, DIFF_ELITE, "8A+"))
            gradeDAO().insert(Grade(19, DIFF_ELITE, "8B"))
            gradeDAO().insert(Grade(20, DIFF_ELITE, "8B+"))
            gradeDAO().insert(Grade(21, DIFF_ELITE, "8C"))
            gradeDAO().insert(Grade(22, DIFF_ELITE, "8C+"))
            gradeDAO().insert(Grade(23, DIFF_ELITE, "9A"))
        }
    }
}
