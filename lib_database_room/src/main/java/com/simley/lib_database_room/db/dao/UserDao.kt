package com.simley.lib_database_room.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.simley.lib_database_room.db.model.User

@Dao
interface UserDao {

    @Query("SELECT * FROM user")
    suspend fun getAll(): List<User>

    @Query("SELECT * FROM user WHERE uid in (:userIds)")
    suspend fun loadAllByIds(userIds: IntArray): List<User>

    @Query("SELECT * FROM user WHERE name LIKE :userName LIMIT 1")
    suspend fun findByName(userName: String): User

    @Query("SELECT * FROM user WHERE uid = :uid")
    suspend fun findByUid(uid: Int): User

    @Insert
    suspend fun insertAll(vararg users: User)

    @Delete
    suspend fun delete(user: User)

    @Update
    suspend fun updateUser(user: User)

}
