package com.epatay.digitalwallet.data

import android.content.Context
import com.google.gson.Gson

class CurrencyManager(context: Context) {

    companion object {
        private const val PREFS_NAME =
            "currency_prefs"

        private const val RATES_KEY =
            "last_rates"

        private const val GRAM_GOLD_PRICE_KEY =
            "gram_gold_try"

        private const val RATES_FETCHED_AT_KEY =
            "rates_fetched_at"
    }

    private val sharedPreferences =
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

    private val gson = Gson()

    /**
     * Kurları ve uygulamanın bu veriyi aldığı yerel zamanı
     * aynı SharedPreferences işlemi içinde kaydeder.
     *
     * Dönen değer ekrandaki "Son güncelleme" yazısının
     * anında yenilenmesi için kullanılır.
     */
    fun saveRates(
        response: ExchangeRateResponse
    ): Long {

        val json =
            gson.toJson(response)

        val fetchedAt =
            System.currentTimeMillis()

        sharedPreferences
            .edit()
            .putString(
                RATES_KEY,
                json
            )
            .putLong(
                RATES_FETCHED_AT_KEY,
                fetchedAt
            )
            .apply()

        return fetchedAt
    }

    fun getRatesFetchedAt(): Long {

        return sharedPreferences.getLong(
            RATES_FETCHED_AT_KEY,
            0L
        )
    }

    fun shouldRefreshRates(
        maxAgeMillis: Long
    ): Boolean {

        val fetchedAt =
            getRatesFetchedAt()

        return fetchedAt <= 0L ||
                System.currentTimeMillis() - fetchedAt >= maxAgeMillis
    }

    fun getSavedRates(): ExchangeRateResponse? {

        val json =
            sharedPreferences.getString(
                RATES_KEY,
                null
            ) ?: return null

        return try {

            gson.fromJson(
                json,
                ExchangeRateResponse::class.java
            )

        } catch (exception: Exception) {

            null
        }
    }

    fun saveGramGoldPrice(
        price: Double
    ) {

        if (
            !price.isFinite() ||
            price <= 0.0
        ) {
            return
        }

        sharedPreferences
            .edit()
            .putString(
                GRAM_GOLD_PRICE_KEY,
                price.toString()
            )
            .apply()
    }

    fun getSavedGramGoldPrice(): Double? {

        return sharedPreferences
            .getString(
                GRAM_GOLD_PRICE_KEY,
                null
            )
            ?.toDoubleOrNull()
            ?.takeIf {
                it.isFinite() &&
                        it > 0.0
            }
    }
}