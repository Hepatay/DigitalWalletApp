package com.epatay.digitalwallet.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    // 1. Yeni bir işlem (gelir veya gider) ekle
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction)

    // 2. İşlemi sil (Kaydırmalı silme için kullanacağız)
    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    // 3. Tüm işlemleri listelemek için (En son eklenen en üstte çıksın diye id DESC yaptık)
    @Query("SELECT * FROM transactions_table ORDER BY id DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    // 4. BÜYÜK GÜÇ: Sadece GELİR olanların toplamını otomatik hesapla
    // Tablo boşken null dönmesin diye Double? yapıyoruz.
    @Query("SELECT SUM(amount) FROM transactions_table WHERE type = 'INCOME'")
    fun getTotalIncome(): Flow<Double?>

    // 5. BÜYÜK GÜÇ: Sadece GİDER olanların toplamını otomatik hesapla
    @Query("SELECT SUM(amount) FROM transactions_table WHERE type = 'EXPENSE'")
    fun getTotalExpense(): Flow<Double?>
}