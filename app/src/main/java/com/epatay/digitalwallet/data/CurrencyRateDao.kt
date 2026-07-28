package com.epatay.digitalwallet.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class CurrencyRateDao {

    @Query("SELECT * FROM currency_rates")
    abstract fun observeAllRates(): Flow<List<CurrencyRateEntity>>

    @Query(
        """
        SELECT *
        FROM currency_rates
        """
    )
    abstract suspend fun getAllRates(): List<CurrencyRateEntity>

    @Query("SELECT MAX(fetchedAtMillis) FROM currency_rates")
    abstract suspend fun getLatestFetchedAt(): Long?

    @Query("DELETE FROM currency_rates WHERE UPPER(currencyCode) = 'XDR'")
    abstract suspend fun deleteXdr()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertAll(
        rates: List<CurrencyRateEntity>
    )

    @Query("DELETE FROM currency_rates")
    protected abstract suspend fun deleteAll()

    @Transaction
    open suspend fun replaceAll(
        rates: List<CurrencyRateEntity>
    ) {
        deleteAll()
        insertAll(rates)
    }
}
