package com.epatay.digitalwallet.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

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
}