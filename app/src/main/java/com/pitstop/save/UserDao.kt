package com.pitstop.save

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pitstop.save.UserEntity

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE username = :username AND password = :password AND role = :role LIMIT 1")
    suspend fun login(username: String, password: String, role: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUserCount(): Int
}