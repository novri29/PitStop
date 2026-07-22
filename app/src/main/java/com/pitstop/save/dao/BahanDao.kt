package com.pitstop.save.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.pitstop.save.entity.Bahan

@Dao
interface BahanDao {
    @Query("SELECT * FROM bahan ORDER BY nama ASC")
    fun getAllLive(): LiveData<List<Bahan>>

    @Query("SELECT * FROM bahan ORDER BY nama ASC")
    suspend fun getAll(): List<Bahan>

    @Query("SELECT * FROM bahan WHERE id = :id")
    suspend fun getById(id: Int): Bahan?

    @Insert
    suspend fun insert(bahan: Bahan): Long

    @Update
    suspend fun update(bahan: Bahan)

    @Delete
    suspend fun delete(bahan: Bahan)

    @Query("UPDATE bahan SET stock = stock - :jumlah WHERE id = :id")
    suspend fun kurangiStock(id: Int, jumlah: Double)
}
