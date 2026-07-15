package com.epatay.digitalwallet.data
// CurrencyManager.kt (Yeni bir dosya oluştur)
import android.content.Context
import com.google.gson.Gson

class CurrencyManager(context: Context) {
    private val sharedPreferences = context.getSharedPreferences("currency_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveRates(response: ExchangeRateResponse) {
        val json = gson.toJson(response)
        sharedPreferences.edit().putString("last_rates", json).apply()
    }

    fun getSavedRates(): ExchangeRateResponse? {
        val json = sharedPreferences.getString("last_rates", null) ?: return null
        return gson.fromJson(json, ExchangeRateResponse::class.java)
    }
}