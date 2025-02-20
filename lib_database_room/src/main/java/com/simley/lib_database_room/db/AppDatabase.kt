package com.simley.lib_database_room.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.simley.lib_database_room.db.dao.UserDao
import com.simley.lib_database_room.db.model.User

@Database(entities = [User::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
}
