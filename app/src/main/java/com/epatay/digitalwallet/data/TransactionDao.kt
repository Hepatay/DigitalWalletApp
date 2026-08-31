package com.epatay.digitalwallet.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class MonthlyTransactionTotals(
    val totalIncome: Double,
    val totalExpense: Double,
    val transactionCount: Int
) {
    val balance: Double
        get() = totalIncome - totalExpense
}

data class CategoryTransactionTotal(
    val category: String,
    val totalAmount: Double,
    val transactionCount: Int
)

@Dao
interface TransactionDao {

    // Yeni gelir veya gider ekler
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(
        transaction: Transaction
    )

    // Mevcut gelir veya gider kaydını günceller
    @Update
    suspend fun updateTransaction(
        transaction: Transaction
    )

    // İşlemi siler
    @Delete
    suspend fun deleteTransaction(
        transaction: Transaction
    )

    // Tüm işlemleri en yeni kayıt üstte olacak şekilde getirir
    @Query(
        "SELECT * FROM transactions_table " +
                "WHERE is_deleted = 0 ORDER BY occurredOn DESC, updated_at DESC, uuid DESC"
    )
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query(
        "SELECT * FROM transactions_table " +
                "WHERE is_deleted = 0 ORDER BY occurredOn DESC, updated_at DESC, uuid DESC"
    )
    suspend fun getAllTransactionsSnapshot(): List<Transaction>

    @Query("SELECT * FROM transactions_table")
    suspend fun getAllTransactionsSync(): List<Transaction>

    // Toplam geliri hesaplar
    @Query(
        "SELECT SUM(amount) FROM transactions_table " +
                "WHERE is_deleted = 0 AND type = 'INCOME'"
    )
    fun getTotalIncome(): Flow<Double?>

    // Toplam gideri hesaplar
    @Query(
        "SELECT SUM(amount) FROM transactions_table " +
                "WHERE is_deleted = 0 AND type = 'EXPENSE'"
    )
    fun getTotalExpense(): Flow<Double?>

    @Query(
        """
        SELECT *
        FROM transactions_table
        WHERE
            is_deleted = 0
            AND (:startDateKey IS NULL OR occurredOn >= :startDateKey)
            AND (:endDateKey IS NULL OR occurredOn <= :endDateKey)
            AND (:category IS NULL OR category = :category)
            AND (:type IS NULL OR type = :type)
            AND (
                :escapedQuery = ''
                OR title COLLATE NOCASE
                    LIKE '%' || :escapedQuery || '%' ESCAPE '\'
                OR category COLLATE NOCASE
                    LIKE '%' || :escapedQuery || '%' ESCAPE '\'
            )
        ORDER BY occurredOn DESC, updated_at DESC, uuid DESC
        """
    )
    fun observeFilteredTransactions(
        escapedQuery: String,
        startDateKey: Int?,
        endDateKey: Int?,
        category: String?,
        type: TransactionType?
    ): Flow<List<Transaction>>

    @Query(
        """
        SELECT *
        FROM transactions_table
        WHERE
            is_deleted = 0
            AND (:startDateKey IS NULL OR occurredOn >= :startDateKey)
            AND (:endDateKey IS NULL OR occurredOn <= :endDateKey)
            AND (:category IS NULL OR category = :category)
            AND (:type IS NULL OR type = :type)
            AND (
                :escapedQuery = ''
                OR title COLLATE NOCASE
                    LIKE '%' || :escapedQuery || '%' ESCAPE '\'
                OR category COLLATE NOCASE
                    LIKE '%' || :escapedQuery || '%' ESCAPE '\'
            )
        ORDER BY occurredOn DESC, updated_at DESC, uuid DESC
        """
    )
    suspend fun getFilteredTransactionsSnapshot(
        escapedQuery: String,
        startDateKey: Int?,
        endDateKey: Int?,
        category: String?,
        type: TransactionType?
    ): List<Transaction>

    @Query(
        """
        SELECT
            COALESCE(
                SUM(CASE WHEN type = 'INCOME' THEN amount ELSE 0 END),
                0
            ) AS totalIncome,
            COALESCE(
                SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0 END),
                0
            ) AS totalExpense,
            COUNT(*) AS transactionCount
        FROM transactions_table
        WHERE is_deleted = 0 AND occurredOn BETWEEN :startDateKey AND :endDateKey
        """
    )
    fun observeMonthlyTotals(
        startDateKey: Int,
        endDateKey: Int
    ): Flow<MonthlyTransactionTotals>

    @Query(
        """
        SELECT
            category,
            COALESCE(SUM(amount), 0) AS totalAmount,
            COUNT(*) AS transactionCount
        FROM transactions_table
        WHERE
            is_deleted = 0
            AND type = :type
            AND occurredOn BETWEEN :startDateKey AND :endDateKey
        GROUP BY category
        ORDER BY totalAmount DESC, category COLLATE NOCASE ASC
        """
    )
    fun observeCategoryTotals(
        startDateKey: Int,
        endDateKey: Int,
        type: TransactionType
    ): Flow<List<CategoryTransactionTotal>>

    @Query(
        """
        SELECT DISTINCT category
        FROM transactions_table
        WHERE is_deleted = 0 AND TRIM(category) != ''
        ORDER BY category COLLATE NOCASE ASC
        """
    )
    fun observeCategories(): Flow<List<String>>

    @Query(
        "SELECT COUNT(*) FROM transactions_table WHERE is_deleted = 0 AND occurredOn = 0"
    )
    fun observeUnknownDateCount(): Flow<Int>

    @Query(
        "SELECT COUNT(*) FROM transactions_table WHERE is_deleted = 0 AND occurredOn BETWEEN :startDateKey AND :endDateKey"
    )
    fun observeCurrentMonthTransactionCount(startDateKey: Int, endDateKey: Int): Flow<Int>

    @Query(
        "SELECT COUNT(*) FROM transactions_table WHERE is_deleted = 0 AND occurredOn BETWEEN :startDateKey AND :endDateKey"
    )
    suspend fun getCurrentMonthTransactionCount(startDateKey: Int, endDateKey: Int): Int
    @Query("DELETE FROM transactions_table")
    suspend fun clearAll()
    @Query("UPDATE transactions_table SET user_id = :userId, updated_at = :now, is_synced = 0 WHERE user_id IS NULL OR user_id = 'guest'")
    suspend fun assignUserToGuestRecords(userId: String, now: Long)

    @Query("DELETE FROM transactions_table WHERE uuid = :uuid")
    suspend fun hardDeleteTransactionById(uuid: String)

    @Query("DELETE FROM transactions_table WHERE uuid LIKE 'DEMO_TUTORIAL_%'")
    suspend fun clearDemoTransactions()

    @Query("UPDATE transactions_table SET is_deleted = 1, is_synced = 0, updated_at = :timestamp WHERE uuid = :uuid")
    suspend fun deleteTransactionById(uuid: String, timestamp: Long = System.currentTimeMillis())
}
