package com.pitstop.save

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.pitstop.save.dao.BahanDao
import com.pitstop.save.dao.MenuKopiDao
import com.pitstop.save.dao.StockSteamDao
import com.pitstop.save.dao.TransaksiDao
import com.pitstop.save.dao.UserDao
import com.pitstop.save.entity.Bahan
import com.pitstop.save.entity.JENIS_MOBIL
import com.pitstop.save.entity.JENIS_MOTOR
import com.pitstop.save.entity.KATEGORI_COFFEE
import com.pitstop.save.entity.KATEGORI_NON_COFFEE
import com.pitstop.save.entity.KATEGORI_SNACK
import com.pitstop.save.entity.Layanan
import com.pitstop.save.entity.MenuKopi
import com.pitstop.save.entity.MenuKopiBahan
import com.pitstop.save.entity.ROLE_ADMIN
import com.pitstop.save.entity.ROLE_KASIR
import com.pitstop.save.entity.StockSteam
import com.pitstop.save.entity.Transaksi
import com.pitstop.save.entity.TransaksiDetail
import com.pitstop.save.entity.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        User::class,
        Bahan::class,
        MenuKopi::class,
        MenuKopiBahan::class,
        StockSteam::class,
        Layanan::class,
        Transaksi::class,
        TransaksiDetail::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun bahanDao(): BahanDao
    abstract fun menuKopiDao(): MenuKopiDao
    abstract fun stockSteamDao(): StockSteamDao
    abstract fun transaksiDao(): TransaksiDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cafesteam.db"
                ).addCallback(seedCallback(context))
                    // Skema baru (kolom gambarPath) belum punya migration tertulis;
                    // untuk tahap development ini aman -> DB lama otomatis dibuat ulang.
                    // Kalau sudah rilis ke pengguna nyata, ganti dengan Migration resmi.
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * Seed data awal: akun default admin/kasir dan harga layanan default,
         * supaya aplikasi bisa langsung dicoba tanpa setup manual.
         */
        private fun seedCallback(context: Context) = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                CoroutineScope(Dispatchers.IO).launch {
                    val database = getInstance(context)
                    database.userDao().insertAll(
                        User(username = "admin", password = "admin123", role = ROLE_ADMIN),
                        User(username = "kasir", password = "kasir123", role = ROLE_KASIR)
                    )
                    database.stockSteamDao().insertLayanan(
                        Layanan(nama = "Cuci Motor", jenis = JENIS_MOTOR, harga = 12000.0)
                    )
                    database.stockSteamDao().insertLayanan(
                        Layanan(nama = "Cuci Mobil", jenis = JENIS_MOBIL, harga = 35000.0)
                    )

                    val bahanDao = database.bahanDao()
                    bahanDao.insert(Bahan(nama = "Biji Kopi Arabica", satuan = "gram", stock = 1000.0, hargaPerSatuan = 120.0))
                    bahanDao.insert(Bahan(nama = "Biji Kopi Robusta", satuan = "gram", stock = 500.0, hargaPerSatuan = 90.0))
                    bahanDao.insert(Bahan(nama = "Gula Pasir", satuan = "gram", stock = 1000.0, hargaPerSatuan = 15.0))
                    bahanDao.insert(Bahan(nama = "Susu UHT Full Cream", satuan = "ml", stock = 1000.0, hargaPerSatuan = 18.0))
                    bahanDao.insert(Bahan(nama = "Susu Kental Manis", satuan = "ml", stock = 500.0, hargaPerSatuan = 22.0))
                    bahanDao.insert(Bahan(nama = "Coklat Bubuk", satuan = "gram", stock = 200.0, hargaPerSatuan = 35.0))

                    val menuDao = database.menuKopiDao()
                    menuDao.insertMenu(MenuKopi(nama = "Kopi Susu", kategori = KATEGORI_COFFEE, hargaModal = 1850.0, hargaJual = 15000.0))
                    menuDao.insertMenu(MenuKopi(nama = "Americano", kategori = KATEGORI_COFFEE, hargaModal = 1200.0, hargaJual = 12000.0))
                    menuDao.insertMenu(MenuKopi(nama = "Cappuccino", kategori = KATEGORI_COFFEE, hargaModal = 2200.0, hargaJual = 16000.0))
                    menuDao.insertMenu(MenuKopi(nama = "Latte", kategori = KATEGORI_COFFEE, hargaModal = 2300.0, hargaJual = 16000.0))
                    menuDao.insertMenu(MenuKopi(nama = "Mochaccino", kategori = KATEGORI_COFFEE, hargaModal = 2500.0, hargaJual = 17000.0))
                    menuDao.insertMenu(MenuKopi(nama = "Caramel Macchiato", kategori = KATEGORI_COFFEE, hargaModal = 2400.0, hargaJual = 16000.0))
                    menuDao.insertMenu(MenuKopi(nama = "Es Teh", kategori = KATEGORI_NON_COFFEE, hargaModal = 1000.0, hargaJual = 5000.0))
                    menuDao.insertMenu(MenuKopi(nama = "Chocolate", kategori = KATEGORI_NON_COFFEE, hargaModal = 3500.0, hargaJual = 13000.0))
                    menuDao.insertMenu(MenuKopi(nama = "Croissant", kategori = KATEGORI_SNACK, hargaModal = 6000.0, hargaJual = 12000.0))
                }
            }
        }
    }
}
