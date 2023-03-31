package com.example.climbit.view

import android.view.MotionEvent
import com.github.chrisbanes.photoview.OnSingleFlingListener
import kotlin.math.abs

class SwipeListener : OnSingleFlingListener {
    var onLeftSwipe: Runnable? = null
    var onRightSwipe: Runnable? = null
    var onTopSwipe: Runnable? = null
    var onBottomSwipe: Runnable? = null

    private val swipeThreshold = 100

    override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
        val diffX = e2.x - e1.x
        val diffY = e2.y - e1.y

        if (abs(diffX) > abs(diffY)) {
            if (abs(diffX) > swipeThreshold && abs(velocityX) > swipeThreshold) {
                if (diffX > 0) {
                    onRightSwipe?.also { it.run() }
                } else {
                    onLeftSwipe?.also { it.run() }
                }

                return true
            }
        } else if (abs(diffY) > swipeThreshold && abs(velocityY) > swipeThreshold) {
            if (diffY > 0) {
                onBottomSwipe?.also { it.run() }
            } else {
                onTopSwipe?.also { it.run() }
            }

            return true
        }

        return false
    }
}
