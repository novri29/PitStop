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
import com.pitstop.save.entity.JENIS_MOTOR
import com.pitstop.save.entity.KATEGORI_COFFEE
import com.pitstop.save.entity.KATEGORI_NON_COFFEE
import com.pitstop.save.entity.KATEGORI_SNACK
import com.pitstop.save.entity.Layanan
import com.pitstop.save.entity.LayananBahan
import com.pitstop.save.entity.MenuKopi
import com.pitstop.save.entity.MenuKopiBahan
import com.pitstop.save.entity.ROLE_ADMIN
import com.pitstop.save.entity.ROLE_KASIR
import com.pitstop.save.entity.StockSteam
import com.pitstop.save.entity.Transaksi
import com.pitstop.save.entity.TransaksiDetail
import com.pitstop.save.entity.UKURAN_MOTOR_BESAR
import com.pitstop.save.entity.UKURAN_MOTOR_KECIL
import com.pitstop.save.entity.UKURAN_MOTOR_SEDANG
import com.pitstop.save.entity.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.room.migration.Migration

@Database(
    entities = [
        User::class,
        Bahan::class,
        MenuKopi::class,
        MenuKopiBahan::class,
        StockSteam::class,
        Layanan::class,
        LayananBahan::class,
        Transaksi::class,
        TransaksiDetail::class
    ],
    version = 11,
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

        private val MIGRATION_7_8 = object : Migration(7, 8) {

            override fun migrate(
                database: SupportSQLiteDatabase
            ) {

                database.execSQL(
                    """
            ALTER TABLE bahan
            ADD COLUMN initialStock REAL NOT NULL DEFAULT 0
            """.trimIndent()
                )

                database.execSQL(
                    """
            UPDATE bahan
            SET initialStock = stock
            """.trimIndent()
                )
            }
        }

        /**
         * Migrasi untuk fitur Laba Bersih (Omzet - Modal):
         * - transaksi_detail.hargaModal: snapshot harga modal PER UNIT saat item terjual
         *   (supaya laba periode lama tetap akurat walau harga modal berubah belakangan)
         * - layanan.hargaModal: modal per layanan Cuci Motor, dihitung otomatis dari komposisi
         *   StockSteam yang dipakai (mirip pola MenuKopi.hargaModal di sisi Cafe)
         * - stock_steam.hargaPerSatuan: harga modal per satuan barang steam (mirip Bahan.hargaPerSatuan)
         * Data transaksi LAMA (sebelum migrasi ini) otomatis dapat hargaModal = 0 (laba dianggap
         * penuh = omzet untuk transaksi lama, karena histori modal saat itu tidak tercatat).
         */
        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE transaksi_detail ADD COLUMN hargaModal REAL NOT NULL DEFAULT 0"
                )
                database.execSQL(
                    "ALTER TABLE layanan ADD COLUMN hargaModal REAL NOT NULL DEFAULT 0"
                )
                database.execSQL(
                    "ALTER TABLE stock_steam ADD COLUMN hargaPerSatuan REAL NOT NULL DEFAULT 0"
                )
            }
        }

        /**
         * Migrasi untuk fitur notifikasi Stock Steam Menipis (menyusul fitur yang sama
         * di Bahan/Cafe pada MIGRATION_7_8):
         * - stock_steam.initialStock: baseline "stok penuh" dipakai untuk menghitung ambang 30%.
         * Karena fitur ini baru pertama kali ada di versi ini, tidak ada baseline lama yang
         * "dirusak" seperti kasus MIGRATION_7_8 dulu — initialStock = stock saat migrasi adalah
         * titik awal yang wajar untuk SEMUA item Stock Steam yang sudah ada.
         */
        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE stock_steam ADD COLUMN initialStock REAL NOT NULL DEFAULT 0"
                )
                database.execSQL(
                    "UPDATE stock_steam SET initialStock = stock"
                )
            }
        }

        /**
         * Migrasi untuk fitur Upah Karyawan per ukuran motor, supaya ikut dihitung ke HPP:
         * - layanan.upahKaryawan: upah/jasa karyawan untuk 1x pengerjaan ukuran motor ini.
         * - layanan.hargaModal (HPP) sejak migrasi ini = total pemakaian StockSteam (bahan) + upahKaryawan.
         * Data lama otomatis dapat upahKaryawan = 0 (HPP lama tetap sama seperti sebelumnya,
         * hanya bahan, sampai admin mengisi upah karyawan untuk tiap ukuran).
         */
        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE layanan ADD COLUMN upahKaryawan REAL NOT NULL DEFAULT 0"
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cafesteam.db"
                ).addMigrations(MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11)
                    .addCallback(seedCallback(context))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private fun seedCallback(context: Context) = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                seedDatabase(context)
            }

            override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
                super.onDestructiveMigration(db)
                seedDatabase(context)
            }
        }

        /**
         * Seed data awal: akun default admin/kasir, layanan steam, bahan, dan menu
         */
        private fun seedDatabase(context: Context) {
            CoroutineScope(Dispatchers.IO).launch {
                val database = getInstance(context)

                // --- User default ---
                database.userDao().insertAll(
                    User(username = "admin", password = "admin123", role = ROLE_ADMIN),
                    User(username = "kasir", password = "kasir123", role = ROLE_KASIR)
                )

                // --- Layanan Steam ---
                val stockSteamDao = database.stockSteamDao()
                stockSteamDao.insertLayanan(
                    Layanan(nama = "Cuci Motor Kecil", ukuran = UKURAN_MOTOR_KECIL, harga = 35000.0)
                )
                stockSteamDao.insertLayanan(
                    Layanan(nama = "Cuci Motor Sedang", ukuran = UKURAN_MOTOR_SEDANG, harga = 45000.0)
                )
                val idLayananBesar = stockSteamDao.insertLayanan(
                    Layanan(nama = "Cuci Motor Besar", ukuran = UKURAN_MOTOR_BESAR, harga = 60000.0)
                ).toInt()

                // --- Stock barang steam ---
                val idSabunMotor = stockSteamDao.insert(
                    StockSteam(nama = "Sabun Motor", jenis = JENIS_MOTOR, satuan = "ml", stock = 5000.0)
                ).toInt()
                val idSemirBan = stockSteamDao.insert(
                    StockSteam(nama = "Semir Ban", jenis = JENIS_MOTOR, satuan = "ml", stock = 2000.0)
                ).toInt()

                stockSteamDao.insertLayananBahan(
                    LayananBahan(layananId = idLayananBesar, stockSteamId = idSabunMotor, jumlahDigunakan = 100.0)
                )
                stockSteamDao.insertLayananBahan(
                    LayananBahan(layananId = idLayananBesar, stockSteamId = idSemirBan, jumlahDigunakan = 30.0)
                )

                // --- Bahan Kopi & Bahan Baku ---
                val bahanDao = database.bahanDao()
                val idKopiArabica = bahanDao.insert(Bahan(nama = "Biji Kopi Arabica", satuan = "gram", stock = 1000.0, hargaPerSatuan = 120.0)).toInt()
                val idKopiRobusta = bahanDao.insert(Bahan(nama = "Biji Kopi Robusta", satuan = "gram", stock = 500.0, hargaPerSatuan = 90.0)).toInt()
                val idGula = bahanDao.insert(Bahan(nama = "Gula Pasir", satuan = "gram", stock = 1000.0, hargaPerSatuan = 15.0)).toInt()
                val idSusuUht = bahanDao.insert(Bahan(nama = "Susu UHT Full Cream", satuan = "ml", stock = 1000.0, hargaPerSatuan = 18.0)).toInt()
                val idSusuKental = bahanDao.insert(Bahan(nama = "Susu Kental Manis", satuan = "ml", stock = 500.0, hargaPerSatuan = 22.0)).toInt()
                val idCoklatBubuk = bahanDao.insert(Bahan(nama = "Coklat Bubuk", satuan = "gram", stock = 200.0, hargaPerSatuan = 35.0)).toInt()

                // --- Bahan Non Coffee & Snack ---
                val idTehCelup = bahanDao.insert(Bahan(nama = "Teh Celup", satuan = "pcs", stock = 100.0, hargaPerSatuan = 500.0)).toInt()
                val idMatchaBubuk = bahanDao.insert(Bahan(nama = "Matcha Bubuk", satuan = "gram", stock = 500.0, hargaPerSatuan = 150.0)).toInt()
                val idAirMineral = bahanDao.insert(Bahan(nama = "Air Mineral 600ml", satuan = "pcs", stock = 100.0, hargaPerSatuan = 2500.0)).toInt()
                val idCroissant = bahanDao.insert(Bahan(nama = "Croissant", satuan = "pcs", stock = 30.0, hargaPerSatuan = 6000.0)).toInt()
                val idKeripik = bahanDao.insert(Bahan(nama = "Keripik Kentang", satuan = "pcs", stock = 50.0, hargaPerSatuan = 5000.0)).toInt()
                val idWafer = bahanDao.insert(Bahan(nama = "Wafer Coklat", satuan = "pcs", stock = 50.0, hargaPerSatuan = 3000.0)).toInt()

                // --- Menu ---
                val menuDao = database.menuKopiDao()
                menuDao.insertMenu(MenuKopi(nama = "Kopi Susu", kategori = KATEGORI_COFFEE, hargaModal = 1850.0, hargaJual = 15000.0))
                menuDao.insertMenu(MenuKopi(nama = "Americano", kategori = KATEGORI_COFFEE, hargaModal = 1200.0, hargaJual = 12000.0))
                menuDao.insertMenu(MenuKopi(nama = "Cappuccino", kategori = KATEGORI_COFFEE, hargaModal = 2200.0, hargaJual = 16000.0))
                menuDao.insertMenu(MenuKopi(nama = "Latte", kategori = KATEGORI_COFFEE, hargaModal = 2300.0, hargaJual = 16000.0))
                menuDao.insertMenu(MenuKopi(nama = "Mochaccino", kategori = KATEGORI_COFFEE, hargaModal = 2500.0, hargaJual = 17000.0))
                menuDao.insertMenu(MenuKopi(nama = "Caramel Macchiato", kategori = KATEGORI_COFFEE, hargaModal = 2400.0, hargaJual = 16000.0))

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