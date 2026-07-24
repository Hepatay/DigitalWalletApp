package com.epatay.digitalwallet.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringTransactionDao {

    @Query(
        "SELECT * FROM recurring_transactions_table " +
            "ORDER BY isActive DESC, dayOfMonth ASC, id ASC"
    )
    fun getAllRecurringTransactions():
        Flow<List<RecurringTransaction>>

    @Query(
        "SELECT * FROM recurring_transactions_table " +
            "WHERE isActive = 1 " +
            "ORDER BY dayOfMonth ASC, id ASC"
    )
    suspend fun getActiveRecurringTransactions():
        List<RecurringTransaction>

    @Query(
        "SELECT * FROM recurring_transactions_table " +
            "WHERE id = :id LIMIT 1"
    )
    suspend fun getById(
        id: Int
    ): RecurringTransaction?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(
        recurringTransaction: RecurringTransaction
    ): Long

    @Query(
        """
        UPDATE recurring_transactions_table
        SET
            title = :title,
            amount = :amount,
            category = :category,
            type = :type,
            dayOfMonth = :dayOfMonth,
            autoCreate = :autoCreate,
            notificationEnabled = :notificationEnabled,
            isActive = :isActive,
            lastNotifiedPeriod =
                CASE
                    WHEN
                        dayOfMonth != :dayOfMonth
                        OR (
                            notificationEnabled = 0
                            AND :notificationEnabled = 1
                        )
                    THEN NULL
                    ELSE lastNotifiedPeriod
                END
        WHERE id = :id
        """
    )
    suspend fun updateEditableFields(
        id: Int,
        title: String,
        amount: Double,
        category: String,
        type: TransactionType,
        dayOfMonth: Int,
        autoCreate: Boolean,
        notificationEnabled: Boolean,
        isActive: Boolean
    )

    @Query(
        """
        UPDATE recurring_transactions_table
        SET lastGeneratedPeriod = :periodKey
        WHERE id = :id
        """
    )
    suspend fun updateLastGeneratedPeriod(
        id: Int,
        periodKey: String
    )

    @Query(
        """
        UPDATE recurring_transactions_table
        SET lastNotifiedPeriod = :periodKey
        WHERE id = :id
        """
    )
    suspend fun updateLastNotifiedPeriod(
        id: Int,
        periodKey: String?
    )

    @Delete
    suspend fun delete(
        recurringTransaction: RecurringTransaction
    )
}
