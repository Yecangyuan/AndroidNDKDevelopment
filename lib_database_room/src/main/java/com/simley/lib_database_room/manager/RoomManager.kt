package com.simley.lib_database_room.manager

import android.app.Application
import androidx.room.Room
import androidx.room.RoomDatabase
import com.simley.lib_database_room.db.AppDatabase

object RoomManager {

    private var db: RoomDatabase? = null

    fun getDB(context: Application): AppDatabase {
        if (db == null) {
            db = Room.databaseBuilder(context, AppDatabase::class.java, "ccm_db")
                .enableMultiInstanceInvalidation()
                .build()
        }
        return db as AppDatabase
    }

}
