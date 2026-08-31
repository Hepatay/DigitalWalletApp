package com.epatay.digitalwallet.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RecurringOccurrenceDao {

    @Query("UPDATE recurring_occurrences SET user_id = :uid, updated_at = :timestamp, is_synced = 0 WHERE user_id IS NULL OR user_id = 'guest'")
    suspend fun assignUserToGuestRecords(uid: String, timestamp: Long)

    @Query("""SELECT EXISTS(
            SELECT 1
            FROM recurring_occurrences
            WHERE is_deleted = 0 AND
                recurringId = :recurringId
                AND periodKey = :periodKey
        )""")
    suspend fun exists(
        recurringId: String,
        periodKey: String
    ): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(
        occurrence: RecurringOccurrence
    ): Long

    @Query("UPDATE recurring_occurrences SET is_deleted = 1, is_synced = 0, updated_at = :timestamp WHERE recurringId = :recurringId AND periodKey = :periodKey")
    suspend fun delete(recurringId: String, periodKey: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM recurring_occurrences WHERE recurringId = :recurringId AND periodKey = :periodKey")
    suspend fun hardDelete(recurringId: String, periodKey: String)

    @Query("SELECT * FROM recurring_occurrences")
    suspend fun getAllSnapshot(): List<RecurringOccurrence>

    @Query("SELECT * FROM recurring_occurrences")
    suspend fun getAllSync(): List<RecurringOccurrence>

    @androidx.room.Update
    suspend fun update(occurrence: RecurringOccurrence)
    @Query("DELETE FROM recurring_occurrences")
    suspend fun clearAll()
}
