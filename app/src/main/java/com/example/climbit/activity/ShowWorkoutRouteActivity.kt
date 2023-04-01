package com.example.climbit.activity

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.graphics.*
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
import com.example.climbit.model.HoldAnnotation
import com.example.climbit.model.WorkoutSet
import com.example.climbit.photo.WorkoutRoutePhoto
import com.example.climbit.photo.WorkoutRoutePhotos
import com.example.climbit.view.SwipeListener
import com.github.chrisbanes.photoview.PhotoView
import java.io.File
import java.io.IOException
import java.util.*
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class ShowWorkoutRouteActivity : BaseActivity() {
    private var routeID: Long? = null
    private var workoutID: Long? = null
    private var takePhotoLauncher: ActivityResultLauncher<Uri>? = null
    private var photoPath: String? = null
    private var isFinished = false
    private var handler: Handler? = null
    private val annotations: MutableList<HoldAnnotation> = ArrayList()
    private val photosList: MutableList<WorkoutRoutePhoto> = ArrayList()

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
        val backgroundRunner = Executors.newFixedThreadPool(3)

        routeID?.also { routeID ->
            runOnUiThread {
                photos.removeAllViews()
            }

            photosList.addAll(WorkoutRoutePhotos(this, routeID).photos)

            for (photo in photosList) {
                val bitmapThumbnail = photo.asBitmap(125.0F)
                val index = count
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
                            showPhotoFullScreen(index, bitmapFullSize, false)
                        }
                    }
                }

                drawAnnotations(bitmapThumbnail, photo)
                count++
            }
        }

        if (count == 0) {
            photosScroll.visibility = View.GONE
        } else {
            photosScroll.visibility = View.VISIBLE
        }
    }

    private fun drawAnnotations(bitmap: Bitmap, photo: WorkoutRoutePhoto) {
        val canvas = Canvas(bitmap)

        annotations.clear()
        annotations.addAll(App.getDB(this).holdAnnotationDAO().getAll(photo.file.name))

        for (annotation in annotations) {
            val annotationX = annotation.x * bitmap.width
            val annotationY = annotation.y * bitmap.width
            val annotationRadius = annotation.radius * bitmap.width
            val annotationStrokeWidth = annotation.strokeWidth * bitmap.width

            var paint = Paint()
            paint.color = Color.WHITE
            paint.style = Paint.Style.FILL
            paint.blendMode = BlendMode.OVERLAY
            paint.alpha = 80

            canvas.drawCircle(annotationX, annotationY, annotationRadius, paint)

            paint = Paint()
            paint.color = Color.BLUE
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = annotationStrokeWidth
            paint.alpha = 128

            canvas.drawCircle(annotationX, annotationY, annotationRadius + (annotationStrokeWidth / 2), paint)
        }
    }

    private fun showPhotoFullScreen(index: Int, bitmap: Bitmap, wasEdited: Boolean) {
        photosList.getOrNull(index)?.also { photo ->
            val builder = AlertDialog.Builder(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
            val dialogView = layoutInflater.inflate(R.layout.dialog_image_enlarged, null, false)
            val photoView = dialogView.findViewById<PhotoView>(R.id.image)
            var edited = wasEdited

            photoView.setImageBitmap(bitmap)
            photoView.setScaleLevels(1F, 5F, 10F)

            bitmap.copy(bitmap.config, true).also { tmpBitmap ->
                photoView.setImageBitmap(tmpBitmap)

                Executors.newSingleThreadExecutor().execute {
                    drawAnnotations(tmpBitmap, photo)
                }
            }

            if (!isFinished) {
                builder.setNeutralButton(R.string.remove, null)
            }

            builder.setPositiveButton(R.string.close, null)

            val dialog = builder.create()
            dialog.setView(dialogView)
            dialog.show()

            val swipeListener = SwipeListener()
            val sleepAndDismissDialog = Runnable {
                Executors.newSingleThreadExecutor().execute {
                    Thread.sleep(100)
                    runOnUiThread {
                        dialog.dismiss()
                    }
                }
            }

            // Right swipe - move to previous photo.
            swipeListener.onRightSwipe = Runnable {
                photosList.getOrNull(index - 1)?.also { prevPhoto ->
                    showPhotoFullScreen(index - 1, prevPhoto.asBitmap(), edited)
                    sleepAndDismissDialog.run()
                }
            }

            // Left swipe - move to next photo.
            swipeListener.onLeftSwipe = Runnable {
                photosList.getOrNull(index + 1)?.also { nextPhoto ->
                    showPhotoFullScreen(index + 1, nextPhoto.asBitmap(), edited)
                    sleepAndDismissDialog.run()
                }
            }

            photoView.setOnSingleFlingListener(swipeListener)

            if (!isFinished) {
                dialog.getButton(Dialog.BUTTON_NEUTRAL).setOnClickListener {
                    photo.file.delete()
                    reloadActivity()
                }

                dialog.getButton(Dialog.BUTTON_POSITIVE).setOnClickListener {
                    if (edited) {
                        reloadActivity()
                    } else {
                        dialog.dismiss()
                    }
                }

                photoView.setOnPhotoTapListener { _, w, h ->
                    edited = true

                    val zoomRatio = photoView.displayRect.width() / photoView.width
                    val matrix = Matrix()

                    bitmap.copy(bitmap.config, true).also { tmpBitmap ->
                        photoView.attacher.getSuppMatrix(matrix)

                        val circleCx = (tmpBitmap.width * w).roundToInt()
                        val circleCy = (tmpBitmap.height * h).roundToInt()
                        val circleRadius = min(250F, max(350F / zoomRatio, 50F)).roundToInt()
                        val strokeWidth = max(circleRadius / 5, 15)

                        var addAnnotation = true
                        val annotationDeleteList: MutableList<Long> = ArrayList()

                        // Check for overlap with existing annotation.
                        // Remove overlapping.
                        for (annotation in annotations) {
                            val circleAvgRadius = (circleRadius + (annotation.radius * tmpBitmap.width)) / 2
                            val annotationX = annotation.x * tmpBitmap.width
                            val annotationY = annotation.y * tmpBitmap.width

                            if (abs(annotationX - circleCx) < circleAvgRadius && abs(annotationY - circleCy) < circleAvgRadius) {
                                addAnnotation = false
                                annotationDeleteList.add(annotation.id)
                            }
                        }

                        Executors.newSingleThreadExecutor().execute {
                            if (addAnnotation) {
                                App.getDB(this).holdAnnotationDAO().insert(
                                    HoldAnnotation(
                                        0,
                                        photo.file.name,
                                        circleRadius.toFloat() / tmpBitmap.width,
                                        strokeWidth.toFloat() / tmpBitmap.width,
                                        circleCx.toFloat() / tmpBitmap.width,
                                        circleCy.toFloat() / tmpBitmap.width,
                                    )
                                )
                            }

                            for (annotationID in annotationDeleteList) {
                                App.getDB(this).holdAnnotationDAO().delete(annotationID)
                            }

                            drawAnnotations(tmpBitmap, photo)

                            runOnUiThread {
                                photoView.setImageBitmap(tmpBitmap)
                                photoView.setDisplayMatrix(matrix)
                            }
                        }
                    }
                }
            }
        }
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
