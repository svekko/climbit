package com.example.climbit.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.text.format.DateFormat
import android.util.TypedValue
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import androidx.recyclerview.widget.RecyclerView
import com.example.climbit.App
import com.example.climbit.R
import com.example.climbit.adapter.WorkoutSetArrayAdapter
import com.example.climbit.model.WorkoutSet
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

    private fun modifyPhoto(file: File, heightDp: Float?): Bitmap {
        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
        val orientation = ExifInterface(file).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_UNDEFINED
        )

        if (orientation == ExifInterface.ORIENTATION_ROTATE_90) {
            return modifyPhoto(bitmap, 90.0F, heightDp)
        }

        if (orientation == ExifInterface.ORIENTATION_ROTATE_180) {
            return modifyPhoto(bitmap, 180.0F, heightDp)
        }

        if (orientation == ExifInterface.ORIENTATION_ROTATE_270) {
            return modifyPhoto(bitmap, 270.0F, heightDp)
        }

        return bitmap
    }

    private fun modifyPhoto(source: Bitmap, angle: Float, heightDp: Float?): Bitmap {
        var scale = 1.0F

        heightDp?.also {
            val heightPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, heightDp, Resources.getSystem().displayMetrics)
            if (heightPx < source.height) {
                scale = heightPx / source.height
            }
        }

        val matrix = Matrix()
        matrix.postRotate(angle)
        matrix.postScale(scale, scale)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
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

                for (photoFile in files) {
                    // Temporary image with no content.
                    if (photoFile.length() == 0L) {
                        photoFile.delete()
                        continue
                    }

                    val nameParts = photoFile.name.split("___")
                    val id = nameParts[0].toLong()

                    if (id == routeID) {
                        val bitmapThumbnail = modifyPhoto(photoFile, 125.0F)
                        val imgView = ImageView(this)

                        val params = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.MATCH_PARENT,
                        )

                        if (count != 0) {
                            params.leftMargin = 25
                        }

                        imgView.adjustViewBounds = true
                        imgView.layoutParams = params
                        imgView.setImageBitmap(bitmapThumbnail)

                        runOnUiThread {
                            val animation = AnimationUtils.loadAnimation(this, androidx.appcompat.R.anim.abc_fade_in)
                            animation.startOffset = 0
                            animation.duration = 500

                            photos.addView(imgView)
                            imgView.startAnimation(animation)

                            imgView.setOnClickListener {
                                val bitmapFullSize = modifyPhoto(photoFile, null)
                                showPhotoFullScreen(photoFile, bitmapFullSize)
                            }
                        }

                        count++
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

        findViewById<TextView>(R.id.timer).also { timer ->
            if (diff >= (60 * 60 * 1000)) {
                timer.visibility = View.INVISIBLE
                return
            }

            val dur = Date(diff)
            val text = "${getString(R.string.rest_timer)}: ${DateFormat.format("mm:ss", dur)}"

            timer.text = text
        }
    }

    @SuppressLint("SetTextI18n")
    private fun populateSetsListView() {
        routeID?.also {
            Executors.newSingleThreadExecutor().execute {
                val db = App.getDB(this)
                val sets = db.workoutSetDAO().getAll(it)
                val route = db.workoutRouteDAO().get(it)
                val workout = db.workoutDAO().get(route.workoutID)
                val difficulty = db.difficultyDAO().get(route.difficultyID)
                val workoutFinished = workout.dateFinished != null

                workoutID = route.workoutID

                if (workoutFinished || (sets.isNotEmpty() && sets[0].finished)) {
                    isFinished = true
                }

                runOnUiThread {
                    // Update rest timer every second.
                    if (sets.isNotEmpty() && !isFinished) {
                        handler?.also {
                            it.removeCallbacksAndMessages(null)
                        }

                        handler = Handler(mainLooper).also {
                            it.post(object : Runnable {
                                override fun run() {
                                    sets.firstOrNull()?.also { set ->
                                        showRestTimer(set)
                                    }

                                    it.postDelayed(this, 1000)
                                }
                            })
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

                    findViewById<TextView>(R.id.difficulty).also { v ->
                        v.text = difficulty.name
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
