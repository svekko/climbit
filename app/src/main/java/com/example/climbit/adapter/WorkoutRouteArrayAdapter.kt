package com.example.climbit.adapter

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.climbit.App
import com.example.climbit.R
import com.example.climbit.activity.BaseActivity
import com.example.climbit.activity.ShowWorkoutRouteActivity
import com.example.climbit.model.WorkoutRouteWithSets
import com.example.climbit.photo.WorkoutRoutePhotos
import java.util.concurrent.Executors

class WorkoutRouteArrayAdapter(act: BaseActivity, finished: Boolean, list: List<WorkoutRouteWithSets>) : RecyclerView.Adapter<WorkoutRouteArrayAdapter.ViewHolder>() {
    private val routes = list
    private val workoutFinished = finished
    private val activity = act

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleView: TextView
        val subtitleView: TextView
        val deleteButton: ImageButton

        init {
            titleView = view.findViewById(R.id.title)
            subtitleView = view.findViewById(R.id.subtitle)
            deleteButton = view.findViewById(R.id.delete)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(activity)
        val view = inflater.inflate(R.layout.list_item_workout_route, parent, false)
        return ViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        routes.getOrNull(position)?.also { route ->
            val title = route.workoutRoute.name
            var subtitle = activity.resources.getQuantityString(R.plurals.n_attempts, route.sets.size, route.sets.size)

            for (set in route.sets) {
                if (set.finished) {
                    subtitle = "${activity.getString(R.string.completed)}, ${subtitle.lowercase()}"
                    break
                }
            }

            if (workoutFinished) {
                holder.deleteButton.visibility = View.INVISIBLE
            }

            var difficulty = "${route.difficulty.name} ${activity.getString(R.string.difficulty).lowercase()}"

            // Grade is not mandatory.
            route.grade?.also {
                difficulty = "$difficulty (${it.fontScale})"
            }

            holder.titleView.text = title
            holder.titleView.setTextColor(Color.parseColor("#${route.difficulty.hexColor}"))
            holder.subtitleView.text = "${difficulty}\n${subtitle}"

            holder.itemView.setOnClickListener {
                val intent = Intent(activity, ShowWorkoutRouteActivity::class.java)
                intent.putExtra("id", route.workoutRoute.id)
                activity.startActivity(intent)
            }

            holder.deleteButton.setOnClickListener {
                activity.withConfirmation {
                    Executors.newSingleThreadExecutor().execute {
                        WorkoutRoutePhotos(route.workoutRoute.id).deleteAll()
                        App.getDB(activity).workoutRouteDAO().delete(route.workoutRoute.id)

                        activity.runOnUiThread {
                            activity.finish()
                            activity.startActivity(activity.intent)
                        }
                    }
                }
            }
        }
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemCount(): Int {
        return routes.size
    }
}
