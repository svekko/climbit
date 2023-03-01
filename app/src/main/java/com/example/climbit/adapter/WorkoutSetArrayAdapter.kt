package com.example.climbit.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.climbit.App
import com.example.climbit.R
import com.example.climbit.activity.BaseActivity
import com.example.climbit.model.WorkoutSet
import com.example.climbit.util.TimeUtil
import java.util.concurrent.Executors

class WorkoutSetArrayAdapter(act: BaseActivity, finished: Boolean, list: List<WorkoutSet>) : RecyclerView.Adapter<WorkoutSetArrayAdapter.ViewHolder>() {
    private val sets = list
    private val workoutFinished = finished
    private val activity = act

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleView: TextView
        val dateInView: TextView
        val deleteButton: ImageButton

        init {
            titleView = view.findViewById(R.id.title)
            dateInView = view.findViewById(R.id.date_in)
            deleteButton = view.findViewById(R.id.delete)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(activity)
        val view = inflater.inflate(R.layout.list_item_workout_set, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        sets.getOrNull(position)?.also { set ->
            var title = String.format(activity.getString(R.string.set_n), sets.size - position)

            if (set.finished) {
                title += " - ${activity.getString(R.string.completed).lowercase()}"
            }

            if (workoutFinished) {
                holder.deleteButton.visibility = View.INVISIBLE
            }

            holder.titleView.text = title
            holder.dateInView.text = TimeUtil.formatDatetime(set.dateIn)

            holder.deleteButton.setOnClickListener {
                activity.withConfirmation {
                    Executors.newSingleThreadExecutor().execute {
                        App.getDB(activity).workoutSetDAO().delete(set.id)

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
        return sets[position].id
    }

    override fun getItemCount(): Int {
        return sets.size
    }
}
