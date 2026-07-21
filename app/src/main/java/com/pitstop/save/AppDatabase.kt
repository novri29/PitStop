package com.pitstop.save

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [UserEntity::class, BahanStokEntity::class],
    version = 2, // dinaikkan dari 1 karena ada tabel baru (bahan_stok)
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun bahanStokDao(): BahanStokDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "clean_and_cup_db"
                )
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Seed data default akun Admin dan Kasir
                            INSTANCE?.let { database ->
                                CoroutineScope(Dispatchers.IO).launch {
                                    database.userDao().insertUser(
                                        UserEntity(username = "admin", password = "123", role = "ADMIN")
                                    )
                                    database.userDao().insertUser(
                                        UserEntity(username = "kasir", password = "123", role = "KASIR")
                                    )
                                }
                            }
                        }
                    })
                    // Karena masih tahap development: kalau skema berubah lagi nanti (nambah kolom/tabel),
                    // Room akan hapus & bikin ulang database daripada crash minta Migration.
                    // Hapus baris ini dan tulis Migration manual begitu app sudah rilis / datanya tidak boleh hilang.
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
