package com.example.climbit

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.example.climbit.database.Database

class App : Application() {
    companion object {
        @Volatile
        private lateinit var db: Database

        fun getDB(ctx: Context): Database {
            synchronized(this) {
                if (!::db.isInitialized) {
                    db = Room.databaseBuilder(ctx, Database::class.java, "climb-it").build()
                    db.afterBuild()
                }

                return db
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
    }
}
