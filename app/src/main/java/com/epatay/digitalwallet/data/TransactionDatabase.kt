package com.epatay.digitalwallet.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
@Database(entities = [Transaction::class, InvestmentItem::class], version = 4, exportSchema = true)
abstract class TransactionDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao

    abstract fun investmentDao(): InvestmentDao

    companion object {
        private val MIGRATION_2_4 = object : Migration(2, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `investments_table` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `assetName` TEXT NOT NULL,
                        `amount` REAL NOT NULL,
                        `buyPrice` REAL NOT NULL,
                        `buyDate` TEXT NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DROP TABLE IF EXISTS `investments_table_new`")
                database.execSQL(
                    """
                    CREATE TABLE `investments_table_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `assetName` TEXT NOT NULL,
                        `amount` REAL NOT NULL,
                        `buyPrice` REAL NOT NULL,
                        `buyDate` TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    INSERT INTO `investments_table_new`
                        (`id`, `assetName`, `amount`, `buyPrice`, `buyDate`)
                    SELECT
                        `id`, `assetName`, CAST(`amount` AS REAL), `buyPrice`, `buyDate`
                    FROM `investments_table`
                    """.trimIndent()
                )
                database.execSQL("DROP TABLE `investments_table`")
                database.execSQL(
                    "ALTER TABLE `investments_table_new` RENAME TO `investments_table`"
                )
            }
        }

        @Volatile
        private var INSTANCE: TransactionDatabase? = null

        fun getDatabase(context: Context): TransactionDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TransactionDatabase::class.java,
                    "transaction_database"
                )
                    .addMigrations(MIGRATION_2_4, MIGRATION_3_4)
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
