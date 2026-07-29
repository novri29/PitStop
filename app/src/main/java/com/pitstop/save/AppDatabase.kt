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
    version = 4,
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
            override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
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

                    val bahanDao = database.bahanDao()

                    // --- Bahan racikan kopi (satuan gram/ml) ---
                    val idKopiArabica = bahanDao.insert(Bahan(nama = "Biji Kopi Arabica", satuan = "gram", stock = 1000.0, hargaPerSatuan = 120.0)).toInt()
                    val idKopiRobusta = bahanDao.insert(Bahan(nama = "Biji Kopi Robusta", satuan = "gram", stock = 500.0, hargaPerSatuan = 90.0)).toInt()
                    val idGula = bahanDao.insert(Bahan(nama = "Gula Pasir", satuan = "gram", stock = 1000.0, hargaPerSatuan = 15.0)).toInt()
                    val idSusuUht = bahanDao.insert(Bahan(nama = "Susu UHT Full Cream", satuan = "ml", stock = 1000.0, hargaPerSatuan = 18.0)).toInt()
                    val idSusuKental = bahanDao.insert(Bahan(nama = "Susu Kental Manis", satuan = "ml", stock = 500.0, hargaPerSatuan = 22.0)).toInt()
                    val idCoklatBubuk = bahanDao.insert(Bahan(nama = "Coklat Bubuk", satuan = "gram", stock = 200.0, hargaPerSatuan = 35.0)).toInt()

                    // --- Bahan/stock untuk menu Non Coffee (gram/ml) ---
                    val idTehCelup = bahanDao.insert(Bahan(nama = "Teh Celup", satuan = "pcs", stock = 100.0, hargaPerSatuan = 500.0)).toInt()
                    val idMatchaBubuk = bahanDao.insert(Bahan(nama = "Matcha Bubuk", satuan = "gram", stock = 500.0, hargaPerSatuan = 150.0)).toInt()

                    // --- Stock barang jadi satuan pcs: air mineral & snack (Non Coffee / Snack) ---
                    val idAirMineral = bahanDao.insert(Bahan(nama = "Air Mineral 600ml", satuan = "pcs", stock = 100.0, hargaPerSatuan = 2500.0)).toInt()
                    val idCroissant = bahanDao.insert(Bahan(nama = "Croissant", satuan = "pcs", stock = 30.0, hargaPerSatuan = 6000.0)).toInt()
                    val idKeripik = bahanDao.insert(Bahan(nama = "Keripik Kentang", satuan = "pcs", stock = 50.0, hargaPerSatuan = 5000.0)).toInt()
                    val idWafer = bahanDao.insert(Bahan(nama = "Wafer Coklat", satuan = "pcs", stock = 50.0, hargaPerSatuan = 3000.0)).toInt()

                    val menuDao = database.menuKopiDao()

                    // --- Menu Coffee ---
                    menuDao.insertMenu(MenuKopi(nama = "Kopi Susu", kategori = KATEGORI_COFFEE, hargaModal = 1850.0, hargaJual = 15000.0))
                    menuDao.insertMenu(MenuKopi(nama = "Americano", kategori = KATEGORI_COFFEE, hargaModal = 1200.0, hargaJual = 12000.0))
                    menuDao.insertMenu(MenuKopi(nama = "Cappuccino", kategori = KATEGORI_COFFEE, hargaModal = 2200.0, hargaJual = 16000.0))
                    menuDao.insertMenu(MenuKopi(nama = "Latte", kategori = KATEGORI_COFFEE, hargaModal = 2300.0, hargaJual = 16000.0))
                    menuDao.insertMenu(MenuKopi(nama = "Mochaccino", kategori = KATEGORI_COFFEE, hargaModal = 2500.0, hargaJual = 17000.0))
                    menuDao.insertMenu(MenuKopi(nama = "Caramel Macchiato", kategori = KATEGORI_COFFEE, hargaModal = 2400.0, hargaJual = 16000.0))

                    // --- Menu Non Coffee (dengan resep, supaya stock ikut terpotong saat terjual) ---
                    val idMenuEsTeh = menuDao.insertMenu(
                        MenuKopi(nama = "Es Teh", kategori = KATEGORI_NON_COFFEE, hargaModal = 650.0, hargaJual = 5000.0)
                    ).toInt()
                    menuDao.insertBahanUsage(MenuKopiBahan(menuKopiId = idMenuEsTeh, bahanId = idTehCelup, jumlahDigunakan = 1.0))
                    menuDao.insertBahanUsage(MenuKopiBahan(menuKopiId = idMenuEsTeh, bahanId = idGula, jumlahDigunakan = 10.0))

                    val idMenuChocolate = menuDao.insertMenu(
                        MenuKopi(nama = "Chocolate", kategori = KATEGORI_NON_COFFEE, hargaModal = 2850.0, hargaJual = 13000.0)
                    ).toInt()
                    menuDao.insertBahanUsage(MenuKopiBahan(menuKopiId = idMenuChocolate, bahanId = idCoklatBubuk, jumlahDigunakan = 30.0))
                    menuDao.insertBahanUsage(MenuKopiBahan(menuKopiId = idMenuChocolate, bahanId = idSusuUht, jumlahDigunakan = 100.0))

                    val idMenuMatcha = menuDao.insertMenu(
                        MenuKopi(nama = "Matcha Latte", kategori = KATEGORI_NON_COFFEE, hargaModal = 5100.0, hargaJual = 18000.0)
                    ).toInt()
                    menuDao.insertBahanUsage(MenuKopiBahan(menuKopiId = idMenuMatcha, bahanId = idMatchaBubuk, jumlahDigunakan = 15.0))
                    menuDao.insertBahanUsage(MenuKopiBahan(menuKopiId = idMenuMatcha, bahanId = idSusuUht, jumlahDigunakan = 150.0))
                    menuDao.insertBahanUsage(MenuKopiBahan(menuKopiId = idMenuMatcha, bahanId = idGula, jumlahDigunakan = 10.0))

                    val idMenuAirMineral = menuDao.insertMenu(
                        MenuKopi(nama = "Air Mineral 600ml", kategori = KATEGORI_NON_COFFEE, hargaModal = 2500.0, hargaJual = 5000.0)
                    ).toInt()
                    menuDao.insertBahanUsage(MenuKopiBahan(menuKopiId = idMenuAirMineral, bahanId = idAirMineral, jumlahDigunakan = 1.0))

                    // --- Menu Snack (masing-masing terhubung ke stock pcs-nya sendiri) ---
                    val idMenuCroissant = menuDao.insertMenu(
                        MenuKopi(nama = "Croissant", kategori = KATEGORI_SNACK, hargaModal = 6000.0, hargaJual = 12000.0)
                    ).toInt()
                    menuDao.insertBahanUsage(MenuKopiBahan(menuKopiId = idMenuCroissant, bahanId = idCroissant, jumlahDigunakan = 1.0))

                    val idMenuKeripik = menuDao.insertMenu(
                        MenuKopi(nama = "Keripik Kentang", kategori = KATEGORI_SNACK, hargaModal = 5000.0, hargaJual = 10000.0)
                    ).toInt()
                    menuDao.insertBahanUsage(MenuKopiBahan(menuKopiId = idMenuKeripik, bahanId = idKeripik, jumlahDigunakan = 1.0))

                    val idMenuWafer = menuDao.insertMenu(
                        MenuKopi(nama = "Wafer Coklat", kategori = KATEGORI_SNACK, hargaModal = 3000.0, hargaJual = 7000.0)
                    ).toInt()
                    menuDao.insertBahanUsage(MenuKopiBahan(menuKopiId = idMenuWafer, bahanId = idWafer, jumlahDigunakan = 1.0))
                }
            }
        }
    }
}