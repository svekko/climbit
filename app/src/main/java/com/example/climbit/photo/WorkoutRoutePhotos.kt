package com.example.climbit.photo

import android.content.Context
import android.os.Environment

class WorkoutRoutePhotos(ctx: Context, private val routeID: Long) {
    val photos = ArrayList<WorkoutRoutePhoto>()

    init {
        ctx.getExternalFilesDir(Environment.DIRECTORY_PICTURES)?.also { dir ->
            dir.listFiles()?.also { files ->
                val photosResult: MutableList<WorkoutRoutePhoto> = arrayListOf()

                for (file in files) {
                    // Temporary image with no content.
                    if (file.length() == 0L) {
                        file.delete()
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
    }

    fun deleteAll() {
        for (photo in photos) {
            photo.file.delete()
        }
    }
}
