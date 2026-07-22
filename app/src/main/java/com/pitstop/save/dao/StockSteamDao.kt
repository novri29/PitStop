package com.pitstop.save.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.pitstop.save.entity.Layanan
import com.pitstop.save.entity.StockSteam

@Dao
interface StockSteamDao {
    @Query("SELECT * FROM stock_steam ORDER BY jenis, nama ASC")
    fun getAllLive(): LiveData<List<StockSteam>>

    @Query("SELECT * FROM stock_steam ORDER BY jenis, nama ASC")
    suspend fun getAll(): List<StockSteam>

    @Insert
    suspend fun insert(item: StockSteam): Long

    @Update
    suspend fun update(item: StockSteam)

    @Delete
    suspend fun delete(item: StockSteam)

    @Query("SELECT * FROM layanan ORDER BY jenis ASC")
    fun getAllLayananLive(): LiveData<List<Layanan>>

    @Query("SELECT * FROM layanan ORDER BY jenis ASC")
    suspend fun getAllLayanan(): List<Layanan>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLayanan(layanan: Layanan)

    @Update
    suspend fun updateLayanan(layanan: Layanan)
}
