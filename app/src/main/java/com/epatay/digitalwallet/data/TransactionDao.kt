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
                "ORDER BY id DESC"
    )
    fun getAllTransactions(): Flow<List<Transaction>>

    // Toplam geliri hesaplar
    @Query(
        "SELECT SUM(amount) FROM transactions_table " +
                "WHERE type = 'INCOME'"
    )
    fun getTotalIncome(): Flow<Double?>

    // Toplam gideri hesaplar
    @Query(
        "SELECT SUM(amount) FROM transactions_table " +
                "WHERE type = 'EXPENSE'"
    )
    fun getTotalExpense(): Flow<Double?>

    @Query(
        """
        SELECT *
        FROM transactions_table
        WHERE
            (:startDateKey IS NULL OR occurredOn >= :startDateKey)
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
        ORDER BY occurredOn DESC, id DESC
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
            (:startDateKey IS NULL OR occurredOn >= :startDateKey)
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
        ORDER BY occurredOn DESC, id DESC
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
        WHERE occurredOn BETWEEN :startDateKey AND :endDateKey
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
            type = :type
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
        WHERE TRIM(category) != ''
        ORDER BY category COLLATE NOCASE ASC
        """
    )
    fun observeCategories(): Flow<List<String>>

    @Query(
        "SELECT COUNT(*) FROM transactions_table WHERE occurredOn = 0"
    )
    fun observeUnknownDateCount(): Flow<Int>
}
