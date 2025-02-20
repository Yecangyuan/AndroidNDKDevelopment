package com.simley.lib_database_room.db.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity
data class User(
    @PrimaryKey(autoGenerate = true)
    var uid: Int = 1, var name: String? = ""
) : Serializable
