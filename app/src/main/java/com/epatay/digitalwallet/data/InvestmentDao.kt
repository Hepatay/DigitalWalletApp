package com.epatay.digitalwallet.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface InvestmentDao {
    @Query("UPDATE investments_table SET user_id = :uid, updated_at = :timestamp, is_synced = 0 WHERE user_id IS NULL OR user_id = 'guest'")
    suspend fun assignUserToGuestRecords(uid: String, timestamp: Long)

    @Query("UPDATE investments_table SET is_deleted = 1, is_synced = 0 WHERE uuid = :investmentId")
    suspend fun deleteInvestmentById(investmentId: String)

    @Query("DELETE FROM investments_table WHERE uuid = :investmentId")
    suspend fun hardDeleteInvestmentById(investmentId: String)


    @Query("SELECT * FROM investments_table WHERE is_deleted = 0 ORDER BY uuid DESC")
    fun observeAllInvestments(): Flow<List<InvestmentItem>>

    @Query("SELECT COUNT(*) FROM investments_table WHERE is_deleted = 0")
    fun observeCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM investments_table WHERE is_deleted = 0")
    suspend fun getCount(): Int

    // Tüm yatırımları en yeni kayıt üstte olacak şekilde getirir
    @Query("SELECT * FROM investments_table " +
                "WHERE is_deleted = 0 ORDER BY uuid DESC")
    fun getAllInvestments(): LiveData<List<InvestmentItem>>

    // Yeni yatırım ekler
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvestment(
        investment: InvestmentItem
    )

    @Query("SELECT * FROM investments_table ORDER BY uuid DESC")
    suspend fun getAllInvestmentsSnapshot(): List<InvestmentItem>

    @Query("SELECT * FROM investments_table")
    suspend fun getAllInvestmentsSync(): List<InvestmentItem>

    // Mevcut yatırımı aynı id üzerinden günceller
    @Update
    suspend fun updateInvestment(
        investment: InvestmentItem
    )

    // Mevcut yatırımı siler
    @Delete
    suspend fun deleteInvestment(
        investment: InvestmentItem
    )
    @Query("DELETE FROM investments_table WHERE uuid LIKE 'DEMO_TUTORIAL_%'")
    suspend fun clearDemoInvestments()

    @Query("DELETE FROM investments_table")
    suspend fun clearAll()
}
