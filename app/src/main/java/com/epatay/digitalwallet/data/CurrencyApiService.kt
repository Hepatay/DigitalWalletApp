package com.epatay.digitalwallet.data

import retrofit2.http.GET

interface CurrencyApiService {

    @GET("v6/latest/TRY")
    suspend fun getLatestRates():
            ExchangeRateResponse
}