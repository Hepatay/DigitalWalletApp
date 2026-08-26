package com.epatay.digitalwallet.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface WalletDao {
    @Query("UPDATE wallet_table SET user_id = :uid, updated_at = :timestamp, is_synced = 0 WHERE user_id IS NULL OR user_id = 'guest'")
    suspend fun assignUserToGuestRecords(uid: String, timestamp: Long)

    // Tüm cüzdan bakiyelerini getirir
    @Query("SELECT * FROM wallet_table WHERE is_deleted = 0")
    suspend fun getAllBalances(): List<WalletItem>

    // Belirli bir döviz türünün (Örn: USD) bakiyesini getirir
    @Query("SELECT balance FROM wallet_table WHERE is_deleted = 0 AND currencyCode = :code")
    suspend fun getBalanceByCode(code: String): Double?

    // Yeni bakiye ekler veya mevcut olanı günceller (REPLACE)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateBalance(walletItem: WalletItem)
    @Query("DELETE FROM wallet_table")
    suspend fun clearAll()
}
