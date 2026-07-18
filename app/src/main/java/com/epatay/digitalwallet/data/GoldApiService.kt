package com.epatay.digitalwallet.data

import retrofit2.http.GET

interface GoldApiService {

    @GET("price/XAU")
    suspend fun getGoldPrice(): GoldPriceResponse
}