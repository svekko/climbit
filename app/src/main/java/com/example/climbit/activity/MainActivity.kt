package com.example.climbit.activity

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
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

        checkPermissions()
    }

    private fun checkPermissions() {
        val permissions = ArrayList<String>()

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
        }

        if (permissions.size < 1) {
            return
        }

        val launcher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { isGrantedMap ->
            isGrantedMap.entries.forEach { isGranted ->
                if (!isGranted.value) {
                    alertError("permission is required: ${isGranted.key.split(".").last()}")

                    findViewById<Button>(R.id.list_workouts).also {
                        it.isEnabled = false
                        it.alpha = 0.5F
                    }

                    findViewById<Button>(R.id.start_workout).also {
                        it.isEnabled = false
                        it.alpha = 0.5F
                    }
                }
            }
        }

        launcher.launch(permissions.toTypedArray())
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
