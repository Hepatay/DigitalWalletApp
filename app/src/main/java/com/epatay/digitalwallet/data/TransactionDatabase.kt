package com.epatay.digitalwallet.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
@Database(
    entities = [
        Transaction::class,
        InvestmentItem::class,
        RecurringTransaction::class,
        RecurringOccurrence::class,
        CategoryBudget::class,
        SavingsGoal::class,
        SavingsGoalEntry::class
    ],
    version = 8,
    exportSchema = true
)
abstract class TransactionDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao

    abstract fun investmentDao(): InvestmentDao

    abstract fun recurringTransactionDao():
        RecurringTransactionDao

    abstract fun recurringOccurrenceDao():
        RecurringOccurrenceDao

    abstract fun categoryBudgetDao():
        CategoryBudgetDao

    abstract fun savingsGoalDao():
        SavingsGoalDao

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

        private val MIGRATION_4_5 = object : Migration(4, 5) {
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

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `recurring_transactions_table` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `amount` REAL NOT NULL,
                        `category` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `dayOfMonth` INTEGER NOT NULL,
                        `autoCreate` INTEGER NOT NULL,
                        `notificationEnabled` INTEGER NOT NULL,
                        `isActive` INTEGER NOT NULL,
                        `lastGeneratedPeriod` TEXT,
                        `lastNotifiedPeriod` TEXT
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    ALTER TABLE `transactions_table`
                    ADD COLUMN `occurredOn` INTEGER NOT NULL DEFAULT 0
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    UPDATE `transactions_table`
                    SET `occurredOn` =
                        CASE
                            WHEN
                                LENGTH(`date`) >= 10
                                AND SUBSTR(`date`, 1, 10) GLOB
                                    '[0-9][0-9].[0-9][0-9].[0-9][0-9][0-9][0-9]'
                                AND CAST(SUBSTR(`date`, 7, 4) AS INTEGER)
                                    BETWEEN 1 AND 9999
                                AND CAST(SUBSTR(`date`, 4, 2) AS INTEGER)
                                    BETWEEN 1 AND 12
                                AND CAST(SUBSTR(`date`, 1, 2) AS INTEGER)
                                    BETWEEN 1 AND
                                    CASE
                                        WHEN CAST(
                                            SUBSTR(`date`, 4, 2)
                                            AS INTEGER
                                        ) IN (4, 6, 9, 11)
                                            THEN 30
                                        WHEN CAST(
                                            SUBSTR(`date`, 4, 2)
                                            AS INTEGER
                                        ) = 2
                                            THEN
                                                CASE
                                                    WHEN
                                                        CAST(
                                                            SUBSTR(`date`, 7, 4)
                                                            AS INTEGER
                                                        ) % 400 = 0
                                                        OR (
                                                            CAST(
                                                                SUBSTR(`date`, 7, 4)
                                                                AS INTEGER
                                                            ) % 4 = 0
                                                            AND CAST(
                                                                SUBSTR(`date`, 7, 4)
                                                                AS INTEGER
                                                            ) % 100 != 0
                                                        )
                                                        THEN 29
                                                    ELSE 28
                                                END
                                        ELSE 31
                                    END
                                THEN CAST(
                                    SUBSTR(`date`, 7, 4) ||
                                    SUBSTR(`date`, 4, 2) ||
                                    SUBSTR(`date`, 1, 2)
                                    AS INTEGER
                                )
                            ELSE 0
                        END
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                    `index_transactions_table_occurredOn`
                    ON `transactions_table` (`occurredOn`)
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                    `index_transactions_table_type_occurredOn`
                    ON `transactions_table` (`type`, `occurredOn`)
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                    `index_transactions_table_category_occurredOn`
                    ON `transactions_table` (`category`, `occurredOn`)
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `category_budgets` (
                        `monthKey` INTEGER NOT NULL,
                        `category` TEXT NOT NULL,
                        `limitAmount` REAL NOT NULL,
                        `updatedAtMillis` INTEGER NOT NULL,
                        PRIMARY KEY(`monthKey`, `category`)
                    )
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `savings_goals` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `targetAmount` REAL NOT NULL,
                        `targetDateKey` INTEGER,
                        `createdAtMillis` INTEGER NOT NULL,
                        `isArchived` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                    `index_savings_goals_isArchived`
                    ON `savings_goals` (`isArchived`)
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `savings_goal_entries` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `goalId` INTEGER NOT NULL,
                        `amountDelta` REAL NOT NULL,
                        `occurredOn` INTEGER NOT NULL,
                        `note` TEXT,
                        `createdAtMillis` INTEGER NOT NULL,
                        FOREIGN KEY(`goalId`)
                            REFERENCES `savings_goals`(`id`)
                            ON UPDATE NO ACTION
                            ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                    `index_savings_goal_entries_goalId_occurredOn`
                    ON `savings_goal_entries` (`goalId`, `occurredOn`)
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `recurring_occurrences` (
                        `recurringId` INTEGER NOT NULL,
                        `periodKey` TEXT NOT NULL,
                        `transactionId` INTEGER,
                        `createdAtMillis` INTEGER NOT NULL,
                        PRIMARY KEY(`recurringId`, `periodKey`),
                        FOREIGN KEY(`recurringId`)
                            REFERENCES `recurring_transactions_table`(`id`)
                            ON UPDATE NO ACTION
                            ON DELETE CASCADE
                    )
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                    `index_recurring_occurrences_transactionId`
                    ON `recurring_occurrences` (`transactionId`)
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    INSERT OR IGNORE INTO `recurring_occurrences` (
                        `recurringId`,
                        `periodKey`,
                        `transactionId`,
                        `createdAtMillis`
                    )
                    SELECT
                        `id`,
                        `lastGeneratedPeriod`,
                        NULL,
                        CAST(STRFTIME('%s', 'now') AS INTEGER) * 1000
                    FROM `recurring_transactions_table`
                    WHERE
                        `lastGeneratedPeriod` IS NOT NULL
                        AND `lastGeneratedPeriod` GLOB
                            '[0-9][0-9][0-9][0-9]-[0-9][0-9]'
                        AND CAST(
                            SUBSTR(`lastGeneratedPeriod`, 6, 2)
                            AS INTEGER
                        ) BETWEEN 1 AND 12
                    """.trimIndent()
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
                    .addMigrations(
                        MIGRATION_2_4,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8
                    )
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
