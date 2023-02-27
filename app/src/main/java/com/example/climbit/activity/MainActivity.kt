package com.example.climbit.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import com.example.climbit.App
import com.example.climbit.R
import java.util.concurrent.Executors

class MainActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.list_workouts).setOnClickListener {
            startActivity(Intent(this, ListWorkoutsActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()

        Executors.newSingleThreadExecutor().execute {
            val startWorkoutBtn = findViewById<Button>(R.id.start_workout)
            val workouts = App.getDB(this).workoutDAO().getAll(1)
            var isFinished = true

            workouts.firstOrNull()?.also { workout ->
                if (workout.dateFinished == null) {
                    isFinished = false
                    startWorkoutBtn.setText(R.string.continue_workout)
                    startWorkoutBtn.setOnClickListener {
                        val intent = Intent(this, ShowWorkoutActivity::class.java)
                        intent.putExtra("id", workout.id)
                        startActivity(intent)
                    }
                }
            }

            if (isFinished) {
                startWorkoutBtn.setText(R.string.start_workout)
                startWorkoutBtn.setOnClickListener {
                    startActivity(Intent(this, StartWorkoutActivity::class.java))
                }
            }
        }
    }
}
