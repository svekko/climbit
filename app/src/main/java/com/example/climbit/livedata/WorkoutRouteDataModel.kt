package com.example.climbit.livedata

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.climbit.App
import com.example.climbit.model.WorkoutRouteBundle
import java.util.concurrent.Executors

class WorkoutRouteDataModel(ctx: Context, routeID: Long) : ViewModel() {
    private val workoutRoute = MutableLiveData<WorkoutRouteBundle>()

    init {
        Executors.newSingleThreadExecutor().execute {
            val db = App.getDB(ctx)
            val sets = db.workoutSetDAO().getAll(routeID)
            val route = db.workoutRouteDAO().get(routeID)
            val workout = db.workoutDAO().get(route.workoutID)
            val lastSet = db.workoutSetDAO().getLastForWorkout(route.workoutID)
            val difficulty = db.difficultyDAO().get(route.difficultyID)
            val grade = route.gradeID?.let {
                db.gradeDAO().get(it)
            }

            workoutRoute.postValue(
                WorkoutRouteBundle(
                    workout,
                    route,
                    lastSet,
                    difficulty,
                    grade,
                    sets,
                )
            )
        }
    }

    fun getWorkoutRoute(): LiveData<WorkoutRouteBundle> {
        return workoutRoute
    }
}
