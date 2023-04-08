package com.example.climbit.livedata

import android.os.Environment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.climbit.photo.WorkoutRoutePhoto
import java.io.File
import java.nio.file.Paths
import java.util.concurrent.Executors

class PhotosDataModel(private val routeID: Long) : ViewModel() {
    private val photos = MutableLiveData<List<WorkoutRoutePhoto>>()

    companion object {
        fun getPhotosDir(): File {
            val base = Environment.getExternalStorageDirectory().absolutePath
            val dir = Paths.get(base, Environment.DIRECTORY_PICTURES, "ClimbIt").toFile()

            if (!dir.exists()) {
                dir.mkdirs()
            }

            return dir
        }
    }

    init {
        Executors.newSingleThreadExecutor().execute {
            getPhotosDir().listFiles()?.also { files ->
                val photosResult: MutableList<WorkoutRoutePhoto> = arrayListOf()

                for (file in files) {
                    // Temporary image with no content.
                    if (file.length() == 0L) {
                        file.delete()
                        continue
                    }

                    if (!file.name.contains("___")) {
                        continue
                    }

                    WorkoutRoutePhoto(file).also { photo ->
                        if (photo.routeID == routeID) {
                            photosResult.add(photo)
                        }
                    }
                }

                photos.postValue(photosResult.sortedWith(compareByDescending { it.file.name }))
            }
        }
    }

    fun getPhotos(): LiveData<List<WorkoutRoutePhoto>> {
        return photos
    }

    fun deleteAll(owner: LifecycleOwner) {
        photos.observe(owner) { p ->
            for (photo in p) {
                photo.file.delete()
            }
        }
    }
}
