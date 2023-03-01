package com.example.climbit.adapter

import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.widget.AppCompatCheckedTextView
import com.example.climbit.model.Difficulty

class DifficultySpinnerAdapter(ctx: Context, rows: List<Difficulty>) : ArrayAdapter<Difficulty>(ctx, android.R.layout.simple_spinner_dropdown_item, rows) {
    private fun colorifyDifficulty(position: Int, view: View) {
        getItem(position)?.also { difficulty ->
            (view as? AppCompatCheckedTextView)?.also {
                if (difficulty.id == 0L) {
                    it.alpha = 0.45F
                } else {
                    it.setTextColor(Color.parseColor("#${difficulty.hexColor}"))
                }

                it.textSize = 17.0F
            }
        }
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent)
        colorifyDifficulty(position, view)
        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getDropDownView(position, convertView, parent)
        colorifyDifficulty(position, view)
        return view
    }
}
