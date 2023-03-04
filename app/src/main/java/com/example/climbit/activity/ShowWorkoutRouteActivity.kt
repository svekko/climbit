package com.example.climbit.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.text.format.DateFormat
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import com.example.climbit.App
import com.example.climbit.R
import com.example.climbit.adapter.WorkoutSetArrayAdapter
import com.example.climbit.model.WorkoutSet
import com.example.climbit.photo.WorkoutRoutePhoto
import com.github.chrisbanes.photoview.PhotoView
import java.io.File
import java.io.IOException
import java.util.*
import java.util.concurrent.Executors

class ShowWorkoutRouteActivity : BaseActivity() {
    private var routeID: Long? = null
    private var workoutID: Long? = null
    private var takePhotoLauncher: ActivityResultLauncher<Uri>? = null
    private var photoPath: String? = null
    private var isFinished = false
    private var handler: Handler? = null

    override fun onDestroy() {
        deleteTempPhotoOnPhotoPath(true)
        super.onDestroy()
    }

    private fun deleteTempPhotoOnPhotoPath(tempFileOnly: Boolean) {
        photoPath?.also { path ->
            File(path).also { f ->
                if (f.exists()) {
                    if (tempFileOnly && f.length() > 0L) {
                        return
                    }

                    f.delete()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show_workout_route)
        routeID = intent.getLongExtra("id", 0)

        takePhotoLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { result ->
            if (!result) {
                // Taking photo was cancelled.
                // Remove temporary photo that was initially created.
                deleteTempPhotoOnPhotoPath(false)
                photoPath = null
            }

            reloadActivity()
        }

        findViewById<Button>(R.id.add_attempt).setOnClickListener {
            addWorkoutSet(false)
        }

        findViewById<Button>(R.id.route_completed).setOnClickListener {
            withConfirmation {
                addWorkoutSet(true)
            }
        }
    }

    override fun onResume() {
        super.onResume()

        initTakePhotoListener()
        populateSetsListView()

        Executors.newSingleThreadExecutor().execute {
            loadPhotos()
        }
    }

    private fun reloadActivity() {
        finish()
        startActivity(intent)
    }

    private fun loadPhotos() {
        var count = 0
        val photos = findViewById<LinearLayout>(R.id.images)
        val photosScroll = findViewById<HorizontalScrollView>(R.id.images_scroll)

        getExternalFilesDir(Environment.DIRECTORY_PICTURES)?.also { dir ->
            dir.listFiles()?.also { files ->
                runOnUiThread {
                    photos.removeAllViews()
                }

                val backgroundRunner = Executors.newFixedThreadPool(3)

                for (photoFile in files) {
                    // Temporary image with no content.
                    if (photoFile.length() == 0L) {
                        photoFile.delete()
                        continue
                    }

                    WorkoutRoutePhoto(photoFile).let { photo ->
                        if (photo.routeID == routeID) {
                            val bitmapThumbnail = photo.asBitmap(125.0F)
                            val imgView = ImageView(this)
                            val cardView = CardView(this)

                            val params = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.MATCH_PARENT,
                            )

                            if (count != 0) {
                                params.leftMargin = 25
                            }

                            imgView.adjustViewBounds = true
                            imgView.setImageBitmap(bitmapThumbnail)

                            runOnUiThread {
                                val animation = AnimationUtils.loadAnimation(this, androidx.appcompat.R.anim.abc_fade_in)
                                animation.startOffset = 0
                                animation.duration = 500

                                cardView.addView(imgView)
                                cardView.radius = 20.0F
                                cardView.layoutParams = params

                                photos.addView(cardView)
                                imgView.startAnimation(animation)
                            }

                            backgroundRunner.execute {
                                val bitmapFullSize = photo.asBitmap()

                                runOnUiThread {
                                    imgView.setOnClickListener {
                                        showPhotoFullScreen(photoFile, bitmapFullSize)
                                    }
                                }
                            }

                            count++
                        }
                    }
                }
            }
        }

        if (count == 0) {
            photosScroll.visibility = View.GONE
        } else {
            photosScroll.visibility = View.VISIBLE
        }
    }

    private fun showPhotoFullScreen(file: File, bitmap: Bitmap) {
        val builder = AlertDialog.Builder(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val dialogView = layoutInflater.inflate(R.layout.dialog_image_enlarged, null, false)

        val photoView = dialogView.findViewById<PhotoView>(R.id.image)
        photoView.setImageBitmap(bitmap)

        if (!isFinished) {
            builder.setNeutralButton(R.string.remove) { _, _ ->
                file.delete()
                reloadActivity()
            }
        }

        builder.setNegativeButton(R.string.close, null)

        val dialog = builder.create()
        dialog.setView(dialogView)
        dialog.show()
    }

    private fun initTakePhotoListener() {
        val photoFile = try {
            createPhotoFile()
        } catch (ex: IOException) {
            alertError("could not create image file")
            null
        }

        photoFile?.also { f ->
            val uri = FileProvider.getUriForFile(this, "com.example.android.fileprovider", f)

            findViewById<ImageButton>(R.id.take_photo).setOnClickListener {
                takePhotoLauncher?.also { launcher ->
                    launcher.launch(uri)
                }
            }
        }
    }

    @Throws(IOException::class)
    private fun createPhotoFile(): File {
        val timeStamp = DateFormat.format("yyyyMMdd_HHmmss", Date()).toString()
        val dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        return File.createTempFile("${routeID}___${timeStamp}", ".jpg", dir).also {
            photoPath = it.absolutePath
        }
    }

    private fun showRestTimer(lastSet: WorkoutSet) {
        val diff = Date().time - lastSet.dateIn.time
        val timer = findViewById<TextView>(R.id.timer)
        val timerTitle = findViewById<TextView>(R.id.timer_title)

        if (diff >= (60 * 60 * 1000)) {
            timer.visibility = View.GONE
            timerTitle.visibility = View.GONE
            return
        }

        val dur = Date(diff)
        val text = DateFormat.format("mm:ss", dur)
        val titleText = "${getString(R.string.rest_timer)}:"

        timer.text = text
        timerTitle.text = titleText
    }

    @SuppressLint("SetTextI18n")
    private fun populateSetsListView() {
        routeID?.also {
            Executors.newSingleThreadExecutor().execute {
                val db = App.getDB(this)
                val sets = db.workoutSetDAO().getAll(it)
                val route = db.workoutRouteDAO().get(it)
                val workout = db.workoutDAO().get(route.workoutID)
                val lastSet = db.workoutSetDAO().getLastForWorkout(route.workoutID)
                val difficulty = db.difficultyDAO().get(route.difficultyID)
                val workoutFinished = workout.dateFinished != null
                val grade = route.gradeID?.let {
                    db.gradeDAO().get(it)
                }

                workoutID = route.workoutID

                if (workoutFinished || (sets.isNotEmpty() && sets[0].finished)) {
                    isFinished = true
                }

                runOnUiThread {
                    // Update rest timer every second.
                    lastSet?.also { set ->
                        if (isFinished) {
                            findViewById<TextView>(R.id.timer).visibility = View.GONE
                            findViewById<TextView>(R.id.timer_title).visibility = View.GONE
                        } else {
                            handler?.also {
                                it.removeCallbacksAndMessages(null)
                            }

                            handler = Handler(mainLooper).also {
                                it.post(object : Runnable {
                                    override fun run() {
                                        showRestTimer(set)
                                        it.postDelayed(this, 1000)
                                    }
                                })
                            }
                        }
                    }

                    val adapter = WorkoutSetArrayAdapter(this, workoutFinished, sets)
                    val list = findViewById<RecyclerView>(R.id.sets_list)
                    list.adapter = adapter

                    // Can not modify if workout is finished or workout route has been marked complete.
                    if (isFinished) {
                        findViewById<ImageButton>(R.id.take_photo).visibility = View.GONE
                        findViewById<Button>(R.id.add_attempt).visibility = View.GONE
                        findViewById<Button>(R.id.route_completed).visibility = View.GONE
                    }

                    findViewById<TextView>(R.id.title).also { v ->
                        v.text = route.name
                    }

                    var difficultyText = "${difficulty.name} ${getString(R.string.difficulty).lowercase()}"

                    // Grade is not mandatory.
                    grade?.also { grade ->
                        difficultyText = "$difficultyText (${grade.fontScale})"
                    }

                    findViewById<TextView>(R.id.difficulty).also { v ->
                        v.text = difficultyText
                        v.setTextColor(Color.parseColor("#${difficulty.hexColor}"))
                    }
                }
            }
        }
    }

    private fun addWorkoutSet(completed: Boolean) {
        routeID?.also {
            Executors.newSingleThreadExecutor().execute {
                val set = WorkoutSet(0, Date(), it, completed)
                App.getDB(this).workoutSetDAO().insert(set)

                runOnUiThread {
                    if (!completed) {
                        reloadActivity()
                        return@runOnUiThread
                    }

                    workoutID?.also {
                        val intent = Intent(this, ShowWorkoutActivity::class.java)
                        intent.putExtra("id", it)
                        startActivity(intent)
                        finish()
                    }
                }
            }
        }
    }
}
