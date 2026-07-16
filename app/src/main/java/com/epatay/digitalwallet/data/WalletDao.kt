package com.epatay.digitalwallet.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface WalletDao {
    // Tüm cüzdan bakiyelerini getirir
    @Query("SELECT * FROM wallet_table")
    suspend fun getAllBalances(): List<WalletItem>

    // Belirli bir döviz türünün (Örn: USD) bakiyesini getirir
    @Query("SELECT balance FROM wallet_table WHERE currencyCode = :code")
    suspend fun getBalanceByCode(code: String): Double?

    // Yeni bakiye ekler veya mevcut olanı günceller (REPLACE)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateBalance(walletItem: WalletItem)
}