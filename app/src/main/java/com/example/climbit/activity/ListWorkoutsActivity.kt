package com.example.climbit.activity

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import com.example.climbit.App
import com.example.climbit.R
import com.example.climbit.adapter.WorkoutArrayAdapter
import com.example.climbit.model.Workout
import java.util.concurrent.Executors

class ListWorkoutsActivity : BaseActivity() {
    private var page = 1
    private var adapter: WorkoutArrayAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list_workouts)

        findViewById<Button>(R.id.load_more).setOnClickListener {
            page++
            populateListView(page)
        }
    }

    override fun onResume() {
        super.onResume()

        page = 1
        populateListView(page)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun populateListView(p: Int) {
        Executors.newSingleThreadExecutor().execute {
            val workouts = App.getDB(this).workoutDAO().getAll(p)

            runOnUiThread {
                if (page == 1) {
                    adapter = WorkoutArrayAdapter(this, workouts as ArrayList<Workout>)
                    findViewById<RecyclerView>(R.id.list).adapter = adapter
                }

                if (page != 1) {
                    adapter?.also {
                        it.append(workouts as ArrayList<Workout>)
                        it.notifyDataSetChanged()
                    }
                }

                if (workouts.size < 20) {
                    findViewById<Button>(R.id.load_more).visibility = View.GONE
                }
            }
        }
    }
}
