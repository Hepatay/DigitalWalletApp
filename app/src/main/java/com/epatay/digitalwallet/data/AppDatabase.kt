package com.epatay.digitalwallet.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// DEĞİŞİKLİK 1: InvestmentItem tablosunu ekledik ve versiyonu 3 yaptık.
@Database(entities = [Expense::class, WalletItem::class, InvestmentItem::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun expenseDao(): ExpenseDao
    abstract fun walletDao(): WalletDao

    // DEĞİŞİKLİK 2: Yatırım DAO'muzu bağladık
    abstract fun investmentDao(): InvestmentDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "wallet_database"
                )
                    .fallbackToDestructiveMigration() // Versiyon 3'e çökmeden geçiş yapmak için
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}