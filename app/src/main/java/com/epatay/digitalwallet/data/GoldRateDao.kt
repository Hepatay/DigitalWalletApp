package com.epatay.digitalwallet.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
abstract class GoldRateDao {

    @Query("SELECT * FROM gold_rates")
    abstract fun observeAll(): Flow<List<GoldRateEntity>>

    @Query("SELECT * FROM gold_rates")
    abstract suspend fun getAllOnce(): List<GoldRateEntity>

    @Upsert
    protected abstract suspend fun upsertAll(
        rates: List<GoldRateEntity>
    )

    @Query("DELETE FROM gold_rates")
    protected abstract suspend fun deleteAll()

    @Transaction
    open suspend fun replaceAll(
        rates: List<GoldRateEntity>
    ) {
        deleteAll()
        upsertAll(rates)
    }
}
