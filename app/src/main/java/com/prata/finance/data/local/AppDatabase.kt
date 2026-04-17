package com.prata.finance.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.prata.finance.data.local.dao.*
import com.prata.finance.data.local.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Database(
    entities = [
        User::class,
        Wallet::class,
        Category::class,
        Transaction::class
    ],
    version = 3,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun walletDao(): WalletDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao

    private class AppDatabaseCallback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
                scope.launch {
                    val userDao = database.userDao()
                    val walletDao = database.walletDao()
                    val categoryDao = database.categoryDao()

                    // 1. First, create the default mock User to satisfy Foreign Key constraints
                    val userId = 1L
                    userDao.insertUser(User(id = userId, name = "Prata", email = "prata@finance.com"))

                    // Wallets and Categories will be completely empty initially.
                }
            }
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "financial_tracker_database"
                )
                .addCallback(AppDatabaseCallback())
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
