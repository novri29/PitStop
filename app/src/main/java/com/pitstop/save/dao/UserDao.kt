package com.pitstop.save.dao

import androidx.room.*
import com.pitstop.save.entity.User

@Dao
interface UserDao {
    @Query("SELECT * FROM user WHERE username = :username AND password = :password LIMIT 1")
    suspend fun login(username: String, password: String): User?

    @Query("SELECT * FROM user WHERE role = :role LIMIT 1")
    suspend fun getUserByRole(role: String): User?

    @Query("SELECT * FROM user WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): User?

    @Update
    suspend fun update(user: User)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(vararg users: User)

    @Query("SELECT COUNT(*) FROM user")
    suspend fun count(): Int
}
