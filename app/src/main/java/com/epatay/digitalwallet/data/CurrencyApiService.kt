package com.epatay.digitalwallet.data // Doğru pakette olduğundan emin ol

import retrofit2.http.GET
// --- EKSİK OLAN IMPORT ---
import com.epatay.digitalwallet.data.ExchangeRateResponse

interface CurrencyApiService {
    // API anahtarını alıp "YOUR_API_KEY" yerine yapıştır
    @GET("v6/41b4fd1b7f933911cb12ee68/latest/TRY")
    suspend fun getLatestRates(): ExchangeRateResponse
}