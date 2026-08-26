package com.epatay.digitalwallet.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringTransactionDao {
    @Query("UPDATE recurring_transactions_table SET user_id = :uid, updated_at = :timestamp, is_synced = 0 WHERE user_id IS NULL OR user_id = 'guest'")
    suspend fun assignUserToGuestRecords(uid: String, timestamp: Long)

    @Update
    suspend fun update(recurringTransaction: RecurringTransaction)


    @Query("SELECT * FROM recurring_transactions_table " +
            "WHERE is_deleted = 0 ORDER BY isActive DESC, dayOfMonth ASC, uuid ASC")
    fun getAllRecurringTransactions():
        Flow<List<RecurringTransaction>>

    @Query("SELECT * FROM recurring_transactions_table " +
            "WHERE is_deleted = 0 AND isActive = 1 " +
            "ORDER BY dayOfMonth ASC, uuid ASC")
    suspend fun getActiveRecurringTransactions():
        List<RecurringTransaction>

    @Query("SELECT * FROM recurring_transactions_table " +
            "WHERE is_deleted = 0 AND uuid = :id LIMIT 1")
    suspend fun getById(
        id: String
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
        WHERE uuid = :id
        """
    )
    suspend fun updateEditableFields(
        id: String,
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
        WHERE uuid = :id
        """
    )
    suspend fun updateLastGeneratedPeriod(
        id: String,
        periodKey: String
    )

    @Query(
        """
        UPDATE recurring_transactions_table
        SET lastNotifiedPeriod = :periodKey
        WHERE uuid = :id
        """
    )
    suspend fun updateLastNotifiedPeriod(
        id: String,
        periodKey: String?
    )

    @Query("UPDATE recurring_transactions_table SET is_deleted = 1, is_synced = 0, updated_at = :timestamp WHERE uuid = :id")
    suspend fun delete(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM recurring_transactions_table WHERE uuid = :id")
    suspend fun hardDelete(id: String)

    @Query("SELECT * FROM recurring_transactions_table")
    suspend fun getAllSnapshot(): List<RecurringTransaction>

    @Query("SELECT * FROM recurring_transactions_table")
    suspend fun getAllSync(): List<RecurringTransaction>
    @Query("DELETE FROM recurring_transactions_table")
    suspend fun clearAll()
}
