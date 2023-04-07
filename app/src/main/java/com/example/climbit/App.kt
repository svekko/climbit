package com.example.climbit

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import androidx.room.Room
import com.example.climbit.database.Database

class App : Application() {
    companion object {
        @Volatile
        private lateinit var db: Database

        private var sharedPreferenceID = "climbItPreferences"
        private var sharedPreferenceMaskEnabled = "maskEnabled"

        fun getDB(ctx: Context): Database {
            synchronized(this) {
                if (!::db.isInitialized) {
                    db = Room.databaseBuilder(ctx, Database::class.java, "climb-it").build()
                    db.afterBuild()
                }

                return db
            }
        }

        @SuppressLint("ApplySharedPref")
        fun setMaskEnabled(ctx: Context, v: Boolean) {
            val pref = ctx.getSharedPreferences(sharedPreferenceID, 0)
            pref.edit().putBoolean(sharedPreferenceMaskEnabled, v).commit()
        }

        fun getMaskEnabled(ctx: Context): Boolean {
            val pref = ctx.getSharedPreferences(sharedPreferenceID, 0)
            return pref.getBoolean(sharedPreferenceMaskEnabled, true)
        }
    }

    override fun onCreate() {
        super.onCreate()
    }
}
