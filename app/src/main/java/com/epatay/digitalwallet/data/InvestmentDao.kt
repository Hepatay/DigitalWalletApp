package com.epatay.digitalwallet.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface InvestmentDao {
    // Tüm yatırımları getirir (Canlı olarak UI'ı güncellemek için LiveData kullanıyoruz)
    @Query("SELECT * FROM investments_table ORDER BY id DESC")
    fun getAllInvestments(): LiveData<List<InvestmentItem>>

    // Yeni bir yatırım ekler
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertInvestment(investment: InvestmentItem)

    // Mevcut bir yatırımı siler
    @Delete
    suspend fun deleteInvestment(investment: InvestmentItem)
}