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
    }

    private val sharedPreferences =
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

    private val gson = Gson()

    fun saveRates(response: ExchangeRateResponse) {

        val json = gson.toJson(response)

        sharedPreferences
            .edit()
            .putString(RATES_KEY, json)
            .apply()
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

        } catch (e: Exception) {
            null
        }
    }

    fun saveGramGoldPrice(price: Double) {

        if (!price.isFinite() || price <= 0.0) {
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
                it.isFinite() && it > 0.0
            }
    }
}