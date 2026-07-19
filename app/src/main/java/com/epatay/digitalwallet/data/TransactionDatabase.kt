package com.epatay.digitalwallet.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.epatay.digitalwallet.data.InvestmentItem
import com.epatay.digitalwallet.data.InvestmentDao

// 1. DEĞİŞİKLİK: entities listesine InvestmentItem::class eklendi ve versiyon 3 yapıldı.
@Database(entities = [Transaction::class, InvestmentItem::class], version = 4, exportSchema = false)
abstract class TransactionDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao

    // 2. DEĞİŞİKLİK: ViewModel'in aradığı investmentDao buraya eklendi!
    abstract fun investmentDao(): InvestmentDao

    companion object {
        @Volatile
        private var INSTANCE: TransactionDatabase? = null

        fun getDatabase(context: Context): TransactionDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TransactionDatabase::class.java,
                    "transaction_database"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
