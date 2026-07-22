package com.pitstop.save.dao

import androidx.room.*
import com.pitstop.save.entity.User

@Dao
interface UserDao {
    @Query("SELECT * FROM user WHERE username = :username AND password = :password LIMIT 1")
    suspend fun login(username: String, password: String): User?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(vararg users: User)

    @Query("SELECT COUNT(*) FROM user")
    suspend fun count(): Int
}
