package com.example.climbit.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import com.example.climbit.App
import com.example.climbit.R
import com.example.climbit.model.Workout
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.Executors

class StartWorkoutActivity : BaseActivity() {
    private var title: EditText? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start_workout)

        title = findViewById<EditText>(R.id.title)?.also {
            it.setText(generateTitle())
        }

        findViewById<Button>(R.id.next).setOnClickListener(this::onNextClick)
    }

    private fun onNextClick(view: View) {
        title?.also {
            val titleText = it.text.toString()

            if (titleText.isEmpty()) {
                alertError("title is empty")
                return
            }

            val workout = Workout(0, titleText, Date(), null)

            Executors.newSingleThreadExecutor().execute {
                val id = App.getDB(this).workoutDAO().insert(workout)

                runOnUiThread {
                    val intent = Intent(this, ShowWorkoutActivity::class.java)
                    intent.putExtra("id", id)
                    startActivity(intent)
                    finish()
                }
            }
        }
    }

    private fun generateTitle(): String {
        val now = LocalDateTime.now();
        val hour = now.hour;

        if (hour in 4..10) {
            return getString(R.string.early_morning_workout)
        }

        if (hour in 11..16) {
            return getString(R.string.lunchtime_workout);
        }

        if (hour in 17..22) {
            return getString(R.string.nighttime_workout);
        }

        return getString(R.string.late_night_workout);
    }
}
