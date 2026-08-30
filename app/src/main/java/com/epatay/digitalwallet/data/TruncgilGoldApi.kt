package com.epatay.digitalwallet.data

import com.google.gson.JsonObject
import retrofit2.http.GET

interface TruncgilGoldApi {
    @GET("v3/today.json")
    suspend fun getTodayRates(): JsonObject
}
