package com.pitstop.save


import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BahanStokDao {
    @Insert
    suspend fun insert(bahan: BahanStokEntity): Long

    @Query("SELECT * FROM bahan_stok ORDER BY tanggalDitambahkan DESC")
    fun getAll(): Flow<List<BahanStokEntity>>
}
