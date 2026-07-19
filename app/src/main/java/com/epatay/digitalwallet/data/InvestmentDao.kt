package com.epatay.digitalwallet.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface InvestmentDao {

    // Tüm yatırımları en yeni kayıt üstte olacak şekilde getirir
    @Query(
        "SELECT * FROM investments_table " +
                "ORDER BY id DESC"
    )
    fun getAllInvestments(): LiveData<List<InvestmentItem>>

    // Yeni yatırım ekler
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertInvestment(
        investment: InvestmentItem
    )

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
}