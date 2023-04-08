package com.example.climbit.activity

import android.app.Dialog
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Bundle
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
import com.example.climbit.livedata.PhotosDataModel
import com.example.climbit.livedata.WorkoutRouteDataModel
import com.example.climbit.model.HoldAnnotation
import com.example.climbit.model.WorkoutRouteBundle
import com.example.climbit.model.WorkoutSet
import com.example.climbit.photo.WorkoutRoutePhoto
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
    private var isFinished = true
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

        routeID?.also {
            PhotosDataModel(it).getPhotos().observe(this) { photos ->
                photosList.clear()
                photosList.addAll(photos)

                loadPhotos()
            }

            WorkoutRouteDataModel(this, it).getWorkoutRoute().observe(this) { bundle ->
                populateSetsListView(bundle)
            }
        }
    }

    private fun reloadActivity() {
        finish()
        startActivity(intent)
    }

    private fun loadPhotos() {
        var count = 0
        val photosView = findViewById<LinearLayout>(R.id.images)
        val photosScroll = findViewById<HorizontalScrollView>(R.id.images_scroll)
        val backgroundRunner = Executors.newFixedThreadPool(3)

        photosView.removeAllViews()

        Executors.newSingleThreadExecutor().execute {
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

                    photosView.addView(cardView)
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

            runOnUiThread {
                photosScroll.visibility = if (count == 0) {
                    View.GONE
                } else {
                    View.VISIBLE
                }
            }
        }
    }

    class BitmapAnnotation(private val annotation: HoldAnnotation, private val bitmap: Bitmap) {
        fun x(): Float {
            return annotation.x * bitmap.width
        }

        fun y(): Float {
            return annotation.y * bitmap.width
        }

        fun radius(): Float {
            return annotation.radius * bitmap.width
        }

        fun strokeWidth(): Float {
            return annotation.strokeWidth * bitmap.width
        }
    }

    private fun drawAnnotations(bitmap: Bitmap, photo: WorkoutRoutePhoto) {
        var maskBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        var maskCanvas = Canvas(maskBitmap)

        annotations.clear()
        annotations.addAll(App.getDB(this).holdAnnotationDAO().getAll(photo.file.name))

        // Skip drawing mask layer if there are no annotations.
        if (annotations.size > 0 && App.getMaskEnabled(this)) {
            val maskBG = Paint()
            maskBG.setARGB(85, 0, 0, 0)
            maskCanvas.drawPaint(maskBG)
        }

        // Remove masked from the parts where annotations are located.
        for (annotation in annotations) {
            val ann = BitmapAnnotation(annotation, bitmap)

            val paint = Paint()
            paint.color = Color.WHITE

            maskCanvas.drawCircle(ann.x(), ann.y(), ann.radius(), paint)
        }

        val finalCanvas = Canvas(bitmap)
        val maskPaint = Paint()

        maskPaint.blendMode = BlendMode.DARKEN
        finalCanvas.drawBitmap(maskBitmap, 0F, 0F, maskPaint)

        maskBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        maskCanvas = Canvas(maskBitmap)

        // Draw stroke pt. 1 (draw outer circle).
        for (annotation in annotations) {
            val ann = BitmapAnnotation(annotation, bitmap)

            val paint = Paint()
            paint.color = Color.WHITE

            maskCanvas.drawCircle(ann.x(), ann.y(), ann.radius() + ann.strokeWidth(), paint)
        }

        // Draw stroke pt. 2 (draw inner circle).
        for (annotation in annotations) {
            val ann = BitmapAnnotation(annotation, bitmap)

            val paint = Paint()
            paint.color = Color.BLACK

            maskCanvas.drawCircle(ann.x(), ann.y(), ann.radius(), paint)
        }

        maskPaint.blendMode = BlendMode.SCREEN
        maskPaint.alpha = 175
        finalCanvas.drawBitmap(maskBitmap, 0F, 0F, maskPaint)
    }

    private fun showPhotoFullScreen(index: Int, bitmap: Bitmap, wasEdited: Boolean) {
        photosList.getOrNull(index)?.also { photo ->
            val builder = AlertDialog.Builder(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
            val dialogView = layoutInflater.inflate(R.layout.dialog_image_enlarged, null, false)
            val photoView = dialogView.findViewById<PhotoView>(R.id.image)
            val menuView = dialogView.findViewById<ImageView>(R.id.menu)
            val menuContentView = dialogView.findViewById<LinearLayout>(R.id.menu_content)
            var edited = wasEdited

            photoView.setScaleLevels(1F, 5F, 10F)

            bitmap.copy(bitmap.config, true).also { tmpBitmap ->
                Executors.newSingleThreadExecutor().execute {
                    drawAnnotations(tmpBitmap, photo)

                    runOnUiThread {
                        photoView.setImageBitmap(tmpBitmap)
                    }
                }
            }

            val deleteButton = menuContentView.findViewById<TextView>(R.id.delete)

            if (isFinished) {
                deleteButton.visibility = View.GONE
            } else {
                deleteButton.setOnClickListener {
                    photo.file.delete()
                    reloadActivity()
                }
            }

            menuView.setOnClickListener {
                menuContentView.visibility = if (menuContentView.visibility == View.GONE) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
            }

            menuContentView.findViewById<TextView>(R.id.toggle_mask).setOnClickListener {
                edited = true

                Executors.newSingleThreadExecutor().execute {
                    bitmap.copy(bitmap.config, true)?.also { tmpBitmap ->
                        App.setMaskEnabled(this, !App.getMaskEnabled(this))
                        drawAnnotations(tmpBitmap, photo)

                        runOnUiThread {
                            photoView.setImageBitmap(tmpBitmap)
                        }
                    }
                }
            }

            menuContentView.findViewById<TextView>(R.id.open_externally).setOnClickListener {
                menuContentView.visibility = View.GONE

                val uri = FileProvider.getUriForFile(this, "com.example.android.fileprovider", photo.file)
                val intent = Intent();

                intent.action = Intent.ACTION_VIEW
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.setDataAndType(uri, "image/*");

                startActivity(intent);
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

            photoView.setOnScaleChangeListener { _, _, _ ->
                menuContentView.visibility = View.GONE
            }

            dialog.getButton(Dialog.BUTTON_POSITIVE).setOnClickListener {
                if (edited) {
                    reloadActivity()
                } else {
                    dialog.dismiss()
                }
            }

            if (isFinished) {
                photoView.setOnPhotoTapListener { _, _, _ ->
                    if (menuContentView.visibility == View.VISIBLE) {
                        menuContentView.visibility = View.GONE
                    }
                }
            } else {
                photoView.setOnPhotoTapListener { _, w, h ->
                    if (menuContentView.visibility == View.VISIBLE) {
                        menuContentView.visibility = View.GONE
                        return@setOnPhotoTapListener
                    }

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
                            val ann = BitmapAnnotation(annotation, tmpBitmap)
                            val circleAvgRadius = (circleRadius + ann.radius()) / 2

                            if (abs(ann.x() - circleCx) < circleAvgRadius && abs(ann.y() - circleCy) < circleAvgRadius) {
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
        val dir = PhotosDataModel.getPhotosDir()

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

    private fun populateSetsListView(bundle: WorkoutRouteBundle) {
        val workoutFinished = bundle.workout.dateFinished != null

        workoutID = bundle.route.workoutID
        isFinished = workoutFinished || (bundle.sets.isNotEmpty() && bundle.sets[0].finished)

        // Update rest timer every second.
        bundle.lastSet?.also { set ->
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

        val adapter = WorkoutSetArrayAdapter(this, workoutFinished, bundle.sets)
        val list = findViewById<RecyclerView>(R.id.sets_list)
        list.adapter = adapter

        // Can not modify if workout is finished or workout route has been marked complete.
        if (isFinished) {
            findViewById<ImageButton>(R.id.take_photo).visibility = View.GONE
            findViewById<Button>(R.id.add_attempt).visibility = View.GONE
            findViewById<Button>(R.id.route_completed).visibility = View.GONE
        }

        findViewById<TextView>(R.id.title).also { v ->
            v.text = bundle.route.name
        }

        var difficultyText = "${bundle.difficulty.name} ${getString(R.string.difficulty).lowercase()}"

        // Grade is not mandatory.
        bundle.grade?.also { g ->
            difficultyText = "$difficultyText (${g.fontScale})"
        }

        findViewById<TextView>(R.id.difficulty).also { v ->
            v.text = difficultyText
            v.setTextColor(Color.parseColor("#${bundle.difficulty.hexColor}"))
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
