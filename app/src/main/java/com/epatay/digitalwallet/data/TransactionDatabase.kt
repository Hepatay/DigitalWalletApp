package com.epatay.digitalwallet.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// entity olarak artık Transaction::class kullanıyoruz.
// versiyonu artırdık ki eski tablo yapısıyla çakışmasın.
@Database(entities = [Transaction::class], version = 2, exportSchema = false)
abstract class TransactionDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao

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
                    // Tablo yapısını değiştirdiğimiz için eski verileri silip yeni tabloyu kurar.
                    // Geliştirme aşamasında olduğumuz için migration (göç) yazmakla uğraşmadan temiz bir başlangıç yapıyoruz.
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}