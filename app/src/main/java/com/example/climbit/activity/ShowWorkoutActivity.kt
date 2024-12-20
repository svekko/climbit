package com.example.climbit.activity

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.text.format.DateFormat
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.example.climbit.App
import com.example.climbit.R
import com.example.climbit.adapter.DifficultySpinnerAdapter
import com.example.climbit.adapter.WorkoutRouteArrayAdapter
import com.example.climbit.model.*
import com.example.climbit.util.TimeUtil
import java.util.*
import java.util.concurrent.Executors

class ShowWorkoutActivity : BaseActivity() {
    private var workoutID: Long? = null
    private var workout: Workout? = null
    private var routes: List<WorkoutRouteWithSets>? = null
    private var handler: Handler? = null
    private var dialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show_workout)
        workoutID = intent.getLongExtra("id", 0)

        findViewById<Button>(R.id.add_route).setOnClickListener(this::addRoute)
        findViewById<Button>(R.id.finish).setOnClickListener(this::finishWorkkout)
    }

    private fun showTimer() {
        workout?.also {
            val diff = Date().time - it.dateStarted.time
            val timer = findViewById<TextView>(R.id.timer)
            val subtitle = findViewById<TextView>(R.id.subtitle)

            if (diff >= (24 * 60 * 60 * 1000)) {
                timer.visibility = View.GONE
                subtitle.visibility = View.GONE
                return
            }

            val calendar = Calendar.getInstance()
            calendar.timeZone = TimeZone.getTimeZone("UTC")
            calendar.time = Date(diff)

            var dateFormat = "mm:ss"

            if (diff >= (60 * 60 * 1000)) {
                dateFormat = "HH:mm:ss"
            }

            timer.text = DateFormat.format(dateFormat, calendar)
        }
    }

    override fun onResume() {
        dialog?.also {
            it.dismiss()
        }

        super.onResume()

        workoutID?.also { workoutID ->
            workout = App.getDB(this).workoutDAO().get(workoutID).also { workout ->
                routes = App.getDB(this).workoutRouteDAO().getRoutesAndSets(workout.id)

                findViewById<TextView>(R.id.title).text = workout.title

                routes?.also { routes ->
                    val adapter = WorkoutRouteArrayAdapter(this, workout.dateFinished != null, routes)
                    val list = findViewById<RecyclerView>(R.id.routes_list)
                    list.adapter = adapter
                }

                var subtitle: String

                workout.dateFinished?.let {
                    subtitle = "${TimeUtil.formatDatetime(workout.dateStarted)} - ${TimeUtil.formatDatetime(it)}"
                    findViewById<TextView>(R.id.subtitle).text = subtitle
                    findViewById<TextView>(R.id.timer).visibility = View.GONE
                    findViewById<Button>(R.id.add_route).visibility = View.GONE
                    findViewById<Button>(R.id.finish).visibility = View.GONE
                } ?: run {
                    subtitle = "${getString(R.string.workout_timer)}:"
                    findViewById<TextView>(R.id.subtitle).text = subtitle

                    handler?.also {
                        it.removeCallbacksAndMessages(null)
                    }

                    handler = Handler(mainLooper).also {
                        it.post(object : Runnable {
                            override fun run() {
                                showTimer()
                                it.postDelayed(this, 1000)
                            }
                        })
                    }
                }
            }
        }
    }

    private fun finishWorkkout(view: View) {
        workout?.also {
            withConfirmation {
                Executors.newSingleThreadExecutor().execute {
                    it.dateFinished = Date()
                    App.getDB(this).workoutDAO().update(it)

                    runOnUiThread {
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                }
            }
        }
    }

    private fun addRoute(view: View) {
        Executors.newSingleThreadExecutor().execute {
            val difficulties = App.getDB(this).difficultyDAO().getAll().toMutableList()
            difficulties.add(0, Difficulty(0, getString(R.string.select_difficulty), "FFFFFF"))

            val grades = App.getDB(this).gradeDAO().getAll().toMutableList()
            grades.add(0, Grade(0, 0, getString(R.string.unknown_grade)))

            runOnUiThread {
                val builder = AlertDialog.Builder(this)
                val dialogView = layoutInflater.inflate(R.layout.dialog_add_route, null, false)

                val difficultyAdapter = DifficultySpinnerAdapter(this, difficulties)
                val difficultySpinner = dialogView.findViewById<Spinner>(R.id.difficulty)
                difficultySpinner.adapter = difficultyAdapter

                val gradeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, grades)
                val gradeSpinner = dialogView.findViewById<Spinner>(R.id.grade)
                gradeSpinner.adapter = gradeAdapter

                // Show difficulty only if grade has not been selected.
                gradeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onNothingSelected(parent: AdapterView<*>?) {
                        difficultySpinner.visibility = View.VISIBLE
                    }

                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        difficultySpinner.visibility = if (position > 0) {
                            View.GONE
                        } else {
                            View.VISIBLE
                        }
                    }
                }

                builder.setMessage(R.string.add_route)
                builder.setView(dialogView)
                builder.setNegativeButton(R.string.cancel, null)
                builder.setPositiveButton(R.string.confirm, null)

                dialog = builder.create().also { dialog ->
                    dialog.setOnShowListener {
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                            val grade = (gradeSpinner.selectedItem as? Grade) ?: run {
                                alertError("invalid grade")
                                return@setOnClickListener
                            }

                            val difficulty = (difficultySpinner.selectedItem as? Difficulty) ?: run {
                                alertError("invalid difficulty")
                                return@setOnClickListener
                            }

                            var gradeID: Long? = null
                            var difficultyID = difficulty.id

                            // If selected, then grade selection overrides difficulty.
                            if (grade.id > 0) {
                                difficultyID = grade.difficultyID
                                gradeID = grade.id
                            }

                            if (difficultyID < 1) {
                                alertError("difficulty must be selected")
                                return@setOnClickListener
                            }

                            val name = dialogView.findViewById<EditText>(R.id.name).text.toString().let {
                                var out = it

                                if (out.isEmpty()) {
                                    out = getString(R.string.route)

                                    routes?.let { routes ->
                                        out = "$out #${routes.size + 1}"
                                    }
                                }

                                out
                            }

                            workoutID?.also {
                                Executors.newSingleThreadExecutor().execute {
                                    val route = WorkoutRoute(0, name, Date(), it, difficultyID, gradeID)
                                    val id = App.getDB(this).workoutRouteDAO().insert(route)

                                    runOnUiThread {
                                        val intent = Intent(this, ShowWorkoutRouteActivity::class.java)
                                        intent.putExtra("id", id)
                                        startActivity(intent)
                                    }
                                }
                            }
                        }
                    }

                    dialog.show()
                }
            }
        }
    }
}
