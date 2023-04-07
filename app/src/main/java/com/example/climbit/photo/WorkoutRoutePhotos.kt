package com.example.climbit.photo

import android.os.Environment
import java.io.File
import java.nio.file.Paths

class WorkoutRoutePhotos(private val routeID: Long) {
    val photos = ArrayList<WorkoutRoutePhoto>()

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

            photos.addAll(photosResult.sortedWith(compareByDescending { it.file.name }))
        }
    }

    fun deleteAll() {
        for (photo in photos) {
            photo.file.delete()
        }
    }
}
