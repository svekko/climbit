package com.example.climbit.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
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

        val settingsDAO = App.getDB(this).settingsDAO()
        val settings = settingsDAO.select()

        val activityLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.also {
                contentResolver.openInputStream(it).use { inputStream ->
                    inputStream?.also {
                        val imgBase64 = Base64.encodeToString(inputStream.readBytes(), Base64.DEFAULT)

                        settings.profileImageBase64 = imgBase64
                        settingsDAO.update(settings)

                        showProfileImage(imgBase64)
                    }
                }
            }
        }

        findViewById<Button>(R.id.next).setOnClickListener(this::onNextClick)
        findViewById<Button>(R.id.select_image).setOnClickListener {
            activityLauncher.launch("image/*")
        }

        settings.profileImageBase64?.also {
            showProfileImage(it)
        }
    }

    @SuppressLint("InflateParams")
    private fun showProfileImage(imgBase64: String) {
        val bytes = Base64.decode(imgBase64, Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

        val imageView = findViewById<ImageView>(R.id.image)
        val imageWrapper = findViewById<CardView>(R.id.image_wrapper)
        val deleteView = layoutInflater.inflate(R.layout.layout_item_delete, null)

        imageWrapper.visibility = View.VISIBLE
        imageView.setImageBitmap(bitmap)
        imageView.setOnLongClickListener {
            imageWrapper.removeAllViews()
            imageWrapper.addView(deleteView)
            vibrateShort()
            true
        }

        deleteView.findViewById<Button>(R.id.cancel).setOnClickListener {
            imageWrapper.removeAllViews()
            imageWrapper.addView(imageView)
        }

        deleteView.findViewById<Button>(R.id.delete).setOnClickListener {
            val settingsDAO = App.getDB(this).settingsDAO()
            val settings = settingsDAO.select()

            settings.profileImageBase64 = null
            settingsDAO.update(settings)

            reloadActivity()
        }

        findViewById<Button>(R.id.select_image).visibility = View.GONE
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
