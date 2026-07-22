package com.pitstop.save.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.pitstop.save.entity.MenuKopi
import com.pitstop.save.entity.MenuKopiBahan

@Dao
interface MenuKopiDao {
    @Query("SELECT * FROM menu_kopi ORDER BY nama ASC")
    fun getAllLive(): LiveData<List<MenuKopi>>

    @Query("SELECT * FROM menu_kopi ORDER BY nama ASC")
    suspend fun getAll(): List<MenuKopi>

    @Insert
    suspend fun insertMenu(menuKopi: MenuKopi): Long

    @Update
    suspend fun updateMenu(menuKopi: MenuKopi)

    @Delete
    suspend fun deleteMenu(menuKopi: MenuKopi)

    @Insert
    suspend fun insertBahanUsage(usage: MenuKopiBahan)

    @Query("SELECT * FROM menu_kopi_bahan WHERE menuKopiId = :menuId")
    suspend fun getBahanUsageForMenu(menuId: Int): List<MenuKopiBahan>

    @Query("DELETE FROM menu_kopi_bahan WHERE menuKopiId = :menuId")
    suspend fun deleteBahanUsageForMenu(menuId: Int)

    @Query("SELECT * FROM menu_kopi_bahan")
    suspend fun getAllUsage(): List<MenuKopiBahan>

    @Query("""
        SELECT mb.menuKopiId as menuKopiId, b.nama as namaBahan, mb.jumlahDigunakan as jumlah, b.satuan as satuan
        FROM menu_kopi_bahan mb INNER JOIN bahan b ON b.id = mb.bahanId
    """)
    suspend fun getKomposisiRaw(): List<KomposisiRow>
}

data class KomposisiRow(
    val menuKopiId: Int,
    val namaBahan: String,
    val jumlah: Double,
    val satuan: String
)
