package com.example.climbit.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.example.climbit.App
import com.example.climbit.R
import com.example.climbit.adapter.DifficultySpinnerAdapter
import com.example.climbit.adapter.WorkoutRouteArrayAdapter
import com.example.climbit.model.Difficulty
import com.example.climbit.model.Workout
import com.example.climbit.model.WorkoutRoute
import com.example.climbit.util.TimeUtil
import java.util.*
import java.util.concurrent.Executors

class ShowWorkoutActivity : BaseActivity() {
    var workoutID: Long? = null
    var workout: Workout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show_workout)
        workoutID = intent.getLongExtra("id", 0)

        findViewById<Button>(R.id.add_route).setOnClickListener(this::addRoute)
        findViewById<Button>(R.id.finish).setOnClickListener(this::finishWorkkout)
    }

    override fun onResume() {
        super.onResume()

        workoutID?.also {
            Executors.newSingleThreadExecutor().execute {
                workout = App.getDB(this).workoutDAO().get(it).also {
                    val routes = App.getDB(this).workoutRouteDAO().getRoutesAndSets(it.id)

                    runOnUiThread {
                        findViewById<TextView>(R.id.title).text = it.title
                        findViewById<TextView>(R.id.date_started).text = TimeUtil.formatDatetime(it.dateStarted)

                        it.dateFinished?.also {
                            findViewById<Button>(R.id.add_route).visibility = View.GONE
                            findViewById<Button>(R.id.finish).visibility = View.GONE
                        }

                        val adapter = WorkoutRouteArrayAdapter(this, it.dateFinished != null, routes)
                        val list = findViewById<RecyclerView>(R.id.routes_list)
                        list.adapter = adapter
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

            runOnUiThread {
                val builder = AlertDialog.Builder(this)
                val dialogView = layoutInflater.inflate(R.layout.dialog_add_route, null, false)
                val adapter = DifficultySpinnerAdapter(this, difficulties)

                val difficultySpinner = dialogView.findViewById<Spinner>(R.id.difficulty)
                difficultySpinner.adapter = adapter

                builder.setMessage(R.string.add_route)
                builder.setView(dialogView)
                builder.setNegativeButton(R.string.cancel, null)
                builder.setPositiveButton(R.string.confirm, null)

                val dialog = builder.create()

                dialog.setOnShowListener {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val difficulty = (difficultySpinner.selectedItem as? Difficulty) ?: run {
                            alertError("invalid difficulty")
                            return@setOnClickListener
                        }

                        if (difficulty.id < 1) {
                            alertError("difficulty must be selected")
                            return@setOnClickListener
                        }

                        val name = dialogView.findViewById<EditText>(R.id.name).text.toString().also {
                            if (it.isEmpty()) {
                                alertError("name is empty")
                                return@setOnClickListener
                            }
                        }

                        workoutID?.also {
                            Executors.newSingleThreadExecutor().execute {
                                val route = WorkoutRoute(0, name, Date(), it, difficulty.id)
                                val id = App.getDB(this).workoutRouteDAO().insert(route)

                                runOnUiThread {
                                    val intent = Intent(this, ShowWorkoutRouteActivity::class.java)
                                    intent.putExtra("id", id)
                                    startActivity(intent)
                                    dialog.dismiss()
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
