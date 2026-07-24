package com.epatay.digitalwallet.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RecurringOccurrenceDao {

    @Query(
        """
        SELECT EXISTS(
            SELECT 1
            FROM recurring_occurrences
            WHERE
                recurringId = :recurringId
                AND periodKey = :periodKey
        )
        """
    )
    suspend fun exists(
        recurringId: Int,
        periodKey: String
    ): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(
        occurrence: RecurringOccurrence
    ): Long
}
