package com.pitstop.save.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

const val ROLE_ADMIN = "ADMIN"
const val ROLE_KASIR = "KASIR"

@Entity(tableName = "user")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val username: String,
    val password: String,
    val role: String // ADMIN or KASIR
)
