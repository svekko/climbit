package com.example.climbit.util

import android.text.format.DateFormat
import java.util.*

object TimeUtil {
    fun formatDatetime(date: Date): CharSequence {
        return DateFormat.format("yyyy-MM-dd HH:mm:ss", date)
    }
}
