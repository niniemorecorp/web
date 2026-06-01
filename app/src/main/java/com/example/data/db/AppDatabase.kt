package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.Receipt
import com.example.data.model.User
import com.example.data.model.UserRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [Receipt::class, User::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun receiptDao(): ReceiptDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "resi_usaha_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }

        suspend fun prepopulate(context: Context, db: AppDatabase) {
            val userDao = db.userDao()
            val receiptDao = db.receiptDao()

            // 1. Seed Roles for Multi-user Access
            userDao.deleteAllUsers()
            userDao.insertUser(User("ninieowner", "Polkadot5.", "Owner", UserRole.OWNER))
            userDao.insertUser(User("admin", "Ninie123", "Admin",  UserRole.STAFF))

            // Clear all receipt transactions to satisfy "kosongkan transaksi resi"
            receiptDao.deleteAll()
        }
    }
}
