package com.pitstop.save

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pitstop.save.UserEntity

@Dao
interface UserDao {

    // 1. Tambahkan fungsi ini untuk mengecek apakah tabel user ada isinya
    @Query("SELECT * FROM users")
    suspend fun getAllUsers(): List<UserEntity>

    // 2. Fungsi login milikmu
    @Query("SELECT * FROM users WHERE LOWER(username) = LOWER(:username) AND password = :password AND role = :role LIMIT 1")
    suspend fun login(username: String, password: String, role: String): UserEntity?

    // 3. Fungsi insert user
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)
}