package com.example.climbit.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.climbit.App
import com.example.climbit.R
import com.example.climbit.activity.BaseActivity
import com.example.climbit.activity.ShowWorkoutActivity
import com.example.climbit.livedata.PhotosDataModel
import com.example.climbit.model.Workout
import com.example.climbit.util.TimeUtil

class WorkoutArrayAdapter(act: BaseActivity, list: ArrayList<Workout>) : RecyclerView.Adapter<WorkoutArrayAdapter.ViewHolder>() {
    private val workouts = list
    private val activity = act

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleView: TextView
        val dateStartFinishView: TextView
        val deleteButton: ImageButton

        init {
            titleView = view.findViewById(R.id.title)
            dateStartFinishView = view.findViewById(R.id.date_start_finish)
            deleteButton = view.findViewById(R.id.delete)
        }
    }

    fun append(list: ArrayList<Workout>) {
        workouts.addAll(list)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(activity)
        val view = inflater.inflate(R.layout.list_item_workout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        workouts.getOrNull(position)?.also { workout ->
            var dateStartFinish = TimeUtil.formatDatetime(workout.dateStarted).toString()

            workout.dateFinished?.let {
                dateStartFinish += " - ${TimeUtil.formatDatetime(it)}"
            } ?: run {
                dateStartFinish += " - ..."
            }

            holder.titleView.text = workout.title
            holder.dateStartFinishView.text = dateStartFinish

            holder.itemView.setOnClickListener {
                val intent = Intent(activity, ShowWorkoutActivity::class.java)
                intent.putExtra("id", workout.id)
                activity.startActivity(intent)
            }

            holder.deleteButton.setOnClickListener {
                activity.withConfirmation {
                    val db = App.getDB(activity)
                    val routes = db.workoutRouteDAO().getRoutesAndSets(workout.id)

                    for (route in routes) {
                        PhotosDataModel(route.workoutRoute.id).deleteAll(activity)
                    }

                    db.workoutDAO().delete(workout.id)

                    activity.finish()
                    activity.startActivity(activity.intent)
                }
            }
        }
    }

    override fun getItemId(position: Int): Long {
        return workouts[position].id
    }

    override fun getItemCount(): Int {
        return workouts.size
    }
}
